import model.*;
import java.util.*;

import static java.lang.StrictMath.*;

public final class MyStrategy implements Strategy {

    private static final double BRAKE_SPEED = 10.0;
    private static final double DAMAGE_EPS = 0.001;
    private static final double STUCK_SPEED = 5.0;
    private static final int STUCK_START_TICKS = 150;
    private static final int STUCK_DURATION = 150;

    enum State { START, RUN, STUCK };

    private State state = State.START;
    private int stuckTickCount = 0;
    private int stuckStartTick;

    @Override
    public void move(Car self, World world, Game game, Move move) {
        updateFields(self, world, game);

        List<Point2I> nextSubtiles = getNextSubtiles();
        Point2I nextSubtile = nextSubtiles.get(min(CHASE_TILE, nextSubtiles.size() - 1));
        double nextX = (nextSubtile.x + 0.5) * getSubtileSize();
        double nextY = (nextSubtile.y + 0.5) * getSubtileSize();
        double angle = self.getAngleTo(nextX, nextY);

        if (state == State.START && world.getTick() > game.getInitialFreezeDurationTicks()) {
            state = State.RUN;
        }

        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());
        if (state == state.RUN) {
            // TODO: don't wait when pointed at a wall
            if (speedModule < STUCK_SPEED
                    && self.getDurability() > DAMAGE_EPS
                    && ++stuckTickCount >= STUCK_START_TICKS){
                state = State.STUCK;
                stuckStartTick = world.getTick();
            }
        }
        else if (state == State.STUCK
                && (speedModule > STUCK_SPEED
                || world.getTick() - stuckStartTick > STUCK_DURATION)) {
            state = State.RUN;
            stuckTickCount = 0;
        }

        if (state == State.STUCK) {
            move.setEnginePower(-1.0);
            move.setWheelTurn(-32.0 / PI * angle);
        }
        else {
            // TODO: improve steering to cause less damage
            move.setWheelTurn(32.0 / PI * angle);
            move.setEnginePower(1.0);
            if (speedModule * abs(angle) > 25.0 * PI / 8 && speedModule > BRAKE_SPEED) {
                move.setBrake(true);
            }

            if (state != state.START) {
                if (self.getProjectileCount() > 0) {
                    for (Car enemy : world.getCars()) {
                        if (!enemy.isTeammate()
                                && self.getDistanceTo(enemy) < game.getTrackTileSize() * 1.5
                                && abs(self.getAngleTo(enemy)) < game.getSideWasherAngle()) {
                            move.setThrowProjectile(true);
                            break;
                        }
                    }
                }

                if (self.getOilCanisterCount() > 0) {
                    for (Car enemy : world.getCars()) {
                        if (!enemy.isTeammate()
                                && self.getDistanceTo(enemy) < game.getTrackTileSize() * 1.5
                                && self.getDistanceTo(enemy) > game.getCarHeight() * 2
                                && (self.getAngleTo(enemy) < -PI * 5 / 6 || self.getAngleTo(enemy) > PI * 5 / 6)) {
                                move.setSpillOil(true);
                                break;
                            }
                        }
                    }

                if (self.getNitroChargeCount() > 0 && abs(angle) < PI / 10) {
                    move.setUseNitro(true); // TODO: only use nitro when going straight or ladder
                }
            }
        }
    }

    private Car self;
    private Point2D nose;
    private World world;
    private Game game;

    private void updateFields(Car self, World world, Game game) {
        this.self = self;
        this.nose = new Point2D(self.getX() + cos(self.getAngle()) * game.getCarWidth() / 2,
                                self.getY() + sin(self.getAngle()) * game.getCarWidth() / 2);
        this.world = world;
        this.game = game;
    }


    enum SubtileType {WALL, ROAD};

    private static final int SUBTILE_COUNT = 5;
    private static final int SUBTILE_LEFT;
    private static final int SUBTILE_RIGHT;
    private static final int SUBTILE_TOP;
    private static final int SUBTILE_BOTTOM;

    static {
        SUBTILE_LEFT = 0;
        SUBTILE_RIGHT = SUBTILE_COUNT - 1;
        SUBTILE_TOP = 0;
        SUBTILE_BOTTOM = SUBTILE_COUNT - 1;
    }

    private static final Point2I[] tileDijkstraDXY = {
            new Point2I(0, -1),
            new Point2I(1, 0),
            new Point2I(0, 1),
            new Point2I(-1, 0),
    };

    private List<Point2I> tileDijkstra(Point2I start, Point2I end) {
        Map<Point2I, Point2I> prev = new HashMap<Point2I, Point2I>();
        Map<Point2I, Double> dist = new HashMap<Point2I, Double>();
        Queue<Point2I> queue = new PriorityQueue<Point2I>(new Comparator<Point2I>() {
            @Override
            public int compare(Point2I a, Point2I b) {
                return Double.compare(dist.get(a), dist.get(b));
            }
        });
        prev.put(start, start);
        dist.put(start, 0.0);
        queue.add(start);
        TileType[][] tiles = world.getTilesXY();
        while (!queue.isEmpty()) {
            Point2I vertex = queue.remove();
            if (vertex.equals(end)) {
                break;
            }
            for (Point2I dxy : tileDijkstraDXY) {
                if (canGo(tiles[vertex.x][vertex.y], dxy)) {
                    Point2I nextVertex = new Point2I(vertex.x + dxy.x, vertex.y + dxy.y);
                    if (0 <= nextVertex.x && nextVertex.x < tiles.length
                            && 0 <= nextVertex.y && nextVertex.y < tiles[nextVertex.x].length
                            && tiles[nextVertex.x][nextVertex.y] != TileType.EMPTY
                            && !prev.containsKey(nextVertex)) {
                        Double option = dist.get(vertex) + hypot(nextVertex.x - vertex.x, nextVertex.y - vertex.y);
                        if (option < dist.getOrDefault(nextVertex, Double.POSITIVE_INFINITY)) {
                            prev.put(nextVertex, vertex);
                            dist.put(nextVertex, option);
                            queue.add(nextVertex);
                        }
                    }
                }
            }
        }

        List<Point2I> path = new LinkedList<Point2I>();
        Point2I vertex = end;
        do {
            path.add(vertex);
            vertex = prev.get(vertex);
        } while (!vertex.equals(start));
        Collections.reverse(path);
        return path;
    }

    private static final Point2I DIRECTION_UP    = new Point2I(0, -1);
    private static final Point2I DIRECTION_DOWN  = new Point2I(0, 1);
    private static final Point2I DIRECTION_LEFT  = new Point2I(-1, 0);
    private static final Point2I DIRECTION_RIGHT = new Point2I(1, 0);

    private boolean canGo(TileType type, Point2I direction) {
        switch (type) {
            case LEFT_TOP_CORNER:
                return direction.equals(DIRECTION_DOWN) || direction.equals(DIRECTION_RIGHT);
            case RIGHT_TOP_CORNER:
                return direction.equals(DIRECTION_DOWN) || direction.equals(DIRECTION_LEFT);
            case LEFT_BOTTOM_CORNER:
                return direction.equals(DIRECTION_UP) || direction.equals(DIRECTION_RIGHT);
            case RIGHT_BOTTOM_CORNER:
                return direction.equals(DIRECTION_UP) || direction.equals(DIRECTION_LEFT);
            case VERTICAL:
                return direction.equals(DIRECTION_UP) || direction.equals(DIRECTION_DOWN);
            case HORIZONTAL:
                return direction.equals(DIRECTION_LEFT) || direction.equals(DIRECTION_RIGHT);
            case CROSSROADS:
                return true;
            case LEFT_HEADED_T:
                return !direction.equals(DIRECTION_RIGHT);
            case RIGHT_HEADED_T:
                return !direction.equals(DIRECTION_LEFT);
            case TOP_HEADED_T:
                return !direction.equals(DIRECTION_DOWN);
            case BOTTOM_HEADED_T:
                return !direction.equals(DIRECTION_UP);
            case EMPTY:
                return false;
            case UNKNOWN:
                return true;
            default:
                return false;
        }
    }

    private static final Point2I[] subtileDijkstraDXY = {
            new Point2I(0, -1),
            new Point2I(1, -1),
            new Point2I(1, 0),
            new Point2I(1, 1),
            new Point2I(0, 1),
            new Point2I(-1, 1),
            new Point2I(-1, 0),
            new Point2I(-1, -1),
    };

    // TODO: vary on bonus type
    private static final double BONUS_REWARD = -1.0; // straight is 1.0, diagonal is 1.41421356

    private List<Point2I> subtileDijkstra(Point2I start, Point2I end, SubtileType[][] subtiles) {
        Map<Point2I, Integer> bonusCount = countBonuses();
        Map<Point2I, Point2I> prev = new HashMap<Point2I, Point2I>();
        Map<Point2I, Double> dist = new HashMap<Point2I, Double>();
        Queue<Point2I> queue = new PriorityQueue<Point2I>(new Comparator<Point2I>() {
            @Override
            public int compare(Point2I a, Point2I b) {
                return Double.compare(dist.get(a), dist.get(b));
            }
        });
        prev.put(start, start);
        dist.put(start, 0.0);
        queue.add(start);
        while (!queue.isEmpty()) {
            Point2I vertex = queue.remove();
            if (vertex.equals(end)) {
                break;
            }
            for (Point2I dxy : subtileDijkstraDXY) {
                Point2I nextVertex = new Point2I(vertex.x + dxy.x, vertex.y + dxy.y);
                if (0 <= nextVertex.x && nextVertex.x < subtiles.length
                        && 0 <= nextVertex.y && nextVertex.y < subtiles[nextVertex.x].length
                        && subtiles[nextVertex.x][nextVertex.y] != SubtileType.WALL
                        && !prev.containsKey(nextVertex)) {
                    Double option = dist.get(vertex)
                            + hypot(nextVertex.x - vertex.x, nextVertex.y - vertex.y)
                            + BONUS_REWARD * bonusCount.getOrDefault(nextVertex, 0);
                    if (option < dist.getOrDefault(nextVertex, Double.POSITIVE_INFINITY)) {
                        prev.put(nextVertex, vertex);
                        dist.put(nextVertex, option);
                        queue.add(nextVertex);
                    }
                }
            }
        }

        List<Point2I> path = new LinkedList<Point2I>();
        Point2I vertex = end;
        do {
            path.add(vertex);
            vertex = prev.get(vertex);
        } while (!vertex.equals(start));
        Collections.reverse(path);
        return path;
    }

    private Map<Point2I, Integer> countBonuses() {
        Map<Point2I, Integer> result = new HashMap<Point2I, Integer>();
        for (Bonus bonus : world.getBonuses()) {
            Point2I subtile = toSubtilePoint(bonus);
            result.put(subtile, result.getOrDefault(subtile, 0) + 1);
        }
        return result;
    }

    private Point2I getNextWP(int skip) {
        int[] nextWPArray = world.getWaypoints()[(self.getNextWaypointIndex() + skip) % world.getWaypoints().length];
        return new Point2I(nextWPArray[0], nextWPArray[1]);
    }

    private List<Point2I> getNextTiles(int count) {
        List<Point2I> result = new LinkedList<Point2I>();
        Point2I tile = toTilePoint(nose);
        for (int skip = 0; result.size() < count; ++skip) {
            Point2I target = getNextWP(skip);
            for (Point2I pathTile : tileDijkstra(tile, target)) {
                result.add(pathTile);
                if (result.size() == count) {
                    break;
                }
            }
            tile = target;
        }
        return result;
    }

    private static final int CHASE_TILE = 2;

    private List<Point2I> getNextSubtiles() {
        List<Point2I> tiles = getNextTiles(4);
        tiles.add(0, toTilePoint(nose));

        Point2I t0 = tiles.get(0);
        Point2I t1 = tiles.get(1);
        Point2I t2 = tiles.get(2);
        Point2I t3 = tiles.get(3);
        Point2I t4 = tiles.get(4);

        SubtileType[][] subtiles = new SubtileType[world.getWidth() * SUBTILE_COUNT][world.getHeight() * SUBTILE_COUNT];
        for (int x = 0; x < subtiles.length; ++x) {
            for (int y = 0; y < subtiles[x].length; ++y) {
                subtiles[x][y] = SubtileType.WALL;
            }
        }
        for (Point2I tile : tiles) {
            for (int dx = 0; dx < SUBTILE_COUNT; ++dx) {
                for (int dy = 0; dy < SUBTILE_COUNT; ++dy) {
                    SubtileType subtileType = SubtileType.ROAD;
                    switch (world.getTilesXY()[tile.x][tile.y]) {
                        case LEFT_TOP_CORNER:
                            if (dx == SUBTILE_LEFT || dy == SUBTILE_TOP || (dx == SUBTILE_RIGHT && dy == SUBTILE_BOTTOM)) {
                                subtileType = SubtileType.WALL;
                            }
                            break;
                        case RIGHT_TOP_CORNER:
                            if (dx == SUBTILE_RIGHT || dy == SUBTILE_TOP || (dx == SUBTILE_LEFT && dy == SUBTILE_BOTTOM)) {
                                subtileType = SubtileType.WALL;
                            }
                            break;
                        case LEFT_BOTTOM_CORNER:
                            if (dx == SUBTILE_LEFT || dy == SUBTILE_BOTTOM || (dx == SUBTILE_RIGHT && dy == SUBTILE_TOP)) {
                                subtileType = SubtileType.WALL;
                            }
                            break;
                        case RIGHT_BOTTOM_CORNER:
                            if (dx == SUBTILE_RIGHT || dy == SUBTILE_BOTTOM || (dx == SUBTILE_LEFT && dy == SUBTILE_TOP)) {
                                subtileType = SubtileType.WALL;
                            }
                            break;
                        case VERTICAL:
                            if (dx == SUBTILE_LEFT || dx == SUBTILE_RIGHT) {
                                subtileType = SubtileType.WALL;
                            }
                            break;
                        case HORIZONTAL:
                            if (dy == SUBTILE_TOP || dy == SUBTILE_BOTTOM) {
                                subtileType = SubtileType.WALL;
                            }
                            break;
                        case CROSSROADS:
                            if ((dx == SUBTILE_LEFT || dx == SUBTILE_RIGHT) && (dy == SUBTILE_TOP || dy == SUBTILE_BOTTOM)) {
                                subtileType = SubtileType.WALL;
                            }
                            break;
                        case LEFT_HEADED_T:
                            if (dx == SUBTILE_RIGHT || (dx == SUBTILE_LEFT && (dy == SUBTILE_TOP || dy == SUBTILE_BOTTOM))) {
                                subtileType = SubtileType.WALL;
                            }
                            break;
                        case RIGHT_HEADED_T:
                            if (dx == SUBTILE_LEFT || (dx == SUBTILE_RIGHT && (dy == SUBTILE_TOP || dy == SUBTILE_BOTTOM))) {
                                subtileType = SubtileType.WALL;
                            }
                            break;
                        case TOP_HEADED_T:
                            if (dy == SUBTILE_BOTTOM || (dy == SUBTILE_TOP && (dx == SUBTILE_LEFT || dx == SUBTILE_RIGHT))) {
                                subtileType = SubtileType.WALL;
                            }
                            break;
                        case BOTTOM_HEADED_T:
                            if (dy == SUBTILE_TOP || (dy == SUBTILE_BOTTOM && (dx == SUBTILE_LEFT || dx == SUBTILE_RIGHT))) {
                                subtileType = SubtileType.WALL;
                            }
                            break;
                        case EMPTY:
                            subtileType = SubtileType.WALL;
                            break;
                        case UNKNOWN:
                            break;
                    }
                    subtiles[tile.x * SUBTILE_COUNT + dx][tile.y * SUBTILE_COUNT + dy] = subtileType;
                }
            }
        }

        addWalls(t0, t1, t2, subtiles);
        addWalls(t1, t2, t3, subtiles);

        if (isStraight(t0, t1) && isStraight(t0, t2)) {
            if (!isStraight(t0, t3)) {
                Point2I forward = new Point2I(t1.x - t0.x, t1.y - t0.y);
                Point2I turn = new Point2I(t3.x - t2.x, t3.y - t2.y);

                addWall(subtiles, t1, -forward.x, -forward.y);
                addWall(subtiles, t1, turn.x - forward.x, turn.y - forward.y);
            }
            else if (!isStraight(t0, t4)) {
                Point2I forward = new Point2I(t1.x - t0.x, t1.y - t0.y);
                Point2I turn = new Point2I(t4.x - t3.x, t4.y - t3.y);

                addWall(subtiles, t1, -forward.x, -forward.y);
                addWall(subtiles, t1, turn.x - forward.x, turn.y - forward.y);
            }
        }

        Point2I start = toSubtilePoint(nose);
        Point2I end = centerSubtile(tiles.get(tiles.size() - 1));
        subtiles[start.x][start.y] = SubtileType.ROAD;
        subtiles[end.x][end.y] = SubtileType.ROAD;
        return subtileDijkstra(start, end, subtiles);
    }

    private boolean isStraight(Point2I a, Point2I b) {
        return a.x == b.x || a.y == b.y;
    }

    private void addWalls(Point2I a, Point2I b, Point2I c, SubtileType[][] subtiles) {
        Point2I ac = new Point2I(c.x - a.x, c.y - a.y);
        if (abs(ac.x) == 1 && abs(ac.y) == 1) { // Not straight
            Point2I ab = new Point2I(b.x - a.x, b.y - a.y);
            Point2I bc = new Point2I(c.x - b.x, c.y - b.y);

            addWall(subtiles, b, 0, 0);
            addWall(subtiles, b, ab.x, ab.y);
            addWall(subtiles, b, ab.x + bc.x, ab.y + bc.y);
            addWall(subtiles, b, -bc.x, -bc.y);
            addWall(subtiles, b, -bc.x - ab.x, -bc.y - ab.y);
        }
    }

    private void addWall(SubtileType[][] subtiles, Point2I tile, int offsetX, int offsetY) {
        int x = tile.x * SUBTILE_COUNT + SUBTILE_COUNT / 2 + offsetX;
        int y = tile.y * SUBTILE_COUNT + SUBTILE_COUNT / 2 + offsetY;
        if (0 <= x && x < subtiles.length && 0 <= y && y < subtiles[x].length) {
            subtiles[x][y] = SubtileType.WALL;
        }
    }

    private Point2I centerSubtile(Point2I tile) {
        return new Point2I(tile.getX() * SUBTILE_COUNT + SUBTILE_COUNT / 2,
                tile.getY() * SUBTILE_COUNT + SUBTILE_COUNT / 2);
    }

    private Point2I subtileToTile(Point2I subtile) {
        return new Point2I(subtile.x / SUBTILE_COUNT, subtile.y / SUBTILE_COUNT);
    }

    private int toTileCoordinate(double coordinate) {
        return (int) (coordinate / game.getTrackTileSize());
    }

    private Point2I toTilePoint(Unit unit) {
        return new Point2I(toTileCoordinate(unit.getX()), toTileCoordinate(unit.getY()));
    }

    private Point2I toTilePoint(Point2D unit) {
        return new Point2I(toTileCoordinate(unit.getX()), toTileCoordinate(unit.getY()));
    }

    private int toSubtileCoordinate(double coordinate) {
        return (int) (coordinate / getSubtileSize());
    }

    private Point2I toSubtilePoint(Unit unit) {
        return new Point2I(toSubtileCoordinate(unit.getX()), toSubtileCoordinate(unit.getY()));
    }

    private Point2I toSubtilePoint(Point2D unit) {
        return new Point2I(toSubtileCoordinate(unit.getX()), toSubtileCoordinate(unit.getY()));
    }

    private double getSubtileSize() {
        return game.getTrackTileSize() / SUBTILE_COUNT;
    }
}

class Point2I {
    public final int x;
    public final int y;

    public Point2I(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point2I(double x, double y) {
        this.x = toInt(round(x));
        this.y = toInt(round(y));
    }

    private static int toInt(double value) {
        @SuppressWarnings("NumericCastThatLosesPrecision") int intValue = (int) value;
        if (abs((double) intValue - value) < 1.0) {
            return intValue;
        }
        throw new IllegalArgumentException("Can't convert double " + value + " to int.");
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point2I point2I = (Point2I) o;

        if (x != point2I.x) return false;
        if (y != point2I.y) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    @Override
    public String toString() {
        return "Point2I{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}

class Point2D {
    private double x;
    private double y;

    public Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point2D() {
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}

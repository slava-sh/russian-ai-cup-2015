import model.*;
import java.util.*;

import static java.lang.StrictMath.*;

public final class MyStrategy implements Strategy {
    private static final double STUCK_SPEED = 5.0;
    private static final int STUCK_TICKS = 40;
    private static final int LONG_STUCK_TICKS = 150;

    enum State { START, RUN, STUCK };

    private State state = State.START;
    private int stuckTickCount = 0;
    private int stuckStartTick;
    private Point2I stuckTarget;

    @Override
    public void move(Car self, World world, Game game, Move move) {
        updateFields(self, world, game);

        List<Point2I> nextSubtiles = getNextSubtiles();
        Point2I nextSubtile = nextSubtiles.get(3);
        double nextX = (nextSubtile.x + 0.5) * getSubtileSize();
        double nextY = (nextSubtile.y + 0.5) * getSubtileSize();
        double angle = self.getAngleTo(nextX, nextY);

        if (state == State.START && world.getTick() > game.getInitialFreezeDurationTicks()) {
            state = State.RUN;
        }

        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());
        if (state == state.RUN) {
            if (speedModule < STUCK_SPEED && ++stuckTickCount >= STUCK_TICKS){
                state = State.STUCK;
                stuckStartTick = world.getTick();
            }
        }
        else if (state == State.STUCK
                && (speedModule > STUCK_SPEED
                || abs(angle) < PI / 6
                || world.getTick() - stuckStartTick > LONG_STUCK_TICKS)) {
            state = State.RUN;
            stuckTickCount = 0;
        }

        if (state == State.STUCK) {
            move.setEnginePower(-1.0);
            move.setWheelTurn(-32.0 / PI * angle);
        }
        else {
            boolean isReverse = self.getEnginePower() < 0.0;
            if (isReverse) {
                move.setWheelTurn(-32.0 / PI * angle);
            } else {
                move.setWheelTurn(32.0 / PI * angle);
            }

            move.setEnginePower(0.75);
            if (abs(angle) > PI / 4 && speedModule > STUCK_SPEED) {
                //move.setBrake(true);
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
                    //move.setUseNitro(true);
                }
            }
        }
    }

    private Car self;
    private World world;
    private Game game;

    private void updateFields(Car self, World world, Game game) {
        this.self = self;
        this.world = world;
        this.game = game;


        setNextWP(self.getNextWaypointX(), self.getNextWaypointY());
    }

    private Point2I nextWP;
    private Point2I nextWPSubtile;

    private void setNextWP(int x, int y) {
        nextWP = new Point2I(x, y); // TODO: need this?
        nextWPSubtile = new Point2I(x * SUBTILE_COUNT + SUBTILE_COUNT / 2,
                                    y * SUBTILE_COUNT + SUBTILE_COUNT / 2);
    }

    private int manhattanDistance(Point2I a, Point2I b) {
        return abs(a.x - b.x) + abs(a.y - b.y);
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
                Point2I nextVertex = new Point2I(vertex.x + dxy.x, vertex.y + dxy.y);
                if (0 <= nextVertex.x && nextVertex.x < tiles.length
                        && 0 <= nextVertex.y && nextVertex.y < tiles[nextVertex.x].length
                        && tiles[nextVertex.x][nextVertex.y] != TileType.EMPTY
                        && tiles[nextVertex.x][nextVertex.y] != TileType.UNKNOWN
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

        List<Point2I> path = new LinkedList<Point2I>();
        Point2I vertex = end;
        do {
            path.add(vertex);
            vertex = prev.get(vertex);
        } while (!vertex.equals(start));
        Collections.reverse(path);
        return path;
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

    private List<Point2I> subtileDijkstra(Point2I start, Point2I end, SubtileType[][] subtiles) {
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
                    Double option = dist.get(vertex) + hypot(nextVertex.x - vertex.x, nextVertex.y - vertex.y);
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

    private Point2I getNextWP(int skip) {
        int[] nextWPArray = world.getWaypoints()[(self.getNextWaypointIndex() + skip) % world.getWaypoints().length];
        return new Point2I(nextWPArray[0], nextWPArray[1]);
    }

    private List<Point2I> getNextTiles(int count) {
        List<Point2I> result = new LinkedList<Point2I>();
        Point2I tile = toTilePoint(self);
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

    private List<Point2I> getNextSubtiles() {
        List<Point2I> tiles = getNextTiles(3);
        tiles.add(0, toTilePoint(self));

        SubtileType[][] subtiles = new SubtileType[world.getWidth() * SUBTILE_COUNT][world.getHeight() * SUBTILE_COUNT];
        for (int x = 0; x < subtiles.length; ++x) {
            for (int y = 0; y < subtiles[x].length; ++y) {
                subtiles[x][y] = SubtileType.WALL;
            }
        }
        for (Point2I tile : tiles) {
            for (int dx = 0; dx < SUBTILE_COUNT; ++dx) {
                for (int dy = 0; dy < SUBTILE_COUNT; ++dy) {
                    subtiles[tile.x * SUBTILE_COUNT + dx][tile.y * SUBTILE_COUNT + dy] = SubtileType.ROAD;
                }
            }
        }

        Point2I start = toSubtilePoint(self);
        Point2I end = centerSubtile(tiles.get(tiles.size() - 1));
        return subtileDijkstra(start, end, subtiles);
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

    private int toSubtileCoordinate(double coordinate) {
        return (int) (coordinate / getSubtileSize());
    }

    private Point2I toSubtilePoint(Unit unit) {
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

class Endpoints {
    public final Point2I start;
    public final Point2I end;

    public Endpoints(Point2I start, Point2I end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Endpoints endpoints = (Endpoints) o;

        if (!start.equals(endpoints.start)) return false;
        if (!end.equals(endpoints.end)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = start.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }
}

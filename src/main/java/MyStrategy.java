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

        Point2I target = nextWPSubtile;
        Point2I nextSubtile0 = getNextSubtile(toSubtilePoint(self), target);
        Point2I nextSubtile1 = getNextSubtile(nextSubtile0, target);
        Point2I nextSubtile2 = getNextSubtile(nextSubtile1, target);
        double nextX = (nextSubtile2.x + 0.5) * getSubtileSize();
        double nextY = (nextSubtile2.y + 0.5) * getSubtileSize();
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

            move.setEnginePower(1.0);
            if (abs(angle) > PI / 4 && speedModule > STUCK_SPEED) {
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
                    move.setUseNitro(true);
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

        if (subtilesXY == null || needRebuildSubtiles) {
            needRebuildSubtiles = false;
            createSubtiles();
        }

        setNextWP(self.getNextWaypointX(), self.getNextWaypointY());
    }

    private Point2I nextWP;
    private Point2I nextWPSubtile;

    private void setNextWP(int x, int y) {
        nextWP = new Point2I(x, y);
        nextWPSubtile = new Point2I(x * SUBTILE_COUNT + SUBTILE_COUNT / 2,
                                    y * SUBTILE_COUNT + SUBTILE_COUNT / 2);

        int[] afterNextWPArray = world.getWaypoints()[(self.getNextWaypointIndex() + 1) % world.getWaypoints().length];
        Point2I afterNextWPSubtile = new Point2I(afterNextWPArray[0] * SUBTILE_COUNT + SUBTILE_COUNT / 2,
                                                 afterNextWPArray[1] * SUBTILE_COUNT + SUBTILE_COUNT / 2);

        int dist = manhattanDistance(nextWPSubtile, afterNextWPSubtile);
        for (int dx = 0; dx < SUBTILE_COUNT; ++dx) {
            for (int dy = 0; dy < SUBTILE_COUNT; ++dy) {
                Point2I option = new Point2I(x * SUBTILE_COUNT + dx, y * SUBTILE_COUNT + dy);
                int optionDist = manhattanDistance(option, afterNextWPSubtile);
                if (subtilesXY[option.x][option.y] != SubtileType.WALL) {
                    if (optionDist < dist) {
                        nextWPSubtile = option;
                        dist = optionDist;
                    }
                }
            }
        }
    }

    private int manhattanDistance(Point2I a, Point2I b) {
        return abs(a.x - b.x) + abs(a.y - b.y);
    }

    enum SubtileType {WALL, ROAD};

    private static final int SUBTILE_COUNT = 7;
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

    private int toSubtileCoordinate(double coordinate) {
        return (int) (coordinate / getSubtileSize());
    }

    private Point2I toSubtilePoint(Unit unit) {
        return new Point2I(toSubtileCoordinate(unit.getX()), toSubtileCoordinate(unit.getY()));
    }

    private double getSubtileSize() {
        return game.getTrackTileSize() / SUBTILE_COUNT;
    }

    private SubtileType[][] subtilesXY;
    private boolean needRebuildSubtiles = false;

    private void createSubtiles() {
        subtilesXY = new SubtileType[world.getWidth() * SUBTILE_COUNT][world.getHeight() * SUBTILE_COUNT];
        for (int tileX = 0; tileX < world.getWidth(); ++tileX) {
            for (int i = 0; i < SUBTILE_COUNT; ++i) {
                int subtileX = tileX * SUBTILE_COUNT + i;
                for (int tileY = 0; tileY < world.getWidth(); ++tileY) {
                    for (int j = 0; j < SUBTILE_COUNT; ++j) {
                        int subtileY = tileY * SUBTILE_COUNT + j;
                        SubtileType subtileType = SubtileType.ROAD;
                        switch (world.getTilesXY()[tileX][tileY]) {
                            case LEFT_TOP_CORNER:
                                if (i == SUBTILE_LEFT || j == SUBTILE_TOP || (i == SUBTILE_RIGHT && j == SUBTILE_BOTTOM)) {
                                    subtileType = SubtileType.WALL;
                                }
                                break;
                            case RIGHT_TOP_CORNER:
                                if (i == SUBTILE_RIGHT || j == SUBTILE_TOP || (i == SUBTILE_LEFT && j == SUBTILE_BOTTOM)) {
                                    subtileType = SubtileType.WALL;
                                }
                                break;
                            case LEFT_BOTTOM_CORNER:
                                if (i == SUBTILE_LEFT || j == SUBTILE_BOTTOM || (i == SUBTILE_RIGHT && j == SUBTILE_TOP)) {
                                    subtileType = SubtileType.WALL;
                                }
                                break;
                            case RIGHT_BOTTOM_CORNER:
                                if (i == SUBTILE_RIGHT || j == SUBTILE_BOTTOM || (i == SUBTILE_LEFT && j == SUBTILE_TOP)) {
                                    subtileType = SubtileType.WALL;
                                }
                                break;
                            case VERTICAL:
                                if (i == SUBTILE_LEFT || i == SUBTILE_RIGHT) {
                                    subtileType = SubtileType.WALL;
                                }
                                break;
                            case HORIZONTAL:
                                if (j == SUBTILE_TOP || j == SUBTILE_BOTTOM) {
                                    subtileType = SubtileType.WALL;
                                }
                                break;
                            case CROSSROADS:
                                if ((i == SUBTILE_LEFT || i == SUBTILE_RIGHT) && (j == SUBTILE_TOP || j == SUBTILE_BOTTOM)) {
                                    subtileType = SubtileType.WALL;
                                }
                                break;
                            case LEFT_HEADED_T:
                                if (i == SUBTILE_RIGHT || (i == SUBTILE_LEFT && (j == SUBTILE_TOP || j == SUBTILE_BOTTOM))) {
                                    subtileType = SubtileType.WALL;
                                }
                                break;
                            case RIGHT_HEADED_T:
                                if (i == SUBTILE_LEFT || (i == SUBTILE_RIGHT && (j == SUBTILE_TOP || j == SUBTILE_BOTTOM))) {
                                    subtileType = SubtileType.WALL;
                                }
                                break;
                            case TOP_HEADED_T:
                                if (j == SUBTILE_BOTTOM || (j == SUBTILE_TOP && (i == SUBTILE_LEFT || i == SUBTILE_RIGHT))) {
                                    subtileType = SubtileType.WALL;
                                }
                                break;
                            case BOTTOM_HEADED_T:
                                if (j == SUBTILE_TOP || (j == SUBTILE_BOTTOM && (i == SUBTILE_LEFT || i == SUBTILE_RIGHT))) {
                                    subtileType = SubtileType.WALL;
                                }
                                break;
                            case EMPTY:
                                subtileType = SubtileType.WALL;
                                break;
                            case UNKNOWN:
                                subtileType = SubtileType.WALL;
                                needRebuildSubtiles = true;
                                break;
                            default:
                        }
                        subtilesXY[subtileX][subtileY] = subtileType;
                    }
                }
            }
        }
    }

    private static final Point2I[] DXY = {
            new Point2I(0, -1),
            new Point2I(1, -1),
            new Point2I(1, 0),
            new Point2I(1, 1),
            new Point2I(0, 1),
            new Point2I(-1, 1),
            new Point2I(-1, 0),
            new Point2I(-1, -1),
    };

    private Map<Endpoints, Point2I> dijkstraNextSubtile = new HashMap<Endpoints, Point2I>();

    private void dijkstra(Point2I start, Point2I end) {
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
            for (Point2I dxy : DXY) {
                Point2I nextVertex = new Point2I(vertex.x + dxy.x, vertex.y + dxy.y);
                if (0 <= nextVertex.x && nextVertex.x < subtilesXY.length
                        && 0 <= nextVertex.y && nextVertex.y < subtilesXY[nextVertex.x].length
                        && subtilesXY[nextVertex.x][nextVertex.y] != SubtileType.WALL
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

        Point2I vertex = end;
        do {
            Point2I prevVertex = prev.get(vertex);
            dijkstraNextSubtile.put(new Endpoints(prevVertex, end), vertex);
            vertex = prevVertex;
        } while (!vertex.equals(start));
    }

    private Point2I getNextSubtile(Point2I position, Point2I target) {
        Endpoints endpoints = new Endpoints(position, target);
        Point2I result = dijkstraNextSubtile.get(endpoints);
        if (result == null) {
            dijkstra(position, target);
            result = dijkstraNextSubtile.get(endpoints);
        }
        return result;
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

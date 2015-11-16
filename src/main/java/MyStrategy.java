import model.*;
import java.util.*;

import static java.lang.StrictMath.*;

public final class MyStrategy implements Strategy {
    private Car self;
    private World world;
    private Game game;

    private Point2I nextWP;
    private Point2I nextWPSubtile;

    @Override
    public void move(Car self, World world, Game game, Move move) {
        updateFields(self, world, game);

        Point2I nextSubtile = getNextSubtile(toSubtilePoint(self));
        double nextX = (nextSubtile.x + 0.5D) * getSubtileSize();
        double nextY = (nextSubtile.y + 0.5D) * getSubtileSize();
        double angleToWaypoint = self.getAngleTo(nextX, nextY);

        //for (Bonus bonus : world.getBonuses()) {
        //    if (self.getDistanceTo(bonus) < game.getTrackTileSize() * 2D
        //            && abs(angleToWaypoint - self.getAngleTo(bonus)) < PI / 8.0D) {
        //        nextX = bonus.getX();
        //        nextWaypointY = bonus.getY();
        //        break;
        //    }
        //}

        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());

        boolean isReverse = self.getEnginePower() < 0;

        if (isReverse) {
            move.setWheelTurn(-32.0D / PI * angleToWaypoint);
        } else {
            move.setWheelTurn(32.0D / PI * angleToWaypoint);
        }

        if (abs(angleToWaypoint) > PI / 6 && (speedModule < 3D || isReverse)) {
            move.setEnginePower(-1.0D);
        } else {
            move.setEnginePower(1.0D);

            if (abs(angleToWaypoint) > PI / 4.0D && speedModule > 2D) {
                move.setBrake(true);
            }
        }

        if (abs(self.getSpeedX()) > 0.1D && abs(self.getSpeedY()) > 0.1D) {
            if (self.getProjectileCount() > 0) {
                for (Car enemy : world.getCars()) {
                    if (enemy.isTeammate()) {
                        continue;
                    }
                    if (self.getDistanceTo(enemy) < game.getTrackTileSize() * 3) {
                        if (abs(self.getAngleTo(enemy)) < game.getSideWasherAngle()) {
                            move.setThrowProjectile(true);
                            break;
                        }
                    }
                }
            }

            if (self.getOilCanisterCount() > 0) {
                for (Car enemy : world.getCars()) {
                    if (enemy.isTeammate()) {
                        continue;
                    }
                    if (self.getDistanceTo(enemy) < game.getTrackTileSize() * 3 &&
                            self.getDistanceTo(enemy) > game.getCarHeight() * 1.5D) {
                        if (self.getAngleTo(enemy) < -PI / 6.0D || self.getAngleTo(enemy) > PI - PI / 6.0D) {
                            move.setSpillOil(true);
                            break;
                        }
                    }
                }
            }
        }

        if (self.getNitroChargeCount() > 0) {
            if (abs(angleToWaypoint) < PI / 10.0D) {
                if (world.getTick() > game.getInitialFreezeDurationTicks()) {
                    move.setUseNitro(true);
                }
            }
        }
    }

    private void updateFields(Car self, World world, Game game) {
        this.self = self;
        this.world = world;
        this.game = game;
        if (subtilesXY == null) {
            createSubtiles();
        }
        setNextWP(self.getNextWaypointX(), self.getNextWaypointY());
    }

    private void setNextWP(int x, int y) {
        nextWP = new Point2I(x, y);
        nextWPSubtile = new Point2I(x * SUBTILE_COUNT + SUBTILE_COUNT / 2,
                                    y * SUBTILE_COUNT + SUBTILE_COUNT / 2);
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

    private void createSubtiles() {
        subtilesXY = new SubtileType[world.getWidth() * SUBTILE_COUNT][world.getHeight() * SUBTILE_COUNT];
        for (int tile_x = 0; tile_x < world.getWidth(); ++tile_x) {
            for (int i = 0; i < SUBTILE_COUNT; ++i) {
                int subtile_x = tile_x * SUBTILE_COUNT + i;
                for (int tile_y = 0; tile_y < world.getWidth(); ++tile_y) {
                    for (int j = 0; j < SUBTILE_COUNT; ++j) {
                        int subtile_y = tile_y * SUBTILE_COUNT + j;
                        SubtileType subtileType = SubtileType.ROAD;
                        switch (world.getTilesXY()[tile_x][tile_y]) {
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
                            default:
                        }
                        subtilesXY[subtile_x][subtile_y] = subtileType;
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

    private Map<Endpoints, Point2I> bfsNextSubtile = new HashMap<Endpoints, Point2I>();

    private void bfs(Point2I start, Point2I end) {
        Queue<Point2I> queue = new LinkedList<Point2I>();
        Map<Point2I, Point2I> prev = new HashMap<Point2I, Point2I>();
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
                    prev.put(nextVertex, vertex);
                    queue.add(nextVertex);
                }
            }
        }

        Point2I vertex = end;
        do {
            Point2I prevVertex = prev.get(vertex);
            bfsNextSubtile.put(new Endpoints(prevVertex, end), vertex);
            vertex = prevVertex;
        } while (!vertex.equals(start));
    }

    private Point2I getNextSubtile(Point2I position) {
        Endpoints endpoints = new Endpoints(position, nextWPSubtile);
        Point2I result = bfsNextSubtile.get(endpoints);
        if (result == null) {
            bfs(position, nextWPSubtile);
            result = bfsNextSubtile.get(endpoints);
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

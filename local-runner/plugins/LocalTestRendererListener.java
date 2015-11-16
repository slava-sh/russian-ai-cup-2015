import model.*;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;

import java.lang.Override;
import java.lang.String;
import java.util.*;
import java.util.LinkedList;

import static java.lang.StrictMath.*;

public final class LocalTestRendererListener {
    private static final int FONT_SIZE_BIG = 42;
    private static final int FONT_SIZE_SMALL = 18;

    private static int currentWPId = -1;

    public void beforeDrawScene(Graphics graphics, World world, Game game, int canvasWidth, int canvasHeight,
                                double left, double top, double width, double height) {
        updateFields(graphics, world, game, canvasWidth, canvasHeight, left, top, width, height);

        double trackTileSize = game.getTrackTileSize();
        double nOffset = 60.0D;

        for (int[] waypoint : world.getWaypoints()) {
            double x = waypoint[0] * trackTileSize + 100.0D;
            double y = waypoint[1] * trackTileSize + 100.0D;

            setColor(new Color(252, 255, 127));
            fillRect(x, y, trackTileSize - 200.0D, trackTileSize - 200.0D);
        }

        setColor(new Color(75, 255, 63));
        fillRect(nextWP.x * trackTileSize + 100.0D, nextWP.y * trackTileSize + 100.0D, trackTileSize - 200.0D, trackTileSize - 200.0D);

        //drawSubtileGrid();

        int nextWPId = 1;
        for (int[] waypoint : world.getWaypoints()) {
            if (world.getMapName().equals("map01") && currentWPId > 6 && nextWP.x == 3 && nextWP.y == 4) {
                currentWPId = 11;
                break;
            }
            if (nextWP.x == waypoint[0] && nextWP.y == waypoint[1]) {
                currentWPId = nextWPId - 1;
                break;
            }
            nextWPId = nextWPId + 1;
        }

        nextWPId = 1;
        setColor(Color.BLACK);
        for (int[] waypoint : world.getWaypoints()) {
            double x = waypoint[0] * trackTileSize + 320.0D;
            double y = waypoint[1] * trackTileSize + 490.0D;
            if (nextWPId >= 10) {
                x = x - nOffset;
            }

            if (world.getMapName().equals("map01")) {
                if (nextWPId == 5 && (currentWPId >= 0 && currentWPId < 6)) {
                    drawString(nextWPId + "", FONT_SIZE_BIG, x, y);
                    drawString("12", FONT_SIZE_SMALL, x + 220.0D, y + 200.0D);
                } else if (nextWPId == 12 && currentWPId >= 6) {
                    drawString(nextWPId + "", FONT_SIZE_BIG, x, y);
                    drawString("5", FONT_SIZE_SMALL, x - 170.0D, y + 200.0D);
                } else if (nextWPId != 5 && nextWPId != 12) {
                    drawString(nextWPId + "", FONT_SIZE_BIG, x, y);
                }
            } else {
                drawString(nextWPId + "", FONT_SIZE_BIG, x, y);
            }

            nextWPId = nextWPId + 1;
        }

        //countSubtiles();
        renderBfs();

        setColor(Color.BLACK);
    }

    public void afterDrawScene(Graphics graphics, World world, Game game, int canvasWidth, int canvasHeight,
                               double left, double top, double width, double height) {
        updateFields(graphics, world, game, canvasWidth, canvasHeight, left, top, width, height);

        //drawTrajectory();

        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());
        setColor(Color.BLACK);
        drawString(String.format("%2.0f", speedModule) + "", FONT_SIZE_SMALL, self.getX(), self.getY());

        setColor(Color.BLACK);
    }

    private void countSubtiles() {
        int subtileSum = 0;
        setColor(new Color(240, 240, 240));
        for (int i = 0; i < world.getWidth(); ++i) {
            for (int j = 0; j < world.getWidth(); ++j) {
                if (world.getTilesXY()[i][j] != TileType.EMPTY) {
                    subtileSum += SUBTILE_COUNT * SUBTILE_COUNT;
                }
            }
        }
        setColor(Color.BLACK);
        drawString("" + subtileSum, FONT_SIZE_BIG, game.getTrackTileSize(), game.getTrackTileSize());
    }

    private LinkedList<Point2I> realTrajectory = new LinkedList<Point2I>();
    private LinkedList<Point2I> predictedTrajectory = new LinkedList<Point2I>();

    private void drawTrajectory() {
        double TIME = 50;

        if (world.getTick() % TIME == 0) {
            realTrajectory.add(new Point2I(self.getX(), self.getY()));
            predictedTrajectory.add(new Point2I(self.getX() + self.getSpeedX() * TIME, self.getY() + self.getSpeedY() * TIME));
        }

        for (Point2I point : predictedTrajectory) {
            setColor(Color.CYAN);
            fillCircle(point.x, point.y, game.getWasherRadius());
        }
        for (Point2I point : realTrajectory) {
            setColor(Color.RED);
            fillCircle(point.x, point.y, game.getWasherRadius());
        }
    }

    private void renderBfs() {
        Point2I subtile = new Point2I(toSubtileCoordinate(self.getX()), toSubtileCoordinate(self.getY()));
        int subtileI = 0;
        do {
            if (subtileI == 2) {
                setColor(Color.PINK);
                fillSubtile(subtile);
            }
            setColor(Color.RED);
            drawSubtile(subtile);
            subtile = getNextSubtile(subtile);
            ++subtileI;
        } while (!subtile.equals(nextWPSubtile));
    }

    private void fillWallSubtiles() {
        for (int x = 0; x < subtilesXY.length; ++x) {
            for (int y = 0; y < subtilesXY[x].length; ++y) {
                if (0 <= x && x < subtilesXY.length
                        && 0 <= y && y < subtilesXY[x].length) {
                    if (subtilesXY[x][y] == SubtileType.WALL) {
                        setColor(Color.PINK);
                        fillSubtile(x, y);
                    }
                }
            }
        }
    }

    private void drawSubtileGrid() {
        setColor(new Color(240, 240, 240));
        for (int i = 0; i < world.getWidth(); ++i) {
            for (int j = 0; j < world.getWidth(); ++j) {
                if (world.getTilesXY()[i][j] != TileType.EMPTY) {
                    double minX = i * game.getTrackTileSize();
                    double maxX = (i + 1) * game.getTrackTileSize();
                    double minY = j * game.getTrackTileSize();
                    double maxY = (j + 1) * game.getTrackTileSize();
                    for (double x = minX; x < maxX; x += getSubtileSize()) {
                        drawLine(x, minY, x, maxY);
                    }
                    for (double y = minY; y < maxY; y += getSubtileSize()) {
                        drawLine(minX, y, maxX, y);
                    }
                }
            }
        }
    }

    private void drawVector(Unit unit, Vector2D vector) {
        drawLine(unit.getX(), unit.getY(), unit.getX() + vector.getX(), unit.getY() + vector.getY());
    }

    private void setColor(Color c) {
        graphics.setColor(c);
    }

    private void drawString(String text, int fontSize, double x1, double y1) {
        Point2I stringBegin = toCanvasPosition(x1, y1);
        graphics.setFont(new Font("Serif", Font.PLAIN, fontSize));
        graphics.drawString(text, stringBegin.getX(), stringBegin.getY());
    }

    private void drawLine(double x1, double y1, double x2, double y2) {
        Point2I lineBegin = toCanvasPosition(x1, y1);
        Point2I lineEnd = toCanvasPosition(x2, y2);
        graphics.drawLine(lineBegin.getX(), lineBegin.getY(), lineEnd.getX(), lineEnd.getY());
    }

    private void fillSubtile(Point2I p) {
        fillSubtile(p.getX(), p.getY());
    }

    private void fillSubtile(int x, int y) {
        fillRect(x * getSubtileSize(), y * getSubtileSize(), getSubtileSize(), getSubtileSize());
    }

    private void drawSubtile(Point2I p) {
        drawSubtile(p.getX(), p.getY());
    }

    private void drawSubtile(int x, int y) {
        drawRect(x * getSubtileSize(), y * getSubtileSize(), getSubtileSize(), getSubtileSize());
    }

    private void fillCircle(double centerX, double centerY, double radius) {
        Point2I topLeft = toCanvasPosition(centerX - radius, centerY - radius);
        Point2I size = toCanvasOffset(2.0D * radius, 2.0D * radius);

        graphics.fillOval(topLeft.getX(), topLeft.getY(), size.getX(), size.getY());
    }

    private void drawCircle(double centerX, double centerY, double radius) {
        Point2I topLeft = toCanvasPosition(centerX - radius, centerY - radius);
        Point2I size = toCanvasOffset(2.0D * radius, 2.0D * radius);

        graphics.drawOval(topLeft.getX(), topLeft.getY(), size.getX(), size.getY());
    }

    private void fillArc(double centerX, double centerY, double radius, int startAngle, int arcAngle) {
        Point2I topLeft = toCanvasPosition(centerX - radius, centerY - radius);
        Point2I size = toCanvasOffset(2.0D * radius, 2.0D * radius);

        graphics.fillArc(topLeft.getX(), topLeft.getY(), size.getX(), size.getY(), startAngle, arcAngle);
    }

    private void drawArc(double centerX, double centerY, double radius, int startAngle, int arcAngle) {
        Point2I topLeft = toCanvasPosition(centerX - radius, centerY - radius);
        Point2I size = toCanvasOffset(2.0D * radius, 2.0D * radius);

        graphics.drawArc(topLeft.getX(), topLeft.getY(), size.getX(), size.getY(), startAngle, arcAngle);
    }

    private void fillRect(double left, double top, double width, double height) {
        Point2I topLeft = toCanvasPosition(left, top);
        Point2I size = toCanvasOffset(width, height);

        graphics.fillRect(topLeft.getX(), topLeft.getY(), size.getX(), size.getY());
    }

    private void drawRect(double left, double top, double width, double height) {
        Point2I topLeft = toCanvasPosition(left, top);
        Point2I size = toCanvasOffset(width, height);

        graphics.drawRect(topLeft.getX(), topLeft.getY(), size.getX(), size.getY());
    }

    private void drawPolygon(Point2D... points) {
        int pointCount = points.length;

        for (int pointIndex = 1; pointIndex < pointCount; ++pointIndex) {
            Point2D pointA = points[pointIndex];
            Point2D pointB = points[pointIndex - 1];
            drawLine(pointA.getX(), pointA.getY(), pointB.getX(), pointB.getY());
        }

        Point2D pointA = points[0];
        Point2D pointB = points[pointCount - 1];
        drawLine(pointA.getX(), pointA.getY(), pointB.getX(), pointB.getY());
    }

    private Point2I toCanvasOffset(double x, double y) {
        return new Point2I(x * canvasWidth / width, y * canvasHeight / height);
    }

    private Point2I toCanvasPosition(double x, double y) {
        return new Point2I((x - left) * canvasWidth / width, (y - top) * canvasHeight / height);
    }

    private Graphics graphics;
    private World world;
    private Game game;

    private int canvasWidth;
    private int canvasHeight;

    private double left;
    private double top;
    private double width;
    private double height;

    private long myId = -1;
    private Car self;

    private void updateFields(Graphics graphics, World world, Game game, int canvasWidth, int canvasHeight,
                              double left, double top, double width, double height) {
        this.graphics = graphics;
        this.world = world;
        this.game = game;

        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;

        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;

        if (myId == -1) {
            for (Player player : world.getPlayers()) {
                if (player.getName().equals("MyStrategy")) {
                    myId = player.getId();
                }
            }
        }

        for (Car car : world.getCars()) {
            if (car.getPlayerId() == myId) {
                this.self = car;
            }
        }

        if (subtilesXY == null) {
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
        dist.put(start, 0D);
        Queue<Point2I> queue = new PriorityQueue<Point2I>(new Comparator<Point2I>() {
            @Override
            public int compare(Point2I a, Point2I b) {
                return Double.compare(dist.get(a), dist.get(b));
            }
        });
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

    private Point2I getNextSubtile(Point2I position) {
        Endpoints endpoints = new Endpoints(position, nextWPSubtile);
        Point2I result = dijkstraNextSubtile.get(endpoints);
        if (result == null) {
            dijkstra(position, nextWPSubtile);
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
        if (abs((double) intValue - value) < 1.0D) {
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

class Point2D {
    private double x;
    private double y;

    private Point2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    private Point2D() {
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

/*
 * Based on com.codeforces.commons.geometry.Vector2D
 */
class Vector2D {
    public static final double DEFAULT_EPSILON = 1.0E-6D;

    private double x;
    private double y;

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2D(double x1, double y1, double x2, double y2) {
        this(x2 - x1, y2 - y1);
    }

    public Vector2D(Vector2D other) {
        this(other.getX(), other.getY());
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

    public Vector2D add(Vector2D other) {
        setX(getX() + other.getX());
        setY(getY() + other.getY());
        return this;
    }

    public Vector2D subtract(Vector2D other) {
        setX(getX() - other.getX());
        setY(getY() - other.getY());
        return this;
    }

    public Vector2D multiply(double factor) {
        setX(factor * getX());
        setY(factor * getY());
        return this;
    }

    public Vector2D rotate(double angle) {
        double cos = cos(angle);
        double sin = sin(angle);
        double x = getX();
        double y = getY();
        setX(x * cos - y * sin);
        setY(x * sin + y * cos);
        return this;
    }

    public double dotProduct(Vector2D other) {
        return getX() * other.getX() + getY() * other.getY();
    }

    public Vector2D negate() {
        setX(-getX());
        setY(-getY());
        return this;
    }

    public Vector2D normalize() {
        double length = getLength();
        if (length == 0.0D) {
            throw new IllegalStateException("Can't set angle of zero-width vector.");
        }
        setX(getX() / length);
        setY(getY() / length);
        return this;
    }

    public double getAngle() {
        return atan2(getY(), getX());
    }

    public Vector2D setAngle(double angle) {
        double length = getLength();
        if (length == 0.0D) {
            throw new IllegalStateException("Can't set angle of zero-width vector.");
        }
        setX(cos(angle) * length);
        setY(sin(angle) * length);
        return this;
    }

    public double getAngle(Vector2D other) {
        double dot = getX() * other.getX() + getY() * other.getY();
        double cross = getX() * other.getY() - getY() * other.getX();
        return atan2(cross, dot);
    }

    public double getLength() {
        return hypot(getX(), getY());
    }

    public Vector2D setLength(double length) {
        double currentLength = getLength();
        if (currentLength == 0.0D) {
            throw new IllegalStateException("Can't resize zero-width vector.");
        }
        return multiply(length / currentLength);
    }

    public double getSquaredLength() {
        return getX() * getX() + getY() * getY();
    }

    public Vector2D setSquaredLength(double squaredLength) {
        double currentSquaredLength = getSquaredLength();
        if (currentSquaredLength == 0.0D) {
            throw new IllegalStateException("Can't resize zero-width vector.");
        }
        return multiply(sqrt(squaredLength / currentSquaredLength));
    }

    public Vector2D copy() {
        return new Vector2D(this);
    }

    public Vector2D copyNegate() {
        return new Vector2D(-getX(), -getY());
    }

    public boolean nearlyEquals(Vector2D other, double epsilon) {
        return other != null
                && abs(getX() - other.getX()) < epsilon
                && abs(getY() - other.getY()) < epsilon;
    }

    public boolean nearlyEquals(Vector2D other) {
        return nearlyEquals(other, DEFAULT_EPSILON);
    }

    public Vector2D getPerp() {
        return new Vector2D(-getY(), getX());
    }
}

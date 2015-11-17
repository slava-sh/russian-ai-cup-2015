import model.*;
import model.TileType;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;

import java.lang.Override;
import java.lang.String;
import java.util.*;
import java.util.LinkedList;
import java.util.List;

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

        drawSubtileGrid();

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

        renderTileDijkstra();
        renderSubtileDijkstra();

        setColor(Color.BLACK);
    }

    public void afterDrawScene(Graphics graphics, World world, Game game, int canvasWidth, int canvasHeight,
                               double left, double top, double width, double height) {
        updateFields(graphics, world, game, canvasWidth, canvasHeight, left, top, width, height);

        drawTrajectory();

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

    private LinkedList<Point2D> realTrajectory = new LinkedList<Point2D>();
    private LinkedList<Point2D> predictedTrajectory = new LinkedList<Point2D>();

    private void drawTrajectory() {
        double TIME = 5;

        if (world.getTick() % TIME == 0) {
            realTrajectory.add(new Point2D(self.getX(), self.getY()));
            //predictedTrajectory.add(postition);
        }

        for (Point2D point : predictedTrajectory) {
            setColor(Color.CYAN);
            fillCircle(point.getX(), point.getY(), game.getWasherRadius());
        }
        for (Point2D point : realTrajectory) {
            setColor(Color.RED);
            fillCircle(point.getX(), point.getY(), game.getWasherRadius());
        }
    }

    private void renderTileDijkstra() {
        setColor(Color.BLUE);
        drawTile(toTilePoint(self));
        int tileI = 0;
        for (Point2I tile : getNextTiles(3)) {
            ++tileI;
            setColor(Color.GREEN);
            drawTile(tile);
            setColor(Color.BLACK);
            drawString("t" + tileI, FONT_SIZE_BIG, tile.getX() * game.getTrackTileSize() + 0.5 * game.getTrackTileSize(), tile.getY() * game.getTrackTileSize() + 0.5 * game.getTrackTileSize());
        }
    }

    private void renderSubtileDijkstra() {
        int subtileI = 0;
        for (Point2I subtile : getNextSubtiles()) {
            ++subtileI;
            if (false) {
                setColor(Color.PINK);
                fillSubtile(subtile);
            }
            setColor(Color.RED);
            fillSubtile(subtile);
        }
    }

    private Point2I getNextWPSubtile() {
        return getNextWPSubtile(0);
    }

    private Point2I getNextWPSubtile(int skip) {
        int[] nextWPArray = world.getWaypoints()[(self.getNextWaypointIndex() + skip) % world.getWaypoints().length];
        return new Point2I(nextWPArray[0] * SUBTILE_COUNT + SUBTILE_COUNT / 2,
                           nextWPArray[1] * SUBTILE_COUNT + SUBTILE_COUNT / 2);
    }

    private Point2I getNextWP() {
        return getNextWP(0);
    }

    private Point2I getNextWP(int skip) {
        int[] nextWPArray = world.getWaypoints()[(self.getNextWaypointIndex() + skip) % world.getWaypoints().length];
        return new Point2I(nextWPArray[0], nextWPArray[1]);
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

    private void fillTile(Point2I p) {
        fillTile(p.getX(), p.getY());
    }

    private void fillTile(int x, int y) {
        fillRect(x * game.getTrackTileSize(), y * game.getTrackTileSize(), game.getTrackTileSize(), game.getTrackTileSize());
    }

    private void drawTile(Point2I p) {
        drawTile(p.getX(), p.getY());
    }

    private void drawTile(int x, int y) {
        drawRect(x * game.getTrackTileSize(), y * game.getTrackTileSize(), game.getTrackTileSize(), game.getTrackTileSize());
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

        setNextWP(self.getNextWaypointX(), self.getNextWaypointY());
    }

    private Point2I nextWP;
    private Point2I nextWPSubtile;

    private void setNextWP(int x, int y) {
        nextWP = new Point2I(x, y);
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

    private Point2I subtileToTile(Point2I subtile) {
        return new Point2I(subtile.x / SUBTILE_COUNT, subtile.y / SUBTILE_COUNT);
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

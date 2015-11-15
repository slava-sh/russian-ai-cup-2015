import model.*;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;

import java.lang.Override;
import java.util.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import java.io.*;

import static java.lang.StrictMath.*;

public final class LocalTestRendererListener {
    private Graphics graphics;
    private World world;
    private Game game;

    private int canvasWidth;
    private int canvasHeight;

    private double left;
    private double top;
    private double width;
    private double height;

    private static final int FONT_SIZE_BIG = 42;
    private static final int FONT_SIZE_SMALL = 18;

    private static int currentWPId = -1;
    private static long myId = -1;

    enum SubtileType {WALL, ROAD}

    ;

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

    private SubtileType[][] subtilesXY;

    public void beforeDrawScene(Graphics graphics, World world, Game game, int canvasWidth, int canvasHeight,
                                double left, double top, double width, double height) {
        updateFields(graphics, world, game, canvasWidth, canvasHeight, left, top, width, height);

        if (subtilesXY == null) {
            createSubtiles();
        }

        double trackTileSize = game.getTrackTileSize();
        Point2I nextWP = new Point2I();
        int nextWPId = 1;
        double nOffset = 60.0D;

        if (myId == -1) {
            for (Player player : world.getPlayers()) {
                if (player.getName().equals("MyStrategy")) {
                    myId = player.getId();
                }
            }
        }

        for (int[] waypoint : world.getWaypoints()) {
            double x = waypoint[0] * trackTileSize + 100.0D;
            double y = waypoint[1] * trackTileSize + 100.0D;

            setColor(new Color(252, 255, 127));
            fillRect(x, y, trackTileSize - 200.0D, trackTileSize - 200.0D);
        }

        for (Car car : world.getCars()) {
            if (car.getPlayerId() == myId) {
                nextWP.setX(car.getNextWaypointX());
                nextWP.setY(car.getNextWaypointY());
                double x = nextWP.getX() * trackTileSize + 100.0D;
                double y = nextWP.getY() * trackTileSize + 100.0D;

                setColor(new Color(75, 255, 63));
                fillRect(x, y, trackTileSize - 200.0D, trackTileSize - 200.0D);
            }
        }

        drawSubtileGrid();

        for (int[] waypoint : world.getWaypoints()) {
            if (world.getMapName().equals("map01") && currentWPId > 6 && nextWP.getX() == 3 && nextWP.getY() == 4) {
                currentWPId = 11;
                break;
            }
            if (nextWP.getX() == waypoint[0] && nextWP.getY() == waypoint[1]) {
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

        for (Car car : world.getCars()) {
            if (car.getPlayerId() == myId) {
                int subtile_x = toSubtileCoordinate(car.getX());
                int subtile_y = toSubtileCoordinate(car.getY());
                for (int dx = -SUBTILE_COUNT * 10; dx <= SUBTILE_COUNT * 10; ++dx) {
                    for (int dy = -SUBTILE_COUNT * 10; dy <= SUBTILE_COUNT * 10; ++dy) {
                        int x = subtile_x + dx;
                        int y = subtile_x + dy;
                        if (0 <= x && x < subtilesXY.length && 0 <= y && y < subtilesXY[x].length) {
                            if (subtilesXY[x][y] == SubtileType.ROAD) {
                                setColor(Color.YELLOW);
                            } else {
                                setColor(Color.PINK);
                                fillRect(x * getSubtileSize(), y * getSubtileSize(), getSubtileSize(), getSubtileSize());
                            }
                        }
                    }
                }

                Point2I target = new Point2I(nextWP.x * SUBTILE_COUNT + SUBTILE_COUNT / 2,
                        nextWP.y * SUBTILE_COUNT + SUBTILE_COUNT / 2);
                drawString("qqqq", FONT_SIZE_SMALL, car.getX(), car.getY() + 150);
                List<Point2I> path = bfs(new Point2I(subtile_x, subtile_y), target);
                setColor(Color.BLACK);
                drawString("" + path.size(), FONT_SIZE_SMALL, car.getX(), car.getY() + 100);
                drawString("eeee", FONT_SIZE_SMALL, car.getX(), car.getY() + 150);
                for (Point2I subtile : path) {
                    setColor(Color.RED);
                    fillRect(subtile.x * getSubtileSize(), subtile.y * getSubtileSize(), getSubtileSize(), getSubtileSize());
                }
            }
        }

        setColor(Color.BLACK);
    }

    public void afterDrawScene(Graphics graphics, World world, Game game, int canvasWidth, int canvasHeight,
                               double left, double top, double width, double height) {
        updateFields(graphics, world, game, canvasWidth, canvasHeight, left, top, width, height);

        for (Car car : world.getCars()) {
            if (car.getPlayerId() == myId) {
                double speedModule = hypot(car.getSpeedX(), car.getSpeedY());
                //setColor(Color.WHITE);
                //fillRect(car.getX() - 50.0D, car.getY() - 50.0D, 200.0D, 100.0D);
                setColor(Color.BLACK);
                drawString(String.format("%2.0f", speedModule) + "", FONT_SIZE_SMALL, car.getX(), car.getY());
            }
        }

        setColor(Color.BLACK);
    }

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
                            default:
                        }
                        subtilesXY[subtile_x][subtile_y] = subtileType;
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

    private int toSubtileCoordinate(double coordinate) {
        return (int) (coordinate / getSubtileSize());
    }

    private double getSubtileSize() {
        return game.getTrackTileSize() / SUBTILE_COUNT;
    }

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

    private List<Point2I> bfs(Point2I start, Point2I end) {
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

        List<Point2I> path = new LinkedList<Point2I>();
        Point2I vertex = end;
        do {
            path.add(vertex);
            vertex = prev.get(vertex);
        } while (!vertex.equals(start));
        Collections.reverse(path);
        return path;
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

    private static final class Point2I {
        private int x;
        private int y;

        private Point2I(double x, double y) {
            this.x = toInt(round(x));
            this.y = toInt(round(y));
        }

        private Point2I(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private Point2I() {
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        private static int toInt(double value) {
            @SuppressWarnings("NumericCastThatLosesPrecision") int intValue = (int) value;
            if (abs((double) intValue - value) < 1.0D) {
                return intValue;
            }
            throw new IllegalArgumentException("Can't convert double " + value + " to int.");
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

    private static final class Point2D {
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
}

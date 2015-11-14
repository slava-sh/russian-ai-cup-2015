import model.Car;
import model.Game;
import model.Move;
import model.World;

import static java.lang.StrictMath.*;

public final class MyStrategy implements Strategy {
    @Override
    public void move(Car self, World world, Game game, Move move) {
        double nextWaypointX = (self.getNextWaypointX() + 0.5D) * game.getTrackTileSize();
        double nextWaypointY = (self.getNextWaypointY() + 0.5D) * game.getTrackTileSize();

        double cornerTileOffset = 0.25D * game.getTrackTileSize();

        switch (world.getTilesXY()[self.getNextWaypointX()][self.getNextWaypointY()]) {
            case LEFT_TOP_CORNER:
                nextWaypointX += cornerTileOffset;
                nextWaypointY += cornerTileOffset;
                break;
            case RIGHT_TOP_CORNER:
                nextWaypointX -= cornerTileOffset;
                nextWaypointY += cornerTileOffset;
                break;
            case LEFT_BOTTOM_CORNER:
                nextWaypointX += cornerTileOffset;
                nextWaypointY -= cornerTileOffset;
                break;
            case RIGHT_BOTTOM_CORNER:
                nextWaypointX -= cornerTileOffset;
                nextWaypointY -= cornerTileOffset;
                break;
            default:
        }

        double angleToWaypoint = self.getAngleTo(nextWaypointX, nextWaypointY);
        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());

        move.setWheelTurn(angleToWaypoint * 32.0D / PI);
        move.setEnginePower(1.0D); // 0.75D

        //if (speedModule * speedModule * abs(angleToWaypoint) > 2.5D * 2.5D * PI) {
        //    move.setBrake(true);
        //}

        if (self.getProjectileCount() > 0) {
            move.setThrowProjectile(true);
        }

        if (self.getOilCanisterCount() > 0) {
            move.setSpillOil(true);
        }

        if (self.getNitroChargeCount() > 0) {
            if (abs(angleToWaypoint) < PI / 10.0D) {
                move.setUseNitro(true);
            }
        }
    }
}

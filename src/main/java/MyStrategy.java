import model.*;

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

        for (Bonus bonus : world.getBonuses()) {
            if (self.getDistanceTo(bonus) < game.getTrackTileSize() * 2D
                    && abs(angleToWaypoint - self.getAngleTo(bonus)) < PI / 8.0D) {
                nextWaypointX = bonus.getX();
                nextWaypointY = bonus.getY();
                break;
            }
        }

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
}

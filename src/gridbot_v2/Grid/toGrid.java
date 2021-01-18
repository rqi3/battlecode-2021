package gridbot_v2.Grid;


import battlecode.common.*;
import gridbot_v2.RobotPlayer;
import gridbot_v2.common.Communication;
import gridbot_v2.common.Displacement;
import gridbot_v2.common.Sensing;
import gridbot_v2.common.localPathfinding;

public class toGrid {
    static RobotController rc;

    public static void init()
    {
        rc = RobotPlayer.rc;
    }
    public static void run() throws GameActionException {

        //System.out.println("im trying to reach a grid point");

        {
            Communication.gridBotInfo nearestEmptySlot = null;
            int nearestEmptySlotDistance = 1000;
            for (Communication.gridBotInfo grid : Sensing.nearByGrid) {
                if ((!grid.haveGridBot) && Displacement.getMaxXYDistance(Sensing.currentLocation, grid.location) < nearestEmptySlotDistance) {
                    nearestEmptySlot = grid;
                    nearestEmptySlotDistance = Displacement.getMaxXYDistance(Sensing.currentLocation, grid.location);
                }
            }
            if (nearestEmptySlot != null) {
                localPathfinding.pathFindTo(
                        Displacement.subtract(
                                nearestEmptySlot.location,
                                Sensing.currentLocation
                        )
                );
                return;
            }
        }
        {
            Communication.gridBotInfo nearestMessageGroup0 = null;
            int nearestMessageGroup0Distance = 1000;
            for (Communication.gridBotInfo grid : Sensing.nearByGrid) {
                if (grid.haveGridBot&&grid.message.haveMessageGroup0 && Displacement.getMaxXYDistance(Sensing.currentLocation, grid.location) < nearestMessageGroup0Distance) {
                    nearestMessageGroup0 = grid;
                    nearestMessageGroup0Distance = Displacement.getMaxXYDistance(Sensing.currentLocation, grid.location);
                }
            }
            if (nearestMessageGroup0 != null) {
                localPathfinding.pathFindTo(
                        Displacement.subtract(
                                Displacement.add(nearestMessageGroup0.location,Displacement.scale(nearestMessageGroup0.message.emptyGridDirection,3)),
                                Sensing.currentLocation
                        )
                );
                return;
            }
        }

    }
}


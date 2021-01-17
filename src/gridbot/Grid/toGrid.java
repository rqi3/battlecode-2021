package gridbot.Grid;


import battlecode.common.*;
import gridbot.RobotPlayer;
import gridbot.common.Communication;
import gridbot.common.Displacement;
import gridbot.common.Sensing;
import gridbot.common.localPathfinding;

public class toGrid {
    static RobotController rc;

    public static void run() throws GameActionException {
        rc = RobotPlayer.rc;
        System.out.println("im trying to reach a grid point");
        Communication.gridInfo gi=Sensing.nearByGrid[2][2];
        if(gi.haveSensed)
        {
            if(gi.haveGridBot)
            {
                localPathfinding.pathFindTo(
                        Displacement.subtract(
                                Displacement.add(Displacement.getGrid(Sensing.currentLocation),Displacement.scale(gi.emptyGridDirection,3)),
                                Sensing.currentLocation
                        )
                );
                return;
            }
            else
            {
                localPathfinding.pathFindTo(
                        Displacement.subtract(
                                Displacement.getGrid(Sensing.currentLocation),
                                Sensing.currentLocation
                        )
                );
                return;
            }
        }
        Displacement whereGO=Displacement.CENTER;
        for(Displacement dir:Displacement.DirectionsPerpendicular)
        {
            if(!rc.onTheMap(Displacement.add(Sensing.currentLocation,dir)))
                whereGO=Displacement.subtract(whereGO,dir);
        }
        localPathfinding.pathFindTo(whereGO);

    }
}


package gridbot_v2.common;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import gridbot_v2.RobotPlayer;

public class localPathfinding {
    static RobotController rc;


    public static void init()
    {
        rc = RobotPlayer.rc;
    }
    public static void pathFindTo(Displacement target) throws GameActionException
    {//TODO: better pathfinding


        System.out.println("im trying to reach: "+target);
        MapLocation targetLocation=Displacement.add(Sensing.currentLocation,target);
        Displacement best_direction = Displacement.CENTER;
        int lowestDistance= Displacement.getMaxXYDistance(Sensing.currentLocation,targetLocation);


        for(Displacement dir: Displacement.DirectionsWithoutCenter){
            if(rc.canMove(Displacement.displacementToDirection(dir))){
                MapLocation newLocation=Displacement.add(Sensing.currentLocation,dir);

                int newDistance = Displacement.getMaxXYDistance(newLocation, targetLocation);
                if(newDistance <= lowestDistance){
                    best_direction = dir;
                    lowestDistance = newDistance;
                }
            }
        }
        if(best_direction!=Displacement.CENTER){
            rc.move(Displacement.displacementToDirection(best_direction));
        }
    }

}

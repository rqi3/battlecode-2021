package pathfindingtest.common;

import battlecode.common.*;
import pathfindingtest.RobotPlayer;

import java.util.PriorityQueue;

public class localPathfinding {
    static RobotController rc;

    static double TotalInverseTerrain=6;
    static double averageInverseTerrain=2;
    static int numberCellSeen=3;
    public static void init()
    {
        rc = RobotPlayer.rc;

    }
    public static void moveToNaive(Displacement target) throws GameActionException {
        System.out.println("moveToNaive: " + target);
        //assert(nathanSpawner.RobotPlayer.has_parent_EC); //destination was not defined if this has no parent_EC
        MapLocation current_loc = rc.getLocation();
        MapLocation destination =Displacement.add(current_loc,target);

        Displacement best_direction = Displacement.CENTER;
        int lowestMaxXYDistance = Displacement.getMaxXYDistance(current_loc, destination);
        int radiusSquaredDistance=Displacement.getRadiusSquaredDistance(current_loc, destination);;

        for(Displacement dir: Displacement.DirectionsWithoutCenter){
            if(rc.canMove(Displacement.displacementToDirection(dir))){
                MapLocation new_loc = Displacement.add(current_loc,dir);

                int MaxXYDistance = Displacement.getMaxXYDistance(new_loc, destination);
                int newSquaredDistance = Displacement.getRadiusSquaredDistance(new_loc, destination);
                if(MaxXYDistance < lowestMaxXYDistance||
                        (MaxXYDistance == lowestMaxXYDistance&&newSquaredDistance<=radiusSquaredDistance)) {
                    best_direction = dir;
                    lowestMaxXYDistance = MaxXYDistance;
                    radiusSquaredDistance = newSquaredDistance;
                }

            }
        }


        if(best_direction!=Displacement.CENTER){
            rc.move(Displacement.displacementToDirection(best_direction));

        }

    }
    public static void pathFindTo(Displacement target) throws GameActionException
    {//TODO: better pathfinding


        System.out.println("PathFindTo: "+target);
        System.out.println("bytecode checkpoint 1:"+Clock.getBytecodeNum());

        //assert(nathanSpawner.RobotPlayer.has_parent_EC); //destination was not defined if this has no parent_EC
        MapLocation current_loc = rc.getLocation();
        MapLocation destination =Displacement.add(current_loc,target);

        Displacement best_direction = Displacement.CENTER;
        double bestScore=1000;
        int currentDistance=Displacement.getMaxXYNorm(target);
        int currentSquaredDistance=Displacement.getRadiusSquaredDistance(rc.getLocation(), destination);

        for(Displacement dir: Displacement.DirectionsWithoutCenter){
            if(rc.canMove(Displacement.displacementToDirection(dir))){
                MapLocation new_loc = Displacement.add(current_loc,dir);

                int maxXYDistance = Displacement.getMaxXYDistance(new_loc, destination);
                int squaredDistance = Displacement.getRadiusSquaredDistance(new_loc, destination);

                if(maxXYDistance< currentDistance||(maxXYDistance== currentDistance&&squaredDistance<currentSquaredDistance)) {
                    double terrain = rc.sensePassability(new_loc);
                    double score = averageInverseTerrain * maxXYDistance +Math.sqrt(squaredDistance)/10+ 1 / terrain;
                    //Math.sqrt(newSquaredDistance)

                    if (score < bestScore) {
                        best_direction = dir;
                        bestScore = score;
                    }
                }

            }
        }


        if(best_direction!=Displacement.CENTER){
            rc.move(Displacement.displacementToDirection(best_direction));
            TotalInverseTerrain+=1/rc.sensePassability(rc.getLocation());
            numberCellSeen++;
            averageInverseTerrain=TotalInverseTerrain/numberCellSeen;
        }
        System.out.println("bytecode checkpoint 10:"+Clock.getBytecodeNum());
    }


}

package my_player;

import battlecode.common.*;
import java.util.*;

public class Movement {
    static RobotController rc;

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static final int[] directions_x = {
            0,
            1,
            1,
            1,
            0,
            -1,
            -1,
            -1,
    };

    static final int[] directions_y = {
            1,
            1,
            0,
            -1,
            -1,
            -1,
            0,
            1,
    };


    private static int movementDistance(Point a, Point b)
    /*
    How many movements are needed to get from a to b
     */
    {
        return Math.max(Math.abs(b.x-a.x), Math.abs(b.y-a.y));
    }


    public static void moveToNaive(Point destination) throws GameActionException
    /*
    Naively attempts to move the robot to the destination.
     */
    {
        System.out.println("moveToNaive: " + destination);
        assert(RobotPlayer.has_parent_EC); //destination was not defined if this has no parent_EC
        Point current_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());

        boolean found_direction = false;
        Direction best_direction = Direction.NORTH;
        int lowest_radius_squared = 0;

        for(Direction dir: directions){
            if(rc.canMove(dir)){
                Point new_loc = current_loc.add(dir);
                if(!found_direction){
                    found_direction = true;
                    best_direction = dir;
                    lowest_radius_squared = Point.getRadiusSquaredDistance(new_loc, destination);
                }
                else{
                    int new_radius_squared = Point.getRadiusSquaredDistance(new_loc, destination);
                    if(new_radius_squared <= lowest_radius_squared){
                        best_direction = dir;
                        lowest_radius_squared = new_radius_squared;
                    }
                }
            }
        }

        if(found_direction){
            rc.move(best_direction);
        }

    }
}

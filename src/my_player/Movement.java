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

    public static int flooredDiv(int a, int b){
        if(b <= -1){
            a = -a;
            b = -b;
        }

        if(a >= 0){
            return a/b;
        }

        return (a-b+1)/b;
    }


    /*
    Store information about boundaries, north, east, south, west.
    Boundary is the last coordinate that is still on the map.
     */
    static int[] boundaries = {-1, -1, 1, 1}; //relative location of boundaries

    static Direction[] boundary_directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    static int[] relative_boundary_directions = {1, 1, -1, -1};


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

    static boolean moved_to_destination = true; //when this is true, we need a new destination
    static Point current_destination;

    public static void moveToDestination() throws GameActionException
    {
        //will try to move closer to its destination
        /*
        If nowhere decreases distance, rotate around
         */

        moveToNaive(current_destination);

        if(RobotPlayer.convertToRelativeCoordinates(rc.getLocation()).equals(current_destination)){
            moved_to_destination = true;
        }

        /*
        TODO: If we determine that we can't get to destination and need to choose a new one,
        also set moved_to_destination = true
         */

    }

    public static int getSector(int x_or_y)
    /*
    Given x or y coordinate relative to parent_EC, determine the x or y sector coordinate
     */
    {
        int rel_middle_sector = x_or_y+3; //relative to (0,0) of sector (8, 8)
        return flooredDiv(rel_middle_sector, 8) + 8;
    }

    public static Point getSectorLoc(Point sector)
    /*
    Returns a middle point of a sector in relative coordinates
     */
    {
        Point sector_loc = new Point(8*(sector.x-8), 8*(sector.y-8));
        if(Math.random() < 0.5){
            sector_loc.x++;
        }
        if(Math.random() < 0.5){
            sector_loc.y++;
        }
        return sector_loc;
    }

    public static int distanceToSector(Point sector)
    /*
    Get distance from current location to a sector
     */
    {
        Point sector_loc = getSectorLoc(sector);
        Point my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());
        return Point.getRadiusSquaredDistance(sector_loc, my_rel_loc);
    }

    public static void assignDestination(Point dest){
        current_destination = dest;
        moved_to_destination = false;
    }
}

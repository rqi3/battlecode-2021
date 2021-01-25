package pathfinding_old;

import battlecode.common.*;

import java.util.*;

/**
 * Movement facilitates the movement of different types of robots.
 * @author    Coast
 */
public class Movement {
    static RobotController rc;
    static double TotalInverseTerrain=6;
    static double averageInverseTerrain=2;
    static int numberCellSeen=3;
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


    /**
     * @param a integer
     * @param b integer
     * @return a/b rounded down
     */
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


    /**
     * Store information about boundaries: north, east, south, west.
     *  Boundary is the last coordinate that is still on the map.
     *  Default values are -1, -1, 1, 1.
     */
    static int[] boundaries = {-1, -1, 1, 1}; //relative location of boundaries

    static Direction[] boundary_directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    static int[] relative_boundary_directions = {1, 1, -1, -1};


    /**
     * How many movements are needed to get from a to b
     */
    public static int movementDistance(Point a, Point b) {
        return Math.max(Math.abs(b.x-a.x), Math.abs(b.y-a.y));
    }

    /**
     * Naively attempts to move the robot to the destination according to Euclidean distance.
     * Always tries to move, even if brings you farther away.
     * @param destination destination in relative coordinates
     * @throws GameActionException
     */
    public static void moveToNaive(Point destination) throws GameActionException {

        //System.out.println("moveToNaive: " + destination);
        assert(RobotPlayer.has_parent_EC); //destination was not defined if this has no parent_EC
        Point current_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());

        //assert(nathanSpawner.RobotPlayer.has_parent_EC); //destination was not defined if this has no parent_EC

        Direction best_direction = Direction.CENTER;
        double bestScore=1000;
        int currentDistance=Point.getMaxXYDistance(current_loc,destination);
        int currentSquaredDistance=Point.getRadiusSquaredDistance(current_loc, destination);

        for(Direction dir: directions){
            if(rc.canMove(dir)){
                Point new_loc = current_loc.add(dir);

                int maxXYDistance = Point.getMaxXYDistance(new_loc, destination);
                int squaredDistance = Point.getRadiusSquaredDistance(new_loc, destination);

                if(maxXYDistance< currentDistance||(maxXYDistance== currentDistance&&squaredDistance<currentSquaredDistance)) {
                    double terrain = rc.sensePassability(RobotPlayer.convertFromRelativeCoordinates(new_loc));
                    double score = averageInverseTerrain * maxXYDistance +Math.sqrt(squaredDistance)/10+ 1 / terrain;
                    //Math.sqrt(newSquaredDistance)

                    if (score < bestScore) {
                        best_direction = dir;
                        bestScore = score;
                    }
                }

            }
        }


        if(best_direction!=Direction.CENTER){
            rc.move(best_direction);
            TotalInverseTerrain+=1/rc.sensePassability(rc.getLocation());
            numberCellSeen++;
            averageInverseTerrain=TotalInverseTerrain/numberCellSeen;
        }

    }

    /**
     * The relative location of our current destination and whether we have reached our current destination.
     */
    static boolean moved_to_destination = true; //when this is true, we need a new destination
    static Point current_destination;

    /**
     * Assigns this destination as the current destination
     * @param dest destination
     */
    public static void assignDestination(Point dest){
        current_destination = dest;
        moved_to_destination = false;
    }

    /**
     * Updates current location if we know it is off of the map
     */
    public static void updateCurrentLocationBoundary(){
        if(boundaries[0] >= 0){
            current_destination.y = Math.min(current_destination.y, boundaries[0]);
        }
        if(boundaries[1] >= 0){
            current_destination.x = Math.min(current_destination.x, boundaries[1]);
        }
        if(boundaries[2] <= 0){
            current_destination.y = Math.max(current_destination.y, boundaries[2]);
        }
        if(boundaries[3] <= 0){
            current_destination.x = Math.max(current_destination.x, boundaries[3]);
        }
    }
    /**
     * Moves rc closer to the current_destination.
     * TODO: If nowhere decreases distance, rotate around
    */
    public static void moveToDestination() throws GameActionException {

        Point current_location = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());

        updateCurrentLocationBoundary();

        moveToNaive(current_destination);

        if(RobotPlayer.convertToRelativeCoordinates(rc.getLocation()).equals(current_destination)){
            moved_to_destination = true;
        }

        if(Point.getRadiusSquaredDistance(current_location, current_destination) <= 2) {
            if (rc.isLocationOccupied(RobotPlayer.convertFromRelativeCoordinates(current_destination))) {
                moved_to_destination = true; //We give up and reaching the exact destination
            }
        }
    }


    /**
     * Given x or y coordinate relative to parent_EC, determine the x or y sector coordinate
     */
    public static int getSector(int x_or_y) {
        int rel_middle_sector = x_or_y+3; //relative to (0,0) of sector (8, 8)
        //System.out.println("getSector Debug: " + x_or_y + ", " + (flooredDiv(rel_middle_sector, 8) + 8));
        return flooredDiv(rel_middle_sector, 8) + 8;
    }

    /**
     * Returns a random middle point of a sector in relative coordinates
     * @param sector coordinates
     * @return relative coordinates of a middle point
     */
    public static Point getSectorLoc(Point sector) {
        Point sector_loc = new Point(8*(sector.x-8), 8*(sector.y-8));
        if(Math.random() < 0.5){
            sector_loc.x++;
        }
        if(Math.random() < 0.5){
            sector_loc.y++;
        }
        return sector_loc;
    }

    /**
     * Get distance from current location to a middle point of a sector
     * @param sector sector coordinates
     * @return distance to sector
     */
    public static int distanceToSector(Point sector) {
        Point sector_loc = getSectorLoc(sector);
        Point my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());
        return Point.getRadiusSquaredDistance(sector_loc, my_rel_loc);
    }


}

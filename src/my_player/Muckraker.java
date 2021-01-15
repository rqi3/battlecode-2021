package my_player;

import battlecode.common.*;
import java.util.*;

/**
 * Muckraker controls the actions of our Muckrakers.
 * @author    Coast
 */
public class Muckraker {
    static RobotController rc;
    static boolean is_scout = true; //whether this Muckraker will be communicating stuff it sees to parent_EC
    static boolean is_attacker = false;
    static List<MapLocation> communicated_ecs = new ArrayList<>();

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

    /**
     * Check whether parent EC died and possibly change type if this happens.
     */
    private static void updateParentEC() {
        if(!RobotPlayer.has_parent_EC) return;
        if(!rc.canGetFlag(RobotPlayer.parent_EC.getID())){ //parent EC died
            RobotPlayer.has_parent_EC = false; //should never change to true again
        }
    }

    static Point my_rel_loc; //if parent EC exists, stores relative location
    static RobotInfo[] all_nearby_robots;


    /**
     * Updates my_rel_loc, all_nearby_robots.
     * Find boundaries.
     * Scout updates their visited sectors.
     */
    static void lookAround() throws GameActionException {
        all_nearby_robots = rc.senseNearbyRobots();

        if(RobotPlayer.has_parent_EC){
            my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());
            System.out.println("My relative location: " + my_rel_loc);
            for(int i = 0; i < 4; i++){
                if(Movement.boundaries[i] == -Movement.relative_boundary_directions[i])
                /*
                boundary hasn't been calculated yet
                 */
                {
                    MapLocation loc = rc.getLocation();
                    for(int j = 1;;j++){
                        loc = loc.add(Movement.boundary_directions[i]);
                        if(!rc.canSenseRadiusSquared(j*j)){
                            break;
                        }
                        if(!rc.onTheMap(loc)){
                            //found a boundary!
                            if(i % 2 == 0){
                                Movement.boundaries[i] = my_rel_loc.y;
                            }
                            else{
                                Movement.boundaries[i] = my_rel_loc.x;
                            }
                            Movement.boundaries[i] += Movement.relative_boundary_directions[i]*(j-1);
                            System.out.println("Found " + Movement.boundary_directions[i] + " boundary at location " + Movement.boundaries[i]);
                            updateBoundarySectors(i, Movement.boundaries[i]);
                            break;
                        }
                    }
                }

            }
        }
    }

    /**
     * Attacks an enemy slanderer if one is in range.
     */
    static void attackSlanderer() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    System.out.println("exposed");
                    rc.expose(robot.location);
                }
            }
        }
    }

    //////////////BEGIN SCOUT MOVEMENT CODE

    /**
     * Sectors that the scout has visited
     *  Starting sector is 8, 8.
     *  Starting position in this sector is (3, 3) 0-indexed
     */
    static int[][] visited_sectors = new int[17][17];
    static Point next_sector = new Point(8, 8); //the sector that this scout is trying to get to

    /**
     * When we find a boundary, some sectors are declared invalid (visited = 3)
     */
    private static void updateBoundarySectors(int boundary_type, int boundary_loc) {
        int boundary_sector_coordinate = Movement.getSector(boundary_loc);
        while(true){
            boundary_sector_coordinate+=Movement.relative_boundary_directions[boundary_type];
            if(0 <= boundary_sector_coordinate && boundary_sector_coordinate <= 16){
                if((boundary_type&1) == 0){
                    for(int i = 0; i <= 16; i++){
                        visited_sectors[i][boundary_sector_coordinate] = 3;
                    }
                }
                else{
                    for(int i = 0; i <= 16; i++){
                        visited_sectors[boundary_sector_coordinate][i] = 3;
                    }
                }
                System.out.println("boundary_sector_coordinate: " + boundary_sector_coordinate);
            }
            else break;
        }
    }

    /**
     *
     * @param sector coordinates of the sector
     * @return whether the sector is on the map (based on current information)
     */
    private static boolean isValidSector(Point sector){
        if(sector.x < 0 || sector.x > 16 || sector.y < 0 || sector.y > 16) return false;
        if(visited_sectors[sector.x][sector.y] == 3) return false;
        return true;
    }


    static boolean roaming_sectors = false; //initially goes in the initial direction, then roams around sectors
    static double scout_initial_direction = 0;
    static int scout_initial_quadrant = 0;

    /**
     *
     * @return the value that scout_initial_direction will take on
     */
    static double getInitialDirection(){
        double a = Math.random();
        double b = Math.random();
        double random_value = a+b;
        if(random_value-1.0 >= 0) random_value-=1.0;
        double cardinal_direction = (double)((int)(random_value*8))*0.25*Math.PI;
        return cardinal_direction;
    }
    /**
     * Assigns a next_sector based on the current sector (next_sector) and scout_initial_direction
     * @return whether a next_sector was decided upon
     */
    static boolean assignInitialDirectionSector(){
        if(scout_initial_quadrant == 0){
            if(scout_initial_direction <= 0.5*Math.PI){
                scout_initial_quadrant = 1;
            }
            else if(scout_initial_direction <= Math.PI){
                scout_initial_quadrant = 2;
            }
            else if(scout_initial_direction <= 1.5*Math.PI){
                scout_initial_quadrant = 3;
            }
            else{
                scout_initial_quadrant = 4;
            }
        }
        Point first_possible_sector = next_sector.clone(); //first in order of angle from theta=0
        Point second_possible_sector = next_sector.clone();
        double deciding_point_x = next_sector.x;
        double deciding_point_y = next_sector.y;
        if(scout_initial_quadrant == 1){
            first_possible_sector.x++;
            second_possible_sector.y++;
            deciding_point_x+=0.5;
            deciding_point_y+=0.5;
        }
        else if(scout_initial_quadrant == 2){
            //System.out.println("possible sectors: " + first_possible_sector + second_possible_sector);
            first_possible_sector.y++;
            second_possible_sector.x--;
            //System.out.println("possible sectors: " + first_possible_sector + second_possible_sector);
            deciding_point_x-=0.5;
            deciding_point_y+=0.5;
        }
        else if(scout_initial_quadrant == 3){
            first_possible_sector.x--;
            second_possible_sector.y--;
            deciding_point_x-=0.5;
            deciding_point_y-=0.5;
        }
        else if(scout_initial_quadrant == 4){
            first_possible_sector.y--;
            second_possible_sector.x++;
            deciding_point_x+=0.5;
            deciding_point_y-=0.5;
        }

        double deciding_angle = Math.atan2(deciding_point_y-8.0, deciding_point_x-8.0);
        if(deciding_angle < 0.0) deciding_angle+=2*Math.PI;

        //System.out.println("deciding point: " + deciding_point_x + ", " + deciding_point_y);
        //System.out.println("deciding angle: " + deciding_angle);
        Point correct_sector;
        if(deciding_angle <= scout_initial_direction){
            correct_sector = second_possible_sector;
        }
        else{
            correct_sector = first_possible_sector;
        }

        if(isValidSector(correct_sector) && visited_sectors[correct_sector.x][correct_sector.y] == 0){
            next_sector = correct_sector;
            /*System.out.println("scout_initial_direction: " + scout_initial_direction);
            System.out.println("scout_initial_quadrant: " + scout_initial_quadrant);
            System.out.println("Possible sectors: " + first_possible_sector + ", " + second_possible_sector);
            System.out.println("Angle method found a next_sector: " + next_sector);*/
            return true;
        }

        return false;
    }


    static int[][] possible_new_sector_distances = new int[17][17];
    /**
     * Chooses a new sector for this scout; updates next_sector
     */
    static void assignNewSector() {
        Point my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());

        //next_sector should currently be the current sector
        next_sector.x = Movement.getSector(my_rel_loc.x);
        next_sector.y = Movement.getSector(my_rel_loc.y);

        if(!roaming_sectors){
            boolean found_directional_sector = assignInitialDirectionSector();
            if(found_directional_sector){
                return;
            }
            roaming_sectors = true;
        }

        int min_sector_dist = 9999;
        for(int i = 0; i <= 16; i++){
            for(int j = 0; j <= 16; j++){
                if(visited_sectors[i][j] != 0) {
                    possible_new_sector_distances[i][j] = 9999;
                    continue;
                }
                possible_new_sector_distances[i][j] = Math.max(Math.abs(next_sector.x-i), Math.abs(next_sector.y-j));
                min_sector_dist = Math.min(min_sector_dist, possible_new_sector_distances[i][j]);
            }
        }
        if(min_sector_dist < 9999){
            List<Point> min_sector0s = new ArrayList<>();
            for(int i = 0; i <= 16; i++){
                for(int j = 0; j <= 16; j++){
                    if(possible_new_sector_distances[i][j] == min_sector_dist){
                        min_sector0s.add(new Point(i, j));
                    }
                }
            }
            int min_sector0s_ind = (int)(Math.random()*min_sector0s.size());
            next_sector = min_sector0s.get(min_sector0s_ind);
            System.out.println("Found new unvisited sector: " + next_sector);
            return;
        }

        List<Point> sector1s = new ArrayList<>();

        for(int i = Math.max(0, next_sector.x-2); i <= Math.min(16, next_sector.x+2); i++){
            for(int j = Math.max(0, next_sector.y-2); j <= Math.min(16, next_sector.y+2); j++){
                if(i == next_sector.x && j == next_sector.y) continue; //don't choose the same
                if(visited_sectors[i][j] == 3) continue; //impossible sector

                Point new_sector = new Point(i, j);
                if(visited_sectors[i][j] == 1){
                    sector1s.add(new_sector);
                }
            }
        }

        //if sector hasn't been decided, choose the closest visited = 1 sector
        if(sector1s.size() > 0){
            int sector1s_ind = (int)(Math.random()*sector1s.size());
            next_sector = sector1s.get(sector1s_ind);
            return;
        }
        System.out.println("NEXT SECTOR NOT FOUND");
    }

    /**
     * Movement code for Scouts
     */
    private static void moveScout() throws GameActionException {
        visited_sectors[8][8] = 1; //default set your starting sector

        if(Movement.moved_to_destination){
            if(visited_sectors[next_sector.x][next_sector.y] == 0){
                visited_sectors[next_sector.x][next_sector.y] = 1;
            }
        }

        /*
        If you reached the destination sector or the sector is now invalid,
        assign a new sector & destination.
         */
        if(visited_sectors[next_sector.x][next_sector.y] != 0) {
            assignNewSector();
            Movement.assignDestination(Movement.getSectorLoc(next_sector));
        }

        System.out.println("Current Sector: " + next_sector);
        System.out.println("Current Sector Destination: " + Movement.current_destination);
        Movement.moveToDestination();

        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }

    ///////////////END OF SCOUT MOVEMENT CODE


    static Point goal = null;
    /**
     * Moves an attacker to surround goal (a chosen enemy_ec)
     * @throws GameActionException
     */
    public static void moveAttacker() throws GameActionException {
        Point my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());

        if(goal == null){
            if(RobotPlayer.enemy_ecs.size() > 0) {
                goal = RobotPlayer.enemy_ecs.get((int)(Math.random()*RobotPlayer.enemy_ecs.size())).rel_loc;
            }
        }

        if(goal == null){
            tryMove(randomDirection());
        }
        else {
            if(Point.getRadiusSquaredDistance(goal, my_rel_loc) <= 2){
                //it has done its job and is next to the enemy base
                return;
            }
            Movement.moveToNaive(goal);
        }
    }

    /**
     * Sends information to parent_EC by generating a flag value
     * @return flag value to set
     */
    private static int generateFlagValue(){
        int flag_value = 0;

        if(is_scout && RobotPlayer.has_parent_EC){
            /*
            Communication Type (first 3 flag bits):
            100 - Neutral EC, next 7 bits are Influence*double(127/500), last 14 bits are location
            010 - Enemy EC, last 14 bits are location
            110 - Friend EC, last 14 bits are location
            001 - Enemy Muckraker, next 7 bits are Conviction*double(127/500), last 14 bits are location
            Todo: Whether Neutral EC is being contested, Map Edges
            000 - not communicating anything on this list
             */

            boolean neutral_EC_just_found = false;
            boolean enemy_EC_just_found = false;
            boolean friend_EC_just_found = false;

            boolean neutral_EC_nearby = false;
            boolean enemy_EC_nearby = false;
            boolean friend_EC_nearby = false;

            Neutral_EC_Info neutral_ec = new Neutral_EC_Info();
            Enemy_EC_Info enemy_ec = new Enemy_EC_Info();
            Friend_EC_Info friend_ec = new Friend_EC_Info();

            //boolean enemy_Muckraker_nearby = false;

            for(RobotInfo nearby_robot: all_nearby_robots){
                if(nearby_robot.getTeam() == Team.NEUTRAL && nearby_robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                    //Neutral Enlightenment Center found
                    neutral_EC_nearby = true;
                    if(!communicated_ecs.contains(nearby_robot.getLocation())){
                        neutral_EC_just_found = true;
                        neutral_ec.setPosition(nearby_robot.getLocation(), RobotPlayer.parent_EC.getLocation());
                        neutral_ec.setInfluence(nearby_robot.getInfluence());
                    }
                    else if(!neutral_EC_just_found){
                        neutral_ec.setPosition(nearby_robot.getLocation(), RobotPlayer.parent_EC.getLocation());
                        neutral_ec.setInfluence(nearby_robot.getInfluence());
                    }
                }
                else if(nearby_robot.getTeam() == rc.getTeam().opponent() && nearby_robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                    //Enemy Enlightenment Center found
                    enemy_EC_nearby = true;
                    if(!communicated_ecs.contains(nearby_robot.getLocation())){
                        enemy_EC_just_found = true;
                        enemy_ec.setPosition(nearby_robot.getLocation(), RobotPlayer.parent_EC.getLocation());
                    }
                    else if(!enemy_EC_just_found){
                        enemy_ec.setPosition(nearby_robot.getLocation(), RobotPlayer.parent_EC.getLocation());
                    }
                }
                else if(nearby_robot.getTeam() == rc.getTeam() && nearby_robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                    if(nearby_robot.getLocation().equals(RobotPlayer.parent_EC.getLocation())) continue; //don't need to communicate this
                    //Friend Enlightenment Center found
                    friend_EC_nearby = true;
                    if(!communicated_ecs.contains(nearby_robot.getLocation())){
                        friend_EC_just_found = true;
                        friend_ec.setPosition(nearby_robot.getLocation(), RobotPlayer.parent_EC.getLocation());
                    }
                    else if(!friend_EC_just_found){
                        friend_ec.setPosition(nearby_robot.getLocation(), RobotPlayer.parent_EC.getLocation());
                    }
                }
                else if(nearby_robot.getTeam() == rc.getTeam().opponent() && nearby_robot.getType() == RobotType.MUCKRAKER){
                    //TODO: Muckraker detection
                }

            }


            /*
            Communication Priority
             */
            if(neutral_EC_just_found){
                flag_value = neutral_ec.toFlagValue();
                communicated_ecs.add(neutral_ec.loc);
            }
            else if(enemy_EC_just_found){
                flag_value = enemy_ec.toFlagValue();
                communicated_ecs.add(enemy_ec.loc);
            }
            else if(neutral_EC_nearby){
                flag_value = neutral_ec.toFlagValue();
            }
            else if(enemy_EC_nearby){
                flag_value = enemy_ec.toFlagValue();
            }
            else if(friend_EC_just_found){
                flag_value = friend_ec.toFlagValue();
                communicated_ecs.add(friend_ec.loc);
            }
            else if(friend_EC_nearby){
                flag_value = friend_ec.toFlagValue();
            }
            //also update communicated_ecs
        }

        return flag_value;
    }

    public static void run() throws GameActionException{
        ////////////////////Creation Begin
        if(RobotPlayer.just_made){
            scout_initial_direction = getInitialDirection();
            System.out.println("scout_initial_direction: " + scout_initial_direction);
            rc = RobotPlayer.rc;
            Movement.rc = RobotPlayer.rc;
            if(rc.getInfluence() == 1){
                is_scout = true;
                is_attacker = false;
            }
            else if(rc.getInfluence() > 1){
                is_scout = false;
                is_attacker = true;
            }
            RobotPlayer.assignParentEC(); //after it spawns, record which EC spawned it (if any)

            System.out.println("has_parent_EC: " + RobotPlayer.has_parent_EC);
            if(RobotPlayer.has_parent_EC){
                System.out.println("parent Location: " + RobotPlayer.parent_EC.getLocation());
            }
        }
        ////////////////////Creation End

        //////////////////// Begin Initialization
        updateParentEC();
        if(!RobotPlayer.has_parent_EC){
            is_scout = false;
            is_attacker = true;
        }
        //////////////////// End Initialization

        //////////////////// Begin Sense
        lookAround();
        //////////////////// End Sense


        //////////////////// Begin Receive Broadcast
        RobotPlayer.receiveECBroadcast();
        //////////////////// End Receive Broadcast

        //////////////////// Attack Begin
        attackSlanderer(); //assume that we always want to attack if we can
        //////////////////// Attack End

        //////////////////// Movement Begin
        if(is_scout){ //movement for scout
            moveScout();
        }
        else if(is_attacker){
            moveAttacker();
        }
        //////////////////// Movement End


        ////////////////////Send Communication Begin
        int flag_value = generateFlagValue();
        if(rc.canSetFlag(flag_value)){
            rc.setFlag(flag_value);
        }
        ////////////////////Send Communication End
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
}

package my_player;
import battlecode.common.*;
import java.util.*;

/**
 * Contains useful methods for Unit Communication and keeping track of neutral_ecs, enemy_ecs, friend_ecs, enemy_units
 */
public class UnitComms {
    static RobotController rc;

    static List<MapLocation> communicated_ec_locations = new ArrayList<>(); //EC locations already sent to parent_EC
    static RobotInfo[] all_nearby_robots;
    static Point my_rel_loc = new Point();
    /**
     * Updates ec lists and enemy_units
     */
    static void lookAround() throws GameActionException {
        if(!RobotPlayer.has_parent_EC) return;

        all_nearby_robots = rc.senseNearbyRobots();
        my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());

        for(RobotInfo nearby_robot: all_nearby_robots){
            if(nearby_robot.getTeam() == Team.NEUTRAL && nearby_robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                //Neutral Enlightenment Center found
                Neutral_EC_Info neutral_ec = new Neutral_EC_Info();
                neutral_ec.setPosition(nearby_robot.getLocation(), RobotPlayer.parent_EC.getLocation());
                neutral_ec.setInfluence(nearby_robot.getInfluence());
                System.out.println("Neutral EC found");
            }
            else if(nearby_robot.getTeam() == rc.getTeam().opponent() && nearby_robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                //Enemy Enlightenment Center found
                Enemy_EC_Info enemy_ec = new Enemy_EC_Info();
                enemy_ec.setPosition(nearby_robot.getLocation(), RobotPlayer.parent_EC.getLocation());
                enemy_ec.setInfluence(nearby_robot.getInfluence());
                System.out.println("Enemy EC found");
            }
            else if(nearby_robot.getTeam() == rc.getTeam() && nearby_robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                if(nearby_robot.getLocation().equals(RobotPlayer.parent_EC.getLocation())) continue; //don't need to communicate this
                //Friend Enlightenment Center found
                Friend_EC_Info friend_ec = new Friend_EC_Info();
                friend_ec.setPosition(nearby_robot.getLocation(), RobotPlayer.parent_EC.getLocation());
                friend_ec.setInfluence(nearby_robot.getInfluence());
            }
            else if(nearby_robot.getTeam() == rc.getTeam().opponent()){
                //TODO: ENEMY UNIT
            }

        }
    }

    //Communication priorities
    static final int NEW_NEUTRAL_EC = 1;
    static final int NEW_ENEMY_EC = 2;
    static final int CLOSEST_ENEMY_MUCKRAKER = 3;
    static final int CLOSEST_ENEMY_POLITICIAN = 4;
    static final int NEW_BORDER = 5;
    static final int OLD_NEUTRAL_EC = 6;
    static final int CLOSEST_ENEMY_SLANDERER = 7;
    static final int NEW_FRIEND_EC = 8;
    static final int OLD_FRIEND_EC = 9;

    /**
     * Generates a flag value for a non-EC unit
     * @return
     */
    static int generateFlagValue(){
        /**
         * Possible flag values to communicate in order of priority
         */
        int[] possible_flag_values = new int[30];



        for(int i = 0; i < 30; i++){
            if(possible_flag_values[i] != 0) return possible_flag_values[i];
        }

        return 0;
    }

}

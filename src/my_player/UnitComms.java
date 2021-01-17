package my_player;
import battlecode.common.*;
import java.util.*;

/**
 * Contains useful methods for Unit Communication and keeping track of neutral_ecs, enemy_ecs, friend_ecs, enemy_units
 */
public class UnitComms {
    static RobotController rc;

    /**
     * TODO: These should be updated when receiving broadcasts as well, otherwise units will communicate these back.
     */
    static List<MapLocation> communicated_neutral_ecs = new ArrayList<>(); //EC locations already sent to parent_EC or received from parent EC
    static List<MapLocation> communicated_enemy_ecs = new ArrayList<>();
    static List<MapLocation> communicated_friend_ecs = new ArrayList<>();

    static RobotInfo[] all_nearby_robots;
    static Point my_rel_loc = new Point();

    static int BYTECODE_LIMIT = 1300;
    /**
     * Method for Any Robot:
     * Updates ec lists and enemy_units
     */
    static void lookAround() throws GameActionException {
        if(rc.getType() != RobotType.ENLIGHTENMENT_CENTER && !RobotPlayer.has_parent_EC) return;
        System.out.println("Bytecode 1.1: " + Clock.getBytecodeNum());
        all_nearby_robots = rc.senseNearbyRobots();
        System.out.println("Bytecode 1.2: " + Clock.getBytecodeNum());
        my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());

        RobotInfo intermediate;
        for(int i = all_nearby_robots.length-1; i >= 0; i--){
            int j = (int)(Math.random()*(i+1));
            intermediate = all_nearby_robots[i];
            all_nearby_robots[i] = all_nearby_robots[j];
            all_nearby_robots[j] = intermediate;
        }
        int starting_bytecode = Clock.getBytecodeNum();
        for(RobotInfo nearby_robot: all_nearby_robots){
            if(nearby_robot.getTeam() == Team.NEUTRAL){
                //Neutral Enlightenment Center found
                Neutral_EC_Info neutral_ec = new Neutral_EC_Info();
                neutral_ec.setPosition(nearby_robot.getLocation());
                neutral_ec.setInfluence(nearby_robot.getInfluence());

                RobotPlayer.addECInfo(neutral_ec);
                //System.out.println("Neutral EC found");
            }
            else if(nearby_robot.getTeam() == rc.getTeam().opponent()){
                //TODO: ENEMY UNIT (NON EC)
                EnemyUnitInfo enemy_unit = new EnemyUnitInfo();
                enemy_unit.setType(nearby_robot.getType());
                enemy_unit.setRound(rc.getRoundNum());
                enemy_unit.setLocation(nearby_robot.getLocation());
                RobotPlayer.addEnemyUnitInfo(enemy_unit);
                //System.out.println("Found enemy unit at: " + nearby_robot.getLocation());

            }
            else if(nearby_robot.getTeam() == rc.getTeam()){
                //TODO: FRIENDLY UNIT COMMUNICATION, LOOK AT THEIR FLAG FOR PROPAGATED ENEMY UNITS
            }
            else if(nearby_robot.getTeam() == rc.getTeam().opponent() && nearby_robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                //Enemy Enlightenment Center found
                Enemy_EC_Info enemy_ec = new Enemy_EC_Info();
                enemy_ec.setPosition(nearby_robot.getLocation());
                enemy_ec.setInfluence(nearby_robot.getInfluence());

                RobotPlayer.addECInfo(enemy_ec);
                //System.out.println("Enemy EC found");
            }
            else if(nearby_robot.getTeam() == rc.getTeam() && nearby_robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
                if(nearby_robot.getLocation().equals(RobotPlayer.parent_EC.getLocation())) continue; //don't need to communicate this
                //Friend Enlightenment Center found
                Friend_EC_Info friend_ec = new Friend_EC_Info();
                friend_ec.setPosition(nearby_robot.getLocation());
                friend_ec.setInfluence(nearby_robot.getInfluence());

                RobotPlayer.addECInfo(friend_ec);
            }
            if(Clock.getBytecodeNum()-starting_bytecode > BYTECODE_LIMIT){
                break;
            }
        }

        System.out.println("Bytecode 1.3: " + Clock.getBytecodeNum());
    }

    /**
     * Communication priorities, can be different between units or randomized
     * New EC means that the location was not already in its corresponding communicated_ec list
     */
    static int NEW_NEUTRAL_EC = 0;
    static int OLD_NEUTRAL_EC = 0;
    static int NEW_ENEMY_EC = 0;
    static int OLD_ENEMY_EC = 0;
    static int NEW_FRIEND_EC = 0;
    static int OLD_FRIEND_EC = 0;
    static int CLOSEST_ENEMY_ATTACKER = 0; //closest enemy muckraker/politician
    static int NEW_BORDER = 0;
    static int CLOSEST_ENEMY_SLANDERER = 0;

    /**
     * Generates a flag value for a non-EC unit
     * 100 - Neutral EC
     * 010 - Enemy EC
     * 110 - Friend EC
     * 001 - Enemy Muckraker
     * 101 - Enemy Politician
     * 011 - Enemy Slanderer
     * @return
     */
    static int generateFlagValue(){
        /**
         * Possible flag values to communicate in order of priority.
         */

        if(rc.getType() == RobotType.MUCKRAKER && Muckraker.muckraker_type == Muckraker.SCOUT){
            NEW_NEUTRAL_EC = 1;
            NEW_ENEMY_EC = 2;
            NEW_FRIEND_EC = 3;
            CLOSEST_ENEMY_ATTACKER = 4;
            NEW_BORDER = 5;
            CLOSEST_ENEMY_SLANDERER = 6;
            OLD_NEUTRAL_EC = 7;
            OLD_ENEMY_EC = 8;
            OLD_FRIEND_EC = 9;
        }
        else{
            NEW_NEUTRAL_EC = 1;
            NEW_ENEMY_EC = 2;
            NEW_FRIEND_EC = 3;
            CLOSEST_ENEMY_ATTACKER = 4;
            NEW_BORDER = 5;
            CLOSEST_ENEMY_SLANDERER = 6;
            OLD_NEUTRAL_EC = 7+(int)(Math.random()*10);
            OLD_ENEMY_EC = 7+(int)(Math.random()*10);
            OLD_FRIEND_EC = 7+(int)(Math.random()*10);
        }

        int[] possible_flag_values = new int[20];

        /*
        possible_flag_values[NEW_NEUTRAL_EC] =
         */


        return 0;
    }

}

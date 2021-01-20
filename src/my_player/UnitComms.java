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
    static MapLocation my_loc;
    static Point my_rel_loc = new Point();

    static int BYTECODE_LIMIT = 1000;



    /**
     * Method for Any Robot:
     * Updates ec lists and ClosestEnemyAttacker
     */
    static void lookAroundBeforeMovement() throws GameActionException {
        if(rc.getType() != RobotType.ENLIGHTENMENT_CENTER && !RobotPlayer.has_parent_EC) return;
        all_nearby_robots = rc.senseNearbyRobots();

        my_loc = rc.getLocation();
        my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());

        int bytecode_before = Clock.getBytecodeNum();


        boolean enemy_nearby = false;
        MapLocation closest_enemy = my_loc;
        int closest_enemy_type = 0;

        int enemy_propagated = 0;

        /*
        Scan for the nearest enemy attacker (non-slanderer)
         */
        for(RobotInfo nearby_robot: all_nearby_robots){
            RobotType nearby_robot_type = nearby_robot.getType();
            if(nearby_robot.getTeam() == rc.getTeam().opponent() && nearby_robot_type != RobotType.ENLIGHTENMENT_CENTER){
                //TODO: ENEMY UNIT (NON EC)
                if(nearby_robot_type == RobotType.MUCKRAKER){
                    if(enemy_nearby){
                        if(my_loc.distanceSquaredTo(nearby_robot.getLocation()) < my_loc.distanceSquaredTo(closest_enemy)){
                            closest_enemy = nearby_robot.getLocation();
                            closest_enemy_type = 1;
                        }
                    }
                    else{
                        enemy_nearby = true;
                        closest_enemy = nearby_robot.getLocation();
                        closest_enemy_type = 1;
                    }
                }
                else if(nearby_robot_type == RobotType.POLITICIAN){
                    if(enemy_nearby){
                        if(my_loc.distanceSquaredTo(nearby_robot.getLocation()) < my_loc.distanceSquaredTo(closest_enemy)){
                            closest_enemy = nearby_robot.getLocation();
                            closest_enemy_type = 2;
                        }
                    }
                    else{
                        enemy_nearby = true;
                        closest_enemy = nearby_robot.getLocation();
                        closest_enemy_type = 2;
                    }
                }
                //System.out.println("Found enemy unit at: " + nearby_robot.getLocation());
            }
        }

        /*
        Receive Info from friendly units
         */
        for(RobotInfo nearby_robot: all_nearby_robots){
            RobotType nearby_robot_type = nearby_robot.getType();
            if(nearby_robot.getTeam() == rc.getTeam() && nearby_robot_type != RobotType.ENLIGHTENMENT_CENTER){
                int robot_flag = rc.getFlag(nearby_robot.getID());
                if(ClosestEnemyAttacker.updateUsingSeenFlagValue(robot_flag)){
                    //System.out.println("Received new information about close enemy from: " + nearby_robot.getLocation());
                    enemy_propagated++;
                }
            }
            if(Clock.getBytecodeNum()-bytecode_before > BYTECODE_LIMIT){
                System.out.println("Only propagated: " + enemy_propagated);
                break;
            }
        }



        if(ClosestEnemyAttacker.enemy_exists && rc.canSenseLocation(ClosestEnemyAttacker.enemy_position)){
            ClosestEnemyAttacker.enemy_exists = false;
        }
        if(enemy_nearby){
            ClosestEnemyAttacker.foundAttacker(closest_enemy, closest_enemy_type, rc.getRoundNum());
        }


        //System.out.println("Bytecode used in UnitComms.lookAroundBeforeMovement(): " + (Clock.getBytecodeNum()-bytecode_before));
    }

    static void lookAroundAfterMovement() throws GameActionException {
        if(rc.getType() != RobotType.ENLIGHTENMENT_CENTER && !RobotPlayer.has_parent_EC) return;
        all_nearby_robots = rc.senseNearbyRobots();

        my_loc = rc.getLocation();
        my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());

        int bytecode_before = Clock.getBytecodeNum();


        boolean enemy_nearby = false;
        MapLocation closest_enemy = my_loc;
        int closest_enemy_type = 0;

        /*
        Scan for the nearest enemy attacker (non-slanderer)
         */
        for(RobotInfo nearby_robot: all_nearby_robots){
            RobotType nearby_robot_type = nearby_robot.getType();
            if(nearby_robot.getTeam() == rc.getTeam().opponent() && nearby_robot_type != RobotType.ENLIGHTENMENT_CENTER){
                //TODO: ENEMY UNIT (NON EC)
                if(nearby_robot_type == RobotType.MUCKRAKER){
                    if(enemy_nearby){
                        if(my_loc.distanceSquaredTo(nearby_robot.getLocation()) < my_loc.distanceSquaredTo(closest_enemy)){
                            closest_enemy = nearby_robot.getLocation();
                            closest_enemy_type = 1;
                        }
                    }
                    else{
                        enemy_nearby = true;
                        closest_enemy = nearby_robot.getLocation();
                        closest_enemy_type = 1;
                    }
                }
                else if(nearby_robot_type == RobotType.POLITICIAN){
                    if(enemy_nearby){
                        if(my_loc.distanceSquaredTo(nearby_robot.getLocation()) < my_loc.distanceSquaredTo(closest_enemy)){
                            closest_enemy = nearby_robot.getLocation();
                            closest_enemy_type = 2;
                        }
                    }
                    else{
                        enemy_nearby = true;
                        closest_enemy = nearby_robot.getLocation();
                        closest_enemy_type = 2;
                    }
                }
                //System.out.println("Found enemy unit at: " + nearby_robot.getLocation());
            }
        }

        if(ClosestEnemyAttacker.enemy_exists && rc.canSenseLocation(ClosestEnemyAttacker.enemy_position)){
            ClosestEnemyAttacker.enemy_exists = false;
        }
        if(enemy_nearby){
            ClosestEnemyAttacker.foundAttacker(closest_enemy, closest_enemy_type, rc.getRoundNum());
        }

        for(RobotInfo nearby_robot: all_nearby_robots){
            RobotType nearby_robot_type = nearby_robot.getType();
            if(nearby_robot.getTeam() == Team.NEUTRAL){
                //Neutral Enlightenment Center found
                Neutral_EC_Info neutral_ec = new Neutral_EC_Info();
                neutral_ec.setPosition(nearby_robot.getLocation());
                neutral_ec.setInfluence(nearby_robot.getInfluence());

                RobotPlayer.addECInfo(neutral_ec);
                //System.out.println("Neutral EC found");
            }
            else if(nearby_robot.getTeam() == rc.getTeam().opponent() && nearby_robot_type == RobotType.ENLIGHTENMENT_CENTER){
                //Enemy Enlightenment Center found
                Enemy_EC_Info enemy_ec = new Enemy_EC_Info();
                enemy_ec.setPosition(nearby_robot.getLocation());
                enemy_ec.setInfluence(nearby_robot.getInfluence());

                RobotPlayer.addECInfo(enemy_ec);
                //System.out.println("Enemy EC found");
            }
            else if(nearby_robot.getTeam() == rc.getTeam() && nearby_robot_type == RobotType.ENLIGHTENMENT_CENTER){
                if(rc.getType() != RobotType.ENLIGHTENMENT_CENTER && nearby_robot.getLocation().equals(RobotPlayer.parent_EC.getLocation())) continue; //don't need to communicate parent EC
                //Friend Enlightenment Center found
                Friend_EC_Info friend_ec = new Friend_EC_Info();
                friend_ec.setPosition(nearby_robot.getLocation());
                friend_ec.setInfluence(nearby_robot.getInfluence());

                RobotPlayer.addECInfo(friend_ec);
            }
            if(Clock.getBytecodeNum()-bytecode_before > BYTECODE_LIMIT) break;
        }
        //System.out.println("Bytecode used in UnitComms.lookAroundAfterMovement(): " + (Clock.getBytecodeNum()-bytecode_before));
    }

    public static void receivedECBroadcast(Neutral_EC_Info neutral_ec){
        if(!communicated_neutral_ecs.contains(neutral_ec.loc)){
            communicated_neutral_ecs.add(neutral_ec.loc);
        }
    }

    public static void receivedECBroadcast(Enemy_EC_Info enemy_ec){
        if(!communicated_enemy_ecs.contains(enemy_ec.loc)){
            communicated_enemy_ecs.add(enemy_ec.loc);
        }
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

    /**
     * Generates a flag value for a non-EC unit
     * 100 - Neutral EC
     * 010 - Enemy EC
     * 110 - Friend EC
     * 001 - Enemy Muckraker
     * 101 - Enemy Politician
     * 011 - Enemy Slanderer
     * @return value of the flag
     */
    static int generateFlagValue(){
        /*
         * List possible flag values to communicate in order of priority.
         */
        if(rc.getType() == RobotType.MUCKRAKER && Muckraker.muckraker_type == Muckraker.SCOUT){
            NEW_NEUTRAL_EC = 1;
            NEW_ENEMY_EC = 2;
            NEW_FRIEND_EC = 3;
            CLOSEST_ENEMY_ATTACKER = 4;
            NEW_BORDER = 5;
            OLD_NEUTRAL_EC = 6;
            OLD_ENEMY_EC = 7;
            OLD_FRIEND_EC = 8;
        }
        else{
            if(Math.random() < 0.25){
                NEW_NEUTRAL_EC = 1;
                NEW_ENEMY_EC = 2;
                NEW_FRIEND_EC = 3;
                OLD_NEUTRAL_EC = 4;
                OLD_ENEMY_EC = 5;
                OLD_FRIEND_EC = 6;
                CLOSEST_ENEMY_ATTACKER = 7;
                NEW_BORDER = 8;
            }
            else{
                NEW_NEUTRAL_EC = 1;
                NEW_ENEMY_EC = 2;
                NEW_FRIEND_EC = 3;
                CLOSEST_ENEMY_ATTACKER = 4;
                NEW_BORDER = 5;
                OLD_NEUTRAL_EC = 6;
                OLD_ENEMY_EC = 7;
                OLD_FRIEND_EC = 8;
            }

        }


        for(int priority = 1; priority <= 20; priority++){
            if(priority == NEW_NEUTRAL_EC){
                for(Neutral_EC_Info neutral_ec: RobotPlayer.neutral_ecs){
                    if(!rc.canSenseLocation(neutral_ec.loc) || neutral_ec.influence == -1) continue; //make sure you are communicating seen, correct info

                    if(!communicated_neutral_ecs.contains(neutral_ec.loc)){
                        communicated_enemy_ecs.removeIf(ec_loc -> (ec_loc.equals(neutral_ec.loc)));
                        communicated_friend_ecs.removeIf(ec_loc -> (ec_loc.equals(neutral_ec.loc)));
                        communicated_neutral_ecs.add(neutral_ec.loc);
                        System.out.println("Communicated a neutral EC");
                        return neutral_ec.toFlagValue();
                    }
                }
            }
            else if(priority == NEW_ENEMY_EC){
                for(Enemy_EC_Info enemy_ec: RobotPlayer.enemy_ecs){
                    if(!rc.canSenseLocation(enemy_ec.loc) || enemy_ec.influence == -1) continue; //make sure you are communicating seen, correct info

                    if(!communicated_enemy_ecs.contains(enemy_ec.loc)){
                        communicated_neutral_ecs.removeIf(ec_loc -> (ec_loc.equals(enemy_ec.loc)));
                        communicated_friend_ecs.removeIf(ec_loc -> (ec_loc.equals(enemy_ec.loc)));
                        communicated_enemy_ecs.add(enemy_ec.loc);
                        System.out.println("Communicated a enemy EC");
                        return enemy_ec.toFlagValue();
                    }
                }
            }
            else if(priority == NEW_FRIEND_EC){
                for(Friend_EC_Info friend_ec: RobotPlayer.friend_ecs){
                    if(!rc.canSenseLocation(friend_ec.loc) || friend_ec.influence == -1) continue; //make sure you are communicating seen, correct info

                    if(!communicated_friend_ecs.contains(friend_ec.loc)){
                        communicated_neutral_ecs.removeIf(ec_loc -> (ec_loc.equals(friend_ec.loc)));
                        communicated_enemy_ecs.removeIf(ec_loc -> (ec_loc.equals(friend_ec.loc)));
                        communicated_friend_ecs.add(friend_ec.loc);
                        System.out.println("Communicated a friend EC");
                        return friend_ec.toFlagValue();
                    }
                }
            }
            else if(priority == OLD_NEUTRAL_EC){
                for(Neutral_EC_Info neutral_ec: RobotPlayer.neutral_ecs){
                    if(!rc.canSenseLocation(neutral_ec.loc) || neutral_ec.influence == -1) continue; //make sure you are communicating seen, correct info
                    System.out.println("Communicated a neutral EC");
                    return neutral_ec.toFlagValue();
                }
            }
            else if(priority == OLD_ENEMY_EC){
                for(Enemy_EC_Info enemy_ec: RobotPlayer.enemy_ecs){
                    if(!rc.canSenseLocation(enemy_ec.loc) || enemy_ec.influence == -1) continue; //make sure you are communicating seen, correct info
                    System.out.println("Communicated a enemy EC");
                    return enemy_ec.toFlagValue();
                }
            }
            else if(priority == OLD_FRIEND_EC){
                for(Friend_EC_Info friend_ec: RobotPlayer.friend_ecs){
                    if(!rc.canSenseLocation(friend_ec.loc) || friend_ec.influence == -1) continue; //make sure you are communicating seen, correct info
                    System.out.println("Communicated a friend EC");
                    return friend_ec.toFlagValue();
                }
            }
            else if(priority == CLOSEST_ENEMY_ATTACKER){
                if(ClosestEnemyAttacker.enemy_exists){
                    System.out.println("Communicated closest enemy attacker");
                    return ClosestEnemyAttacker.toFlagValue();
                }
            }
        }

        return 0;
    }

}

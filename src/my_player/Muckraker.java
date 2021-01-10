package my_player;

import battlecode.common.*;
import java.util.*;

public class Muckraker {
    static RobotController rc;
    static boolean is_scout = true; //whether this Muckraker will be communicating stuff it sees to parent_EC
    static List<MapLocation> communicated_ecs = new ArrayList<MapLocation>();

    static final RobotType[] spawnableRobot = {
            RobotType.POLITICIAN,
            RobotType.SLANDERER,
            RobotType.MUCKRAKER,
    };

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

            RobotInfo[] all_nearby_robots = rc.senseNearbyRobots();

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
        rc = RobotPlayer.rc;

        //Receive Communications (TODO)

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
        if (tryMove(randomDirection()))
            System.out.println("I moved!");

        int flag_value = generateFlagValue();
        if(rc.canSetFlag(flag_value)){
            rc.setFlag(flag_value);
        }
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
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
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

/*
USEFUL INFORMATION;

 *** BOT PARAMETERS ***

 Bit 4..5:
	00 = Defense Politician (Slanderers, default)
  01 = Attack Politician (EC)
	10 = Defense Politician (Spawner EC) [Not implemented]
	11 = Attack Politician (Enemy units) [Not implemented]

*/

package my_player;

import battlecode.common.*;
import java.util.*;

/**
 * Politician controls the actions of our Politicians.
 * @author    Coast
 */
public class Politician {
    static RobotController rc;
    static boolean is_EC_attacker = false;
    static boolean is_slanderer_politician = true; //should be set to false if it is newly made from an EC
    static boolean is_converted_politician = false;

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
     * Check whether parent EC died and updates parent_EC
     * possibly change the way the politician acts now
     */
    private static void updateParentEC() {
        if(!RobotPlayer.has_parent_EC) return;
        if(!rc.canGetFlag(RobotPlayer.parent_EC.getID())){ //parent EC died
            RobotPlayer.has_parent_EC = false; //should never change to true again
        }
    }

<<<<<<< HEAD
//////////////// PARAMETERS

		static int politician_type = 0; // read above. By default, defense politician
		public static final int SLANDERER_DEFENSE = 0;
		public static final int EC_ATTACK = 1;
		public static final int EC_DEFENSE = 2;
		public static final int ENEMY_ATTACK = 3;

		// Type 1 politican parameters
=======
    /**
     * Targeting for Attacker Politicians
     */
>>>>>>> refs/remotes/origin/master
    static boolean hasECTarget = false;
    static int ec_target_type = 0; // 1 = neutral, 2 = enemy
    static Point ec_target = new Point(); //relative location

////////////////


		// Type 0 Politician Functions
		static void doSlandererDefenseAction() throws GameActionException
		/*
			Summary of Algorithm:
			* Copy slanderer pathfinding (to hopefully find them)
			* If you see enemy muckraker within range, use empower
		*/
		{
			RobotInfo[] close_robots = rc.senseNearbyRobots(2); //If there are nearby enemy units, empower
			boolean do_empower = false;
			for(RobotInfo x : close_robots)
			{
				//check whether robot x is an enemy muckraker
				if(x.getTeam() != rc.getTeam() && x.getType() == RobotType.MUCKRAKER)
					do_empower = true;
			}
			if(do_empower)
				if(rc.canEmpower(2)){ // TODO what should we do about empower parameter?
					rc.empower(2);
					return; // successfully empowered
				}

			//otherwise: copy slanderer pathfinding
			tryMove(Slanderer.greedyPathfinding());
			return;
		}

		// Type 1 Politician Functions


    /**
     * Assign an EC target if !hasECTarget
     * reassign EC target if there is a better target
     */
    static void assignECTarget() {
        if(RobotPlayer.neutral_ecs.size() == 0 && RobotPlayer.enemy_ecs.size() == 0){
            //no potential targets
            return;
        }

        if(!hasECTarget){
            //choose closest Neutral
            if(RobotPlayer.neutral_ecs.size() > 0){
                hasECTarget = true;
                ec_target_type = 1;
                ec_target = RobotPlayer.getClosestNeutralECLocation();
            }
            //choose closest Enemy
            if(RobotPlayer.enemy_ecs.size() > 0){
                hasECTarget = true;
                ec_target_type = 2;
                ec_target = RobotPlayer.getClosestEnemyECLocation();
            }
            return;
        }

        if(ec_target_type == 1) return; //already assigned a neutral

        //already assigned an enemy

        //choose closest Neutral
        if(RobotPlayer.neutral_ecs.size() > 0){
            ec_target_type = 1;
            ec_target = RobotPlayer.getClosestNeutralECLocation();
            return;
        }
        //choose closest Enemy
        if(RobotPlayer.enemy_ecs.size() > 0){
            ec_target_type = 2;
            ec_target = RobotPlayer.getClosestEnemyECLocation();
        }
    }

    /*
    EC Attacker
    Decide to Empower or Move to EC
     */
    private static void doECAttackerAction() throws GameActionException {
        if(!hasECTarget){
            if (tryMove(randomDirection()))
                System.out.println("I moved!");
            return;
        }

        Point my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());
        int distance_to_target = Point.getRadiusSquaredDistance(ec_target, my_rel_loc);

        boolean moveAction = false;
        for(Direction dir: directions){
            Point candidate_rel_loc = my_rel_loc.add(dir);
            MapLocation candidate_loc = rc.getLocation().add(dir);
            if(Point.getRadiusSquaredDistance(candidate_rel_loc, ec_target) < distance_to_target){
                if(rc.canSenseLocation(candidate_loc) && !rc.isLocationOccupied(candidate_loc)){
                    moveAction = true;
                }

            }
        }

        if(!moveAction){
            if(rc.canEmpower(distance_to_target)){
                rc.empower(distance_to_target);
            }
            return;
        }

        Movement.assignDestination(ec_target);
        Movement.moveToDestination();
    }


<<<<<<< HEAD
// General Politician Functions

    static void doRandomAction() throws GameActionException
    {
=======
    /**
     * Attacks if it can. Otherwise, move randomly.
     * @throws GameActionException
     */
    private static void doRandomAction() throws GameActionException {
>>>>>>> refs/remotes/origin/master
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        if (tryMove(Slanderer.greedyPathfinding()))// just use slanderer movement
            System.out.println("I moved!");
    }

    /**
     * Controls this Politician
     * @throws GameActionException
     */
    public static void run() throws GameActionException{
        ////////////////////Creation Begin
        rc = RobotPlayer.rc;
        Movement.rc = RobotPlayer.rc;
        //TODO: Consider Slanderer-converted Politicians
        if(RobotPlayer.just_made){
<<<<<<< HEAD
            politician_type = RobotPlayer.assignParentEC(); //after it spawns, record which EC spawned it (if any)
						//Also record type of politician (note this will result in SLANDERER_DEFENSE by default)
=======

            RobotPlayer.assignParentEC(); //after it spawns, record which EC spawned it (if any)

            is_slanderer_politician = false;
            if(RobotPlayer.has_parent_EC){
                is_EC_attacker = true;
            }
>>>>>>> refs/remotes/origin/master

            System.out.println("has_parent_EC: " + RobotPlayer.has_parent_EC);
            if(RobotPlayer.has_parent_EC){
                System.out.println("parent Location: " + RobotPlayer.parent_EC.getLocation());
            }
        }
        ////////////////////Creation End

        ////////////////////Initialization Begin
        updateParentEC();
        if(!RobotPlayer.has_parent_EC){
            is_EC_attacker = false;
            is_slanderer_politician = false;
            is_converted_politician = true;
        }
        ////////////////////Initialization End

        ////////////////////Receive Broadcast Begin
        RobotPlayer.receiveECBroadcast();
        ////////////////////Receive Broadcast End

        ////////////////////Action Begin
        assignECTarget();

        //Do an action (attack or move)
<<<<<<< HEAD
				switch(politician_type)
				{
					case SLANDERER_DEFENSE:
						doSlandererDefenseAction();
						break;
					case EC_ATTACK:
						doECAttackerAction();
						break;
					case EC_DEFENSE:
						doRandomAction(); // TODO Implement
						break;
					case ENEMY_ATTACK:
						doRandomAction(); // TODO Implement
					default:
						break;// or throw some exception
				}
=======
        if(is_EC_attacker){
            doECAttackerAction();
        }
        else{
            doRandomAction();
        }
>>>>>>> refs/remotes/origin/master

        ////////////////////Action End


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

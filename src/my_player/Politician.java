/*
USEFUL INFORMATION;

 *** BOT PARAMETERS ***

 Bit 4..5:
  01 = Attack Politician (attacks neutral/enemy EC)
	10 = Police Politician (defends EC)

*/

package my_player;

import battlecode.common.*;
import java.util.*;

/**
 * Politician controls the actions of our Politicians.
 * @author	Coast
 */
public class Politician {
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
		
	public static final Direction dir[][] = //[x][y]
	{{Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST},
		{Direction.SOUTH, null, Direction.NORTH},
		{Direction.SOUTHEAST, Direction.EAST, Direction.NORTHEAST}};


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

//////////////// PARAMETERS
		public static final int LOST_POLITICIAN = 0;
		public static final int EC_ATTACK = 1;
		public static final int POLICE = 2;
		public static final int MONEY = 3;
		static int politician_type = POLICE; // read above. By default, police politician

		// Type 1 politican parameters
	/**
	 * Targeting for Attacker Politicians
	 */
	static boolean hasECTarget = false;
	static int ec_target_type = 0; // 1 = neutral, 2 = enemy
	static Point ec_target = new Point(); //relative location

////////////////


		// Type 0 Politician Functions
		static void doLostPoliticianAction() throws GameActionException
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

			//otherwise: no move
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
				//System.out.println("I moved!");
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

	/*
	 * IMPLEMENTATION OF POLICE POLITICIAN
	*/

	public static final double HOME_WEIGHT = 1.0; 
	public static final double REPEL_SL = 20.0; 
	public static final double REPEL_EC = 100.0; 
	public static final double REPEL_PT = 60.0; 
	public static final double SPAWN_BLOCK_WEIGHT = -1000.0;
	public static final double CHASE_WEIGHTS = [0.0, 10000.0, 500.0]; 
	public static final int KILL_DISTANCE = 5; 

	public static void doPoliceAction() throws GameActionException
	{
		double score[][] = new double[3][3];// each of the 9 square police can move to. Higher score is better
		MapLocation cur = rc.getLocation();


		//TODO Modify score based on passability
		
		//Modify score to naturally favor moving closer to home RC
		if(RobotPlayer.parent_EC != null)
		{
			MapLocation home = RobotPlayer.parent_EC.getLocation();
			for(int i=-1;i<=1;++i)
				for(int j=-1;j<=1;++j)
				{
					int x=cur.x+i;
					int y=cur.y+j;
					int dist = home.distanceSquaredTo(new MapLocation(x, y));
					if(dist <= 2)
						score[i+1][j+1] += SPAWN_BLOCK_WEIGHT;
					else
						score[i+1][j+1] += HOME_WEIGHT/Math.sqrt(dist+1);
				}
		}

		//Modify score based on nearby robots
		RobotInfo closest_friendly = null;
		int closest_friendly_dist = 1000000;
		for(RobotInfo info : rc.senseNearbyRobots(20, rc.getTeam()))
		{
			int dist = cur.distanceSquaredTo(info.getLocation());
			if(dist < closest_friendly_dist)
			{
				closest_friendly_dist = dist;
				closest_friendly = info;
			}
		}

		//Handle nearest friendly
		if(closest_friendly != null)
		{
			double wt = 0.0;
			switch(closest_friendly.getType()) // MORE PARAMETERS
			{
				case SLANDERER:
					wt = REPEL_SL;
					break;
				case ENLIGHTENMENT_CENTER:
					wt = REPEL_EC;
					break;
				case POLITICIAN:
					wt = REPEL_PT;
					break;
				default:
					break;
			}
			MapLocation loc = closest_friendly.getLocation();
			for(int i=-1;i<=1;++i)
				for(int j=-1;j<=1;++j)
				{
					int x=cur.x+i;
					int y=cur.y+j;
					//treat like magnets
					score[i+1][j+1] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(x, y))); // sub because we want to move away from other politicians
				}
		}

		//Handle nearest enemy
		if(ClosestEnemyAttacker.enemy_exists)
		{
			double wt = CHASE_WEIGHTS[ClosestEnemyAttacker.enemy_type];
			MapLocation loc = ClosestEnemyAttacker.enemy_position;
			for(int i=-1;i<=1;++i)
				for(int j=-1;j<=1;++j)
				{
					int x=cur.x+i;
					int y=cur.y+j;
					score[i+1][j+1] -= loc.distanceSquaredTo(new MapLocation(x, y))*wt; // subtract because we want to move to smaller distance
				}
		}

		//AFTER SCORES ARE COMPUTED: DETERMINE ACTION

		//Kill muckraker
		if(closest_muckraker_dist < KILL_DISTANCE) // action radius is 9
			if(rc.canEmpower(KILL_DISTANCE))
			{
				rc.empower(KILL_DISTANCE);
				return;
			}

		//Move
		double best = score[1][1]-1;
		Direction to_go = null;
		for(int i=-1;i<=1;++i)
			for(int j=-1;j<=1;++j)
				if(best < score[i+1][j+1])
				{
					best = score[i+1][j+1];
					to_go = dir[i+1][j+1];
				}
		if(to_go != null)
			if(rc.canMove(to_go))
			{
				rc.move(to_go);
				return;
			}
	}

// General Politician Functions

	/**
	 * Attacks if it can. Otherwise, move randomly.
	 * @throws GameActionException
	 */
	private static void doRandomAction() throws GameActionException {
		Team enemy = rc.getTeam().opponent();
		int actionRadius = rc.getType().actionRadiusSquared;
		RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
		if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
			//System.out.println("empowering...");
			rc.empower(actionRadius);
			//System.out.println("empowered");
			return;
		}
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
			int additional_info = RobotPlayer.assignParentEC(); //after it spawns, record which EC spawned it (if any)
			politician_type = additional_info % 4;
			if(politician_type == Politician.EC_ATTACK){
				if(RobotPlayer.getBitsBetween(additional_info, 0, 0) == 1){
					//ec assigned a target
					hasECTarget = true;
					ec_target = RobotPlayer.convertFromFlagRelativeLocation(RobotPlayer.getBitsBetween(additional_info, 6, 19));
					ec_target_type = 1;
					System.out.println("ec_target: " + ec_target);
				}
			}
						// ASSERT politician_type != -1
						if(politician_type == -1) politician_type = Politician.LOST_POLITICIAN; // if no parent EC make it LOST_POLITICIAN

						//Also record type of politician (note this will result in LOST_POLITICIAN by default)

						/*
			System.out.println("has_parent_EC: " + RobotPlayer.has_parent_EC);
			if(RobotPlayer.has_parent_EC){
				System.out.println("parent Location: " + RobotPlayer.parent_EC.getLocation());
			}
						*/
		}
		////////////////////Creation End

		////////////////////Initialization Begin
		updateParentEC();
		if(!RobotPlayer.has_parent_EC){
			politician_type = Politician.LOST_POLITICIAN;
		}
		////////////////////Initialization End

		////////////////////Receive Broadcast Begin
		RobotPlayer.receiveECBroadcast();
		////////////////////Receive Broadcast End

		UnitComms.lookAroundBeforeMovement();
		////////////////////Action Begin
		doMoneyAction(); //does money action if possible

		if(politician_type == Politician.EC_ATTACK)
			assignECTarget();

		int bytecode_before = Clock.getBytecodeNum();
		//Do an action (attack or move)
		switch(politician_type)
		{
			case LOST_POLITICIAN:
				doLostPoliticianAction();
				break;
			case EC_ATTACK:
				doECAttackerAction();
				break;
			case POLICE:
				doPoliceAction();
				break;
			case MONEY:
				doMoneyAction();
				break;
			default:
				break;// or throw some exception
		}
		System.out.println("movement bytecode: " + (Clock.getBytecodeNum()-bytecode_before));

		////////////////////Action End

		UnitComms.lookAroundAfterMovement();
		ClosestEnemyAttacker.forgetOldInfo(); //forgets old info to make sure we don't communicate something ambiguous
		int flag_value = UnitComms.generateFlagValue();
		if(rc.canSetFlag(flag_value)){
			rc.setFlag(flag_value);
		}

	}
	
	public static void doMoneyAction() throws GameActionException {
		if(rc.getEmpowerFactor(rc.getTeam(), 0) >= 10.0){
			for(Direction dir: directions){
				RobotInfo possible_ec = rc.senseRobotAtLocation(rc.getLocation().add(dir));
				if(possible_ec != null){
					if(possible_ec.getType() == RobotType.ENLIGHTENMENT_CENTER){
						rc.empower(rc.getLocation().distanceSquaredTo(possible_ec.getLocation()));
					}
				}
			}
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
	 * Attempts to move in a given direction.
	 *
	 * @param dir The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryMove(Direction dir) throws GameActionException {
		//System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		} else return false;
	}
}

/*
USEFUL INFORMATION;

 *** BOT PARAMETERS ***

 Bit 4..5:
  01 = Attack Politician (attacks neutral/enemy EC)
	10 = Police Politician (defends EC)

*/

package pold;

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
	static int politician_type = EC_ATTACK; // read above. By default, EC attack

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
		RobotInfo[] close_robots = rc.senseNearbyRobots(4); //If there are nearby enemy units, empower
		boolean do_empower = false;
		for(RobotInfo x : close_robots)
		{
			//check whether robot x is an enemy muckraker
			if(x.getTeam() != rc.getTeam() && x.getType() == RobotType.MUCKRAKER)
				do_empower = true;
			if(x.getType() == RobotType.ENLIGHTENMENT_CENTER)
				do_empower = true;
		}
		if(do_empower) {
			if (rc.canEmpower(4)) { // TODO what should we do about empower parameter?
				rc.empower(4);
				return; // successfully empowered
			}
		}
		//otherwise: no move
		tryMove(randomDirection());
		return;
	}

	// Type 1 Politician Functions

	/**
	 * Empower if you are a politician scout
	 */
	static void tryPoliticianScoutEmpower(){

	}

	/**
	 * Attacker Politicians without a target
	 * @throws GameActionException
	 */
	static Point scout_destination = null;
	static void doPoliticianScoutAction() throws GameActionException
	{
		System.out.println("Doing Politician Scout Action");

		tryPoliticianScoutEmpower();

		if(scout_destination == null){
			scout_destination = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());
			Movement.assignDestination(scout_destination);
		}

		if(ClosestEnemyAttacker.enemy_exists){
			Point potential_destination = RobotPlayer.convertToRelativeCoordinates(ClosestEnemyAttacker.enemy_position);
			int current_dist = (scout_destination.x*scout_destination.x)+(scout_destination.y*scout_destination.y);
			int potential_dist = (potential_destination.x*potential_destination.x)+(potential_destination.y*potential_destination.y);
			if(potential_dist > current_dist){
				scout_destination = potential_destination;
				Movement.assignDestination(scout_destination);
			}
		}

		if(!Movement.moved_to_destination){
			Movement.moveToDestination();
		}
		else{
			doPoliceAction();
		}
	}

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
		if(hasECTarget){
			//ec target might be friendly
			for(Friend_EC_Info friend_ec: RobotPlayer.friend_ecs){
				//System.out.println("Friend ec at: " + friend_ec.rel_loc);
				if(friend_ec.rel_loc.equals(ec_target)){
					//System.out.println("Found friend_ec at " + friend_ec.loc + ", reassigning target");
					hasECTarget = false;
					assignECTarget(); //assign a new target
					break;
				}
			}
		}
		else{
			assignECTarget();
		}

		if(!hasECTarget){
			doPoliticianScoutAction();
			//System.out.println("I moved!");
			return;
		}

		//System.out.println("My target: " + ec_target);

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
			int num_nearby_enemies = 0;
			for(RobotInfo r : rc.senseNearbyRobots(9)) {
				if(r.getTeam() == rc.getTeam().opponent()){
					num_nearby_enemies++;
					if(r.getType() == RobotType.ENLIGHTENMENT_CENTER){
						num_nearby_enemies = 100;
					}
				}

			}

			int radius = Math.min(distance_to_target, 9);

			if(distance_to_target <= 9 || num_nearby_enemies > 3) {
				if(rc.canEmpower(radius)){
					rc.empower(radius);
				}
			}
			return;
		}

		if(rc.canSenseRadiusSquared(distance_to_target) && rc.senseNearbyRobots(distance_to_target).length == 1){
			if(rc.canEmpower(distance_to_target)){
				rc.empower(distance_to_target);
			}

		}

		Movement.assignDestination(ec_target);
		Movement.moveToDestination();
	}

	/*
	 * IMPLEMENTATION OF POLICE POLITICIAN
	 */

	public static final double HOME_WEIGHT = 1.0;
	public static final double REPEL_SL = 1.0;
	public static final double REPEL_EC = 100.0;
	public static final double REPEL_PT = 100.0;
	public static final double SPAWN_BLOCK_WEIGHT = -100.0;
	public static final double CHASE_WEIGHTS[] = {0.0, -1000.0, -500.0};
	public static final double INF = 1e12;
	public static final int MAX_KILL_DIST = 5;

	public static void doPoliceAction() throws GameActionException
	{
		double score[][] = new double[3][3];// each of the 9 square police can move to. Higher score is better
		int cnt[] = new int[10];
		MapLocation cur = rc.getLocation();


		//TODO Modify score based on passability

		//Modify score to naturally favor moving closer to home RC
		if(rc.canGetFlag(RobotPlayer.parent_EC.getID())) { // parent EC alive
			MapLocation home = RobotPlayer.parent_EC.getLocation();
			int dist;
			dist = home.distanceSquaredTo(new MapLocation(cur.x-1, cur.y-1));if(dist <= 2)score[0][0] += SPAWN_BLOCK_WEIGHT;else score[0][0] -= HOME_WEIGHT*Math.sqrt(dist+1);
			dist = home.distanceSquaredTo(new MapLocation(cur.x-1, cur.y));if(dist <= 2)score[0][1] += SPAWN_BLOCK_WEIGHT;else score[0][1] -= HOME_WEIGHT*Math.sqrt(dist+1);
			dist = home.distanceSquaredTo(new MapLocation(cur.x-1, cur.y+1));if(dist <= 2)score[0][2] += SPAWN_BLOCK_WEIGHT;else score[0][2] -= HOME_WEIGHT*Math.sqrt(dist+1);
			dist = home.distanceSquaredTo(new MapLocation(cur.x, cur.y-1));if(dist <= 2)score[1][0] += SPAWN_BLOCK_WEIGHT;else score[1][0] -= HOME_WEIGHT*Math.sqrt(dist+1);
			dist = home.distanceSquaredTo(new MapLocation(cur.x, cur.y));if(dist <= 2)score[1][1] += SPAWN_BLOCK_WEIGHT;else score[1][1] -= HOME_WEIGHT*Math.sqrt(dist+1);
			dist = home.distanceSquaredTo(new MapLocation(cur.x, cur.y+1));if(dist <= 2)score[1][2] += SPAWN_BLOCK_WEIGHT;else score[1][2] -= HOME_WEIGHT*Math.sqrt(dist+1);
			dist = home.distanceSquaredTo(new MapLocation(cur.x+1, cur.y-1));if(dist <= 2)score[2][0] += SPAWN_BLOCK_WEIGHT;else score[2][0] -= HOME_WEIGHT*Math.sqrt(dist+1);
			dist = home.distanceSquaredTo(new MapLocation(cur.x+1, cur.y));if(dist <= 2)score[2][1] += SPAWN_BLOCK_WEIGHT;else score[2][1] -= HOME_WEIGHT*Math.sqrt(dist+1);
			dist = home.distanceSquaredTo(new MapLocation(cur.x+1, cur.y+1));if(dist <= 2)score[2][2] += SPAWN_BLOCK_WEIGHT;else score[2][2] -= HOME_WEIGHT*Math.sqrt(dist+1);
		}

		//Modify score based on nearby robots
		RobotInfo closest_enemy = null;
		int closest_enemy_dist = 1000000;
		for(RobotInfo info : rc.senseNearbyRobots(20))
			if(info.getTeam() == rc.getTeam()) // same team
			{
				double wt = 0.0;
				switch(info.getType()) // MORE PARAMETERS
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
				MapLocation loc = info.getLocation();
				int dist = loc.distanceSquaredTo(cur);
				score[0][0] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x-1, cur.y-1)));
				score[0][1] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x-1, cur.y)));
				score[0][2] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x-1, cur.y+1)));
				score[1][0] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x, cur.y-1)));
				score[1][1] -= wt/Math.sqrt(1+(double)dist);
				score[1][2] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x, cur.y+1)));
				score[2][0] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x+1, cur.y-1)));
				score[2][1] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x+1, cur.y)));
				score[2][2] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x+1, cur.y+1)));
				if(dist <= MAX_KILL_DIST)
					++ cnt[dist];
			}
			else // enemy team
			{
				int new_dist = info.getLocation().distanceSquaredTo(cur);
				switch(info.getType())
				{
					case MUCKRAKER:
					case POLITICIAN:
						if(closest_enemy == null || new_dist < closest_enemy_dist)
						{
							closest_enemy = info;
							closest_enemy_dist = new_dist;
						}
						break;
					default:
						break;
				}
				if(new_dist <= MAX_KILL_DIST)
					++ cnt[new_dist];
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
					score[i+1][j+1] -= wt/Math.sqrt(loc.distanceSquaredTo(new MapLocation(x, y))+1); // subtract because we want to move to smaller distance
				}
		}

		//AFTER SCORES ARE COMPUTED: DETERMINE ACTION

		//Kill enemy unit
		if(closest_enemy != null)
		{
			for(int i=0;i<MAX_KILL_DIST;++i)
				cnt[i+1] += cnt[i];
			int dist = rc.getLocation().distanceSquaredTo(closest_enemy.getLocation());
			int to_emp = 0;
			if(dist == 1) to_emp = 1; // if distance == 1, then you have to empower

			for(int i=dist;i <= MAX_KILL_DIST;++i) // if can insta, increase radius
				if(((int)((rc.getConviction()-10)*rc.getEmpowerFactor(rc.getTeam(), 0)))/cnt[i] > closest_enemy.getConviction())
					to_emp = i;

			if(to_emp > 0)
				if(rc.canEmpower(to_emp))
				{
					rc.empower(to_emp);
					return;
				}
		}

		//Move (should take at most (9 * (4 + 9*5 + 2)) = 459 bytecodes
		while(true)
		{
			double best = score[1][1]-1;
			int bi=-1, bj=-1;
			Direction to_go = null;
			for(int i=-1;i<=1;++i)
				for(int j=-1;j<=1;++j)
					if(best < score[i+1][j+1])
					{
						best = score[i+1][j+1];
						bi=i+1; bj=j+1;
						to_go = dir[i+1][j+1];
					}
			if(to_go == null) // no movement
				return;
			if(rc.canMove(to_go))
			{
				rc.move(to_go);
				return;
			}
			else
				score[bi][bj] -= INF;
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
			if(RobotPlayer.has_parent_EC){
				politician_type = additional_info % 4;
			}
			else{
				politician_type = LOST_POLITICIAN;
			}

			if(politician_type == Politician.EC_ATTACK){
				if(RobotPlayer.getBitsBetween(additional_info, 2, 2) == 1){
					//ec assigned a target
					hasECTarget = true;
					ec_target = RobotPlayer.convertFromFlagRelativeLocation(RobotPlayer.getBitsBetween(additional_info, 6, 19));
					ec_target_type = 1;
					//System.out.println("ec_target: " + ec_target);
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

		/*if(politician_type == Politician.LOST_POLITICIAN){
			System.out.println("I am a lost politician");
		}*/
		////////////////////Initialization End

		//System.out.println("I am a politician of type: " + politician_type);

		////////////////////Receive Broadcast Begin
		RobotPlayer.receiveECBroadcast();
		////////////////////Receive Broadcast End

		UnitComms.lookAroundBeforeMovement();
		////////////////////Action Begin
		//doMoneyAction(); No longer applicable with specs change


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
			default:
				break;// or throw some exception
		}
		//System.out.println("movement bytecode: " + (Clock.getBytecodeNum()-bytecode_before));

		////////////////////Action End

		UnitComms.lookAroundAfterMovement();
		ClosestEnemyAttacker.forgetOldInfo(); //forgets old info to make sure we don't communicate something ambiguous
		int flag_value = UnitComms.generateFlagValue();
		if(rc.canSetFlag(flag_value)){
			rc.setFlag(flag_value);
		}

	}
	
	/*public static void doMoneyAction() throws GameActionException {
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

	}*/

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

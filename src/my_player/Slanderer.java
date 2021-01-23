package my_player;

import battlecode.common.*;
import java.util.*;
import java.lang.Math.*;

/**
 * Controls our slanderers.
 */
public class Slanderer {
	static RobotController rc;

	public static final float EPS = 0.1f;
	public static final float PI = (float)(Math.atan(1)*4);

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
	 * Check whether parent EC died
     */
	static void updateParentEC() {
		if(!RobotPlayer.has_parent_EC) return;
		if(!rc.canGetFlag(RobotPlayer.parent_EC.getID())){ //parent EC died
			RobotPlayer.has_parent_EC = false; //should never change to true again
		}
	}

	private static int generateFlagValue(){
		return 0;
	}

	public static void run() throws GameActionException{
		////////////////////Creation Begin
		if(RobotPlayer.just_made){
			rc = RobotPlayer.rc;
			RobotPlayer.assignParentEC(); //after it spawns, record which EC spawned it (if any)

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
		UnitComms.BYTECODE_LIMIT = 700;
		System.out.println("Bytecode before lookaround1: " + Clock.getBytecodeNum());
		UnitComms.lookAroundBeforeMovement();
		System.out.println("Bytecode after lookaround2: " + Clock.getBytecodeNum());
		////////////////////Initialization End

		////////////////////Receive Broadcast Begin
		RobotPlayer.receiveECBroadcast();
		////////////////////Receive Broadcast End

		////////////////////Movement Begin
		System.out.println("Bytecode before movement: " + Clock.getBytecodeNum());
		moveAction();
		System.out.println("Bytecode after movement: " + Clock.getBytecodeNum());
		////////////////////Movement End

		UnitComms.lookAroundAfterMovement();
		ClosestEnemyAttacker.forgetOldInfo(); //forgets old info to make sure we don't communicate something ambiguous
		int bytecode_before = Clock.getBytecodeNum();
		int flag_value = UnitComms.generateFlagValue();
		System.out.println("Bytecode used on generateFlagValue(): " + (Clock.getBytecodeNum()-bytecode_before));
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

	// FROM POLICE POLITICIAN MOVEMENT

	public static final double LATTICE_PREF = 2.0;
	public static final double HOME_WEIGHT = 1.0; // proportional to dist
	public static final double REPEL_SL = 0.0; // inverse to dist
	public static final double REPEL_EC = 0.0; // inverse to dist
	public static final double REPEL_PT = 25.0; // inverse to dist
	public static final double SPAWN_BLOCK_WEIGHT = -1000.0;
	public static final double CHASE_WEIGHTS[] = {0.0, 200.0, 100.0};  // tendency to move away from enemy muckrakers (inversely proportional)
	public static final double INF = 1e12;
	public static double score[][] = new double[3][3];// each of the 9 squares it can move to. Higher score is better

	public static void moveAction() throws GameActionException
	{
		//int cnt = Clock.getBytecodeNum(), ncnt=-1;
		MapLocation cur = rc.getLocation();
		if((cur.x+cur.y&1)==1) // preference to odd tiles
		{
			score[0][0]=score[0][2]=score[1][1]=score[2][0]=score[2][2]=LATTICE_PREF;
			score[0][1]=score[1][0]=score[1][2]=score[2][1]=0;
		}
		else
		{
			score[0][0]=score[0][2]=score[1][1]=score[2][0]=score[2][2]=0;
			score[0][1]=score[1][0]=score[1][2]=score[2][1]=LATTICE_PREF;
		}
		
		//Modify score to naturally favor moving closer to home RC

		if(RobotPlayer.parent_EC != null)
		{
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

		//ncnt = Clock.getBytecodeNum(); System.out.println("Process home EC: " + (ncnt - cnt)); cnt = ncnt;

		//Retrieve nearest robot
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

		//ncnt = Clock.getBytecodeNum(); System.out.println("Retrieve nearest friendly: " + (ncnt - cnt)); cnt = ncnt;

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
			score[0][0] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x-1, cur.y-1)));
			score[0][1] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x-1, cur.y)));
			score[0][2] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x-1, cur.y+1)));
			score[1][0] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x, cur.y-1)));
			score[1][1] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x, cur.y)));
			score[1][2] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x, cur.y+1)));
			score[2][0] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x+1, cur.y-1)));
			score[2][1] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x+1, cur.y)));
			score[2][2] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x+1, cur.y+1)));
		}

		//ncnt = Clock.getBytecodeNum(); System.out.println("Handle nearest friendly: " + (ncnt - cnt)); cnt = ncnt;

		//Handle nearest enemy
		if(ClosestEnemyAttacker.enemy_exists)
		{
			double wt = CHASE_WEIGHTS[ClosestEnemyAttacker.enemy_type];
			MapLocation loc = ClosestEnemyAttacker.enemy_position;
			score[0][0] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x-1, cur.y-1)));
			score[0][1] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x-1, cur.y)));
			score[0][2] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x-1, cur.y+1)));
			score[1][0] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x, cur.y-1)));
			score[1][1] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x, cur.y)));
			score[1][2] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x, cur.y+1)));
			score[2][0] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x+1, cur.y-1)));
			score[2][1] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x+1, cur.y)));
			score[2][2] -= wt/Math.sqrt(1+(double)loc.distanceSquaredTo(new MapLocation(cur.x+1, cur.y+1)));
		}

		//ncnt = Clock.getBytecodeNum(); System.out.println("Handle nearest enemy: " + (ncnt - cnt)); cnt = ncnt;
		
		for(int z=0;z<3;++z) // try 3 best locations
		{
			int bi=0, bj=0;
			if(score[0][1]>score[bi][bj]) {bi=0; bj=1;}
			if(score[0][2]>score[bi][bj]) {bi=0; bj=2;}
			if(score[1][0]>score[bi][bj]) {bi=1; bj=0;}
			if(score[1][1]>score[bi][bj]) {bi=1; bj=1;}
			if(score[1][2]>score[bi][bj]) {bi=1; bj=2;}
			if(score[2][0]>score[bi][bj]) {bi=2; bj=0;}
			if(score[2][1]>score[bi][bj]) {bi=2; bj=1;}
			if(score[2][2]>score[bi][bj]) {bi=2; bj=2;}

			Direction to_go = dir[bi][bj];
			if(to_go == null) // no movement
				return;
			if(rc.canMove(to_go))
			{
				rc.move(to_go);
				return;
			}
			else
				score[bi][bj] -= INF;
			//ncnt = Clock.getBytecodeNum(); System.out.println("Try move & failed: " + (ncnt - cnt)); cnt = ncnt;
		}
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

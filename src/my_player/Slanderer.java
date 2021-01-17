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
		UnitComms.lookAround();
		////////////////////Initialization End

		////////////////////Receive Broadcast Begin
		RobotPlayer.receiveECBroadcast();
		////////////////////Receive Broadcast End

		////////////////////Movement Begin
		System.out.println("Bytecode before movement: " + Clock.getBytecodeNum());
		moveAction();
		System.out.println("Bytecode after movement: " + Clock.getBytecodeNum());
		////////////////////Movement End

		RobotPlayer.updateEnemyUnitList(); //make sure we don't communicate something ambiguous
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

	// FROM POLICE POLITICIAN MOVEMENT

	public static final double HOME_WEIGHT = 1.0; 
	public static final double REPEL_SL = 20.0; 
	public static final double REPEL_EC = 20.0; 
	public static final double REPEL_PT = 20.0; 
	public static final double CHASE_WEIGHT = -1000.0;  // tendency to move towards enemy muckrakers

	public static void moveAction() throws GameActionException
	{
		double score[][] = new double[3][3];// each of the 9 square police can move to. Higher score is better
		MapLocation cur = rc.getLocation();
		
		//Modify score to naturally favor moving closer to home RC
		{
			MapLocation home = RobotPlayer.parent_EC.getLocation();
			for(int i=-1;i<=1;++i)
				for(int j=-1;j<=1;++j)
				{
					int x=cur.x+i;
					int y=cur.y+j;
					score[i+1][j+1] -= home.distanceSquaredTo(new MapLocation(x, y))*HOME_WEIGHT; // subtract because we favor shorter locations
				}
		}

		//Modify score based on nearby robots
		RobotInfo closest_muckraker = null;
		int closest_muckraker_dist = 1000000;
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
				for(int i=-1;i<=1;++i)
					for(int j=-1;j<=1;++j)
					{
						int x=cur.x+i;
						int y=cur.y+j;
						//treat like magnets
						score[i+1][j+1] -= wt/Math.sqrt((double)loc.distanceSquaredTo(new MapLocation(x, y))); // sub because we want to move away from other politicians
					}
			}
			else // enemy team
				switch(info.getType())
				{
					case MUCKRAKER:
						int new_dist = info.getLocation().distanceSquaredTo(cur);
						if(closest_muckraker == null || new_dist < closest_muckraker_dist)
						{
							closest_muckraker = info;
							closest_muckraker_dist = new_dist;
						}
						break;
					default:
						break;
			}

		//Modify score to chase nearby muckraker
		//TODO Can change this to weighting ALL muckrakers as well
		if(closest_muckraker != null)
		{
			MapLocation loc = closest_muckraker.getLocation();
			for(int i=-1;i<=1;++i)
				for(int j=-1;j<=1;++j)
				{
					int x=cur.x+i;
					int y=cur.y+j;
					score[i+1][j+1] -= loc.distanceSquaredTo(new MapLocation(x, y))*CHASE_WEIGHT; // subtract because we want to move to smaller distance
				}
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

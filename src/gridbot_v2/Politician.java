package gridbot_v2;

import battlecode.common.*;
import gridbot_v2.common.Communication;
import gridbot_v2.common.Displacement;
import gridbot_v2.common.Sensing;
import gridbot_v2.common.localPathfinding;

public class Politician {
	public static RobotController rc;

	public static void turnOne() throws GameActionException{
		rc = RobotPlayer.rc;
	}
	
	public static void run() throws GameActionException
	{
		if(rc.senseNearbyRobots(100,rc.getTeam().opponent()).length>0)
		{
			doPoliceAction();
			return;
		}
		Communication.gridBotInfo nearestMessageGroup0 = null;
		int nearestMessageGroup0Distance = 1000;
		for (Communication.gridBotInfo grid : Sensing.nearByGrid) {
			if (grid.haveGridBot&&grid.message.haveMessageGroup0 && Displacement.getMaxXYDistance(Sensing.currentLocation, grid.location) < nearestMessageGroup0Distance) {
				nearestMessageGroup0 = grid;
				nearestMessageGroup0Distance = Displacement.getMaxXYDistance(Sensing.currentLocation, grid.location);
			}
		}
		if (nearestMessageGroup0 != null) {
			localPathfinding.pathFindTo(
					Displacement.subtract(
							Displacement.add(nearestMessageGroup0.location,Displacement.scale(nearestMessageGroup0.message.attackDirection,2)),
							Sensing.currentLocation
					)
			);
			return;
		}
		doPoliceAction();
	}


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

	//////////////// PARAMETERS
	public static final int LOST_POLITICIAN = 0;
	public static final int EC_ATTACK = 1;
	public static final int POLICE = 2;
	public static final int MONEY = 3;
	static int politician_type = POLICE; // read above. By default, police politician



	/*
	 * IMPLEMENTATION OF POLICE POLITICIAN
	 */

	public static final double HOME_WEIGHT = 1.0;
	public static final double REPEL_SL = 20.0;
	public static final double REPEL_EC = 100.0;
	public static final double REPEL_PT = 60.0;
	public static final double CHASE_WEIGHT = 1000.0;
	public static final int KILL_DISTANCE = 5;

	public static void doPoliceAction() throws GameActionException
	{
		double score[][] = new double[3][3];// each of the 9 square police can move to. Higher score is better
		MapLocation cur = rc.getLocation();


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

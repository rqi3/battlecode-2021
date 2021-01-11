package my_player;

import battlecode.common.*;
import java.util.*;
import java.lang.Math.*;

public class Slanderer {
	static RobotController rc;

	public static final float EPS = 0.1f;
	public static final float PI = (float)(Math.atan(1)*4);

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


	public static void run() throws GameActionException{
		rc = RobotPlayer.rc;
		//Receive broadcast from parent_EC
		RobotPlayer.receiveECBroadcast();

		//Movement
		if (tryMove(greedyPathfinding()))
			System.out.println("I moved!");
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
	 * Returns the direction away from all enemy ECs
	 *
	 * @return a greedy Direction, or randomly if no enemy ecs are known
	 */
	static Direction greedyPathfinding()
	{
		List<Enemy_EC_Info> enemy_ecs = RobotPlayer.enemy_ecs;
		if(enemy_ecs.isEmpty())
			return randomDirection();
		Vec dir = new Vec();
		for(Enemy_EC_Info ec : enemy_ecs)
		{
			Vec ec_dir = new Vec();//formula for this_position - ec_position??? (depends on relative/absolute)
			float mag2 = ec_dir.mag2();
			if(mag2 > EPS)
				ec_dir.mul(1/mag2);
			dir.add(ec_dir);
		}
		if(dir.mag2() < EPS)
			return randomDirection(); // OR RETURN NO MOVEMENT!!!

		float ang = dir.ang(); // hardcoding cancer
		if(ang < PI/8) return Direction.EAST;
		if(ang < 3*PI/8) return Direction.NORTHEAST;
		if(ang < 5*PI/8) return Direction.NORTH;
		if(ang < 7*PI/8) return Direction.NORTHWEST;
		if(ang < 9*PI/8) return Direction.WEST;
		if(ang < 11*PI/8) return Direction.SOUTHWEST;
		if(ang < 13*PI/8) return Direction.SOUTH;
		if(ang < 15*PI/8) return Direction.SOUTHEAST;
		return Direction.EAST;
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

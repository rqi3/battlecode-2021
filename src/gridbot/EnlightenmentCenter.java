package gridbot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * EnlightenmentCenter controls the actions of our Enlightenment Centers.
 * @author    Coast
 */
public class EnlightenmentCenter {

	public static RobotController rc;

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
	static final List<Direction> neardirs =Arrays.asList (
			Direction.NORTH,
			Direction.EAST,
			Direction.SOUTH,
			Direction.WEST
			);
	static final List<Direction> diagsdirs = Arrays.asList (
			Direction.NORTHEAST,
			Direction.SOUTHEAST,
			Direction.SOUTHWEST,
			Direction.NORTHWEST
			);

	/**
	 * Updates alive_scout_ids based on whether they are alive
	 */
	public static boolean trySpawn(int influence) throws GameActionException
	{
		Collections.shuffle(neardirs);
		for (Direction dir : neardirs) {
			if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, influence)) {
				rc.buildRobot(RobotType.MUCKRAKER, dir, influence);
				return true;
			}
		}
		Collections.shuffle(diagsdirs);
		for (Direction dir : diagsdirs) {
			if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, influence)) {
				rc.buildRobot(RobotType.MUCKRAKER, dir, influence);
				return true;
			}
		}
		return false;
	}
	public static void turnOne() throws GameActionException{
		rc = RobotPlayer.rc;

		trySpawn(1);
	}
	public static void run() throws GameActionException{

		trySpawn(1);
	}

}

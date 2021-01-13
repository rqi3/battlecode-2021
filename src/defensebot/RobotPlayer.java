package defensebot;
import battlecode.common.*;
import java.util.*;

/**
 * RobotPlayer controls the actions of all robots and contains common methods.
 */
public strictfp class RobotPlayer {
	public static RobotController rc;

	private static final Direction[] directions = {
		Direction.NORTH,
		Direction.NORTHEAST,
		Direction.EAST,
		Direction.SOUTHEAST,
		Direction.SOUTH,
		Direction.SOUTHWEST,
		Direction.WEST,
		Direction.NORTHWEST,
	};

	static boolean just_made = true;
	static int turn_count;


	/**
	 * Computes relative location of loc with respect to parent_EC, assuming that this Robot has a parent enlightenment center
	 */

	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {
		// This is the RobotController object. You use it to perform actions from this robot,
		// and to get information on its current status.
		RobotPlayer.rc = rc;

		turn_count = 0;

		while (true) {
			turn_count += 1;
			// Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
			try {
				// Here, we've separated the controls into a different method for each RobotType.
				// You may rewrite this into your own control structure if you wish.


				switch (rc.getType()) {
					case ENLIGHTENMENT_CENTER: EnlightenmentCenter.run(); break;
					case MUCKRAKER: Muckraker.run(); break;
					case POLITICIAN: Politician.run(); break;
				}
				just_made = false;
				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Tries to move in a specified direction
	 * @param dir direction of movement
	 * @return whether move succeeded
	 * @throws GameActionException Bad battlecode call
	 */
	static boolean tryMove(Direction dir) throws GameActionException {
		System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		} else return false;
	}
}

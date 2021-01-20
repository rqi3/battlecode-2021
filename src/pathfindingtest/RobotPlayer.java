package pathfindingtest;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import pathfindingtest.common.*;
/**
 * RobotPlayer controls the actions of all robots and contains common methods.
 */
public strictfp class RobotPlayer {
	public static RobotController rc;

	public static int turnSinceMade;

	enum missionType
	{
		MuckrackerOnGrid,
		MuckrackerToGrid,
		EnlightenmentCenterGrid
	}



	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;

		turnSinceMade = 0;
		Displacement.init();
		localPathfinding.init();


		switch (rc.getType()) {
			case ENLIGHTENMENT_CENTER: EnlightenmentCenter.turnOne(); break;
			case MUCKRAKER: Muckraker.turnOne(); break;
		}
		Clock.yield();

		while (true) {
			turnSinceMade ++;
			if(turnSinceMade ==200) 	rc.resign();
			try {

				switch (rc.getType()) {
					case ENLIGHTENMENT_CENTER: EnlightenmentCenter.run(); break;
					case MUCKRAKER: Muckraker.run(); break;
				}
				Clock.yield();

			} catch (Exception e) {
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}

package gridbot;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import gridbot.Grid.onGrid;
import gridbot.common.Communication;
import gridbot.common.Sensing;

/**
 * RobotPlayer controls the actions of all robots and contains common methods.
 */
public strictfp class RobotPlayer {
	public static RobotController rc;

	static int turnSinceMade;

	enum missionType
	{
		MuckrackerOnGrid,
		MuckrackerToGrid,
		EnlightenmentCenterGrid
	}

	static missionType currentMission;

	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;

		turnSinceMade = 0;
		Communication.init();
		Sensing.init();
		System.out.println("before turn 1 sensing: "+Clock.getBytecodeNum());
		Sensing.sense(true);
		switch (rc.getType()) {
			case ENLIGHTENMENT_CENTER: EnlightenmentCenter.turnOne(); break;
			case MUCKRAKER: Muckraker.turnOne(); break;
			case POLITICIAN: Politician.turnOne(); break;
		}
		if(Sensing.currentLocation.x%3==1&&Sensing.currentLocation.y%3==1)
			onGrid.run();
		System.out.println("after turn 1 sensing: "+Clock.getBytecodeNum());
		Clock.yield();

		while (true) {
			turnSinceMade ++;
			if(turnSinceMade ==500) 	rc.resign();
			try {
				System.out.println("before first sensing: "+Clock.getBytecodeNum());
				Sensing.sense(true);
				System.out.println("after first sensing: "+Clock.getBytecodeNum());
				switch (rc.getType()) {
					case ENLIGHTENMENT_CENTER: EnlightenmentCenter.run(); break;
					case MUCKRAKER: Muckraker.run(); break;
					case POLITICIAN: Politician.run(); break;
				}
				System.out.println("before second sensing: "+Clock.getBytecodeNum());
				Sensing.sense(false);
				System.out.println("after second sensing: "+Clock.getBytecodeNum());
				if(Sensing.currentLocation.x%3==1&&Sensing.currentLocation.y%3==1)
					onGrid.run();
				Clock.yield();

			} catch (Exception e) {
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
			}
		}
	}
}

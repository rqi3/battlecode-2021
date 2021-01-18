package gridbot_v2;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import gridbot_v2.Grid.*;
import gridbot_v2.common.*;
/**
 * RobotPlayer controls the actions of all robots and contains common methods.
 */
public strictfp class RobotPlayer {
	public static RobotController rc;

	public static int turnSinceMade;
	public static int timeCycle;

	enum missionType
	{
		MuckrackerOnGrid,
		MuckrackerToGrid,
		EnlightenmentCenterGrid
	}

	public static missionType currentMission;


	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {
		RobotPlayer.rc = rc;

		turnSinceMade = 0;
		Communication.init();
		Displacement.init();
		localPathfinding.init();
		Sensing.init();

		onGrid.init();
		toGrid.init();

		timeCycle=Sensing.getTimeCycle();

		//System.out.println("before turn 1 sensing: "+Clock.getBytecodeNum());
		Sensing.senseNearByGrid(true);
		switch (rc.getType()) {
			case ENLIGHTENMENT_CENTER: EnlightenmentCenter.turnOne(); break;
			case MUCKRAKER: Muckraker.turnOne(); break;
			case POLITICIAN: Politician.turnOne(); break;
		}
		if(Displacement.isGrid(Sensing.currentLocation))
			onGrid.run();
		//System.out.println("after turn 1 sensing: "+Clock.getBytecodeNum());
		Communication.turnEnd();
		Clock.yield();

		while (true) {
			timeCycle=(timeCycle+1)%2;
			turnSinceMade ++;
			Communication.setFlag(timeCycle+2);
			if(turnSinceMade ==800) 	rc.resign();
			try {
				//System.out.println("before first sensing: "+Clock.getBytecodeNum());
				Sensing.senseNearByGrid(true);
				//System.out.println("after first sensing: "+Clock.getBytecodeNum());
				switch (rc.getType()) {
					case ENLIGHTENMENT_CENTER: EnlightenmentCenter.run(); break;
					case MUCKRAKER: Muckraker.run(); break;
					case POLITICIAN: Politician.run(); break;
				}
				//System.out.println("before second sensing: "+Clock.getBytecodeNum());
				Sensing.senseNearByGrid(false);
				//System.out.println("after second sensing: "+Clock.getBytecodeNum());
				if(Displacement.isGrid(Sensing.currentLocation))
					onGrid.run();
				Communication.turnEnd();
				Clock.yield();

			} catch (Exception e) {
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}

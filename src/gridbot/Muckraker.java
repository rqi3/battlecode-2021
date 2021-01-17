package gridbot;

import battlecode.common.*;

import java.awt.*;

public class Muckraker {
	public static RobotController rc;

	public static void turnOne() throws GameActionException{
		rc = RobotPlayer.rc;
		RobotPlayer.currentMission= RobotPlayer.missionType.MuckrackerToGrid;
		if(rc.getLocation().x%3==1&&rc.getLocation().y%3==1)
			RobotPlayer.currentMission= RobotPlayer.missionType.MuckrackerOnGrid;
	}

	public static void run() throws GameActionException{
		if(rc.getLocation().x%3==1&&rc.getLocation().y%3==1)
			RobotPlayer.currentMission= RobotPlayer.missionType.MuckrackerOnGrid;
		System.out.println("im a muckracker, and my mission is: "+RobotPlayer.currentMission.toString());

		switch (RobotPlayer.currentMission) {
			case MuckrackerToGrid: gridbot.Grid.toGrid.run(); break;
			case MuckrackerOnGrid: break;
		}

	}

}

package gridbot_v2;

import battlecode.common.*;
import gridbot_v2.common.*;
import gridbot_v2.Grid.*;


public class Muckraker {
	public static RobotController rc;
	static void attackSlanderer() throws GameActionException {
		Team enemy = rc.getTeam().opponent();
		int actionRadius = rc.getType().actionRadiusSquared;
		for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
			if (robot.type.canBeExposed()) {
				// It's a slanderer... go get them!
				if (rc.canExpose(robot.location)) {
					System.out.println("exposed");
					rc.expose(robot.location);
				}
			}
		}
	}

	public static void turnOne() throws GameActionException{
		rc = RobotPlayer.rc;
		RobotPlayer.currentMission= RobotPlayer.missionType.MuckrackerToGrid;
		if(Displacement.isGrid(rc.getLocation()))
			RobotPlayer.currentMission= RobotPlayer.missionType.MuckrackerOnGrid;
	}

	public static void run() throws GameActionException{
		if(Displacement.isGrid(rc.getLocation()))
			RobotPlayer.currentMission= RobotPlayer.missionType.MuckrackerOnGrid;
		System.out.println("im a muckracker, and my mission is: "+RobotPlayer.currentMission.toString());

		attackSlanderer();
		switch (RobotPlayer.currentMission) {
			case MuckrackerToGrid: toGrid.run(); break;
			case MuckrackerOnGrid: break;
		}

	}

}

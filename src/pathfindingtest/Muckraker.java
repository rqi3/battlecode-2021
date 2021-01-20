package pathfindingtest;

import battlecode.common.*;
import pathfindingtest.common.*;

import java.util.ArrayList;
import java.util.Map;


public class Muckraker {
	public static RobotController rc;

	static MapLocation startingLocation;

	public static void turnOne() throws GameActionException{
		rc = RobotPlayer.rc;
		startingLocation=rc.getLocation();
	}
	static ArrayList<MapLocation> pastLocation=new ArrayList<>();

	public static void run() throws GameActionException{
		Displacement target=new Displacement(25,50);
		pastLocation.add(rc.getLocation());

		if(Displacement.add(startingLocation,target).equals(rc.getLocation()))
			rc.resign();
		for(MapLocation ml:pastLocation)
			rc.setIndicatorDot(ml,255,0,0);
		localPathfinding.pathFindTo(Displacement.subtract(Displacement.add(startingLocation,target),rc.getLocation()));

	}

}

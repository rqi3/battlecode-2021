package defensebot;

import battlecode.common.*;

public class Muckraker {
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
	static final Direction[][] cordtodir={
			{Direction.SOUTHWEST,Direction.WEST,Direction.NORTHWEST},
			{Direction.SOUTH,Direction.CENTER,Direction.NORTH},
			{Direction.SOUTHEAST,Direction.EAST,Direction.NORTHEAST}
	};
	/*
	{
		{Direction.NORTHWEST,Direction.NORTH,Direction.NORTHEAST},
		{Direction.WEST,Direction.CENTER,Direction.EAST},
		{Direction.SOUTHWEST,Direction.SOUTH,Direction.SOUTHEAST}
	};
	*/

	static class intcord
	{
		intcord(int a,int b)
		{
			this.x=a; this.y=b;
		}
		public int x;
		public int y;
		public intcord rotate()
		{
			return new intcord(-y,x);
		}
	}

	/**
	 * Updates alive_scout_ids based on whether they are alive
	 */

	static boolean trymove(intcord itc,int rt) throws GameActionException
	{
		System.out.println("try to move "+itc.x+" "+itc.y);
		for(int i=0;i<4-rt;i++) itc=itc.rotate();
		Direction dir=cordtodir[itc.x+1][itc.y+1];
		if(rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}
		return false;
	}
	static boolean cantrymove(intcord itc,int rt) throws GameActionException
	{
		System.out.println("can i move "+itc.x+" "+itc.y);
		for(int i=0;i<4-rt;i++) itc=itc.rotate();
		Direction dir=cordtodir[itc.x+1][itc.y+1];
		if(rc.canMove(dir)) {
			System.out.println("yes");
			return true;
		}
		System.out.println("no");
		return false;
	}

	static MapLocation center = null;
	public static void run() throws GameActionException{

		rc=RobotPlayer.rc;
		if(RobotPlayer.just_made) {
			RobotInfo[] nearby = rc.senseNearbyRobots(2);

			for (RobotInfo x : nearby) {
				if (x.type == RobotType.ENLIGHTENMENT_CENTER) {
					center = x.location;
				}
			}
		}
		MapLocation cur = rc.getLocation();
		if(center!=null) {
			intcord disp=new intcord(cur.x - center.x,cur.y - center.y);
			int rtnum=0;
			while(!(disp.y>0&&disp.x>=-disp.y&&disp.x<disp.y))
			{
				disp=disp.rotate();
				rtnum++;
			}
			int x=disp.x;
			int y=disp.y;
			if(y==-x)
			{
				if(trymove((new intcord(1,0)),rtnum)) return;
				if(!cantrymove((new intcord(1,-1)),rtnum))
				{
					if(trymove((new intcord(1, 1)), rtnum)) return;
					if(trymove((new intcord(0, 1)), rtnum)) return;
					if(trymove((new intcord(-1, 1)), rtnum)) return;
				}
				return;
			}

			if(y>=-x+2&&y>=x+2&&trymove((new intcord(0,-1)),rtnum)) return;
			if(y>=-x+3&&trymove((new intcord(-1,-1)),rtnum)) return;
			if(y>=x+3&&trymove((new intcord(1,-1)),rtnum)) return;
			if(y>=x+2&&trymove((new intcord(1,0)),rtnum)) return;
			return;


		}


	}

}

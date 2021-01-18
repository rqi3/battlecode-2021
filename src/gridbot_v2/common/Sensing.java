package gridbot_v2.common;


import battlecode.common.*;
import gridbot_v2.RobotPlayer;

import java.util.ArrayList;

public class Sensing
{
    static RobotController rc;

    public static MapLocation previousLocation=null;
    public static MapLocation currentLocation;


    public static Communication.gridBotInfo[] nearByGrid;
    static int nearByGridSize;


    public static void init()
    {
        rc = RobotPlayer.rc;
        nearByGrid=new Communication.gridBotInfo[10];
        nearByGridSize=0;
    }
    public static int getTimeCycle() throws GameActionException
    {
        RobotInfo[] nearby=rc.senseNearbyRobots(100, rc.getTeam());
        for(RobotInfo ri:nearby)
        {
            int flag=Communication.getFlag(ri.ID);
            if(flag!=0)
                return flag%2;
        }
        return 0;
    }

    public static void senseNearByGrid(boolean haveTurnPassed) throws GameActionException {
        currentLocation=rc.getLocation();

        if(previousLocation!=null&&previousLocation.equals(currentLocation)&&haveTurnPassed==false) return;
        if(previousLocation!=null)
        {
            Communication.gridBotInfo[] newNearByGrid=new Communication.gridBotInfo[10];
            int newNearByGridSize=0;
            for(int gridNum=0;gridNum<nearByGridSize;gridNum++)
            {
                Communication.gridBotInfo grid=nearByGrid[gridNum];
                if(rc.canSenseLocation(grid.location))
                {
                    newNearByGrid[newNearByGridSize]=grid;
                    newNearByGridSize++;
                }

            }
            for(int gridNum=0;gridNum<newNearByGridSize;gridNum++) {
                Communication.gridBotInfo grid = newNearByGrid[gridNum];
                assert (rc.canSenseLocation(grid.location));
            }
            nearByGrid=newNearByGrid;
            nearByGridSize=newNearByGridSize;
        }

        MapLocation nearestGrid=Displacement.getGrid(currentLocation);
        int nearestGridX=nearestGrid.x;
        int nearestGridY=nearestGrid.y;

        for(int i=-2;i<3;i++) for(int j=-2;j<3;j++)
        {
            int gridPointX=nearestGridX+3*i;
            int gridPointY=nearestGridY+3*j;

            if((gridPointX+gridPointY)%2==0) {
                MapLocation gridPoint = new MapLocation(gridPointX, gridPointY);
                if (rc.canSenseLocation(gridPoint)) {

                    boolean foundInNearByGrid=false;

                    for(int gridNum=0;gridNum<nearByGridSize;gridNum++)
                    {
                        Communication.gridBotInfo grid=nearByGrid[gridNum];
                        if(grid.location.equals(gridPoint)) {
                            foundInNearByGrid=true;
                            if(haveTurnPassed)
                            {
                                Communication.updateGridInfo(grid);
                            }
                        }
                    }
                    if(!foundInNearByGrid) {
                        nearByGrid[nearByGridSize]=new Communication.gridBotInfo(gridPoint);
                        nearByGridSize++;
                    }
                }
            }
        }



        Communication.gridBotInfo[] tempNearByGrid=new Communication.gridBotInfo[nearByGridSize];
        for(int gridNum=0;gridNum<nearByGridSize;gridNum++) tempNearByGrid[gridNum]=nearByGrid[gridNum];
        nearByGrid=tempNearByGrid;


        previousLocation=currentLocation;
    }
}

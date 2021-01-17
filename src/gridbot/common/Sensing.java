package gridbot.common;


import battlecode.common.*;
import gridbot.RobotPlayer;

import java.util.Map;

public class Sensing
{
    static RobotController rc;

    public static MapLocation previousLocation=null;
    public static MapLocation currentLocation;
    public static RobotInfo[] nearByAll;
    public static RobotInfo[] locationToRobots;

    public static Communication.gridInfo[][] nearByGrid=new Communication.gridInfo[5][5];

    public static void init()
    {
        for(int i=0;i<5;i++) for(int j=0;j<5;j++) nearByGrid[i][j]=new Communication.gridInfo();
    }

    public static void sense(boolean haveTurnPassed) throws GameActionException {
        rc = RobotPlayer.rc;
        currentLocation=rc.getLocation();

        System.out.println("sensing bytecode checkpoint 1: "+Clock.getBytecodeNum());
        if(previousLocation!=null)
        {
            for(int i=0;i<5;i++) for(int j=0;j<5;j++) assert nearByGrid[i][j]!=null;
            Displacement gridChange=new Displacement(
                    currentLocation.x/3-previousLocation.x/3,
                    currentLocation.y/3-previousLocation.y/3
            );
            Communication.gridInfo[][] newNearByGrid=new Communication.gridInfo[5][5];
            for(int i=0;i<5;i++) for(int j=0;j<5;j++) newNearByGrid[i][j]=new Communication.gridInfo();
            for(int i=0;i<5;i++) for(int j=0;j<5;j++)
            {
                int newX=i+gridChange.x;
                int newY=j+gridChange.y;
                if(0<=newX&&newX<5
                &&0<=newY&&newY<5
                )
                    newNearByGrid[newX][newY]=nearByGrid[i][j];
            }
            for(int i=0;i<5;i++) for(int j=0;j<5;j++) assert newNearByGrid[i][j]!=null;
            nearByGrid=newNearByGrid;
            for(int i=0;i<5;i++) for(int j=0;j<5;j++) assert nearByGrid[i][j]!=null;
        }
        else
        {
            for(int i=0;i<5;i++) for(int j=0;j<5;j++) nearByGrid[i][j]=new Communication.gridInfo();
        }
        System.out.println("sensing bytecode checkpoint 2: "+Clock.getBytecodeNum());
        nearByAll=rc.senseNearbyRobots();
        System.out.println("sensing bytecode checkpoint 3: "+Clock.getBytecodeNum());
        locationToRobots=new RobotInfo[169];
        System.out.println("sensing bytecode checkpoint 3.5: "+Clock.getBytecodeNum());


        int offsetX=6-currentLocation.x;
        int offsetY=6-currentLocation.y;
        for(RobotInfo ri: nearByAll)
        {
            //System.out.println("Robot found at: "+Displacement.subtract(ri.location,currentLocation));
            /* faster equivalent of this
                Displacement disp=Displacement.subtract(ri.location,currentLocation);
                locationToRobots[disp.x+6][disp.y+6]=ri;
             */
            locationToRobots[(ri.location.x+offsetX)*13+ri.location.y+offsetY]=ri;
        }
        System.out.println("sensing bytecode checkpoint 4: "+Clock.getBytecodeNum());

        MapLocation nearestGrid=new MapLocation(currentLocation.x/3*3+1,currentLocation.y/3*3+1);
        //MapLocation nearestGrid=Displacement.getGrid(currentLocation);
        System.out.println("sensing bytecode checkpoint 4.1: "+Clock.getBytecodeNum());
        int offsetTotal=13*offsetX+offsetY;
        for(int i=-2;i<3;i++) for(int j=-2;j<3;j++)
        {
            int gridPointX=nearestGrid.x+3*i;
            int gridPointY=nearestGrid.y+3*j;
            MapLocation gridPoint=new MapLocation(gridPointX,gridPointY);
            if(rc.canSenseLocation(gridPoint))
            {
                //System.out.println("Sensing robot at gridpoint: "+Displacement.subtract(gridPoint,currentLocation));
                //RobotInfo rb=locationToRobots[(gridPointX+offsetX)*13+gridPointY+offsetY];
                RobotInfo rb=locationToRobots[gridPointX*13+gridPointY+offsetTotal];
                //System.out.println("sensing loop checkpoint 3.05: "+Clock.getBytecodeNum());
                Communication.gridInfo gridToUpdate=nearByGrid[i+2][j+2];
                assert(gridToUpdate!=null);
                if(haveTurnPassed||gridToUpdate.haveSensed==false) {
                    //System.out.println("sensing loop checkpoint 3.2: "+Clock.getBytecodeNum());
                    if (rb != null&&rb.team==rc.getTeam())//TODO: test whether rb is actually a gridBot?
                    {
                        Communication.updateInfo(gridToUpdate, Communication.getFlag(rb.ID));
                    }
                    else
                    {
                        Communication.updateInfo(gridToUpdate, -1);
                    }
                }
            }
            else{
                nearByGrid[i+2][j+2]=new Communication.gridInfo();
            }
            /* faster equivlent of this
            MapLocation gridPoint=Displacement.add(Displacement.getGrid(currentLocation),new Displacement(3*i,3*j));

            if(rc.canSenseLocation(gridPoint))
            {
                //System.out.println("Sensing robot at gridpoint: "+Displacement.subtract(gridPoint,currentLocation));
                RobotInfo rb=locationToRobots[gridPoint.x- currentLocation.x+6][gridPoint.y- currentLocation.y+6];
                if(haveTurnPassed||nearByGrid[i+2][j+2].haveSensed==false) {
                    if (rb != null&&rb.team==rc.getTeam())//TODO: test whether rb is actually a gridBot?
                    {
                        //System.out.println("getting flag of: "+rb);
                        Communication.updateInfo(nearByGrid[i + 2][j + 2], Communication.getFlag(rb.ID));
                    } else Communication.updateInfo(nearByGrid[i + 2][j + 2], -1);
                }
            }
            else{
                nearByGrid[i+2][j+2]=null;
            }
             */
        }

        previousLocation=currentLocation;
        System.out.println("sensing bytecode checkpoint 5: "+Clock.getBytecodeNum());
    }


}

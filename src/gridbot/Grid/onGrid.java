package gridbot.Grid;


import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import gridbot.RobotPlayer;
import gridbot.common.Communication;
import gridbot.common.Displacement;
import gridbot.common.Sensing;

public class onGrid {
    static RobotController rc;

    static Communication.gridInfo result;
    public static void run() throws GameActionException {
        rc = RobotPlayer.rc;

        result=new Communication.gridInfo();
        int bestDist=100000;
        Displacement bestDirection=new Displacement(0,0);
        for(int i=-1;i<=1;i++) for(int j=-1;j<=1;j++)
            if(i!=0||j!=0) {
                int dist;
                Communication.gridInfo gi=Sensing.nearByGrid[i + 2][j + 2];
                if(gi.haveSensed) {
                    if (gi.haveGridBot) {
                        dist = gi.emptyGridDistance + 1;
                    } else dist = 0;
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestDirection.x = i;
                        bestDirection.y = j;
                    }
                }
            }
        result.emptyGridDirection=bestDirection;
            result.emptyGridDistance=bestDist;
        Communication.setGridInfo(result);
    }
}


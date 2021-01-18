package gridbot_v2.Grid;


import battlecode.common.*;
import gridbot_v2.RobotPlayer;
import gridbot_v2.common.Communication;
import gridbot_v2.common.Displacement;
import gridbot_v2.common.Sensing;

public class onGrid {
    static RobotController rc;

    static Communication.gridMessageInfo result;

    public static void init()
    {
        rc = RobotPlayer.rc;
    }

    public static void run() throws GameActionException {


        result=new Communication.gridMessageInfo();

        {
            int bestDist = 100000;
            Displacement bestDirection = new Displacement(1, -1);

            for (Communication.gridBotInfo gi : Sensing.nearByGrid) {
                Displacement disp = Displacement.subtract(gi.location, Sensing.currentLocation);
                if (Displacement.getMaxXYNorm(disp) == 3) {
                    int dist;
                    if (gi.haveGridBot) {
                        if (gi.message.haveMessageGroup0)
                            dist = gi.message.emptyGridDistance + 1;
                        else dist = 200000;
                    } else dist = 0;
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestDirection.x = disp.x / 3;
                        bestDirection.y = disp.y / 3;
                    }

                }
            }
            result.emptyGridDirection = bestDirection;
            result.emptyGridDistance = bestDist;
        }
        {
            int highestThreat = -100;

            Displacement bestDirection = new Displacement(1, -1);

            for (Communication.gridBotInfo gi : Sensing.nearByGrid) {
                Displacement disp = Displacement.subtract(gi.location, Sensing.currentLocation);
                if (Displacement.getMaxXYNorm(disp) == 3) {
                    int threat;
                    if (gi.haveGridBot) {
                        if (gi.message.haveMessageGroup0)
                            threat = gi.message.threat - 1;
                        else threat=100;
                    } else threat = 200;
                    if (threat > highestThreat) {
                        highestThreat = threat;
                        if (gi.haveGridBot&&gi.message.haveMessageGroup0) {
                            bestDirection.x = disp.x / 3;
                            bestDirection.y = disp.y / 3;
                        }
                    }

                }
            }
            RobotInfo[] nearBy=rc.senseNearbyRobots();
            for(RobotInfo ri:nearBy)
            {
                if(ri.team==rc.getTeam())
                {
                    highestThreat-=ri.influence;
                }
                else if(ri.team==rc.getTeam().opponent())
                    highestThreat+=ri.influence;
            }

            if(highestThreat<0) highestThreat=0;
            if(highestThreat>=512) highestThreat=511;
            result.attackDirection = bestDirection;
            result.threat = highestThreat;
        }

        result.escapeDirection=new Displacement(1,1);
        Communication.setGridInfo(result);
    }
}


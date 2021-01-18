package gridbot_v2.common;

import battlecode.common.GameActionException;

import static java.lang.Math.min;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import gridbot_v2.RobotPlayer;

public class Communication
{
    static RobotController rc;
    //public static final int noCommunication=(1<<24)-1;
    static int encrypt(int val)
    {
        //TODO
        //must map 0 to 0!
        return val;
    }
    static int decrypt(int val)
    {
        //TODO
        return val;
    }
    static int flagToSet=0;
    public static void setFlag(int flag)
    {
        flagToSet=flag;
    }
    public static void turnEnd() throws  GameActionException
    {
        RobotPlayer.rc.setFlag(encrypt(flagToSet));
    }
    public static int getFlag(int id) throws GameActionException
    {
        assert(rc.canGetFlag(id));
        return decrypt(rc.getFlag(id));
    }
    public static class gridMessageInfo
    {
        //message group 0
        public boolean haveMessageGroup0;
        public int emptyGridDistance;//5,[0,23)
        public Displacement emptyGridDirection;//2,[0,4)
        public int threat;//9,[0,512)
        public Displacement attackDirection;//2,[0,4)
        public Displacement escapeDirection;//2,[0,4)

        //direction to grid(2), distance to grid(8)
        //direction attack(2)
        //direction retreat(2)
        //threat level(9)

        //message group 1
        public boolean haveMessageGroup1;

        //safest point threat (9), safest point time(5), safest point distance(8), safest point direction(2)

        //public int messageGroup;
        public void updateMessageInfo(int flagReceived,RobotInfo ri)
        {
            int messageGroup =(flagReceived%2!=ri.location.x%2)?1:0;
            flagReceived/=2;
            if(flagReceived!=0) {
                flagReceived--;
                if (messageGroup == 0) {
                    haveMessageGroup0 = true;
                    emptyGridDistance = flagReceived % 23;
                    flagReceived /= 23;
                    emptyGridDirection = codetodisp[flagReceived % 4].duplicate();
                    flagReceived /= 4;
                    threat = flagReceived % 512;
                    flagReceived /= 512;
                    attackDirection = codetodisp[flagReceived % 4].duplicate();
                    flagReceived /= 4;
                    escapeDirection = codetodisp[flagReceived % 4].duplicate();
                }
                if (messageGroup == 1) {
                    haveMessageGroup1 = true;
                }
            }
        }
        public gridMessageInfo()
        {
            haveMessageGroup0=false;
            haveMessageGroup1=false;
        }
        public gridMessageInfo(int flagReceived,RobotInfo ri)
        {
            haveMessageGroup0=false;
            haveMessageGroup1=false;
            updateMessageInfo(flagReceived,ri);
        }
    }
    public static class gridBotInfo
    {
        public boolean haveGridBot;
        public RobotInfo robot;
        public MapLocation location;
        public gridMessageInfo message;
        public gridBotInfo(MapLocation loc) throws GameActionException
        {
            location=loc;
            robot=rc.senseRobotAtLocation(loc);
            haveGridBot=(robot!=null)&&(robot.team==rc.getTeam());
            if(haveGridBot)
            {
                assert(rc.canGetFlag(robot.ID));
                message=new gridMessageInfo(rc.getFlag(robot.ID),robot);
            }
        }

    }
    static final int[][] disptocode={
            {0,4,1},
            {7,8,5},
            {3,6,2}
    };
    static Displacement[] codetodisp=new Displacement[9];
    public static void init()
    {
        rc=RobotPlayer.rc;
        for(int i=0;i<3;i++)
            for(int j=0;j<3;j++)
                codetodisp[disptocode[i][j]]=new Displacement(i-1,j-1);
    }
    static int getCodeFromDisplacement(Displacement disp)
    {
        return disptocode[disp.x+1][disp.y+1];
    }
    public static void setGridInfo(gridMessageInfo gi) throws GameActionException
    {
        rc.setIndicatorDot(Displacement.add(rc.getLocation(),gi.attackDirection), 255,0,0);
        int messageGroup=(RobotPlayer.timeCycle!=rc.getLocation().x%2)?1:0;
        if(messageGroup==0) {

            int tmp;
            tmp=getCodeFromDisplacement(gi.escapeDirection);
            assert(0<=tmp&&tmp<4);
            tmp=getCodeFromDisplacement(gi.attackDirection);
            assert(0<=tmp&&tmp<4);
            assert(0<=gi.threat&&gi.threat<512);
            tmp=getCodeFromDisplacement(gi.emptyGridDirection);
            assert(0<=tmp&&tmp<4);
            assert(0<=gi.emptyGridDistance&&gi.emptyGridDistance<23);


            int flag=0;
            flag+= getCodeFromDisplacement(gi.escapeDirection);
            flag*=4; flag+=getCodeFromDisplacement(gi.attackDirection);
            flag*=512; flag+= gi.threat;
            flag*=4; flag+=getCodeFromDisplacement(gi.emptyGridDirection);
            flag*=23; flag+= gi.emptyGridDistance;

            flag++;
            setFlag(flag * 2 + RobotPlayer.timeCycle);
        }
        if(messageGroup==1) {
            setFlag(100 * 2 + RobotPlayer.timeCycle);
        }
    }

    public static void updateGridInfo(gridBotInfo gi) throws GameActionException
    {
        gi.robot=rc.senseRobotAtLocation(gi.location);
        boolean haveBotNow=(gi.robot!=null)&&(gi.robot.team==rc.getTeam());
        if(haveBotNow)
        {
            if(gi.haveGridBot)
            {
                gi.message.updateMessageInfo(getFlag(gi.robot.ID),gi.robot);
            }
            else
            {
                gi.message=new gridMessageInfo(getFlag(gi.robot.ID),gi.robot);
            }
            gi.haveGridBot=true;
        }
        else gi.haveGridBot=false;
    }

}

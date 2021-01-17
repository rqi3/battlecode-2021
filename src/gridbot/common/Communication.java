package gridbot.common;

import battlecode.common.Clock;
import battlecode.common.GameActionException;

import static java.lang.Math.min;
import gridbot.RobotPlayer;

public class Communication
{
    static int encrypt(int val)
    {
        //TODO
        return val;
    }
    static int decrypt(int val)
    {
        //TODO
        return val;
    }
    public static void setFlag(int flag) throws GameActionException
    {
        RobotPlayer.rc.setFlag(encrypt(flag));
    }
    public static int getFlag(int id) throws GameActionException
    {
        return decrypt(RobotPlayer.rc.getFlag(id));
    }
    public static class gridInfo
    {
        public boolean haveSensed;
        public boolean haveGridBot;
        public Displacement emptyGridDirection;
        public int emptyGridDistance;
        public gridInfo(){ haveSensed=false;}
    }
    static final int[][] disptocode={
            {0,1,2},
            {7,8,3},
            {6,5,4}
    };
    static Displacement[] codetodisp=new Displacement[9];
    public static void init()
    {
        for(int i=0;i<3;i++)
            for(int j=0;j<3;j++)
                codetodisp[disptocode[i][j]]=new Displacement(i-1,j-1);
    }
    public static void setGridInfo(gridInfo gi) throws GameActionException
    {
        setFlag(min(gi.emptyGridDistance,22)*8+disptocode[gi.emptyGridDirection.x+1][gi.emptyGridDirection.y+1]);
    }
    public static void updateInfo(gridInfo gi,int flagReceived)
    {
        gi.haveSensed=true;
        if(flagReceived!=-1) {
            gi.haveGridBot =true;
            gi.emptyGridDistance = flagReceived/8;
            gi.emptyGridDirection= codetodisp[flagReceived%8].duplicate();
        }
        else gi.haveGridBot =false;
    }

}

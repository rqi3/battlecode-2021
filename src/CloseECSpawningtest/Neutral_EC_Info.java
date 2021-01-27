package CloseECSpawningtest;

import battlecode.common.*;

/**
 * Neutral_EC_Info contains information about a neutral EC
 * @author    Coast
 */
public class Neutral_EC_Info {
    public Point rel_loc; //position relative to parent EC of unit that is accessing this
    public MapLocation loc;
    public int influence;

    final int flag_signal = 1;

    Neutral_EC_Info(){
        rel_loc = new Point();
        influence = -1;
    }

    public void setPosition(Point _rel_loc){
        MapLocation origin;
        if(RobotPlayer.rc.getType() == RobotType.ENLIGHTENMENT_CENTER){
            origin = RobotPlayer.rc.getLocation();
        }
        else{
            origin = RobotPlayer.parent_EC.getLocation();
        }
        loc = new MapLocation(origin.x+_rel_loc.x, origin.y+_rel_loc.y);
        rel_loc = _rel_loc;
    }

    public void setPosition(MapLocation _loc){
        MapLocation origin;
        if(RobotPlayer.rc.getType() == RobotType.ENLIGHTENMENT_CENTER){
            origin = RobotPlayer.rc.getLocation();
        }
        else{
            origin = RobotPlayer.parent_EC.getLocation();
        }
        rel_loc.x = _loc.x-origin.x;
        rel_loc.y = _loc.y-origin.y;
        loc = _loc;
    }


    public void setInfluence(int _influence){
        influence = _influence;
    }

    public int toFlagValue(){ //Muckraker uses this to convert EC they see into flag
        int flag_influence = (int)((double)(influence)*127.0/500.0);

        int flag_loc = RobotPlayer.convertToFlagRelativeLocation(rel_loc);

        return flag_signal+flag_influence*(1<<3)+flag_loc*(1<<10);
    }

    public static Neutral_EC_Info fromFlagValue(int flag_value){ //parent_EC uses this to convert flag into EC found by scout
        Neutral_EC_Info neutral_ec = new Neutral_EC_Info();

        int location_bits = RobotPlayer.getBitsBetween(flag_value, 10, 23);
        int influence_bits = RobotPlayer.getBitsBetween(flag_value, 3, 9);

        neutral_ec.setPosition(RobotPlayer.convertFromFlagRelativeLocation(location_bits));
        neutral_ec.setInfluence((int)(influence_bits*500.0/127.0));

        return neutral_ec;
    }

    public int toBroadcastFlagValue() //parent_EC uses this to broadcast to all units

    {
        /*
        0th bit is 0.
        1-3 bits are flag_signal
        10-23 bits are location
         */
        int flag_loc = RobotPlayer.convertToFlagRelativeLocation(rel_loc);

        return flag_signal*(1<<1)+flag_loc*(1<<10);
    }

    public static Neutral_EC_Info fromBroadcastFlagValue(int flag_value)
    //units use this to convert to EC location known by parent_EC
    {
        int location_bits = RobotPlayer.getBitsBetween(flag_value, 10, 23);

        Neutral_EC_Info neutral_ec = new Neutral_EC_Info();
        neutral_ec.setPosition(RobotPlayer.convertFromFlagRelativeLocation(location_bits));

        return neutral_ec;
    }


}

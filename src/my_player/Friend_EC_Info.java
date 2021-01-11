package my_player;

import battlecode.common.*;
import java.util.*;

public class Friend_EC_Info {
    public Point rel_loc; //position relative to parent EC of unit that is accessing this
    public MapLocation loc;

    final int flag_signal = 3;

    Friend_EC_Info(){
        rel_loc = new Point();
    }

    public void setPosition(Point _rel_loc){
        rel_loc = _rel_loc;
    }

    public void setPosition(MapLocation _loc, MapLocation origin){
        rel_loc.x = _loc.x-origin.x;
        rel_loc.y = _loc.y-origin.y;
        loc = _loc;
    }


    public int toFlagValue(){
        int flag_value = flag_signal;
        int flag_loc = RobotPlayer.convertToFlagRelativeLocation(rel_loc);
        return flag_value+flag_loc*(1<<10);
    }

    public static Friend_EC_Info fromFlagValue(int flag_value){
        Friend_EC_Info friend_ec = new Friend_EC_Info();
        int location_bits = RobotPlayer.getBitsBetween(flag_value, 10, 23);
        friend_ec.setPosition(RobotPlayer.convertFromFlagRelativeLocation(location_bits));
        return friend_ec;
    }

    /*
    We are not broadcasting this (so far)
     */
}

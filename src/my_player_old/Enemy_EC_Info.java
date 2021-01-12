package my_player_old;

import battlecode.common.*;
import java.util.*;

public class Enemy_EC_Info {
    public Point rel_loc; //position relative to parent EC of unit that is accessing this
    public MapLocation loc;

    final int flag_signal = 2;

    Enemy_EC_Info(){
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

    public static Enemy_EC_Info fromFlagValue(int flag_value){
        Enemy_EC_Info enemy_ec = new Enemy_EC_Info();
        int location_bits = RobotPlayer.getBitsBetween(flag_value, 10, 23);
        enemy_ec.setPosition(RobotPlayer.convertFromFlagRelativeLocation(location_bits));
        return enemy_ec;
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

    public static Enemy_EC_Info fromBroadcastFlagValue(int flag_value)
    //units use this to convert to EC location known by parent_EC
    {
        int location_bits = RobotPlayer.getBitsBetween(flag_value, 10, 23);

        Enemy_EC_Info enemy_ec = new Enemy_EC_Info();
        enemy_ec.setPosition(RobotPlayer.convertFromFlagRelativeLocation(location_bits));

        return enemy_ec;
    }
}

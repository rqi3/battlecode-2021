package my_player;

import battlecode.common.*;
import java.util.*;

/**
 * Contains information about enemy units
 */
public class EnemyUnitInfo {
    static final int ENEMY_MEMORY = 2; //how many bits will be used to communicate turn_received. Units must remove according to (1<<ENEMY_MEMORY)
    int round_received;
    int unit_type; //1 = muckraker, 2 = politician, 3 = slanderer
    public Point rel_loc; //position relative to parent EC of unit that is accessing this
    public MapLocation loc;

    EnemyUnitInfo(){
        round_received = 0;
        unit_type = 0;
        rel_loc = new Point();
    }

    @Override
    public boolean equals(Object o){
        if(o == this){
            return true;
        }

        if(!(o instanceof EnemyUnitInfo)){
            return false;
        }

        EnemyUnitInfo e = (EnemyUnitInfo)(o);
        return round_received == e.round_received && unit_type == e.unit_type && rel_loc.equals(e.rel_loc);
    }

    @Override
    public int hashCode(){
        return (int)(round_received+1000*loc.x+10*loc.y);
    }

    void setRound(int r){
        round_received = r;
    }

    void setType(RobotType rt){
        if(rt == RobotType.MUCKRAKER){
            unit_type = 1;
        }
        else if(rt == RobotType.POLITICIAN){
            unit_type = 2;
        }
        else if(rt == RobotType.SLANDERER){
            unit_type = 3;
        }
    }

    void setLocation(MapLocation ml){
        loc = ml;
        rel_loc = RobotPlayer.convertToRelativeCoordinates(ml);
    }




    /**
     * Converts this EnemyUnitInfo object into a flag value.
     * Use mods for both turn and location
     * @return flag_value
     */
    int toFlagValue(){
        return 0;
    }

    /**
     * inverts toFlagValue
     */
    public static EnemyUnitInfo fromFlagValue(int flag_value){
        return new EnemyUnitInfo();
    }

    /**
     * Converts this EnemyUnitInfo object into a broadcast flag value
     */
    int toBroadcastFlagValue(){
        return 0;
    }

    /**
     * Inverts toBroadcastFlagValue
     */
    public static EnemyUnitInfo fromBroadcastFlagValue(int flag_value){
        return new EnemyUnitInfo();
    }
}

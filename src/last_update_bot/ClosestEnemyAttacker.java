package last_update_bot;

import battlecode.common.MapLocation;
import java.util.*;

/**
 * Stores information about the closest enemy attacker (politician/muckraker), if it exists
 */
public class ClosestEnemyAttacker {
    static final int ENEMY_MEMORY = 4; //how many bits will be used to communicate turn_received. Units must remove according to (1<<ENEMY_MEMORY). EC broadcasts according to this
    static boolean enemy_exists = false;
    static MapLocation enemy_position;
    static int enemy_type = 0; //1 = muckraker, 2 = politician
    static int round_seen = 0;

    public static void forgetOldInfo(){
        if(round_seen <= RobotPlayer.rc.getRoundNum()+1-(1<<ENEMY_MEMORY)){
            enemy_exists = false;
        }
    }

    public static void foundAttacker(MapLocation loc, int typ, int round){
        if(!enemy_exists || round > round_seen || RobotPlayer.rc.getLocation().distanceSquaredTo(loc) <= RobotPlayer.rc.getLocation().distanceSquaredTo(enemy_position)){
            enemy_exists = true;
            enemy_position = loc;
            enemy_type = typ;
            round_seen = round;
        }
    }

    static int toFlagValue(){
        int flag_signal = 0;
        if(enemy_type == 1){
            flag_signal = 4;
        }
        else if(enemy_type == 2){
            flag_signal = 5;
        }
        int round_bits = round_seen % (1<<ENEMY_MEMORY);
        int location_bits = (enemy_position.x % 128) + 128*(enemy_position.y % 128);
        return flag_signal + (round_bits << 3) + (location_bits << 10);
    }

    /**
     * Updates this class's parameters based on a seen flag_value
     * @param flag_value
     */
    static boolean updateUsingSeenFlagValue(int flag_value){
        int flag_signal = flag_value % 8;
        if(flag_signal != 4 && flag_signal != 5) return false; //not communicating enemy muckraker/politician
        int round = RobotPlayer.rc.getRoundNum()/(1<<ENEMY_MEMORY)*(1<<ENEMY_MEMORY) + (flag_value>>3) % (1<<ENEMY_MEMORY);
        if(round > RobotPlayer.rc.getRoundNum()) round-=(1<<ENEMY_MEMORY);

        if(enemy_exists && round < round_seen) return true;

        int loc_x = (RobotPlayer.rc.getLocation().x+63)/128*128 + (flag_value>>10) % (1<<7);
        int loc_y = (RobotPlayer.rc.getLocation().y+63)/128*128 + (flag_value>>17) % (1<<7);
        if(loc_x > RobotPlayer.rc.getLocation().x+63){
            loc_x-=128;
        }
        if(loc_y > RobotPlayer.rc.getLocation().y+63){
            loc_y-=128;
        }

        //System.out.println("Received information about enemy unit at " + new MapLocation(loc_x, loc_y) + "on round " + round);

        if(!enemy_exists || round > round_seen){
            enemy_exists = true;
            enemy_position = new MapLocation(loc_x, loc_y);
            enemy_type = flag_signal-3;
            round_seen = round;
            return true;
        }


        MapLocation new_loc = new MapLocation(loc_x, loc_y);
        if(RobotPlayer.rc.getLocation().distanceSquaredTo(enemy_position) <= RobotPlayer.rc.getLocation().distanceSquaredTo(new_loc)){
            return true;
        }
        enemy_exists = true;
        enemy_position = new_loc;
        enemy_type = flag_signal-3;
        round_seen = round;
        return true;
    }

    /**
     * Should be used only by EnlightenmentCenter
     * @return broadcast flag value of closest enemy attacker
     */
    static int toBroadcastFlagValue(){
        if(!enemy_exists) return 0;
        MapLocation my_ec_loc = RobotPlayer.rc.getLocation();
        Point enemy_rel_loc = new Point(enemy_position.x-my_ec_loc.x, enemy_position.y-my_ec_loc.y);

        int flag_signal = 3+enemy_type; //4 for muckraker, 5 for politician
        int round_bits = round_seen % (1<<ENEMY_MEMORY);
        int flag_loc = RobotPlayer.convertToFlagRelativeLocation(enemy_rel_loc);

        return flag_signal*(1<<1) + round_bits*(1<<4) + flag_loc*(1<<10);
    }

    static void updateUsingBroadcastFlagValue(int flag_value){
        if(!RobotPlayer.has_parent_EC) return; //should be receiving a broadcast from parent_EC

        int flag_signal = (flag_value>>1) % (1<<3);
        int round_bits = RobotPlayer.getBitsBetween(flag_value, 4, 4+ENEMY_MEMORY-1);
        int location_bits = RobotPlayer.getBitsBetween(flag_value, 10, 23);

        int round = RobotPlayer.rc.getRoundNum()/(1<<ENEMY_MEMORY)*(1<<ENEMY_MEMORY) + round_bits;
        if(round > RobotPlayer.rc.getRoundNum()) round-=(1<<ENEMY_MEMORY);

        Point rel_loc = RobotPlayer.convertFromFlagRelativeLocation(location_bits);
        MapLocation loc = new MapLocation(RobotPlayer.parent_EC.getLocation().x+rel_loc.x, RobotPlayer.parent_EC.getLocation().y+rel_loc.y);

        if(flag_signal == 4 || flag_signal == 5){
            //System.out.println("Broadcast info: Closest Enemy Attacker at location " + loc + " seen on round " + round);
            foundAttacker(loc, flag_signal-3, round);
        }
    }

}

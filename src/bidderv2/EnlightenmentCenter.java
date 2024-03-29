package bidderv2;

import battlecode.common.*;
import java.util.*;

public class EnlightenmentCenter {
    static RobotController rc;

    static ArrayList<Point> friendly_ecs_locs;
    static ArrayList<Point> enemy_ecs_locs;
    static ArrayList<Point> neutral_ecs_locs;

    static boolean bot_made_last_turn = false;
    static Direction bot_direction_last_turn = Direction.NORTH; //
    static boolean bot_made_this_turn = false; //was a bot made this turn?
    static Direction bot_direction_this_turn = Direction.NORTH; //direction the bot is facing

    static final RobotType[] spawnableRobot = {
            RobotType.POLITICIAN,
            RobotType.SLANDERER,
            RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static void foundEnemyMuckraker(int x, int y)
    /*
    Called when the EC receives communication that Enemy Muckraker is at relative position (x, y)
    location relative to EC
    */
    {
        // maybe send politician/make fewer slanderers
    }
    
    ////////////////////////////// Nathan Chen Bidder Code //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    static ArrayList<Integer> previous_scores = new ArrayList<Integer>();
    
    static double current_bid_value = 5; //calibrate this based on what other bots are doing
    static double BID_PERCENTAGE_UPPER_BOUND = 0.15; //don't spend too much... in theory if the opponent is going above our upper bound then they will be too poor to win remaining rounds
    //though maybe we want to raise this upper bound in the the last 200 rounds?
    static double volatility = 3; 
    static double bid_multiplier = 1;
    static final int LAST_FEW_BIDS = 6;
    
    static int getBidValue(){ //returns the value this Enlightenment Center will bid
    	System.out.println("Current influence: " + rc.getInfluence());
    	int us = rc.getTeamVotes();
    	int them = rc.getRoundNum() - rc.getTeamVotes(); //might be slightly overestimated in the case of ties - in reality ties should be really unlikely
    	bid_multiplier = 1; //reset
    	
    	if(us > 1500) return 0; //we have majority vote, just invest in full defense
    	
    	int check = Math.min(LAST_FEW_BIDS, previous_scores.size());
    	if(previous_scores.size() > check) {
    		int bids_lost = check - (us - previous_scores.get(previous_scores.size() - check));
    		bid_multiplier *= (.90 + .1 * bids_lost);
    	}
    	
    	if(rc.getRoundNum() > 2800) { //untested
    		BID_PERCENTAGE_UPPER_BOUND = 0.65;
    		bid_multiplier *= 1.5;
    	}
    	
    	current_bid_value *= Math.pow(bid_multiplier, volatility);
    	current_bid_value = Math.min(current_bid_value, BID_PERCENTAGE_UPPER_BOUND * rc.getInfluence());
    	previous_scores.add(rc.getTeamVotes());
    	return (int) current_bid_value;
    }
    
    /////////////////////////////// END Nathan Chen Bidder Code ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static void update_bot_made_lastorthis_turn(){
        bot_made_last_turn = bot_made_this_turn;
        bot_direction_last_turn = bot_direction_this_turn;
        bot_made_this_turn = false;
        bot_direction_this_turn = Direction.NORTH;
    }

    public static boolean getFlagBotMade(int flag_value)
    /*
    returns the value of bot_made_this_turn given only the flag value.
    Depends on how we made the flag_value in generateFlagValue()
     */
    {
        if((flag_value&1) == 1){ //the first bit represented bot_made_this_turn
            return true;
        }
        return false;
    }

    public static Direction getFlagDirectionMade(int flag_value)
    /*
    returns the value of bot_made_this_turn given only the flag value.
    Depends on how we made the flag_value in generateFlagValue()
     */
    {
        int dir = 0;
        for(int i = 1; i <= 3; i++){
            int bit_value = ((flag_value>>i)&1);
            dir+=(bit_value<<(i-1));
        }
        return directions[dir];
    }

    static int generateFlagValue(){ //returns the flag this Enlightenment Center will set to
        boolean[] flag_bits = new boolean[24];

        //0th bit is whether a bot was made last turn
        flag_bits[0] = bot_made_last_turn;
        //1st, 2nd, 3rd bits indicate the direction of where the bot was made
        for(int dir = 0; dir < 8; dir++){
            if(directions[dir].equals(bot_direction_last_turn)){
                //direction index is dir
                System.out.println("Bot was made in direction: " + dir + " " + directions[dir]);
                for(int j = 0; j < 3; j++){
                    if(((dir>>j)&1) == 1){
                        System.out.println("bit " + j+1 + " was set to 1");
                        flag_bits[j+1] = true; //sets the 1st, 2nd, 3rd flag_bits
                    }
                }
            }
        }

        int returned_flag_value = 0; //convert the bits to an integer
        for(int bit_position = 0; bit_position < 24; bit_position++){
            if(flag_bits[bit_position]) {
                returned_flag_value += (1 << bit_position);
            }
        }
        return returned_flag_value;
    }


    public static void run() throws GameActionException{
        //initialization
        rc = RobotPlayer.rc;

        update_bot_made_lastorthis_turn();

        //Build units. Currently only builds Muckraker of Influence 1
        RobotType toBuild = RobotType.MUCKRAKER;
        int influence = 1;
        for (Direction dir : directions) {
            if (rc.canBuildRobot(toBuild, dir, influence)) {
                rc.buildRobot(toBuild, dir, influence);
                bot_made_this_turn = true;
                bot_direction_this_turn = dir;
                System.out.println("Made bot in Direction: " + dir);
                break;
            }
        }


        //Bidding using the getBidValue function
        int bid_value = getBidValue();
        if(rc.canBid(bid_value)){
            rc.bid(bid_value);
        }

        //Flag communication
        int flag_value = generateFlagValue();
        if(rc.canSetFlag(flag_value)){
            rc.setFlag(flag_value);
        }

        System.out.println("Set Flag Value to: " + flag_value);

    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */

    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }
}

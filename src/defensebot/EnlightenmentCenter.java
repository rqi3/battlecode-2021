package defensebot;

import battlecode.common.*;
import java.util.*;

/**
 * EnlightenmentCenter controls the actions of our Enlightenment Centers.
 * @author    Coast
 */
public class EnlightenmentCenter {
	/**
	 * Allows the robot to be controlled.
	 */
	public static RobotController rc;

	/**
	 * Maximum number of scouts this EC builds. More scouts = more bytecode usage by Enlightenment Center.
	 */
	public static final int MAX_SCOUTS = 16;

	/**
	 * ???
	 */
	public static final int[] OPTIMAL_SLANDERER_INFLUENCE = {21,41,63,85,107,130,154,178,203,228,255,282,310,339,368,399,431,463,497,532,568,605,643,683,724,766,810,855,902,949};

	/**
	 * Whether a bot was made last or this turn and which direction it was made in.
	 */
	 static boolean bot_made_last_turn = false;
	    static Direction bot_direction_last_turn = Direction.NORTH; //
	    static boolean bot_made_this_turn = false; //was a bot made this turn?
	    static Direction bot_direction_this_turn = Direction.NORTH; //direction the bot is facing


	/**
	 * A list of the scout ids that this EC is keeping track of.
	 */
	static List<Integer> alive_scout_ids = new ArrayList<Integer>();

	/**
	 * ??
	 */
	static float slanderer_frequency;
	static int slanderer_investment;

	/**
	 * List of possible directions.
	 */
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
	static final List<Direction> neardirs =Arrays.asList (
			Direction.NORTH,
			Direction.EAST,
			Direction.SOUTH,
			Direction.WEST
			);
	static final List<Direction> diagsdirs = Arrays.asList (
			Direction.NORTHEAST,
			Direction.SOUTHEAST,
			Direction.SOUTHWEST,
			Direction.NORTHWEST
			);

	/**
	Called when the EC receives communication that Enemy Muckraker is at relative position (x, y)
	location relative to EC
	 @param x The x coordinate of a found muckraker
	 @param y The y coordinate of a found muckraker
	*/
	static void foundEnemyMuckraker(int x, int y) {

	}

	////////////////////////////// Nathan Chen Bidder Code //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    static ArrayList<Integer> previous_scores = new ArrayList<Integer>();
    
    static double current_bid_value = 5; //calibrate this based on what other bots are doing
    static double BID_PERCENTAGE_UPPER_BOUND = 0.5; //don't spend too much... in theory if the opponent is going above our upper bound then they will be too poor to win remaining rounds
    //though maybe we want to raise this upper bound in the the last 200 rounds?
    static double volatility = 3; 
    static double bid_multiplier = 1;
    static final int LAST_FEW_BIDS = 4;
    
    static int getBidValue(){ //returns the value this Enlightenment Center will bid
    	System.out.println("Current influence: " + rc.getInfluence());
    	int us = rc.getTeamVotes();
    	int them = rc.getRoundNum() - rc.getTeamVotes(); //might be slightly overestimated in the case of ties - in reality ties should be really unlikely
    	bid_multiplier = 1; //reset
    	
    	if(us > 1500) return 0; //we have majority vote, just invest in full defense
    	
    	if(rc.getRoundNum() >= 2500) {
    		BID_PERCENTAGE_UPPER_BOUND = 0.9;
    	}
    	
    	int check = Math.min(LAST_FEW_BIDS, previous_scores.size());
    	if(previous_scores.size() > check) {
    		int bids_lost = check - (us - previous_scores.get(previous_scores.size() - check));
    		if(rc.getRoundNum() >= 2750) {
    			bid_multiplier *= (.9996 + .02 * bids_lost);
    		} else {
    			bid_multiplier *= (.90 + .1 * bids_lost);
    		}
    	}
    	
    	current_bid_value *= Math.pow(bid_multiplier, volatility);
    	current_bid_value = Math.min(current_bid_value, BID_PERCENTAGE_UPPER_BOUND * rc.getInfluence());
    	previous_scores.add(rc.getTeamVotes());
    	
    	return (int) current_bid_value;
    }
    
    /////////////////////////////// END Nathan Chen Bidder Code ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Updates bot_made_this/last_turn and bot_direction_this/last_turn
	 */
	private static void update_bot_made_lastorthis_turn(){
		bot_made_last_turn = bot_made_this_turn;
		bot_direction_last_turn = bot_direction_this_turn;
		bot_made_this_turn = false;
		bot_direction_this_turn = Direction.NORTH;
	}

	/**
	 * Updates alive_scout_ids based on whether they are alive
	 */
	public static boolean trySpawn(int influence) throws GameActionException
	{
		Collections.shuffle(neardirs);
		for (Direction dir : neardirs) {
			if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, influence)) {
				rc.buildRobot(RobotType.MUCKRAKER, dir, influence);
				bot_made_this_turn = true;
				bot_direction_this_turn = dir;
				return true;
			}
		}
		Collections.shuffle(diagsdirs);
		for (Direction dir : diagsdirs) {
			if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, influence)) {
				rc.buildRobot(RobotType.MUCKRAKER, dir, influence);
				bot_made_this_turn = true;
				bot_direction_this_turn = dir;
				return true;
			}
		}
		return false;
	}
	public static void run() throws GameActionException{
		////////////////////Creation Begin
		if(RobotPlayer.just_made){
			rc = RobotPlayer.rc;
		}
		////////////////////Creation End


		////////////////////Initialization Begin
		update_bot_made_lastorthis_turn();
		////////////////////Initialization End

		trySpawn(1);

		////////////////////Bid Begin
		int bid_value = getBidValue();
		if(rc.canBid(bid_value)){
			System.out.println("I bid " + bid_value);
			rc.bid(bid_value);
		}
	}

}

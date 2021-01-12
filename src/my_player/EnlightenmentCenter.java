package my_player;

import battlecode.common.*;
import java.util.*;

public class EnlightenmentCenter {
	static RobotController rc;

	public static final int MAX_SCOUTS = 16; //reduce bytecode usage. Currently accounts for ~7000 bytecode
	public static final int[] OPTIMAL_SLANDERER_INFLUENCE = {21,41,63,85,107,130,154,178,203,228,255,282,310,339,368,399,431,463,497,532,568,605,643,683,724,766,810,855,902,949};

	static boolean bot_made_last_turn = false;
	static Direction bot_direction_last_turn = Direction.NORTH; //
	static boolean bot_made_this_turn = false; //was a bot made this turn?
	static Direction bot_direction_this_turn = Direction.NORTH; //direction the bot is facing

	static List<Integer> alive_scout_ids = new ArrayList<Integer>();

	static float slanderer_frequency;
	static int slanderer_investment;

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
    static final int LAST_FEW_BIDS = 4;
    
    static int getBidValue(){ //returns the value this Enlightenment Center will bid
    	System.out.println("Current influence: " + rc.getInfluence());
    	int us = rc.getTeamVotes();
    	int them = rc.getRoundNum() - rc.getTeamVotes(); //might be slightly overestimated in the case of ties - in reality ties should be really unlikely
    	bid_multiplier = 1; //reset
    	
    	if(us > 1500) return 0; //we have majority vote, just invest in full defense
    	
    	if(rc.getRoundNum() >= 2750) {
    		BID_PERCENTAGE_UPPER_BOUND = 0.40;
		if(rc.getRoundNum() >= 2875) {
			BID_PERCENTAGE_UPPER_BOUND = 0.45;
			if(rc.getRoundNum() >= 2960) {
				BID_PERCENTAGE_UPPER_BOUND = 0.50;	
			}
		}
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

	public static void receiveScoutCommunication() throws GameActionException{
		for(Integer scout_id: alive_scout_ids){
			if(!rc.canGetFlag(scout_id)) continue;
			int flag_value = rc.getFlag(scout_id);
			int flag_signal = flag_value % (1<<3);
			if(flag_signal == 1){
				//Neutral_EC_Info
				Neutral_EC_Info neutral_ec = Neutral_EC_Info.fromFlagValue(flag_value);

				//in case we have this information already in an ec information list.
				//prevents duplicates
				RobotPlayer.removeECInfo(neutral_ec.rel_loc);

				RobotPlayer.neutral_ecs.add(neutral_ec);
				System.out.println("Neutral EC Information Received:");
				System.out.println("Influence: " + neutral_ec.influence);
				System.out.println("Relative Position: " + neutral_ec.rel_loc);
			}
			else if(flag_signal == 2){
				//Enemy_EC_Info
				Enemy_EC_Info enemy_ec = Enemy_EC_Info.fromFlagValue(flag_value);

				//in case we have this information already in an ec information list.
				//prevents duplicates
				RobotPlayer.removeECInfo(enemy_ec.rel_loc);

				RobotPlayer.enemy_ecs.add(enemy_ec);
				System.out.println("Enemy EC Information Received:");
				System.out.println("Relative Position: " + enemy_ec.rel_loc);
			}
			else if(flag_signal == 3){
				//Friend_EC_Info
				Friend_EC_Info friend_ec = Friend_EC_Info.fromFlagValue(flag_value);

				//in case we have this information already in an ec information list.
				//prevents duplicates
				RobotPlayer.removeECInfo(friend_ec.rel_loc);

				RobotPlayer.friend_ecs.add(friend_ec);
				System.out.println("Friend EC Information Received:");
				System.out.println("Relative Position: " + friend_ec.rel_loc);
			}
		}
	}

	public static int getOptimalSlandererInfluence(int max_val)
		/*
			 upper_bound-1 on OPTIMAL_SLANDERER_INFLUENCE;
			 returns value, or -1 if no such value exists
			 */
	{
		int l=-1;
		int r=OPTIMAL_SLANDERER_INFLUENCE.length;
		while(r-l>1)
		{
			int m=l+(r-l)/2;
			if(OPTIMAL_SLANDERER_INFLUENCE[m] > max_val)
				r=m;
			else
				l=m;
		}
		if(l<0) return -1;
		return OPTIMAL_SLANDERER_INFLUENCE[l];
	}

	public static void updateScoutList()
	/*
	Updates the list of scouts based on whether one died or not
	 */
	{
		for(int i = alive_scout_ids.size()-1; i >= 0; i--){
			Integer robot_id = alive_scout_ids.get(i);
			if(!rc.canGetFlag(robot_id)){ //Can't get the flag if they are dead
				alive_scout_ids.remove(robot_id); //Scout died, remove it from the list!
			}
		}
	}

	public static boolean trySpawnScout() throws GameActionException
	/*
	Tries to spawn a scout in a random direction.
	Returns whether it did spawn a scout.
	 */
	{

		int scout_influence = 1;

		if(alive_scout_ids.size() < MAX_SCOUTS){
			for (Direction dir : directions) {
				if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, scout_influence)) {
					rc.buildRobot(RobotType.MUCKRAKER, dir, scout_influence);
					bot_made_this_turn = true;
					bot_direction_this_turn = dir;
					System.out.println("Made bot in Direction: " + dir);
					MapLocation spawn_loc = rc.getLocation().add(dir);
					int spawn_id = rc.senseRobotAtLocation(spawn_loc).getID();
					alive_scout_ids.add(spawn_id);
					return true;
				}
			}
		}

		return false;
	}

	public static boolean trySpawnAttackerPolitician(int attacker_influence) throws GameActionException
	{
		for (Direction dir : directions) {
			if (rc.canBuildRobot(RobotType.POLITICIAN, dir, attacker_influence)) {
				rc.buildRobot(RobotType.POLITICIAN, dir, attacker_influence);
				bot_made_this_turn = true;
				bot_direction_this_turn = dir;
				System.out.println("Made Attacker in Direction: " + dir);
				/*MapLocation spawn_loc = rc.getLocation().add(dir);
				int spawn_id = rc.senseRobotAtLocation(spawn_loc).getID();*/
				break;
			}
		}
		return false;
	}

	static int chooseRandomFreq(double[] freq){
		for(int i = 0; i < freq.length; i++){
			assert(freq[i] >= 0);
		}
		for(int i = 1; i < freq.length; i++){
			freq[i]+= freq[i-1];
		}

		if(freq[freq.length-1] < 0.001) return 0;

		for(int i = 0; i < freq.length; i++){
			freq[i]/= freq[freq.length-1];
		}
		double rand_val = Math.random();
		for(int i = 0; i < freq.length; i++){
			if(rand_val <= freq[i]){
				return i;
			}
		}
		System.out.println("chooseRandomFreq error");
		return 0;
	}

	static void trySpawnAttackerMuckraker() throws GameActionException
	{
		int attacker_influence = 2;
		for (Direction dir : directions) {
			if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, attacker_influence)) {
				rc.buildRobot(RobotType.MUCKRAKER, dir, attacker_influence);
				bot_made_this_turn = true;
				bot_direction_this_turn = dir;
				System.out.println("Made Attacker in Direction: " + dir);
				/*MapLocation spawn_loc = rc.getLocation().add(dir);
				int spawn_id = rc.senseRobotAtLocation(spawn_loc).getID();*/
				break;
			}
		}
	}

	public static void spawnRobot() throws GameActionException
	/*
			Spawns robots
			Current algorithm:
				Spawns muckraker by default. Randomly spawn slanderer according to slanderer_frequency
				slanderer_investment is current set at 10% of enlightenment center influence
	*/
	{
		System.out.println("Spawning a robot...");
		RobotType toBuild = RobotType.MUCKRAKER;
		int influence = 1;

		int attacker_politician_influence = 1;

		double build_scout_muckraker = 0; //scale of 0 to 2 of spawning weights
		double build_attacker_politician = 0;
		double build_attacker_muckraker = 0.1;
		double build_defender_politician = 0;
		double build_nothing = 0;

		if(alive_scout_ids.size() < MAX_SCOUTS){
			build_scout_muckraker = 1.0;
		}

		if(RobotPlayer.neutral_ecs.size() > 0){
			System.out.println(RobotPlayer.neutral_ecs.size());
			Point ec_target = RobotPlayer.getClosestNeutralECLocation();
			int ec_target_influence = 1;
			for(Neutral_EC_Info neutral_ec: RobotPlayer.neutral_ecs){
				if(neutral_ec.rel_loc == ec_target){
					ec_target_influence = neutral_ec.influence;
					break;
				}
			}

			attacker_politician_influence = ec_target_influence+20;

			if(attacker_politician_influence >= rc.getInfluence()/3){
				build_attacker_politician = 0;
			}
			else if(RobotPlayer.neutral_ecs.size() == 0){
				build_attacker_politician = 0;
			}
			else{
				build_attacker_politician = 0.5;
				build_nothing = 0;
			}
		}
		System.out.println("Breakpoint 2");
		if(alive_scout_ids.size() >= 2*MAX_SCOUTS/3 && RobotPlayer.enemy_ecs.size() > 0){
			build_attacker_muckraker = 0.5;
			build_nothing = 0;
		}

		System.out.println("Breakpoint 3");
		if(Math.random() < slanderer_frequency)
		{
			System.out.println("Breakpoint 4");
			//Attempt to build slanderer
			slanderer_investment = rc.getInfluence()/10;
			int cost = getOptimalSlandererInfluence(slanderer_investment);
			if(cost > 0)
			{
				toBuild = RobotType.SLANDERER;
				influence = cost;
				slanderer_frequency = Math.max(slanderer_frequency-0.5f, 0.0f);
			}
		}
		System.out.println("Breakpoint 5");
		if(toBuild == RobotType.SLANDERER){
			System.out.println("Breakpoint 6");
			for (Direction dir : directions) {
				if (rc.canBuildRobot(RobotType.SLANDERER, dir, influence)) {
					rc.buildRobot(RobotType.SLANDERER, dir, influence);
					bot_made_this_turn = true;
					bot_direction_this_turn = dir;
					System.out.println("Made bot in Direction: " + dir);
					break;
				}
			}
		}
		else{
			System.out.println("Breakpoint 7");
			double[] spawn_freq = new double[5];
			spawn_freq[0] = build_scout_muckraker; //scale of 0 to 2 of spawning weights
			spawn_freq[1] = build_attacker_politician;
			spawn_freq[2] = build_attacker_muckraker;
			spawn_freq[3] = build_defender_politician;
			spawn_freq[4] = build_nothing; // build nothing

			System.out.println("spawn_freq: " + spawn_freq[0] + ", " + spawn_freq[1] + ", " + spawn_freq[2] + ", " + spawn_freq[3] + ", "+ spawn_freq[4]);

			int spawn_type = chooseRandomFreq(spawn_freq);
			if(spawn_type == 0){
				trySpawnScout();
			}
			else if(spawn_type == 1){
				trySpawnAttackerPolitician(attacker_politician_influence);
			}
			else if(spawn_type == 2){
				trySpawnAttackerMuckraker();
			}
			else if(spawn_type == 3){

			}
		}

		slanderer_frequency = Math.min(slanderer_frequency+0.05f, 1f);
	}


	private static int generateFlagValue(){ //returns the flag this Enlightenment Center will set to
		boolean global_broadcast; //will we be communicating to everyone

		global_broadcast = !bot_made_last_turn;
		int returned_flag_value = 0; //convert the bits to an integer

		if(!global_broadcast){
			boolean[] flag_bits = new boolean[24];

			flag_bits[0] = true;
			//1st, 2nd, 3rd bits indicate the direction of where the bot was made.
			//this allows bot to find its parent_EC
			for(int dir = 0; dir < 8; dir++){
				if(directions[dir].equals(bot_direction_last_turn)){
					//direction index is dir
					System.out.println("Bot was made in direction: " + dir + " " + directions[dir]);
					for(int j = 0; j < 3; j++){
						if(((dir>>j)&1) == 1){
							flag_bits[j+1] = true; //sets the 1st, 2nd, 3rd flag_bits
						}
					}
				}
			}


			for(int bit_position = 0; bit_position < 24; bit_position++){
				if(flag_bits[bit_position]) {
					returned_flag_value += (1 << bit_position);
				}
			}
		}
		else{
			//broadcast an enemy or neutral EC
			boolean broadcast_enemy = false;
			boolean broadcast_neutral = false;
			if(RobotPlayer.enemy_ecs.size() > 0 && RobotPlayer.neutral_ecs.size() == 0){
				broadcast_enemy = true;
			}
			else if(RobotPlayer.enemy_ecs.size() == 0 && RobotPlayer.neutral_ecs.size() > 0){
				broadcast_neutral = true;
			}
			else if(RobotPlayer.enemy_ecs.size() > 0 && RobotPlayer.neutral_ecs.size() > 0){
				if(Math.random() <= 0.5){
					broadcast_enemy = true;
				}
				else{
					broadcast_neutral = true;
				}
			}

			if(broadcast_enemy){
				int enemy_ec_ind = (int)(Math.random()*RobotPlayer.enemy_ecs.size());
				returned_flag_value = RobotPlayer.enemy_ecs.get(enemy_ec_ind).toBroadcastFlagValue();
			}
			else if(broadcast_neutral){
				int neutral_ec_ind = (int)(Math.random()*RobotPlayer.neutral_ecs.size());
				returned_flag_value = RobotPlayer.neutral_ecs.get(neutral_ec_ind).toBroadcastFlagValue();
			}


		}

		return returned_flag_value;
	}

	public static void run() throws GameActionException{
		////////////////////Creation Begin
		if(RobotPlayer.just_made){
			rc = RobotPlayer.rc;
		}
		////////////////////Creation End


		////////////////////Initialization Begin
		update_bot_made_lastorthis_turn();
		updateScoutList();
		////////////////////Initialization End


		////////////////////Receive Communication Begin
		receiveScoutCommunication();
		for(Neutral_EC_Info a: RobotPlayer.neutral_ecs){
			System.out.println("I know a neutral EC is here: " + a.rel_loc);
		}
		for(Enemy_EC_Info a: RobotPlayer.enemy_ecs){
			System.out.println("I know an enemy EC is here: " + a.rel_loc);
		}
		for(Friend_EC_Info a: RobotPlayer.friend_ecs){
			System.out.println("I know a friend EC is here: " + a.rel_loc);
		}
		////////////////////Receive Communication End


		////////////////////Spawn Robot Begin
		spawnRobot();
		////////////////////Spawn Robot End


		////////////////////Bid Begin
		int bid_value = getBidValue();
		if(rc.canBid(bid_value)){
			System.out.println("I bid " + bid_value);
			rc.bid(bid_value);
		}
		////////////////////Bid End


		////////////////////Broadcast to Units Begin
		int flag_value = generateFlagValue();
		if(rc.canSetFlag(flag_value)){
			rc.setFlag(flag_value);
		}

		System.out.println("Set Flag Value to: " + flag_value);
		////////////////////Broadcast to Units End
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
}

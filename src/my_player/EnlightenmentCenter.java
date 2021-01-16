package my_player;

import battlecode.common.*;
import java.util.*;

/**
 * EnlightenmentCenter controls the actions of our Enlightenment Centers.
 * @author    Coast
 */
public class EnlightenmentCenter {
	/**
	 * Allows the robot to be controlled.
	 */ public static RobotController rc;

	/**
	 * Maximum number of scouts this EC builds. More scouts = more bytecode usage by Enlightenment Center.
	 */
	public static final int MAX_SCOUTS = 16;

	/**
	 * ???
	 */
	public static final int[] OPTIMAL_SLANDERER_INFLUENCE = {21,41,63,85,107,130,154,178,203,228,255,282,310,339,368,399,431,463,497,532,568,605,643,683,724,766,810,855,902,949};

	/**
	 * Whether a bot was made last or this turn and which direction it was made in. Used for local communication
	 */
	static boolean bot_made_last_turn = false;
	static Direction bot_direction_last_turn = Direction.NORTH; //
	static int bot_parameter_last_turn = 0; //extra communication bits 4...23, so can go up to 2^20-1
	static boolean bot_made_this_turn = false; //was a bot made this turn?
	static Direction bot_direction_this_turn = Direction.NORTH; //direction the bot is facing
	static int bot_parameter_this_turn = 0;

	/**
	 * A list of the scout ids that this EC is keeping track of.
	 */
	static List<Integer> alive_scout_ids = new ArrayList<Integer>();

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
	static double BID_PERCENTAGE_UPPER_BOUND = 0.15; //don't spend too much... in theory if the opponent is going above our upper bound then they will be too poor to win remaining rounds
	//though maybe we want to raise this upper bound in the the last 200 rounds?
	static double volatility = 3; 
	static double bid_multiplier = 1;
	static final int LAST_FEW_BIDS = 4;
	
	static int getBidValue(){ //returns the value this Enlightenment Center will bid
		//System.out.println("Current influence: " + rc.getInfluence());
		int us = rc.getTeamVotes();
		int them = rc.getRoundNum() - rc.getTeamVotes(); //might be slightly overestimated in the case of ties - in reality ties should be really unlikely
		bid_multiplier = 1; //reset
		
		if(us > 750) return 0; //we have majority vote, just invest in full defense
		
		if(rc.getRoundNum() >= 1300) {
			BID_PERCENTAGE_UPPER_BOUND = 0.50;
			if(rc.getRoundNum() >= 1480) {
				BID_PERCENTAGE_UPPER_BOUND = 0.80;
			}
		}
		
		int check = Math.min(LAST_FEW_BIDS, previous_scores.size());
		if(check > 0) {
			int bids_lost = check - (us - previous_scores.get(previous_scores.size() - check));
			if(rc.getRoundNum() >= 1300) {
				bid_multiplier *= (.9996 + .02 * bids_lost);
			} else {
				bid_multiplier *= (.95 + .1 * bids_lost);
			}
		}

		/*System.out.println("current_bid_value: " + current_bid_value);
		System.out.println("bid_multiplier: " + bid_multiplier);
		System.out.println("BID_PERCENTAGE_UPPER_BOUND: " + BID_PERCENTAGE_UPPER_BOUND);*/

		current_bid_value *= Math.pow(bid_multiplier, volatility);
		current_bid_value = Math.min(current_bid_value, BID_PERCENTAGE_UPPER_BOUND * rc.getInfluence());
		current_bid_value = Math.max(current_bid_value, 0.1);
		previous_scores.add(rc.getTeamVotes());
		
		return (int) current_bid_value;
	}
	
	/////////////////////////////// END Nathan Chen Bidder Code ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Updates bot_made_this/last_turn and bot_direction_this/last_turn
	 */
	private static void updateBotCreationInfo(){
		bot_made_last_turn = bot_made_this_turn;
		bot_direction_last_turn = bot_direction_this_turn;
		bot_parameter_last_turn = bot_parameter_this_turn;
		bot_made_this_turn = false;
		bot_direction_this_turn = Direction.NORTH;
		bot_parameter_this_turn = 0;
	}

	/**
	 * Updates alive_scout_ids based on whether they are alive
	 */
	private static void updateScoutList()

	{
		for(int i = alive_scout_ids.size()-1; i >= 0; i--){
			Integer robot_id = alive_scout_ids.get(i);
			if(!rc.canGetFlag(robot_id)){ //Can't get the flag if they are dead
				alive_scout_ids.remove(robot_id); //Scout died, remove it from the list!
			}
		}
	}


	/**
	 * Depends on how we made the flag_value in generateFlagValue()
	 * @param flag_value The value of an Enlightenment Center flag
	 * @return the value of bot_made_last_turn given only the flag value.
	 */
	public static boolean getFlagBotMade(int flag_value) {
		//the first bit represented bot_made_this_turn
		return (flag_value & 1) == 1;
	}

	/**
	 * Depends on how we made the flag_value in generateFlagValue()
	 * @param flag_value The value of an Enlightenment Center flag\
	 * @return The value of bot_made_last_turn given only the flag value.
	 */
	public static Direction getFlagDirectionMade(int flag_value) {
		// return directions[flag_value>>1&7];
		int dir = 0;
		for(int i = 1; i <= 3; i++){
			int bit_value = ((flag_value>>i)&1);
			dir+=(bit_value<<(i-1));
		}
		return directions[dir];
	}

	/**
	 * Receives communication from scouts
	 */
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
				//System.out.println("Neutral EC Information Received:");
				//System.out.println("Influence: " + neutral_ec.influence);
				//System.out.println("Relative Position: " + neutral_ec.rel_loc);
			}
			else if(flag_signal == 2){
				//Enemy_EC_Info
				Enemy_EC_Info enemy_ec = Enemy_EC_Info.fromFlagValue(flag_value);

				//in case we have this information already in an ec information list.
				//prevents duplicates
				RobotPlayer.removeECInfo(enemy_ec.rel_loc);

				RobotPlayer.enemy_ecs.add(enemy_ec);
				//System.out.println("Enemy EC Information Received:");
				//System.out.println("Relative Position: " + enemy_ec.rel_loc);
			}
			else if(flag_signal == 3){
				//Friend_EC_Info
				Friend_EC_Info friend_ec = Friend_EC_Info.fromFlagValue(flag_value);

				//in case we have this information already in an ec information list.
				//prevents duplicates
				RobotPlayer.removeECInfo(friend_ec.rel_loc);

				RobotPlayer.friend_ecs.add(friend_ec);
				//System.out.println("Friend EC Information Received:");
				//System.out.println("Relative Position: " + friend_ec.rel_loc);
			}
		}
	}

	/**
	 * upper_bound-1 on OPTIMAL_SLANDERER_INFLUENCE;
	 * returns value, or -1 if no such value exists
	 */
	public static int getOptimalSlandererInfluence(int max_val) {
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


	/**
	Tries to spawn a scout in a random direction.
	@return whether it did spawn a scout.
	 */
	private static boolean trySpawnScout() throws GameActionException {

		int scout_influence = 1;

		if(alive_scout_ids.size() < MAX_SCOUTS){
			for (Direction dir : directions) {
				if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, scout_influence)) {
					rc.buildRobot(RobotType.MUCKRAKER, dir, scout_influence);
					int bot_parameter = Muckraker.SCOUT;
					bot_made_this_turn = true;
					bot_direction_this_turn = dir;
					bot_parameter_this_turn = bot_parameter;
					//System.out.println("Made bot in Direction: " + dir);
					MapLocation spawn_loc = rc.getLocation().add(dir);
					int spawn_id = rc.senseRobotAtLocation(spawn_loc).getID();
					alive_scout_ids.add(spawn_id);
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Tries to spawn an Attack Muckraker
	 * @throws GameActionException
	 */
	private static void trySpawnAttackerMuckraker() throws GameActionException {
		int attacker_influence = 2;
		for (Direction dir : directions) {
			if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, attacker_influence)) {
				rc.buildRobot(RobotType.MUCKRAKER, dir, attacker_influence);
				int bot_parameter = Muckraker.EC_ATTACKER;

				bot_made_this_turn = true;
				bot_direction_this_turn = dir;
				bot_parameter_this_turn = bot_parameter;


				//System.out.println("Made Attacker in Direction: " + dir);
				/*MapLocation spawn_loc = rc.getLocation().add(dir);
				int spawn_id = rc.senseRobotAtLocation(spawn_loc).getID();*/
				break;
			}
		}
	}

	/**
	 *
	 * @param attacker_influence The influence that we will put into the attacker politican.
	 * @return Whether a politician was spawned
	 */
	public static boolean trySpawnAttackerPolitician(int attacker_influence) throws GameActionException
	{
		for (Direction dir : directions) {
			if (rc.canBuildRobot(RobotType.POLITICIAN, dir, attacker_influence)) {
				rc.buildRobot(RobotType.POLITICIAN, dir, attacker_influence);
				int bot_parameter = Politician.EC_ATTACK;
				bot_made_this_turn = true;
				bot_direction_this_turn = dir;
				bot_parameter_this_turn = bot_parameter;
				//System.out.println("Made Attacker in Direction: " + dir);
				/*MapLocation spawn_loc = rc.getLocation().add(dir);
				int spawn_id = rc.senseRobotAtLocation(spawn_loc).getID();*/
				return true;
			}
		}
		return false;
	}

	/**
	 *
	 * @param influence The influence that we will put into the police politican.
	 * @return Whether a politician was spawned
	 */
	public static boolean trySpawnPolicePolitician(int influence) throws GameActionException
	{
		for (Direction dir : directions) {
			if (rc.canBuildRobot(RobotType.POLITICIAN, dir, influence)) {
				rc.buildRobot(RobotType.POLITICIAN, dir, influence);
				bot_made_this_turn = true;
				bot_direction_this_turn = dir;
				bot_parameter_this_turn = Politician.POLICE;
				//System.out.println("Made Police Politician in Direction: " + dir);
				/*MapLocation spawn_loc = rc.getLocation().add(dir);
				int spawn_id = rc.senseRobotAtLocation(spawn_loc).getID();*/
				return true;
			}
		}
		return false;
	}

	/**
	 * Randomly chooses an index based on the frequencies of freq
	 * @param freq The weights for each index (does not have to sum to 1)
	 * @return A chosen index
	 */
	private static int chooseRandomFreq(double[] freq){
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


	
	static double slanderer_probability = 1.0;
	
	/**
	 * Chooses what robot to spawn and spawns it.
	 *
	 *
	 *
	 * @throws GameActionException
	 */
	public static void spawnRobot() throws GameActionException {
		System.out.println("Spawning a robot...");
		RobotType toBuild = RobotType.MUCKRAKER;
		int influence = 1;

		int attacker_politician_influence = 1;
		int police_politician_influence = 20;

		double build_scout_muckraker = 0; //scale of 0 to 2 of spawning weights
		double build_attacker_politician = 0;
		double build_attacker_muckraker = 0.3;
		double build_police_politician = 1.0;
		double build_nothing = 40.0/rc.getInfluence();

		if(alive_scout_ids.size() < MAX_SCOUTS){
			build_scout_muckraker = 1.0;
		}

		/*
		if(alive_scout_ids.size() < MAX_SCOUTS){
			build_scout_muckraker = 1.0;
		}

		if(RobotPlayer.neutral_ecs.size() > 0){
			//System.out.println(RobotPlayer.neutral_ecs.size());
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
		if(alive_scout_ids.size() >= 2*MAX_SCOUTS/3 && RobotPlayer.enemy_ecs.size() > 0){
			build_attacker_muckraker = 0.5;
			build_nothing = 0;
		}
		*/

		double ran = Math.random();
		if(ran < slanderer_probability) {
		//	Attempt to build slanderer
			int cost = getOptimalSlandererInfluence((int) (rc.getInfluence() * slanderer_probability));
			if(cost > 0) {
				toBuild = RobotType.SLANDERER;
				influence = cost;
				slanderer_probability = 0;
			}
		}
		if(toBuild == RobotType.SLANDERER){
			for (Direction dir : directions) {
				if (rc.canBuildRobot(RobotType.SLANDERER, dir, influence)) {
					rc.buildRobot(RobotType.SLANDERER, dir, influence);
					bot_made_this_turn = true;
					bot_direction_this_turn = dir;
					//System.out.println("Made bot in Direction: " + dir);
					break;
				}
			}
		}
		else{
			double[] spawn_freq = new double[5];
			spawn_freq[0] = build_scout_muckraker; //scale of 0 to 2 of spawning weights
			spawn_freq[1] = build_attacker_politician;
			spawn_freq[2] = build_attacker_muckraker;
			spawn_freq[3] = build_police_politician;
			spawn_freq[4] = build_nothing; // build nothing

			//System.out.println("spawn_freq: " + spawn_freq[0] + ", " + spawn_freq[1] + ", " + spawn_freq[2] + ", " + spawn_freq[3] + ", "+ spawn_freq[4]);

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
				// Spawn politician with bot parameter = Politician.SLANDERER_DEFENSE or Politician.EC_DEFENSE
				trySpawnPolicePolitician(police_politician_influence);
			}
		}
		//slanderer_probability += .20;
    //slanderer_probability = Math.sqrt(0.01 + slanderer_probability * slanderer_probability);
    //if(slanderer_probability > 1.0) slanderer_probability = 1.0;
		slanderer_probability = (slanderer_probability + 0.3)/1.3;
	}

	/**
	 * Gives a local communication to a newly spawned unit
	 * If no newly spawned unit, do a global broadcast
	 * @return The flag value that this Enlightenment Center will set
	 */
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
					//System.out.println("Bot was made in direction: " + dir + " " + directions[dir]);
					for(int j = 0; j < 3; j++){
						if(((dir>>j)&1) == 1){
							flag_bits[j+1] = true; //sets the 1st, 2nd, 3rd flag_bits
						}
					}
					// or flag_bits |= dir << 1; // sets bits 1..3, indicating one of the 8 possible directions
				}
			}

			for(int bit_position = 0; bit_position <= 3; bit_position++){
				if(flag_bits[bit_position]) {
					returned_flag_value += (1 << bit_position);
				}
			}

			returned_flag_value |= (bot_parameter_last_turn << 4); // bits 4..?? are set to the bot's extra parameters. For example, whether politician is attack/defense
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

	/**
	 * Run the robot.
	 */
	public static void run() throws GameActionException{
		////////////////////Creation Begin
		if(RobotPlayer.just_made){
			rc = RobotPlayer.rc;
		}
		////////////////////Creation End


		////////////////////Initialization Begin
		updateBotCreationInfo();
		updateScoutList();
		////////////////////Initialization End


		////////////////////Receive Communication Begin
		receiveScoutCommunication();

		/*for(Neutral_EC_Info a: RobotPlayer.neutral_ecs){
			System.out.println("I know a neutral EC is here: " + a.rel_loc);
		}
		for(Enemy_EC_Info a: RobotPlayer.enemy_ecs){
			System.out.println("I know an enemy EC is here: " + a.rel_loc);
		}
		for(Friend_EC_Info a: RobotPlayer.friend_ecs){
			System.out.println("I know a friend EC is here: " + a.rel_loc);
		}*/
		////////////////////Receive Communication End

		////////////////////Spawn Robot Begin
		spawnRobot();
		////////////////////Spawn Robot End


		////////////////////Bid Begin
		int bid_value = getBidValue();
		//System.out.println("bid_value: " + bid_value);
		if(rc.canBid(bid_value)){
			System.out.println("I bid " + bid_value);
			rc.bid(bid_value);
		}
		////////////////////Bid End


		////////////////////Broadcast to Units Begin (or individual communication to newly spawned unit)
		int flag_value = generateFlagValue();
		if(rc.canSetFlag(flag_value)){
			rc.setFlag(flag_value);
		}

		//System.out.println("Set Flag Value to: " + flag_value);
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
}

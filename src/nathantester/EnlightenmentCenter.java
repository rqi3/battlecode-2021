package nathantester;

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
	static Queue<Integer> alive_attack_muckraker_ids = new LinkedList<>();

	static Queue<Integer> alive_police_politician_ids = new LinkedList<>();
	static Queue<Integer> alive_attack_politician_ids = new LinkedList<>();

	static Queue<Integer> alive_slanderer_ids = new LinkedList<>();

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

	////////////////////////////// Nathan Chen Bidder Code //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	static ArrayList<Integer> previous_scores = new ArrayList<Integer>();
	
	static double current_bid_value = 5; //calibrate this based on what other bots are doing
	static double BID_PERCENTAGE_UPPER_BOUND = 0.15; //don't spend too much... in theory if the opponent is going above our upper bound then they will be too poor to win remaining rounds
	//though maybe we want to raise this upper bound in the the last 200 rounds?
	static double volatility = 1.5; 
	static double bid_multiplier = 1;
	static final int LAST_FEW_BIDS = 4;
	
	static boolean save_money = false; //THIS IS MODIFIED BASED ON spawnRobot()
	
	static int getBidValue(){ //returns the value this Enlightenment Center will bid
		//System.out.println("Current influence: " + rc.getInfluence());
		int us = rc.getTeamVotes();
		int them = rc.getRoundNum() - rc.getTeamVotes(); //might be slightly overestimated in the case of ties - in reality ties should be really unlikely
		bid_multiplier = 1; //reset
		
		if(us > 750) return 0; //we have majority vote, just invest in full defense

		BID_PERCENTAGE_UPPER_BOUND = 0.15;
		if(rc.getRoundNum() >= 1300) {
			BID_PERCENTAGE_UPPER_BOUND = 0.30;
			if(rc.getRoundNum() >= 1480) {
				BID_PERCENTAGE_UPPER_BOUND = 0.50;
			}
		}
		
		int check = Math.min(LAST_FEW_BIDS, previous_scores.size());
		if(check > 0) {
			int bids_lost = check - (us - previous_scores.get(previous_scores.size() - check));
			
			bid_multiplier *= (.95 + .075 * bids_lost);
		}

		/*System.out.println("current_bid_value: " + current_bid_value);
		System.out.println("bid_multiplier: " + bid_multiplier);
		System.out.println("BID_PERCENTAGE_UPPER_BOUND: " + BID_PERCENTAGE_UPPER_BOUND);*/

		current_bid_value *= Math.pow(bid_multiplier, volatility);
		
		double upper_bound = save_money ? BID_PERCENTAGE_UPPER_BOUND / 5 : BID_PERCENTAGE_UPPER_BOUND;
		current_bid_value = Math.min(current_bid_value, upper_bound * rc.getInfluence());
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

	public static void processUnitFlagValue(int flag_value){
		int flag_signal = flag_value % (1<<3);
		if(flag_signal == 1){
			//Neutral_EC_Info
			Neutral_EC_Info neutral_ec = Neutral_EC_Info.fromFlagValue(flag_value);
			System.out.println("Received Communication about a neutral_ec with influence: " + neutral_ec.influence);
			//in case we have this information already in an ec information list.
			//prevents duplicates
			if(neutral_ec.influence == -1) return;
			RobotPlayer.addECInfo(neutral_ec);
			//System.out.println("Neutral EC Information Received:");
			//System.out.println("Influence: " + neutral_ec.influence);
			//System.out.println("Relative Position: " + neutral_ec.rel_loc);
		}
		else if(flag_signal == 2){
			//Enemy_EC_Info
			Enemy_EC_Info enemy_ec = Enemy_EC_Info.fromFlagValue(flag_value);

			//in case we have this information already in an ec information list.
			//prevents duplicates
			if(enemy_ec.influence == -1) return;
			RobotPlayer.addECInfo(enemy_ec);
			System.out.println("Enemy EC Information Received:");
			System.out.println("Relative Position: " + enemy_ec.rel_loc);
		}
		else if(flag_signal == 3){
			//Friend_EC_Info
			Friend_EC_Info friend_ec = Friend_EC_Info.fromFlagValue(flag_value);

			//in case we have this information already in an ec information list.
			//prevents duplicates
			if(friend_ec.influence == -1) return;
			RobotPlayer.addECInfo(friend_ec);
			//System.out.println("Friend EC Information Received:");
			//System.out.println("Relative Position: " + friend_ec.rel_loc);
		}
	}


	/**
	 * Receives communication from scouts
	 */
	public static void receiveScoutCommunication() throws GameActionException{
		for(Integer scout_id: alive_scout_ids){
			if(!rc.canGetFlag(scout_id)) continue;
			int flag_value = rc.getFlag(scout_id);
			processUnitFlagValue(flag_value);
		}
	}

	public static void receiveNonScoutCommunication() throws GameActionException{

		int ALIVE_ATTACK_MUCKRAKER_RECEIVERS = Math.min(8, alive_attack_muckraker_ids.size());
		int ALIVE_POLICE_POLITICIAN_RECEIVERS = Math.min(8, alive_police_politician_ids.size());
		int ALIVE_ATTACK_POLITICIAN_RECEIVERS = Math.min(8, alive_attack_politician_ids.size());
		int ALIVE_SLANDERER_RECEIVERS = Math.min(8, alive_slanderer_ids.size());

		for(int i = 0; i < ALIVE_ATTACK_MUCKRAKER_RECEIVERS; i++){
			int id = alive_attack_muckraker_ids.remove();
			if(!rc.canGetFlag(id)){
				continue;
			}
			processUnitFlagValue(rc.getFlag(id));
			alive_attack_muckraker_ids.add(id);
		}

		for(int i = 0; i < ALIVE_POLICE_POLITICIAN_RECEIVERS; i++){
			int id = alive_police_politician_ids.remove();
			if(!rc.canGetFlag(id)){
				continue;
			}
			processUnitFlagValue(rc.getFlag(id));
			alive_police_politician_ids.add(id);
		}

		for(int i = 0; i < ALIVE_ATTACK_POLITICIAN_RECEIVERS; i++){
			int id = alive_attack_politician_ids.remove();
			if(!rc.canGetFlag(id)){
				continue;
			}
			processUnitFlagValue(rc.getFlag(id));
			alive_attack_politician_ids.add(id);
		}

		for(int i = 0; i < ALIVE_SLANDERER_RECEIVERS; i++){
			int id = alive_slanderer_ids.remove();
			if(!rc.canGetFlag(id)){
				continue;
			}
			processUnitFlagValue(rc.getFlag(id));
			alive_slanderer_ids.add(id);
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

	static int current_scout_direction = 0;
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
					bot_parameter += current_scout_direction<<2;
					current_scout_direction = (current_scout_direction+5) % 8;

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
	private static boolean trySpawnAttackerMuckraker(int attacker_influence) throws GameActionException {
		for (Direction dir : directions) {
			if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, attacker_influence)) {
				rc.buildRobot(RobotType.MUCKRAKER, dir, attacker_influence);
				int bot_parameter = Muckraker.EC_ATTACKER;

				bot_made_this_turn = true;
				bot_direction_this_turn = dir;
				bot_parameter_this_turn = bot_parameter;

				MapLocation spawn_loc = rc.getLocation().add(dir);
				int spawn_id = rc.senseRobotAtLocation(spawn_loc).getID();
				alive_attack_muckraker_ids.add(spawn_id);
				return true;
			}
			//System.out.println("Cannot build unit: " + dir + " " + attacker_influence);
		}
		System.out.println("Could not build attack muckraker");
		return false;
	}

	/**
	 *
	 * @param attacker_influence The influence that we will put into the attacker politican.
	 * @return Whether a politician was spawned
	 */
	public static boolean trySpawnAttackerPolitician(int attacker_influence, Point ec_target) throws GameActionException
	{
		if(RobotPlayer.neutral_ecs.size() == 0) return false;
		for (Direction dir : directions) {
			if (rc.canBuildRobot(RobotType.POLITICIAN, dir, attacker_influence)) {
				rc.buildRobot(RobotType.POLITICIAN, dir, attacker_influence);
				int bot_parameter = Politician.EC_ATTACK;
				bot_parameter+=1*(1<<3);
				bot_parameter+=RobotPlayer.convertToFlagRelativeLocation(ec_target)*(1<<6);

				bot_made_this_turn = true;
				bot_direction_this_turn = dir;
				bot_parameter_this_turn = bot_parameter;

				MapLocation spawn_loc = rc.getLocation().add(dir);
				int spawn_id = rc.senseRobotAtLocation(spawn_loc).getID();
				alive_attack_politician_ids.add(spawn_id);
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
	public static boolean trySpawnPolicePolitician(int influence) throws GameActionException {
		for (Direction dir : directions) {
			if (rc.canBuildRobot(RobotType.POLITICIAN, dir, influence)) {
				rc.buildRobot(RobotType.POLITICIAN, dir, influence);
				bot_made_this_turn = true;
				bot_direction_this_turn = dir;
				bot_parameter_this_turn = Politician.POLICE;

				MapLocation spawn_loc = rc.getLocation().add(dir);
				int spawn_id = rc.senseRobotAtLocation(spawn_loc).getID();
				alive_police_politician_ids.add(spawn_id);
				return true;
			}
		}
		return false;
	}

	public static boolean trySpawnSlanderer(int influence) throws GameActionException{
		for (Direction dir : directions) {
			if (rc.canBuildRobot(RobotType.SLANDERER, dir, influence)) {
				rc.buildRobot(RobotType.SLANDERER, dir, influence);
				bot_made_this_turn = true;
				bot_direction_this_turn = dir;

				MapLocation spawn_loc = rc.getLocation().add(dir);
				int spawn_id = rc.senseRobotAtLocation(spawn_loc).getID();
				alive_slanderer_ids.add(spawn_id);
				return true;
			}
		}

		return false;
	}

	/**
	 *I AM NOT ADDING THESE TO A LIST (since they should commit suicide in a couple rounds anyway)
	 * @param influence The influence that we will put into the MONEY politican.
	 * @return Whether a politician was spawned
	 */
	public static boolean trySpawnMoneyPolitician(int influence) throws GameActionException
	{
		for (Direction dir : directions) {
			if (rc.canBuildRobot(RobotType.POLITICIAN, dir, influence)) {
				rc.buildRobot(RobotType.POLITICIAN, dir, influence);
				int bot_parameter = Politician.MONEY;
				bot_made_this_turn = true;
				bot_direction_this_turn = dir;
				bot_parameter_this_turn = bot_parameter;
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Tries to spawn a unit of influence 1
	 * @throws GameActionException
	 */
	public static void trySpawnCheap() throws GameActionException{
		trySpawnScout();
		trySpawnAttackerMuckraker(1);
		System.out.println("Spawned cheap");
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

	/**
	 * Chooses what robot to spawn and spawns it.
	 *
	 *
	 *
	 * @throws GameActionException
	 */
	public static void spawnRobot() throws GameActionException {
		save_money = false; //this will be set true within the spawnRobot method if our strategy involves saving money for some reason
		if(rc.getRoundNum() < 200) save_money = true; //LOL hard code, just making sure we definitely have the money to expand
		
		//MAKE MONEY (should only happen against bad, undefended opponents) - necessary to capitalize
		if(rc.getEmpowerFactor(rc.getTeam(), 12) > 10.0 && rc.getInfluence() < 80000000) {
			System.out.println("Spawning Money Politician...");
			trySpawnMoneyPolitician(rc.getInfluence()/2); //Money politicians are always good!
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////////
		//DEFENSE Code, takes highest priority if there is a threat
		//Note that a unit's conviction has to go NEGATIVE (not just 0) for it to die...
		
		boolean surround_unit = false;
		int surround_unit_max_conviction = -1;
		for(RobotInfo close_enemy_unit: rc.senseNearbyRobots(2, rc.getTeam().opponent())){
			surround_unit = true;
			surround_unit_max_conviction = Math.max(surround_unit_max_conviction, close_enemy_unit.getConviction());
		}

		if(surround_unit){
			//very urgent need for police politician'
			
			surround_unit_max_conviction++; //SUPER SHADY LINE because I thought the conviction to die only had to go to 0, not negative
			int cost = (int)((8*surround_unit_max_conviction+10)/rc.getEmpowerFactor(rc.getTeam(), 0));
			if(cost < rc.getInfluence()) {
				trySpawnPolicePolitician(cost);
			} else {
				if(rc.getInfluence() <= 50) save_money = true;
				return; //DONT SPAWN CHEAP... waste of space and money when surrounded
			}
		}

		boolean very_close_muckraker = false;
		int very_close_muckraker_max_conviction = -1;
		for(RobotInfo close_enemy_unit: rc.senseNearbyRobots(40, rc.getTeam().opponent())){
			if(close_enemy_unit.getType() == RobotType.MUCKRAKER){
				very_close_muckraker = true;
				very_close_muckraker_max_conviction = Math.max(very_close_muckraker_max_conviction, close_enemy_unit.getConviction());
			}
		}

		if(very_close_muckraker && alive_police_politician_ids.size() < 2*alive_slanderer_ids.size()+5){ // check this ratio later
			//urgent need for police politician
			very_close_muckraker_max_conviction++; //ANOTHER SUPER SHADY LINE because I thought the conviction to die only had to go to 0, not negative
			
			int cost;
			if(very_close_muckraker_max_conviction <= 6) cost = (int)((8*very_close_muckraker_max_conviction+10)/rc.getEmpowerFactor(rc.getTeam(), 0)); //probably cheap unit flood, just prepare for 8-way surround
			else cost = (int)((2*very_close_muckraker_max_conviction+10)/rc.getEmpowerFactor(rc.getTeam(), 0) + 1); //tanky muckraker, probably not going to be a flood of these
			if(cost < rc.getInfluence()) {
				trySpawnPolicePolitician(cost);
			} else {
				if(rc.getInfluence() <= 50) save_money = true;
				return; //DONT SPAWN CHEAP... waste of space and money when surrounded
			}
		}
		
		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		//NO DANGER (outline of what to do when no danger):
		/*
		 
		 if # slanderers < 10:
		 	if no money:
		 		if(police : slanderer < 1/10) make police
		 		else make cheap 
		 	else make slanderer
		 else if neutral EC exists
		 	if no money:
		 		if(police : slanderer < 1/10) make police
		 		else make cheap
		 	else make politician
		 else if slanderers > 25 && money > 500:
		 	make attacker 400 politician
		 else
		 	make police / slanderer such that ratio is 2?:1
		 */
		
		//Make slanderer, assuming DEFENSE code didn't have to do anything
		//if our economy is huge, then it's probably a troll game where the buffs are huge - don't make vulnerable slanderers
		int spam = 10; // = how many slanderers to make first before doing anything. After the early game, if an EC wants to do this it's probably in the middle of the map and vulnerable - so don't
		if(rc.getRoundNum() >= 150) spam = 4;
		if(alive_slanderer_ids.size() < spam && rc.getInfluence() < 1000000) { 
			int cost = getOptimalSlandererInfluence(rc.getInfluence());
			if(cost > 0) {
				trySpawnSlanderer(cost);
			} else {
				if(alive_police_politician_ids.size()*5 <= alive_slanderer_ids.size()) {
					trySpawnPolicePolitician(20);
				} else {
					trySpawnCheap();
				}
			}
		}
		
		//Enough economy to start thinking about taking more bases
		if(RobotPlayer.neutral_ecs.size() > 0) {
			Point ec_target = RobotPlayer.getClosestNeutralECLocation();
			int ec_target_influence = 1;
			for(Neutral_EC_Info neutral_ec: RobotPlayer.neutral_ecs){
				if(neutral_ec.rel_loc.equals(ec_target)){
					ec_target_influence = neutral_ec.influence;
					break;
				}
			}

			if(Math.random() < 0.5){ //also go to other neutral ecs
				int random_neutral_ec_ind = (int)(Math.random()*(RobotPlayer.neutral_ecs.size()));
				ec_target = RobotPlayer.neutral_ecs.get(random_neutral_ec_ind).rel_loc;
				ec_target_influence = RobotPlayer.neutral_ecs.get(random_neutral_ec_ind).influence;
				if(ec_target_influence == -1) ec_target_influence = 150;
			}

			int attacker_politician_influence = ec_target_influence+20;

			if(attacker_politician_influence < rc.getInfluence()){
				trySpawnAttackerPolitician(attacker_politician_influence, ec_target);
			}
			else{
				save_money = true;
				if(alive_police_politician_ids.size()*5 <= alive_slanderer_ids.size()) {
					trySpawnPolicePolitician(20);
				} else {
					trySpawnCheap();
				}
			}
		}
		
		//Huge economy, start thinking about attacking
		if(alive_slanderer_ids.size() >= 25 || rc.getInfluence() >= 1000000) {
			//ATTACK
			if(alive_scout_ids.size() < MAX_SCOUTS) trySpawnScout();

			int attacker_politician_influence = -1;
			if(RobotPlayer.enemy_ecs.size() > 0 && rc.getInfluence() > 500){
				Point enemy_ec_rel_loc = RobotPlayer.getClosestEnemyECLocation();
				attacker_politician_influence = Math.max(250, (int)(rc.getInfluence()/3));
				trySpawnAttackerPolitician(attacker_politician_influence, enemy_ec_rel_loc); //Attack enemy ECs
			}

			int attacker_muckraker_influence = 1;
			if(Math.random() < 0.5){
				if(rc.getInfluence() > 500){
					attacker_muckraker_influence = Math.max(50, (int)(rc.getInfluence()*0.1));
				}
			}

			trySpawnAttackerMuckraker(attacker_muckraker_influence);
		}
		
		//Build economy (with a 1.8?/1 ratio police : slanderer)
		
		if(alive_scout_ids.size() < MAX_SCOUTS/3) trySpawnScout(); //Didn't have this line in my original plan, but probably a good one

		if(!ClosestEnemyAttacker.enemy_exists){
			double police_to_slanderer = 2;
			
			if(police_to_slanderer*alive_slanderer_ids.size() < alive_police_politician_ids.size()){
				if(!very_close_muckraker) {
					//	Attempt to build slanderer
					int cost = getOptimalSlandererInfluence(rc.getInfluence()/2);
					if (cost > 0) {
						trySpawnSlanderer(cost);
					} else {
						trySpawnCheap();
					}
				}
			}
			else{
				trySpawnPolicePolitician(20);
			}
		}
		
		double police_to_slanderer = 3; //ratio up a little bit if enemy_exists?
		if(ClosestEnemyAttacker.enemy_exists && ClosestEnemyAttacker.enemy_position.distanceSquaredTo(rc.getLocation()) < 256){
			police_to_slanderer = 5;
		}
		if(police_to_slanderer*alive_slanderer_ids.size() < alive_police_politician_ids.size()){
			if(!very_close_muckraker) {
				//	Attempt to build slanderer
				int cost = getOptimalSlandererInfluence(rc.getInfluence()/2);
				if (cost > 0) {
					trySpawnSlanderer(cost);
				} else {
					trySpawnCheap();
				}
			}
			else{
				trySpawnPolicePolitician(20);
			}
		}

		trySpawnCheap();
	}

	static int last_round_broadcast_enemy_attacker = 1; // the last round in which we broadcast an enemy attacker

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
			//Global Broadcast. First bit is 0.

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

			if(last_round_broadcast_enemy_attacker <= rc.getRoundNum()-(1<<ClosestEnemyAttacker.ENEMY_MEMORY) && ClosestEnemyAttacker.enemy_exists){
				last_round_broadcast_enemy_attacker = rc.getRoundNum();
				returned_flag_value = ClosestEnemyAttacker.toBroadcastFlagValue();
				System.out.println("Broadcast closest enemy attacker");
			}
			else if(broadcast_enemy){
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


		////////////////////Sensing Begin
		UnitComms.BYTECODE_LIMIT = 2000;
		UnitComms.lookAroundBeforeMovement();
		////////////////////Sensing End

		////////////////////Receive Communication Begin
		receiveScoutCommunication();
		receiveNonScoutCommunication();
		////////////////////Receive Communication End
		
		//Due to the save_money boolean, it is crazy important that spawnRobot() is called before getBidValue()

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
		UnitComms.lookAroundAfterMovement();
		ClosestEnemyAttacker.forgetOldInfo(); //forgets old info to make sure we don't communicate something ambiguous
		int flag_value = generateFlagValue();
		if(rc.canSetFlag(flag_value)){
			rc.setFlag(flag_value);
		}

		System.out.println("No errors");
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

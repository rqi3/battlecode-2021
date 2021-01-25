package my_player;
import battlecode.common.*;
import java.util.*;

/**
 * RobotPlayer controls the actions of all robots and contains common methods.
 */
public strictfp class RobotPlayer {
	public static RobotController rc;

	private static final Direction[] directions = {
		Direction.NORTH,
		Direction.NORTHEAST,
		Direction.EAST,
		Direction.SOUTHEAST,
		Direction.SOUTH,
		Direction.SOUTHWEST,
		Direction.WEST,
		Direction.NORTHWEST,
	};

	static boolean just_made = true;
	static int turn_count;

	static boolean has_parent_EC = false; //whether this unit spawned by an Enlightenment Center
	static RobotInfo parent_EC; //the Enlightenment Center that spawned the unit, if it exists

	static List<Neutral_EC_Info> neutral_ecs = new ArrayList<>(); //List of information about neutral ecs that it knows
	static List<Enemy_EC_Info> enemy_ecs = new ArrayList<>(); //List of information about enemy ecs that it knows
	static List<Friend_EC_Info> friend_ecs = new ArrayList<>();; //List of information about friend ecs that it knows

	/**
	 * Computes relative location of loc with respect to parent_EC, assuming that this Robot has a parent enlightenment center
	 */
	static Point convertToRelativeCoordinates(MapLocation loc) {
		Point rel_loc = new Point();
		if(rc.getType() == RobotType.ENLIGHTENMENT_CENTER){
			rel_loc.x = loc.x-rc.getLocation().x;
			rel_loc.y = loc.y-rc.getLocation().y;
		}
		else{
			rel_loc.x = loc.x-parent_EC.getLocation().x;
			rel_loc.y = loc.y-parent_EC.getLocation().y;
		}

		return rel_loc;
	}

	/**
	 * Inverse of convertToRelativeCoordinates
	 */
	static MapLocation convertFromRelativeCoordinates(Point rel_loc) {
		return parent_EC.getLocation().translate(rel_loc.x, rel_loc.y);
	}

	/**
	 * Gets the nearest known neutral ec location by Euclidean Distance
	 * @return relative location of neutral ec
	 */
	static Point getClosestNeutralECLocation(){
		Point closest_neutral_ec = neutral_ecs.get(0).rel_loc;
		Point my_rel_loc = convertToRelativeCoordinates(rc.getLocation());
		for(int i = 1; i < RobotPlayer.neutral_ecs.size(); i++){
			int cur_dist = Point.getRadiusSquaredDistance(my_rel_loc, closest_neutral_ec);
			Point this_neutral_ec = RobotPlayer.neutral_ecs.get(i).rel_loc;
			int this_dist = Point.getRadiusSquaredDistance(my_rel_loc, this_neutral_ec);
			if(this_dist < cur_dist){
				closest_neutral_ec = this_neutral_ec;
			}
		}
		return closest_neutral_ec;
	}

	/**
	 * Gets the nearest known enemy ec location by Euclidean Distance
	 * @return relative location of enemy ec
	 */
	static Point getClosestEnemyECLocation(){
		Point closest_enemy_ec = RobotPlayer.enemy_ecs.get(0).rel_loc;
		Point my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());
		for(int i = 1; i < RobotPlayer.enemy_ecs.size(); i++){
			int cur_dist = Point.getRadiusSquaredDistance(my_rel_loc, closest_enemy_ec);
			Point this_enemy_ec = RobotPlayer.enemy_ecs.get(i).rel_loc;
			int this_dist = Point.getRadiusSquaredDistance(my_rel_loc, this_enemy_ec);
			if(this_dist < cur_dist){
				closest_enemy_ec = this_enemy_ec;
			}
		}
		return closest_enemy_ec;
	}

	/**
	 * Converts relative location to a flag value up to 2^14.
	 * Coordinates are from 0 to 126.
	 * @param rel_loc a relative location
	 * @return flag value
	 */
	static int convertToFlagRelativeLocation(Point rel_loc) {
		//System.out.println("converting from: " + rel_loc);
		int flag_loc = (rel_loc.x+63)+((rel_loc.y+63)<<7);

		//System.out.println("converting to: " + flag_loc);
		return flag_loc;
	}

	/**
	 * Inverts convertToFlagRelativeLocation
	 */
	static Point convertFromFlagRelativeLocation(int flag_loc) {
		//System.out.println("converting from: " + flag_loc);
		Point rel_loc = new Point();
		rel_loc.x = flag_loc % (1<<7) - 63;
		rel_loc.y = flag_loc/(1<<7) - 63;

		//System.out.println("converting to: " + rel_loc);
		return rel_loc;
	}

	/**
	 * reads bits l to r and concatenates them to form an integer.
	 * @return integer corresponding to the bits [l, r] in flag_value
	 */
	static int getBitsBetween(int flag_value, int l, int r){
		//return integer corresponding to the bits [l, r] in flag_value
		//return flag_value>>l&((1<<r-l+1)-1); //assuming java & cpp have same operator precedence, which I think is true
		int res = 0;
		for(int i = l; i <= r; i++){
			if((((flag_value)>>i)&1) == 1){
				res+=(1<<(i-l));
			}
		}
		return res;
	}

	/**
	 * Removes this ec location from all ec lists, usually so we can add an updated version back if its team changes.
	 * @param ec_rel_loc ec location in relative coordinates
	 */
	public static void removeECInfo(Point ec_rel_loc) {
		RobotPlayer.neutral_ecs.removeIf(ec -> ec.rel_loc.equals(ec_rel_loc));
		RobotPlayer.enemy_ecs.removeIf(ec -> ec.rel_loc.equals(ec_rel_loc));
		RobotPlayer.friend_ecs.removeIf(ec -> ec.rel_loc.equals(ec_rel_loc));
	}

	public static void removeECInfo(Neutral_EC_Info ec) {
		removeECInfo(ec.rel_loc);
	}
	public static void removeECInfo(Enemy_EC_Info ec) {
		removeECInfo(ec.rel_loc);
	}
    public static void removeECInfo(Friend_EC_Info ec) {
        removeECInfo(ec.rel_loc);
    }

    public static void addECInfo(Neutral_EC_Info ec){
		//System.out.println("New neutral ec information was obtained at location: " + ec.loc);

		for(int i = 0; i < neutral_ecs.size(); i++){
			if(neutral_ecs.get(i).rel_loc.equals(ec.rel_loc)){
				if(ec.influence != -1) neutral_ecs.set(i, ec);
				return;
			}
		}
		removeECInfo(ec.rel_loc);
		neutral_ecs.add(ec);
	}

	public static void addECInfo(Enemy_EC_Info ec){
		//System.out.println("adding enemy ECInfo...");
		for(int i = 0; i < enemy_ecs.size(); i++){
			//System.out.println("enemy ec at rel_loc: " + enemy_ecs.get(i).rel_loc);
			if(enemy_ecs.get(i).rel_loc.equals(ec.rel_loc)){
				//System.out.println("enemy ec reset");
				if(ec.influence != -1) enemy_ecs.set(i, ec);
				return;
			}
		}
		removeECInfo(ec.rel_loc);
		enemy_ecs.add(ec);
	}

	public static void addECInfo(Friend_EC_Info ec){
		int before_set = Clock.getBytecodeNum();
		for(int i = 0; i < friend_ecs.size(); i++){
			if(friend_ecs.get(i).rel_loc.equals(ec.rel_loc)){
				if(ec.influence != -1) friend_ecs.set(i, ec);
				return;
			}
		}
		//System.out.println("After set: " + (Clock.getBytecodeNum()-before_set));
		removeECInfo(ec.rel_loc);
		friend_ecs.add(ec);
	}

	/**
	 * When a unit is spawned, look for a parent EC that spawned it and edit parent_EC
	 * TODO: Local communication can also be stored in this information
	 * @return The value of the additional information on the flag of the parent EC, -1 if it does not exist
	 */
	static int assignParentEC() throws GameActionException
	/*
		 Returns additional bot parameters (or 0 by default), and -1 if error occurred
	*/
	{ //create parent_EC value
		if(rc.getType() == RobotType.ENLIGHTENMENT_CENTER){
			has_parent_EC = false;
			return 0; //if the robot is an Enlightenment Center it has no parent
		}


		RobotInfo[] close_robots = rc.senseNearbyRobots(2); //check adjacent tiles
		for(RobotInfo possible_parent: close_robots){
			if(!(possible_parent.getType() == RobotType.ENLIGHTENMENT_CENTER)){
				continue; //skip over it if it's not an Enlightenment Center
			}
			//System.out.println("FOUND CANDIDATE EC");
			if(possible_parent.getTeam() != rc.getTeam()){
				continue; //skip over if it's on the enemy team
			}
			//System.out.println("PASSED CONTINUE 1");
			if(!rc.canGetFlag(possible_parent.getID())){
				continue; //if we can't get its flag
			}
			//System.out.println("PASSED CONTINUE 2");
			int possible_parent_flag = rc.getFlag(possible_parent.getID());
			if(!EnlightenmentCenter.getFlagBotMade(possible_parent_flag)){
				continue; //if the Enlightenment Center's flag says it did not make a unit
			}
			//System.out.println("PASSED CONTINUE 3");
			//the direction in which the possible parent EC spawned a unit
			Direction possible_parent_spawn_direction = EnlightenmentCenter.getFlagDirectionMade(possible_parent_flag);

			//if this EC says they spawned a unit where I am, this is my parent!
			if(possible_parent.getLocation().add(possible_parent_spawn_direction).equals(rc.getLocation())){
				has_parent_EC = true;
				parent_EC = possible_parent;
				return possible_parent_flag >> 4; // FLAG
			}

		}
		//System.out.println("DID NOT FIND PARENT EC"); //should only occur for converted
		return -1;
	}

	/**
	 * Receives a broadcast from the parent_EC, if it exists.
	 * Updates ec lists.
	 * @throws GameActionException
	 */
	public static void receiveECBroadcast() throws GameActionException{
		if(!has_parent_EC) return;

		int ec_flag_value = rc.getFlag(parent_EC.getID());
		int is_global_broadcast_bit = getBitsBetween(ec_flag_value, 0, 0);
		if(is_global_broadcast_bit == 1) return; //careful, 1 means it is NOT a global broadcast.
		int flag_signal = getBitsBetween(ec_flag_value, 1, 3);

		if(flag_signal == 1){
			//neutral EC found
			Neutral_EC_Info neutral_ec = Neutral_EC_Info.fromBroadcastFlagValue(ec_flag_value);
			addECInfo(neutral_ec);
			UnitComms.receivedECBroadcast(neutral_ec);
			/*
			System.out.println("Neutral EC at location " + neutral_ec.rel_loc + " was broadcast to me.");
			System.out.println("Current neutral_ecs: ");
			for(Neutral_EC_Info a: neutral_ecs){
				System.out.println(a.rel_loc);
			}
			*/
		}
		else if(flag_signal == 2){
			//enemy EC found
			Enemy_EC_Info enemy_ec = Enemy_EC_Info.fromBroadcastFlagValue(ec_flag_value);
			//System.out.println("enemy ec broadcast: " + ec_flag_value);
			//System.out.println("enemy ec broadcast rel_loc: " + enemy_ec.rel_loc);
			addECInfo(enemy_ec);
			UnitComms.receivedECBroadcast(enemy_ec);
			/*
			System.out.println("Enemy EC at location " + enemy_ec.rel_loc + " was broadcast to me.");
			System.out.println("Current enemy_ecs: ");
			for(Enemy_EC_Info a: enemy_ecs){
				System.out.println(a.rel_loc);
			}
			*/
		}
		else if(flag_signal == 4 || flag_signal == 5){
			ClosestEnemyAttacker.updateUsingBroadcastFlagValue(ec_flag_value);
		}
	}



	/**
	 * run() is the method that is called when a robot is instantiated in the Battlecode world.
	 * If this method returns, the robot dies!
	 **/
	@SuppressWarnings("unused")
	public static void run(RobotController rc) throws GameActionException {
		// This is the RobotController object. You use it to perform actions from this robot,
		// and to get information on its current status.
		RobotPlayer.rc = rc;
		Movement.rc = rc;
		UnitComms.rc = rc;

		turn_count = 0;

		//System.out.println("I'm a " + rc.getType() + " and I just got created!");


		while (true) {
			turn_count += 1;
			// Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
			try {
				// Here, we've separated the controls into a different method for each RobotType.
				// You may rewrite this into your own control structure if you wish.

				//System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());

				switch (rc.getType()) {
					case ENLIGHTENMENT_CENTER: EnlightenmentCenter.run(); break;
					case POLITICIAN:		   Politician.run();		  break;
					case SLANDERER:			Slanderer.run();		   break;
					case MUCKRAKER:			Muckraker.run();		   break;
				}
				just_made = false;

				/*if(ClosestEnemyAttacker.enemy_exists){
					System.out.println("CLOSEST ENEMY AT LOC " + ClosestEnemyAttacker.enemy_position + " seen at round " + ClosestEnemyAttacker.round_seen);
				}*/

				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Tries to move in a specified direction
	 * @param dir direction of movement
	 * @return whether move succeeded
	 * @throws GameActionException Bad battlecode call
	 */
	static boolean tryMove(Direction dir) throws GameActionException {
		//System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		} else return false;
	}
}

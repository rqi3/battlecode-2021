package my_player;
import battlecode.common.*;
import java.util.*;

public strictfp class RobotPlayer {
	static RobotController rc;

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

	static int turn_count;

	static boolean has_parent_EC = false; //whether this unit spawned by an Enlightenment Center
	static RobotInfo parent_EC; //the Enlightenment Center that spawned the unit, if it exists

	static List<Neutral_EC_Info> neutral_ecs;
	static List<Enemy_EC_Info> enemy_ecs;
	static List<Friend_EC_Info> friend_ecs;

	static Point convertToRelativeCoordinates(MapLocation loc)
	/*
	Computes relative coordinates, assuming that it has a parent enlightenment center
	 */
	{
		Point rel_loc = new Point();
		rel_loc.x = loc.x-parent_EC.getLocation().x;
		rel_loc.y = loc.y-parent_EC.getLocation().y;
		return rel_loc;
	}

	static int convertToFlagRelativeLocation(Point rel_loc)
	//takes a relative location and converts it to a single integer up to 2^14
	{
		int flag_loc = (rel_loc.x+63)+((rel_loc.y+63)<<7);
		return flag_loc;
	}

	static Point convertFromFlagRelativeLocation(int flag_loc)
	//Inverts convertToFlagRelativeLocation
	{
		Point rel_loc = new Point();
		rel_loc.x = flag_loc % (1<<7) - 63;
		rel_loc.y = flag_loc/(1<<7) - 63;
		return rel_loc;
	}

	static int getBitsBetween(int flag_value, int l, int r){
		//return integer corresponding to the bits [l, r] in flag_value
		int res = 0;
		for(int i = l; i <= r; i++){
			if((((flag_value)>>i)&1) == 1){
				res+=(1<<(i-l));
			}
		}
		return res;
	}

	public static void removeECInfo(Point ec_rel_loc)
	/*
	If we just received information about an ec, remove it from the old information so we can safely add it.
	 */
	{
		RobotPlayer.neutral_ecs.removeIf(ec -> ec.rel_loc.equals(ec_rel_loc));
		RobotPlayer.enemy_ecs.removeIf(ec -> ec.rel_loc.equals(ec_rel_loc));
		RobotPlayer.friend_ecs.removeIf(ec -> ec.rel_loc.equals(ec_rel_loc));
	}
	public static void removeECInfo(Neutral_EC_Info ec)
	/*
	If we just received information about an ec, remove it from the old information so we can safely add it.
	 */
	{
		removeECInfo(ec.rel_loc);
	}
	public static void removeECInfo(Enemy_EC_Info ec)
	/*
	If we just received information about an ec, remove it from the old information so we can safely add it.
	 */
	{
		removeECInfo(ec.rel_loc);
	}

	public static void receiveECBroadcast() throws GameActionException{
		if(!has_parent_EC) return;

		int ec_flag_value = rc.getFlag(parent_EC.getID());
		int is_global_broadcast_bit = getBitsBetween(ec_flag_value, 0, 0);
		if(is_global_broadcast_bit == 1) return; //careful, 1 means it is NOT a global broadcast.
		int flag_signal = getBitsBetween(ec_flag_value, 1, 3);

		if(flag_signal == 1){
			//neutral EC found
			Neutral_EC_Info neutral_ec = Neutral_EC_Info.fromBroadcastFlagValue(ec_flag_value);
			removeECInfo(neutral_ec);
			neutral_ecs.add(neutral_ec);

			System.out.println("Neutral EC at location " + neutral_ec.rel_loc + " was broadcast to me.");
			System.out.println("Current neutral_ecs: ");
			for(Neutral_EC_Info a: neutral_ecs){
				System.out.println(a.rel_loc);
			}
		}
		else if(flag_signal == 2){
			//enemy EC found
			System.out.println("Enemy EC was broadcast to me.");
			Enemy_EC_Info enemy_ec = Enemy_EC_Info.fromBroadcastFlagValue(ec_flag_value);
			removeECInfo(enemy_ec);
			enemy_ecs.add(enemy_ec);

			System.out.println("Enemy EC at location " + enemy_ec.rel_loc + " was broadcast to me.");
			System.out.println("Current enemy_ecs: ");
			for(Enemy_EC_Info a: enemy_ecs){
				System.out.println(a.rel_loc);
			}
		}
	}


	static void assignParentEC() throws GameActionException{ //create parent_EC value
		if(rc.getType() == RobotType.ENLIGHTENMENT_CENTER){
			has_parent_EC = false;
			return; //if the robot is an Enlightenment Center it has no parent
		}


		RobotInfo[] close_robots = rc.senseNearbyRobots(2); //check adjacent tiles
		for(RobotInfo possible_parent: close_robots){
			if(!(possible_parent.getType() == RobotType.ENLIGHTENMENT_CENTER)){
				continue; //skip over it if it's not an Enlightenment Center
			}
			System.out.println("FOUND CANDIDATE EC");
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
				return;
			}

		}
		System.out.println("DID NOT FIND PARENT EC"); //should only occur for converted
	}



	/**
	 * run() is the method that is called when a robot is instantiated in the Battlecode world.
	 * If this method returns, the robot dies!
	 **/
	@SuppressWarnings("unused")

	public static void run(RobotController rc) throws GameActionException {

		//initialize ec arrays
		friend_ecs = new ArrayList<Friend_EC_Info>();
		enemy_ecs = new ArrayList<Enemy_EC_Info>();
		neutral_ecs = new ArrayList<Neutral_EC_Info>();

		// This is the RobotController object. You use it to perform actions from this robot,
		// and to get information on its current status.
		RobotPlayer.rc = rc;

		turn_count = 0;

		System.out.println("I'm a " + rc.getType() + " and I just got created!");
		assignParentEC(); //after it spawns, record which EC spawned it (if any)
		System.out.println("has_parent_EC: " + has_parent_EC);
		if(has_parent_EC){
			System.out.println("parent Location: " + parent_EC.getLocation());
		}

		while (true) {
			turn_count += 1;
			// Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
			try {
				// Here, we've separated the controls into a different method for each RobotType.
				// You may rewrite this into your own control structure if you wish.

				System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());

				switch (rc.getType()) {
					case ENLIGHTENMENT_CENTER: EnlightenmentCenter.run(); break;
					case POLITICIAN:		   Politician.run();		  break;
					case SLANDERER:			Slanderer.run();		   break;
					case MUCKRAKER:			Muckraker.run();		   break;
				}

				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println(rc.getType() + " Exception");
				e.printStackTrace();
			}
		}
	}
/* Example bot Code:
	static void runEnlightenmentCenter() throws GameActionException {
		RobotType toBuild = randomSpawnableRobotType();
		int influence = 50;
		for (Direction dir : directions) {
			if (rc.canBuildRobot(toBuild, dir, influence)) {
				rc.buildRobot(toBuild, dir, influence);
			} else {
				break;
			}
		}
	}

	static void runPolitician() throws GameActionException {
		Team enemy = rc.getTeam().opponent();
		int actionRadius = rc.getType().actionRadiusSquared;
		RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
		if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
			System.out.println("empowering...");
			rc.empower(actionRadius);
			System.out.println("empowered");
			return;
		}
		if (tryMove(randomDirection()))
			System.out.println("I moved!");
	}

	static void runSlanderer() throws GameActionException {
		if (tryMove(randomDirection()))
			System.out.println("I moved!");
	}

	static void runMuckraker() throws GameActionException {
		Team enemy = rc.getTeam().opponent();
		int actionRadius = rc.getType().actionRadiusSquared;
		for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
			if (robot.type.canBeExposed()) {
				// It's a slanderer... go get them!
				if (rc.canExpose(robot.location)) {
					System.out.println("e x p o s e d");
					rc.expose(robot.location);
					return;
				}
			}
		}
		if (tryMove(randomDirection()))
			System.out.println("I moved!");
	}
*/
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

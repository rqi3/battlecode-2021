package my_player;

import battlecode.common.*;
import java.util.*;

/**
 * Muckraker controls the actions of our Muckrakers.
 * @author	Coast
 */
public class Muckraker {
	static RobotController rc;
	static List<MapLocation> communicated_ecs = new ArrayList<>();

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
	public static final int LOST_MUCKRAKER = 0;
	public static final int SCOUT = 1;
	public static final int EC_ATTACKER = 2;

	static int muckraker_type = SCOUT;

	/**
	 * Check whether parent EC died and possibly change type if this happens.
	 */
	private static void updateParentEC() {
		if(!RobotPlayer.has_parent_EC) return;
		if(!rc.canGetFlag(RobotPlayer.parent_EC.getID())){ //parent EC died
			RobotPlayer.has_parent_EC = false; //should never change to true again
		}
	}

	static Point my_rel_loc; //if parent EC exists, stores relative location
	static RobotInfo[] all_nearby_robots;


	/**
	 * Updates my_rel_loc, all_nearby_robots.
	 * Find boundaries.
	 * Scout updates their visited sectors.
	 */
	static void lookAround() throws GameActionException {
		all_nearby_robots = rc.senseNearbyRobots();

		if(RobotPlayer.has_parent_EC){
			my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());
			//System.out.println("My relative location: " + my_rel_loc);
			for(int i = 0; i < 4; i++){
				if(Movement.boundaries[i] == -Movement.relative_boundary_directions[i])
				/*
				boundary hasn't been calculated yet
				 */
				{
					MapLocation loc = rc.getLocation();
					for(int j = 1;;j++){
						loc = loc.add(Movement.boundary_directions[i]);
						if(!rc.canSenseRadiusSquared(j*j)){
							break;
						}
						if(!rc.onTheMap(loc)){
							//found a boundary!
							if(i % 2 == 0){
								Movement.boundaries[i] = my_rel_loc.y;
							}
							else{
								Movement.boundaries[i] = my_rel_loc.x;
							}
							Movement.boundaries[i] += Movement.relative_boundary_directions[i]*(j-1);
							//System.out.println("Found " + Movement.boundary_directions[i] + " boundary at location " + Movement.boundaries[i]);
							updateBoundarySectors(i, Movement.boundaries[i]);
							break;
						}
					}
				}

			}
		}
	}

	/**
	 * Attacks an enemy slanderer if one is in range.
	 */
	static void attackSlanderer() throws GameActionException {
		Team enemy = rc.getTeam().opponent();
		int actionRadius = rc.getType().actionRadiusSquared;
		for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
			if (robot.type.canBeExposed()) {
				// It's a slanderer... go get them!
				if (rc.canExpose(robot.location)) {
					//System.out.println("exposed");
					rc.expose(robot.location);
				}
			}
		}
	}

	//////////////BEGIN SCOUT MOVEMENT CODE

	/**
	 * Sectors that the scout has visited
	 *  Starting sector is 8, 8.
	 *  Starting position in this sector is (3, 3) 0-indexed
	 */
	static int[][] visited_sectors = new int[17][17];
	static Point next_sector = new Point(8, 8); //the sector that this scout is trying to get to

	/**
	 * When we find a boundary, some sectors are declared invalid (visited = 3)
	 */
	private static void updateBoundarySectors(int boundary_type, int boundary_loc) {
		int boundary_sector_coordinate = Movement.getSector(boundary_loc);
		while(true){
			boundary_sector_coordinate+=Movement.relative_boundary_directions[boundary_type];
			if(0 <= boundary_sector_coordinate && boundary_sector_coordinate <= 16){
				if((boundary_type&1) == 0){
					for(int i = 0; i <= 16; i++){
						visited_sectors[i][boundary_sector_coordinate] = 3;
					}
				}
				else{
					for(int i = 0; i <= 16; i++){
						visited_sectors[boundary_sector_coordinate][i] = 3;
					}
				}
				//System.out.println("boundary_sector_coordinate: " + boundary_sector_coordinate);
			}
			else break;
		}
	}

	/**
	 *
	 * @param sector coordinates of the sector
	 * @return whether the sector is on the map (based on current information)
	 */
	private static boolean isValidSector(Point sector){
		if(sector.x < 0 || sector.x > 16 || sector.y < 0 || sector.y > 16) return false;
		if(visited_sectors[sector.x][sector.y] == 3) return false;
		return true;
	}


	static boolean roaming_sectors = false; //initially goes in the initial direction, then roams around sectors
	static double scout_initial_direction = 0;
	static int scout_initial_quadrant = 0;

	/**
	 *
	 * @return the value that scout_initial_direction will take on
	 */
	static double getInitialDirection(){
		double random_value = Math.random()*Math.random()+Math.random();
		while(random_value-1.0 >= 0) random_value-=1.0;
		int cardinal_direction = (int)(random_value*8);
		//System.out.println("cardinal_direction: " + cardinal_direction);
		double cardinal_angle = cardinal_direction*0.25*Math.PI;
		return cardinal_angle;
	}
	/**
	 * Assigns a next_sector based on the current sector (next_sector) and scout_initial_direction
	 * @return whether a next_sector was decided upon
	 */
	static boolean assignInitialDirectionSector(){
		if(scout_initial_quadrant == 0){
			if(scout_initial_direction <= 0.5*Math.PI){
				scout_initial_quadrant = 1;
			}
			else if(scout_initial_direction <= Math.PI){
				scout_initial_quadrant = 2;
			}
			else if(scout_initial_direction <= 1.5*Math.PI){
				scout_initial_quadrant = 3;
			}
			else{
				scout_initial_quadrant = 4;
			}
		}
		Point first_possible_sector = next_sector.clone(); //first in order of angle from theta=0
		Point second_possible_sector = next_sector.clone();
		Point third_possible_sector = next_sector.clone();
		double deciding_point_x = next_sector.x;
		double deciding_point_y = next_sector.y;
		if(scout_initial_quadrant == 1){
			first_possible_sector.x++;
			second_possible_sector.y++;
			third_possible_sector.x++; third_possible_sector.y++;
			deciding_point_x+=0.5;
			deciding_point_y+=0.5;
		}
		else if(scout_initial_quadrant == 2){
			//System.out.println("possible sectors: " + first_possible_sector + second_possible_sector);
			first_possible_sector.y++;
			second_possible_sector.x--;
			third_possible_sector.y++; third_possible_sector.x--;
			//System.out.println("possible sectors: " + first_possible_sector + second_possible_sector);
			deciding_point_x-=0.5;
			deciding_point_y+=0.5;
		}
		else if(scout_initial_quadrant == 3){
			first_possible_sector.x--;
			second_possible_sector.y--;
			third_possible_sector.x--; third_possible_sector.y--;
			deciding_point_x-=0.5;
			deciding_point_y-=0.5;
		}
		else if(scout_initial_quadrant == 4){
			first_possible_sector.y--;
			second_possible_sector.x++;
			third_possible_sector.y--; third_possible_sector.x++;
			deciding_point_x+=0.5;
			deciding_point_y-=0.5;
		}
		List<Point> possible_sectors = new ArrayList<>();
		possible_sectors.add(first_possible_sector);
		possible_sectors.add(second_possible_sector);
		possible_sectors.add(third_possible_sector);

		//System.out.println("possible sectors: " + first_possible_sector + second_possible_sector + third_possible_sector);
		Point correct_sector = new Point();
		double best_angle_dif = 9999;
		for(Point p: possible_sectors){
			double p_angle = Math.atan2(p.y-8.0, p.x-8.0);
			if(p_angle < 0.0) p_angle+=2*Math.PI;
			double p_dif = Math.abs(p_angle-scout_initial_direction);
			p_dif = Math.min(p_dif, 2*Math.PI-p_dif);

			if(p_dif < best_angle_dif){
				correct_sector = p;
				best_angle_dif = p_dif;
			}
		}

		if(isValidSector(correct_sector) && visited_sectors[correct_sector.x][correct_sector.y] == 0){
			next_sector = correct_sector;
			/*System.out.println("scout_initial_direction: " + scout_initial_direction);
			System.out.println("scout_initial_quadrant: " + scout_initial_quadrant);
			System.out.println("Possible sectors: " + first_possible_sector + ", " + second_possible_sector);
			System.out.println("Angle method found a next_sector: " + next_sector);*/
			return true;
		}
		//System.out.println("Directional sector not found: " + Clock.getBytecodeNum());
		return false;
	}


	static int[][] possible_new_sector_distances = new int[17][17];
	/**
	 * Chooses a new sector for this scout; updates next_sector
	 */
	static void assignNewSector() {
		Point my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());

		//next_sector should currently be the current sector
		next_sector.x = Movement.getSector(my_rel_loc.x);
		next_sector.y = Movement.getSector(my_rel_loc.y);

		if(!roaming_sectors){
			boolean found_directional_sector = assignInitialDirectionSector();
			if(found_directional_sector){
				//System.out.println("Initial direction: " + scout_initial_direction);
				//System.out.println("next_sector: " + next_sector);
				return;
			}
			roaming_sectors = true;
		}



		int min_sector_dist = 9999;

		int low_x = Math.max(0, next_sector.x-2);
		int high_x = Math.min(16, next_sector.x+2);
		int low_y = Math.max(0, next_sector.y-2);
		int high_y = Math.min(16, next_sector.y+2);
		for(int i = low_x; i <= high_x; i++){
			for(int j = low_y; j <= high_y; j++){
				if(visited_sectors[i][j] != 0) {
					possible_new_sector_distances[i][j] = 9999;
					continue;
				}
				possible_new_sector_distances[i][j] = Math.max(Math.abs(next_sector.x-i), Math.abs(next_sector.y-j));
				min_sector_dist = Math.min(min_sector_dist, possible_new_sector_distances[i][j]);
			}
		}
		if(min_sector_dist < 9999){
			List<Point> min_sector0s = new ArrayList<>();
			//System.out.println("Curbytecode Sector: " + Clock.getBytecodeNum());
			for(int i = low_x; i <= high_x; i++){
				for(int j = low_y; j <= high_y; j++){
					if(possible_new_sector_distances[i][j] == min_sector_dist){
						min_sector0s.add(new Point(i, j));
					}
				}
			}
			int min_sector0s_ind = (int)(Math.random()*min_sector0s.size());
			next_sector = min_sector0s.get(min_sector0s_ind);
			//System.out.println("Found new unvisited sector: " + next_sector);
			//System.out.println("Found new unvisited bytecode: " + Clock.getBytecodeNum());
			return;
		}

		List<Point> sector1s = new ArrayList<>();

		for(int i = low_x; i <= high_x; i++){
			for(int j = low_y; j <= high_y; j++){
				if(i == next_sector.x && j == next_sector.y) continue; //don't choose the same
				if(visited_sectors[i][j] == 3) continue; //impossible sector

				Point new_sector = new Point(i, j);
				if(visited_sectors[i][j] == 1){
					sector1s.add(new_sector);
				}
			}
		}

		//if sector hasn't been decided, choose the closest visited = 1 sector
		if(sector1s.size() > 0){
			int sector1s_ind = (int)(Math.random()*sector1s.size());
			next_sector = sector1s.get(sector1s_ind);
			return;
		}
		//System.out.println("NEXT SECTOR NOT FOUND");
	}

	/**
	 * Movement code for Scouts
	 */
	private static void moveScout() throws GameActionException {
		visited_sectors[8][8] = 1; //default set your starting sector

		if(Movement.moved_to_destination){
			if(visited_sectors[next_sector.x][next_sector.y] == 0){
				visited_sectors[next_sector.x][next_sector.y] = 1;
			}
		}

		/*
		If you reached the destination sector or the sector is now invalid,
		assign a new sector & destination.
		 */
		if(visited_sectors[next_sector.x][next_sector.y] != 0) {
			assignNewSector();
			Movement.assignDestination(Movement.getSectorLoc(next_sector));
		}

		//System.out.println("Current Sector: " + next_sector);
		//System.out.println("Current Sector Destination: " + Movement.current_destination);
		Movement.moveToDestination();

		if (tryMove(randomDirection()))
		{
			//System.out.println("I moved!");
		}
	}

	///////////////END OF SCOUT MOVEMENT CODE


	static Point goal = null;
	/**
	 * Moves an attacker to surround goal (a chosen enemy_ec)
	 * @throws GameActionException
	 */
	public static void moveAttacker() throws GameActionException {
		Point my_rel_loc = RobotPlayer.convertToRelativeCoordinates(rc.getLocation());

		if(goal == null){
			if(RobotPlayer.enemy_ecs.size() > 0) {
				goal = RobotPlayer.enemy_ecs.get((int)(Math.random()*RobotPlayer.enemy_ecs.size())).rel_loc;
			}
		}

		if(goal == null){
			moveScout();
		}
		else {
			//System.out.println("moveAttacker: " + goal);
			if(Point.getMaxXYDistance(goal, my_rel_loc) <= 1){
				//it has done its job and is next to the enemy base
				return;
			}
			assert(RobotPlayer.has_parent_EC); //destination was not defined if this has no parent_EC

			Movement.moveToNaive(goal);
			Direction best_direction = Direction.CENTER;
			int lowest_maxXY_distance = Point.getMaxXYDistance(my_rel_loc, goal);;

			for(Direction dir: directions){
				if(rc.canMove(dir)){
					Point new_loc = my_rel_loc.add(dir);

					int new_maxXY_distance = Point.getMaxXYDistance(new_loc, goal);
					if(new_maxXY_distance <= lowest_maxXY_distance){
						best_direction = dir;
						lowest_maxXY_distance = new_maxXY_distance;
					}
				}

			}

			if(best_direction!=Direction.CENTER){
				rc.move(best_direction);
			}
		}
	}

	public static void run() throws GameActionException{
		////////////////////Creation Begin
		if(RobotPlayer.just_made){


			rc = RobotPlayer.rc;
			Movement.rc = RobotPlayer.rc;

			int parent_ec_info = RobotPlayer.assignParentEC(); //after it spawns, record which EC spawned it (if any)

			if(parent_ec_info == -1){
				muckraker_type = LOST_MUCKRAKER;
			}
			else{
				muckraker_type = RobotPlayer.getBitsBetween(parent_ec_info, 0, 1);
			}

			if(muckraker_type == SCOUT){
				scout_initial_direction = RobotPlayer.getBitsBetween(parent_ec_info, 2, 4)*0.25*Math.PI;
			}
			else{
				scout_initial_direction = getInitialDirection();
			}
			//System.out.println("parent EC direction: " + RobotPlayer.getBitsBetween(parent_ec_info, 2, 4));
			//System.out.println("scout_initial_direction: " + scout_initial_direction);
		}
		////////////////////Creation End

		//////////////////// Begin Initialization
		updateParentEC();
		if(!RobotPlayer.has_parent_EC){
			muckraker_type = LOST_MUCKRAKER;
		}

		//////////////////// End Initialization
		//System.out.println("I am a type " + muckraker_type + " muckraker.");
		//////////////////// Begin Sense
		lookAround();
		UnitComms.lookAroundBeforeMovement();
		//////////////////// End Sense

		//////////////////// Begin Receive Broadcast
		RobotPlayer.receiveECBroadcast();
		//////////////////// End Receive Broadcast
		//////////////////// Attack Begin
		attackSlanderer(); //assume that we always want to attack if we can
		//////////////////// Attack End
		//////////////////// Movement Begin
		if(muckraker_type == SCOUT){ //movement for scout
			moveScout();
		}
		else if(muckraker_type == EC_ATTACKER){
			moveAttacker();
		}
		//////////////////// Movement End

		////////////////////Send Communication Begin
		UnitComms.lookAroundAfterMovement();

		ClosestEnemyAttacker.forgetOldInfo(); //forgets old info to make sure we don't communicate something ambiguous
		int flag_value = UnitComms.generateFlagValue();
		if(rc.canSetFlag(flag_value)){
			rc.setFlag(flag_value);
		}
		////////////////////Send Communication End

		//TODO: CHANGE TYPE FROM SCOUT TO ATTACKER BASED ON SOME CONDITION, which will change flag
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
	 * Attempts to move in a given direction.
	 *
	 * @param dir The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryMove(Direction dir) throws GameActionException {
		//System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		} else return false;
	}
}

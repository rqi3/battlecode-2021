package my_player;

import battlecode.common.*;
import java.util.*;

public class EnlightenmentCenter {
	static RobotController rc;

	public static final int OPTIMAL_SLANDERER_INFLUENCE[] = {21,41,63,85,107,130,154,178,203,228,255,282,310,339,368,399,431,463,497,532,568,605,643,683,724,766,810,855,902,949};

	static boolean bot_made_last_turn = false;
	static Direction bot_direction_last_turn = Direction.NORTH; //
	static boolean bot_made_this_turn = false; //was a bot made this turn?
	static Direction bot_direction_this_turn = Direction.NORTH; //direction the bot is facing

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

	static int getBidValue(){ //returns the value this Enlightenment Center will bid
		return 2;

	}

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

	public static void spawnRobot() throws GameActionException
	/*
			Spawns robots
			Current algorithm:
				Spawns muckraker by default. Randomly spawn slanderer according to slanderer_frequency
				slanderer_investment is current set at 10% of enlightenment center influence
	*/
	{
		RobotType toBuild = RobotType.MUCKRAKER;
		int influence = 1;

		if(Math.random() < slanderer_frequency)
		{
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

		for (Direction dir : directions) {
			if (rc.canBuildRobot(toBuild, dir, influence)) {
				rc.buildRobot(toBuild, dir, influence);
				bot_made_this_turn = true;
				bot_direction_this_turn = dir;
				System.out.println("Made bot in Direction: " + dir);
				break;
			}
		}
		slanderer_frequency = Math.min(slanderer_frequency+0.05f, 1f);
	}

	public static void run() throws GameActionException{
		//initialization
		rc = RobotPlayer.rc;

		update_bot_made_lastorthis_turn();

		//Receive Flag Communication

		spawnRobot();

		//Bidding using the getBidValue function
		int bid_value = getBidValue();
		if(rc.canBid(bid_value)){
			rc.bid(bid_value);
		}

		//Flag Send Communication
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
}

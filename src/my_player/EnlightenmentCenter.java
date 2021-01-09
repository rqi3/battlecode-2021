package my_player;

import battlecode.common.*;

public class EnlightenmentCenter {
    static RobotController rc;
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

    static int getBidValue(){ //returns the value this Enlightenment Center will bid
        return 2;

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
                for(int j = 0; j < 3; j++){
                    if(((dir>>j)&1) == 1){
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
        bot_made_last_turn = bot_made_this_turn;
        bot_made_this_turn = false;

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

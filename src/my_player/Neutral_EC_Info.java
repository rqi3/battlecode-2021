package my_player;

import battlecode.common.*;
import java.util.*;

public class Neutral_EC_Info {
    public int x; //x position relative to parent EC of unit that is accessing this
    public int y; //y position relative to parent EC of unit that is accessing this

    public void setPosition(int _x, int _y){
        x = _x; y = _y;
    }
}

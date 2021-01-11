package my_player;

import battlecode.common.*;
import java.util.*;

public class Point {

	public int x, y;

	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Point()
	{
		this(0, 0);
	}

	public Point add(Direction dir){
		Point newp = new Point(x, y);
		for(int i = 0; i < 8; i++){
			if(Movement.directions[i] == dir){
				newp.x+=Movement.directions_x[i];
				newp.y+=Movement.directions_y[i];
				break;
			}
		}
		return newp;
	}


	public boolean equals(Object o){
		if(o == this) return true;

		if (!(o instanceof Point)) {
			return false;
		}

		Point p = (Point)(o);
		if(x == p.x && y == p.y) return true;
		return false;
	}


	public String toString(){
		return "(" + x + "," + y + ")";
	}
}

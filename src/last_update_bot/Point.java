package last_update_bot;

import battlecode.common.*;
import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.max;

/**
 * A pair of integers, typically describing a relative location or coordinate.
 */
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

	public Point clone(){ return new Point(x, y);}

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

	public static int getRadiusSquaredDistance(Point a, Point b){
		return (b.x-a.x)*(b.x-a.x)+(b.y-a.y)*(b.y-a.y);
	}
	public static int getMaxXYDistance(Point a, Point b){
		return max(abs(b.x-a.x),abs(b.y-a.y));
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
	
	public int hashCode() {
		return 150*x + y; 
	}
}

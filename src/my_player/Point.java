package my_player;

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

package nathan_1_23_2021;

import java.lang.Math.*;

public class Vec {

	public float x, y;

	public Vec(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public Vec()
	{
		this(0, 0);
	}

	public Vec clone()
	{
		return new Vec(x, y);
	}

	public void add(Vec o) {x += o.x; y += o.y;}
	public void sub(Vec o) {x -= o.x; y -= o.y;}
	public void mul(float o) {x *= o; y *= o;}

	public float mag() {return (float)Math.hypot(x, y);}
	public float mag2() {return x*x+y*y;}
	public float ang() {return (float)Math.atan2(y, x);}
	public float dot(Vec o) {return x*o.x+y*o.y;}
	public float cross(Vec o) {return x*o.y-y*o.x;}

	public static Vec sum(Vec a, Vec b) {return new Vec(a.x+b.x, a.y+b.y);}
	public static Vec dif(Vec a, Vec b) {return new Vec(a.x-b.x, a.y-b.y);}
	public static Vec prod(Vec a, float b) {return new Vec(a.x*b, a.y*b);}
}

package pathfindingtest.common;

import battlecode.common.*;

import static java.lang.Math.abs;
import static java.lang.Math.max;

public class Displacement {

    public int x, y;

    public Displacement(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Displacement()
    {
        this(0, 0);
    }

    public Displacement duplicate()
    {
        return new Displacement(x,y);
    }

    public Displacement add(Displacement dir){
        return new Displacement(x+dir.x,y+dir.y);
    }

    public static final Displacement CENTER=new Displacement(0,0);
    public static final Displacement NORTH=new Displacement(0,1);
    public static final Displacement SOUTH=new Displacement(0,-1);
    public static final Displacement WEST=new Displacement(-1,0);
    public static final Displacement EAST=new Displacement(1,0);
    public static final Displacement NORTHEAST=NORTH.add(EAST);
    public static final Displacement NORTHWEST=NORTH.add(WEST);
    public static final Displacement SOUTHEAST=SOUTH.add(EAST);
    public static final Displacement SOUTHWEST=SOUTH.add(WEST);

    public static final Displacement[] DirectionsWithoutCenter=
            {
                    NORTH,NORTHEAST,EAST,SOUTHEAST,SOUTH,SOUTHWEST,WEST,NORTHWEST
            };
    public static final Displacement[] DirectionsWithCenter=
            {
                    NORTH,NORTHEAST,EAST,SOUTHEAST,SOUTH,SOUTHWEST,WEST,NORTHWEST,CENTER
            };
    public static final Displacement[] DirectionsPerpendicular=
            {
                    NORTH,EAST,SOUTH,WEST
            };
    public static final Displacement[] DirectionsDiagonal=
            {
                    NORTHEAST,SOUTHEAST,SOUTHWEST,NORTHWEST
            };

    static final Direction[][] cordtodir={
            {Direction.SOUTHWEST,Direction.WEST,Direction.NORTHWEST},
            {Direction.SOUTH,Direction.CENTER,Direction.NORTH},
            {Direction.SOUTHEAST,Direction.EAST,Direction.NORTHEAST}
    };
    public static Direction displacementToDirection(Displacement disp)
    {
        assert(-1<=disp.x&&disp.x<=1);
        assert(-1<=disp.y&&disp.y<=1);
        return cordtodir[disp.x+1][disp.y+1];
    }
    static final Displacement[] dirtocord=new Displacement[9];
    public static void init()
    {
        for(int i=-1;i<=1;i++)
            for(int j=-1;j<=1;j++) {
                Displacement disp = new Displacement(i, j);
                dirtocord[displacementToDirection(disp).ordinal()] = disp;
            }
    }
    public static Displacement directionToDisplacement(Direction dir)
    {
        return dirtocord[dir.ordinal()].duplicate();
    }

    public static int getRadiusSquaredDistance(MapLocation a, MapLocation b){
        return (b.x-a.x)*(b.x-a.x)+(b.y-a.y)*(b.y-a.y);
    }
    public static int getMaxXYDistance(MapLocation a, MapLocation b){
        return max(abs(b.x-a.x),abs(b.y-a.y));
    }
    public static int getMaxXYNorm(Displacement disp){
        return max(abs(disp.x),abs(disp.y));
    }
    public boolean equals(Object o){
        if(o == this) return true;

        if (!(o instanceof pathfindingtest.common.Displacement)) {
            return false;
        }

        Displacement p= (Displacement)(o);
        if(x == p.x && y == p.y) return true;
        return false;
    }

    public String toString(){
        return "(" + x + "," + y + ")";
    }

    public static MapLocation add(MapLocation ml, Displacement disp)
    {
        return new MapLocation(ml.x+disp.x,ml.y+disp.y);
    }
    public static Displacement subtract(Displacement disp1, Displacement disp2)
    {
        return new Displacement(disp1.x-disp2.x,disp1.y-disp2.y);
    }
    public static MapLocation subtract(MapLocation ml, Displacement disp)
    {
        return new MapLocation(ml.x-disp.x,ml.y-disp.y);
    }
    public static Displacement subtract(MapLocation ml, MapLocation ml2)
    {
        return new Displacement(ml.x-ml2.x,ml.y-ml2.y);
    }
    public static MapLocation getGrid(MapLocation m1)
    {
        return new MapLocation((m1.x/3)*3+1,(m1.y/3)*3+1);
    }
    public static Boolean isGrid(MapLocation m1)
    {
        return m1.x%3==1&&m1.y%3==1&&(m1.x+m1.y)%2==0;
    }

    public static Displacement scale(Displacement disp,int factor)
    {
        return new Displacement(disp.x*factor,disp.y*factor);
    }
}


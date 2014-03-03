/***
Graham Scan adopted from Segwick's Algorithm Book, retrieved from:
http://algs4.cs.princeton.edu/99hull/GrahamScan.java.html

Polar Comparator from Bo Majewski, retrieved fom
http://read.pudn.com/downloads157/sourcecode/java/699192/org/bluear/cg/hull/GrahamScanHull.java__.htm

**/


package sn.recover;
import java.util.Arrays;
import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.Stack;
import java.util.List;




public class GrahamScan {
	// The set of points that is the convex hull after the 
    private  Stack<Point2D> hull = new Stack<Point2D>();

    public GrahamScan(Point2D[] pts) {

        // defensive copy
        int N = pts.length;
        Point2D[] points = new Point2D[N];
        for (int i = 0; i < N; i++)
            points[i] = pts[i];

        // preprocess so that points[0] has lowest y-coordinate; break ties by x-coordinate
        // points[0] is an extreme point of the convex hull
        // (alternatively, could do easily in linear time)
        Arrays.sort(points, new YCompare());

        // sort by polar angle with respect to base point points[0],
        // breaking ties by distance to points[0]
        Arrays.sort(points, 1, N, new PolarComparator(points[0]));

        hull.push(points[0]);       // p[0] is first extreme point

        // find index k1 of first point not equal to points[0]
        int k1;
        for (k1 = 1; k1 < N; k1++)
            if (!points[0].equals(points[k1])) break;
        if (k1 == N) return;        // all points equal

        // find index k2 of first point not collinear with points[0] and points[k1]
        int k2;
        for (k2 = k1 + 1; k2 < N; k2++)
            if (ccw(points[0], points[k1], points[k2]) != 0) break;
        hull.push(points[k2-1]);    // points[k2-1] is second extreme point

        // Graham scan; note that points[N-1] is extreme point different from points[0]
        for (int i = k2; i < N; i++) {
            Point2D top = hull.pop();
            while (ccw(hull.peek(), top, points[i]) <= 0) {
                top = hull.pop();
            }
            hull.push(top);
            hull.push(points[i]);
        }

        assert isConvex();
    }

    // return extreme points on convex hull in counterclockwise order as an Iterable
    public Iterable<Point2D> hull() {
        Stack<Point2D> s = new Stack<Point2D>();
        for (Point2D p : hull) s.push(p);
        return s;
    }

    // check that boundary of hull is strictly convex
    private boolean isConvex() {
        int N = hull.size();
        if (N <= 2) return true;

        Point2D[] points = new Point2D[N];
        int n = 0;
        for (Point2D p : hull()) {
            points[n++] = p;
        }

        for (int i = 0; i < N; i++) {
            if (ccw(points[i], points[(i+1) % N], points[(i+2) % N]) <= 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Is a->b->c a counterclockwise turn?
     * @param a first point
     * @param b second point
     * @param c third point
     * @return { -1, 0, +1 } if a->b->c is a { clockwise, collinear; counterclocwise } turn.
     */
    public static int ccw(Point2D a, Point2D b, Point2D c) {
        double area2 = (b.getX()-a.getX())*(c.getY()-a.getY()) - (b.getY()-a.getY())*(c.getX()-a.getX());
        if      (area2 < 0) return -1;
        else if (area2 > 0) return +1;
        else                return  0;
    }
    
    // Compare the Y between two points.
 	private class YCompare implements Comparator<Point2D> {
 		@Override
 		public int compare(Point2D o1, Point2D o2) {
 			return (new Double(o1.getY())).compareTo(new Double(o2.getY()));
 		}
 	}
    
    
    private static class PolarComparator implements Comparator {  
        
        private Point2D p0;  
          
        /** 
         * Creates a new comparator that used the given pivot point. 
         *  
         * @param p0 The pivot point. 
         */  
        public PolarComparator(Point2D p0) {  
            this.p0 = p0;  
        }  
  
        /** 
         * Compares object o1 to object o2. This method returns a negative 
         * value if o1 lies to the left of o2 with respect to the pivot p0. 
         *  
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object) 
         */  
        public int compare(Object o1, Object o2) {  
            Point2D p1 = (Point2D) o1;  
            Point2D p2 = (Point2D) o2;  
              
            // take care of degenerate cases first ...  
            if (p1.equals(this.p0)) {  
                return (p2.equals(this.p0)? 0 : -1);  
            }  
              
            if (p2.equals(this.p0)) {  
                return (1);  
            }  
              
            // this is when neither p1 nor p2 is identical to pivot ...  
            double d = ((p2.getX() - p0.getX())*(p1.getY() - p0.getY())   
                       - (p1.getX() - p0.getX())*(p2.getY() - p0.getY()));  
              
            return (d < 0.0? -1 : d > 0.0? 1 : 0);  
        }  
    }
    
    public List<Point2D> getList(){
    	return (List<Point2D>) hull;
    }

    // test client
    public static void main(String[] args) {
        ;
    }

}
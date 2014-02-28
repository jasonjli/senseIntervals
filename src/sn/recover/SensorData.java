package sn.recover;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import javax.imageio.ImageIO;


// The class for one set of sensor data
public class SensorData {
	
	// variables in the class
	// list of positive intervals detected in data
	private List<Interval> positiveIntervals = new ArrayList<Interval>();	
	
	// Angle of the parallel positive intervals in radians
	private double sensorAngle=Double.NaN; // initiated at NaN
	private double sensorGap = Double.NaN; // initiated at NaN
	private int sensorCount = Integer.MIN_VALUE; // initiated at min value

	// sub-class for intervals, denoting positive components detected in sensor data
	public class Interval{		
		private int sensorID;	// the id of the sensor where this interval belongs
		private Line2D interval;
		
		// constructor
		public Interval(int id, Point2D s, Point2D e){
			sensorID = id; 
			interval = new Line2D.Double(s,e);
		}
				
		// methods for accessing interval information
		public int getSensorID(){
			return sensorID;
		}
		
		public Point2D getStart(){
			return interval.getP1();
		}
		
		public Point2D getEnd(){
			return interval.getP2();
		}
		
	}	
	
	// Constructor from a given file
	public SensorData(String sensorFileName){
		File file = new File(sensorFileName);
		
		if (!file.exists()){
			System.err.println("failed to read file: " + sensorFileName);
			System.exit(0);
		}
		
		BufferedReader reader = null;
		try{
			reader = new BufferedReader(new FileReader(file));
			String sensorData = null;
			int sensorId=-1;
			
			// variables to work out distance SensorData parameters
			int prevSensorID = -1;
			Point2D prevPoint = new Point2D.Double(Double.MAX_VALUE,Double.MAX_VALUE);
			int maxSensor = Integer.MIN_VALUE;			
			// Read each line
			// Each line is a single positive component of the format:
			// Sensor(\d+) [startPt.x,startPt.y] [endPt.x, endPt.y]
			while ((sensorData = reader.readLine()) != null) {
				
				// parse the string, detecting positive component
				String[] data = sensorData.split(" ");
				sensorId = Integer.parseInt(data[0].split("Sensor")[1]);
				
				// update count of sensor
				if (sensorId > maxSensor){
					maxSensor = sensorId;
				}
												
				String p1 = data[1].substring(1, data[1].length() - 1);
				double x1 = Double.parseDouble(p1.split(",")[0]);
				double y1 = Double.parseDouble(p1.split(",")[1]);
				Point2D start = new Point2D.Double(x1, y1);

				String p2 = data[2].substring(1, data[2].length() - 1);
				double x2 = Double.parseDouble(p2.split(",")[0]);
				double y2 = Double.parseDouble(p2.split(",")[1]);
				Point2D end = new Point2D.Double(x2, y2);
				
				double angle = Math.atan2(y2-y1, x2-x1);
				
				
				// set gradient if we see it for the first time. 
				if (sensorAngle == Double.NaN){
					sensorAngle = angle;
				}
				
				// enforce parallel positive intervals
				assert (sensorAngle == angle): 
					"In file " + sensorFileName +", positive intervals not parallel!";				
				
				// if this was not the first sensor we saw with positive interval
				// work out the distance between adjacent intervals
				if (prevSensorID != sensorId && prevSensorID != -1){					
					
					assert(true) : "got here!";
					
					double dY = y1 - prevPoint.getY();
					double dX = x1 - prevPoint.getX();
					
					// Distance between 
					double hyp = prevPoint.distance(start);
					double tmpAngle = Math.atan2(dY, dX)-sensorAngle;
					
					sensorGap = Math.sin(tmpAngle) * hyp / (sensorId - prevSensorID);
				}
				else{
					prevSensorID = sensorId;
					prevPoint = start;
				}
				
				// add the interval to data
				addPositiveInterval(new Interval(sensorId,start,end));
			}
			
			// update sensor count
			sensorCount = maxSensor;
			
			// Print sensor data information
			System.out.println("File " + sensorFileName + " read.");
			System.out.println("Sensor count" + sensorCount);
			System.out.println("Angle " + sensorAngle);
			System.out.println("Sensor gap " + sensorGap);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// public methods for modifying variables in the class
	public void addPositiveInterval(Interval positiveInterval){
		positiveIntervals.add(positiveInterval);
	}
	
	// public method for reading variables in the class	
	
	// read list of intervals
	public List<Interval> getPositiveIntervals(){
		return positiveIntervals;
	}
	
	// read angle of intervals in atan2
	public double getAngle(){
		return sensorAngle;
	}
	
	// read list of coordinates that make up the positive intervals
	public List<Point2D> getPositiveCoordinates(){
		List<Point2D> positiveCoordinates = new ArrayList<Point2D>();
		
		for (int i=0; i<positiveIntervals.size(); i++){
			positiveCoordinates.add(positiveIntervals.get(i).getStart());
			positiveCoordinates.add(positiveIntervals.get(i).getEnd());
		}
		
		return positiveCoordinates;
	}
	
	// read the convex hull of the positive intervals
	public List<Point2D> getConvexHull(){
		List<Point2D> coordList = getPositiveCoordinates();
		Point2D[] coords = new Point2D[coordList.size()];					
		
		coordList.toArray(coords);
		
		
		GrahamScan scan = new GrahamScan(coords);		
		return (List<Point2D>) scan.hull();
	}
	
	// Drawing stuff
	
	// draw the positive intervals to a specified filename
	public void drawPositiveIntervals(String filename){
		
		// Initialize image
		BufferedImage img = new BufferedImage(1024, 800,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();
		g2d.setColor(Color.BLACK);
		
		// Calculate offsets
		double xOffset=0, yOffset=0;
		
		// calculate hull offset for drawing
		// otherwise it draws outside the canvas.
		double minX=Double.MAX_VALUE, minY=Double.MIN_VALUE;
		for (int i=0; i<positiveIntervals.size(); i++){
			Interval curInterval = positiveIntervals.get(i);
			if (curInterval.getStart().getX() < minX){
				minX = curInterval.getStart().getX();				
			}
			if (curInterval.getStart().getY() < minY){
				minY = curInterval.getStart().getY();				
			}
		}
		
		if (minX < 20){
			xOffset = 20-minX;
		}
		if (minY < 20){
			yOffset = 20-minY;
		}
		
		// Draw components					
		for (int i=0; i<positiveIntervals.size(); i++){
			Interval curInterval = positiveIntervals.get(i);
			Path2D path = new Path2D.Double();
			path.moveTo(curInterval.getStart().getX()+xOffset, curInterval.getStart().getY()+yOffset);
			path.lineTo(curInterval.getEnd().getX()+xOffset, curInterval.getEnd().getY()+yOffset);
			path.closePath();
			g2d.draw(path);
		}
		
		// Write to file
		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}
	}
	
	// draw the convex hull of the positive intervals to a specified filename
	public void drawConvexHull(String filename){
						
		// get the convex hull
		List <Point2D> hull = getConvexHull();
		
		double xOffset=0, yOffset=0;
		
		// calculate hull offset for drawing
		// otherwise it draws outside the canvas.
		double minX=Double.MAX_VALUE, minY=Double.MIN_VALUE;
		for (int i=0; i<hull.size(); i++){
			if (hull.get(i).getX() < minX){
				minX = hull.get(i).getX();				
			}
			if (hull.get(i).getY() < minY){
				minY = hull.get(i).getY();				
			}
		}
		
		if (minX < 20){
			xOffset = 20-minX;
		}
		if (minY < 20){
			yOffset = 20-minY;
		}
		
		Path2D hullPath = new Path2D.Double();
		hullPath.moveTo(hull.get(0).getX()+xOffset, hull.get(0).getY()+yOffset);
		for (int i = 1; i < hull.size(); i++) {
			hullPath.lineTo(hull.get(i).getX()+xOffset, hull.get(i).getY()+yOffset);
		}

		hullPath.closePath();
		
		// draw the image
		BufferedImage img = new BufferedImage(1024, 800,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();
		g2d.setColor(Color.BLACK);
		g2d.draw(hullPath);
		
		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}
	}

	// tests
	
	// test if two SensorData is compatible, with constraints:
	// The two have at least one intersection in the positive component
	// The two have no intersection between positive and negative components
	public boolean isCompatible(SensorData otherData){
		
		boolean positiveIntersect = false;		
		
		// to be done. 
		
		return positiveIntersect;
	}
	
	// test parsing file "data/test0000-linecoord[0..2]"
	// draw their convex hull
	public static void testDraw(){
		
		String filePrefix = "data/test0000-linecoordnorm";
		for (int i = 0; i<=2; i++){
			String filename = filePrefix + "[" + i + "]";
			SensorData d = new SensorData(filename);
			
			String hullpicname = filename + "-hull.png";
			d.drawConvexHull(hullpicname);
			
			String intervalPicName = filename + "-intervals.png";
			d.drawPositiveIntervals(intervalPicName);
			
			System.out.println("Drawn " + hullpicname +", " + intervalPicName);
		}		
	}
	
	public static void main(String[] args){
		testDraw();
	}
}

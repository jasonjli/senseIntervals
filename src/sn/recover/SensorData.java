package sn.recover;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
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
	private List<SensorInterval> positiveIntervals;	
	
	// Angle of the parallel positive intervals in radians
	private double sensorAngle;
	// Distance between adjacent sensor lines
	private double sensorGap;
	// the count of number of sensors
	private int sensorCount;

	
	
	// Constructor from a given file
	public SensorData(String sensorFileName){
		
		// initialize variables
		positiveIntervals = new ArrayList<SensorInterval>();
		sensorAngle=Double.NaN; // initiated at NaN
		sensorGap = Double.NaN; // initiated at NaN
		sensorCount = Integer.MIN_VALUE; // initiated at min value
		
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
			
			boolean sensorGapSet = false;
			boolean angleSet = false;
			
			// variables to work out distance SensorData parameters
			int prevSensorID = -1;
			Point2D prevPoint = new Point2D.Double(Double.MAX_VALUE,Double.MAX_VALUE);
			int maxSensor = Integer.MIN_VALUE;			
			// Read each line
			// Each line is a single positive component of the format:
			// Sensor(\d+) [startPt.x,startPt.y] [endPt.x, endPt.y]
			while ((sensorData = reader.readLine()) != null) {
				
				// create new sensor interval
				SensorInterval newInterval = new SensorInterval(sensorData);
								
				// add the interval to data
				addPositiveInterval(newInterval);
				
				// update count of sensor
				sensorId = newInterval.getSensorID();
				if (sensorId > maxSensor){
					maxSensor = sensorId;
				}
				
				// set gradient if we see it for the first time. 
				double angle = newInterval.getAngle();
				if (!angleSet){					
					sensorAngle = angle;
					angleSet = true;
				}
				else{
					// enforce parallel positive intervals
					assert (sensorAngle == angle): 
						"In file " + sensorFileName +", positive intervals not parallel!";				
				}
				
				// if it's a new sensor with a positive component, work out the gap between sensor

				if (prevSensorID != sensorId && prevSensorID != -1){

					// work out the current gap 
					double currentGap = newInterval.getDistanceToLine(prevPoint) / (sensorId - prevSensorID);
					// round to 3 decimal places. 
					currentGap = (double)Math.round(currentGap * 1000) / 1000;
					
					// if the sensorGap has not been set, then set it
					if (!sensorGapSet){
						sensorGap = currentGap;
						sensorGapSet = true;
					}
					// otherwise, assert that it's the same.
					else{
						assert(sensorGap==currentGap): "In file " + sensorFileName +", gaps between sensors are not uniform!";
					}									
				}
				// update prevSensorID and prevPoint
				else{
					prevSensorID = sensorId;
					prevPoint = newInterval.getStart();					
				}
				
				
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
	public void addPositiveInterval(SensorInterval positiveInterval){
		positiveIntervals.add(positiveInterval);
	}
	
	// public method for reading variables in the class	
	
	// read list of intervals
	public List<SensorInterval> getPositiveIntervals(){
		return positiveIntervals;
	}
	
	// work out list of negative intervals
	public List<SensorInterval>getNegativeIntervals(){
		
		
		List<SensorInterval> negIntervals = new ArrayList<SensorInterval>();
		
		SensorInterval prevInterval = null;
		
		int prevIntervalID = 0;
		
		for (int i=0; i<positiveIntervals.size(); i++){
			SensorInterval curInterval = positiveIntervals.get(i);
			
			int curIntervalID = curInterval.getSensorID();
			
			// If it's a new interval
			if (curIntervalID != prevIntervalID){
				// if it's the first positive interval we've seen
				if (prevIntervalID==0){
					// the first sensor is completely negative					
				}				
								
				// write the negative sensors
				prevIntervalID++;
				while (prevIntervalID < curIntervalID){
					// create full negative interval on this sensorID. 
					Point2D curIntervalStart = curInterval.getStart();
					
					//
					
					prevIntervalID++;
				}
				
			}
			
			if (prevInterval != null){
				SensorInterval curNeg = prevInterval.getNegativeInterval(curInterval);
				if (curNeg != null){
					negIntervals.add(curNeg);
				}
				else{
					// add final neg interval after prevInteval
					prevInterval.getNegativePostInterval();
					// add first neg interval before curInterval
					curInterval.getNegativePreInterval();
				}
			}
			else{
				// add first neg interval before curInterval
				curInterval.getNegativePreInterval();
			}
			
			prevIntervalID = curIntervalID;
			prevInterval = curInterval;
		}		
		return negIntervals;
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
	
	// add a set of intervals to graphics
	public void addIntervalsToGraphic(Graphics2D g2d, List<SensorInterval> intervals, boolean useOffset){
		double xOffset=0, yOffset=0;
		if (useOffset){
			// calculate hull offset for drawing
			// otherwise it draws outside the canvas.
			double minX=Double.MAX_VALUE, minY=Double.MIN_VALUE;
			for (int i=0; i<intervals.size(); i++){
				SensorInterval curInterval = positiveIntervals.get(i);
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
		}
		
		// Draw components					
		for (int i=0; i<intervals.size(); i++){
			SensorInterval curInterval = intervals.get(i);
			Path2D path = new Path2D.Double();
			path.moveTo(curInterval.getStart().getX()+xOffset, curInterval.getStart().getY()+yOffset);
			path.lineTo(curInterval.getEnd().getX()+xOffset, curInterval.getEnd().getY()+yOffset);
			path.closePath();
			g2d.draw(path);
		}
		
	}
	
	// draw the positive intervals to a specified filename
	public void drawPositiveIntervals(String filename){
		
		// Initialize image
		BufferedImage img = new BufferedImage(1024, 800,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();
		g2d.setColor(Color.BLACK);
		
		boolean useOffsets = true;
		addIntervalsToGraphic(g2d,positiveIntervals,useOffsets);
		
		// Write to file
		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}
	}
	
	// draw the positive intervals to a specified filename
	public void drawIntervals(List<SensorInterval> intervals, String filename){
		
		// Initialize image
		BufferedImage img = new BufferedImage(1024, 800,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();
		g2d.setColor(Color.BLACK);
		
		boolean useOffsets = true;
		addIntervalsToGraphic(g2d,intervals,useOffsets);
		
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
		
		List<SensorInterval> thisNegativeIntervals = getNegativeIntervals();
		List<SensorInterval> otherPositiveIntervals = otherData.getPositiveIntervals();
		List<SensorInterval> otherNegativeIntervals = otherData.getNegativeIntervals();
		
		for (int i=0; i<positiveIntervals.size(); i++){
			SensorInterval curPositive = positiveIntervals.get(i);
			
			// check for intersection of other positive intervals
			for (int j=0; j<otherPositiveIntervals.size() && !positiveIntersect; j++){
				SensorInterval otherPositive = otherPositiveIntervals.get(j);
				if (curPositive.intersects(otherPositive)){
					positiveIntersect = true;					
				}
			}
			
			// check for intersection of other negative intervals
			for (int j=0; j<otherNegativeIntervals.size(); j++){
				SensorInterval otherNegative = otherNegativeIntervals.get(j);
				if (curPositive.intersects(otherNegative)){
					return false;
				}
			}
		}
		
		// check for intersection of negative intervals with other positive
		for (int i=0; i<thisNegativeIntervals.size(); i++){
			SensorInterval curNegative = thisNegativeIntervals.get(i);
			
			// check for intersection of other positive intervals
			for (int j=0; j<otherPositiveIntervals.size();j++){
				if (curNegative.intersects(otherPositiveIntervals.get(j))){
					return false;
				}
			}
		}
		
		// check outer intervals
		
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

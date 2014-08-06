package sn.recover;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.Timestamp;

import javax.imageio.ImageIO;

import sn.debug.ShowDebugImage;
import sn.regiondetect.ComplexRegion;
import sn.regiondetect.GeomUtil;
import sn.regiondetect.Region;

// The class for one set of sensor data
public class SensorData implements java.io.Serializable{

	// serial version uid
	private static final long serialVersionUID = 1L;
	
	// variables in the class
	// list of positive intervals detected in data
	private List<SensorInterval> positiveIntervals;
	private List<SensorInterval> negativeIntervals;

	// Angle of the parallel positive intervals in radians
	private double sensorAngle;
	// Distance between adjacent sensor lines
	private double sensorGap;
	// the count of number of sensors
	private int sensorCount;

	private int width; // canvas width
	private int height; // canvas height

	/**
	 * Constructor
	 * 
	 * @param canvasWidth
	 * @param canvasHeight
	 */
	public SensorData(int canvasWidth, int canvasHeight) {
		// initialize variables
		positiveIntervals = new ArrayList<SensorInterval>();
		negativeIntervals = new ArrayList<SensorInterval>();
		sensorAngle = Double.NaN; // initiated at NaN
		sensorGap = Double.NaN; // initiated at NaN
		sensorCount = Integer.MIN_VALUE; // initiated at min value
		width = canvasWidth;
		height = canvasHeight;
	}
	
	/***
	 * Complete constructor
	 * @param pis: positive intervals
	 * @param nis: negative intervals
	 * @param sa: sensor angle
	 * @param sg: sensor gap
	 * @param sc: sensor count
	 * @param wd: width
	 * @param ht: height
	 */
	public SensorData(List<SensorInterval> pis, List<SensorInterval> nis, 
					  double sa, double sg, int sc, int wd, int ht){
		// initialize variables
				positiveIntervals = pis;
				negativeIntervals = nis;
				sensorAngle = sa;
				sensorGap = sg;
				sensorCount = sc;
				width = wd;
				height = ht;
	}
	
	/***
	 * Copy constructor
	 * @param old: the old sensorData to copy
	 */
	public SensorData(SensorData old){
		this(old.positiveIntervals,old.negativeIntervals,old.sensorAngle,old.sensorGap,
				old.sensorCount,old.width,old.height);
	}

	/**
	 * Construct from a complex region and other info
	 * 
	 * @param complexRegion : the given complex region delivered from the generator
	 * @param gap : The length between parallel intervals 
	 * @param angle : the angle for which the lines are sampling the region
	 * @param canvasWidth : width of the canvas
	 * @param canvasHeight : height of the canvas
	 * @throws Exception
	 */
	public SensorData(ComplexRegion complexRegion, double gap, double angle,
			int canvasWidth, int canvasHeight) throws Exception {

		positiveIntervals = new ArrayList<SensorInterval>();
		negativeIntervals = new ArrayList<SensorInterval>();
		sensorAngle = angle;
		sensorGap = gap;
		sensorCount = Integer.MIN_VALUE; // initiated at min value
		width = canvasWidth;
		height = canvasHeight;

		// generate a set of parallel lines to fill the canvas
		List<Line2D> parallelLines = GeomUtil.generateParallelLines(sensorGap,
				sensorAngle, canvasWidth, canvasHeight);
		sensorCount = parallelLines.size();

		// generate a complex region
		Region[] regions = complexRegion.getComplexRegion();

		// sensor ID initialized to 1
		int sensorId = 1;

		for (Line2D l : parallelLines) {
			List<Line2D> intersectLines = new ArrayList<Line2D>();

			// iterate over all sub-regions in the complex region
			for (Region p : regions) {
				if (!p.isHole()) {// if sub-region is not a hole, there is a
									// positive interval
					intersectLines = GeomUtil.lineRegion(intersectLines, p, l,
							sensorAngle, canvasHeight, canvasWidth);
				} else {// otherwise negative interval
					intersectLines = GeomUtil.lineJumpHole(intersectLines, p,
							l, sensorAngle, canvasHeight, canvasWidth);
				}
			}
			for (Line2D il : intersectLines) {
				SensorInterval positiveInterval = new SensorInterval(sensorId,
						il);
				positiveIntervals.add(positiveInterval);
			}
			sensorId++;
		}

		negativeIntervals = getNegativeIntervalsFromPositive();

	}

	/**
	 * Constructor from a given file
	 * 
	 * @param sensorFileName
	 * @param canvasWidth
	 * @param canvasHeight
	 */
	public SensorData(String sensorFileName, int canvasWidth, int canvasHeight) {

		// initialize variables
		positiveIntervals = new ArrayList<SensorInterval>();
		negativeIntervals = new ArrayList<SensorInterval>();
		sensorAngle = Double.NaN; // initiated at NaN
		sensorGap = Double.NaN; // initiated at NaN
		sensorCount = Integer.MIN_VALUE; // initiated at min value
		width = canvasWidth;
		height = canvasHeight;

		File file = new File(sensorFileName);

		if (!file.exists()) {
			System.err.println("failed to read file: " + sensorFileName);
			System.exit(-1);
		}

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String sensorData = null;
			int sensorId = -1;

			boolean sensorGapSet = false;
			boolean angleSet = false;

			// variables to work out distance SensorData parameters
			int prevSensorID = -1;
			Point2D prevPoint = new Point2D.Double(Double.MAX_VALUE,
					Double.MAX_VALUE);
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
				if (sensorId > maxSensor) {
					maxSensor = sensorId;
				}

				// set gradient if we see it for the first time.
				double angle = newInterval.getAngle();
				if (!angleSet) {
					sensorAngle = angle;
					angleSet = true;
				} else {
					// enforce parallel positive intervals
					assert (sensorAngle == angle) : "In file " + sensorFileName
							+ ", positive intervals not parallel!";
				}

				// if it's a new sensor with a positive component, work out the
				// gap between sensor

				if (prevSensorID != sensorId && prevSensorID != -1) {

					// work out the current gap
					double currentGap = newInterval
							.getDistanceToLine(prevPoint)
							/ (sensorId - prevSensorID);
					// round to 3 decimal places.
					currentGap = (double) Math.round(currentGap * 1000) / 1000;

					// if the sensorGap has not been set, then set it
					if (!sensorGapSet) {
						sensorGap = currentGap;
						sensorGapSet = true;
					}
					// otherwise, assert that it's the same.
					else {
						assert (sensorGap == currentGap) : "In file "
								+ sensorFileName
								+ ", gaps between sensors are not uniform!";
					}
				}
				// update prevSensorID and prevPoint
				else {
					prevSensorID = sensorId;
					prevPoint = newInterval.getStart();
				}

			}

			// update sensor count
			sensorCount = maxSensor;

			// Derive negative intervals
			negativeIntervals = getNegativeIntervalsFromPositive();

			// Print sensor data information
			System.out.println("File " + sensorFileName + " read.");
			System.out.println("Sensor count" + sensorCount);
			System.out.println("Angle " + sensorAngle);
			System.out.println("Sensor gap " + sensorGap);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * public methods for modifying variables in the class
	 * 
	 * @param positiveInterval
	 */
	public void addPositiveInterval(SensorInterval positiveInterval) {
		positiveIntervals.add(positiveInterval);
	}
	
	public void addNegativeInterval(SensorInterval negativeInterval){
		negativeIntervals.add(negativeInterval);
	}

	// public method for reading variables in the class

	/**
	 * read list of intervals
	 * 
	 * @return
	 */
	public List<SensorInterval> getPositiveIntervals() {
		return positiveIntervals;
	}

	public List<SensorInterval> getNegativeIntervals() {
		return negativeIntervals;
	}

	/**
	 * read angle of intervals in atan2
	 * 
	 * @return
	 */
	public double getAngle() {
		return sensorAngle;
	}
	
	public int getSensorCount(){
		return sensorCount;
	}

	/**
	 * read list of coordinates that make up the positive intervals
	 * 
	 * @return List<Point2D> pts
	 */
	public List<Point2D> getPositiveCoordinates() {
		List<Point2D> positiveCoordinates = new ArrayList<Point2D>();

		for (int i = 0; i < positiveIntervals.size(); i++) {
			positiveCoordinates.add(positiveIntervals.get(i).getStart());
			positiveCoordinates.add(positiveIntervals.get(i).getEnd());
		}

		return positiveCoordinates;
	}

	/**
	 * work out list of negative intervals from positiveIntervals
	 * 
	 * @return negative intervals
	 */
	public List<SensorInterval> getNegativeIntervalsFromPositive() {

		List<SensorInterval> negIntervals = new ArrayList<SensorInterval>();
		List<SensorInterval> intervalsInSameSensor = new ArrayList<SensorInterval>();

		// gap between two adjacent sensor against x axis
		double gapInX = Math.abs(sensorGap / Math.sin(sensorAngle));

		// Sensor id starts from 1
		int prevIntervalID = 1;

		for (int i = 0; i < positiveIntervals.size(); i++) {

			SensorInterval curInterval = positiveIntervals.get(i);
			int curIntervalID = curInterval.getSensorID();

			// If it's a new interval
			if (curIntervalID != prevIntervalID) {

				if (prevIntervalID != 1) {// if current interval is not from
											// first interval
					// add negative intervals from previous sensor
					addNegativeIntervals(intervalsInSameSensor, negIntervals);
					prevIntervalID++;
				}

				// write the full negative sensors
				while (prevIntervalID < curIntervalID) {
					addFullnegativeInterval(curInterval, curIntervalID,
							prevIntervalID, gapInX, negIntervals);
					prevIntervalID++;
				}

				// clear for the new interval
				intervalsInSameSensor.clear();
				intervalsInSameSensor.add(curInterval);
			}

			else {
				intervalsInSameSensor.add(curInterval);
			}

			if (i == positiveIntervals.size() - 1) {
				addNegativeIntervals(intervalsInSameSensor, negIntervals);
			}
			prevIntervalID = curIntervalID;
		}

		// add negatives after the last positive interval
		if (prevIntervalID < sensorCount) {
			SensorInterval lastPositiveInterval = positiveIntervals
					.get(positiveIntervals.size() - 1);
			int lastPositiveID = lastPositiveInterval.getSensorID();

			while (prevIntervalID < sensorCount) {
				prevIntervalID++;
				// add full negative sensors
				addFullnegativeInterval(lastPositiveInterval, lastPositiveID,
						prevIntervalID, gapInX, negIntervals);
			}
		}

		return negIntervals;
	}

	/**
	 * get negative intervals from positive ones of a sensor
	 * 
	 * @param intervalsInSameSensor
	 *            a set of intervals that belong to the same sensor
	 * @param negIntervals
	 */
	public void addNegativeIntervals(
			List<SensorInterval> intervalsInSameSensor,
			List<SensorInterval> negIntervals) {
		List<Point2D> ptsOnFullSensor = new ArrayList<Point2D>();
		Line2D fullInterval;
		int sensorID;

		// rotate the points to ensure they can be sorted vertically
		AffineTransform rotate = new AffineTransform();
		rotate.rotate(Math.PI / 2 - sensorAngle, width / 2, height / 2);

		// rotate inverse
		AffineTransform rotateInverse = new AffineTransform();
		rotateInverse.rotate(sensorAngle - Math.PI / 2, width / 2, height / 2);

		// derive the full sensor from a positive interval
		fullInterval = intervalsInSameSensor.get(0).getFullInterval(width,
				height);

		sensorID = intervalsInSameSensor.get(0).getSensorID();

		// add the start and end points of the full interval
		ptsOnFullSensor.add(fullInterval.getP1());
		ptsOnFullSensor.add(fullInterval.getP2());

		// add all other points from positive intervals
		for (SensorInterval si : intervalsInSameSensor) {

			ptsOnFullSensor.add(si.getStart());
			ptsOnFullSensor.add(si.getEnd());
		}

		for (int j = 0; j < ptsOnFullSensor.size(); j++) {
			Point2D pt = ptsOnFullSensor.get(j);
			pt = rotate.transform(pt, null);
			ptsOnFullSensor.set(j, pt);
		}

		// sort the points vertically
		for (int m = 0; m < ptsOnFullSensor.size() - 1; m++) {
			for (int n = ptsOnFullSensor.size() - 1; n > 0; n--) {
				if (ptsOnFullSensor.get(n).getY() < ptsOnFullSensor.get(n - 1)
						.getY()) {
					Point2D temPt = ptsOnFullSensor.get(n);
					ptsOnFullSensor.set(n, ptsOnFullSensor.get(n - 1));
					ptsOnFullSensor.set(n - 1, temPt);
				}
			}
		}

		// add negative intervals
		boolean hasP1 = false, hasP2 = false;
		Point2D s = null, e = null;
		for (Point2D pt : ptsOnFullSensor) {
			pt = rotateInverse.transform(pt, null);
			if (!hasP1) {
				s = pt;
				hasP1 = true;
			} else if (!hasP2) {
				e = pt;
				hasP2 = true;
			}

			// add a negative interval
			if (hasP1 && hasP2) {
				negIntervals.add(new SensorInterval(sensorID, s, e));
				hasP1 = false;
				hasP2 = false;
			}
		}
	}

	/**
	 * add a full negative interval
	 * 
	 * @param curInterval
	 * @param curIntervalID
	 * @param prevIntervalID
	 * @param gapInX
	 *            gap between two adjacent intervals corresponding to x axis
	 * @param negIntervals
	 */
	public void addFullnegativeInterval(SensorInterval curInterval,
			int curIntervalID, int prevIntervalID, double gapInX,
			List<SensorInterval> negIntervals) {
		// calculate shifted points of a full negative interval from
		// current interval
		double shiftedStartX = curInterval.getInterval().getX1()
				- (curIntervalID - prevIntervalID) * gapInX;
		double shiftedEndX = curInterval.getInterval().getX2()
				- (curIntervalID - prevIntervalID) * gapInX;
		double shiftedStartY = curInterval.getInterval().getY1();
		double shiftedEndY = curInterval.getInterval().getY2();

		Point2D shiftedStart = new Point2D.Double(shiftedStartX, shiftedStartY);
		Point2D shiftedEnd = new Point2D.Double(shiftedEndX, shiftedEndY);

		// derive a full negative interval
		SensorInterval shiftedNegativeInterval = new SensorInterval(
				prevIntervalID, shiftedStart, shiftedEnd);
		Line2D fullNegativeLine = shiftedNegativeInterval.getFullInterval(
				width, height);
		SensorInterval fullNegativeInterval = new SensorInterval(
				prevIntervalID, fullNegativeLine);

		// create full negative interval on this sensorID.
		negIntervals.add(fullNegativeInterval);
	}

	/**
	 * read the convex hull of the positive intervals
	 * 
	 * @return
	 */
	public List<Point2D> getConvexHull() {
		List<Point2D> coordList = getPositiveCoordinates();
		Point2D[] coords = new Point2D[coordList.size()];

		coordList.toArray(coords);

		GrahamScan scan = new GrahamScan(coords);
		return (List<Point2D>) scan.hull();
	}
	
	// return convex hull with N points equidistant to its neighbour
	// not finished
	public List<Point2D> getConvexHullManyPoints(){
		List<Point2D> cHull = getConvexHull();
		return null;
	}
	
	/***
	 * Method: draw conflict points to graphics
	 * @param g2d
	 * @param otherData
	 */
	public void addConflictPointsToGraphic(Graphics2D g2d, SensorData otherData){
		List<Point2D> conflicts = getConflictPoints(otherData);
		conflicts.addAll(otherData.getConflictPoints(this));
		
		System.out.println("Drawing conflict points");
		
		// set color as red
		g2d.setColor(Color.RED);
		
		for (Point2D conflict:conflicts){
			// draw a cross
			
			Path2D path1 = new Path2D.Double();
			path1.moveTo(conflict.getX()-5, conflict.getY()-5);
			path1.lineTo(conflict.getX()+5, conflict.getY()+5);
			
			Path2D path2 = new Path2D.Double();
			path2.moveTo(conflict.getX()-5, conflict.getY()+5);
			path2.lineTo(conflict.getX()+5, conflict.getY()-5);
			
			g2d.draw(path1);
			g2d.draw(path2);
		}
		
		System.out.println("Conflict points drawn");
	}
	
	/***
	 * 
	 * @param g2d 
	 * @param intervals
	 * @param useOffset
	 * @param c
	 */

	public void addIntervalsToGraphic(Graphics2D g2d,
			List<SensorInterval> intervals, boolean useOffset, Color c) {
		
		
		double xOffset = 0, yOffset = 0;
		
		
		if (useOffset) {
			// calculate hull offset for drawing
			// otherwise it draws outside the canvas.
			double minX = Double.MAX_VALUE, minY = Double.MIN_VALUE;
			for (int i = 0; i < intervals.size(); i++) {
				SensorInterval curInterval = positiveIntervals.get(i);
				if (curInterval.getStart().getX() < minX) {
					minX = curInterval.getStart().getX();
				}
				if (curInterval.getStart().getY() < minY) {
					minY = curInterval.getStart().getY();
				}
			}

			if (minX < 20) {
				xOffset = 20 - minX;
			}
			if (minY < 20) {
				yOffset = 20 - minY;
			}
		}

		g2d.setColor(c);
		
		// Draw components
		for (int i = 0; i < intervals.size(); i++) {
			SensorInterval curInterval = intervals.get(i);
			Path2D path = new Path2D.Double();
			path.moveTo(curInterval.getStart().getX() + xOffset, curInterval
					.getStart().getY() + yOffset);
			path.lineTo(curInterval.getEnd().getX() + xOffset, curInterval
					.getEnd().getY() + yOffset);
			path.closePath();
			g2d.draw(path);
		}

	}

	/**
	 * add a set of intervals to graphics
	 * 
	 * @param g2d
	 * @param intervals
	 * @param useOffset
	 *            if use offset
	 */
	public void addIntervalsToGraphic(Graphics2D g2d,
			List<SensorInterval> intervals, boolean useOffset) {
		addIntervalsToGraphic(g2d, intervals, useOffset, Color.BLACK);
	}

	/**
	 * draw the positive intervals to a specified filename
	 * 
	 * @param filename
	 */
	public void drawPositiveIntervals(String filename) {

		// Initialize image
		BufferedImage img = new BufferedImage(1024, 800,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();
		g2d.setColor(Color.BLACK);

		boolean useOffsets = true;
		addIntervalsToGraphic(g2d, positiveIntervals, useOffsets);

		// Write to file
		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}
	}

	/**
	 * draw the intervals to a specified filename
	 * 
	 * @param intervals
	 * @param filename
	 */
	public void saveIntervalsImgToFile(List<SensorInterval> intervals,
			String filename) {

		// Initialize image
		BufferedImage img = new BufferedImage(1024, 800,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();
		g2d.setColor(Color.BLACK);

		boolean useOffsets = true;
		addIntervalsToGraphic(g2d, intervals, useOffsets);

		// Write to file
		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}
	}

	/**
	 * draw the convex hull of the positive intervals to a specified filename
	 * 
	 * @param filename
	 */
	public void drawConvexHull(String filename) {

		// get the convex hull
		List<Point2D> hull = getConvexHull();

		double xOffset = 0, yOffset = 0;

		// calculate hull offset for drawing
		// otherwise it draws outside the canvas.
		double minX = Double.MAX_VALUE, minY = Double.MIN_VALUE;
		for (int i = 0; i < hull.size(); i++) {
			if (hull.get(i).getX() < minX) {
				minX = hull.get(i).getX();
			}
			if (hull.get(i).getY() < minY) {
				minY = hull.get(i).getY();
			}
		}

		if (minX < 20) {
			xOffset = 20 - minX;
		}
		if (minY < 20) {
			yOffset = 20 - minY;
		}

		Path2D hullPath = new Path2D.Double();
		hullPath.moveTo(hull.get(0).getX() + xOffset, hull.get(0).getY()
				+ yOffset);
		for (int i = 1; i < hull.size(); i++) {
			hullPath.lineTo(hull.get(i).getX() + xOffset, hull.get(i).getY()
					+ yOffset);
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
	
	public void drawTwoConvexHulls(SensorData otherData, String str){
		
		// get the convex hull
		List<Point2D> thisHull = getConvexHull();
		List<Point2D> otherHull = otherData.getConvexHull();
		
		List<Point2D> fullList = new ArrayList<Point2D>(thisHull);
		fullList.addAll(otherHull);
		
		// variables denoting the parameters of affine transformation
		double minX=Double.MAX_VALUE, minY=Double.MAX_VALUE,maxX=Double.MIN_VALUE, maxY=Double.MIN_VALUE;
		
		for (int i=0; i<fullList.size(); i++){
			Point2D pt1 = fullList.get(i);
			minX =  Math.min(pt1.getX(),minX);
			minY =  Math.min(pt1.getY(),minY);
			maxX =  Math.max(pt1.getX(),maxX);
			maxY =  Math.max(pt1.getY(),maxY);
		}
		
		double dx=0, dy=0;
		
		if (maxX < 0) dx = -maxX+50;
		if (maxY < 0) dy = -maxY+50; 
		if (minX < 0) dx += -minX+50;
		if (minY < 0) dy += -minY+50;
		
		int newWidth = (int)(maxX + dx), newHeight = (int)(maxY+dy);
		
		Path2D thisHullPath = new Path2D.Double();
		thisHullPath.moveTo(thisHull.get(0).getX() + dx, thisHull.get(0).getY()
				+ dy);
		for (int i = 1; i < thisHull.size(); i++) {
			thisHullPath.lineTo(thisHull.get(i).getX() + dx, thisHull.get(i).getY()
					+ dy);
		}
		thisHullPath.lineTo(thisHull.get(0).getX() + dx, thisHull.get(0).getY()
				+ dy);
		
		Path2D otherHullPath = new Path2D.Double();
		otherHullPath.moveTo(otherHull.get(0).getX() + dx, otherHull.get(0).getY()
				+ dy);
		for (int i = 1; i < otherHull.size(); i++) {
			otherHullPath.lineTo(otherHull.get(i).getX() + dx, otherHull.get(i).getY()
					+ dy);
		}
		otherHullPath.lineTo(otherHull.get(0).getX() + dx, otherHull.get(0).getY()
				+ dy);
		
		
		Line2D thisLongest = getLongestDimension(thisHull);
		thisLongest.setLine(new Point2D.Double(thisLongest.getX1()+dx, thisLongest.getY1()+dy), 
							new Point2D.Double(thisLongest.getX2()+dx, thisLongest.getY2()+dy));
		Line2D otherLongest = getLongestDimension(otherHull);
		otherLongest.setLine(new Point2D.Double(otherLongest.getX1()+dx, otherLongest.getY1()+dy), 
				new Point2D.Double(otherLongest.getX2()+dx, otherLongest.getY2()+dy));
		// draw the image
		BufferedImage img = new BufferedImage(newWidth, newHeight,
						BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();
		g2d.setColor(Color.RED);
		g2d.draw(thisHullPath);
		g2d.draw(thisLongest);
		g2d.setColor(Color.BLUE);
		g2d.draw(otherHullPath);
		g2d.draw(otherLongest);
		
		String filename = "experiments/" + getCurrentTimeStamp() + "-"+ str + ".png";
		
		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}
		
		ShowDebugImage frame = new ShowDebugImage(str, img);
		frame.refresh(img);
	}
	
	public static String getCurrentTimeStamp() {
	    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd-HHmm");//dd/MM/yyyy
	    Date now = new Date();
	    String strDate = sdfDate.format(now);
	    return strDate;
	}
	
	/**
	 * 
	 * Get possible shape/matching 
	 * @return an array of affine transform showing possible matches.
	 * 
	 */
	
	
	
	public List<AffineTransform> getMatchingTransformList(SensorData otherData){
		
		List <AffineTransform> transformList = new ArrayList<AffineTransform>();		
		
		return transformList;
	}
	
	// Find the affine transform that translates this SensorData to match the other.
	public AffineTransform matchCentroid(SensorData otherData){
		AffineTransform at = new AffineTransform();
		
		Point2D thisCentroid = getConvexHullCentroid();
		Point2D otherCentroid = otherData.getConvexHullCentroid();
		
		double dx = otherCentroid.getX()-thisCentroid.getX();
		double dy = otherCentroid.getY()-thisCentroid.getY();
		
		at.translate(dx, dy);
		
		return at;
	}
	
	public TransformationRobustHelmert getHelmertInitial(SensorData otherData){
		AffineTransform at = new AffineTransform();
		
		List<Point2D> thisHull = getConvexHull();
		List<Point2D> otherHull = otherData.getConvexHull();
		
		int n = Math.min(thisHull.size(), otherHull.size());
		
		double[][] destSet = new double[n][2];
		double[][] srcSet = new double[n][2];
		
		for (int i=0; i<n; i++){
			srcSet[i][0] = thisHull.get(i).getX();
			srcSet[i][1] = thisHull.get(i).getY();
			destSet[i][0] = otherHull.get(i).getX();
			destSet[i][1] = otherHull.get(i).getY();
		}
		
		TransformationRobustHelmert ht = new TransformationRobustHelmert();
		ht.initWithPoints(destSet, srcSet);
		
		
		return ht;
	}
	
	// Greedy Search for matching other data
	public AffineTransform greedySearch(SensorData otherData, AffineTransform at, int remainingSteps){
		
		// base case: no more search
		if (remainingSteps ==0) return at;
		
		int dx, dy;
		
		for (int i=0; i<4; i++){
			dx = ((i & 1) == 1) ? 1 : -1;
			dy = ((i & 1) == 1) ? 1 : -1;
		}
		
		return at;
	}
	
	/* Method getCentroid
	 * return the centroid for a given list of points
	 * with centroid defined as the average of a finite set of 2D points
	 * @param Point2D[] pointList 
	 * @returns Point2D the new centroid
	 */	 
	public Point2D getCentroid(Point2D[] pointList){
		double centroidX=0, centroidY=0;
		
		for (Point2D point : pointList){			
			centroidX +=  point.getX();
			centroidY +=  point.getY();
		}
		
		double newX = centroidX/pointList.length, newY = centroidY/pointList.length;
		
		return new Point2D.Double(newX, newY);
	}
	
	// find matching affineTransform from ICP
	public AffineTransform getATfromICP(SensorData otherData){
		
		List<Point2D> thisHull = getConvexHull();
		List<Point2D> otherHull = otherData.getConvexHull();
		
		int minSize = Math.min(thisHull.size(), otherHull.size());
		
		List<Point2D> thisHullMinSize = new ArrayList<Point2D>();
		List<Point2D> otherHullMinSize = new ArrayList<Point2D>();
		
		for (int i=0; i<minSize; i++){
			thisHullMinSize.add(thisHull.get(i));
			otherHullMinSize.add(otherHull.get(i));
		}
		
		IterativeClosestPoint icp = new IterativeClosestPoint(thisHullMinSize);
		AffineTransform at = icp.getAffineTransform(otherHullMinSize);
		
		return at;
	}
	
	
	
	public AffineTransform[] getATfromLongest(SensorData otherData){
		
		// Two Affine transforms, one for rotate, other for translate.
		AffineTransform[] transforms = new AffineTransform[2];
		
		// First, we work out the two longest dimensions
		Line2D thisLongest = getLongestDimension(getConvexHull());
		Line2D otherLongest = getLongestDimension(otherData.getConvexHull());
		
		double thisAngle = Math.atan2((thisLongest.getY2()-thisLongest.getY1()), (thisLongest.getX2() - thisLongest.getX1()));
		double otherAngle = Math.atan2((otherLongest.getY2()-otherLongest.getY1()), (otherLongest.getX2() - otherLongest.getX1()));
		
		// we translate this SensorData where the one of the points on the longest dimension is at the origin.
		
		AffineTransform at = new AffineTransform();
		

		
		
		
		// rotate about the origin so the longest dimension has the same direction
		AffineTransform firstRotate = new AffineTransform();
		firstRotate.rotate(otherAngle - thisAngle, thisLongest.getX1(), thisLongest.getY1());
		
		AffineTransform secondRotate = new AffineTransform();
		secondRotate.rotate(otherAngle - thisAngle + Math.PI, thisLongest.getX1(), thisLongest.getY1());
		
		at.rotate(otherAngle - thisAngle, thisLongest.getX1(), thisLongest.getY1());
		
		SensorData firstRotateData = applyAffineTransform(firstRotate);
		
		SensorData secondRotateData = applyAffineTransform(secondRotate);
		
		Line2D firstRotateLongest = getLongestDimension(firstRotateData.getConvexHull());
		Line2D secondRotateLongest = getLongestDimension(secondRotateData.getConvexHull());
		
		// System.out.println("rotate longest:" + rotateLongest.getP1().toString() + " " + rotateLongest.getP2().toString());

				
		// firstRotateData.drawTwoConvexHulls(otherData, "AfterRotate");
		
		// translate the data so the point of the longest element matches.
		double first_dx = otherLongest.getX1()-firstRotateLongest.getX1();
		double first_dy = otherLongest.getY1()-firstRotateLongest.getY1();
		
		double second_dx = otherLongest.getX1()-secondRotateLongest.getX1();
		double second_dy = otherLongest.getY1()-secondRotateLongest.getY1();
		
		// System.out.println("translating dx:" + first_dx + " dy:" + first_dy );
		
		
		
		//at.translate(dx,dy);
		
		AffineTransform firstTranslate = new AffineTransform();
		firstTranslate.translate(first_dx,first_dy);
		at.translate(first_dx, first_dy);
		
		AffineTransform secondTranslate = new AffineTransform();
		secondTranslate.translate(second_dx, second_dy);
				
		SensorData firstTransformedData = firstRotateData.applyAffineTransform(firstTranslate); //applyAffineTransform(at);
		SensorData secondTransformedData = secondRotateData.applyAffineTransform(secondTranslate);
		
		// Drawing things
		//firstTransformedData.drawTwoConvexHulls(otherData, "FirstRotateTranslate");

		//secondTransformedData.drawTwoConvexHulls(otherData, "SecondRotateTranslate");
		
		//firstTransformedData.drawMeasurements(otherData, "firstRT");
		//secondTransformedData.drawMeasurements(otherData, "secondRT");
		
		int secondError = secondTransformedData.countConflicts(otherData);
		int firstError = firstTransformedData.countConflicts(otherData);
		
		double secondScore = secondTransformedData.sumClosestHullError(otherData);
		double firstScore = firstTransformedData.sumClosestHullError(otherData);
		
		//System.out.println("First error:" + firstError + ", Second error:" + secondError);
		//System.out.println("First score:" + firstScore + ", Second score:" + secondScore);
		
		if (firstScore < secondScore){
			transforms[0] = firstRotate; 
			transforms[1] = firstTranslate;
			
			System.out.println("First selected");
		}
		else{
			transforms[0] = secondRotate;
			transforms[1] = secondTranslate;
			System.out.println("Second selected");
		}
		
		Line2D transformedLongest = getLongestDimension(firstTransformedData.getConvexHull());
		double newAngle =  Math.atan2((transformedLongest.getY2()-transformedLongest.getY1()), 
				  (transformedLongest.getX2()-transformedLongest.getX1()));

		System.out.println("transformed angle:" + newAngle + "\n other angle:" + otherAngle);

		
		System.out.println("this longest:" + thisLongest.getP1().toString() + " " + thisLongest.getP2().toString());
		System.out.println("other longest:" + otherLongest.getP1().toString() + " " + otherLongest.getP2().toString());
		System.out.println("transformed longest:" + transformedLongest.getP1().toString() + " " + transformedLongest.getP2().toString());
		
		return transforms;
	}
	
	// Find the affine transform from this SensorData to match other sensorData.  	
	public AffineTransform getMatchingTransformThroughCentroid(SensorData otherData){
				
		
		
		Point2D thisCentroid = getPosIntCentroid();//getConvexHullCentroid();
		Point2D otherCentroid = otherData.getPosIntCentroid();//otherData.getConvexHullCentroid();
		
		double dx = otherCentroid.getX()-thisCentroid.getX();
		double dy = otherCentroid.getY()-thisCentroid.getY();
		
		
		
		int minError = Integer.MAX_VALUE;
		AffineTransform minAT = new AffineTransform();
		
		// Find the rotation, 1 degree at a time
		int steps = 1000;
		for (int i=0; i<steps; i+=1){
			double angle = 2*Math.PI*i/(steps); 
			
			AffineTransform at = new AffineTransform();
			at.rotate(angle, thisCentroid.getX(), thisCentroid.getY());
			at.translate(dx, dy);

			SensorData transformedData = applyAffineTransform(at);
			
			Point2D newCentroid = transformedData.getConvexHullCentroid();
			
			if (Math.abs((int)newCentroid.getX()-(int)otherCentroid.getX())>5 || Math.abs((int)newCentroid.getY()-(int)otherCentroid.getY())>5){
				System.err.println("ERROR - Centroid don't match. i = " + i + ", dx = " + dx + ", dy = " + dy);
				System.err.println("newCentroid: " + newCentroid.getX() + " " + newCentroid.getY());
				System.err.println("otherCentroid: " + otherCentroid.getX() + " " + otherCentroid.getY());
				System.out.println("Minimum error = " + minError);
				//System.exit(0);
			}
			
			
			int thisTransformError = transformedData.countConflicts(otherData);
			
			if (thisTransformError == 0){
				return at;
			}
			if (thisTransformError < minError){
				minError = thisTransformError;
				minAT = (AffineTransform)at.clone();
				transformedData.drawMeasurements(otherData, "New min, error: " + thisTransformError);				
			}
					
			
		}
		
		System.out.println("Minimum error = " + minError);
		
		return minAT;
	}
	
	
	
	// apply the output of the robust helmert transform to sensor data. 
	public SensorData applyHelmertTransform(TransformationRobustHelmert ht){
		
		SensorData transformedData = new SensorData(width, height);
		
		for (int i = 0; i<positiveIntervals.size(); i++){
			SensorInterval interval = positiveIntervals.get(i);
			Point2D pt1 = interval.getStart();
			Point2D pt2 = interval.getEnd();			
			SensorInterval newInterval = new SensorInterval(interval.getSensorID(), ht.transform(pt1), ht.transform(pt2)  );
			transformedData.addPositiveInterval(newInterval);
		}
		
		for (int i = 0; i<negativeIntervals.size(); i++){
			SensorInterval interval = negativeIntervals.get(i);
			Point2D pt1 = interval.getStart();
			Point2D pt2 = interval.getEnd();
			SensorInterval newInterval = new SensorInterval(interval.getSensorID(), ht.transform(pt1), ht.transform(pt2)  );
			transformedData.addNegativeInterval(newInterval);
		}	
		
		return transformedData;
	}
	
	public SensorData applyAffineTransform(AffineTransform[] at) throws Exception{
		
		if (at.length==0){
			return this;
		}
		
		SensorData transformedData = applyAffineTransform(at[0]);
		
		for (int i=1; i<at.length; i++){			
			transformedData = transformedData.applyAffineTransform(at[i]);
		}
		
		return transformedData;
	}
	
	public SensorData applyAffineTransform(AffineTransform at){
						
		SensorData transformedData = new SensorData(width, height);
		
		for (int i = 0; i<positiveIntervals.size(); i++){
			SensorInterval interval = positiveIntervals.get(i);
			Point2D pt1 = interval.getStart();
			Point2D pt2 = interval.getEnd();
			SensorInterval newInterval = new SensorInterval(interval.getSensorID(), at.transform(pt1, null), at.transform(pt2, null)  );
			transformedData.addPositiveInterval(newInterval);
		}
		
		for (int i = 0; i<negativeIntervals.size(); i++){
			SensorInterval interval = negativeIntervals.get(i);
			Point2D pt1 = interval.getStart();
			Point2D pt2 = interval.getEnd();
			SensorInterval newInterval = new SensorInterval(interval.getSensorID(), at.transform(pt1, null), at.transform(pt2, null)  );
			transformedData.addNegativeInterval(newInterval);
		}	
		
		return transformedData;
	}
	
	/**
	 * 
	 * @return the centroid of the convex hull of this SensorData
	 */
	
	public Point2D getConvexHullCentroid(){
		
		
		List<Point2D> convexHull = getConvexHull();
		
		Point2D centroid = getCentroid(convexHull.toArray(new Point2D[convexHull.size()])); 
				//new Point2D.Double(centroidX/convexHull.size(), centroidY/convexHull.size());
		
		return centroid;
	}
	
	// return the centroid of the positive intervals
	public Point2D getPosIntCentroid(){
		
		List<Point2D> ptList = new ArrayList<Point2D>();
		for (int i=0; i<positiveIntervals.size(); i++){
			ptList.add(positiveIntervals.get(i).getStart());
			ptList.add(positiveIntervals.get(i).getEnd());
		}
		
		return getCentroid(ptList.toArray(new Point2D[ptList.size()]));
	}
	
	public double getAngleRotateLongestDimension(SensorData otherData){
		double angle = 0;
		
		double thisAngle = getAngleLongestDimension();
		double otherAngle = otherData.getAngleLongestDimension();
		
		int steps = 720;
		for (int i=0; i<steps; i++){
			AffineTransform at = new AffineTransform();
			double rotateAngle = i*2*Math.PI/steps;
			at.rotate(rotateAngle);
			SensorData newData = applyAffineTransform(at);
			double newAngle = newData.getAngleLongestDimension();
			
			double error = Math.abs(otherAngle-newAngle);
			System.out.println("angle:" + rotateAngle + " error:" + error);
			if (error < 0.005){
				return newAngle;
			}
		}
		System.err.println("Angle Not Found!");
		System.exit(0);
		return angle;
	}
	
	public Line2D  getLongestDimension(List<Point2D> convexHull){
		// find the logest dimension of the convex hull		

		Point2D maxpt1 = convexHull.get(0), maxpt2=convexHull.get(1);
		double maxDistance = Double.MIN_VALUE;

		for (int i=0; i<convexHull.size(); i++){
			for (int j=1; j<convexHull.size(); j++){
				Point2D pt1 = convexHull.get(i);
				Point2D pt2 = convexHull.get(j);
				double distance = pt1.distance(pt2);
				if (distance > maxDistance){
					maxDistance = distance;
					maxpt1 = pt1;
					maxpt2 = pt2;
				}
			}
		}

		// reorder the points
		if (maxpt1.getX()>maxpt2.getX()){
			Point2D tmpPt = maxpt2;
			maxpt2 = maxpt1;
			maxpt1 = tmpPt;
		}

		return new Line2D.Double(maxpt1, maxpt2);
	}
	
	public double getAngleLongestDimension(){
		double angle = 0;
		
		// find the logest dimension of the convex hull
		List<Point2D> convexHull = getConvexHull();
		
		Point2D maxpt1 = convexHull.get(0), maxpt2=convexHull.get(1);
		double maxDistance = Double.MIN_VALUE;
		
		for (int i=0; i<convexHull.size(); i++){
			for (int j=1; j<convexHull.size(); j++){
				Point2D pt1 = convexHull.get(i);
				Point2D pt2 = convexHull.get(j);
				double distance = pt1.distance(pt2);
				if (distance > maxDistance){
					maxDistance = distance;
					maxpt1 = pt1;
					maxpt2 = pt2;
				}
			}
		}
		
		// reorder the points
		if (maxpt1.getX()>maxpt2.getX()){
			Point2D tmpPt = maxpt2;
			maxpt2 = maxpt1;
			maxpt1 = tmpPt;
		}
		
		angle = Math.atan2((maxpt2.getY()-maxpt1.getY()), (maxpt2.getX() - maxpt1.getX()));
		
		return angle;
	}
	
	
	
public void drawMeasurements(SensorData second, String errorString){
		
		// get current timeStamp to the output string in the graph
	    Date date = new Date();
	    errorString = new Timestamp(date.getTime()) + " " + errorString;
		
		// variables denoting the parameters of affine transformation
		double minX=Double.MAX_VALUE, minY=Double.MAX_VALUE,maxX=Double.MIN_VALUE, maxY=Double.MIN_VALUE;

		// first we get the sense of the dimension of the positive components
		for (int i = 0; i<getPositiveIntervals().size(); i++){
			SensorInterval interval = getPositiveIntervals().get(i);
			Point2D pt1 = interval.getStart();
			Point2D pt2 = interval.getEnd();
			minX =  Math.min(pt1.getX(),minX);
			minX =  Math.min(pt2.getX(),minX);
			minY =  Math.min(pt1.getY(),minY);
			minY =  Math.min(pt2.getY(),minY);
			maxX =  Math.max(pt1.getX(),maxX);
			maxX =  Math.max(pt2.getX(),maxX);
			maxY =  Math.max(pt1.getY(),maxY);
			maxY =  Math.max(pt2.getY(),maxY);
			
			// System.err.println("("+pt1.getX()+","+pt1.getY()+"), ("+pt2.getX()+","+pt2.getY()+")");
		}
		for (int i = 0; i<second.getPositiveIntervals().size(); i++){
			SensorInterval interval = second.getPositiveIntervals().get(i);
			Point2D pt1 = interval.getStart();
			Point2D pt2 = interval.getEnd();
			minX =  Math.min(pt1.getX(),minX);
			minX =  Math.min(pt2.getX(),minX);
			minY =  Math.min(pt1.getY(),minY);
			minY =  Math.min(pt2.getY(),minY);
			maxX =  Math.max(pt1.getX(),maxX);
			maxX =  Math.max(pt2.getX(),maxX);
			maxY =  Math.max(pt1.getY(),maxY);
			maxY =  Math.max(pt2.getY(),maxY);
		}
		
		double dx=0, dy=0;
		
		if (maxX < 0) dx = -maxX+50;
		if (maxY < 0) dy = -maxY+50; 
		if (minX < 0) dx += -minX+50;
		if (minY < 0) dy += -minY+50;
		
		AffineTransform at = new AffineTransform();
		at.translate(dx, dy);
		
		SensorData newFirst = applyAffineTransform(at);
		SensorData newSecond = second.applyAffineTransform(at);
		
		
		
		int newWidth = (int)(maxX + dx), newHeight = (int)(maxY+dy);
		
		System.err.println(newWidth + " " + newHeight);
		System.err.println("pos. intervals 1: " + positiveIntervals.size() + "pos. intervals 2: " + second.getPositiveIntervals().size());
		System.err.println("minX:" + minX + " minY:" + minY + " maxX:" + maxX + " maxY:" + maxY);
		
		int compatibleCount = countConflicts(second);
		errorString = errorString + " " + compatibleCount + " violations.";
		
		BufferedImage img = new BufferedImage(newWidth, newHeight,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		g2d.setBackground(Color.WHITE);
		g2d.setColor(Color.BLACK);
		g2d.clearRect(0, 0, newWidth, newHeight);
		
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		        RenderingHints.VALUE_ANTIALIAS_ON);
		    Font font = new Font("Serif", Font.PLAIN, 14);
		    g2d.setFont(font);
		g2d.drawString(errorString, 20, 20);
		System.out.println(errorString);
		
		newFirst.addIntervalsToGraphic(g2d, newFirst.getPositiveIntervals(), false,
				Color.BLUE);
		newSecond.addIntervalsToGraphic(g2d, newSecond.getPositiveIntervals(), false,
				Color.GREEN);
		
		newFirst.addConflictPointsToGraphic(g2d, newSecond);
		
		//ShowDebugImage frame = new ShowDebugImage("New intervals", img);
		//frame.refresh(img);
		
		String filename = "experiments/images/" + getCurrentTimeStamp() + "-"+ errorString + ".png";
		
		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}
	}
	
	// tests

	/**
	 * test if two SensorData is compatible, with constraints: The two have at
	 * least one intersection in the positive component The two have no
	 * intersection between positive and negative components
	 * 
	 * @param otherData
	 * @return
	 */
	public int countPositiveIntersection(SensorData otherData){
		int positiveIntersect = 0;


		List<SensorInterval> otherPositiveIntervals = otherData
				.getPositiveIntervals();
		
		for (int i = 0; i < positiveIntervals.size(); i++) {
			SensorInterval curPositive = positiveIntervals.get(i);
			
			
			// check for intersection of other positive intervals
			// there is a intersection with other positive interval
			for (int j = 0; j < otherPositiveIntervals.size(); j++) {
				SensorInterval otherPositive = otherPositiveIntervals.get(j);
				if (curPositive.intersects(otherPositive)) {
					positiveIntersect++;
				}
			}
			

			//if (v) violations++;
		}
		return positiveIntersect;
	}
	
	public double sumClosestHullError(SensorData otherData){
		
		double sum = 0;
		
		List<Point2D> thisHull = getConvexHull();
		List<Point2D> otherHull = otherData.getConvexHull();
		
		for (int i=0; i<thisHull.size(); i++){
			double min = Double.MAX_VALUE;
			Point2D thisPoint = thisHull.get(i);
			for (int j=0; j<otherHull.size(); j++){
				Point2D otherPoint = otherHull.get(j);
				double dist = thisPoint.distance(otherPoint);
				if (dist<min) min = dist;
			}
			sum += min;
		}
				
		return sum;
	}
	
	
	public List<Point2D> getAllPositivePoints(){
		List<Point2D> thisPoints = new ArrayList<Point2D>();
		
		for (int i=0; i<positiveIntervals.size(); i++){
			thisPoints.add(positiveIntervals.get(i).getStart());
			thisPoints.add(positiveIntervals.get(i).getEnd());
		}
		
		return thisPoints;
	}
	
	
	// A heuristic to see how close we are to the matching solution
	public double sumClosestPointError(SensorData otherData){
		double sum = 0;
		List<Point2D> thisPoints = getAllPositivePoints();
		List<Point2D> otherPoints = otherData.getAllPositivePoints();
		
		for (int i=0; i<thisPoints.size(); i++){
			double min = Double.MAX_VALUE;
			Point2D thisPoint = thisPoints.get(i);
			for (int j=0; j<otherPoints.size(); j++){
				Point2D otherPoint = otherPoints.get(j);
				double dist = thisPoint.distance(otherPoint);
				if (dist<min) min = dist;
			}
			sum += min;
		}
		
		return sum;
	}
	
	
	public Point2D getClosestPositivePoint(Point2D point){
		
		SensorInterval shortest = positiveIntervals.get(0);
		double minDist = Double.MAX_VALUE;
		for (SensorInterval s:positiveIntervals){
			double dist = s.getInterval().ptSegDist(point);
			if (dist < minDist){
				minDist = dist;
				shortest = s;
			}
		}		
		return shortest.closestPoint(point);
	}
	
	public SensorData applyRotate(double rotateFactor){
		double rotateAngle = rotateFactor/360*2*Math.PI;
		Point2D centroid = getPosIntCentroid();
		AffineTransform rotate = new AffineTransform();
		rotate.rotate(rotateAngle, centroid.getX(), centroid.getY());
		return applyAffineTransform(rotate);
	}
	
	public double evaluateRotate(double rotateFactor, SensorData otherData){
		SensorData rotateData = applyRotate(rotateFactor);
		return rotateData.sumClosestPointError(otherData);
	}
	
	/***
	 * Using the golden section search to find the best rotate that minimize displacement error
	 * @param a
	 * @param b
	 * @param c
	 * @param tau
	 * @param otherData
	 * @return
	 */
	public SensorData goldenSectionSearchRotate(double a, double b, double c, double tau, SensorData otherData){
		// a and c are the current bounds; the minimum is between them.
		// b is a center point
		// tau is a tolerance parameter
		
		double x;
		double phi = (1+ Math.sqrt(5))/2;
		double resphi = 2 - phi;
		if (c-b > b - a)
			x = b + resphi * (c - b);
		else
			x = b - resphi * (b - a);
		if (Math.abs(c - a) < tau * (Math.abs(b) + Math.abs(x))){
			return applyRotate((c+a)/2);			
		}
		
		if (evaluateRotate(x,otherData) < evaluateRotate(b,otherData)){
			if (c - b > b - a) return goldenSectionSearchRotate(b, x, c, tau,otherData);
		    else return goldenSectionSearchRotate(a, x, b, tau,otherData);
		}
		else {
			if (c - b > b - a) return goldenSectionSearchRotate(a, b, x, tau, otherData);
		      else return goldenSectionSearchRotate(x, b, c, tau, otherData);
		}

	}
	
	public SensorData gsRotateMatch(SensorData otherData){
		return goldenSectionSearchRotate(-5, 0, 5, 0.001, otherData);
	}
	
	
	public SensorData rotateMatch(SensorData otherData){
		return rotateMatch(otherData, -180, 180, 1);
	}
	
	public SensorData limitedRotateMatch(SensorData otherData){
		return rotateMatch(otherData, -5,5,0.1);
	}
	
	
	// rotate this SensorData about its centroid in order to match to otherData
	public SensorData rotateMatch(SensorData otherData, double min, double max, double resolution){
		SensorData bestTransform = null;
		double minError = Double.MAX_VALUE;
		double bestAngle = 0;
		Point2D centroid = getPosIntCentroid();
		/**
		 * 1 degree at a time, avg error 164.2 in 40 seconds for 10 instances g=5; f=0
		 * 0.5 degree at a time, avg error 93.70 in 86 seconds
		 * 0.25 degree at a time, avg error 40 in 154 seconds
		 * 0.1 degree at a time, avg error 20 in 400 seconds
		 * 
		 */
		for (double i=min; i<max; i+=resolution){
			double angle = i/360*2*Math.PI;
			AffineTransform rotateTransform = new AffineTransform();
			rotateTransform.rotate(angle, centroid.getX(), centroid.getY());
			SensorData transformedData = applyAffineTransform(rotateTransform);
			// double hullError = transformedData.sumClosestHullError(otherData);
			double compatibleError = transformedData.sumClosestPointError(otherData);
			// double compatibleError = transformedData.countConflicts(otherData);
			
			if (transformedData.countConflicts(otherData)==0){				
				return transformedData;
			}
			if (compatibleError < minError){
				//System.out.println(String.format("prev min error %.2f, new min error %.2f",minError, compatibleError));
				bestAngle = angle;
				bestTransform = transformedData;
				minError = compatibleError;
				//bestTransform.drawMeasurements(otherData, "bestguess-"+angle + "radians");
			}			
			
		}
		
		int conflicts = bestTransform.countConflicts(otherData);
		
		// System.out.println(String.format(conflicts + " conflicts, error %.2f, recover rotate by %.2f radians about ", minError, bestAngle) + centroid);
		
		if (bestTransform == null){
			System.err.println("rotateMatch found nothing.");
			System.exit(0);
		}
		
		return bestTransform;
	}
	
	/* Local search heuristic to get next translation
	* @returns transformed sensor data that is the next state in the local search.
	*/ 
	public SensorData localSearchStep(SensorData otherData){
		
		// heuristic
		// analyze all conflicts
		List<Point2D> conflictPoints = getConflictPoints(otherData);
		
		double minDist = Double.MAX_VALUE;
		double dx=0, dy=0;
		
		for (Point2D pt:conflictPoints){
			Point2D closestPoint = otherData.getClosestPositivePoint(pt);
			
			double dist = closestPoint.distance(pt);
			
			if (dist < minDist){
				minDist = dist;
				dx = closestPoint.getX() - pt.getX();
				dy = closestPoint.getY() - pt.getY();
			}						
		}
		
		List<Point2D> otherConflictPoints = otherData.getConflictPoints(this);
		
		for (Point2D pt:otherConflictPoints){
			Point2D closestPoint = getClosestPositivePoint(pt);
			
			double dist = closestPoint.distance(pt);
			
			if (dist < minDist){
				minDist = dist;
				dx = pt.getX() - closestPoint.getX();
				dy = pt.getY() - closestPoint.getY();
			}
		}
		
		//System.out.println("Moving dx=" + dx + " dy=" + dy);
		
		AffineTransform at = new AffineTransform();
		
		at.translate(dx, dy);
		
		return applyAffineTransform(at);
	}
	
	
	// local search
	public SensorData localSearchMatch(SensorData otherData, int stepsRemaining){
		// base case
				double error = countConflicts(otherData);
				
				if (error == 0) {		
					System.out.println("Search ends with match found, error " + error);
					return this;
				}
				
				if (stepsRemaining == 0) {		
					System.out.println("Search ends with mininum error = " + error);
					return this;
				}
				
				SensorData translatedData = localSearchStep(otherData);
				
				SensorData transformedData = translatedData.rotateMatch(otherData);
				
				return transformedData.localSearchMatch(otherData, stepsRemaining-1);				
	}
	
	
	// 
	public SensorData greedySearchMatch(SensorData otherData, int stepsRemaining){
		
		// base case
		double error = countConflicts(otherData);
		
		if (error == 0) {		
			System.out.println("Search ends with match found, error " + error);
			return this;
		}
		
		if (stepsRemaining == 0) {		
			System.out.println("Search ends with mininum error = " + error);
			return this;
		}
		
		
		
		// step:
		SensorData bestTransform = this;
		
		double minError = Double.MAX_VALUE;
		
		double bestDx=0, bestDy=0;
		
		for (double dx = -1; dx <=1; dx+=0.5){
			for (double dy=-1; dy <=1; dy+=0.5){
				// if (dx==0 && dy == 0) continue;
				
				AffineTransform translateTransform = new AffineTransform();
				translateTransform.translate(dx, dy);
				
				SensorData translatedData = applyAffineTransform(translateTransform);
				
				SensorData transformedData = translatedData.rotateMatch(otherData);
				
				double thisError = transformedData.sumClosestHullError(otherData);
				
				if (transformedData.countConflicts(otherData)==0){
					System.out.println("Translate dx=" + bestDx + " dy=" + bestDy + ", Search MinError=" + minError + " with " + stepsRemaining + " steps remaining");
					return transformedData;
				}
				
				if (thisError < minError){
					minError = thisError;
					bestTransform = transformedData;
					bestDx = dx; bestDy = dy;
				}
			}
		}
				
		
		System.out.println("Translate dx=" + bestDx + " dy=" + bestDy + ", Search MinError=" + minError + " with " + stepsRemaining + " steps remaining");
		
		if (bestDx == 0 && bestDy==0) return this;
		
		return bestTransform.greedySearchMatch(otherData, stepsRemaining-1);
	}
	
	// return list of points where the positive intervals in this SensorData intersects
	// negative intervals of the other SensorData
	public List<Point2D> getConflictPoints(SensorData otherData){
		List<Point2D> conflictList = new ArrayList<Point2D>();
				
		for (SensorInterval curPositive:positiveIntervals) {			
			for (SensorInterval otherNegative:otherData.getNegativeIntervals()) {			
				Point2D intersectionPoint = curPositive.getIntersectionPoint(otherNegative);
				if (intersectionPoint != null) {
					conflictList.add(intersectionPoint);
				}
			}
		}
		
		return conflictList;
	}
	
	/***
	 * Method: positiveIntersects
	 * @param otherData
	 * @return true iff positive intervals of this SensorData intersects 
	 * with positive intervals of otherData.
	 */
	public boolean positiveIntersects(SensorData otherData){
		
		// loop through positive intervals
		for (SensorInterval curPositive:positiveIntervals) {			
			for (SensorInterval otherPositive:otherData.getPositiveIntervals()) {	
				// return true if we found an intersection
				if (curPositive.getIntersectionPoint(otherPositive) != null) {
					return true;
				}
			}
		}		
		return false;
	}
	
	/***
	 * Method: countConflicts
	 * count conflicts, which are positive intervals intersection with 
	 * negative intervals from another SensorData
	 * @param otherData
	 * @return Integer.MAX_VALUE if this SensorData doesn't positively intersects with 
	 * positive intervals of otherData, otherwise returns the 
	 * number of conflicts this SensorData has with otherData
	 * 
	 */
	
	public int countConflicts(SensorData otherData){
		int conflicts = Integer.MAX_VALUE;
		
		// return -1 
		if (positiveIntersects(otherData) == false){
			return conflicts;
		}
		
		conflicts = getConflictPoints(otherData).size();
		conflicts += otherData.getConflictPoints(this).size();
		
		return conflicts;
	}
	
	
	/***
	 * Depreciated
	 * @param otherData
	 * @return number of violations
	 */
	public int isCompatible(SensorData otherData) {

		boolean positiveIntersect = false;

		List<SensorInterval> thisNegativeIntervals = getNegativeIntervalsFromPositive();
		List<SensorInterval> otherPositiveIntervals = otherData
				.getPositiveIntervals();
		List<SensorInterval> otherNegativeIntervals = otherData
				.getNegativeIntervalsFromPositive();

		int violations = 0;

		// check every positive interval of this sensor
		for (int i = 0; i < positiveIntervals.size(); i++) {
			SensorInterval curPositive = positiveIntervals.get(i);
			
			
			// check for intersection of other positive intervals
			// there is a intersection with other positive interval
			for (int j = 0; j < otherPositiveIntervals.size()
					&& !positiveIntersect; j++) {
				SensorInterval otherPositive = otherPositiveIntervals.get(j);
				if (curPositive.intersects(otherPositive)) {
					positiveIntersect = true;
				}
			}
			
			// no intersection with other negative intervals
			boolean v=false;
			for (int j = 0; j < otherNegativeIntervals.size(); j++) {
				SensorInterval otherNegative = otherNegativeIntervals.get(j);
				if (curPositive.intersects(otherNegative)) {
					v=true;
					break;
					//violations++;
				}
			}
			if (v) violations++;
		}

		// Check every negative interval of this sensor
		for (int j = 0; j < otherPositiveIntervals.size(); j++) {
			boolean v=false;
			// check for intersection of other positive intervals
			for (int i = 0; i < thisNegativeIntervals.size(); i++) {
				SensorInterval curNegative = thisNegativeIntervals.get(i);
				if (curNegative.intersects(otherPositiveIntervals.get(j))) {
					v=true;
					break;
					//violations++;
				}
			}
			if (v) violations++;
		}

		// check outer intervals

		if (positiveIntersect) return violations;
		else return Integer.MAX_VALUE;
	}
	
	// normalizeRotate 
	// return the normalized SensorData (angle = 0)
	// not complete normalize (no translate)
	public SensorData normalizeRotate(){
		SensorData normalizedData = new SensorData(width, height);
		normalizedData.sensorAngle = 0;
		normalizedData.sensorGap = sensorGap;
		normalizedData.sensorCount = sensorCount;
		
		AffineTransform rotate = new AffineTransform();
		rotate.rotate(-sensorAngle + Math.PI / 2, width / 2, height / 2);
		
		for (int i = 0; i<positiveIntervals.size(); i++){
			SensorInterval interval = positiveIntervals.get(i);
			Point2D pt1 = interval.getStart();
			Point2D pt2 = interval.getEnd();
			SensorInterval newInterval = new SensorInterval(interval.getSensorID(), rotate.transform(pt1, null), rotate.transform(pt2, null)  );
			normalizedData.addPositiveInterval(newInterval);
		}
		
		for (int i = 0; i<negativeIntervals.size(); i++){
			SensorInterval interval = negativeIntervals.get(i);
			Point2D pt1 = interval.getStart();
			Point2D pt2 = interval.getEnd();
			SensorInterval newInterval = new SensorInterval(interval.getSensorID(), rotate.transform(pt1, null), rotate.transform(pt2, null)  );
			normalizedData.addNegativeInterval(newInterval);
		}				
		return normalizedData;
	}
	
	// normalize
	// return the normalized SensorData (angle=0) with translate component
	public SensorData normalize(){
		
		// variables denoting the parameters of affine transformation
		double translateX =0, translateY=0;
		double minX=Double.MAX_VALUE, minY=Double.MAX_VALUE,maxX=Double.MIN_VALUE, maxY=Double.MIN_VALUE;
		double rotateAngle = -sensorAngle+Math.PI/2, rotateCentreX = width/2, rotateCentreY = height/2;		
		AffineTransform aTransform = new AffineTransform();
				
		// first we rotate, get the sense of the dimension of the positive components
		aTransform.rotate(rotateAngle, rotateCentreX, rotateCentreY);
		for (int i = 0; i<positiveIntervals.size(); i++){
			SensorInterval interval = positiveIntervals.get(i);
			Point2D pt1 = aTransform.transform(interval.getStart(),null);
			Point2D pt2 = aTransform.transform(interval.getEnd(),null);
			minX =  Math.min(pt1.getX(),minX);
			minX =  Math.min(pt2.getX(),minX);
			minY =  Math.min(pt1.getY(),minY);
			minY =  Math.min(pt2.getY(),minY);
			maxX =  Math.max(pt1.getX(),maxX);
			maxX =  Math.max(pt2.getX(),maxX);
			maxY =  Math.max(pt1.getY(),maxY);
			maxY =  Math.max(pt2.getY(),maxY);
		}
		
		if (minX<0) translateX = -1*minX;
		if (minY<0) translateY = -1*minY;
		maxX += translateX+1;
		maxY += translateY+1;
		
		aTransform.translate(translateX, translateY);
		
		SensorData normalizedData = new SensorData((int)maxX, (int)maxY);
		normalizedData.sensorAngle = 0;
		normalizedData.sensorGap = sensorGap;
		normalizedData.sensorCount = sensorCount;
		
		for (int i = 0; i<positiveIntervals.size(); i++){
			SensorInterval interval = positiveIntervals.get(i);
			Point2D pt1 = interval.getStart();
			Point2D pt2 = interval.getEnd();
			SensorInterval newInterval = new SensorInterval(interval.getSensorID(), aTransform.transform(pt1, null), aTransform.transform(pt2, null)  );
			normalizedData.addPositiveInterval(newInterval);
		}
		
		for (int i = 0; i<negativeIntervals.size(); i++){
			SensorInterval interval = negativeIntervals.get(i);
			Point2D pt1 = interval.getStart();
			Point2D pt2 = interval.getEnd();
			SensorInterval newInterval = new SensorInterval(interval.getSensorID(), aTransform.transform(pt1, null), aTransform.transform(pt2, null)  );
			normalizedData.addNegativeInterval(newInterval);
		}	
		
		return normalizedData;
	}
	

	/**
	 * write positive and negative intervals into 2 files
	 * 
	 * @param positiveFileName
	 * @param negativeFileName
	 * @param normalize
	 *            if the intervals need to be normalized
	 * @throws IOException
	 */
	public void writeIntervalsToFile(String positiveFileName,
			String negativeFileName, boolean normalize) throws IOException {

		System.out.println("saving positive intervals to " + positiveFileName);
		BufferedWriter outPositive = new BufferedWriter(new FileWriter(
				positiveFileName));

		System.out.println("saving negative intervals to " + negativeFileName);
		BufferedWriter outNegative = new BufferedWriter(new FileWriter(
				negativeFileName));

		for (SensorInterval si : positiveIntervals) {
			Point2D pt1 = si.getStart();
			Point2D pt2 = si.getEnd();
			if (normalize) {
				AffineTransform rotate = new AffineTransform();
				rotate.rotate(-sensorAngle + Math.PI / 2, width / 2, height / 2);
				pt1 = rotate.transform(si.getStart(), null);
				pt2 = rotate.transform(si.getEnd(), null);
			}

			outPositive.write("Sensor" + si.getSensorID() + " [" + pt1.getX()
					+ "," + pt1.getY() + "] ");
			outPositive.write("[" + pt2.getX() + "," + pt2.getY() + "]\n");

		}

		for (SensorInterval si : negativeIntervals) {
			Point2D pt1 = si.getStart();
			Point2D pt2 = si.getEnd();
			if (normalize) {
				AffineTransform rotate = new AffineTransform();
				rotate.rotate(-sensorAngle + Math.PI / 2, width / 2, height / 2);
				pt1 = rotate.transform(si.getStart(), null);
				pt2 = rotate.transform(si.getEnd(), null);
			}

			outNegative.write("Sensor" + si.getSensorID() + " [" + pt1.getX()
					+ "," + pt1.getY() + "] ");
			outNegative.write("[" + pt2.getX() + "," + pt2.getY() + "]\n");

		}
		outPositive.close();
		outNegative.close();
	}
	

	
	/***
	 * 
	 * @param extendPercentage = the amount to extend positive intervals in terms of percentage
	 * @return a new sensordata with positive intervals extended by the specified amount.
	 * 
	 * This is to be used to test quality of approximations in matching overlapping sensor intervals.
	 */
	public SensorData lengthenBuffer(double extendPercentage){
		SensorData newData = new SensorData(this);
		
		double extendFactor = 0.01 * extendPercentage;
		
		for (int j=0; j<newData.positiveIntervals.size(); j++){
			
			SensorInterval pi = newData.positiveIntervals.get(j);
			
			Point2D startPt = pi.getStart();
			Point2D endPt = pi.getEnd();
			
			SensorInterval newPI = pi.extend(extendPercentage);
						
			
			for (int i=0; i<newData.negativeIntervals.size(); i++){
				SensorInterval ni = newData.negativeIntervals.get(i);
				if (ni.getStart().distance(pi.getEnd()) <0.0001){				
					if (ni.getInterval().ptSegDist(newPI.getEnd()) < 0.0001){
						ni.setStart(newPI.getEnd());
						newData.negativeIntervals.set(i, ni);
					}
					else {
						newData.negativeIntervals.remove(i);
						i--;
					}
				}
				if (ni.getEnd().distance(pi.getStart()) < 0.0001){
					if (ni.getInterval().ptSegDist(newPI.getStart()) < 0.0001){
						ni.setEnd(newPI.getStart());
						newData.negativeIntervals.set(i, ni);
					}
					else {
						newData.negativeIntervals.remove(i);
						i--;
					}
				}				
			}
			
			// set the line.
			pi.setInterval(newPI.getInterval());
			newData.positiveIntervals.set(j, pi);
		}
		
		return newData;
	}

	// tests
	
	/**
	 * 
	 * Test if another SensorData can be integrated without violating constraints
	 * @return true if no constraint is violated
	 */
	
	public int testExtendMatch(SensorData otherData){
		
		if (this.countConflicts(otherData)== 0) return 0;
		
		int extend = 0;
		
		for (extend = 1; extend <= 1000; extend *= 2){
			SensorData thisExtend = lengthenBuffer(extend);
			SensorData otherExtend = otherData.lengthenBuffer(extend);
			
			if (thisExtend.countConflicts(otherExtend)==0){
				return extend;
			}
		}
		
		return -1;
	}

	

	/**
	 * test parsing file "data/test0000-linecoord[0..2]" draw their convex hull
	 */
	public static void testDraw() {
		int width = 800, height = 600;
		String filePrefix = "data/test0000-linecoordnorm";
		for (int i = 0; i <= 2; i++) {
			String filename = filePrefix + "[" + i + "]";
			SensorData d = new SensorData(filename, width, height);

			String hullpicname = filename + "-hull.png";
			d.drawConvexHull(hullpicname);

			String intervalPicName = filename + "-intervals.png";
			d.drawPositiveIntervals(intervalPicName);

			System.out.println("Drawn " + hullpicname + ", " + intervalPicName);
		}
	}

	/**
	 * Draw region with sensors
	 * 
	 * @param showNeg
	 *            show positive or negative sensor intervals
	 * @throws Exception
	 */
	public static void testDrawFromComplexRegion(boolean showNeg)
			throws Exception {
		ShowDebugImage frame = null;
		int width = 800;
		int height = 600;

		ComplexRegion complexRegion = new ComplexRegion(width, height);
		BufferedImage img = complexRegion.drawRegion();
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		SensorData d = new SensorData(complexRegion, 12, Math.PI / 4,
				complexRegion.getWidth(), complexRegion.getHeight());

		if (showNeg) {
			d.addIntervalsToGraphic(g2d, d.negativeIntervals, false, Color.RED);
		} else {
			d.addIntervalsToGraphic(g2d, d.positiveIntervals, false,
					Color.BLACK);
		}
		frame = new ShowDebugImage("Regions with intervals", img);
		frame.refresh(img);
	}

	public static void main(String[] args) throws Exception {
		// testDraw();
		testDrawFromComplexRegion(true);

	}
}

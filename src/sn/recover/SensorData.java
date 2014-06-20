package sn.recover;

import java.awt.Color;
import java.awt.Graphics2D;
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

import javax.imageio.ImageIO;

import sn.debug.ShowDebugImage;
import sn.regiondetect.ComplexRegion;
import sn.regiondetect.GeomUtil;
import sn.regiondetect.Region;

// The class for one set of sensor data
public class SensorData {

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

	/**
	 * Construct from a complex region and other info
	 * 
	 * @param complexRegion
	 * @param gap
	 * @param angle
	 * @param canvasWidth
	 * @param canvasHeight
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
	public void drawPositiveIntervals(String filename, BufferedImage img, boolean useOffsets) {

		if (img == null) {
			// Initialize image
			img = new BufferedImage(1024, 800,
					BufferedImage.TYPE_4BYTE_ABGR);
		}
		Graphics2D g2d = (Graphics2D) img.createGraphics();
		g2d.setColor(Color.BLACK);

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
	
	public void drawPositiveIntervals(String filename, BufferedImage img) {
		drawPositiveIntervals(filename, img, true);
	}
	
	public void drawPositiveIntervals(String filename){
		drawPositiveIntervals(filename, null); 
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

	// tests

	/**
	 * test if two SensorData is compatible, with constraints: The two have at
	 * least one intersection in the positive component The two have no
	 * intersection between positive and negative components
	 * 
	 * @param otherData
	 * @return
	 */
	public boolean isCompatible(SensorData otherData) {

		boolean positiveIntersect = false;

		List<SensorInterval> thisNegativeIntervals = getNegativeIntervalsFromPositive();
		List<SensorInterval> otherPositiveIntervals = otherData
				.getPositiveIntervals();
		List<SensorInterval> otherNegativeIntervals = otherData
				.getNegativeIntervalsFromPositive();

		for (int i = 0; i < positiveIntervals.size(); i++) {
			SensorInterval curPositive = positiveIntervals.get(i);

			// check for intersection of other positive intervals
			for (int j = 0; j < otherPositiveIntervals.size()
					&& !positiveIntersect; j++) {
				SensorInterval otherPositive = otherPositiveIntervals.get(j);
				if (curPositive.intersects(otherPositive)) {
					positiveIntersect = true;
				}
			}

			// check for intersection of other negative intervals
			for (int j = 0; j < otherNegativeIntervals.size(); j++) {
				SensorInterval otherNegative = otherNegativeIntervals.get(j);
				if (curPositive.intersects(otherNegative)) {
					return false;
				}
			}
		}

		// check for intersection of negative intervals with other positive
		for (int i = 0; i < thisNegativeIntervals.size(); i++) {
			SensorInterval curNegative = thisNegativeIntervals.get(i);

			// check for intersection of other positive intervals
			for (int j = 0; j < otherPositiveIntervals.size(); j++) {
				if (curNegative.intersects(otherPositiveIntervals.get(j))) {
					return false;
				}
			}
		}

		// check outer intervals

		return positiveIntersect;
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

	// tests

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

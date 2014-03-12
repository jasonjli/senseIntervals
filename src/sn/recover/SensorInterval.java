package sn.recover;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

//sub-class for intervals, denoting positive components detected in sensor data
public class SensorInterval {
	private int sensorID; // the id of the sensor where this interval belongs
	private Line2D interval;

	/**
	 * constructor
	 * 
	 * @param id
	 *            sensor ID
	 * @param s
	 *            start point
	 * @param e
	 *            end point
	 */
	public SensorInterval(int id, Point2D s, Point2D e) {
		sensorID = id;
		interval = new Line2D.Double(s, e);
	}
	
	/**
	 * constructor
	 * @param id
	 * @param line 
	 */
	public SensorInterval(int id, Line2D line) {
		sensorID = id;
		interval = line;
	}


	/**
	 * constructor from parsing a string
	 * 
	 * @param sensorData
	 *            sensor data record from file
	 */
	public SensorInterval(String sensorData) {
		// parse the string, detecting positive component
		String[] data = sensorData.split(" ");
		sensorID = Integer.parseInt(data[0].split("Sensor")[1]);

		String p1 = data[1].substring(1, data[1].length() - 1);
		double x1 = Double.parseDouble(p1.split(",")[0]);
		double y1 = Double.parseDouble(p1.split(",")[1]);
		Point2D start = new Point2D.Double(x1, y1);

		String p2 = data[2].substring(1, data[2].length() - 1);
		double x2 = Double.parseDouble(p2.split(",")[0]);
		double y2 = Double.parseDouble(p2.split(",")[1]);
		Point2D end = new Point2D.Double(x2, y2);

		interval = new Line2D.Double(start, end);
	}

	// methods for accessing interval information

	public int getSensorID() {
		return sensorID;
	}

	public Point2D getStart() {
		return interval.getP1();
	}

	public Point2D getEnd() {
		return interval.getP2();
	}

	public Line2D getInterval() {
		return interval;
	}

	/**
	 * get the angle of the interval
	 * 
	 * @return double angle
	 */
	public double getAngle() {
		double y1 = interval.getP1().getY();
		double x1 = interval.getP1().getX();
		double y2 = interval.getP2().getY();
		double x2 = interval.getP2().getX();
		return Math.atan2(y2 - y1, x2 - x1);
	}

	/**
	 * Get the start and end point of the full interval from the function 
	 * y = w0 + w1*x where w0 = (x1*y2-x2*y1)/(x1-x2) w1 = (y1-y2)/(x1-x2)
	 * 
	 * @return Line2D intervalLine
	 */
	public Line2D getFullInterval(int width, int height) {
		Line2D fullInterval;
		double y1 = interval.getP1().getY();
		double x1 = interval.getP1().getX();
		double y2 = interval.getP2().getY();
		double x2 = interval.getP2().getX();
		double w0, w1;
		double xStart, yStart, xEnd, yEnd;

		// if x1 = x2, the interval is parallel to y axis
		if (x1 == x2) {
			xStart = x1;
			xEnd = x2;
			yStart = 0;
			yEnd = height;

			return new Line2D.Double(xStart, yStart, xEnd, yEnd);
		}

		w0 = (x1 * y2 - x2 * y1) / (x1 - x2);
		w1 = (y1 - y2) / (x1 - x2);

		xStart = 0d;
		yStart = w0 + w1 * xStart;

		if (yStart < 0) {
			yStart = 0d;
			xStart = (yStart - w0) / w1;

			xEnd = width;
			yEnd = w0 + w1 * xEnd;

			// if yStart < 0, then yEnd must be greater than 0
			// to ensure part of the interval is in the canvas
			assert (yEnd > 0) : "interval out of bound";

			if (yEnd > height) {
				yEnd = height;
				xEnd = (yEnd - w0) / w1;
			}
		}

		else if (yStart > height) {
			yStart = height;
			xStart = (yStart - w0) / w1;

			xEnd = width;
			yEnd = w0 + w1 * xEnd;

			// if yStart > height, then yEnd must be smaller than 0
			// to ensure part of the interval is in the canvas
			assert (yEnd < height) : "interval out of bound";

			if (yEnd < 0) {
				yEnd = 0;
				xEnd = (yEnd - w0) / w1;
			}
		}

		else {
			xEnd = width;
			yEnd = w0 + w1 * xEnd;
			
			if (yEnd > height) {
				yEnd = height;
				xEnd = (yEnd - w0) / w1;
			}
			
			else if (yEnd < 0) {
				yEnd = 0;
				xEnd = (yEnd - w0) / w1;
			}
		}

		fullInterval = new Line2D.Double(xStart, yStart, xEnd, yEnd);

		return fullInterval;
	}

	/**
	 * get the distance between this interval and another parallel line given a
	 * point on the line
	 * 
	 * @param prevPoint
	 * @return double distance between a point and a line
	 */
	public double getDistanceToLine(Point2D prevPoint) {

		double dY = interval.getP1().getY() - prevPoint.getY();
		double dX = interval.getP1().getX() - prevPoint.getX();
		double hyp = prevPoint.distance(interval.getP1());
		double tmpAngle = Math.atan2(dY, dX) - getAngle();

		return Math.abs(Math.sin(tmpAngle) * hyp);

	}

	/**
	 * get the negative component between this and another following interval
	 * 
	 * @param otherInterval
	 * @return
	 */
	public SensorInterval getNegativeInterval(SensorInterval otherInterval) {
		if (sensorID != otherInterval.getSensorID()) {
			return null;
		}
		return new SensorInterval(sensorID, getEnd(), otherInterval.getStart());
	}

	/**
	 * get negative interval before first positive interval
	 * 
	 * @return
	 */
	public SensorInterval getNegativePreInterval() {
		// work out coordinates
		int extendLength = 200;
		double newX = getStart().getX() - extendLength * Math.cos(getAngle());
		double newY = getStart().getY() - extendLength * Math.sin(getAngle());
		Point2D negStart = new Point2D.Double(newX, newY);

		return new SensorInterval(sensorID, negStart, getStart());
	}

	/**
	 * get negative interval after last positive interval
	 * 
	 * @return
	 */
	public SensorInterval getNegativePostInterval() {
		// work out coordinates
		int extendLength = 200;
		double newX = getEnd().getX() + extendLength * Math.cos(getAngle());
		double newY = getEnd().getY() + extendLength * Math.sin(getAngle());
		Point2D negEnd = new Point2D.Double(newX, newY);

		return new SensorInterval(sensorID, getEnd(), negEnd);
	}

	// tests

	/**
	 * test if two intervals are intersecting
	 * 
	 * @param newInterval
	 * @return true if intersects
	 */
	public boolean intersects(SensorInterval newInterval) {
		return interval.intersectsLine(newInterval.getInterval());
	}
}
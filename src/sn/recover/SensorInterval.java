package sn.recover;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

//sub-class for intervals, denoting positive components detected in sensor data
	public class SensorInterval{		
		private int sensorID;	// the id of the sensor where this interval belongs
		private Line2D interval;
		
		// constructor
		public SensorInterval(int id, Point2D s, Point2D e){
			sensorID = id; 
			interval = new Line2D.Double(s,e);
		}
		
		// constructor from parsing a string
		public SensorInterval(String sensorData){
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
		public int getSensorID(){
			return sensorID;
		}
		
		public Point2D getStart(){
			return interval.getP1();
		}
		
		public Point2D getEnd(){
			return interval.getP2();
		}
		
		public Line2D getInterval(){
			return interval;
		}
		
		// get the angle of the interval
		public double getAngle(){
			double y1 = interval.getP1().getY();
			double x1 = interval.getP1().getX();
			double y2 = interval.getP2().getY();
			double x2 = interval.getP2().getX();
			return Math.atan2(y2-y1, x2-x1);
		}
		
		// get the distance between this interval and another parallel line given a point on the line
		public double getDistanceToLine(Point2D prevPoint){

			double dY = interval.getP1().getY() - prevPoint.getY();
			double dX = interval.getP1().getX() - prevPoint.getX();
			double hyp = prevPoint.distance(interval.getP1());
			double tmpAngle = Math.atan2(dY, dX)-getAngle();
			
			return Math.abs(Math.sin(tmpAngle) * hyp);
						
		}
		
		// get the negative component between this and another following interval
		public SensorInterval getNegativeInterval(SensorInterval otherInterval){					
			if (sensorID != otherInterval.getSensorID()){
				return null;
			}
			return new SensorInterval(sensorID, getEnd(), otherInterval.getStart());
		}
		
		
		
		// tests
		
		// test if two intervals are intersecting
		// returns true if intersects
		public boolean intersects(SensorInterval newInterval){
			return interval.intersectsLine(newInterval.getInterval());
		}
	}	
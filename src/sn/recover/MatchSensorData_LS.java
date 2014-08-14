package sn.recover;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.List;
import java.util.HashSet;

/*
 * Match SensorData using local search
 */
public class MatchSensorData_LS extends MatchSearchingStrategy {

	// sensor data. The task is match firstData to secondData


	// the maximum number of steps in the local search
	private final int searchSteps;


	private final int searchHeuristic;
	private final int randomHeuristic = 0, minDistHeuristic = 1, 
			maxDistHeuristic = 2, avgDistHeuristic = 3;


	// a list of previous positions identified by their centroids
	private HashSet<Point2D> prevPositions;

	// constructor
	public MatchSensorData_LS(int steps, int heuristic){
		searchHeuristic = heuristic;
		searchSteps = steps; 		
		searchInfo = "Local Search, "+ steps + " steps";		
		prevPositions = new HashSet<Point2D>(); // empty set of previous positions
	}


	// constructor, given two sensordata to match eachother
	public MatchSensorData_LS(SensorData a, SensorData b, int steps, int heuristic){
		searchHeuristic = heuristic;
		firstData = a;
		secondData = b;
		searchSteps = steps;
		searchInfo = "Local Search, "+ steps + " steps.";
		prevPositions = new HashSet<Point2D>();
	}	

	public void resetPrevPositions(){
		prevPositions = new HashSet<Point2D>(); // empty set of previous positions
	}

	public SensorData initSearch(){
		return localSearch(firstData, searchSteps);
	}

	public int getSteps(){
		return searchSteps;
	}

	/*
	 * The overall local search algorithm implemented with recursion
	 * Take in the current state
	 */
	public SensorData localSearch(SensorData currentState, int stepsRemaining){

		// first, we rotate the current state on its centroid to see if we have a solution
		SensorData transformedData = currentState.gsRotateMatch(secondData);	
		// SensorData transformedData = currentState.limitedRotateMatch(secondData);

		double error = transformedData.countConflicts(secondData);
		matchingError = error;

		if (error == 0) {		
			System.out.println("Search ends with match found, error " + error);
			return transformedData;
		}

		if (stepsRemaining == 0) {		
			System.out.println("Search ends with mininum error = " + error);
			return transformedData;
		}


		SensorData translatedData = null;

		switch (searchHeuristic){
		case randomHeuristic:
			translatedData = randomHeuristic(currentState);
			break;
		case minDistHeuristic: 
			translatedData = minDistHeuristic(currentState);
			break;
		case maxDistHeuristic:
			translatedData = maxDistHeuristic(currentState);
			break;
		case avgDistHeuristic:
			translatedData = avgDistHeuristic(currentState);
			break;
		default:
			System.err.println("ERROR: Unknown search heuristic " + searchHeuristic + "in MatchSensorData_LS.java");
			System.exit(0);
		}

		if (translatedData==null){ 
			System.out.println("Search ends with mininum error = " + error);
			return transformedData;
		}
		return localSearch(translatedData, stepsRemaining-1);				
	}

	/***
	 * 
	 * @param newCentroid
	 * @return true if newCentroid is very close to a previous centroid
	 */
	public boolean isPrevPosition(Point2D newCentroid){
		boolean isPrev = false;

		for (Point2D oldCentroid : prevPositions){
			if (oldCentroid.distance(newCentroid) < 0.01){
				return true;
			}
		}

		return isPrev;
	}


	public SensorData randomHeuristic(SensorData currentState){

		searchInfo = "Local Search, "+ searchSteps + " steps, random heuristic";

		SensorData rotatedData = currentState.gsRotateMatch(secondData);
		//SensorData rotatedData = currentState.limitedRotateMatch(secondData);

		int currentConflicts = rotatedData.countConflicts(secondData);

		for (int i=0; i<100; i++){
			AffineTransform at = new AffineTransform();

			double dx = Math.random();
			double dy = Math.random();
			at.translate(dx, dy);

			SensorData transformedData = currentState.applyAffineTransform(at).gsRotateMatch(secondData);

			if (currentConflicts > transformedData.countConflicts(secondData) ){
				return transformedData;
			}
		}

		return null;
	}

	public SensorData avgDistHeuristic(SensorData currentState){
		// heuristic
		searchInfo = "Local Search, "+ searchSteps + " steps, avgDist heuristic";

		// analyze all conflicts
		// first, collect set of points on positive intervals intersecting 
		// negative intervals on the other sensor
		List<Point2D> conflictPoints = currentState.getConflictPoints(secondData);

		Point2D centroid = currentState.getPosIntCentroid();
		prevPositions.add(centroid);

		double dx=0, dy=0;
		int count = 0;

		for (Point2D pt:conflictPoints){
			// get the closest point on secondData to this conflict
			Point2D closestPoint = secondData.getClosestPositivePoint(pt);

			// double dist = closestPoint.distance(pt);
			dx += closestPoint.getX() - pt.getX();
			dy += closestPoint.getY() - pt.getY();
			count++;
		}

		List<Point2D> otherConflictPoints = secondData.getConflictPoints(currentState);

		for (Point2D pt:otherConflictPoints){
			Point2D closestPoint = currentState.getClosestPositivePoint(pt);

			//double dist = closestPoint.distance(pt);

			dx += pt.getX() - closestPoint.getX();
			dy += pt.getY() - closestPoint.getY();
			count++;					
		}

		// if (minDist < 0.0001) return null;

		dx = dx / count;
		dy = dy / count;

		// System.out.println("Moving dx=" + dx + " dy=" + dy);

		AffineTransform at = new AffineTransform();

		at.translate(dx, dy);

		return currentState.applyAffineTransform(at);
	}

	/*
	 * Minimum distance heuristic in local search
	 * derive the next search state by moving the minimum distance to resolve a conflict
	 */
	public SensorData minDistHeuristic(SensorData currentState){
		// heuristic
		searchInfo = "Local Search, "+ searchSteps + " steps, minDist heuristic";
		// analyze all conflicts
		List<Point2D> conflictPoints = currentState.getConflictPoints(secondData);

		Point2D centroid = currentState.getPosIntCentroid();
		prevPositions.add(centroid);

		double minDist = Double.MAX_VALUE;
		double dx=0, dy=0;

		for (Point2D pt:conflictPoints){
			Point2D closestPoint = secondData.getClosestPositivePoint(pt);

			double dist = closestPoint.distance(pt);

			if (dist < minDist){

				double tmpDx = closestPoint.getX() - pt.getX();
				double tmpDy = closestPoint.getY() - pt.getY();

				Point2D newCentroid = new Point2D.Double(centroid.getX()+tmpDx, centroid.getY()+tmpDy);

				// avoid repeat previously explored position
				if (isPrevPosition(newCentroid)==false){						
					minDist = dist;
					dx = tmpDx;
					dy = tmpDy;
				}
			}						
		}

		List<Point2D> otherConflictPoints = secondData.getConflictPoints(currentState);

		for (Point2D pt:otherConflictPoints){
			Point2D closestPoint = currentState.getClosestPositivePoint(pt);

			double dist = closestPoint.distance(pt);

			if (dist < minDist){

				double tmpDx = pt.getX() - closestPoint.getX();
				double tmpDy = pt.getY() - closestPoint.getY();

				Point2D newCentroid = new Point2D.Double(centroid.getX()+tmpDx, centroid.getY()+tmpDy);

				// avoid repeat previously explored position
				if (isPrevPosition(newCentroid)==false){						
					minDist = dist;
					dx = tmpDx;
					dy = tmpDy;
				}
			}
		}

		if (minDist < 0.0001) return null;

		// System.out.println("Moving dx=" + dx + " dy=" + dy);

		AffineTransform at = new AffineTransform();

		at.translate(dx, dy);

		return currentState.applyAffineTransform(at);
	}


	/*
	 * Maximum distance heuristic in local search
	 * derive the next search state by moving the minimum distance to resolve a conflict
	 */
	public SensorData maxDistHeuristic(SensorData currentState){


		Point2D centroid = currentState.getPosIntCentroid();
		prevPositions.add(centroid);

		// analyze all conflicts
		List<Point2D> conflictPoints = currentState.getConflictPoints(secondData);

		double maxDist = Double.MIN_VALUE;
		double dx=0, dy=0;

		for (Point2D pt:conflictPoints){
			Point2D closestPoint = secondData.getClosestPositivePoint(pt);

			double dist = closestPoint.distance(pt);

			if (dist > maxDist){

				double tmpDx = closestPoint.getX() - pt.getX();
				double tmpDy = closestPoint.getY() - pt.getY();

				Point2D newCentroid = new Point2D.Double(centroid.getX()+tmpDx, centroid.getY()+tmpDy);

				// avoid repeat previously explored position
				if (isPrevPosition(newCentroid)==false){						
					maxDist = dist;
					dx = tmpDx;
					dy = tmpDy;
				}

			}						
		}

		List<Point2D> otherConflictPoints = secondData.getConflictPoints(currentState);

		for (Point2D pt:otherConflictPoints){
			Point2D closestPoint = currentState.getClosestPositivePoint(pt);

			double dist = closestPoint.distance(pt);

			if (dist > maxDist){
				maxDist = dist;
				double tmpDx = pt.getX() - closestPoint.getX();
				double tmpDy = pt.getY() - closestPoint.getY();

				Point2D newCentroid = new Point2D.Double(centroid.getX()+tmpDx, centroid.getY()+tmpDy);

				// avoid repeat previously explored position
				if (isPrevPosition(newCentroid)==false){						
					maxDist = dist;
					dx = tmpDx;
					dy = tmpDy;
				}
			}
		}

		if (maxDist < 0.0001){
			return null;
		}

		//System.out.println("Moving dx=" + dx + " dy=" + dy);

		AffineTransform at = new AffineTransform();

		at.translate(dx, dy);

		return currentState.applyAffineTransform(at);
	}	
}

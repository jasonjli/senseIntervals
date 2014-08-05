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
	
	// a list of previous positions identified by their centroids
	private HashSet<Point2D> prevPositions;
	
	// constructor
	public MatchSensorData_LS(int steps){
		searchSteps = steps; 		
		searchInfo = "Local Search, "+ steps + " steps, minHeuristic.";		
		prevPositions = new HashSet<Point2D>(); // empty set of previous positions
	}
		
	
	// constructor, given two sensordata to match eachother
	public MatchSensorData_LS(SensorData a, SensorData b, int steps){
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
						
					SensorData transformedData = currentState.limitedRotateMatch(secondData);
			
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
					
					SensorData translatedData = minDistHeuristic(currentState);
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
		
	/*
	 * Minimum distance heuristic in local search
	 * derive the next search state by moving the minimum distance to resolve a conflict
	 */
	public SensorData minDistHeuristic(SensorData currentState){
		// heuristic
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
				
				System.out.println("Moving dx=" + dx + " dy=" + dy);
				
				AffineTransform at = new AffineTransform();
				
				at.translate(dx, dy);
				
				return currentState.applyAffineTransform(at);
	}	
}
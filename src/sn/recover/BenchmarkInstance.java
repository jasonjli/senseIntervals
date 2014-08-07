package sn.recover;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import sn.debug.ShowDebugImage;
import sn.regiondetect.ComplexRegion;

/*
 * A Benchmark Instance consists of:
 * 	A complex region
 */

public class BenchmarkInstance implements java.io.Serializable {
	
		private static final long serialVersionUID = 1L;
		
		public final int instanceID;
		
		private final double gapSize, diffAngle;
		
		private ComplexRegion groundTruth;
		public SensorData firstMeasurement, secondMeasurement;
		
		private static final int canvasWidth = 800;
		private static final int canvasHeight = 600;
		
		// constructing the instance, default gap, random angles
		public BenchmarkInstance(int id) {
			this(id, 20, Math.PI*Math.random(), new ComplexRegion(canvasWidth, canvasHeight));			
		}	
		
		public BenchmarkInstance(int id, double gap, double angleDifference){
			this(id, gap, angleDifference, new ComplexRegion(canvasWidth, canvasHeight));
		}
		
		public BenchmarkInstance(int id, double gap, double angleDifference, ComplexRegion gt){
			
			instanceID = id;			
			// Generate the complex region that will be used as the ground truth
			groundTruth = gt;		
			
			gapSize=gap; diffAngle = angleDifference;
			
			try {
												
				// Generate candidate measurements from the ground truth
				double firstGap = gap, firstAngle =  Math.PI * Math.random();
				firstMeasurement = new SensorData(groundTruth, firstGap, firstAngle, canvasWidth, canvasHeight);
				
				double secondGap = gap, secondAngle =  (firstAngle + angleDifference) % Math.PI;
				secondMeasurement = new SensorData(groundTruth, secondGap, secondAngle, canvasWidth, canvasHeight);												
											
			}
			catch (Exception e){
				System.err.println("ERROR: " + e.getMessage());
				System.exit(0);
			}
		}
		
		public ComplexRegion getGroundTruth(){
			return groundTruth;
		}
		
		public SensorData getFirstMeasurement(){
			return firstMeasurement;
		}
		
		public SensorData getSecondMeasurement(){
			return secondMeasurement;
		}
		
		public void drawInstance(){
			// visualize the ground truth 
			BufferedImage img = groundTruth.drawRegion(Color.LIGHT_GRAY);
			Graphics2D g2d = (Graphics2D) img.createGraphics();
			
			firstMeasurement.addIntervalsToGraphic(g2d, firstMeasurement.getPositiveIntervals(), false,
					Color.BLUE);
						
			secondMeasurement.addIntervalsToGraphic(g2d, secondMeasurement.getPositiveIntervals(), false,
					Color.RED);
						
			ShowDebugImage frame = new ShowDebugImage("Instance " + instanceID + " Ground Truth", img);
			frame.refresh(img);
			
			String filename = "experiments/images/instance-" + instanceID + "-groundTruth.png";
		
			System.out.println("saving image to " + filename);
			try {
				ImageIO.write(img, "png", new File(filename));
			} catch (IOException e) {
				System.err.println("failed to save image " + filename);
				e.printStackTrace();
			}
		}
		
		public double normalizeAndMatch(MatchSearchingStrategy search){
			double error = 0;
			
			SensorData firstNormalized = firstMeasurement.normalize();
			SensorData secondNormalized = secondMeasurement.normalize();
			
			AffineTransform[] at = firstNormalized.getATfromLongest(secondNormalized);
			if (at.length != 0){
				SensorData initialGuess;
				try {
					
					int searchSteps = 0;
					
					initialGuess = firstNormalized.applyAffineTransform(at);
					
					initialGuess.drawMeasurements(secondNormalized, "Instance-" + instanceID + "-InitialGuess");
					
					search.setFirstData(initialGuess);
					search.setSecondData(secondNormalized);
					
					// commence local search							
					//MatchSensorData_LS localSearch = new MatchSensorData_LS(initialGuess, secondNormalized, searchSteps);
					
					SensorData searchResult = search.initSearch();
					
					SensorData resultCopy = new SensorData(searchResult);
					
					/*int bufferRequired = resultCopy.testExtendMatch(secondNormalized);
					
					if (bufferRequired != -1){
						System.out.println("Required " + bufferRequired + "% buffer for resolving all conflicts" );															
					}*/
					
					// draw the results
					searchResult.drawMeasurements(secondNormalized, "Instance-" + instanceID + "-FinalMatch" );					
										
					
					// firstMeasurement.drawMeasurements(secondMeasurement, "Instance-" + instanceID + "-GroundTruth");
					
					System.out.println("Transformation drawn");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			else{
				System.out.println("Match not found.");
			}
			
			error = search.matchingError - firstMeasurement.countConflicts(secondMeasurement);
			
			return error;
		}
		
		/***
		 * Unit testing for SensorData.rotateMatch
		 * See if pure rotation can be recovered wit this method
		 * @param boolean drawMatch - True if we want to draw the outcome of the matching
		 * @return double error - the error after we tried to recover the rotation
		 */
		public double testRotateAndMatch(boolean drawMatch){			
			
			
			double error=0;
			
			// get centroid of the convex hull of the first measurement
			// same as SensorData.rotateMatch
			Point2D centroid = firstMeasurement.getPosIntCentroid();
			
			// set random angle
			double rotateAngle = Math.random()*Math.PI;
			
			//rotateAngle = 0.5*Math.PI;
			
			AffineTransform rotateTransform = new AffineTransform();
			rotateTransform.rotate(rotateAngle, centroid.getX(), centroid.getY());
			
			SensorData rotateFirst = firstMeasurement.applyAffineTransform(rotateTransform);
			
			Point2D rotateCentroid = rotateFirst.getPosIntCentroid();			
			
			// if the centroid don't match after rotate, we analyze what happened
			if (rotateCentroid.distance(centroid) > 1){
				System.err.println(String.format("centroid moved by %.2f. Analyzing...", rotateCentroid.distance(centroid)));
				System.err.println("old centroid: " + centroid);
				System.err.println("new centroid: " + rotateCentroid);
				
				// check if convex hull sizes agree
				int chSize = firstMeasurement.getAllPositivePoints().size();
				int rotateSize = rotateFirst.getAllPositivePoints().size();
				
				if (chSize==rotateSize){
					System.out.println("CH size agrees");
				}
				else{
					System.out.println("CH size disagree first:" + chSize + " second:"+rotateSize);
					System.out.println(firstMeasurement.getPositiveCoordinates().size() + " " + 
										rotateFirst.getPositiveCoordinates().size());
					
					// rotate back
					AffineTransform newRotate = new AffineTransform();
					newRotate.rotate(-rotateAngle, centroid.getX(), centroid.getY());
					SensorData recoverFirst = rotateFirst.applyAffineTransform(newRotate);
					
				}
				
				// firstMeasurement.drawTwoConvexHulls(rotateFirst, "CompareRotate");
					
				
				System.exit(0);
			}
			
			SensorData recoverFirst = rotateFirst.rotateMatch(secondMeasurement);
			
			Point2D recoverCentroid = recoverFirst.getPosIntCentroid();
			
			System.out.println("firstCentroid:" + centroid);
			System.out.println("secondCentroid:" + rotateCentroid);
			System.out.println("thirdCentroid:" + recoverCentroid);
															
			System.out.println(String.format("rotate by %.2f radians about (%.2f, %.2f)", rotateAngle, centroid.getX(), centroid.getY()));
			
			error = recoverFirst.countConflicts(secondMeasurement)-firstMeasurement.countConflicts(secondMeasurement);
			double firstError = firstMeasurement.countConflicts(secondMeasurement);
			
			if (drawMatch){
				firstMeasurement.drawMeasurements(secondMeasurement, "instance:" + instanceID + " originalError"+firstError);
				recoverFirst.drawMeasurements(secondMeasurement, "instance:" + instanceID + " recoverError"+error);
			}
			
			double sumFirstError = firstMeasurement.sumClosestPointError(secondMeasurement);
			double sumRecoverError = recoverFirst.sumClosestPointError(secondMeasurement);
			
			if (Math.abs(sumRecoverError - sumFirstError) < 1){
				System.out.println("Same closest point error:" + sumRecoverError + " " + sumFirstError);												
			}
			
			System.out.println(recoverFirst.countConflicts(secondMeasurement) + " conflicts, " + firstMeasurement.countConflicts(secondMeasurement) + " original conflicts");
			
			return error;
		}
}

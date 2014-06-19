/*  SensingExperiment.java
 *  The class that denote the experiment integrating
 *  two set of measurements
 * 
 */

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

// The class denoting an experiment for integrating sensors

public class SensingExperiment {
	
	private ComplexRegion groundTruth;
	private LayerGraph topoGroundTruth;
	
	SensorData firstMeasurement, secondMeasurement;
	
	private static final int canvasWidth = 800;
	private static final int canvasHeight = 600;
	
	
	
	// constructing the experiment
	public SensingExperiment() {

		// Generate the complex region that will be used as the ground truth
		groundTruth = new ComplexRegion(canvasWidth, canvasHeight);
		
		System.out.println("Ground Truth Generated");
		
		try {
			// Try to identify the topological structure of the gorund truth.
			topoGroundTruth = new LayerGraph(groundTruth);
			
			System.out.println("Layer Graph generated");
			
			// Generate candidate measurements from the ground truth
			double firstGap = 20, firstAngle = 2* Math.PI * Math.random();
			firstMeasurement = new SensorData(groundTruth, firstGap, firstAngle, canvasWidth, canvasHeight);
			
			double secondGap = 20, secondAngle = 2* Math.PI * Math.random();
			secondMeasurement = new SensorData(groundTruth, secondGap, secondAngle, canvasWidth, canvasHeight);
			
			System.out.println("SensorData created");			
			
			visualizeGroundTruth();
			
			System.out.println("Ground truth visualized");
			
			// normalize the measurements
			SensorData firstNormalized = firstMeasurement.normalize();
			SensorData secondNormalized = secondMeasurement.normalize();
			
			// firstNormalized.drawTwoConvexHulls(secondNormalized, "before");
			
			// find the suitable affine transform
			//TransformationRobustHelmert ht = firstNormalized.getHelmertInitial(secondNormalized);
			// at = firstNormalized.matchCentroid(secondNormalized);
			// AffineTransform at = firstNormalized.getATfromICP(secondNormalized);
			//AffineTransform at = firstNormalized.getMatchingTransformThroughCentroid(secondNormalized);
			AffineTransform[] at = firstNormalized.getATfromLongest(secondNormalized);
			if (at.length != 0){
				SensorData firstMatched = firstNormalized.applyAffineTransform(at);
				//SensorData firstMatched = firstNormalized.applyHelmertTransform(ht);
				System.out.println("Transformation found");
				firstMatched.drawMeasurements(secondNormalized, "FINAL-IMAGE");
				firstMatched.drawTwoConvexHulls(secondNormalized, "matchedConvexHull");
				
				// firstNormalized.drawTwoConvexHulls(secondNormalized, "after");
				System.out.println("Transformation drawn");
			}
			else{
				System.out.println("Match not found.");
			}
			
			Point2D firstCentroid = firstMeasurement.getConvexHullCentroid();
			Point2D secondCentroid = secondMeasurement.getConvexHullCentroid();
			
			System.err.println("firstCentroid: " + firstCentroid.getX() + " " + firstCentroid.getY());
			System.err.println("secondCentroid: " + secondCentroid.getX() + " " + secondCentroid.getY());
			System.out.println("dx = " + (secondCentroid.getX()-firstCentroid.getX()) + " dy = " + (secondCentroid.getY()-firstCentroid.getY()));
			
		}
		catch (Exception e){
			System.err.println("ERROR: " + e.getMessage());
			System.exit(0);
		}
						
	}
	
	public ComplexRegion getGroundTruth(){
		return groundTruth;
	}
	
	public LayerGraph getTopoGroundTruth(){
		return topoGroundTruth;
	}
	
	
	public void visualizeGroundTruth(){
		
		System.out.println("Visualizing Ground Truth");
		
		// visualize the ground truth 
		BufferedImage img = groundTruth.drawRegion(Color.LIGHT_GRAY);
		Graphics2D g2d = (Graphics2D) img.createGraphics();
		
		System.out.println("Adding things to graphics.");
		
		ShowDebugImage groundTruthFame = null;
		firstMeasurement.addIntervalsToGraphic(g2d, firstMeasurement.getPositiveIntervals(), false,
				Color.BLUE);
		
		System.out.println("First measurement added to graphics");
		
		secondMeasurement.addIntervalsToGraphic(g2d, secondMeasurement.getPositiveIntervals(), false,
				Color.RED);
		
		System.out.println("Second measurement added to graphics");	
		
		ShowDebugImage frame = new ShowDebugImage("Regions with intervals", img);
		frame.refresh(img);
		
		String filename = "experiments/" + Visualization.getCurrentTimeStamp() + "-groundTruth.png";
		
		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws Exception {
		// testDraw();
		SensingExperiment se = new SensingExperiment();
	}
	
}

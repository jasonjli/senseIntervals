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
				
		
		try {
			// Try to identify the topological structure of the gorund truth.
			topoGroundTruth = new LayerGraph(groundTruth);
			
			// Generate candidate measurements from the ground truth
			double firstGap = 20, firstAngle = Math.PI / 4;
			firstMeasurement = new SensorData(groundTruth, firstGap, firstAngle, canvasWidth, canvasHeight);
			
			double secondGap = 20, secondAngle = Math.PI / 8;
			secondMeasurement = new SensorData(groundTruth, secondGap, secondAngle, canvasWidth, canvasHeight);
			
			visualizeGroundTruth();
			
			// normalize the measurements
			SensorData firstNormalized = firstMeasurement.normalize();
			SensorData secondNormalized = secondMeasurement.normalize();
			
			// find the suitable affine transform
			AffineTransform at = firstNormalized.getMatchingTransform(secondNormalized);
			if (at!=null){
				SensorData firstMatched = firstNormalized.applyAffineTransform(at);
				
				drawMeasurements(firstMatched, secondNormalized);
			}
			else{
				System.out.println("Match not found.");
			}
			
		}
		catch (Exception e){
			System.err.println("ERROR: " + e.getMessage());
			System.exit(0);
		}
						
	}
	
	public void drawMeasurements(SensorData first, SensorData second){
		BufferedImage img = new BufferedImage(canvasWidth, canvasHeight,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, canvasWidth, canvasHeight);
		
		first.addIntervalsToGraphic(g2d, first.getPositiveIntervals(), false,
				Color.BLUE);
		second.addIntervalsToGraphic(g2d, second.getPositiveIntervals(), false,
				Color.RED);
		
		ShowDebugImage frame = new ShowDebugImage("New intervals", img);
		frame.refresh(img);
	}
	
	public void visualizeGroundTruth(){
		
		// visualize the ground truth 
		BufferedImage img = groundTruth.drawRegion(Color.LIGHT_GRAY);
		Graphics2D g2d = (Graphics2D) img.createGraphics();
		
		ShowDebugImage groundTruthFame = null;
		firstMeasurement.addIntervalsToGraphic(g2d, firstMeasurement.getPositiveIntervals(), false,
				Color.BLUE);
		secondMeasurement.addIntervalsToGraphic(g2d, secondMeasurement.getPositiveIntervals(), false,
				Color.RED);
		
		ShowDebugImage frame = new ShowDebugImage("Regions with intervals", img);
		frame.refresh(img);
	}
	
	public static void main(String[] args) throws Exception {
		// testDraw();
		SensingExperiment se = new SensingExperiment();
	}
	
}

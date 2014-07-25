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
	
	private SensorDataBenchmark benchmark;	
	private MatchSensorData_LS search; 
		
	
	// constructing the experiment
	public SensingExperiment() {
		benchmark = new SensorDataBenchmark(10);	
		search = new MatchSensorData_LS(100);
		
	}
	
	public SensingExperiment(SensorDataBenchmark b, MatchSensorData_LS s){
		benchmark = b;
		search = s;		
	}
		
	public void setSearchStrategy(MatchSensorData_LS s){
		search = s;
	}
	
	public void visualizeGroundTruth(){		
		benchmark.drawAll();
	}
	
	public void runLocalSearchExperiment(String outputFileName){
		
		search.resetPrevPositions();
		
		File logFile = new File(outputFileName);
		
		List<BenchmarkInstance> instanceList = benchmark.getInstances();
		
		double[] errors = new double[instanceList.size()];
		double sumErrors = 0;
		int i=0;
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
			writer.write(search.searchInfo + "\n");
			
			for (BenchmarkInstance bi : instanceList){
				double error = bi.normalizeAndMatch(search);
				errors[i++]=error;
				sumErrors += error;
				System.out.println("Instance " + bi.instanceID + " done, error=" + error + ".");
				
			}
			sumErrors = sumErrors / instanceList.size();
			writer.write("Average error = " + sumErrors + "\n");
			
			writer.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	
	public static void main(String[] args) throws Exception {
		
		String inputFileName = "experiments/benchmarks/100-g20-aRand.ser"; 
		String outputFileName = inputFileName + "." + Visualization.getCurrentTimeStamp() + ".log";
		// SensorDataBenchmark benchmark = new SensorDataBenchmark(100);
		
		
		//benchmark.saveBenchmark(fileName);
				
		SensorDataBenchmark newBenchmark = SensorDataBenchmark.readBenchmark(inputFileName);
		MatchSensorData_LS search = new MatchSensorData_LS(100);
		SensingExperiment se = new SensingExperiment(newBenchmark, search);
		
		se.runLocalSearchExperiment(outputFileName);
				
		
	}
	
}

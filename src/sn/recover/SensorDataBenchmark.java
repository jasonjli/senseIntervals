package sn.recover;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.util.regex.*;
import java.util.TreeMap;

import sn.regiondetect.ComplexRegion;

/*
 * A class represent a set of benchmark instances
 * Each benchmark instance is a complex region, its layergraph and two set of sensor measurements
 */
public class SensorDataBenchmark implements java.io.Serializable {
	

	
	// serial version uid
	private static final long serialVersionUID = 1L;

	// benchmark instances stored as a list
	private List<BenchmarkInstance> benchmarkInstances;
	
	// Constructor
	public SensorDataBenchmark(int benchmarkSize){
		benchmarkInstances = new ArrayList<BenchmarkInstance>();
		
		for (int i=0; i<benchmarkSize; i++){
			benchmarkInstances.add(new BenchmarkInstance(i));
			System.out.println("Instance " + i + " created.");
		}
	}
	
	public SensorDataBenchmark(int benchmarkSize, double gap, double angleDifference){
		benchmarkInstances = new ArrayList<BenchmarkInstance>();
		
		for (int i=0; i<benchmarkSize; i++){
			benchmarkInstances.add(new BenchmarkInstance(i, gap, angleDifference));
			System.out.println("Instance " + i + " created.");
		}
	}
	
	public SensorDataBenchmark(List<ComplexRegion> regionList, double gap, double angleDifference){
		benchmarkInstances = new ArrayList<BenchmarkInstance>();
		for (int i=0; i<regionList.size(); i++){
			benchmarkInstances.add(new BenchmarkInstance(i, gap, angleDifference, regionList.get(i)));
			System.out.println("Instance " + i + " created for gap=" + gap + ", angleDifference=" + angleDifference);
		}
	}
	
	public List<BenchmarkInstance> getInstances(){
		return benchmarkInstances;
	}

	public void addInstance(BenchmarkInstance bi){
		benchmarkInstances.add(bi);
	}
	
	// save to serialized form
	public void saveBenchmark(String fileName){
		try {
			FileOutputStream fileOut = new FileOutputStream(fileName);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this);
			out.close();
			fileOut.close();
			System.out.println("Benchmarked saved to " + fileName);
			
		} catch(IOException i){
			i.printStackTrace();
		}
		
	}
	
	public void drawGivenInstance(int id){
		benchmarkInstances.get(id).drawInstance("Instance-" + id);
	}
	
	public void drawAll(){
		for (BenchmarkInstance bi : benchmarkInstances){
			bi.drawInstance("Instance-" + bi.instanceID);
		}
	}
	
	public static SensorDataBenchmark readBenchmark(String fileName){
		SensorDataBenchmark benchmark = null;
		
		try {
			FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            benchmark = (SensorDataBenchmark) in.readObject();
            in.close();
            fileIn.close();
		}catch (Exception e){
			e.printStackTrace();
		}
		
		return benchmark;
	}
	
	
	/***
	 * 
	 * @param n = number of instances to be generated for each gap / angle pair.
	 */
	public static void generateFixedBenchmarks(int n){
		ArrayList<ComplexRegion> regionList = new ArrayList<ComplexRegion>();
		final int canvasWidth = 800;
		final int canvasHeight = 600;
		
		long startTime = System.currentTimeMillis();
		
		for (int i=0; i<n; i++){
			regionList.add(new ComplexRegion(canvasWidth, canvasHeight));
		}
		System.out.println("Regions Created");
		
		int gapCount=0, factorCount=0;
		
		for (double gap = 5; gap < 100; gap = gap*2){						
			factorCount=0;
			for (int factor = 5; factor<=50; factor+=5){
				double angleDiff = (double)factor / 100 * Math.PI;
				String outputFile = "experiments/benchmarks/n"+ n + "-g"+gap+"-a"+(double)factor/100+"PI.ser";
				
				SensorDataBenchmark b = new SensorDataBenchmark(regionList, gap, angleDiff);
				b.saveBenchmark(outputFile);
				System.out.println(outputFile + " written.");
				factorCount++;
			}			
			gapCount++;
		}
		
		long endTime = System.currentTimeMillis();
		
		System.out.println(n*gapCount*factorCount + " benchmarks generated in " + (endTime - startTime) + " milliseconds");
	}
	
	public static void drawGroundTruth(int n){
		for (double gap = 5; gap < 100; gap = gap*2){
			for (int factor = 5; factor<=50; factor+=5){
				String inputFile = "experiments/benchmarks/n"+ n + "-g"+gap+"-a"+ (double)factor/100+"PI.ser";
				SensorDataBenchmark benchmark = readBenchmark(inputFile);
				for (int i=0; i<benchmark.getInstances().size(); i++){
					BenchmarkInstance bi = benchmark.getInstances().get(i);
					if (bi.getConflictCount()==0){
						bi.drawInstance("G" + gap + "-A"+(double)factor/100 + "PI-i"+i);
						break;
					}
				}
			}
		}
	}
	
	
	/**
	 * Given  generated instances in experiments/benchmarks/ folder
	 * sovle them 
	 * @param n = number of instances to be generated for each gap / angle pair
	 */
	
	public static void solveFixedBenchmarks(int n, int searchStrategy){
		long startTime = System.currentTimeMillis();
		
		int gapCount=0, factorCount=0, searchSteps = 100;
		MatchSensorData_LS search = new MatchSensorData_LS(searchSteps,searchStrategy);
		int benchmarkCount = 0;
		
		for (double gap = 5; gap < 100; gap = gap*2){						
			factorCount=0;
			for (int factor = 5; factor<=50; factor+=5){
				String inputFile = "experiments/benchmarks/n"+ n + "-g"+gap+"-a"+(double)factor/100+"PI.ser";
				String outputFile = "experiments/500Instances_g5-80_f5-50/data/n"+ n + "-g"+gap+"-a"+(double)factor/100+"PI.strategy"+searchStrategy+".log";
				File logFile = new File(outputFile);
				
				if (logFile.exists() && !logFile.isDirectory()) continue;
				
				SensorDataBenchmark benchmark = readBenchmark(inputFile);

				double errors[] = new double[benchmark.getInstances().size()];
				double sumError = 0;
				for (int i=0; i<benchmark.getInstances().size(); i++){					
					BenchmarkInstance bi = benchmark.getInstances().get(i);
					bi.drawInstance("G" + gap + "-A"+(double)factor/100 + "PI-i"+i);
					MatchSensorData_LS newSearch = new MatchSensorData_LS(searchSteps,searchStrategy);
					SearchResult result = bi.normalizeAndMatch(newSearch);
					errors[i] = result.getSearchError();
					sumError += errors[i];
					System.out.println("error " + errors[i]);
					benchmarkCount++;
					
					 break;
				}
				
				
				try {
					BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
					writer.write(search.searchInfo + "\n");
					double avg = sumError / benchmark.getInstances().size();
					writer.write("Average error = " + avg + "\n");
					System.out.println("Average error = " + avg);
					writer.close();
				}catch (Exception e){
					e.printStackTrace();
				}
				System.out.println(inputFile + " solved.");
				factorCount++;
				break;
			}			
			
			gapCount++;
			//break;
		}
		
		
		
		long endTime = System.currentTimeMillis();
		System.out.println(benchmarkCount + " benchmarks solved in " + (endTime - startTime) + " milliseconds");

	}
	
	
	public static void readSolvedFixedBenchmarks(int n){
		long startTime = System.currentTimeMillis();
		
		int gapCount=0, factorCount=0;
		
		TreeMap<Integer,Double> gapMean = new TreeMap<Integer,Double>();
		TreeMap<Integer,Double> factorMean = new TreeMap<Integer,Double>();
		
		for (int gap = 5; gap < 100; gap = gap*2){						
			factorCount=0;
			gapMean.put(gap, 0.0);
			
			for (int factor = 5; factor<=50; factor+=5){
				
				if (gap==5) factorMean.put(factor,0.0);
				
				String inputFile = "experiments/500Instances_g5-80_f5-50/data/n"+ n + "-g"+gap+".0-a"+(double)factor/100+"PI.lsMin.log";
				
				try (BufferedReader br = new BufferedReader(new FileReader(inputFile))){
					String line = br.readLine();
					
					while (line != null){
						
						// parse line
						Pattern p = Pattern.compile("Average error = (\\d+.\\d)");
						Matcher m = p.matcher(line);
						
						if (m.find()){
							double angle = (double) factor / 100;
							System.out.print(String.format("%d %.2fPI ", gap, angle));
							double score = Double.parseDouble(m.group(1));
							System.out.println(score);
							gapMean.put(gap, gapMean.get(gap)+score);
							factorMean.put(factor, factorMean.get(factor)+score);
						}
						
						// read the next line
						line = br.readLine();
					}
					
				}catch (Exception e){
					e.printStackTrace();
				} // end catch
				
				factorCount++;
			} // end for factor
			gapCount++;
		} // end for gap
		
		Iterator<Integer> it = gapMean.keySet().iterator();
		System.out.println("gap meanError");
		while (it.hasNext()){
			Integer key = it.next();
			System.out.println(String.format("%d %.2f",  key, gapMean.get(key)/gapCount));
		}
		
		it = factorMean.keySet().iterator();
		System.out.println("angle(radians) meanError");
		while (it.hasNext()){
			Integer key = it.next();
			System.out.println(String.format("%.2f %.2f",  (double)key/100*Math.PI, factorMean.get(key)/factorCount));
		}
		
		long endTime = System.currentTimeMillis();
		System.out.println(n*gapCount*factorCount + " benchmark results parsed in " + (endTime - startTime) + " milliseconds");
				
	}
	
	/***
	 * test only rotate the generated benchmarks
	 * @param n
	 */
	
	public static void testRotateFixedBenchmarks(int n){
		long startTime = System.currentTimeMillis();
		int gapCount=0, factorCount=0;
		String logString = "";
		double sumError = 0;
		int count = 0;
		for (double gap = 5; gap < 100; gap = gap*2){						
			factorCount=0;
			for (int factor = 0; factor<=50; factor+=5){
				String inputFile = "experiments/benchmarks/n"+ n + "-g"+gap+"-a"+(double)factor/100+"PI.ser";
				SensorDataBenchmark benchmark = readBenchmark(inputFile);
				for (int i=0; i<benchmark.getInstances().size(); i++){
					BenchmarkInstance bi = benchmark.getInstances().get(i);
					double rotateError = bi.testRotateAndMatch(true);
					String errorString = gap + ", " + factor + ", " + String.format("%.2f", rotateError);
					logString = logString + errorString + "\n";
					System.out.println(errorString);
					sumError+=rotateError; count++;
					
					// break;
				}	
				factorCount++;
				//break;
			}
			gapCount++;
			//break;
		}
		System.out.println(String.format("Avg error %.2f", sumError/(double)count));
		
		long endTime = System.currentTimeMillis();
		System.out.println(n*gapCount*factorCount + " benchmarks tested in " + (endTime - startTime) + " milliseconds");
			
	}
	
	public static void main(String[] args) throws Exception {
		
		//generateFixedBenchmarks(10);
	    solveFixedBenchmarks(10,3); 
		//testRotateFixedBenchmarks(10);
		//readSolvedFixedBenchmarks(10);
		//drawGroundTruth(10);
	}
}

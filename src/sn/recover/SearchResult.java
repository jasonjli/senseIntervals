package sn.recover;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.geom.*;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.HashSet;



public class SearchResult implements java.io.Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public BenchmarkInstance originalInstance;
	public SensorData matchedFirstData;
	public SensorData matchedSecondData;
	
	public SearchResult(BenchmarkInstance bi, SensorData first, SensorData second){
		originalInstance = bi;
		matchedFirstData = first;
		matchedSecondData = second;
	}
	
	public double getSearchError(){				
		return matchedFirstData.countConflicts(matchedSecondData) - originalInstance.firstMeasurement.countConflicts(originalInstance.secondMeasurement);		
	}
	
	public void saveResult(String fileName){
		try {
			FileOutputStream fileOut = new FileOutputStream(fileName);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this);
			out.close();
			fileOut.close();
			System.out.println("Search results saved to " + fileName);
			
		} catch(IOException i){
			i.printStackTrace();
		}		
	}
	
	public static SearchResult readResult(String fileName){
		SearchResult result = null;
		
		try {
			FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            result = (SearchResult) in.readObject();
            in.close();
            fileIn.close();
		}catch (Exception e){
			e.printStackTrace();
		}
		
		return result;
	}
	
	public void getLayerGraphComponents(){
		
		List<SensorInterval> firstIntervals = matchedFirstData.getPositiveIntervals();
		List<SensorInterval> secondIntervals = matchedSecondData.getPositiveIntervals();
		
		HashMap <SensorInterval, Integer> icMap = new HashMap<SensorInterval,Integer>();
		
		
		
		for (int i=0; i<firstIntervals.size(); i++){
			icMap.put(firstIntervals.get(i), i);
		}
		for (int j=0; j<secondIntervals.size(); j++){
			icMap.put(secondIntervals.get(j), firstIntervals.size()+j);
		}
		
		// first collect all positive intersects.
		for (int i=0; i<firstIntervals.size(); i++){
			SensorInterval first = firstIntervals.get(i);
			for (int j=0; j<secondIntervals.size(); j++){
				SensorInterval second = secondIntervals.get(i);
				if (first.intersects(second)){
					if (icMap.get(first) < icMap.get(second)){
						icMap.put(second, icMap.get(first));
					}
					else{
						icMap.put(first, icMap.get(second));
					}
				}
			}
		}
		
		// then collect all negative intersect
		HashMap <SensorInterval, Integer> icNegMap = new HashMap<SensorInterval,Integer>();
		List<SensorInterval> firstNegIntervals = matchedFirstData.getNegativeIntervals();
		List<SensorInterval> secondNegIntervals = matchedSecondData.getNegativeIntervals();
		
		for (int i=0; i<firstNegIntervals.size(); i++){
			icNegMap.put(firstNegIntervals.get(i), i+firstIntervals.size()+secondIntervals.size());
		}
		for (int j=0; j<secondNegIntervals.size(); j++){
			icNegMap.put(secondNegIntervals.get(j), firstNegIntervals.size()+j+firstIntervals.size()+secondIntervals.size());
		}
		
				for (int i=0; i<firstNegIntervals.size(); i++){
					SensorInterval first = firstNegIntervals.get(i);
					for (int j=0; j<secondNegIntervals.size(); j++){
						SensorInterval second = secondNegIntervals.get(i);
						if (first.intersects(second)){
							if (icNegMap.get(first) < icNegMap.get(second)){
								icNegMap.put(second, icNegMap.get(first));
							}
							else{
								icNegMap.put(first, icNegMap.get(second));
							}
						}
					}
				}
		
				
		// now icMap should contain all the interval-component pair, 
		// create a reverse component-intervals pair
		TreeMap <Integer, HashSet<SensorInterval> > ciMap = new TreeMap <Integer, HashSet<SensorInterval> >();
		Iterator<SensorInterval> keySetIterator = icMap.keySet().iterator();
		while( keySetIterator.hasNext() ){
			SensorInterval keyInterval = keySetIterator.next();
			int component = icMap.get(keyInterval);
			
			HashSet<SensorInterval> set = ciMap.get(component);
			if (set == null){
				set = new HashSet<SensorInterval>();
			}
			set.add(keyInterval);
			ciMap.put(component, set);
		}
		
		TreeMap <Integer, HashSet<SensorInterval> > ciNegMap = new TreeMap <Integer, HashSet<SensorInterval> >();
		keySetIterator=icNegMap.keySet().iterator();
		while ( keySetIterator.hasNext() ){
			SensorInterval keyNegInterval = keySetIterator.next();
			int negComponent = icMap.get(keyNegInterval);
			HashSet<SensorInterval> set = ciNegMap.get(negComponent);
			if (set == null){
				set = new HashSet<SensorInterval>();
			}
			set.add(keyNegInterval);
			ciNegMap.put(negComponent, set);
		}
		
		// normalize key (component id)
		Iterator<Integer> intKeyIterator = ciMap.keySet().iterator();
		int i=0;
		int intervalCount = 0;
		while (intKeyIterator.hasNext()){
			int keyComponent = intKeyIterator.next();
			if (keyComponent != i){
				HashSet<SensorInterval> intervals = ciMap.get(keyComponent);
				ciMap.remove(keyComponent);
				ciMap.put(i, intervals);
			}
			
			int componentIntervalCount = ciMap.get(i).size();
			intervalCount += componentIntervalCount;
			System.out.println("Component " + i + ", " + componentIntervalCount + " intervals.");			
			i++;
		}
		intKeyIterator = ciNegMap.keySet().iterator();
		int negCount = 0;
		while (intKeyIterator.hasNext()){
			int keyNegComponent = intKeyIterator.next();
			if (keyNegComponent != i){
				HashSet<SensorInterval> intervals = ciNegMap.get(keyNegComponent);
				ciNegMap.remove(keyNegComponent);
				ciNegMap.put(i,intervals);
			}
			
			int componentNegIntervalCount = ciNegMap.get(i).size();
		}
					
		// checksum make sure every positive intervals are accounted for
		if (intervalCount != firstIntervals.size() + secondIntervals.size()){
			System.err.println("ERROR: Checksum failed in SearchResult.java!");
			System.exit(0);
		}
						
	}
	
	/***
	 * 
	 * @param setA set of intervals belong to the same component A
	 * @param setB set of intervals belong to the same component B 
	 * @return 
	 * 1 : if B PP A
	 * -1: if A PP B
	 * 0 : all other cases
	 */
	public int getRelations(HashSet<SensorInterval> setA, HashSet<SensorInterval> setB){
		Point2D[] ptsA = new Point2D[setA.size()*2];
		Point2D[] ptsAB = new Point2D[setA.size()*2+setB.size()*2];
		int i=0;
		for (SensorInterval si: setA){
			Point2D start = si.getStart();
			Point2D end = si.getEnd();
			ptsAB[i] = start;
			ptsA[i++] = start;
			ptsAB[i] = end;
			ptsA[i++] = end;						
		}
				
		Point2D[] ptsB = new Point2D[setB.size()*2];	
		i=0;
		for (SensorInterval si: setB){
			Point2D start = si.getStart();
			Point2D end = si.getEnd();
			ptsAB[i+setA.size()*2] = start;
			ptsB[i++] = start;
			ptsAB[i+setA.size()*2] = end;
			ptsB[i++] = end;	
		}
		
		// get convex hull of set A and set B
		GrahamScan aScan = new GrahamScan(ptsA);
		GrahamScan bScan = new GrahamScan(ptsB);
		GrahamScan abScan = new GrahamScan(ptsAB);
		
		List<Point2D> aHull = (List<Point2D>)aScan.hull();
		List<Point2D> bHull = (List<Point2D>)bScan.hull();
		List<Point2D> abHull = (List<Point2D>)abScan.hull();
		
		// now determine the relationship between A and B. 
		if (SensorData.sumClosestDistance(aHull, abHull) < 0.01){
			// set B is in set A
			return 1;
		}
		else if (SensorData.sumClosestDistance(bHull, abHull) < 0.01){
			// set A is in set B
			return -1;
		}
		else{
			return 0;
		}
	}
}

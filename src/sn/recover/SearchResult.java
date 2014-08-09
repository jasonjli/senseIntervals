package sn.recover;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

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
}

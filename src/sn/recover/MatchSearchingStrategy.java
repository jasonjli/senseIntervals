package sn.recover;

/*
 * Strategy for matching two set of measurements
 */
public class MatchSearchingStrategy implements Cloneable {
	// sensor data. The task is match firstData to secondData
	protected SensorData firstData, secondData;
	protected double matchingError=Double.MAX_VALUE;
	
	protected String searchInfo = "Generic search";
	
	public MatchSearchingStrategy(){
		;
	}
	
	public MatchSearchingStrategy(SensorData a, SensorData b){
		firstData = a; secondData = b;
	}
	
	public void setFirstData(SensorData a){
		firstData = a;
	}
	
	public void setSecondData(SensorData b){
		secondData = b;
	}
	
	public SensorData getFirstData(){
		return firstData;
	}
	
	public SensorData getSecondData(){
		return secondData;
	}
	
	public SensorData initSearch(){
		System.err.println("ERROR - implemented search not found!");
		return null;
	}
}

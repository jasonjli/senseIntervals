package sn.regiondetect;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

//A simple region or hole
public class Region{
	private Region _next;
	private Region _prev;
	private int _layer;
	private int _radius;
	private Point _centre;
	private boolean _exceedBoundary;
	private int[] _xArray;
	private int[] _yArray;
	private List<Point> _pts;
	private Path2D _path;
	
	public Region(int[] x, int[] y, int radius, int layer ,Point centre, boolean exceed){

		_next = null;
		_prev = null;
		_pts = new ArrayList<Point>(); 
		_radius = radius;
		_layer = layer;
		_centre = centre;
		_exceedBoundary = exceed;
		_xArray = x;
		_yArray = y;
		for(int i = 0; i < x.length; i++){
			_pts.add(new Point(x[i],y[i]));
		}
		_path = GeomUtil.getRoundedGeneralPathFromPoints(_pts);
	}
	
	public Region(int[] x, int[] y, int layer){

		_next = null;
		_prev = null;
		_pts = new ArrayList<Point>(); 
		_radius = -1;
		_layer = layer;
		_centre = null;
		_exceedBoundary = false;
		_xArray = x;
		_yArray = y;
		for(int i = 0; i < x.length; i++){
			_pts.add(new Point(x[i],y[i]));
		}
		_path = GeomUtil.getRoundedGeneralPathFromPoints(_pts);
	}
	
	
	public boolean isHole(){
		if(_layer%2 == 0)
			return false;
		else
			return true;
	}
	
	public boolean isHead(){
		if(_prev == null)
			return true;
		else 
			return false;
	}
	
	public boolean isExceedBound(){
		return _exceedBoundary;
	}
	
	public Region getPrev(){
		return _prev;
	}
	
	public Region getNext(){
		return _next;
	}
	
	public int getRadius(){
		return _radius;
	}
	
	public Point getCenter(){
		return _centre;
	}
	
	public int getLayer(){
		return _layer;
	}
	
	public void setPrev(Region prev){
		_prev = prev;
	}
	
	public void setNext(Region next){
		_next = next;
	}
	
	public Path2D getShape(){
		return _path;
	}
	
	@Override
	public String toString(){
		String regionInfo = "";
		
		regionInfo += ("nPoints " + _xArray.length +System.getProperty("line.separator"));
		regionInfo += ("layer "+_layer+System.getProperty("line.separator"));
		
		for(int i = 0; i < _xArray.length; i++){
			regionInfo += (_xArray[i]+","+_yArray[i]+System.getProperty("line.separator"));
		}
			
		return regionInfo;
	}
	
}

package sn.regiondetect;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public class Region{
	protected Region _next;
	protected Region _prev;
	protected int _layer;
	protected int _radius;
	protected Point _centre;
	protected boolean _exceedBoundary;
	protected int[] _xArray;
	protected int[] _yArray;
	protected List<Point> _pts;
	protected Path2D _path;
	
	public Region(int[] x, int[] y, int nPts, int radius, int layer ,Point centre, boolean exceed){
		//super(x, y, nPts);
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
		
		regionInfo += ("layer="+_layer+"\n");
		regionInfo += ("Bounding Points\n");
		for(int i = 0; i < _xArray.length; i++){
			regionInfo += ("["+_xArray[i]+","+_yArray[i]+"]\n");
		}
			
		return regionInfo;
	}
	
}

package sn.recover;

import java.awt.Point;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

import sn.regiondetect.GeomUtil;

//A component in the region that does not connect/touch with any other components
public class ComponentInstance {
	
	//Sub-components that contain in this component
	private List<ComponentInstance> _subComponents;
	
	//Component that directly contains this component
	private ComponentInstance _containerComponent;
	
	//unique id for the node in the tree
	private int _id;
	
	//level = 0 means this component is the outer unbounded component
	//odd level indicates the component is a solid component
	//even level indicates the component is a hollow within or outside solid components
	private int _level;
	
	//Extreme points of the shape of the component
	private List<Point> _pts;
	
	//Rounded path of the component
	private Path2D _path;
	
	//Label of the component, used for calculating tree edit distance
	private String _label;
	
	//Order number assigned when searching the tree
	private int _traversalNumber;
	
	public ComponentInstance(int id){
		_id = id;
		_pts = null;
		_containerComponent = null;
		_subComponents = new ArrayList<ComponentInstance>();
		_level = -1;
		_path = null;
	}
	
	public ComponentInstance(int id, int level){
		_id = id;
		_pts = null;
		_containerComponent = null;
		_subComponents = new ArrayList<ComponentInstance>();
		_level = level;
		_path = null;
	}
	
	public ComponentInstance(int id, List<Point> pts){
		_id = id;
		_pts = pts;
		_containerComponent = null;
		_subComponents = new ArrayList<ComponentInstance>();
		_level = -1; //level has not been set
		_path = GeomUtil.getRoundedGeneralPathFromPoints(_pts);
	}
	
	public ComponentInstance(int id, Path2D path){
		_id = id;
		_pts = null;
		_containerComponent = null;
		_subComponents = new ArrayList<ComponentInstance>();
		_level = -1; //level has not been set
		_path = path;
	}
	
	public ComponentInstance(int id, List<Point> pts, int level){
		_id = id;
		_pts = null;
		_containerComponent = null;
		_subComponents = new ArrayList<ComponentInstance>();
		_level = level;
		_path = GeomUtil.getRoundedGeneralPathFromPoints(_pts);
	}
	
	public ComponentInstance(int id, Path2D path, int level){
		_id = id;
		_pts = null;
		_containerComponent = null;
		_subComponents = new ArrayList<ComponentInstance>();
		_level = level;
		_path = path;
	}
	
	//Functions for getting membership variables*************
	public List<ComponentInstance> getSubComponents(){   //**
		return _subComponents;                           //**
	} 													 //**	
														 //**
	public ComponentInstance getContainerComponent(){    //**
		return _containerComponent;		             	 //**
	}													 //**
														 //**
	public Path2D getPath(){						     //**
		return _path;									 //**		
	}													 //**	
														 //**
	public int getLevel(){								 //**
		return _level;									 //**		
	}													 //**	
	public String getLabel(){							 //**
		return _label;									 //**		
	}													 //**
	public int getTraversalNumber(){					 //**
		return _traversalNumber;						 //**
	}													 //** 
	public int getID(){									 //**
		return _id;										 //**
	}													 //** 
	//*******************************************************
	
	public void setID(int id){
		_id = id;
	}

	public void setLabel(String label){
		_label = label;
	}
	
	public void setTraversalNumber(int number){
		 _traversalNumber = number;
	}
	
	public void setSubComponents(List<ComponentInstance> subComponents){
		 _subComponents = subComponents;
	}
	
	public void setPts(List<Point> pts){
		_pts = pts;
	}
	
	public void setPath(Path2D path){
		_path = (Path2D)path.clone();
	}
	
	/**
	 * Add a component into sub-component list 
	 * @param component
	 */
	public void addSubComponent(ComponentInstance component){
		_subComponents.add(component);
	}
	
	
	/**
	 * Set the container component
	 * @param component
	 */
	public void setContainerComponent(ComponentInstance component){
		_containerComponent = component;
	} 
	
	/**
	 * Set level of a component
	 * @param level
	 */
	public void setLevel(int level){
		_level = level;
	}
	

	
}

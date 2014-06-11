package sn.recover;


// the interface for a LayerGraph node
public interface LayerGraphNode {
	
	/*
	 * Return the children of the given node inside an array
	 * 
	 * @return an ordered array containing the children of this node. 
	 * THIS VALUE MUST NOT BE NULL! RETURN AN ARRAY OF LENGTH 0 to SPECIFY THAT THERE ARE NO CHILDREN
	 * 
	 */
	public LayerGraphNode[] LayerGraphGetChildren();
	
	/*
	 * Get the ID of the node
	 * 
	 * @return the node's ID
	 * 
	 */
	public Object LayerGraphGetValue();
	
	/*
	 * Return the color for drawing this node
	 * Black for positive component, red for negative component
	 * 
	 * @return the color to be used for visualizing this node
	 */
	public java.awt.Color LayerGraphGetColor();
	
}

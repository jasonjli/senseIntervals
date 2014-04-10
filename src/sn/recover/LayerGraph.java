package sn.recover;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import sn.regiondetect.ComplexRegion;
import sn.regiondetect.Region;

public class LayerGraph {
	private static int INSERTION_COST = 1;
	private static int DELETION_COST = 1;
	private static int RENAMING_COST = 1;

	private ComplexRegion _complexRegion;
	private ComponentInstance _unboundedComponent;
	private List<ComponentInstance> _componentList;

	public LayerGraph(ComplexRegion complexRegion) throws Exception {
		// Initialize the root component, i.e. the canvas
		_unboundedComponent = new ComponentInstance(0);
		_complexRegion = complexRegion;

		_componentList = getRealComponents();
		setLayerInfo();
	}

	/**
	 * Extract separated components from the raw generated region data
	 * 
	 * @param complexRegion
	 * @return Layered components
	 * @throws Exception
	 */
	public List<ComponentInstance> getRealComponents() throws Exception {
		List<ComponentInstance> components = new ArrayList<ComponentInstance>();

		Region[] rawReigons = _complexRegion.getComplexRegion();
		Area regionCanvas = new Area();

		// combine raw regions into one area
		for (Region r : rawReigons) {
			Path2D outline = r.getShape();
			Area regionArea = new Area(outline);

			// if the region is a hole, then subtract it from the canvas
			// else add the region to the canvas
			if (r.isHole()) {
				regionCanvas.subtract(regionArea);
			} else {
				regionCanvas.add(regionArea);
			}
		}

		PathIterator pathIter = regionCanvas.getPathIterator(null);
		Path2D tempPath = new Path2D.Double();
		tempPath.setWindingRule(PathIterator.WIND_EVEN_ODD);
		// Double array with length 6 needed by iterator
		double[] coords = new double[6];
		while (!pathIter.isDone()) {
			int type = pathIter.currentSegment(coords);
			switch (type) {
			case PathIterator.SEG_LINETO: {
				tempPath.lineTo(coords[0], coords[1]);
				// System.out.println("type: LINETO " + intersectPt.toString());
				break;
			}

			case PathIterator.SEG_MOVETO: {
				tempPath.moveTo(coords[0], coords[1]);
				// System.out.println("type: MOVETO " + intersectPt.toString());
				break;
			}

			case PathIterator.SEG_CUBICTO: {
				tempPath.curveTo(coords[0], coords[1], coords[2], coords[3],
						coords[4], coords[5]);
				// System.out.println("type: CUBICTO " +
				// intersectPt.toString());
				break;
			}

			// while the path is closed, save to component list and start a new
			// path
			case PathIterator.SEG_CLOSE: {
				tempPath.closePath();
				components.add(new ComponentInstance((Path2D) tempPath));
				tempPath = new Path2D.Double();
				tempPath.setWindingRule(PathIterator.WIND_EVEN_ODD);
				// System.out.println("type: CLOSE ");
				break;
			}
			default: {
				throw new Exception("Unsupported PathIterator segment type: "
						+ type);
			}
			}
			pathIter.next();
		}

		return components;
	}

	/**
	 * set the layer information for all components
	 */
	public void setLayerInfo() {
		for (int i = 0; i < _componentList.size(); i++) {
			ComponentInstance c1 = _componentList.get(i);

			// set the component to level 1
			c1.setLevel(1);

			for (int j = 0; j < _componentList.size(); j++) {
				if (i == j) {
					continue;
				}

				ComponentInstance c2 = _componentList.get(j);
				Area area1 = new Area(c1.getPath());
				Area area2 = new Area(c2.getPath());

				// if component 2 entirely contains component 1
				if (contain(area2, area1)) {
					// if a container of component 1 has been found previously
					if (c1.getContainerComponent() == null) {

						// set component 2 as the container of component 1
						c1.setContainerComponent(c2);
						// add 1 to component 1's level
						c1.setLevel(c1.getLevel() + 1);
					} else {
						Area prevContainer = new Area(c1
								.getContainerComponent().getPath());

						// if component 2 entirely contains component 1's
						// previous container
						if (contain(prevContainer, area2)) {
							// set component 2 as the container of component 1
							c1.setContainerComponent(c2);
							// add 1 to component 1's level
							c1.setLevel(c1.getLevel() + 1);
						}

						else {
							c1.setLevel(c1.getLevel() + 1);
						}
					}
				}
			}
		}

		// set sub-components for a list of component
		for (int i = 0; i < _componentList.size(); i++) {
			ComponentInstance component = _componentList.get(i);
			ComponentInstance container = component.getContainerComponent();
			if (container != null) {
				container.addSubComponent(component);
			}

			else {
				component.setContainerComponent(_unboundedComponent);
				_unboundedComponent.addSubComponent(component);
			}
		}

	}

	/**
	 * Draw a component and all its sub-components
	 * 
	 * @param component
	 * @param img
	 * @param c
	 * @param layerInfo
	 */
	public void drawComponents(ComponentInstance component, BufferedImage img,
			Color c, boolean layerInfo) {

		Graphics2D g2d = (Graphics2D) img.createGraphics();

		g2d.setColor(c);
		if (!isHole(component.getLevel())) {

			// draw layer information as required
			if (layerInfo) {
				Area area = new Area(component.getPath());
				double x = area.getBounds().getX();
				double y = area.getBounds().getY();

				// inverse color
				int r = c.getRed();
				int g = c.getGreen();
				int b = c.getBlue();
				int newR = 255 - r;
				int newG = 255 - g;
				int newB = 255 - b;
				Color newC = new Color(newR, newG, newB);

				g2d.setColor(newC);

				g2d.drawString("layer = " + component.getLevel(), (int) x,
						(int) y);

				g2d.setColor(c);
			}
			g2d.draw(component.getPath());
		}

		else if (component.getLevel() > 0) {
			g2d.setColor(Color.GREEN);
			// draw layer information as required
			if (layerInfo) {
				Area area = new Area(component.getPath());
				double centreX = area.getBounds().getCenterX();
				double centreY = area.getBounds().getCenterY();

				g2d.setColor(Color.BLACK);
				g2d.drawString("layer = " + component.getLevel(),
						(int) centreX, (int) centreY);
				g2d.setColor(Color.GREEN);
			}
			g2d.draw(component.getPath());
			g2d.setColor(c);
		}

		for (ComponentInstance subComponent : component.getSubComponents()) {
			drawComponents(subComponent, img, c, layerInfo);
		}

	}

	/**
	 * Use left-to-right post-order to search a tree
	 * 
	 * @param root
	 * @return
	 */
	public static void traversalLeftToRightPostOrder(
			ComponentInstance root, List<ComponentInstance> nodeList) {
		
		// recursion
		for (ComponentInstance node : root.getSubComponents()) {
			traversalLeftToRightPostOrder(node, nodeList);
		}
		
		nodeList.add(root);
		root.setTraversalNumber(nodeList.size());
		
		return;
	}

	/**
	 * Use breadth first to search a tree
	 * 
	 * @param root
	 * @return a list of node in left-to-right order
	 */
	public static List<ComponentInstance> traversalBreadthFirst(ComponentInstance root) {
		// List for storing ordered nodes
		List<ComponentInstance> nodeList = new ArrayList<ComponentInstance>();

		// List for controlling a left-to-right searching order
		List<ComponentInstance> processingList = new ArrayList<ComponentInstance>();

		processingList.add(root);// add root to processing list

		// process until no node in processing list
		while (!processingList.isEmpty()) {

			// get the first node in processing list
			ComponentInstance currentNode = processingList.get(0);

			// add the current node to node list
			nodeList.add(currentNode);

			// remove the processed node
			processingList.remove(0);

			// add all current node's child nodes into processing list
			for (ComponentInstance node : currentNode.getSubComponents()) {
				processingList.add(node);
			}
		}

		return nodeList;
	}

	/**
	 * Calculate unordered tree edit distance <br/>
	 * Implementation of algorithm in 'On the editing distance between unordered
	 * labeled trees' K. Zhang et.al
	 * 
	 * @param nodeList
	 *            node list of a tree retrieved by breadth-first search
	 * @return
	 */
	public static int unodreredTreeEditDistance(List<ComponentInstance> nodeList1,
			List<ComponentInstance> nodeList2) {
		int len1 = nodeList1.size();
		int len2 = nodeList2.size();
		int[][] distMatrix = new int[len1 + 1][len2 + 1];
		distMatrix[0][0] = 0;

		// Initialize the first row of the matrix
		for (int i = 1; i < len1+1; i++) {
			distMatrix[i][0] = distMatrix[i - 1][0] + DELETION_COST;
		}

		// Initialize the first column of the matrix
		for (int i = 1; i < len2+1; i++) {
			int sum = 0;
			List<ComponentInstance> childNodes = nodeList2.get(i-1)
					.getSubComponents();
			for (ComponentInstance child : childNodes) {
				sum += distMatrix[0][child.getTraversalNumber()];
			}
			
			distMatrix[0][i] =  INSERTION_COST + sum;
		}

		for (int i = 1; i < len1 + 1; i++) {
			for (int j = 1; j < len2 + 1; j++) {
				int nChildNodes = nodeList2.get(j-1).getSubComponents().size();
				List<ComponentInstance> childNodes = nodeList2.get(j-1)
						.getSubComponents();
				ComponentInstance node1 = nodeList1.get(i - 1);
				ComponentInstance node2 = nodeList2.get(j - 1);
				int renamingCost = ((node1.getLabel().equals(node2.getLabel())) ? 0
						: RENAMING_COST);

				if (nChildNodes == 0) {
					distMatrix[i][j] = Math.min(distMatrix[i - 1][j]
							+ DELETION_COST, Math.min(distMatrix[i][0]
							+ INSERTION_COST, distMatrix[i - 1][0]
							+ renamingCost));

				} else {
					int min1 = Integer.MAX_VALUE, min2 = Integer.MAX_VALUE;

					for (ComponentInstance child : childNodes) {
						int childNumber = child.getTraversalNumber();
						min1 = Math.min(distMatrix[i][childNumber]
								- distMatrix[0][childNumber], min1);
						min2 = Math.min(distMatrix[i - 1][childNumber]
								- distMatrix[0][childNumber], min2);
					}

					distMatrix[i][j] = Math.min(
							distMatrix[i - 1][j] + DELETION_COST,
							Math.min(distMatrix[0][j] + min1, renamingCost
									+ distMatrix[0][j] - INSERTION_COST + min2));
				}// end if

			}// end for j
		}// end for i

		return distMatrix[len1][len2];
	}

	/**
	 * get the root component
	 * 
	 * @return
	 */
	public ComponentInstance getUnboundedComponent() {
		return _unboundedComponent;
	}

	/**
	 * test if area 1 contains area 2
	 * 
	 * @param a1
	 * @param a2
	 * @return
	 */
	public boolean contain(Area a1, Area a2) {
		Area a1Clone = (Area) a1.clone();
		a1.add(a2);
		if (a1.equals(a1Clone))
			return true;
		else
			return false;
	}

	/**
	 * test if an component is a solid region
	 * 
	 * @param level
	 * @return
	 */
	public boolean isHole(int level) {
		if (level % 2 != 0)
			return false;
		else
			return true;
	}

	public static void main(String[] args) {
		
		
		//tree1
		ComponentInstance root1 = new ComponentInstance();
		root1.setLabel("a");

		ComponentInstance[] components1 = new ComponentInstance[7];
		for (int i = 0; i < components1.length; i++) {
			components1[i] = new ComponentInstance();
			components1[i].setLabel("a");
		}

		for (int i = 0; i < 3; i++) {
			root1.addSubComponent(components1[i]);
		}
		
//		components1[0].addSubComponent(components1[3]);
//		components1[3].addSubComponent(components1[5]);
//		components1[3].addSubComponent(components1[6]);
//
//		components1[1].addSubComponent(components1[4]);
//		
		
		//tree2
		ComponentInstance root2 = new ComponentInstance();
		root1.setLabel("a");

		ComponentInstance[] components2 = new ComponentInstance[7];
		for (int i = 0; i < components2.length; i++) {
			components2[i] = new ComponentInstance();
			components2[i].setLabel("a");
		}

		for (int i = 0; i < 3; i++) {
			root2.addSubComponent(components2[i]);
			components2[i].setContainerComponent(root2);
		}
		
//		components2[2].addSubComponent(components2[4]);
//		components2[4].setContainerComponent(components2[2]);
//		
//		components2[4].addSubComponent(components2[5]);
//		components2[4].addSubComponent(components2[6]);
//		components2[5].setContainerComponent(components2[4]);
//		components2[6].setContainerComponent(components2[4]);
//		
//		components2[1].addSubComponent(components2[3]);
//		components2[3].setContainerComponent(components2[1]);
//		
		List<ComponentInstance> nodes1 =  new ArrayList<ComponentInstance>();
		LayerGraph.traversalLeftToRightPostOrder(root1,nodes1);
		List<ComponentInstance> nodes2 = new ArrayList<ComponentInstance>();
		LayerGraph.traversalLeftToRightPostOrder(root2,nodes2);
		
		int dis = LayerGraph.unodreredTreeEditDistance(nodes1, nodes1);
		
		System.out.println("distance is " + dis);
		
	}
}

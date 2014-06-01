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
import sn.treedistance.ComparisonZhangShasha;
import sn.treedistance.CreateTreeHelper;
import sn.treedistance.OpsZhangShasha;
import sn.treedistance.Transformation;
import sn.treedistance.TreeDefinition;

public class LayerGraph {

	private ComplexRegion _complexRegion;
	private ComponentInstance _unboundedComponent;
	private List<ComponentInstance> _componentList;
	private int _nComponents;

	public LayerGraph(ComplexRegion complexRegion) throws Exception {
		// Initialize the root component, i.e. the canvas
		_unboundedComponent = new ComponentInstance(0);
		_complexRegion = complexRegion;

		_componentList = getRealComponents();
		_nComponents = _componentList.size();
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

		int nodeCount = 1;
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
				components.add(new ComponentInstance(nodeCount,
						(Path2D) tempPath));
				nodeCount++;
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
	public static void traversalLeftToRightPostOrder(ComponentInstance root,
			List<ComponentInstance> nodeList) {

		// recursion
		for (ComponentInstance node : root.getSubComponents()) {
			traversalLeftToRightPostOrder(node, nodeList);
		}

		nodeList.add(root);
		root.setTraversalNumber(nodeList.size());

		return;
	}

	/**
	 * Use breadth first to parse a tree to string in format such as a-b;a-c
	 * 
	 * @param root
	 * @return a string represents the tree
	 */
	public static String parseTreeToStr(ComponentInstance root) {

		String treeStr = new String();
		// List for controlling a left-to-right searching order
		List<ComponentInstance> processingList = new ArrayList<ComponentInstance>();

		processingList.add(root);// add root to processing list

		// process until no node in processing list
		while (!processingList.isEmpty()) {

			// get the first node in processing list
			ComponentInstance currentNode = processingList.get(0);

			if (currentNode.getSubComponents().size() > 0) {
				for (ComponentInstance node : currentNode.getSubComponents()) {
					if (currentNode.getLevel() % 2 == 0)
						treeStr += "hollow" + ":"
								+ String.valueOf(currentNode.getID() + "-");
					else
						treeStr += "solid" + ":"
								+ String.valueOf(currentNode.getID()+ "-") ;

					if (node.getLevel() % 2 == 0)
						treeStr += "hollow" + ":"
								+ String.valueOf(node.getID()) + ";";
					else
						treeStr += "solid" + ":"
								+ String.valueOf(node.getID()) + ";";

				}
			}
			// remove the processed node
			processingList.remove(0);

			// add all current node's child nodes into processing list
			for (ComponentInstance node : currentNode.getSubComponents()) {
				processingList.add(node);
			}
		}

		// remove last semicolon
		treeStr = treeStr.substring(0, treeStr.length() - 1);

		return treeStr;
	}

	/**
	 * Get all permutations for each non-leaf node
	 * 
	 * @param currentNode
	 * @param endID
	 * @param permutations
	 */
	public static void getAllPermutations(List<ComponentInstance> nodeList,
			List<ArrayList<int[]>> permutations) {

		for (ComponentInstance node : nodeList) {
			if (node.getSubComponents().size() > 0) {

				int nSubNodes = node.getSubComponents().size();
				int[] indexArray = new int[nSubNodes];
				for (int i = 0; i < indexArray.length; i++) {
					indexArray[i] = i;
				}

				ArrayList<int[]> subNodePermutations = new ArrayList<int[]>();

				// get all possible permutations of the siblings
				permutation(indexArray, 0, subNodePermutations);
				// depth first search order
				permutations.add(subNodePermutations);
			}
		}
	}

	public static void getAllOrderedTrees(List<ComponentInstance> nodeList,
			List<ArrayList<int[]>> permutations, List<String> treeStrs,
			int depth, int startNode) {

		ComponentInstance node = nodeList.get(startNode);
		List<int[]> perm = permutations.get(depth);
		startNode++;
		while (node.getSubComponents().isEmpty()) {
			node = nodeList.get(startNode);
			startNode++;
		}
		depth++;
		for (int[] indices : perm) {
			List<ComponentInstance> subNodeList = new ArrayList<ComponentInstance>();
			for(ComponentInstance subNode : node.getSubComponents()){
				subNodeList.add(subNode);
			}
			
			for (int i = 0; i < indices.length; i++) {
				int index = indices[i];
				node.getSubComponents().set(i,
						subNodeList.get(index));
			}
			if (depth == permutations.size()) {
				treeStrs.add(parseTreeToStr(nodeList.get(nodeList.size()-1)));
			} else
				getAllOrderedTrees(nodeList, permutations, treeStrs, depth,
						startNode);
		}
		
		
	}

	/**
	 * Full permutation
	 * 
	 * @return
	 */
	public static void permutation(int[] indexArray, int curIndex,
			List<int[]> results) {
		int len = indexArray.length;

		if (curIndex == len - 1) {
			results.add(indexArray.clone());
		} else {
			for (int i = curIndex; i < len; i++) {

				// swap ith and curIndexth element
				int temp = indexArray[i];
				indexArray[i] = indexArray[curIndex];
				indexArray[curIndex] = temp;

				permutation(indexArray, curIndex + 1, results);

				// swap back
				temp = indexArray[i];
				indexArray[i] = indexArray[curIndex];
				indexArray[curIndex] = temp;
			}
		}
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
	 * test if a component is a solid region
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

	public static void test() {
		// tree1
		ComponentInstance root1 = new ComponentInstance(0);
		root1.setLabel("a");

		ComponentInstance[] components1 = new ComponentInstance[7];
		for (int i = 0; i < components1.length; i++) {
			components1[i] = new ComponentInstance(i + 1);
			components1[i].setLabel("a");
		}

		for (int i = 0; i < 3; i++) {
			root1.addSubComponent(components1[i]);
		}

		components1[0].addSubComponent(components1[3]);
		components1[3].addSubComponent(components1[5]);
		components1[3].addSubComponent(components1[6]);

		components1[1].addSubComponent(components1[4]);

		// tree2
		ComponentInstance root2 = new ComponentInstance(0);
		root2.setLabel("a");

		ComponentInstance[] components2 = new ComponentInstance[7];
		for (int i = 0; i < components2.length; i++) {
			components2[i] = new ComponentInstance(i + 1);
			components2[i].setLabel("a");
		}

		for (int i = 0; i < 3; i++) {
			root2.addSubComponent(components2[i]);
			components2[i].setContainerComponent(root2);
		}
		
		components2[2].addSubComponent(components2[4]);
		components2[4].setContainerComponent(components2[2]);

		components2[4].addSubComponent(components2[5]);
		components2[4].addSubComponent(components2[6]);
		components2[5].setContainerComponent(components2[4]);
		components2[6].setContainerComponent(components2[4]);

		components2[1].addSubComponent(components2[3]);
		components2[3].setContainerComponent(components2[1]);

		List<ComponentInstance> nodes1 = new ArrayList<ComponentInstance>();
		LayerGraph.traversalLeftToRightPostOrder(root1, nodes1);
		List<ComponentInstance> nodes2 = new ArrayList<ComponentInstance>();
		LayerGraph.traversalLeftToRightPostOrder(root2, nodes2);



		List<String> treeStrs = new ArrayList<String>();
		List<ArrayList<int[]>> permutations = new ArrayList<ArrayList<int[]>>();

		getAllPermutations(nodes2, permutations);
		getAllOrderedTrees(nodes2, permutations, treeStrs, 0, 0);

		System.out.println(treeStrs.size());
		for (String str : treeStrs) {
			System.out.println(str);
		}
		
		 String treeStr1 = parseTreeToStr(root1);
		 //String treeStr2 = parseTreeToStr(root2);
		
		 TreeDefinition aTree = CreateTreeHelper.makeTree(treeStr1);
		 System.out.println("The tree is: \n" + aTree);
		 
		 for(int i = 0; i < treeStrs.size(); i++){
			 String treeStr2 = treeStrs.get(i);
			 TreeDefinition bTree = CreateTreeHelper.makeTree(treeStr2);
			 System.out.println("The tree is: \n" + bTree);
			
			 ComparisonZhangShasha treeCorrector = new ComparisonZhangShasha();
			 OpsZhangShasha costs = new OpsZhangShasha();
			 Transformation transform = treeCorrector.findDistance(aTree, bTree,
			 costs);
			 System.out.println("Distance: " + transform.getCost());
		 }
		
	}

	public static void main(String[] args) {
		test();
	}
}

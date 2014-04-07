package sn.recover;

import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import sn.regiondetect.ComplexRegion;
import sn.regiondetect.Region;

public class LayerGraph {
	private ComponentInstance _unboundedComponent;
	
	public LayerGraph(ComplexRegion complexRegion){
		//Initialize the root component, i.e. the canvas
		_unboundedComponent = new ComponentInstance(0);
	}
	

	/**
	 *  Extract separated components from the raw generated region data
	 * @param complexRegion
	 * @return Layered components
	 * @throws Exception
	 */
	public List<ComponentInstance> getRealComponents(ComplexRegion complexRegion) throws Exception {
		List<ComponentInstance> components = new ArrayList<ComponentInstance>();

		Region[] rawReigons = complexRegion.getComplexRegion();
		Area regionCanvas = new Area();

		//combine raw regions into one area
		for (Region r : rawReigons) {
			Path2D outline = r.getShape();
			Area regionArea = new Area(outline);

			//if the region is a hole, then subtract it from the canvas
			//else add the region to the canvas
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
				Point2D intersectPt = new Point2D.Double(coords[0], coords[1]);
				tempPath.lineTo(coords[0], coords[1]);
				System.out.println("type: LINETO " + intersectPt.toString());
				break;
			}

			case PathIterator.SEG_MOVETO: {
				Point2D intersectPt = new Point2D.Double(coords[0], coords[1]);
				tempPath.moveTo(coords[0], coords[1]);
				System.out.println("type: MOVETO " + intersectPt.toString());
				break;
			}

			case PathIterator.SEG_CUBICTO: {

				Point2D intersectPt = new Point2D.Double(coords[0], coords[1]);
				tempPath.curveTo(coords[0], coords[1],coords[2], coords[3],coords[4], coords[5]);
				System.out.println("type: CUBICTO " + intersectPt.toString());
				break;
			}

			//while the path is closed, save to component list and start a new path
			case PathIterator.SEG_CLOSE: {
				tempPath.closePath();
				components.add(new ComponentInstance(tempPath));
				tempPath = new Path2D.Double();
				tempPath.setWindingRule(PathIterator.WIND_EVEN_ODD);
				System.out.println("type: CLOSE ");
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
	
	public void getLayerInfo(List<ComponentInstance> components){
		for(int i = 0; i < components.size(); i++){
			ComponentInstance c1 = components.get(i);
			
			//set the component to level 1
			c1.setLevel(1);
			for(int j = 0; j < components.size(); j++){
				ComponentInstance c2 = components.get(j);
				Area area1 = new Area(c1.getPath());
				Area area2 = new Area(c2.getPath());

			}
		}
	}
	
}

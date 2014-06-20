package sn.regiondetect;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GeomUtil {

	private static double MAX_DIST = 1E-2;// Threshold for determining if a
											// point is on a line

	public static List<Line2D> generateParallelLines(double gap, double angle,
			int width, int height) {
		List<Line2D> lines = new ArrayList<Line2D>();
		double xOffset = (1 / Math.tan(angle)) * height;
		double gapInX = Math.abs(gap / Math.sin(angle));
		int nlines = (int) ((width + Math.abs(xOffset)) / gapInX) + 1;
		double xTop;
		double xButtom;
		if (xOffset >= 0) {
			xTop = 0 - xOffset;
			xButtom = 0;
		} else {
			xTop = 0;
			xButtom = 0 + xOffset;
		}
		Line2D l2d;
		for (int i = 0; i < nlines; i++) {

			if (i == 0) {
				l2d = new Line2D.Double(xTop, 0, xButtom, height);
			} else {
				xTop += gapInX;
				xButtom += gapInX;
				l2d = new Line2D.Double(xTop, 0, xButtom, height);

			}
			lines.add(l2d);
		}

		return lines;
	}

	/**
	 * draw lines on regions
	 * 
	 * @param intersectLines
	 * @param regions
	 * @param l
	 * @return
	 * @throws Exception
	 */
	public static List<Line2D> lineRegion(List<Line2D> intersectLines,
			Region p, Line2D l, double lineAngle, int height, int width)
			throws Exception {
		// System.out.print("line : " + lineCount + "\n");

		AffineTransform rotate = new AffineTransform();
		rotate.rotate(Math.PI / 2 - lineAngle, width / 2, height / 2);
		AffineTransform rotateInverse = new AffineTransform();
		rotateInverse.rotate(lineAngle - Math.PI / 2, width / 2, height / 2);

		Set<Point2D> intersections = GeomUtil.getIntersections(p.getShape(), l,
				lineAngle, width, height);

		List<Point2D> intersectionArray = new ArrayList<Point2D>();
		for (Iterator<Point2D> it = intersections.iterator(); it.hasNext();) {
			Point2D rotatedPt = rotate.transform(it.next(), null);
			intersectionArray.add(rotatedPt);
		}

		// sort vertically
		for (int i = 0; i < intersectionArray.size() - 1; i++) {
			for (int j = intersectionArray.size() - 1; j > 0; j--) {
				if (intersectionArray.get(j).getY() < intersectionArray.get(
						j - 1).getY()) {
					Point2D temPt = intersectionArray.get(j);
					intersectionArray.set(j, intersectionArray.get(j - 1));
					intersectionArray.set(j - 1, temPt);
				}
			}
		}

		for (int i = 0; i < intersectionArray.size(); i++) {
			Point2D rotatedPt = rotateInverse.transform(
					intersectionArray.get(i), null);
			intersectionArray.set(i, rotatedPt);
		}

		// System.out.println("Intersection: " +
		// intersections.size());
		Point2D start = new Point2D.Double(), end = new Point2D.Double();
		boolean draw = false, hasStart = false, hasEnd = false;
		for (Iterator<Point2D> it = intersectionArray.iterator(); it.hasNext();) {
			Point2D point = it.next();
			if (!hasStart) {
				start = point;
				hasStart = true;
			}

			else if (hasStart && !hasEnd) {
				end = point;
				hasEnd = true;
			}

			if (hasStart && hasEnd) {
				draw = true;
				hasStart = false;
				hasEnd = false;
			}

			// boolean addLine = false;
			if (draw) {
				intersectLines = GeomUtil
						.extendLine(intersectLines, start, end);
				// intersectLines.add(new Line2D.Double(start,end));
				draw = false;
			}
		}

		return intersectLines;
	}

	/**
	 * Split intervals to jump holes
	 * 
	 * @param intersectLines
	 *            segments of the line <code>l</code>
	 * @param p
	 *            the hole
	 * @param l
	 *            the line that needs to be split to jump the hole
	 * @param lineAngle
	 * @param height
	 *            canvas height
	 * @param width
	 *            canvas width
	 * @return
	 * @throws Exception
	 */
	public static List<Line2D> lineJumpHole(List<Line2D> intersectLines,
			Region p, Line2D l, double lineAngle, int height, int width)
			throws Exception {

		// point set for storing intersections of l with the hole
		Set<Point2D> intersections;

		// rotation operators for rotating the intersecting points to vertical
		AffineTransform rotate = new AffineTransform();
		rotate.rotate(Math.PI / 2 - lineAngle, width / 2, height / 2);
		AffineTransform rotateInverse = new AffineTransform();
		rotateInverse.rotate(lineAngle - Math.PI / 2, width / 2, height / 2);

		// find intersections of l with the hole p
		intersections = GeomUtil.getIntersections(p.getShape(), l, lineAngle,
				width, height);

		// rotate the intersecting points
		List<Point2D> intersectionArray = new ArrayList<Point2D>();
		for (Iterator<Point2D> it = intersections.iterator(); it.hasNext();) {
			Point2D rotatedPt = rotate.transform(it.next(), null);
			intersectionArray.add(rotatedPt);
		}

		// sort the points vertically
		for (int i = 0; i < intersectionArray.size() - 1; i++) {
			for (int j = intersectionArray.size() - 1; j > 0; j--) {
				if (intersectionArray.get(j).getY() < intersectionArray.get(
						j - 1).getY()) {
					Point2D temPt = intersectionArray.get(j);
					intersectionArray.set(j, intersectionArray.get(j - 1));
					intersectionArray.set(j - 1, temPt);
				}
			}
		}

		Point2D rotatedStart = new Point2D.Double(), rotatedEnd = new Point2D.Double(), start = new Point2D.Double(), end = new Point2D.Double();
		boolean drawHole = false, hasStart = false, hasEnd = false;
		for (Iterator<Point2D> it = intersectionArray.iterator(); it.hasNext();) {
			Point2D point = it.next();

			// a whole interval interests with a hole must have even number
			// intersecting points
			// find each two intersections and update the segments accordingly
			if (!hasStart) {
				rotatedStart = point;
				start = rotateInverse.transform(rotatedStart, null);
				hasStart = true;
			} else if (hasStart && !hasEnd) {
				rotatedEnd = point;
				end = rotateInverse.transform(rotatedEnd, null);
				hasEnd = true;
			}
			if (hasStart && hasEnd) {
				drawHole = true;
				hasStart = false;
				hasEnd = false;
			}

			// update previous segments of the line l
			if (drawHole) {
				for (int i = 0; i < intersectLines.size(); i++) {
					Line2D il = intersectLines.get(i);
					Point2D rotatedP1 = rotate.transform(il.getP1(), null);
					Point2D rotatedP2 = rotate.transform(il.getP2(), null);

					double y1 = rotatedP1.getY();
					double y2 = rotatedP2.getY();

					// sort the two ends of a segment of vertically
					if (y1 > y2) {
						Point2D temP;
						temP = rotatedP1;
						rotatedP1 = rotatedP2;
						rotatedP2 = temP;

						double temY;
						temY = y1;
						y1 = y2;
						y2 = temY;
					}

					// if the upper end of the segment is outside the hole and
					// the other end is in, then omit the inside part
					if (y1 < rotatedStart.getY() && y2 >= rotatedStart.getY()
							&& y2 <= rotatedEnd.getY()) {
						il.setLine(il.getP1(), start);
					}

					// if the lower end of the segment is outside the hole and
					// the other end is in, then omit the inside part
					else if (y1 >= rotatedStart.getY()
							&& y1 <= rotatedEnd.getY()
							&& y2 > rotatedEnd.getY()) {
						il.setLine(end, il.getP2());
					}

					// if the entire segment is inside the hole, then omit the
					// segment
					else if (y1 >= rotatedStart.getY()
							&& y2 <= rotatedEnd.getY()) {
						intersectLines.remove(i);
						i--;
					}

					// if the both ends of the segment is outside the hole, then
					// omit the middle inside part
					else if (y1 < rotatedStart.getY() && y2 > rotatedEnd.getY()) {
						Point2D temEnd = rotateInverse.transform(rotatedP2,
								null);
						il.setLine(rotateInverse.transform(rotatedP1, null),
								start);
						// System.out.println(intersectLines.size());
						intersectLines.add(0, new Line2D.Double(end, temEnd));
						// System.out.println(intersectLines.size()
						// +" "
						// + lineC + "\n");
					}
				}
				drawHole = false;
			}

		}
		return intersectLines;
	}

	/**
	 * Extend and merge intersecting lines
	 * 
	 * @param intersectLines
	 * @param start
	 * @param end
	 * @return
	 */
	public static List<Line2D> extendLine(List<Line2D> intersectLines,
			Point2D start, Point2D end) {
		boolean addLine = false;
		for (Line2D il : intersectLines) {
			if (!il.intersectsLine(new Line2D.Double(start, end))) {
				addLine = true;
			}
			if (start.getY() <= il.getY1() && end.getY() >= il.getY1()
					&& end.getY() <= il.getY2()) {
				il.setLine(start, il.getP2());
				addLine = false;
				mergeLine(intersectLines);
				break;
			}

			else if (start.getY() >= il.getY1() && start.getY() <= il.getY2()
					&& end.getY() >= il.getY2()) {
				il.setLine(il.getP1(), end);
				addLine = false;
				mergeLine(intersectLines);
				break;
			}

			else if (il.getY1() >= start.getY() && il.getY2() <= end.getY()) {
				il.setLine(start, end);
				addLine = false;
				mergeLine(intersectLines);
				break;
			}

			else if (!(il.getY2() < start.getY() || il.getY1() > end.getY())) {
				addLine = false;
			}

		}

		if (intersectLines.isEmpty() || addLine) {
			intersectLines.add(new Line2D.Double(start, end));
			mergeLine(intersectLines);
			// System.out.print(addLine+"\n");
		}

		return intersectLines;
	}

	/**
	 * Merge overlapping lines
	 * 
	 * @param intersectLines
	 * @return
	 */
	public static List<Line2D> mergeLine(List<Line2D> intersectLines) {
		boolean merged = false;
		for (int i = 0; i < intersectLines.size();) {
			for (int j = i + 1; j < intersectLines.size(); j++) {
				Line2D l1 = intersectLines.get(i);
				Line2D l2 = intersectLines.get(j);
				if (l1.getY1() <= l2.getY1() && l1.getY2() >= l2.getY1()
						&& l1.getY2() <= l2.getY2()) {
					l2.setLine(l1.getP1(), l2.getP2());
					intersectLines.remove(i);
					// System.out.print("remove : " + i + "\n");
					merged = true;
					break;
				}

				else if (l1.getY1() >= l2.getY1() && l1.getY1() <= l2.getY2()
						&& l1.getY2() >= l2.getY2()) {
					l2.setLine(l2.getP1(), l1.getP2());
					intersectLines.remove(i);
					// System.out.print("remove : " + i + "\n");
					merged = true;
					break;
				}

				else if (l2.getY1() >= l1.getY1() && l2.getY2() <= l1.getY2()) {
					l2.setLine(l1);
					// mergeLine(intersectLines);
					intersectLines.remove(i);
					// System.out.print("remove : " + i + "\n");
					merged = true;
					break;
				}

				else if (l1.getY1() >= l2.getY1() && l1.getY2() <= l2.getY2()) {
					intersectLines.remove(i);
					merged = true;
					break;
				}
			}
			if (merged == true) {
				i = 0;
				merged = false;
			} else
				i++;
		}

		return intersectLines;
	}

	/**
	 * Get intersecting part of a line and an area
	 * 
	 * @param path
	 * @param line
	 * @return a set of point as intersections
	 * @throws Exception
	 */
	public static Set<Point2D> getIntersections(Path2D path, Line2D line,
			double lineAngle, int width, int height) throws Exception {
		// List to hold found intersections

		Set<Point2D> intersections = new HashSet<Point2D>();

		AffineTransform rotate = new AffineTransform();
		rotate.rotate(lineAngle, width / 2, height / 2);
		AffineTransform rotateInverse = new AffineTransform();
		rotateInverse.rotate(-lineAngle, width / 2, height / 2);

		Point2D rtPt1 = rotate.transform(line.getP1(), null);
		Point2D rtPt2 = rotate.transform(line.getP2(), null);
		Point2D boldPt1 = rotateInverse.transform(
				new Point2D.Double(rtPt1.getX() - 1, rtPt1.getY()), null);
		Point2D boldPt2 = rotateInverse.transform(
				new Point2D.Double(rtPt2.getX() - 1, rtPt2.getY()), null);
		Point2D boldPt3 = rotateInverse.transform(
				new Point2D.Double(rtPt2.getX() + 1, rtPt2.getY()), null);
		Point2D boldPt4 = rotateInverse.transform(
				new Point2D.Double(rtPt1.getX() + 1, rtPt1.getY()), null);

		Path2D boldLine = new Path2D.Double();

		boldLine.moveTo(boldPt1.getX(), boldPt1.getY());
		boldLine.lineTo(boldPt2.getX(), boldPt2.getY());
		boldLine.lineTo(boldPt3.getX(), boldPt3.getY());
		boldLine.lineTo(boldPt4.getX(), boldPt4.getY());
		boldLine.closePath();

		Line2D auxLine = new Line2D.Double(boldPt1, boldPt2);

		Area pathArea = new Area(path);
		Area lineArea = new Area(boldLine);

		if (lineArea.isEmpty()) {
			System.out.println("line is empty");
		}

		pathArea.intersect(lineArea);

		if (pathArea.isEmpty()) {
			return intersections;
		}

		PathIterator lineIt = pathArea.getPathIterator(null);

		// Double array with length 6 needed by iterator
		double[] coords = new double[6];

		int type;
		List<Point2D> intersectionsTemp = new LinkedList<Point2D>();
		while (!lineIt.isDone()) {
			double dist;
			type = lineIt.currentSegment(coords);
			switch (type) {
			case PathIterator.SEG_LINETO: {
				Point2D intersectPt = new Point2D.Double(coords[0], coords[1]);
				// System.out.println("type: LINETO "+ intersectPt.toString());
				dist = auxLine.ptLineDistSq(intersectPt);
				// System.out.println(" dist: " + dist);
				// System.out.println("\n not ADD POINT "+
				// intersectPt.toString() + " dist: " + dist + "\n");
				if (dist <= MAX_DIST) {

					boolean inList = false;
					for (Point2D pt : intersections) {
						if ((pt.getX() - coords[0]) * (pt.getX() - coords[0])
								+ (pt.getY() - coords[1])
								* (pt.getY() - coords[1]) <= MAX_DIST) {
							inList = true;
						}
					}
					if (!inList) {
						intersectionsTemp.add(new Point2D.Double(coords[0],
								coords[1]));
						// System.out.println("\nADD POINT "+
						// intersectPt.toString() + " dist: " + dist + "\n");
					}
				}
				break;
			}

			case PathIterator.SEG_MOVETO: {
				if (intersectionsTemp.size() == 2) {
					intersections.addAll(intersectionsTemp);
				} else if (intersectionsTemp.size() > 2) {
					for (int m = 0; m < intersectionsTemp.size() - 1; m++) {
						for (int n = intersectionsTemp.size() - 1; n > 0; n--) {
							if (intersectionsTemp.get(n).getY() < intersectionsTemp
									.get(n - 1).getY()) {
								Point2D temPt = intersectionsTemp.get(n);
								intersectionsTemp.set(n,
										intersectionsTemp.get(n - 1));
								intersectionsTemp.set(n - 1, temPt);
							}
						}
					}
					intersections.add(intersectionsTemp.get(0));
					intersections.add(intersectionsTemp.get(intersectionsTemp
							.size() - 1));

				}

				intersectionsTemp.clear();

				Point2D intersectPt = new Point2D.Double(coords[0], coords[1]);
				// System.out.println("type: MOVETO "+ intersectPt.toString());
				dist = auxLine.ptLineDistSq(intersectPt);
				// System.out.println(" dist: " + dist);
				if (dist <= MAX_DIST) {
					boolean inList = false;
					for (Point2D pt : intersections) {
						if ((pt.getX() - coords[0]) * (pt.getX() - coords[0])
								+ (pt.getY() - coords[1])
								* (pt.getY() - coords[1]) <= MAX_DIST) {
							inList = true;
						}
					}
					if (!inList) {
						intersectionsTemp.add(new Point2D.Double(coords[0],
								coords[1]));
						// System.out.println("\nADD POINT "+
						// intersectPt.toString() + " dist: " + dist + "\n");

					}
				}
				break;
			}

			case PathIterator.SEG_CUBICTO: {

				Point2D intersectPt = new Point2D.Double(coords[0], coords[1]);
				// System.out.println("type: CUBICTO "+ intersectPt.toString());
				dist = auxLine.ptLineDistSq(intersectPt);
				// System.out.println(" dist: " + dist);
				if (dist <= MAX_DIST) {
					boolean inList = false;
					for (Point2D pt : intersections) {
						if ((pt.getX() - coords[0]) * (pt.getX() - coords[0])
								+ (pt.getY() - coords[1])
								* (pt.getY() - coords[1]) <= MAX_DIST) {
							inList = true;
						}
					}
					if (!inList) {
						intersectionsTemp.add(new Point2D.Double(coords[0],
								coords[1]));
						// System.out.println("\nADD POINT "+
						// intersectPt.toString() + " dist: " + dist + "\n");
					}
				}
				break;
			}

			case PathIterator.SEG_CLOSE: {
				if (intersectionsTemp.size() == 2) {
					intersections.addAll(intersectionsTemp);
				} else if (intersectionsTemp.size() > 2) {
					for (int m = 0; m < intersectionsTemp.size() - 1; m++) {
						for (int n = intersectionsTemp.size() - 1; n > 0; n--) {
							if (intersectionsTemp.get(n).getY() < intersectionsTemp
									.get(n - 1).getY()) {
								Point2D temPt = intersectionsTemp.get(n);
								intersectionsTemp.set(n,
										intersectionsTemp.get(n - 1));
								intersectionsTemp.set(n - 1, temPt);
							}
						}
					}
					intersections.add(intersectionsTemp.get(0));
					intersections.add(intersectionsTemp.get(intersectionsTemp
							.size() - 1));

				}

				intersectionsTemp.clear();

				// System.out.println("type: CLOSE "+ intersectPt.toString());
				break;
			}
			default: {
				throw new Exception("Unsupported PathIterator segment type: "
						+ type);
			}
			}
			lineIt.next();
		}

		if (intersections.size() % 2 != 0)
			System.out.println("odd number of intersection points");

		return intersections;

	}

	/**
	 * Get intersecting point of two lines
	 * 
	 * @param line1
	 * @param line2
	 * @return Point2D the intersecting point
	 */
	public static Point2D getIntersection(final Line2D line1, final Line2D line2) {

		double x1, y1, x2, y2, x3, y3, x4, y4;
		x1 = line1.getX1();
		y1 = line1.getY1();
		x2 = line1.getX2();
		y2 = line1.getY2();
		x3 = line2.getX1();
		y3 = line2.getY1();
		x4 = line2.getX2();
		y4 = line2.getY2();
		double x = ((x2 - x1) * (x3 * y4 - x4 * y3) - (x4 - x3)
				* (x1 * y2 - x2 * y1))
				/ ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));
		double y = ((y3 - y4) * (x1 * y2 - x2 * y1) - (y1 - y2)
				* (x3 * y4 - x4 * y3))
				/ ((x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4));

		return new Point2D.Double(x, y);

	}

	/**
	 * Draw positive sensor intervals on a complex region
	 * 
	 * @param bi
	 * @param il
	 * @param lineC
	 * @param subLineC
	 * @param showNum
	 */
	public static void drawLine(BufferedImage bi, Line2D il, int lineC,
			int subLineC, boolean showNum) {

		Graphics2D g2d = (Graphics2D) bi.createGraphics();
		if (showNum) {
			g2d.setFont(new Font("Helvetica", Font.PLAIN, 10));
			g2d.setColor(Color.ORANGE);
			g2d.drawString("s" + String.valueOf(lineC), (int) il.getX1(),
					(int) il.getY1());
			g2d.drawString(":" + String.valueOf(subLineC),
					(int) il.getX1() + 15, (int) il.getY1());
			g2d.setColor(Color.PINK);
			g2d.drawString("e" + String.valueOf(lineC), (int) il.getX2(),
					(int) il.getY2());
			g2d.drawString(":" + String.valueOf(subLineC),
					(int) il.getX2() + 15, (int) il.getY2());
		}
		g2d.setColor(Color.BLACK);
		g2d.drawLine((int) il.getX1(), (int) il.getY1(), (int) il.getX2(),
				(int) il.getY2());

	}

	/**
	 * Smooth corners of a polygon. Take polygon as input
	 * 
	 * @param polygon
	 * @return Path of smoothed region
	 */
	public static Path2D getRoundedGeneralPath(Polygon polygon) {
		List<int[]> l = new ArrayList<int[]>();
		for (int i = 0; i < polygon.npoints; i++) {
			l.add(new int[] { polygon.xpoints[i], polygon.ypoints[i] });
		}
		return getRoundedGeneralPath(l);
	}

	/**
	 * Smooth corners of a polygon. Take an int array as input
	 * 
	 * @param l
	 * @return
	 */
	public static Path2D getRoundedGeneralPath(List<int[]> l) {
		List<Point> list = new ArrayList<Point>();
		for (int[] point : l) {
			list.add(new Point(point[0], point[1]));
		}
		return getRoundedGeneralPathFromPoints(list);
	}

	/**
	 * Smooth corners of a polygon. Take a list of points as input
	 * 
	 * @param l
	 * @return
	 */
	public static Path2D getRoundedGeneralPathFromPoints(List<Point> l) {
		l.add(l.get(0));
		l.add(l.get(1));
		GeneralPath p = new GeneralPath();
		Point begin = calculatePoint(l.get(l.size() - 1), l.get(0));
		p.moveTo(begin.x, begin.y);
		for (int pointIndex = 1; pointIndex < l.size() - 1; pointIndex++) {

			Point p1 = l.get(pointIndex - 1);
			Point p2 = l.get(pointIndex);
			Point p3 = l.get(pointIndex + 1);
			Point m1 = calculatePoint(p1, p2);
			p.lineTo(m1.x, m1.y);
			Point m2 = calculatePoint(p3, p2);
			p.curveTo(p2.x, p2.y, p2.x, p2.y, m2.x, m2.y);
		}
		return p;
	}

	/**
	 * 
	 * @param p1
	 * @param p2
	 * @return
	 */
	private static Point calculatePoint(Point p1, Point p2) {
		double arcSize = 0.4;

		double per = arcSize;
		double d_x = (p1.x - p2.x) * per;
		double d_y = (p1.y - p2.y) * per;
		int xx = (int) (p2.x + d_x);
		int yy = (int) (p2.y + d_y);
		return new Point(xx, yy);
	}

}

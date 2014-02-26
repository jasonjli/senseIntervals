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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;

import sn.debug.ShowDebugImage;

public class GeomUtil {

	private static double _MAXDIST = 1E-4;

	/**
	 * draw lines on regions
	 * 
	 * @param intersectLines
	 * @param regions
	 * @param l
	 * @return
	 * @throws Exception
	 */
	static public List<Line2D> lineRegion(List<Line2D> intersectLines,
			Region p, Line2D l, double lineAngle, int height, int width)
			throws Exception {
		// System.out.print("line : " + lineCount + "\n");

		AffineTransform rotate = new AffineTransform();
		rotate.rotate(lineAngle, width / 2, height / 2);
		AffineTransform rotateInverse = new AffineTransform();
		rotateInverse.rotate(-lineAngle, width / 2, height / 2);

		Set<Point2D> intersections = GeomUtil.getIntersections(p._path, l,
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

//		for (int i = 0; i < intersectionArray.size(); i++) {
//			Point2D temPt = intersectionArray.get(i);
//			intersectionArray.set(i, rotateInverse.transform(temPt, null));
//		}

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
//				intersectLines.add(new Line2D.Double(start,end));
				draw = false;
			}
		}
		return intersectLines;
	}

	static public List<Line2D> lineJumpHole(List<Line2D> intersectLines,
			Region p, Line2D l, double lineAngle, int height, int width)
			throws Exception {
		Set<Point2D> intersections;
		AffineTransform rotate = new AffineTransform();
		rotate.rotate(lineAngle, width / 2, height / 2);
		AffineTransform rotateInverse = new AffineTransform();
		rotateInverse.rotate(-lineAngle, width / 2, height / 2);

		intersections = GeomUtil.getIntersections(p._path, l, lineAngle, width,
				height);

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

//		for (int i = 0; i < intersectionArray.size(); i++) {
//			Point2D temPt = intersectionArray.get(i);
//			intersectionArray.set(i, rotateInverse.transform(temPt, null));
//		}

		// System.out.println("Intersection: " +
		// intersections.size());
		Point2D start = new Point2D.Double(), end = new Point2D.Double();
		boolean drawHole = false, hasStart = false, hasEnd = false;
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
				drawHole = true;
				hasStart = false;
				hasEnd = false;
			}

			// boolean addLine = false;
			if (drawHole) {
				for (int i = 0; i < intersectLines.size(); i++) {
					Line2D il = intersectLines.get(i);
					if (il.getY1() <= start.getY()
							&& il.getY2() >= start.getY()
							&& il.getY2() <= end.getY()) {
						il.setLine(il.getP1(), start);
					}

					else if (il.getY1() >= start.getY()
							&& il.getY1() <= end.getY()
							&& il.getY2() >= end.getY()) {
						il.setLine(end, il.getP2());
					} else if (il.getY1() >= start.getY()
							&& il.getY2() <= end.getY()) {
						intersectLines.remove(i);
						i--;
					} else if (il.getY1() <= start.getY()
							&& il.getY2() >= end.getY()) {
						Point2D temEnd = il.getP2();
						il.setLine(il.getP1(), start);
						// System.out.println(intersectLines.size());
						intersectLines.add(0, new Line2D.Double(end, temEnd));
						// System.out.println(intersectLines.size()
						// +" "
						// + lineC + "\n");
					}
				}
				drawHole = false;
			}
			// System.out.println("Intersection: " +
			// point.toString());

		}
		return intersectLines;

	}

	static public List<Line2D> extendLine(List<Line2D> intersectLines,
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
	static public List<Line2D> mergeLine(List<Line2D> intersectLines) {
		boolean merged = false;
		for (int i = 0; i < intersectLines.size();) {
			for (int j = i + 1; j < intersectLines.size(); j++) {
				Line2D l1 = intersectLines.get(i);
				Line2D l2 = intersectLines.get(j);
				if (l1.getY1() <= l2.getY1() && l1.getY2() >= l2.getY1()
						&& l1.getY2() <= l2.getY2()) {
					l2.setLine(l1.getP1(), l2.getP2());
					// mergeLine(intersectLines);
					intersectLines.remove(i);
					// System.out.print("remove : " + i + "\n");
					merged = true;
					break;
				}

				else if (l1.getY1() >= l2.getY1() && l1.getY1() <= l2.getY2()
						&& l1.getY2() >= l2.getY2()) {
					l2.setLine(l2.getP1(), l1.getP2());
					// mergeLine(intersectLines);
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

		// if (merged) {
		// mergeLine(intersectLines);
		// }

		return intersectLines;
	}

	/**
	 * 
	 * @param path
	 * @param line
	 * @return
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

		// boldLine.addPoint((int) boldPt1.getX(), (int) boldPt1.getY());
		// boldLine.addPoint((int) boldPt2.getX(), (int) boldPt2.getY());
		// boldLine.addPoint((int) boldPt3.getX(), (int) boldPt3.getY());
		// boldLine.addPoint((int) boldPt4.getX(), (int) boldPt4.getY());

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

		// ShowDebugImage frame = null;
		// Area area = new Area(path);
		// BufferedImage img = new BufferedImage(width, height,
		// BufferedImage.TYPE_3BYTE_BGR);
		// Graphics2D g2d = (Graphics2D) img.createGraphics();
		//
		// g2d.setBackground(Color.WHITE);
		// g2d.clearRect(0, 0, width, height);
		//
		// g2d.setColor(Color.BLACK);
		//
		// g2d.draw(pathArea);
		// g2d.fill(pathArea);
		//
		// Color color = new Color(1.0F, 0.75F, 0.0F, 0.45F);
		// g2d.setColor(color);
		// g2d.draw(auxLine);
		// color = new Color(0.0F, 0.5F, 0.6F, 0.45F);
		// g2d.fill(area);
		// frame = new ShowDebugImage("linePoly", img);
		// frame.refresh(img);

		PathIterator lineIt = pathArea.getPathIterator(null);

		// Double array with length 6 needed by iterator
		double[] coords = new double[6];

		int type;

		// lineIt.next();
		// g2d.setColor(Color.RED);

		while (!lineIt.isDone()) {
			double dist;

			type = lineIt.currentSegment(coords);
			switch (type) {
			case PathIterator.SEG_LINETO: {
				Point2D intersectPt = new Point2D.Double(coords[0], coords[1]);
				// System.out.println("type: LINETO "+ intersectPt.toString());
				// g2d.drawRect((int) intersectPt.getX(), (int)
				// intersectPt.getY(), 0,0);
				dist = auxLine.ptLineDistSq(intersectPt);
				// System.out.println(" dist: " + dist);
				// System.out.println("\n not ADD POINT "+
				// intersectPt.toString() + " dist: " + dist + "\n");
				if (dist <= _MAXDIST) {

					boolean inList = false;
					for (Point2D pt : intersections) {
						if (Math.abs(pt.getX() - coords[0]) <= 1
								&& Math.abs(pt.getY() - coords[1]) <= 1) {
							inList = true;
							// g2d.setColor(Color.BLUE);
							// g2d.drawRect((int) intersectPt.getX(), (int)
							// intersectPt.getY(), 0,0);
							// g2d.setColor(Color.RED);

						}
					}
					if (!inList) {
						intersections.add(new Point2D.Double(coords[0],
								coords[1]));
						// g2d.setColor(Color.BLUE);
						// g2d.drawRect((int) intersectPt.getX(), (int)
						// intersectPt.getY(), 0,0);
						// g2d.setColor(Color.RED);
						// System.out.println("\nADD POINT "+
						// intersectPt.toString() + " dist: " + dist + "\n");
					}
				}
				break;
			}

			case PathIterator.SEG_MOVETO: {
				Point2D intersectPt = new Point2D.Double(coords[0], coords[1]);
				// System.out.println("type: MOVETO "+ intersectPt.toString());
				// g2d.drawRect((int) intersectPt.getX(), (int)
				// intersectPt.getY(), 0,0);
				dist = auxLine.ptLineDistSq(intersectPt);
				// System.out.println(" dist: " + dist);
				if (dist <= _MAXDIST) {
					boolean inList = false;
					for (Point2D pt : intersections) {
						if (Math.abs(pt.getX() - coords[0]) <= 1
								&& Math.abs(pt.getY() - coords[1]) <= 1) {
							inList = true;
							// g2d.setColor(Color.BLUE);
							// g2d.drawRect((int) intersectPt.getX(), (int)
							// intersectPt.getY(), 0,0);
							// g2d.setColor(Color.RED);
						}
					}
					if (!inList) {
						intersections.add(new Point2D.Double(coords[0],
								coords[1]));
						// g2d.setColor(Color.BLUE);
						// g2d.drawRect((int) intersectPt.getX(), (int)
						// intersectPt.getY(), 0,0);
						// g2d.setColor(Color.RED);
						// System.out.println("\nADD POINT "+
						// intersectPt.toString() + " dist: " + dist + "\n");

					}
				}
				break;
			}

			case PathIterator.SEG_CUBICTO: {

				Point2D intersectPt = new Point2D.Double(coords[0], coords[1]);
				// System.out.println("type: CUBICTO "+ intersectPt.toString());
				// g2d.drawRect((int) intersectPt.getX(), (int)
				// intersectPt.getY(), 0,0);
				dist = auxLine.ptLineDistSq(intersectPt);
//				 System.out.println(" dist: " + dist);
				if (dist <= _MAXDIST) {
					boolean inList = false;
					for (Point2D pt : intersections) {
						if (Math.abs(pt.getX() - coords[0]) <= 1
								&& Math.abs(pt.getY() - coords[1]) <= 1) {
							inList = true;
							// g2d.setColor(Color.BLUE);
							// g2d.drawRect((int) intersectPt.getX(), (int)
							// intersectPt.getY(), 0,0);
							// g2d.setColor(Color.RED);
						}
					}
					if (!inList) {
						intersections.add(new Point2D.Double(coords[0],
								coords[1]));
						// g2d.setColor(Color.BLUE);
						// g2d.drawRect((int) intersectPt.getX(), (int)
						// intersectPt.getY(), 0,0);
						// g2d.setColor(Color.RED);
						// System.out.println("\nADD POINT "+
						// intersectPt.toString() + " dist: " + dist + "\n");
					}
				}
				break;
			}

			case PathIterator.SEG_CLOSE: {
				Point2D intersectPt = new Point2D.Double(coords[0], coords[1]);
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
		//
		// int imageCount = 0;
		// String imgFilename = String.format("img%d.png", imageCount);
		// System.out.println("saving image to " + imgFilename +
		// "\n===========================");
		// try {
		// ImageIO.write(img, "png", new File(imgFilename));
		// } catch (IOException e) {
		// System.err.println("failed to save image " + imgFilename);
		// e.printStackTrace();
		// }
		// frame = new ShowDebugImage("linePoly", img);
		return intersections;

	}

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

	public static Path2D getRoundedGeneralPath(Polygon polygon) {
		List<int[]> l = new ArrayList<int[]>();
		for (int i = 0; i < polygon.npoints; i++) {
			l.add(new int[] { polygon.xpoints[i], polygon.ypoints[i] });
		}
		return getRoundedGeneralPath(l);
	}

	public static Path2D getRoundedGeneralPath(List<int[]> l) {
		List<Point> list = new ArrayList<Point>();
		for (int[] point : l) {
			list.add(new Point(point[0], point[1]));
		}
		return getRoundedGeneralPathFromPoints(list);
	}

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

	private static Point calculatePoint(Point p1, Point p2) {
		double arcSize = 0.4;

		double per = arcSize;
		double d_x = (p1.x - p2.x) * per;
		double d_y = (p1.y - p2.y) * per;
		int xx = (int) (p2.x + d_x);
		int yy = (int) (p2.y + d_y);
		return new Point(xx, yy);
	}

	public static void main(String[] args) throws Exception {
		Polygon poly1 = new Polygon();
		Polygon poly2 = new Polygon();
		poly1.addPoint(1, 1);
		poly1.addPoint(6, 30);
		poly1.addPoint(25, 30);
		poly1.addPoint(35, 15);

		poly2.addPoint(2, 1);
		poly2.addPoint(2, 3);
		poly2.addPoint(4, 3);
		poly2.addPoint(4, 1);

		Area a1 = new Area(poly1);
		Area a2 = new Area(poly2);

		PathIterator lineIt = a1.getPathIterator(null);

		// Double array with length 6 needed by iterator
		double[] coords = new double[6];

		// First point (needed for closing polygon path)
		double[] firstCoords = new double[2];

		// Previously visited point
		double[] lastCoords = new double[2];

		//

		// Getting the first coordinate pair
		lineIt.currentSegment(firstCoords);

		// Priming the previous coordinate pair
		lastCoords[0] = firstCoords[0];
		lastCoords[1] = firstCoords[1];
		lineIt.next();

		boolean isNewLine = true;
		Point2D[] cornerPts = new Point2D[4];
		int nPts = 0;
		int c = 0;
		while (!lineIt.isDone()) {
			c++;
			int type = lineIt.currentSegment(coords);
			if (isNewLine) {
				cornerPts = new Point2D[8];
				cornerPts[0] = new Point2D.Double(lastCoords[0], lastCoords[1]);
				nPts++;
			}
			switch (type) {
			case PathIterator.SEG_LINETO: {
				lastCoords[0] = coords[0];
				lastCoords[1] = coords[1];
				cornerPts[nPts] = new Point2D.Double(coords[0], coords[1]);

				isNewLine = false;
				System.out
						.println("type: LINETO " + cornerPts[nPts].toString());
				nPts++;
				break;
			}

			case PathIterator.SEG_MOVETO: {
				lastCoords[0] = coords[0];
				lastCoords[1] = coords[1];
				isNewLine = true;

				System.out.println("type: MOVETO" + cornerPts[nPts].toString());
				nPts = 0;
				break;
			}

			case PathIterator.SEG_CUBICTO: {
				lastCoords[0] = coords[0];
				lastCoords[1] = coords[1];
				cornerPts[nPts] = new Point2D.Double(coords[0], coords[1]);

				isNewLine = false;
				System.out
						.println("type: CUBICTO" + cornerPts[nPts].toString());
				nPts++;
				break;
			}

			case PathIterator.SEG_CLOSE: {
				lastCoords[0] = coords[0];
				lastCoords[1] = coords[1];
				isNewLine = true;
				cornerPts[nPts] = new Point2D.Double(coords[0], coords[1]);
				System.out.println("type: CLOSE " + cornerPts[nPts].toString());
				break;
			}
			default: {
				throw new Exception("Unsupported PathIterator segment type: "
						+ type);
			}
			}
			lineIt.next();
		}
		System.out.println("path count: " + c);

		// a1.intersect(a2);

		int width = 100, height = 100;
		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, width, height);

		g2d.setColor(Color.BLACK);

		// g2d.draw(a1);
		g2d.draw(a1);
		Random r = new Random();

		String imgFilename = String.format("img%04d.png", r.nextInt(20));
		System.out.println("saving image to " + imgFilename);
		try {
			ImageIO.write(img, "png", new File(imgFilename));
		} catch (IOException e) {
			System.err.println("failed to save image " + imgFilename);
			e.printStackTrace();
		}

	}

}

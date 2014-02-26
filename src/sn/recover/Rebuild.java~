package sn.recover;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

public class Rebuild {

	public List<Point2D> getOuter(String filename) {
		File file = new File(filename);
		List<Point2D> outer = new ArrayList<Point2D>();
		List<Point2D> starts = new ArrayList<Point2D>();
		List<Point2D> ends = new ArrayList<Point2D>();
		if (file.exists()) {

			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				String sensorData = null;
				int sensorId;
				int prevSensorId = -1;
				Point2D sensorStart = null;
				Point2D sensorEnd = null;
				String[] data;

				while ((sensorData = reader.readLine()) != null) {

					data = sensorData.split(" ");
					sensorId = Integer.parseInt(data[0].split("Sensor")[1]);
					String p1 = data[1].substring(1, data[1].length() - 1);
					double x1 = Double.parseDouble(p1.split(",")[0]);
					double y1 = Double.parseDouble(p1.split(",")[1]);
					Point2D start = new Point2D.Double(x1, y1);

					String p2 = data[2].substring(1, data[2].length() - 1);
					double x2 = Double.parseDouble(p2.split(",")[0]);
					double y2 = Double.parseDouble(p2.split(",")[1]);
					Point2D end = new Point2D.Double(x2, y2);

					if (prevSensorId == -1) {
						sensorStart = start;
						sensorEnd = end;
					}

					if (sensorId != prevSensorId) {
						if (prevSensorId != -1) {
							starts.add(sensorStart);
							ends.add(sensorEnd);
						}
						sensorStart = start;
						sensorEnd = end;
					}

					else {
						if(sensorStart.getY() < start.getY())
							sensorStart = start;
						if(sensorEnd.getY()>end.getY())
							sensorEnd = end;
					}

					prevSensorId = sensorId;

				}
				reader.close();
				
				for(Point2D pt : starts){
					outer.add(pt);
				}
				
				for(int i = ends.size()-1; i >= 0; i--){
					outer.add(ends.get(i));
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("failed to read file: " + filename);
		}
		return outer;
	}

	public Path2D getConvexHull(ArrayList<Point2D> outer) {

		Path2D convexHull = new Path2D.Double();
		List<Point2D> convetPts = executeConvexHull(outer);
		convexHull.moveTo(convetPts.get(0).getX(), convetPts.get(0).getY());
		for (int i = 1; i < convetPts.size(); i++) {
			convexHull.lineTo(convetPts.get(i).getX(), convetPts.get(i).getY());
		}

		convexHull.closePath();

		return convexHull;

	}

	public ArrayList<Point2D> executeConvexHull(ArrayList<Point2D> points) {
		ArrayList<Point2D> xSorted = (ArrayList<Point2D>) points.clone();
		Collections.sort(xSorted, new XCompare());

		int n = xSorted.size();

		Point2D[] lUpper = new Point2D[n];

		lUpper[0] = xSorted.get(0);
		lUpper[1] = xSorted.get(1);

		int lUpperSize = 2;

		for (int i = 2; i < n; i++) {
			lUpper[lUpperSize] = xSorted.get(i);
			lUpperSize++;

			while (lUpperSize > 2
					&& !rightTurn(lUpper[lUpperSize - 3],
							lUpper[lUpperSize - 2], lUpper[lUpperSize - 1])) {
				// Remove the middle point of the three last
				lUpper[lUpperSize - 2] = lUpper[lUpperSize - 1];
				lUpperSize--;
			}
		}

		Point2D[] lLower = new Point2D[n];

		lLower[0] = xSorted.get(n - 1);
		lLower[1] = xSorted.get(n - 2);

		int lLowerSize = 2;

		for (int i = n - 3; i >= 0; i--) {
			lLower[lLowerSize] = xSorted.get(i);
			lLowerSize++;

			while (lLowerSize > 2
					&& !rightTurn(lLower[lLowerSize - 3],
							lLower[lLowerSize - 2], lLower[lLowerSize - 1])) {
				// Remove the middle point of the three last
				lLower[lLowerSize - 2] = lLower[lLowerSize - 1];
				lLowerSize--;
			}
		}

		ArrayList<Point2D> result = new ArrayList<Point2D>();

		for (int i = 0; i < lUpperSize; i++) {
			result.add(lUpper[i]);
		}

		for (int i = 1; i < lLowerSize - 1; i++) {
			result.add(lLower[i]);
		}

		return result;
	}

	private boolean rightTurn(Point2D a, Point2D b, Point2D c) {
		return (b.getX() - a.getX()) * (c.getY() - a.getY())
				- (b.getY() - a.getY()) * (c.getX() - a.getX()) > 0;
	}

	private class XCompare implements Comparator<Point2D> {
		@Override
		public int compare(Point2D o1, Point2D o2) {
			return (new Double(o1.getX())).compareTo(new Double(o2.getX()));
		}
	}

	public static void main(String[] args){
		Rebuild rebuild= new Rebuild();
		List<Point2D> outer = rebuild.getOuter("data/test0000-linecoord[2]");
		outer = rebuild.executeConvexHull((ArrayList<Point2D>) outer);
		Path2D realOuter = new Path2D.Double();
		realOuter.moveTo(outer.get(0).getX(), outer.get(0).getY());
		for (int i = 1; i < outer.size(); i++) {
			realOuter.lineTo(outer.get(i).getX(), outer.get(i).getY());
		}

		realOuter.closePath();

		BufferedImage img = new BufferedImage(800, 600,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();
		g2d.setColor(Color.BLACK);
		g2d.draw(realOuter);
		
		String filename = String.format("data/test0000-linecoord[2]outer.png");
		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}
		
		
	}
	
	
}

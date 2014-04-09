package sn.regiondetect;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import sn.debug.ShowDebugImage;
import sn.recover.LayerGraph;
import sn.recover.ComponentInstance;

/**
 * A complex region constructed with several Regions
 * 
 * @author
 * 
 */
public class ComplexRegion {

	// layer of overlapped simple regions
	final private static int _maxLayer = 7;
	final private static int _minLayer = 4;

	private Region[] _complexRegion;

	private int _caseID;
	private int _regionRad;
	private boolean _exceedBoundary;
	private Point _regionCentre;
	private int _width;
	private int _height;
	private int _currentLayer;

	/**
	 * Constructor takes canvas width and height
	 * 
	 * @param width
	 * @param height
	 */
	public ComplexRegion(int width, int height) {
		_width = width;
		_height = height;
		_complexRegion = this.generateRegions();
		_caseID = -1;
	}

	/**
	 * Constructor takes a file
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public ComplexRegion(String filename) throws IOException {
		_complexRegion = this.rebuildRegionFromFile(filename);
		_caseID = -1;
	}

	// get members

	public Region[] getComplexRegion() {
		return _complexRegion;
	}

	public int getCaseID() {
		return _caseID;
	}

	public int getWidth() {
		return _width;
	}

	public int getHeight() {
		return _height;
	}

	/**
	 * draw a complex region
	 * 
	 * @param c
	 *            Color color
	 * @return img in form of BufferedImage
	 */
	public BufferedImage drawRegion(Color c) {
		BufferedImage img = new BufferedImage(_width, _height,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, _width, _height);

		for (int i = 0; i < _complexRegion.length; i++) {
			if (!_complexRegion[i].isHole()) {
				g2d.setColor(c);

				g2d.fill(_complexRegion[i].getShape());
			} else {
				g2d.setColor(Color.WHITE);
				// GeneralPath p =
				// GeomUtil.getRoundedGeneralPath(regions[i]);
				g2d.fill(_complexRegion[i].getShape());
			}
		}

		return img;

	}

	/**
	 * draw a complex region, use Cyan as default
	 * 
	 * @return img
	 */
	public BufferedImage drawRegion() {
		return drawRegion(Color.CYAN);
	}

	/**
	 * save the image and data of a complex region
	 * 
	 * @return caseID
	 * @throws IOException
	 */
	public int saveRegion() throws IOException {
		int caseCount = 0;
		File file = new File("data/CaseCount.ini");
		if (file.exists()) {

			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				String nCase = null;
				if ((nCase = reader.readLine()) != null) {
					caseCount = Integer.parseInt(nCase);
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (file.createNewFile()) {
				System.out.println("created data/CaseCount.ini");
				BufferedWriter output = new BufferedWriter(new FileWriter(file));
				output.write("0");
				output.close();
			} else {
				System.err.println("failed to created data/CaseCount.ini");
			}
		}

		_caseID = caseCount + 1;
		return saveRegion(_caseID, null, false);

	}

	/**
	 * save the image and data of a complex region
	 * 
	 * @param caseID
	 * @param fileName
	 * @param fixedName
	 *            Boolean value tells if using the input filename
	 * @return caseID
	 * @throws IOException
	 */
	public int saveRegion(int caseID, String fileName, boolean fixedName)
			throws IOException {
		String filename;
		BufferedWriter logOutput, counterOutput;
		_caseID = caseID;
		if (fixedName) {
			filename = fileName;
		} else {
			filename = String.format("data/test%d.log", caseID);
		}

		System.out.println("saving log to " + filename);
		logOutput = new BufferedWriter(new FileWriter(filename));
		logOutput.write("width " + _width
				+ System.getProperty("line.separator"));
		logOutput.write("height " + _height
				+ System.getProperty("line.separator"));
		logOutput.write("nSubRegions " + _complexRegion.length
				+ System.getProperty("line.separator"));
		for (int i = 0; i < _complexRegion.length; i++) {
			logOutput.write("Region " + i
					+ System.getProperty("line.separator"));
			logOutput.write(_complexRegion[i].toString());
		}
		logOutput.close();

		// Save case image without lines
		filename = filename + "-noline.png";

		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(this.drawRegion(), "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}

		// update case counter
		File file = new File("data/CaseCount.ini");
		counterOutput = new BufferedWriter(new FileWriter(file));
		counterOutput.write(String.valueOf(_caseID));
		counterOutput.close();
		return _caseID;
	}

	/**
	 * rebuild a complex region from a file
	 * 
	 * @param filename
	 * @return a complex region in form of Region[]
	 * @throws IOException
	 */
	public Region[] rebuildRegionFromFile(String filename) throws IOException {
		Region[] regions;
		int nSubRegion = 0;
		int lineCount = 0;

		File file = new File(filename);
		if (!file.exists()) {
			System.err.println("ERROR: File " + filename
					+ " does not exist. \nConstruction Failed.");
			System.exit(-1);
		}

		BufferedReader reader = new BufferedReader(new FileReader(file));
		String width_S, height_S, region_S;

		// read width info of canvas
		lineCount++;
		if ((width_S = reader.readLine()) != null) {
			String[] widthInfo = width_S.split(" ");
			if (!widthInfo[0].equals("width")) {
				System.err.println("ERROR: In file " + filename + " line "
						+ lineCount
						+ " expecting width info. \nConstruction Failed.");
				System.exit(-1);
			}
			_width = Integer.parseInt(widthInfo[1]);
		} else {
			System.err.println("ERROR: In file " + filename + " line "
					+ lineCount
					+ " expecting width info. \nConstruction Failed.");
			System.exit(-1);
		}

		// read height info of canvas
		lineCount++;
		if ((height_S = reader.readLine()) != null) {
			String[] heightInfo = height_S.split(" ");
			if (!heightInfo[0].equals("height")) {
				System.err.println("ERROR: In file " + filename + " line "
						+ lineCount
						+ " expecting height info. \nConstruction Failed.");
				System.exit(-1);
			}
			_height = Integer.parseInt(heightInfo[1]);

		} else {
			System.err.println("ERROR: In file " + filename + " line "
					+ lineCount
					+ " expecting height info. \n Construction Failed.");
			System.exit(-1);
		}

		// read number of sub-regions
		lineCount++;
		if ((region_S = reader.readLine()) != null) {
			String[] regionInfo = region_S.split(" ");
			if (!regionInfo[0].equals("nSubRegions")) {
				System.err
						.println("ERROR: In file "
								+ filename
								+ " line "
								+ lineCount
								+ " expecting [number of sub-regions] info. \n Construction Failed.");
				System.exit(-1);
			}
			nSubRegion = Integer.parseInt(regionInfo[1]);

		} else {
			System.err
					.println("ERROR: In file "
							+ filename
							+ " line "
							+ lineCount
							+ " expecting [number of sub-region] info. \n Construction Failed.");
			System.exit(-1);
		}

		regions = new Region[nSubRegion];

		// read sub-region info
		for (int i = 0; i < nSubRegion; i++) {
			lineCount++;
			int[] xCords = null, yCords = null;
			String point_S, layer_S;
			int nExtremePoints = 0;
			int layer = -1;

			// jump region ID
			lineCount++;
			reader.readLine();

			// read number of extreme points in the sub-region
			if ((point_S = reader.readLine()) != null) {
				String[] pointInfo = point_S.split(" ");
				if (!pointInfo[0].equals("nPoints")) {
					System.err
							.println("ERROR: In file "
									+ filename
									+ " line "
									+ lineCount
									+ " expecting number of points in sub-region. \n Construction Failed.");
					System.exit(-1);
				}
				nExtremePoints = Integer.parseInt(pointInfo[1]);
			} else {
				System.err
						.println("ERROR: In file "
								+ filename
								+ " line "
								+ lineCount
								+ " expecting number of points in sub-region. \n Construction Failed.");
				System.exit(-1);
			}

			if ((layer_S = reader.readLine()) != null) {
				String[] layerInfo = layer_S.split(" ");
				if (!layerInfo[0].equals("layer")) {
					System.err
							.println("ERROR: In file "
									+ filename
									+ " line "
									+ lineCount
									+ " expecting layer of sub-region. \n Construction Failed.");
					System.exit(-1);
				}
				layer = Integer.parseInt(layerInfo[1]);
				xCords = new int[nExtremePoints];
				yCords = new int[nExtremePoints];
				for (int j = 0; j < nExtremePoints; j++) {
					String cord_S;
					int x, y;
					lineCount++;

					if ((cord_S = reader.readLine()) != null) {
						String[] cordsInfo = cord_S.split(",");
						x = Integer.parseInt(cordsInfo[0]);
						y = Integer.parseInt(cordsInfo[1]);
						xCords[j] = x;
						yCords[j] = y;
					}

					else {
						System.err.println("ERROR: In file " + filename
								+ " line " + lineCount + " expecting point "
								+ j + ". \n Construction Failed.");
						System.exit(-1);
					}
				}

			} else {
				System.err
						.println("ERROR: In file "
								+ filename
								+ " line "
								+ lineCount
								+ " expecting layer of sub-region. \n Construction Failed.");
				System.exit(-1);
			}

			regions[i] = new Region(xCords, yCords, layer);
		}

		reader.close();
		return regions;
	}

	/**
	 * Generate a set of simple regions with holes
	 * 
	 * @param nRegions
	 * @param width
	 * @param height
	 * @return
	 */
	public Region[] generateRegions() {
		Random r = new Random();
		int nBaseRegion = 20;
		int nRegions = 0;
		int nLayers = r.nextInt(_maxLayer - _minLayer) + _minLayer;
		// Number of regions in each layer
		int nRegionsPerLayer[] = new int[nLayers];

		// Generate number of regions in each layer
		for (int i = 0; i < nLayers; i++) {
			if (i == 0) {
				nRegionsPerLayer[i] = r.nextInt(nBaseRegion - 5) + 5;
				nRegions += nRegionsPerLayer[i];
			} else {
				nRegionsPerLayer[i] = r.nextInt(nRegionsPerLayer[i - 1]) + 1;
				nRegions += nRegionsPerLayer[i];
			}
		}

		int currentRegion = 0; // record current region index

		Region[] regions = new Region[nRegions];

		// Generate regions in each layer
		for (int i = 0; i < nLayers; i++) {
			Point centre = new Point();
			int minRad, maxRad, minPts, maxPts;
			minPts = 8;
			maxPts = 20;
			if (_currentLayer == 0) {
				for (int j = 0; j < nRegionsPerLayer[i]; j++) {
					centre = generateCentre(regions, currentRegion, _width,
							_height);
					minRad = _height / 12;
					maxRad = _height / 4;

					regions[currentRegion] = this.generate(_width, _height,
							centre, minRad, maxRad, minPts, maxPts);
					currentRegion++;
				}
			} else {

				int backtrack = 1;

				for (int j = 0; j < nRegionsPerLayer[i]; j++) {
					minRad = regions[currentRegion - j - backtrack].getRadius() / 2;
					maxRad = regions[currentRegion - j - backtrack].getRadius() * 5 / 6;
					centre = regions[currentRegion - j - backtrack].getCenter();
					regions[currentRegion] = this.generate(_width, _height,
							centre, minRad, maxRad, minPts, maxPts);
					regions[currentRegion - j - backtrack]
							.setNext(regions[currentRegion]);
					regions[currentRegion].setPrev(regions[currentRegion - j
							- backtrack]);
					currentRegion++;
					backtrack++;
				}
			}
			_currentLayer++;
		}

		return regions;
	}

	/**
	 * Generate a centre point that is not in other polygons
	 * 
	 * @param regions
	 * @param nPolygon
	 * @param width
	 * @param height
	 * @return
	 */
	public Point generateCentre(Region[] regions, int nPolygon, int width,
			int height) {
		Random r = new Random();
		Point centre = new Point(width / 2, height / 2);

		int att = 0;
		centre.setLocation(r.nextInt(width), r.nextInt(height));
		while (att <= 2000) {
			boolean inside = false;
			centre.setLocation(r.nextInt(width), r.nextInt(height));
			for (int i = 0; i < nPolygon; i++) {
				if (regions[i].getShape().contains(centre)) {
					inside = true;
					break;
				}
			}
			if (inside)
				centre.setLocation(r.nextInt(width), r.nextInt(height));
			else
				break;
			att++;
		}
		if (att > 2000) {
			centre.setLocation(r.nextInt(width), r.nextInt(height));
		}

		return centre;
	}

	/**
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	public Region generate(int width, int height) {
		return generate(width, height, new Point(width / 2, height / 2),
				height / 6, height / 3, 15, 60);
	}

	public Region generate(int width, int height, Point centre, double minRad,
			double maxRad, int minPts, int maxPts) {
		Random generator = new Random();
		// Set points using a min and max range
		int numPoints = (int) Math.floor(generator.nextDouble()
				* (maxPts - minPts))
				+ minPts;

		int[] yPolyPoints = new int[numPoints];
		int[] xPolyPoints = new int[numPoints];

		// Set the radius using min and max range as well
		int radius = (int) (Math.floor(generator.nextDouble()
				* (maxRad - minRad)) + minRad);
		_regionRad = radius;
		_regionCentre = centre;
		double crAng = 0,

		// Angle between each two points
		angDiff = Math.toRadians(360.0 / numPoints), radJitter = radius / 3.0, angJitter = angDiff * .9;
		_exceedBoundary = false;
		int prevX = centre.x, prevY = centre.y;
		for (int i = 0; i < numPoints; i++) {
			double tRadius = radius
					+ (generator.nextDouble() * radJitter - radJitter / 1.0);
			double tAng = crAng
					+ (generator.nextDouble() * angJitter - angJitter / 1.0);
			int nx = (int) (Math.sin(tAng) * tRadius), ny = (int) (Math
					.cos(tAng) * tRadius);

			double scaleRatio = (double) width / (double) height;

			yPolyPoints[i] = ny + centre.y;
			xPolyPoints[i] = (int) (nx * scaleRatio) + centre.x;

			if (xPolyPoints[i] < 0 || xPolyPoints[i] > width
					|| yPolyPoints[i] < 0 || yPolyPoints[i] > height) {
				xPolyPoints[i] = prevX;
				yPolyPoints[i] = prevY;
				_exceedBoundary = true;
			}

			prevX = xPolyPoints[i];
			prevY = yPolyPoints[i];
			// System.out.print("x : " + nx + " y: " + ny + "\n");

			crAng += angDiff;
		}

		return new Region(xPolyPoints, yPolyPoints, _regionRad, _currentLayer,
				_regionCentre, _exceedBoundary);
	}


	static public void main(String args[]) throws Exception {
		ShowDebugImage frame = null, frameReconstruct = null;
		int width = 800;
		int height = 600;
		ComplexRegion complexRegion = new ComplexRegion(width, height);

		BufferedImage img = complexRegion.drawRegion();
		complexRegion.saveRegion();
		frame = new ShowDebugImage("Regions", img);
		frame.refresh(img);

//		String logName = "data/test" + complexRegion.getCaseID() + ".log";
//		ComplexRegion reconstructedComplexRegion = new ComplexRegion(logName);
//		img = reconstructedComplexRegion.drawRegion(Color.GREEN);
//		frameReconstruct = new ShowDebugImage("Regions from file", img);
//		
		
		//test for layer graph
		BufferedImage imgLayer = new BufferedImage(width, height,
				BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) imgLayer.createGraphics();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, width, height);
		
		LayerGraph layerGraph = new LayerGraph(complexRegion);
		layerGraph.drawComponents(layerGraph.getUnboundedComponent(), imgLayer, Color.RED, false);
		
		frameReconstruct = new ShowDebugImage("Layer graph", imgLayer);
	}

}

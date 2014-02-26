package sn.regiondetect;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.imageio.ImageIO;

import sn.debug.ShowDebugImage;

public class RegionGenerator {

	final static protected int _maxLayer = 7;
	final static protected int _minLayer = 4;
	
	protected int _regionRad;
	protected boolean _exceedBoundary;
	protected Point _regionCentre;
	protected Polygon[] _mainRegions;
	protected int[] _mainRegionRads;
	protected Point[] _mainRegionCentres;
	protected boolean[] _mainRegionExceed;

	protected Polygon[] _holes;
	protected Point[] _holesCentres;
	protected int[] _holeRads;

	protected int _width;
	protected int _height;

	protected int _currentLayer;

	public RegionGenerator() {

	}

	public RegionGenerator(int width, int height) {
		_width = width;
		_height = height;
	}

	/**
	 * 
	 * @param nRegions
	 * @param width
	 * @param height
	 * @return
	 */
	public Region[] generateRegions(int width, int height) {
		Random r = new Random();
		int nRegions = 0;
		int nLayers = r.nextInt(_maxLayer - _minLayer) + _minLayer;

		// Number of regions in each layer
		int nRegionsPerLayer[] = new int[nLayers];

		// Generate number of regions in each layer
		for (int i = 0; i < nLayers; i++) {
			if (i == 0) {
				nRegionsPerLayer[i] = r.nextInt(20-5) + 5;
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
					centre = generateCentre(regions, currentRegion, width,
							height);
					minRad = height / 12;
					maxRad = height / 4;

					regions[currentRegion] = this.generate(width, height,
							centre, minRad, maxRad, minPts, maxPts);
					currentRegion++;
				}
			} else {
				
				int backtrack = 1;
				for (int j = 0; j < nRegionsPerLayer[i]; j++) {
					minRad = regions[currentRegion-j-backtrack].getRadius() / 2;
					maxRad = regions[currentRegion-j-backtrack].getRadius() * 5 / 6;
					centre = regions[currentRegion-j-backtrack].getCenter();
					regions[currentRegion] = this.generate(width, height,
							centre, minRad, maxRad, minPts, maxPts);
					regions[currentRegion-j-1].setNext(regions[currentRegion]);
					regions[currentRegion].setPrev(regions[currentRegion-j-1]);
					currentRegion++;
					backtrack++;
				}
			}
			_currentLayer++;
		}

		return regions;
	}

	/**
	 * Generate a point that is not in other polygons
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
			//centre.setLocation(-1, -1);
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

		// Angle between each point
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

		return new Region(xPolyPoints, yPolyPoints, numPoints, _regionRad,
				_currentLayer, _regionCentre, _exceedBoundary);
	}

	static public void main(String args[]) throws Exception {
		ShowDebugImage frame = null;
		int width = 800;
		int height = 600;
		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		RegionGenerator rg = new RegionGenerator();

		Region[] regions = rg.generateRegions(width, height);

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, width, height);
		for (int i = 0; i < regions.length; i++) {
			g2d.setColor(Color.BLACK);
			// g2d.fill(regions[i]);
			g2d.draw(regions[i]._path);
		}

		frame = new ShowDebugImage("Regions", img);
		frame.refresh(img);

		String imgFilename = String.format("img%04d.png", 1);
		System.out.println("saving image to " + imgFilename);
		try {
			ImageIO.write(img, "png", new File(imgFilename));
		} catch (IOException e) {
			System.err.println("failed to save image " + imgFilename);
			e.printStackTrace();
		}

	}
}

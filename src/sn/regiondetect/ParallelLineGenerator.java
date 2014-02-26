package sn.regiondetect;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import sn.debug.ShowDebugImage;

public class ParallelLineGenerator {
	double _angle; // in radian

	// pixel
	int _gap;
	int _height;
	int _width;

	/**
	 * 
	 * @param angle
	 * @param nLines
	 * @param height
	 * @param width
	 */
	public ParallelLineGenerator(double angle, int gap, int height, int width) {
		_angle = angle;
		_gap = gap;
		_height = height;
		_width = width;
	}

	/**
	 * 
	 * @return
	 */
	public List<Line2D> generateParallelLines() {
		List<Line2D> lines = new ArrayList<Line2D>();
		double xOffset = Math.tan(_angle) * _height;
		double gapInX = Math.abs(_gap / Math.cos(_angle));
		int nlines = (int) ((_width + Math.abs(xOffset)) / gapInX) + 1;
		double xTop;
		double xButtom;
		if (xOffset >= 0) {
			xTop = 0 - xOffset;
			xButtom = 0;
		}
		else{
			xTop = 0;
			xButtom = 0 + xOffset;
		}
		Line2D l2d;
		for (int i = 0; i < nlines; i++) {

			if (i == 0) {
				l2d = new Line2D.Double(xTop, 0, xButtom, _height);
			} else {
				xTop += gapInX;
				xButtom += gapInX;
				l2d = new Line2D.Double(xTop, 0, xButtom, _height);

			}
			lines.add(l2d);
		}

		return lines;
	}

	static public void main(String args[]) {
		ShowDebugImage frame = null;
		int width = 800;
		int height = 600;
		BufferedImage img = new BufferedImage(width, height,
				BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();

		ParallelLineGenerator plg = new ParallelLineGenerator(Math.PI / 11.5,
				10, height, width);
		List<Line2D> lines = plg.generateParallelLines();

		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, width, height);
		for (int i = 0; i < lines.size(); i++) {
			g2d.setColor(Color.BLACK);
			Line2D l = lines.get(i);
			// g2d.fill(regions[i]);
			g2d.drawLine((int) l.getX1(), (int) l.getY1(), (int) l.getX2(),
					(int) l.getY2());
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

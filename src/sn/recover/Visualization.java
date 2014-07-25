package sn.recover;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import sn.debug.ShowDebugImage;
import sn.regiondetect.ComplexRegion;
import sn.regiondetect.GeomUtil;
import sn.regiondetect.Region;

public class Visualization {
	public String id;
	
	public Visualization(){
		id = getCurrentTimeStamp();
	}
	
	
	public static String getCurrentTimeStamp() {
	    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd-HHmm");//dd/MM/yyyy
	    Date now = new Date();
	    String strDate = sdfDate.format(now);
	    return strDate;
	}
	
	public void visualizeGroundTruth(BenchmarkInstance se){
		
		// visualize the ground truth 
		BufferedImage img = se.getGroundTruth().drawRegion(Color.LIGHT_GRAY);
		Graphics2D g2d = (Graphics2D) img.createGraphics();
				
		se.firstMeasurement.addIntervalsToGraphic(g2d, se.firstMeasurement.getPositiveIntervals(), false,
				Color.BLUE);
		se.secondMeasurement.addIntervalsToGraphic(g2d, se.secondMeasurement.getPositiveIntervals(), false,
				Color.RED);
		
		System.out.println("Interval added to graphics");
				
		// show it
		ShowDebugImage frame = new ShowDebugImage("G", img);
		frame.refresh(img);
		
		String filename = "experiments/" + id + "-groundTruth.png";
		
		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}
	}
	
	public void drawTwoConvexHulls(SensorData thisData, SensorData otherData, String str){
		
		// get the convex hull
		List<Point2D> thisHull = thisData.getConvexHull();
		List<Point2D> otherHull = otherData.getConvexHull();
		
		List<Point2D> fullList = new ArrayList<Point2D>(thisHull);
		fullList.addAll(otherHull);
		
		// variables denoting the parameters of affine transformation
		double minX=Double.MAX_VALUE, minY=Double.MAX_VALUE,maxX=Double.MIN_VALUE, maxY=Double.MIN_VALUE;
		
		for (int i=0; i<fullList.size(); i++){
			Point2D pt1 = fullList.get(i);
			minX =  Math.min(pt1.getX(),minX);
			minY =  Math.min(pt1.getY(),minY);
			maxX =  Math.max(pt1.getX(),maxX);
			maxY =  Math.max(pt1.getY(),maxY);
		}
		
		double dx=0, dy=0;
		
		if (maxX < 0) dx = -maxX+50;
		if (maxY < 0) dy = -maxY+50; 
		if (minX < 0) dx += -minX+50;
		if (minY < 0) dy += -minY+50;
		
		int newWidth = (int)(maxX + dx), newHeight = (int)(maxY+dy);
		
		Path2D thisHullPath = new Path2D.Double();
		thisHullPath.moveTo(thisHull.get(0).getX() + dx, thisHull.get(0).getY()
				+ dy);
		for (int i = 1; i < thisHull.size(); i++) {
			thisHullPath.lineTo(thisHull.get(i).getX() + dx, thisHull.get(i).getY()
					+ dy);
		}
		thisHullPath.lineTo(thisHull.get(0).getX() + dx, thisHull.get(0).getY()
				+ dy);
		
		Path2D otherHullPath = new Path2D.Double();
		otherHullPath.moveTo(otherHull.get(0).getX() + dx, otherHull.get(0).getY()
				+ dy);
		for (int i = 1; i < otherHull.size(); i++) {
			otherHullPath.lineTo(otherHull.get(i).getX() + dx, otherHull.get(i).getY()
					+ dy);
		}
		otherHullPath.lineTo(otherHull.get(0).getX() + dx, otherHull.get(0).getY()
				+ dy);
		
		Line2D thisLongest = thisData.getLongestDimension(thisHull);
		Line2D otherLongest = otherData.getLongestDimension(otherHull);
		
		// draw the image
		BufferedImage img = new BufferedImage(newWidth, newHeight,
						BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = (Graphics2D) img.createGraphics();
		g2d.setColor(Color.RED);
		g2d.draw(thisHullPath);
		g2d.draw(thisLongest);
		g2d.setColor(Color.BLUE);
		g2d.draw(otherHullPath);
		g2d.draw(otherLongest);
		
		String filename = "experiments/" + getCurrentTimeStamp() + "twoConvexHull.png";
		
		System.out.println("saving image to " + filename);
		try {
			ImageIO.write(img, "png", new File(filename));
		} catch (IOException e) {
			System.err.println("failed to save image " + filename);
			e.printStackTrace();
		}
		
		ShowDebugImage frame = new ShowDebugImage(str, img);
		frame.refresh(img);
	}
}

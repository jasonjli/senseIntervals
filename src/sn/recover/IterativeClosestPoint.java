/**
 * 
 * This implements the IterativeClosestPoint Algorithm
 * It was modified from http://forums.codeguru.com/showthread.php?523241-Iterative-Closest-Point-Implmentation
 * It implements the algorithm described in http://eecs.vanderbilt.edu/courses/CS359/other_links/papers/1992_besl_mckay_ICP.pdf
 * 
 */

package sn.recover;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Jama.*;

import java.awt.geom.Point2D;
import java.awt.geom.AffineTransform;


public class IterativeClosestPoint
{
	/**
	 * P=first array
	 * X=second array
	 */
	private Point2D[] x, p;

	//private LocalState originalState;


	public IterativeClosestPoint(List<Point2D> newPoints)//, LocalState originalState)
	{
		p = newPoints.toArray(new Point2D[0]);
		//this.originalState = originalState;
	}

	public void runAlgorithm(List<Point2D> newPoints)
	{
		x = getClosestPoints(newPoints);

		Point2D up = getExpectedValue(p);
		Point2D ux = getExpectedValue(x);
		
		// System.out.println(up.print()+" "+ux.print());

		double[][] covarianceMatrix = this.getCovarianceMatrix(p, x, up, ux);
		double[][] q = this.getQ(covarianceMatrix);
		double[] eigenVector = this.getMaxEigenVector(q);
		double[][] rotationMatrix = this.getRotationMatrix(eigenVector);
		double[] translationalVector = this.getTranslationVector(ux, rotationMatrix, up);

		AffineTransform aTransform = new AffineTransform();
		aTransform.translate(translationalVector[0], translationalVector[1]);
		
		
		//System.out.println(Arrays.toString(eigenVector));
		//System.out.println("Rot Matrix: ");
		//for(double[] d :rotationMatrix)
		//{
		//	System.out.println(Arrays.toString(d));
		//}
		//System.out.println(Arrays.toString(translationalVector));
/*
		return new PhysicalState(originalState.getX() + translationalVector[0], originalState.getY() + translationalVector[1], originalState.getOrient());
*/
	}
	/*
	private LocalState applyTransformation(LocalState original ,double[][] rotationMatrix, double[] translationalVector)
	{

	}*/

	/**
	 * Finds the closest points to every Point2D in p.
	 * @return
	 */
	private Point2D[] getClosestPoints(List<Point2D> newPoints)
	{
		Point2D[] x = new Point2D[p.length];

		int i = 0;
		for(Point2D pose : p)
		{
			Point2D closest = null;
			double dist = Double.POSITIVE_INFINITY;

			for(Point2D potential : newPoints)
			{
				double distance = pose.distance(potential);
				if(distance < dist)
				{
					closest = potential;
					dist = distance;
				}
			}
			x[i]=closest;
			//System.out.println("i: "+i+" "+p[i].print()+" "+x[i].print());
			i++;
		}

		return x;
	}

	private Point2D getExpectedValue(Point2D[] u)
	{
		double x=0, y=0;

		for(int i =0; i < u.length; i++)
		{
			x += u[i].getX();
			y += u[i].getY();
		}

		x /= u.length;
		y /= u.length;

		return new Point2D.Double(x,y);
	}

	private double[][] getCovarianceMatrix(Point2D[] p, Point2D[] x, Point2D up, Point2D ux)
	{
		if(p.length!=x.length)
			return null;

		double[][] cov = new double[3][3];

		for(int i = 0; i < p.length; i++)
		{
			cov[0][0] += p[i].getX()*x[i].getX();
			cov[0][1] += p[i].getY()*x[i].getX();
			cov[1][0] += p[i].getX()*x[i].getY();
			cov[1][1] += p[i].getY()*x[i].getY();
		}

		cov[0][0] /= p.length;
		cov[0][1] /= p.length;
		cov[1][0] /= p.length;
		cov[1][1] /= p.length;

		cov[0][0] -= up.getX()*ux.getX();
		cov[0][1] -= up.getY()*ux.getX();
		cov[1][0] -= up.getX()*ux.getY();
		cov[1][1] -= up.getY()*ux.getY();		

		Matrix covM = new Matrix(cov);
		covM = covM.transpose();
		cov = covM.getArray();
		
		return cov;
	}

	private double getTrace(double[][] e)
	{
		double trace = 0;
		for(int i =0; i < e.length; i++)
		{
			trace+=e[i][i];
		}

		return trace;
	}

	private double[][] getQ(double[][] e)
	{
		System.out.println("E: ");
		for(double[] d : e)
		{
			System.out.println(Arrays.toString(d));
		}
		System.out.println();
		
		double[][] q = new double[4][4];

		q[0][0] = getTrace(e);

		q[1][0] = e[1][2] - e[2][1];
		q[2][0] = e[2][0] - e[0][2];
		q[3][0] = e[0][1] - e[1][0];

		q[0][1] = q[1][0];
		q[0][2] = q[2][0];
		q[0][3] = q[3][0];

		for(int i =0; i < 3; i++)
		{
			for(int j =0; j < 3; j++)
			{
				q[1+i][1+j] = e[i][j] + e[j][i] - (i==j?q[0][0]:0);
			}
		}

		return q;
	}

	private double[] getMaxEigenVector(double[][] q)
	{
		System.out.println("Q: ");
		for(double[] d :q)
		{
			System.out.println(Arrays.toString(d));
		}
		System.out.println();
		
		Matrix m = new Matrix(q);

		EigenvalueDecomposition evd = new EigenvalueDecomposition(m);

		double[] eigenValues = evd.getRealEigenvalues();
		double max = Double.NEGATIVE_INFINITY;
		int index = 0;

		for(int i =0; i < eigenValues.length; i++)
		{
			System.out.println("EV: "+eigenValues[i]);
			System.out.println(Arrays.toString(evd.getV().transpose().getArray()[i]));
			if(eigenValues[i]>max)
			{
				max = eigenValues[i];
				index = i;
			}
		}

		return evd.getV().transpose().getArray()[index];
	}

	public double[][] getRotationMatrix(double[] rotationVector)
	{
		double[][] r = new double[3][3];
		double[] rv = rotationVector;

		r[0][0] = rv[0]*rv[0] + rv[1]*rv[1] - rv[2]*rv[2] - rv[3]*rv[3];
		r[1][1] = rv[0]*rv[0] + rv[2]*rv[2] - rv[1]*rv[1] - rv[3]*rv[3];
		r[2][2] = rv[0]*rv[0] + rv[3]*rv[3] - rv[1]*rv[1] - rv[2]*rv[2];

		r[0][1] = 2 * ( rv[1]*rv[2] - rv[0]*rv[3]);
		r[0][2] = 2 * ( rv[1]*rv[3] + rv[0]*rv[2]);

		r[1][0] = 2 * ( rv[1]*rv[2] + rv[0]*rv[3]);
		r[1][2] = 2 * ( rv[2]*rv[3] - rv[0]*rv[1]);

		r[2][0] = 2 * ( rv[1]*rv[3] - rv[0]*rv[2]);
		r[2][1] = 2 * ( rv[2]*rv[3] + rv[0]*rv[1]);

		return r;
	}

	public double[] getTranslationVector(Point2D ux, double[][] r, Point2D up)
	{
		double[] t = new double[3];

		t[0] = ux.getX() - (r[0][0]*up.getX() + r[0][1]*up.getY());
		t[1] = ux.getY() - (r[1][0]*up.getX() + r[1][1]*up.getY());

		return t;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		List<Point2D> orig = new ArrayList<Point2D>();
		orig.add(new Point2D.Double(5,0));
		orig.add(new Point2D.Double(10,0));
		orig.add(new Point2D.Double(0,5));


		List<Point2D> newp = new ArrayList<Point2D>();
		newp.add(new Point2D.Double(4.98 , .436));
		newp.add(new Point2D.Double(9.96,0.872));
		newp.add(new Point2D.Double(-.436, 4.98));

		IterativeClosestPoint a = new IterativeClosestPoint(orig);//, new PhysicalState(0,0,0));

		//System.out.println(.print());

                a.runAlgorithm(newp);
	}
        
        
}
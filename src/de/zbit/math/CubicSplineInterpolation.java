/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.TableResult;
import de.zbit.data.mRNA.mRNATimeSeries;
import de.zbit.util.objectwrapper.ValueTriplet;

/**
 * Interpolate discrete datapoints with natural cubic splines.
 * @author Felix Bartusch
 * @version $Rev$
 */
public class CubicSplineInterpolation extends TimeSeriesModel {
	
	// Following arrays hold the information for every third order polynomial
	// The third order polynomial q_i between points [x_i, y_i] and [x_(i+1), y_(i+1)] can be written as:
	// q_i = (1-t)*y_i + t*y_(i+1) + t*(1-t) * (a_i*(1-t) + b_i * t) ,where
	// t = (x-x_i) / (x_(i+1)-x_i)
	
	/**
	 * The a_i coefficients for the third order polynomial.
	 */
	double[] a;
	/**
	 * The b_i coefficients for the third order polynomial.
	 */
	double[] b;
	
	/**
	 * Distance in x-direction between two points.
	 * dx[0] = x[1] - x[0], 
	 * dx[1] = x[2] - x[1] and so on.
	 * The last entry of the array is 0 and is not needed.
	 */
	double[] dx;

	public CubicSplineInterpolation() {
	}
	
	public CubicSplineInterpolation(mRNATimeSeries mRNA, List<ValueTriplet<Double, String, SignalType>> tp) {	
		super(mRNA.getName(), mRNA.getID(), tp.get(0).getC()); // mRNA.getName() returns null, better mRNA.getProbeName()
		generateModel(mRNA, tp);
	}

	@Override
	public void generateModel(mRNATimeSeries dataPoints,
			List<ValueTriplet<Double, String, SignalType>> timePoints) {
		int numPoints = dataPoints.getNumberOfSignals();
		
		// Get the points (x, f(x))
		this.x = new double[numPoints];
		this.y = new double[numPoints];
		this.dx = new double[numPoints];		// distance between two time points
		
		for(int i=0; i<numPoints; i++) {
			x[i] = timePoints.get(i).getA();			// the i-th timePoints
			y[i] = Double.valueOf(dataPoints.getSignalValue(timePoints.get(i).getC(), timePoints.get(i).getB()).toString());
			dx[i] = i+1==numPoints?0:timePoints.get(i+1).getA()-timePoints.get(i).getA();
		}
		
		// Compute the tridiagonal linear equation system m*k=b
		double[][] m = new double[numPoints][numPoints];
		double[] b = new double[numPoints];		
		// First row of m
		m[0][0] = 2 / dx[0];
		m[0][1] = 1 / dx[0];
		// All other rows of m but the last
		for(int i=1; i<numPoints-1; i++) {
			m[i][i-1] = 1 / dx[i-1];
			m[i][i] = 2 * (1/dx[i-1] + 1/dx[i]);
			m[i][i+1] = 1 / dx[i];
		}
		// The last row of m
		m[numPoints-1][numPoints-2] = 1 / dx[numPoints-2];
		m[numPoints-1][numPoints-1] = 2 / dx[numPoints-2];
		
		// Compute b
		b[0] = 3 * (y[1]-y[0]) / (dx[0] * dx[0]);
		for(int i=1; i<numPoints-1; i++) {
			b[i] = 3 * ( (y[i]-y[i-1])/(dx[i-1]*dx[i-1]) + (y[i+1]-y[i])/(dx[i]*dx[i]) );
		}
		b[numPoints-1] = 3 * ( (y[numPoints-1]-y[numPoints-2])/(dx[numPoints-2]*dx[numPoints-2]) );
		
		// Create the matrices and solve the equation system
		RealMatrix coefficients = new Array2DRowRealMatrix(m);
		DecompositionSolver solver = new LUDecomposition(coefficients).getSolver();
		
		RealVector constants = new ArrayRealVector(b, false);
		RealVector solution = solver.solve(constants);
		
		// Compute the coefficients a_i and b_i
		this.a = new double[numPoints	];
		this.b = new double[numPoints];
		for(int i=0; i<numPoints-1; i++) {													// we just need numPoints-1 polynomials
			this.a[i] = solution.getEntry(i) * dx[i] - (y[i+1] - y[i]);
			this.b[i] = -solution.getEntry(i+1) * dx[i] + (y[i+1] - y[i]);
		}
	}
	
	@Override
	public double computeValueAtTimePoint(double timePoint) {
		// In which range is the time point?
		int range = 0;
		
		// Is the timePoint outside of known timePoints? (extrapolation)
		if(timePoint < x[0] || timePoint > x[x.length-1]) {
			// Model this timePoint with the first or the last polynome
			range = timePoint < x[0] ? 1 : x.length;
		}
		
		for(int i=0; i<x.length; i++){
			range++;																					// is the timePoint in this intervall?
			if(timePoint >= x[i] && timePoint <= x[i+1]) {		// timePoint is in the intervall i. First range indexed with 1
				break;				
			}																									// Otherwise look into the next intervall
		}
		
		// t is often used
		double t = (timePoint-x[range-1]) / dx[range-1];
		
		// return the value of third order polonomial at given timePoint
		return (1-t)*y[range-1] + t*y[range] + t*(1-t) * (a[range-1]*(1-t)+b[range-1]*t);
	}
	
	
	// This main method is just for testing the implementation
	public static void main(String args[]) {
		// generateModel(mRNATimeSeries dataPoints, List<ValueTriplet<Double, String, SignalType>> timePoints)
		// This is the example of Wikipedia: http://en.wikipedia.org/wiki/Spline_interpolation
		mRNATimeSeries m = new mRNATimeSeries("test");
		m.addSignal(0.5, "-1", Signal.SignalType.FoldChange);
		m.addSignal(0.0, "0", Signal.SignalType.FoldChange);
		m.addSignal(3.0, "3", Signal.SignalType.FoldChange);
		
		// timePoints
		List<ValueTriplet<Double, String, SignalType>> tp = new ArrayList<ValueTriplet<Double,String,SignalType>>(3);
		tp.add(new ValueTriplet<>(-1.0,"-1",Signal.SignalType.FoldChange));
		tp.add(new ValueTriplet<>(0.0,"0",Signal.SignalType.FoldChange));
		tp.add(new ValueTriplet<>(3.0,"3",Signal.SignalType.FoldChange));
		
		CubicSplineInterpolation c = new CubicSplineInterpolation(m, tp);
		
		for(double i=-1.0; i<=3; i=i+0.01) {
			System.out.println(c.computeValueAtTimePoint(i));
		}
	}
}

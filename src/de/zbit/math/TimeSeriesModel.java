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

import javax.swing.JComponent;

import de.zbit.data.Signal.SignalType;
import de.zbit.data.Signal;
import de.zbit.data.TableResult;
import de.zbit.data.mRNA.mRNATimeSeries;
import de.zbit.util.objectwrapper.ValueTriplet;

/**
 * Abstract class for time series models.
 * i.e. model methods that are used to interpolate discrete datapoints for a later visualization
 * and to model the course of gene expression.
 * @author Felix Bartusch
 * @version $Rev$
 */
public abstract class TimeSeriesModel {
	
	/**
	 * Name of the modeled gene.
	 */
	String name;
	
	/**
	 * geneID of the modeled gene.
	 */
	int geneID;
	
	/**
	 * Type of the modeled values. (p-value or fold change)
	 */
	SignalType signalType;
	
	/**
	 * The x-values for the given points.
	 */
	double[] x;
	
	/**
	 * The shift value for logarithmized time points.
	 */
	double shift;
	
	/**
	 * The y-values for the given points.
	 */
	double[] y;
	
	/**
	 * Number of measured data points.
	 */
	int numDataPoints;
	
	/**
	 * A cutoff value
	 */
	double cutoff;
	
	/**
	 * Are the time points logarithmized and shifted?
	 * The logarithmization is just internally for this class.
	 * If the first time point is 0.01 and the time points are
	 * exponentially distributed, the first time point will be 
	 * internally handled as 1 (shift is then 4).
	 * If another class asks for the first time point, 0.01 is returned.
	 */
	boolean isExponentiallyDistributed;
	
	/**
	 * Is the model method initialized?
	 * After {@link #init(Collection))} has been called successfully,
	 * the flag should be set.
	 */
	private boolean initialized = false;

	public TimeSeriesModel() {
		this.name = null;
		this.geneID = -1;
	}
	
	/**
	 * @param name of the modeled gene.
	 * @param geneID of the modeled gene.
	 */
	public TimeSeriesModel(String name, int geneID, SignalType type) {
		this.name = name;
		this.geneID = geneID;
		this.signalType = type;
	}
	
	/**
	 * Generate the model for one gene.
	 * @param isExponentiallyDistributed 
	 * @param cutoff 
	 * @return 
	 */
	public abstract void generateModel(mRNATimeSeries dataPoints, List<ValueTriplet<Double, String, SignalType>> timePoints,
			double cutoff, boolean isExponentiallyDistributed);
	
	/**
	 * Compute the interpolated value for certain coefficients at a certain time point.
	 */
	public abstract double computeValueAtTimePoint(double timePoint);
	
	
	/**
	 * Extract the time points from the user input. If they are exponentially distributed,
	 * logarithmize and shift the time points, so that the x-values are more
	 * uniform distributed. The first time point will then be set on 1.
	 * Set also the y-values.
	 */
	protected void processTimePoints(mRNATimeSeries dataPoints,
			List<ValueTriplet<Double, String, SignalType>> timePoints,
			boolean isExponentiallyDistributed) {
		
		// How many data points are there?
		int numPoints = timePoints.size();
		this.x = new double[numPoints];
		this.y = new double[numPoints];
			
		// How far are the time points shifted, if the first logarithmized time point is negative
		shift = 0;
		if(isExponentiallyDistributed) {
			double logValue = Math.log10(timePoints.get(0).getA());
			if(logValue < 0)
				shift = 1 - logValue;
		}
		
		// Set the x- and y-values
		for(int i=0; i<numPoints; i++) {
			if(!isExponentiallyDistributed) {
				x[i] = timePoints.get(i).getA();			// the i-th timePoints
				y[i] = Double.valueOf(dataPoints.getSignalValue(timePoints.get(i).getC(), timePoints.get(i).getB()).toString());
			} else {
				x[i] = Math.log10(timePoints.get(i).getA()) + shift;
				y[i] = Double.valueOf(dataPoints.getSignalValue(timePoints.get(i).getC(), timePoints.get(i).getB()).toString());
			}
		}
	}
	
	
	/**
	 * This is a initialization for the model method.
	 * The {@link #generateModel(mRNATimeSeries, List) generateModel}
	 * method models just one mRNA time series. However, some model methods use the whole
	 * set of mRNA time series to infer their model parameters etc.
	 * The method {@link #init(Collection) init} stores its result, so that it has to be
	 * computed once.
	 * @param data The data to be modeled.
	 * @throws Exception 
	 */
	public void init(List<mRNATimeSeries> data,
			List<ValueTriplet<Double, String, SignalType>> timePoints) {
		// Intentially left blank for model methods, that need no initialization.
		setInitialized(true);
	}
	
	/**
	 * @return the first timePoint that can be modelled by this model.
	 */
	public double getFirstTimePoint() {
		if(isExponentiallyDistributed) {
			return Math.pow(10, x[0] - shift);
		} else {
			return x[0];			
		}
	}
	
	/**
	 * @return the last timePoint that can be modelled by this model.
	 */
	public double getLastTimePoint() {
		if(isExponentiallyDistributed) {
			return Math.pow(10, x[x.length-1] - shift);
		} else {
			return x[x.length-1];		
		}
	}
	
	
	/**
	 * Get a certain amount of time points. If the original time points were exponentially
	 * distributed, the returned time points are also exponentially distributed, with exponents
	 * that are equally distributed between the smallest and the larges exponent of the original
	 * time points.
	 * @param n The number of time points to return.
	 * @return the time points.
	 */
	public double[] getDistributedTimePoints(int n) {
		double[] res = new double[n];
		
		// First and last time point, stepsize between the distributed time points
		double begin = x[0];
		double end = x[x.length - 1];		
		double step = (end - begin) / (n-1);

		// The n distributed time points
		for(int i=0; i<n; i++) {
			res[i] = begin + (i * step);
		}
		
		// If the original time points were exponentially distributed, the returned
		// time points are also exponentially distributed.
		if(isExponentiallyDistributed) {
			for(int i=0; i<n; i++) {
				res[i] = untranslateTimePoint(res[i]);
			}
		}
		
		return res;
	}
	
	
	/**
	 * Get the original time points.
	 * @return Array containing the original time points.
	 */
	public double[] getOriginalTimePoints() {
		double[] res = new double[x.length];
		
		if(!isExponentiallyDistributed) {
			// The time points are not exponentially distributed,
			// therefore they were not translated and can be returned
			// without re-translation
			return x;
		} else {
			// The time points are exponentially distributed,
			// therefore the were translated and have to be
			// re-translated
			for(int i=0; i<x.length; i++) {
				res[i] = untranslateTimePoint(x[i]);
			}
			
			return res;
		}
	}
	
	/**
	 * Translate a time point to its logarithmized and shifted value.
	 */
	public double translateTimePoint(double timePoint) {
		return Math.log10(timePoint) + shift;
	}
	
	/**
	 * Untranslate a time point from its logarithmized and shifted value
	 * to its original value.
	 */
	public double untranslateTimePoint(double timePoint) {
		return Math.pow(10, timePoint - shift);
	}

	/**
	 * @return name of the modeled gene.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return geneID of the modeled gene.
	 */
	public int getGeneID() {
		return geneID;
	}
	
	/**
	 * @return signal type of the modeled values. (SignalType.FoldChange or SignalType.pValue)
	 */
	public SignalType getSignalType() {
		return signalType;
	}
	
	public boolean isExponentiallyDistributed() {
		return this.isExponentiallyDistributed;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setGeneID(int geneID) {
		this.geneID = geneID;
	}

	public void setSignalType(SignalType signalType) {
		this.signalType = signalType;
	}
	
	public double[] getY() {
		return y;
	}
	
	public boolean isInitialized() {
		return initialized;
	}

	protected void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}
	
	/**
	 * A {@link JComponent} asking for parameters used by the model method.
	 * (e.g. maximum number of iterations for the {@link TimeFit} model method.)
	 * @return A {@link JComponent} asking for several parameters or null of no
	 * further parameters are needed by the method.
	 */
	public JComponent getIndividualSettingsPanel() {
		return null;
	}
	
	/**
	 * Fulfills a mRNATimeSeries the cutoff value?
	 */
	public static boolean geneFulfillsCutoff(mRNATimeSeries m, SignalType signalType, double cutoff) {
		for(Signal s : m.getSignals()) {
			// Handle pValues
			if(signalType == SignalType.pValue && s.getSignal().doubleValue() < cutoff) {
				return true;
			}
			// Handle Fold changes
			else if(signalType == SignalType.FoldChange && Math.abs(s.getSignal().doubleValue()) >= cutoff) {
				return true;
			}
		}
		
		// No signal fulfills cutoff
		return false;
	}
}

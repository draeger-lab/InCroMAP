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

import java.util.List;

import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNATimeSeries;
import de.zbit.util.objectwrapper.ValueTriplet;

/**
 * Generic interface for time series models, i.e.,
 * methods that are used to interpolate discrete datapoints for a later visualization.
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
	 * The y-values for the given points.
	 */
	double[] y;
	
	
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
	 */
	public abstract void generateModel(mRNATimeSeries dataPoints, List<ValueTriplet<Double, String, SignalType>> timePoints);
	
	/**
	 * Compute the interpolated value for certain coefficients at a certain time point.
	 */
	public abstract double computeValueAtTimePoint(double timePoint);	
	
	/**
	 * @return the first timePoint that can be modelled by this model.
	 */
	public double getFirstTimePoint() {
		return x[0];
	}
	
	/**
	 * @return the last timePoint that can be modelled by this model.
	 */
	public double getLastTimePoint() {
		return x[x.length-1];
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
}


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
public interface TimeSeriesModel {
	
	/**
	 * Generate the model for one gene.
	 */
	public void generateModel(mRNATimeSeries dataPoints, List<ValueTriplet<Double, String, SignalType>> timePoints);
	
	/**
	 * Compute the interpolated value for certain coefficients at a certain time point.
	 */
	public double computeValueAtTimePoint(double timePoint);	
}

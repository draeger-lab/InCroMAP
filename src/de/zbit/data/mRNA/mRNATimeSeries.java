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
package de.zbit.data.mRNA;

import java.util.List;

import de.zbit.data.Signal;
import de.zbit.data.id.GeneID;

/**
 * A class to hold {@link mRNA} probes with {@link Signal}s and {@link GeneID}s.
 * There are currently no new methods/fields in this class. With this class the toolbar can visualize specific Buttons for
 * time series data.
 * @author Felix Bartusch
 * @version $Rev$
 */
public class mRNATimeSeries extends mRNA {
	private static final long serialVersionUID = -1225343797045535396L;

	public mRNATimeSeries(String name) {
		super(name);
	}

	public mRNATimeSeries(String probeName, String geneName, int geneID) {
		super(probeName, geneName, geneID);
	}

	public mRNATimeSeries(String name, int geneID) {
		super(name, geneID);
	}
	
	/**
	 * Get the signals of this mRNA time series as an array of double values.
	 * @return A double[], containing the signals as double values.
	 */
	public double[] SignalsAsArray() {
		// Get the signals.
		List<Signal> signals = this.getSignals();
		
		double[] res = new double[signals.size()];
		for(int i=0; i<signals.size(); i++) {
			res[i] = signals.get(i).getSignal().doubleValue();
		}
		
		return res;
	}
}

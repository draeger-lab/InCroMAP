/*
  * $Id:  CompoundID.java 11:57:26 rosenbaum $
  * $URL: CompoundID.java $
  * ---------------------------------------------------------------------
  * This file is part of Integrator, a program integratively analyze
  * heterogeneous microarray datasets. This includes enrichment-analysis,
  * pathway-based visualization as well as creating special tabular
  * views and many other features. Please visit the project homepage at
  * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
  * obtain the latest version of Integrator.
  *
  * Copyright (C) 2011-2013 by the University of Tuebingen, Germany.
  *
  * Integrator is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation. A copy of the license
  * agreement is provided in the file named "LICENSE.txt" included with
  * this software distribution and also available online as
  * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
  * ---------------------------------------------------------------------
  */

package de.zbit.data.compounds;

/**
 * Interface for an object that can be assigned an compound identifier
 * The compound identifier is equal to the HMDB ID without the prefix 'HMDB'
 * 
 * @author Lars Rosenbaum
 * @version $Rev$
 */

public interface CompoundID {
	
	/**
	 * The key to use in the {@link #addData(String, Object)} map to add
	 * the corresponding Compound ID.
	 */
	public final static String compound_id_key = "Compound_ID";

	/**
	 * Means compound id has not been set.
	 */
	public final static Integer default_compoundID = -1;

	/**
	 * Set the corresponding Compound ID.
	 * @param compoundID
	 */
	public void setCompoundID(int compoundID);

	/**
	 * @return associated Compound ID.
	 */
	public int getCompoundID();
}

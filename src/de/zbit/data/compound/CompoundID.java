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
package de.zbit.data.compound;

import java.util.Collection;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;

/**
 * Interface for objects that can be assigned to HMDB-Compound-IDs.
 * 
 * <p>If implementing this interface, please also override
 * the {@link NameAndSignals#merge(Collection, NameAndSignals, MergeType)}
 * method and avoid taking mean or other stupid things of Compound-IDs.
 * Instead, if data is not compound-centric anymore, return {@value #default_CompoundID}
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public interface CompoundID {

  /**
   * The key to use in the {@link #addData(String, Object)} map to add
   * the corresponding HMDB ID.
   */
  public final static String compound_id_key = "HMDB_ID";
  
  /**
   * Means compound id has not been set or has no associated id.
   */
  public final static Integer default_CompoundID = -1;
  
  /**
   * Set the corresponding Compound ID as HMDB identifier.
   * Example: "HMDB00001" is "1".
   * @param hmdbID
   */
  public void setCompoundID(int hmdbID);
  
  /**
   * This should accept HMDB ids as strings (e.g., "HMDB00001")
   * and store them as an integer ("1").
   * @param hmdbID
   */
  public void setCompoundID(String hmdbID);
  
  /**
   * @return associated HMDB ID in integer representation.
   */
  public int getCompoundID();
 
}

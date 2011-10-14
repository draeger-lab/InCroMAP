/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/Integrator> to
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

import de.zbit.data.EnrichmentObject;
import de.zbit.util.ValuePair;

/**
 * Generic interface for statistical corrections, i.e.,
 * methods that are used to correct p-Values for multiple comparisons.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public interface FDRCorrection {
  
  /**
   * Adjust pValues according to the current statistical FDR correction method.
   * @param pValues a {@link ValuePair} of any identifier and the pValue. The identifier
   * object is required, because the order of the list is changed in some FDR
   * correction methods and pValues are simply overwritten with qValues.
   */
  public <ID extends Comparable<? super ID>> void pVal_adjust(List<ValuePair<ID, Double>> pValues);
  
  /**
   * Return a list with statistical corrected qValues for the given pValues.
   * Keep the indices of the original list.
   * <p>Note: This method is often slower than {@link #pVal_adjust(List)}, because
   * the list must be cloned in some correction methods to preserve the
   * original list order.
   * @param values pValues
   * @return qValues
   */
  public List<Double> getQvalues(List<Number> values);
  
  /**
   * Adjust pValues according to the current statistical FDR correction method
   * and set the directly via {@link EnrichmentObject#setQValue(double)}
   * @param ret
   */
  public <EnrichIDType> void setQvalue(List<EnrichmentObject<EnrichIDType>> enrichments);
  
}

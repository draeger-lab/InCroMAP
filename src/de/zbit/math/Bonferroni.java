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

import java.util.ArrayList;
import java.util.List;

import de.zbit.data.EnrichmentObject;
import de.zbit.util.ValuePair;

/**
 * Implementation of the Bonferroni FDR correction method.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class Bonferroni implements FDRCorrection {
  
  /**
   * The number of elements in the original list. This is used
   * as multiplier for the Bonferroni correction.
   * A value of 0 means, "infer from input list".
   */
  private int sourceListSize;
  
  public Bonferroni () {
    super();
    sourceListSize = 0;
  }
  
  /**
   * Create a new instance of the Bonferroni FDR correction method.
   * @param sourceListSize size of the source-elements list. This
   * is in most cases equal to the size of the pValues list.
   */
  public Bonferroni (int sourceListSize) {
    this();
    this.sourceListSize = sourceListSize;
  }
  

  /* (non-Javadoc)
   * @see de.zbit.math.Correction#getQvalues(java.util.List)
   */
  public List<Double> getQvalues(List<Number> values) {
    int multiplier = sourceListSize;
    if (multiplier==0) multiplier = values.size();
    
    List<Double> ret = new ArrayList<Double>(values.size());
    for (Number n : values) {
      ret.add(Math.min(n.doubleValue()*multiplier, 1));
    }
    
    return ret;
  }

  /* (non-Javadoc)
   * @see de.zbit.math.Correction#pVal_adjust(java.util.List)
   */
  public <ID extends Comparable<? super ID>> void pVal_adjust(
    List<ValuePair<ID, Double>> values) {
    int multiplier = sourceListSize;
    if (multiplier==0) multiplier = values.size();
    
    for (ValuePair<ID, Double> n : values) {
      n.setB(Math.min(n.getB()*multiplier, 1));
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.math.Correction#setQvalue(java.util.List)
   */
  public <EnrichIDType> void setQvalue(List<EnrichmentObject<EnrichIDType>> enrichments) {
    int multiplier = sourceListSize;
    if (multiplier==0) multiplier = enrichments.size();
    
    for (EnrichmentObject<?> eo : enrichments) {
      eo.setQValue(Math.min(eo.getPValue().doubleValue()*multiplier, 1));
    }
    
  }
  
}

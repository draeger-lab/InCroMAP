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
import java.util.Collections;
import java.util.List;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.util.objectwrapper.ValuePair;

/**
 * Implementation of the Bonferroni-Holm FDR correction method. Also known as
 * "Bonferroni step-down".
 * @see <a href="http://en.wikipedia.org/wiki/Holm%E2%80%93Bonferroni_method">Wikipedia</a>
 * @see <a href="http://www.silicongenetics.com/Support/GeneSpring/GSnotes/analysis_guides/mtc.pdf">Implementation tutorial</a>
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class BonferroniHolm implements FDRCorrection {


  /**
   * The number of elements in the original list. This is used
   * as base multiplier for the Bonferroni-Holm correction.
   * A value of 0 means, "infer from input list".
   */
  private int sourceListSize;
  
  public BonferroniHolm () {
    super();
    sourceListSize = 0;
  }
  
  /**
   * Create a new instance of the Bonferroni-Holm FDR correction method.
   * @param sourceListSize size of the source-elements list. This
   * is in most cases equal to the size of the pValues list.
   */
  public BonferroniHolm (int sourceListSize) {
    this();
    this.sourceListSize = sourceListSize;
  }

  /**
   * Adjust pValues according to Bonferroni-Holm
   * @see for example <a href="http://www.silicongenetics.com/Support/GeneSpring/GSnotes/analysis_guides/mtc.pdf">here</a>
   * @param pValues a {@link ValuePair} of any identifier and the pValue. The identifier
   * is required, because the list is sorted and pValues are overwritten with qValues.
   */
  public <ID extends Comparable<? super ID>> void pVal_adjust(
    List<ValuePair<ID, Double>> pValues) {
    if (pValues.size()<1) return;
    
    // Sort ascending
    Collections.sort(pValues, pValues.iterator().next().getComparator_OnlyCompareB());
    int m = sourceListSize==0?pValues.size():Math.max(sourceListSize, pValues.size());
    
    // Apply (pVal*N/Rank) BH correction
    for (int i = 0; i <pValues.size(); i++) {
      ValuePair<ID, Double> vp = pValues.get(i);
      double q = vp.getB()*(m-i);
      
      vp.setB(Math.min(q, 1));
    }
  }

  /* (non-Javadoc)
   * @see de.zbit.math.Correction#getQvalues(java.util.List)
   */
  public List<Double> getQvalues(List<Number> values) {
    // Create a list with identifiers and double values
    List<ValuePair<Integer, Double>> pValues = new ArrayList<ValuePair<Integer, Double>>();
    for (int i=0; i<values.size(); i++) {
      pValues.add(new ValuePair<Integer, Double>( Integer.valueOf(i), new Double(values.get(i).doubleValue()) ));
    }
    
    // Adjust pValues
    pVal_adjust(pValues);
    
    // Re-order to preserve original ordering
    Collections.sort(pValues, pValues.iterator().next().getComparator_OnlyCompareA());
    
    // Return qValues
    return ValuePair.getListOfB(pValues);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.math.Correction#setQvalue(java.util.List)
   */
  public <EnrichIDType> void setQvalue(List<EnrichmentObject<EnrichIDType>> enrichments) {
    if (enrichments.size()<1) return;
    
    // Sort ascending
    Collections.sort(enrichments, Signal.getComparator(EnrichmentObject.signalNameForPvalues, SignalType.pValue));
    int m = sourceListSize==0?enrichments.size():Math.max(sourceListSize, enrichments.size());
    
    // Apply (pVal*N/Rank) BH correction
    for (int i = 0; i <enrichments.size(); i++) {
      EnrichmentObject<?> vp = enrichments.get(i);
      double q = vp.getPValue().doubleValue()*(m-i);
      
      vp.setQValue(Math.min(q, 1));
    }
  }  
  
}

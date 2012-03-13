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
 * Implementation of the Benjamini and Hochberg FDR correction method.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class BenjaminiHochberg implements FDRCorrection {
  
  
  /**
   * Adjust pValues according to Benjamini and Hochberg
   * @see for example <a href="http://www.silicongenetics.com/Support/GeneSpring/GSnotes/analysis_guides/mtc.pdf">here</a>
   * @param pValues a {@link ValuePair} of any identifier and the pValue. The identifier
   * is required, because the list is sorted and pValues are overwritten with qValues.
   */
  public <ID extends Comparable<? super ID>> void pVal_adjust(
    List<ValuePair<ID, Double>> pValues) {
    if (pValues.size()<1) return;
    
    // Sort ascending
    Collections.sort(pValues, pValues.iterator().next().getComparator_OnlyCompareB());
    int m = pValues.size();
    
    // Apply (pVal*N/Rank) BH correction
    for (int i = (m-1); i > 0; i--) {
      ValuePair<ID, Double> vp = pValues.get(i-1);
      double q = vp.getB()*m/i;
      
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
    // Sort ascending
    Collections.sort(enrichments, Signal.getComparator(EnrichmentObject.signalNameForPvalues, SignalType.pValue));
    int m = enrichments.size();
    
    // Biggest value doesn't change
    EnrichmentObject<?> vp = enrichments.get(m-1);
    vp.setQValue(vp.getPValue().doubleValue());
    
    // Apply (pVal*N/Rank) BH correction
    for (int i = (m-1); i > 0; i--) {
      vp = enrichments.get(i-1);
      double q = vp.getPValue().doubleValue()*m/i;
      
      vp.setQValue(Math.min(q, 1));
    }
  }  
}

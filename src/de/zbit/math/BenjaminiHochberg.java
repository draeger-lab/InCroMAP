/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.util.ValuePair;

/**
 * Implementation of the Benjamini and Hochberg FDR correction method.
 * @author Clemens Wrzodek
 */
public class BenjaminiHochberg implements Correction {
  
  
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
    Collections.sort(enrichments, Signal.getComparator(EnrichmentObject.defaultExperimentName, SignalType.pValue));
    int m = enrichments.size();
    
    // Apply (pVal*N/Rank) BH correction
    for (int i = (m-1); i > 0; i--) {
      EnrichmentObject<?> vp = enrichments.get(i-1);
      double q = vp.getPValue().doubleValue()*m/i;
      
      vp.setQValue(Math.min(q, 1));
    }
  }  
}

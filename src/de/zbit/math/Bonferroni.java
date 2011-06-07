/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.math;

import java.util.ArrayList;
import java.util.List;

import de.zbit.data.EnrichmentObject;
import de.zbit.util.ValuePair;

/**
 * Implementation of the Bonferroni FDR correction method.
 * @author Clemens Wrzodek
 */
public class Bonferroni implements Correction {
  
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

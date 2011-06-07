/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.math;

import java.util.List;

import de.zbit.data.EnrichmentObject;
import de.zbit.util.ValuePair;

/**
 * Generic interface for statistical corrections, i.e.,
 * methods that are used to correct p-Values for multiple comparisons.
 * @author Clemens Wrzodek
 */
public interface Correction {
  
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

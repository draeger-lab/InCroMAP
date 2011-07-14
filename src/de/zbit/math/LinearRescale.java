/**
 * @author Clemens Wrzodek
 */
package de.zbit.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.zbit.util.Utils;

/**
 * Linear model to rescale from an old distrubition to a new one.
 * The new distribution may be defined by a n-dimensional partial
 * function.
 * @author Clemens Wrzodek
 */
public class LinearRescale {
  
  /**
   * Old minimum
   */
  Double min;
  
  /**
   * Old maximum
   */
  Double max;
  
  /**
   * New Values (if only newMin and new Max, then a list
   * with two values).
   */
  List<Double> targetMinMax;
  
  /**
   * Decides when to go from one element from {@link #targetMinMax}
   * to the next.
   */
  double newIntervalThreshold;
  
  
  public <T extends Number> LinearRescale (Number min, Number max, Number newMin, Number mewMax) {
    this (min, max, new Double[]{newMin.doubleValue(), mewMax.doubleValue()});
  }
  public <T extends Number> LinearRescale (Number min, Number max, Number... targetMinMax) {
    this (min, max, Arrays.asList(targetMinMax));
  }
  public <T extends Number> LinearRescale (Number min, Number max, List<T> targetMinMax) {
    super();
    this.min = min.doubleValue();
    this.max = max.doubleValue();
    this.targetMinMax = new ArrayList<Double>(targetMinMax.size());
    for (T t: targetMinMax) {
      this.targetMinMax.add(t.doubleValue());
    }
    
    newIntervalThreshold = ((this.max-this.min) / (this.targetMinMax.size()-1));
  }
  
  
  public Number rescale(Number n) {
    double r = n.doubleValue()-min;
    
    // Divide into parts for the components
    // Interval 0 = from first element in targetminMax to the second, and so on
    int interval = (int) (r/newIntervalThreshold);
    
    // Ensure furthermore an interval from 0<=interval<=targetMinMax.size()
    interval = Math.min(interval, targetMinMax.size()-1);
    int upperInterval = Math.min(interval+1, targetMinMax.size()-1);
    interval = Math.max(interval, 0);
    upperInterval = Math.max(upperInterval, 0);
    
    double oldMin = min+interval*newIntervalThreshold;
    double oldMax = oldMin+newIntervalThreshold;
    
    Double newMin=targetMinMax.get(interval);
    Double newMax=targetMinMax.get(upperInterval);
    
    return Utils.normalize(n.doubleValue(),oldMin,oldMax,newMin,newMax);
  }
  
  public static void main(String[] args) {
    LinearRescale lr = new LinearRescale(-5,5,2,50,255);
    for (int i=-5; i<6; i++) {
      System.out.print(i+ ": ");
      System.out.println(lr.rescale(i));
    }
  }
  
}

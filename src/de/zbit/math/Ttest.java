/**
 * @author Clemens Wrzodek
 */
package de.zbit.math;

import java.util.Arrays;
import java.util.List;

import de.zbit.util.Utils;

/**
 * Implementation of Welch's t test.
 * @see http://www.socialresearchmethods.net/kb/stat_t.php
 * @see http://en.wikipedia.org/wiki/Welch%27s_t_test
 * @author Clemens Wrzodek
 */
public class Ttest {
  
  
  /**
   * Calculates the median of the given values.
   * @param values sorted array!
   * @return median
   */
  public static double medianOfSortedArray(double[] values) {
    if (values==null || values.length<1) return Double.NaN;
    
    if (values.length%2!=0) {
      return values[values.length/2];
    } else {
      int upper = (int) Math.ceil(values.length/2);
      return (values[upper-1]+(values[upper]-values[upper-1])/2);
    }
  }
  
  /**
   * Calculates the median of the given values.
   * @param values sorted list!
   * @return median
   */
  public static <T extends Number> double medianOfSortedList(List<T> values) {
    if (values==null || values.size()<1) return Double.NaN;
    
    if (values.size()%2!=0) {
      return values.get(values.size()/2).doubleValue();
    } else {
      int upper = (int) Math.ceil(values.size()/2);
      double u = values.get(upper).doubleValue();
      double umo = values.get(upper-1).doubleValue();
      return (umo+(u-umo)/2);
    }
  }
  
  /**
   * Calculates the median of the given values.
   * @param values sorted array!
   * @return median
   */
  public static <T extends Number> double medianOfSortedArray(T[] values) {
    if (values==null || values.length<1) return Double.NaN;
    
    if (values.length%2!=0) {
      return values[values.length/2].doubleValue();
    } else {
      int upper = (int) Math.ceil(values.length/2);
      double u = values[upper].doubleValue();
      double umo = values[upper-1].doubleValue();
      return (umo+(u-umo)/2);
    }
  }
  
  
  static double getT(double[] arr1, double[] arr2) {
    Arrays.sort(arr1);
    Arrays.sort(arr2);
    
    // Calculate the nominator
    double mean1 = Utils.average(arr1);
    double mean2 = Utils.average(arr2);
    
    double nominator = mean1-mean2;
    
    
    // Calculate the denominator
    double var1= Utils.variance(arr1, mean1);
    double var2= Utils.variance(arr2, mean2);
    
    double denominator = Math.sqrt(var1/(double)arr1.length + var2/(double)arr2.length);
    
    // return t-value
    return (nominator/ denominator);
  }
  
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    double[] arr1 = new double[]{1.350879, 14.473533, 4.155654, 8.080748, 6.311774, 2.267002, 2.367196};
    double[] arr2 = new double[]{10.421462, 5.632722, 7.794428, 2.759809, 10.159160, 7.478680};
    
    System.out.println(getT(arr1, arr2));
  }
  
}

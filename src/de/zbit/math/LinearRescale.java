/*
 * $Id:  temp 13:46:56 wrzodek $
 * $URL: temp $
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
import java.util.Arrays;
import java.util.List;

import de.zbit.util.Utils;

/**
 * Linear model to rescale from an old distrubition to a new one.
 * The new distribution may be defined by a n-dimensional partial
 * function.
 * @author Clemens Wrzodek
 * @version $Rev$
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
  
  /**
   * Special constructor that let's the user define, at which value to return the number
   * in the middle of the targetMinMax list.
   * @param <T>
   * @param min
   * @param max
   * @param targetMinMax
   * @param middleValue
   */
  public <T extends Number> LinearRescale (Number min, Number max, List<T> targetMinMax, double middleValue) {
    this(min, max, targetMinMax);
    if (!Double.isNaN(middleValue)) {
      newIntervalThreshold = (middleValue - min.doubleValue())/
        (((double)this.targetMinMax.size()-1.0)*1.0/2.0 );
    }
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
    List<Integer> l = Arrays.asList(new Integer[]{2,50,175,255});
    LinearRescale lt = new LinearRescale(-2,2,l,0);
    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(0));
    
    lt = new LinearRescale(0.5,2,l,1);
    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(1));
    
    lt = new LinearRescale(0.05,20,l,1);
    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(1));
    
    lt = new LinearRescale(4,8,l,6);
    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(6));
    
    
    LinearRescale lr = new LinearRescale(-5,5,2,50,255);
    for (int i=-5; i<6; i++) {
      System.out.print(i+ ": ");
      System.out.println(lr.rescale(i));
    }
  }
  
}

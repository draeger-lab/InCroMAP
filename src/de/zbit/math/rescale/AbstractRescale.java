/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.math.rescale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.zbit.math.MathUtils;

/**
 * Abstract superclass form transforming and putting numbers
 * to another distribution / scale.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public abstract class AbstractRescale {

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
  
  
  public <T extends Number & Comparable<T>> AbstractRescale (Number min, Number max, T newMin, T newMax) {
    this (min, max, new Double[]{newMin.doubleValue(), newMax.doubleValue()});
  }
  @SuppressWarnings("unchecked")
  public <T extends Number & Comparable<T>> AbstractRescale (Number min, Number max, T... targetMinMax) {
    this (min, max, Arrays.asList(targetMinMax));
  }
  public <T extends Number & Comparable<T>> AbstractRescale (Number min, Number max, List<T> targetMinMax) {
    super();
    setIntervals(min, max, targetMinMax);
  }
  
  /**
   * Special constructor that let's the user define, at which value to return the number
   * in the middle of the targetMinMax list.
   * @param <T>
   * @param min
   * @param max
   * @param targetMinMax
   * @param middleValue of source distribution
   */
  public <T extends Number  & Comparable<T>> AbstractRescale (Number min, Number max, List<T> targetMinMax, double middleValue) {
    this(min, max, targetMinMax);
    double middleCopy = transform (middleValue);
    if (!Double.isNaN(middleCopy)) {
      newIntervalThreshold = (middleCopy - this.min)/
        (((double)this.targetMinMax.size()-1.0)*1.0/2.0 );
    }
  }
  
  /**
   * Actually performs all the pre-processing steps.
   * @param <T>
   * @param min
   * @param max
   * @param targetMinMax
   */
  public <T extends Number & Comparable<T>> void setIntervals(Number min, Number max, List<T> targetMinMax) {
    double minCopy = transform (min);
    double maxCopy = transform (max);
    this.min = Math.min(minCopy, maxCopy);
    this.max = Math.max(minCopy, maxCopy);
    this.targetMinMax = new ArrayList<Double>(targetMinMax.size());
    for (T t: targetMinMax) {
      this.targetMinMax.add((t.doubleValue()));
    }
    
    newIntervalThreshold = ((this.max.doubleValue()-this.min.doubleValue()) / ((double)this.targetMinMax.size()-1.0));
  }
  
  
  
  /**
   * @param n
   * @return
   */
  protected abstract double transform(Number n);
  
  /**
   * 
   * @param n
   * @return
   */
  protected abstract double reverseTransform(double n);
  
  
  public Number rescale(Number n) {
    double nCopy = transform(n);
    // do not permit smaller values than the defined minimum list value
    if (nCopy<min.doubleValue()) nCopy = min;
    double r = nCopy-min;
    
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
    
    double ret = MathUtils.normalize(nCopy,oldMin,oldMax,newMin,newMax);
    // Don't do this! Might return in invalid values (because of middleValue and such)
    //ret = Math.max(Math.min(ret, newMax), newMin);
    
    return (ret);
  }
  
}

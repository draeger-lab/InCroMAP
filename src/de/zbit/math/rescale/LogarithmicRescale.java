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
package de.zbit.math.rescale;

import java.util.List;


/**
 * Log-transform and linear rescale values.
 * Log transform all input distributions and values and rescales
 * them to the new target distribution.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class LogarithmicRescale extends AbstractRescale {
  
  /**
   * Define the log-base (Math.log10(10) = 1)
   */
  public double base = 1;

  
  public <T extends Number  & Comparable<T>> LogarithmicRescale (Number min, Number max, T newMin, T newMax) {
    super(min, max, newMin, newMax);
  }
  
  public <T extends Number  & Comparable<T>> LogarithmicRescale (Number min, Number max, List<T> targetMinMax) {
    super(min, max, targetMinMax);
  }
  
  public <T extends Number  & Comparable<T>> LogarithmicRescale (Number min, Number max, double logBase, List<T> targetMinMax) {
    super(min, max, targetMinMax);
    base = Math.log10(logBase);
    // Re-set intervals, because base has changed.
    setIntervals(min, max, targetMinMax);
  }
  
  public <T extends Number  & Comparable<T>> LogarithmicRescale (Number min, Number max, List<T> targetMinMax, double middleValue) {
    super(min, max, targetMinMax, middleValue);
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.math.AbstractRescale#transform(java.lang.Number)
   */
  @Override
  protected double transform(Number n) {
    return Math.log10(n.doubleValue())/base;
//    if (Double.isInfinite(d) || Double.isNaN(d)) {
//      return Double.MIN_NORMAL;
//    } else {
//      return d;
//    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.math.AbstractRescale#reverseTransform(double)
   */
  @Override
  protected double reverseTransform(double n) {
    return Math.pow(10, base*n);
  }
  
  
  public static void main(String[] args) {
//    List<Integer> l = Arrays.asList(new Integer[]{2,50,175,255});
//    LogarithmicRescale lt = new LogarithmicRescale(0,2,l,0);
//    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(0));
//    
//    lt = new LogarithmicRescale(0.5,2,l,1);
//    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(1));
//    
    LogarithmicRescale lt = new LogarithmicRescale(0.005,1,0,255);
    System.out.println(0.005 + "\t" + lt.rescale(0.005));
    System.out.println(1 + "\t" + lt.rescale(1));
    System.out.println(0.9 + "\t" + lt.rescale(0.9));
    System.out.println(0.01 + "\t" + lt.rescale(0.01));
    System.out.println(0.006 + "\t" + lt.rescale(0.006));
    System.out.println(0.005 + "\t" + lt.rescale(0.005));
    System.out.println(0.004 + "\t" + lt.rescale(0.004));
    
    
//    lt = new LogarithmicRescale(4,8,l,6);
//    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(6));
//    
    
//    LogarithmicRescale lr = new LogarithmicRescale(0.005,1,1,250,255);
//    for (int i=1; i<150; i++) {
//      System.out.print((1.0/i)+ ": ");
//      System.out.println(lr.rescale((1.0/i)));
//    }
  }

}

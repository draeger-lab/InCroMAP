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

import java.util.Arrays;
import java.util.List;

/**
 * Linear model to rescale from an old distribution to a new one.
 * The new distribution may be defined by a n-dimensional partial
 * function.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class LinearRescale extends AbstractRescale {
  
  
  public <T extends Number & Comparable<T>> LinearRescale (Number min, Number max, T newMin, T newMax) {
    super(min, max, newMin, newMax);
  }
  public <T extends Number & Comparable<T>> LinearRescale (Number min, Number max, T... targetMinMax) {
    super(min, max, targetMinMax);
  }
  public <T extends Number & Comparable<T>> LinearRescale (Number min, Number max, List<T> targetMinMax) {
    super(min, max, targetMinMax);
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
  public <T extends Number & Comparable<T>> LinearRescale (Number min, Number max, List<T> targetMinMax, double middleValue) {
    super(min, max, targetMinMax, middleValue);
  }
  
  
  public static void main(String[] args) {
    AbstractRescale rr = new LogarithmicRescale(0.0666666667,15.0,Arrays.asList(new Double[]{175d,255d,0d}), 1d);
    System.out.println(rr.newIntervalThreshold + "\t" + rr.rescale(0));
    System.out.println(rr.newIntervalThreshold + "\t" + rr.rescale(0.0005));
    System.out.println(rr.newIntervalThreshold + "\t" + rr.rescale(0.005));
    System.out.println(rr.newIntervalThreshold + "\t" + rr.rescale(0.05));
    System.out.println(rr.newIntervalThreshold + "\t" + rr.rescale(0.5));
    System.out.println(rr.newIntervalThreshold + "\t" + rr.rescale(1));
    System.out.println(rr.newIntervalThreshold + "\t" + rr.rescale(2));
    System.out.println(rr.newIntervalThreshold + "\t" + rr.rescale(7));
    System.out.println(rr.newIntervalThreshold + "\t" + rr.rescale(14));
    System.out.println(rr.newIntervalThreshold + "\t" + rr.rescale(15));
    System.out.println("===========");
    
    
    
    
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
  
  /* (non-Javadoc)
   * @see de.zbit.math.AbstractRescale#transform(java.lang.Number)
   */
  @Override
  protected double transform(Number n) {
    // linear
    return n.doubleValue();
  }
  /* (non-Javadoc)
   * @see de.zbit.math.AbstractRescale#reverseTransform(double)
   */
  @Override
  protected double reverseTransform(double n) {
    // linear
    return n;
  }
  
}

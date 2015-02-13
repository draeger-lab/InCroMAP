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

/**
 * WARNING: Not yet WORKING!
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class ExponentialRescale extends AbstractRescale {

  
  public <T extends Number  & Comparable<T>> ExponentialRescale (Number min, Number max, T newMin, T newMax) {
    super(min, max, newMin, newMax);
  }
    
  
  
  /* (non-Javadoc)
   * @see de.zbit.math.AbstractRescale#transform(java.lang.Number)
   */
  @Override
  protected double transform(Number n) {
    return Math.exp(n.doubleValue());
  }
  
  /* (non-Javadoc)
   * @see de.zbit.math.AbstractRescale#reverseTransform(double)
   */
  @Override
  protected double reverseTransform(double n) {
    return Math.log(n);
  }
  
  
  public static void main(String[] args) {
//    List<Integer> l = Arrays.asList(new Integer[]{2,50,175,255});
//    LogarithmicRescale lt = new LogarithmicRescale(0,2,l,0);
//    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(0));
//    
//    lt = new LogarithmicRescale(0.5,2,l,1);
//    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(1));
//    
    ExponentialRescale lt = new ExponentialRescale(0.005,1,0,255);
    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(0.005));
    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(1));
    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(0.9));
    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(0.01));
    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(0.006));
    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(0.005));
    System.out.println(lt.newIntervalThreshold + "\t" + lt.rescale(0.004));
    
    
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

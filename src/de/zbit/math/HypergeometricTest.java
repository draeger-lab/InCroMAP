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

import java.io.Serializable;

/**
 * Quick and Cache-based implementation of the Hypergeometric test
 * (using a hypergeometric distribution).
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Hypergeometric_distribution">
 * Wikipedia</a> for the hypergeometric distribution.
 * @see <a href="http://nar.oxfordjournals.org/content/37/19/e131.full#disp-formula-1">
 * Publication: "SubpathwayMiner: a software package for flexible identification of pathways"</a>
 * for the hypergeometric test
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class HypergeometricTest implements Serializable, EnrichmentPvalue {
  private static final long serialVersionUID = -2415584131303973747L;

  /**
   * Total number of genes in the genome (or number or marbles in the urn) (=N).
   */
  private final int genomeSize;
  
  /**
   * Total number of genes in the genelist (or number of marbles to draw from the urn) (=n).
   */
  private final int geneListSize;
  
  /**
   * This is a threshold to detect overflows in pValue calculations.
   */
  private final double MAXEXP = (1023)*Math.log(2);
  
  private final ExactHypergeometricTest exact;
  
  /**
   * If true, the p-value is never computed by {@link ExactHypergeometricTest).
   */
  private boolean neverExactPValue = false;
  
  /**
   * @param genomeSize
   * @param geneListSize
   */
  public HypergeometricTest (int genomeSize, int geneListSize) {
    this(genomeSize, geneListSize, false);
  }
  

  /**
   * Initialize the hypergeometric test with static parameters.
   * 
   * @param genomeSize Total number of genes in the genome (or number or marbles in the urn).
   * @param geneListSize Total number of genes in the genelist (or number of marbles to draw from the urn).
   * @param neverExactPValue If true, p-value is never computed exactly.
   */
  public HypergeometricTest(int genomeSize, int geneListSize, boolean neverExactPValue) {
  	super();
    if (genomeSize <= 0)
      throw new IllegalArgumentException ("geneListSize must be greater than or equal to 0");
    if (geneListSize <= 0 || geneListSize > genomeSize) {
      if (geneListSize > genomeSize) {
        throw new IllegalArgumentException (String.format("Can not preform an enrichment of %s objects. Maximum number allowed: %s", geneListSize, genomeSize));
      } else {
        throw new IllegalArgumentException (String.format("geneListSize is invalid: 0>=geneListSize %s <=genomeSize %s", geneListSize, genomeSize));
      }
    }
    
    this.genomeSize = genomeSize;
    this.geneListSize = geneListSize;
    this.neverExactPValue = neverExactPValue;
    
    // For small r values, an exact implementation is required.
    exact = new ExactHypergeometricTest(genomeSize, geneListSize);
	}

	/**
   * binomialCoefficient that uses approximation for large numbers.
   * @param n
   * @param k
   * @return
   */
  public static double binomialCoefficient (int n, int k) {
    // Limit defines when to use the approximation.
    final int LIMIT = 100;
    int i;
    if (k == 0 || k == n)
      return 1;
    if (k < 0) {
      System.err.println("binomialCoefficient with k<0: "+k);
      return 0;
    }
    if (k > n) {
      System.err.println("binomialCoefficient with k>n: "+k + " > " + n);
      return 0;
    }
    
    if (k > (n/2)) {
      // Kuerzen ist schneller
      k = n - k;
    } if (n <= LIMIT) {
      // Exact, simple calculation
      double ret = 1.0;
      int n_minus_k = n - k;
      for (i = 1; i <= k; i++) {
        ret *= (double)(n_minus_k + i)/(double)i;
      }
      return ret;
    } else {
      // Approximation (in lnFac) = n!/(k!*(n-k)!) logarithmized
      double ret = (lnFac (n) - lnFac (k)) - lnFac (n - k);
      return Math.exp (ret);
    }
  }
  
  
  /**
   * Approximation of a logarithmized (base e) factorial.
   * @param n
   * @return ln(n!)
   */
  public static double lnFac (int n) {
    n = Math.abs(n);
    
    // Define limit for simple calculations
    final int LIMIT = 15;

    if (n<=1) {
      return 0; // ln(1) = 0
    } else if (n < LIMIT) {
      // Naive calculation
      long ret = 1;
      for (int i = 2; i <= n; i++) {
        ret *= i;
      }
      return Math.log (ret);
    } else {
      // Approximation
      double x = (double)(n + 1);
      double y = 1.0/(Math.pow(x, 2));
      double ret = ((-(5.95238095238E-4*y) + 7.936500793651E-4)*y -
          2.7777777777778E-3)*y + 8.3333333333333E-2;
      ret = ((x - 0.5)*Math.log (x) - x) + 9.1893853320467E-1 + ret/x;
      return ret;
    }
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.math.EnrichmentPvalue#getPvalue(int, int)
   */
  public double getPvalue(int t, int r) {
    final int LIMIT = 100;
    if (t <= 0 || t > genomeSize) {
      throw new IllegalArgumentException ("Invalid pathway size: " + t);
    }
    if (r < Math.max (0, t - genomeSize + geneListSize) || r > Math.min (genomeSize, geneListSize)) {
      System.err.println("Returning zero due to invalid boundaries in hypergeometric test.");
      return 0;
    }
    
    /*
     * Values of r lower than this threshold are not correct approximated.
     */
    if (r<=(t/(genomeSize/geneListSize)) +1 && !neverExactPValue) {
      if (exact!=null) {
        return exact.getPvalue(t, r);
      } else {
        // They are anyway relatively insignificant.
        return 1.0;
      }
    }
    
    if (genomeSize <= LIMIT) {
      // Exact calculation
      return binomialCoefficient (geneListSize, r) * binomialCoefficient (genomeSize - geneListSize, t - r)
        /binomialCoefficient (genomeSize, t);
    } else {
      // Approximation
      double res =
        lnFac (geneListSize) + lnFac (genomeSize-geneListSize) - lnFac (genomeSize)
        - lnFac (r) - lnFac (t - r) + lnFac (t)
        - lnFac (geneListSize - r) - lnFac (genomeSize - geneListSize - t + r) 
        + lnFac (genomeSize - t);
      if (res >= MAXEXP) {
        System.err.println("WARNING: term overflow - numbers too big.");
      }
      // Return a number between 0 and 1.
      return Math.min(Math.max(Math.exp (res), 0), 1);
    }
  }
  

  /* (non-Javadoc)
   * @see de.zbit.math.EnrichmentPvalue#getGeneListSize()
   */
  public int getGeneListSize() {
    return geneListSize;
  }

  /* (non-Javadoc)
   * @see de.zbit.math.EnrichmentPvalue#getGenomeSize()
   */
  public int getGenomeSize() {
    return genomeSize;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    // Checks, based on class type and total list sizes.
    // Does not check the cache (because that's senseless).
    if(obj.getClass().equals(getClass())) {
      HypergeometricTest t = (HypergeometricTest) obj;
      if (t.getGeneListSize()==getGeneListSize() &&
          t.getGenomeSize()==getGenomeSize()) {
        return true;
      }
    }
    return false;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return genomeSize + geneListSize;
  }
  
  public static void main(String[] args) {
    HypergeometricTest t = new HypergeometricTest(24391, 1977);
    
    /*int schranke=1;
    for (int i=100; i<2000; i+=50) {
      double lastPVAL = Integer.MAX_VALUE;
      for (int j=1; j<=i; j++) {
        double pVal = t.getPvalue(i,j);
        if (pVal > lastPVAL) schranke = j;
        lastPVAL = pVal;
      }
      System.out.println(i+"\t" + schranke);
      System.out.println(lnFac(1977-(schranke-1)) + " :: " + lnFac(1977-(schranke)) + " :: " + lnFac(1977-(schranke+1)));
      System.out.println(lnFac(i-(schranke-1)) + " :: " + lnFac(i-(schranke)) + " :: " + lnFac(i-(schranke+1)));
    }
    //System.out.println(t.getPvalue(100, 1));
    //System.out.println(t.getPvalue(100, 8));
    
    if (true) return;
    */
    
    ExactHypergeometricTest t2 = new ExactHypergeometricTest(24391, 1977);
    /*System.out.println(t.getPvalue(1035, 1));
    System.out.println(t.getPvalue(1035, 10));
    System.out.println(t.getPvalue(1035, 100));
    System.out.println(t.getPvalue(1035, 1000));
    System.out.println("=================");
    System.out.println(t.getPvalue(1193, 1));
    System.out.println(t.getPvalue(1193, 361));
    System.out.println(t.getPvalue(1193, 1000));*/
    
    for (int i=1; i<100; i++) {
      System.out.println(i + "\t" + t.getPvalue(100,i) + "\t" + t2.getPvalue(100,i));
    }
    
    /*
    HypergeometricDist d = new HypergeometricDist(1977, 24391, 1035);
    System.out.println(d.prob(1));
    System.out.println(d.prob(100));
    
    System.out.println(HypergeometricDist.prob(1, 24391, 1035, 1977));
    System.out.println(HypergeometricDist.prob(100, 24391, 1035, 1977));
    */
  }
  

}

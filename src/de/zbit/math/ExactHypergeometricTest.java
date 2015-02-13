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
package de.zbit.math;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import de.zbit.util.objectwrapper.ValuePair;

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
public class ExactHypergeometricTest implements Serializable, EnrichmentPvalue {
  private static final long serialVersionUID = -2415584131303973747L;

  /**
   * Total number of genes in the genome (or number or marbles in the urn) (=N).
   */
  private final BigInteger genomeSize;
  
  /**
   * Total number of genes in the genelist (or number of marbles to draw from the urn) (=n).
   */
  private final BigInteger geneListSize;
  
  /**
   * {@link #binomialCoefficient(BigInteger, BigInteger)} of {@link #genomeSize} over {@link #geneListSize}.
   */
  private final BigDecimal binom_N_n;
  
  /**
   * A cache for the binomial coefficient.
   */
  private Map<ValuePair<BigInteger,BigInteger>, BigInteger> binomCache = new HashMap<ValuePair<BigInteger,BigInteger>, BigInteger>();
  
  /**
   * Maximum allowed cache size.
   */
  private static final int maxCacheSize = 100;
  
  /**
   * Math context, defining the precision when dealing with large numbers.
   */
  private MathContext mc = new MathContext(100, RoundingMode.HALF_EVEN);//MathContext.DECIMAL128;
  
  /**
   * Initialize the hypergeometric test with static parameters.
   * 
   * @param genomeSize Total number of genes in the genome (or number or marbles in the urn).
   * @param geneListSize Total number of genes in the genelist (or number of marbles to draw from the urn).
   */
  public ExactHypergeometricTest (long genomeSize, long geneListSize) {
    this.genomeSize = BigInteger.valueOf(genomeSize);
    this.geneListSize = BigInteger.valueOf(geneListSize);
    binom_N_n = new BigDecimal(binomialCoefficient(this.genomeSize, this.geneListSize), mc);
  }
  
  /**
   * Logarithm of a {@link BigInteger}.
   * @param a
   * @param base
   * @return
   */
  public static double log(BigInteger a, double base) {
    int b = a.bitLength() - 1;
    double c = 0;
    double d = 1;
    for (int i = b; i >= 0; --i) {
        if (a.testBit(i))
            c += d;
        d *= 0.5;
    }
    return (Math.log(c) + Math.log(2) * b) / Math.log(base);
}
  
  /**
   * binomialCoefficient that also can calculate bigger values, using a {@link BigInteger}.
   * @param n
   * @param k
   * @return
   */
  private BigInteger binomialCoefficient(BigInteger n, BigInteger k){
    
    // Return cached bigInt
    ValuePair<BigInteger, BigInteger> vp = new ValuePair<BigInteger, BigInteger>(n,k);
    if (binomCache.containsKey(vp)) return binomCache.get(vp);
    
    // Symetric enhancement (swap k with n_minus_k eventually.
    BigInteger n_minus_k=n.subtract(k);
    if(n_minus_k.compareTo(k)<0){
      BigInteger temp=k;
      k=n_minus_k;
      n_minus_k=temp;
    }
    
    
    // Calculate the binomial coefficient
    BigInteger numerator=BigInteger.ONE;
    BigInteger denominator=BigInteger.ONE;
    for(BigInteger j=BigInteger.ONE; j.compareTo(k)<=0; j=j.add(BigInteger.ONE)){
      numerator=numerator.multiply(j.add(n_minus_k));
      denominator=denominator.multiply(j);
      BigInteger gcd=numerator.gcd(denominator);
      numerator=numerator.divide(gcd);
      denominator=denominator.divide(gcd);
    }
    
    // Cache and return
    binomCache.put(vp, numerator);
    return numerator;
  }
  
  /**
   * The hypergeometric distribution, calculated as<br/>
   * <img src="http://upload.wikimedia.org/math/7/e/c/7ecd507bdc33dc636fdae2000d9b31fb.png"/>
   * 
   * @see <a href="http://en.wikipedia.org/wiki/Hypergeometric_distribution">Wikipedia</a>.
   * 
   * @param N total number of objects in urn
   * @param m number of white objects in urn
   * @param n number objects to draw without replacement
   * @param k number of objects to calculate the probability that they are white
   * @return
   */
  private BigDecimal hypergeometric_distribution(BigInteger m, BigInteger k) {
    // Values are getting really really big in here!
    BigDecimal zaehler = new BigDecimal(binomialCoefficient(m,k).multiply(binomialCoefficient(genomeSize.subtract(m),geneListSize.subtract(k))), mc);
    return (zaehler.divide(this.binom_N_n, mc));//.doubleValue();
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.math.EnrichmentPvalue#getPvalue(long, long)
   */
  public double getPvalue(int t, int r) {
    BigDecimal p = BigDecimal.ZERO;
    BigInteger t2 = BigInteger.valueOf(t);
    
    for (int x=0; x<r; x++) {
      //p+=hypergeometric_distribution(t2,BigInteger.valueOf(x));
      p=p.add(hypergeometric_distribution(t2,BigInteger.valueOf(x)), mc);
    }
    
    cleanCache();
    
    //return 1-p;
    return BigDecimal.ONE.subtract(p, mc).doubleValue();
  }

  /**
   * Avoid the cache from being too big.
   */
  private void cleanCache() {
    if (binomCache.size()>maxCacheSize) {
      binomCache.clear();
    }
  }

  /* (non-Javadoc)
   * @see de.zbit.math.EnrichmentPvalue#getGeneListSize()
   */
  public int getGeneListSize() {
    return geneListSize.intValue();
  }

  /* (non-Javadoc)
   * @see de.zbit.math.EnrichmentPvalue#getGenomeSize()
   */
  public int getGenomeSize() {
    return genomeSize.intValue();
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
    return genomeSize.add(geneListSize).intValue();
  }

}


/* *
 * Title:        <p>
 * Description:  <p>
 * Copyright:    Copyright (c) <p>
 * Company:      <p>
 * @author
 * @version 1.0
 */


package umontreal.iro.lecuyer.probdist;


/**
 * Extends the class {@link DiscreteDistributionInt} for
 * the <EM>hypergeometric</EM> distribution with
 * <SPAN CLASS="MATH"><I>k</I></SPAN> elements chosen among <SPAN CLASS="MATH"><I>l</I></SPAN>, <SPAN CLASS="MATH"><I>m</I></SPAN> being
 * of one type, and <SPAN CLASS="MATH"><I>l</I> - <I>m</I></SPAN> of the other.
 * The parameters <SPAN CLASS="MATH"><I>m</I></SPAN>, <SPAN CLASS="MATH"><I>k</I></SPAN> and <SPAN CLASS="MATH"><I>l</I></SPAN> are positive integers 
 * where 
 * <SPAN CLASS="MATH">1&nbsp;&lt;=&nbsp;<I>m</I>&nbsp;&lt;=&nbsp;<I>l</I></SPAN> and 
 * <SPAN CLASS="MATH">1&nbsp;&lt;=&nbsp;<I>k</I>&nbsp;&lt;=&nbsp;<I>l</I></SPAN>.
 * Its mass function is given by
 * <P>
 * </P>
 * <DIV ALIGN="CENTER" CLASS="mathdisplay">
 * <I>p</I>(<I>x</I>) = nCr(<I>m</I>, <I>x</I>)nCr(<I>l</I> - <I>m</I>, <I>k</I> - <I>x</I>)/nCr(<I>l</I>, <I>k</I>),&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;for max(0, <I>k</I> - <I>l</I> + <I>m</I>)&nbsp;&lt;=&nbsp;<I>x</I>&nbsp;&lt;=&nbsp;min(<I>k</I>, <I>m</I>),
 * </DIV><P></P>
 * where nCr is defined in {@link BinomialDist}.
 * 
 */
public class HypergeometricDist extends DiscreteDistributionInt {

   private int m;
   private int l;
   private int k;
   private double p0;


   public static double MAXN = 100000;


   /**
    * Constructs an hypergeometric distribution with parameters
    *    <SPAN CLASS="MATH"><I>m</I></SPAN>, <SPAN CLASS="MATH"><I>l</I></SPAN> and <SPAN CLASS="MATH"><I>k</I></SPAN>.
    * 
    */
   public HypergeometricDist (int m, int l, int k) {
      setParams (m, l, k);
   }


   public double prob (int x) {
      if (x < supportA || x > supportB)
         return 0.0;
      if (pdf == null || x < xmin || x > xmax)
         return prob (m, l, k, x);
      return pdf[x - xmin];
   }

   public double cdf (int x) {
      if (x < supportA)
         return 0.0;
      if (x >= supportB)
         return 1.0;
      if (cdf != null) {
         if (x >= xmax)
            return 1.0;
         if (x < xmin)
            return cdf (m, l, k, x);
         if (x <= xmed)
            return cdf[x - xmin];
         else
            // We keep the complementary distribution in the upper part of cdf
            return 1.0 - cdf[x + 1 - xmin];
      } 
      else
         return cdf (m, l, k, x);  
   }

   public double barF (int x) {
      if (x <= supportA)
         return 1.0;
      if (x > supportB)
         return 0.0;
      if (cdf != null) {
         if (x > xmax)
            return barF (m, l, k, x);
         if (x <= xmin)
            return 1.0;
         if (x > xmed)
           // We keep the complementary distribution in the upper part of cdf
           return cdf[x - xmin];
         else
            return 1.0 - cdf[x - 1 - xmin];
      }
      else
         return barF (m, l, k, x);   
   }

   public int inverseFInt (double u) {
      if (u < 0 || u > 1)
         throw new IllegalArgumentException ("u is not in [0,1]");
      if (u <= 0.0)
         return Math.max (0, k - l + m);
      if (u >= 1.0)
         return Math.min (k, m);
      double p = p0;
     // Empirical correction, the original algorithm sets x=0.
     int x = Math.max (0, k - l + m);
     if (u <= p) return x;
     do {
        u = u - p;
        p = p*(m - x)*(k - x)/((x+1)*(l - m - k + 1.0 + x));
        x++;
     } while (u > p);
     return x;
   }

   public double getMean() {
      return HypergeometricDist.getMean (m, l, k);
   }

   public double getVariance() {
      return HypergeometricDist.getVariance (m, l, k);
   }

   public double getStandardDeviation() {
      return HypergeometricDist.getStandardDeviation (m, l, k);
   }

   /**
    * Computes the hypergeometric probability
    *    <SPAN CLASS="MATH"><I>p</I>(<I>x</I>)</SPAN>.
    * 
    */
   public static double prob (int m, int l, int k, int x) {
      final int SLIM = 70;
      final double MAXEXP = (Num.DBL_MAX_EXP - 1)*Num.LN2;// To avoid overflow
      if (l <= 0)
         throw new IllegalArgumentException ("l must be greater than 0");
      if (m <= 0 || m > l)
         throw new IllegalArgumentException ("m is invalid: 1 <= m < l");
      if (k <= 0 || k > l)
         throw new IllegalArgumentException ("k is invalid: 1 <= k < l");
      if (x < Math.max (0, k - l + m) || x > Math.min (k, m))
         return 0;

      if (l <= SLIM)
         return Num.combination (m, x) 
            * Num.combination (l - m, k - x)/Num.combination (l, k);
      else {
         double res =
             Num.lnFactorial (m) + Num.lnFactorial (l-m) - Num.lnFactorial (l)
           - Num.lnFactorial (x) - Num.lnFactorial (k - x) + Num.lnFactorial (k)
           - Num.lnFactorial (m - x) - Num.lnFactorial (l - m - k + x) 
           + Num.lnFactorial (l - k);
         if (res >= MAXEXP)
            throw new IllegalArgumentException ("term overflow");
         return Math.exp (res);
      }
   }


   /**
    * Computes the distribution function <SPAN CLASS="MATH"><I>F</I>(<I>x</I>)</SPAN>.
    * 
    */
   public static double cdf (int m, int l, int k, int x) {
      if (l <= 0)
         throw new IllegalArgumentException ("l must be greater than 0");
      if (m <= 0 || m > l)
         throw new IllegalArgumentException ("m is invalid: 1 <= m < l");
      if (k <= 0 || k > l)
         throw new IllegalArgumentException ("k is invalid: 1 <= k < l");
      int imin = Math.max (0, k - l + m);
      int imax = Math.min (k, m);
      if (x < imin)
         return 0.0;
      if (x >= imax)
         return 1.0;
      // Very inefficient
      double res = 0.0;
      for (int i = imin; i <= x; i++)
         res += prob (m, l, k, i);
      if (res >= 1.0)
         return 1.0;
      return res;
   }


   /**
    * Computes the complementary distribution function.
    * <SPAN  CLASS="textit">WARNING:</SPAN> The complementary distribution function is defined as 
    * 
    * <SPAN CLASS="MATH">bar(F)(<I>x</I>) = <I>P</I>[<I>X</I>&nbsp;&gt;=&nbsp;<I>x</I>]</SPAN>.
    * 
    */
   public static double barF (int m, int l, int k, int x) {
      if (l <= 0)
         throw new IllegalArgumentException ("l must be greater than 0");
      if (m <= 0 || m > l)
         throw new IllegalArgumentException ("m is invalid: 1 < =m < l");
      if (k <= 0 || k > l)
         throw new IllegalArgumentException ("k is invalid: 1 < =k < l");
      int imin = Math.max (0, k - l + m);
      int imax = Math.min (k, m);
      if (x <= imin)
         return 1.0;
      if (x > imax)
         return 0.0;
      // Very inefficient
      double res = 0.0;
      for (int i = imax; i >= x; i--)
         res += prob (m, l, k, i);
      if (res >= 1.0)
         return 1.0;
      return res;
   }


   /**
    * Computes <SPAN CLASS="MATH"><I>F</I><SUP>-1</SUP>(<I>u</I>)</SPAN> for the hypergeometric distribution without
    *   using precomputed tables.  The inversion is computed
    *   using the chop-down algorithm.
    * 
    */
   public static int inverseF (int m, int l, int k, double u) {
      //algo hin dans Kachitvichyanukul
      if (u < 0 || u >= 1)
         throw new IllegalArgumentException ("u is not in [0,1]");
      if (u <= 0.0)
         return Math.max (0, k - l + m);
      if (u >= 1.0)
         return Math.min (k, m);
      double p = 0;
      if (k < l - m)
          p = Math.exp (Num.lnFactorial (l - m) + Num.lnFactorial (l - k)
                       -Num.lnFactorial (l) - Num.lnFactorial (l - m - k));
      else
          p = Math.exp (Num.lnFactorial (m) + Num.lnFactorial (k)
                      -Num.lnFactorial (k- l + m) - Num.lnFactorial (l));

     // Empirical correction, the original algorithm sets x=0.
     int x = Math.max (0, k - l + m);
     if (u <= p) return x;
     
     do {
        u = u - p;       
	p = p*(m - x)*(k - x)/((x+1)*(l - m - k + 1.0 + x));
        x++;
     } while (u > p && p > 0);
     return x;
   }


   /**
    * Computes and returns the mean 
    * <SPAN CLASS="MATH"><I>E</I>[<I>X</I>] = <I>km</I>/<I>l</I></SPAN>
    *    of the Hypergeometric distribution with parameters <SPAN CLASS="MATH"><I>m</I></SPAN>, <SPAN CLASS="MATH"><I>l</I></SPAN> and <SPAN CLASS="MATH"><I>k</I></SPAN>.
    * 
    * @return the mean of the hypergeometric distribution 
    * <SPAN CLASS="MATH"><I>E</I>[<I>X</I>] = <I>km</I>/<I>l</I></SPAN>
    * 
    */
   public static double getMean (int m, int l, int k) {
      if (l <= 0)
         throw new IllegalArgumentException ("l must be greater than 0");
      if (m <= 0 || m > l)
         throw new IllegalArgumentException ("m is invalid: 1<=m<l");
      if (k <= 0 || k > l)
         throw new IllegalArgumentException ("k is invalid: 1<=k<l");

      return ((double) k *  (double) m / (double) l);
   }


   /**
    * Computes and returns the variance
    * of the hypergeometric distribution with parameters <SPAN CLASS="MATH"><I>m</I></SPAN>, <SPAN CLASS="MATH"><I>l</I></SPAN> and <SPAN CLASS="MATH"><I>k</I></SPAN>.
    * 
    * @return the variance of the Hypergeometric distribution
    *     
    * <SPAN CLASS="MATH">Var[<I>X</I>] = (<I>km</I>/<I>l</I> )(1 - <I>m</I>/<I>l</I> )(<I>l</I> - <I>k</I>)/(<I>l</I> - 1)</SPAN>
    * 
    */
   public static double getVariance (int m, int l, int k) {
      if (l <= 0)
         throw new IllegalArgumentException ("l must be greater than 0");
      if (m <= 0 || m > l)
         throw new IllegalArgumentException ("m is invalid: 1<=m<l");
      if (k <= 0 || k > l)
         throw new IllegalArgumentException ("k is invalid: 1<=k<l");

      return (((double) k * (double) m / (double) l) * 
              ( 1 - ((double) m / (double) l)) * ((double) l - (double) k) /
              ((double) l - 1));
   }


   /**
    * Computes and returns the standard deviation of the hypergeometric distribution
    *    with parameters <SPAN CLASS="MATH"><I>m</I></SPAN>, <SPAN CLASS="MATH"><I>l</I></SPAN> and <SPAN CLASS="MATH"><I>k</I></SPAN>.
    * 
    * @return the standard deviation of the hypergeometric distribution
    * 
    */
   public static double getStandardDeviation (int m, int l, int k) {
      return Math.sqrt (HypergeometricDist.getVariance (m, l, k));
   }


   /**
    * Returns the <SPAN CLASS="MATH"><I>m</I></SPAN> associated with this object.
    * 
    */
   public int getM() {
      return m;
   }


   /**
    * Returns the <SPAN CLASS="MATH"><I>l</I></SPAN> associated with this object.
    * 
    */
   public int getL() {
      return l;
   }


   /**
    * Returns the <SPAN CLASS="MATH"><I>k</I></SPAN> associated with this object.
    * 
    */
   public int getK() {
      return k;
   }

   private void setHypergeometric() {
      int imin = Math.max (0, k - l + m);
      int imax = Math.min (k, m);
      supportA = imin;
      supportB = imax;
      int ns = imax - imin + 1;
      if (ns > MAXN) {
         pdf = null;
         cdf = null;
         return;
      }

      int offset = imin;
      imin = 0;
      imax -= offset;
      double[] P = new double[ns];
      double[] F = new double[ns];

      // Computes the mode (taken from UNURAN)
      int mode = (int)((k + 1.0)*(m + 1.0)/(l + 2.0));
      int imid = mode - offset;

      P[imid] = prob (m, l, k, mode);

      int i = imid;
      while (i > imin && Math.abs (P[i]) > EPSILON) {
         P[i-1] = P[i]*(i + offset)/(m-i-offset+1) 
                  * (l - m - k + i + offset)/(k - i - offset + 1);
         i--;
      }
      imin = i;

      i = imid;
      while (i < imax && Math.abs (P[i]) > EPSILON) {
         P[i+1] = P[i]*(m - i - offset)/(i + offset + 1)
                  * (k - i - offset)/(l - m - k + i + offset + 1);
         i++;
      }
      imax = i;

      F[imin] = P[imin];
      i = imin;
      while (i < imax && F[i] < 0.5) {
         i++;
         F[i] = F[i-1] + P[i];
      }
      xmed = i;

      F[imax] = P[imax];
      i = imax - 1;
      while (i > xmed) {
         F[i] = P[i] + F[i + 1];
         i--;
      }

       xmin = imin + offset;
       xmax = imax + offset;
       xmed += offset;
       pdf  = new double[imax + 1 - imin];
       cdf  = new double[imax + 1 - imin];
       System.arraycopy (P, imin, pdf, 0, imax+1-imin);
       System.arraycopy (F, imin, cdf, 0, imax+1-imin);  
   }


   /**
    * Return a table containing the parameters of the current distribution.
    *    This table is put in regular order: [<SPAN CLASS="MATH"><I>m</I></SPAN>, <SPAN CLASS="MATH"><I>l</I></SPAN>, <SPAN CLASS="MATH"><I>k</I></SPAN>].
    * 
    */
   public double[] getParams () {
      double[] retour = {m, l, k};
      return retour;
   }


   /**
    * Resets the parameters of this object to <SPAN CLASS="MATH"><I>m</I></SPAN>, <SPAN CLASS="MATH"><I>l</I></SPAN> and <SPAN CLASS="MATH"><I>k</I></SPAN>.
    * 
    * 
    */
   public void setParams (int m, int l, int k) {
      if (l <= 0)
         throw new IllegalArgumentException ("l must be greater than 0");
      if (m <= 0 || m > l)
         throw new IllegalArgumentException ("m is invalid: 1 <= m < l");
      if (k <= 0 || k > l)
         throw new IllegalArgumentException ("k is invalid: 1 <= k < l");
      this.m = m;
      this.l = l;
      this.k = k;
      setHypergeometric();
      if (k < l - m)
          p0 = Math.exp (Num.lnFactorial (l - m) + Num.lnFactorial (l - k)
                               -Num.lnFactorial (l) - Num.lnFactorial (l - m - k));
      else
          p0 = Math.exp (Num.lnFactorial (m) + Num.lnFactorial (k)
                               -Num.lnFactorial (k- l + m) - Num.lnFactorial (l));
   }


   public String toString () {
      return getClass().getSimpleName() + " : m = " + m + ", l = " + l + ", k = " + k;
   }

}
/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.data;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.zbit.data.Signal.MergeType;
import de.zbit.util.Utils;

/**
 * A ratio of two integers (e.g. 4/106).
 * @author Clemens Wrzodek
 */
public class Ratio extends Number implements Serializable, Comparable<Object> {
  private static final long serialVersionUID = -1113485587640309349L;
  private final int a;
  private final int b;
  private final double valueOf;
  
  /**
   * Create a new {@link Ratio} a/b.
   * @param a
   * @param b
   */
  public Ratio (int a, int b) {
    this.a = a;
    this.b = b;
    this.valueOf = b!=0? (double)a/(double)b :0;
  }
  

  /**
   * @return the a
   */
  public int getA() {
    return a;
  }

  /**
   * @return the b
   */
  public int getB() {
    return b;
  }

  /**
   * @return the value of a/b.
   */
  public double getValueOf() {
    return valueOf;
  }


  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object o) {
    if (o instanceof Ratio) {
      return Double.compare(valueOf, ((Ratio) o).valueOf);
    } else if (o instanceof Signal || Signal.class.isAssignableFrom(o.getClass())) {
      return Double.compare(valueOf, ((Signal) o).getSignal().doubleValue());
    } else if (o instanceof Number || Number.class.isAssignableFrom(o.getClass())) {
      return Double.compare(valueOf, ((Number)o).doubleValue());
    }
    return -1;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return a+"/"+b;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    return new Ratio(a,b);
  }


  /* (non-Javadoc)
   * @see java.lang.Number#doubleValue()
   */
  @Override
  public double doubleValue() {
    return getValueOf();
  }


  /* (non-Javadoc)
   * @see java.lang.Number#floatValue()
   */
  @Override
  public float floatValue() {
    return (float)getValueOf();
  }


  /* (non-Javadoc)
   * @see java.lang.Number#intValue()
   */
  @Override
  public int intValue() {
    return (int)getValueOf();
  }


  /* (non-Javadoc)
   * @see java.lang.Number#longValue()
   */
  @Override
  public long longValue() {
    return (long)getValueOf();
  }
  
  /**
   * Returns a list, that only contains the {@link #getB()} values of {@link Ratio}s.
   * @param values
   * @return
   */
  public static List<Integer> getListOfB(Iterable<Ratio> values) {
    List<Integer> ret = new LinkedList<Integer>();
    
    Iterator<Ratio> it = values.iterator();
    while (it.hasNext()) {
      ret.add(it.next().getB());
    }
    
    return ret;
  }
  
  /**
   * Returns a list, that only contains the {@link #getA()} values of {@link Ratio}s.
   * @param values
   * @return
   */
  public static List<Integer> getListOfA(Iterable<Ratio> values) {
    List<Integer> ret = new LinkedList<Integer>();
    
    Iterator<Ratio> it = values.iterator();
    while (it.hasNext()) {
      ret.add(it.next().getA());
    }
    
    return ret;
  }


  /**
   * Merge multiple {@link Ratio}s to one, by taking the Mean or Median
   * of all {@link #getA()}s and {@link #getB()}s.
   * @param c
   * @param m
   * @return 
   */
  public static Ratio merge(Iterable<Ratio> c, MergeType m) {
    int newA = (int) Utils.round(Signal.calculate(m, Ratio.getListOfA(c)), 0);
    int newB = (int) Utils.round(Signal.calculate(m, Ratio.getListOfB(c)), 0);
    return new Ratio(newA, newB);
  }
  
}

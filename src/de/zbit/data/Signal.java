/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import de.zbit.util.ScientificNumberRenderer;
import de.zbit.util.Utils;
import de.zbit.util.ValuePair;

/**
 * A signal. This can be almost anything... It holds a name
 * (can be used, e.g., for "treatment name" or "experiment name"),
 * a Type (which describes the signal and is one of {@link SignalType})
 * and the actual signal.
 * 
 * <p> I used it to hold microarray signals of different treatments
 * (encoded in name) and to read pValues, as well as fold changes.</p>
 * 
 * @author Clemens Wrzodek
 */
public class Signal implements Serializable, Comparable<Object>  {
  private static final long serialVersionUID = -6025922649069761114L;
  public static final transient Logger log = Logger.getLogger(Signal.class.getName());

  /**
   * Name to match this signal to a treatment group or experiment.
   */
  private String name;
  
  /**
   * The actual signal.
   */
  private Number signal;
  
  /**
   * Type of the signal value
   */
  private SignalType type;
  
  /**
   * Type of Signal.
   * @author Clemens Wrzodek
   */
  public static enum SignalType {
    Unknown, Raw, Processed, FoldChange, pValue, qValue, ratio, log_ratio, Merged;
    // XXX: Is it possible to overwrite toString() and return "p-value", which would be correct spelling.
    // If this is done, check that other methods still can translate the string to the SignalType!
  }
  
  /**
   * Defines the way how to merge signals. Used, e.g. in
   * {@link NameAndSignals#merge(java.util.Collection, MergeType)}
   * @author Clemens Wrzodek
   */
  public static enum MergeType {
    Mean, Median, Minimum, Maximum, MaximumDistanceToZero, AskUser;
  }

  
  public Signal (Number signal) {
    super();
    this.signal = signal;
    name=null;
    type = SignalType.Unknown;
  }
  /**
   * @param signal
   * @param name - Name to match this signal to a treatment group or experiment.
   * @param type - Type of the signal value. See {@link SignalType}.
   */
  public Signal (Number signal, String name, SignalType type) {
    this(signal);
    this.name = name;
    this.type = type;
  }
  

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }
  /**
   * @param name - Name to match this signal to a treatment group or experiment.
   */
  public void setName(String name) {
    this.name = name;
  }
  /**
   * @return the signal. If it is not set, {@link Double#NaN} is returned.
   * This ensures that this method can never retur <code>null</code>.
   */
  public Number getSignal() {
    if (signal==null) return Double.NaN;
    return signal;
  }
  
  /**
   * @return {@link #getSignal()} as human-readable
   * text string.
   */
  public String getNiceSignalString() {
    return ScientificNumberRenderer.getNiceString(signal, null, 100);
  }
  
  /**
   * @param signal - Type of the signal value. See {@link SignalType}.
   */
  public void setSignal(Number signal) {
    this.signal = signal;
  }
  /**
   * @return the type
   */
  public SignalType getType() {
    return type;
  }
  /**
   * @param type the type to set
   */
  public void setType(SignalType type) {
    this.type = type;
  }
  
  
  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object o) {
    int r = -1;
    if (o instanceof Number) {
      r = Double.compare(signal.doubleValue(), ((Number)o).doubleValue());
    } else if (o instanceof Signal) {
      Signal o2 = (Signal)o;
      r = name.compareTo(o2.name);
      if (r==0) r = type.compareTo(o2.type);
      //if (r==0) r = Float.compare(signal, o2.signal);
      if (r==0) r = Double.compare(signal.doubleValue(), o2.signal.doubleValue());
      
    }
      
    return r;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer r = new StringBuffer();
    r.append("[Signal ");
    if (name!=null && name.length()>0) {
      r.append("'"+name+"' ");
    }
    if (!type.equals(SignalType.Unknown)) {
      r.append(type.toString()+": ");
    }
    r.append(signal);
    r.append("]");
    return r.toString();
  }
  
  /**
   * Create a {@link Comparator} that compares {@link NameAndSignals} objects, based
   * on defined {@link Signal}s.
   * @param <T>
   * @param experimentName
   * @param type
   * @return
   */
  public static <T extends NameAndSignals> Comparator<T> getComparator(final String experimentName, final SignalType type) {
    
    Comparator<T> comp = new Comparator<T>() {

      public int compare(T o1, T o2) {
        Signal one = o1.getSignal(type, experimentName);
        Signal two = o2.getSignal(type, experimentName);
        
        return one.compareTo(two);
      }
    };
    
    return comp;
  }
  
  /**
   * Merges all <b>compatible</b> signals, i.e., all signals with same experimentName and SignalType.
   * @param c list of signals to merge
   * @param m {@link MergeType} - Either Mean or Median, etc.
   * @return a new list of {@link Signal}s.
   */
  public static List<Signal> merge(Collection<Signal> c, MergeType m) {
    
    // Collect all compatible signals
    Map<ValuePair<String, SignalType>, List<Number>> compatibleSignals = new HashMap<ValuePair<String, SignalType>, List<Number>>();
    for (Signal signal : c) {
      ValuePair<String, SignalType> key = new ValuePair<String, SignalType>(signal.name, signal.type);
      List<Number> list = compatibleSignals.get(key);
      if (list==null) {
        list = new ArrayList<Number>();
        compatibleSignals.put(key, list);
      }
      list.add(signal.signal);
    }
    
    // Merge the signals (e.g., taking the mean)
    List<Signal> toReturn = new ArrayList<Signal>();
    for (ValuePair<String, SignalType> key : compatibleSignals.keySet()) {
      double sig = calculate(m, compatibleSignals.get(key));
      toReturn.add(new Signal(sig, key.getA(), key.getB()));
    }
    
    return toReturn;
  }
  
  /**
   * Merges all signals from the given <code>nsList</code>, matching the given
   * <code>experimentName</code> and <code>type</code> to one {@link Signal}.
   * @param <T>
   * @param nsList
   * @param m
   * @param experimentName
   * @param type
   * @return merged signal.
   */
  public static <T extends NameAndSignals> Signal merge(Collection<T> nsList, MergeType m, String experimentName, final SignalType type) {
    List<Number> list = new ArrayList<Number>();
    
    // Collect all compatible signals
    for (T ns : nsList) {
      if (!ns.hasSignals()) continue;
      for (Signal signal: ns.getSignals()) {
        if ((experimentName==null || signal.getName().equals(experimentName)) &&
          (type==null || signal.getType().equals(type)) && signal.signal!=null) {
          list.add(signal.signal);
        }
      }
    }
    
    // Merge the signals (e.g., taking the mean)
    double sig = calculate(m, list);
    return new Signal(sig, experimentName, type);
  }
  
  /**
   * Merges all signals, no matter if they are compatible or not.
   * @see #merge(Collection, MergeType)
   * @param c list of signals to merge
   * @param m {@link MergeType} - Either Mean or Median, etc.
   * @return a new list of {@link Signal}s.
   */
  public static double mergeAll(Collection<Signal> c, MergeType m) {
    
    // Collect all signal values
    double[] values = new double[c.size()];
    int i=0;
    Iterator<Signal> it = c.iterator();
    while (it.hasNext()) {
      values[i++] = it.next().getSignal().doubleValue();
    }
    
    return calculate(m, values);
  }
  
  /**
   * Calculates the {@link MergeType} (e.g., mean) of a numeric collection.
   * @param m
   * @param values
   * @return
   */
  public static double calculate(MergeType m, Collection<? extends Number> values) {
    if (values==null || values.size()<1) return Double.NaN;
    else if (values.size()==1) return values.iterator().next().doubleValue();
    
    if (m.equals(MergeType.Mean)) {
      return Utils.average(values);
    } else if (m.equals(MergeType.Median)) {
      return Utils.median(values);
    } else if (m.equals(MergeType.Minimum)) {
      return Utils.min(values);
    } else if (m.equals(MergeType.Maximum)) {
      return Utils.max(values);
    } else if (m.equals(MergeType.MaximumDistanceToZero)) {
      return Utils.max(values);
    } else {
      log.severe("Please implement calculation for " + m.toString() + "!");
      return 0;
    }
  }
  
  /**
   * Calculates the {@link MergeType} (e.g., mean) of a numeric array.
   * @param m
   * @param values
   * @return
   */
  public static double calculate(MergeType m, double... values) {
    if (values==null || values.length<1) return Double.NaN;
    else if (values.length==1) return values[0];
    
    if (m.equals(MergeType.Mean)) {
      return Utils.average(values);
    } else if (m.equals(MergeType.Median)) {
      return Utils.median(values);
    } else if (m.equals(MergeType.Minimum)) {
      if (values==null||values.length<1) return Double.NaN;
      double min = values[0];
      for (double value:values)
        min = Math.min(min, value);
      return min;
    } else if (m.equals(MergeType.Maximum)) {
      double max = values[0];
      for (double value:values)
        max = Math.max(max, value);
      return max;
    } else if (m.equals(MergeType.MaximumDistanceToZero)) {
      double max = values[0];
      for (double value:values)
        if (Math.abs(value)>Math.abs(max))
          max = value;
      return max;
    } else {
      log.severe("Please implement calculation for " + m.toString() + "!");
      return 0;
    }
  }
  
}


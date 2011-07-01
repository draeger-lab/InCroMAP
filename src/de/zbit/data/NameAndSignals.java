/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.data;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.exception.CorruptInputStreamException;
import de.zbit.io.CSVwriteable;
import de.zbit.util.ArrayUtils;
import de.zbit.util.Reflect;
import de.zbit.util.StringUtil;
import de.zbit.util.ValuePair;

/**
 * An abstract class that handles something
 * with a name and (multiple or one) Signal(s).
 * <p>When extending this class, please don't forger to
 * implement a public {@link #clone()} method. This is required for
 * many functionalities.
 * @author Clemens Wrzodek
 */
public abstract class NameAndSignals implements Serializable, Comparable<Object>, Cloneable, CSVwriteable, TableResult  {
  private static final long serialVersionUID = 732610412438240532L;
  public static final transient Logger log = Logger.getLogger(NameAndSignals.class.getName());

  /**
   * If only one signal is added, this is the name of the
   * experiment of the default signal.
   */
  public final static String defaultExperimentName = "default";
  
  
  /**
   * The SystematicName
   */
  protected String name;
  
  /**
   * The final processed Signal(s).
   * String is the name of the experiment (defaults to {@link #defaultExperimentName}).
   */
  protected List<Signal> signals=null;
  
  /**
   * Can hold any additional data (Mostly strings, such as gene descriptions
   * or additional identifiers).
   */
  private Map<String, Object> additional_data = null;
  
  /**
   * Simply sets the name of this instance.
   * @param name
   */
  public NameAndSignals(String name) {
    super();
    this.name = name;
  }
    
  /**
   * Add a signal intensity.
   * @param intensity - signal value
   * @param experimentName - name of the experiment (e.g. RAS or Control).
   */
  public void addSignal(Number intensity, String experimentName) {
    addSignal(intensity, experimentName, SignalType.Unknown);
  }
  
  public void addSignal(Number intensity, SignalType type) {
    addSignal(intensity, defaultExperimentName, type);
  }
  /**
   * Add a signal intensity.
   * <p>Be careful, this does NOT override existing Signals. You need to delete
   * existing signals by yourself.
   * @param intensity signal value
   * @param experimentName name of the experiment (e.g. RAS or Control).
   * @param type type of the signal
   */
  public void addSignal(Number intensity, String experimentName, SignalType type) {
    addSignal(new Signal(intensity, experimentName, type));
  }
  
  /**
   * Be careful: {@link Signal} compares itself on all contained variables,
   * including intensity. Thus, it is possible to add multiple FoldChanges of
   * the same experiment. This might lead to unexpected results, e.g., when
   * using {@link #getSignal(SignalType, String)}.
   * @param s
   */
  public void addSignal(Signal s) {
    if (signals==null) initializeSignals();
    signals.add(s);
  }
  
  /**
   * Add a signal intensity.
   * @param intensity
   */
  public void addSignal(Number intensity) {
    addSignal(intensity, defaultExperimentName, SignalType.Unknown);
  }
  
  /**
   * Remove all signals that match the given name and type.
   * @param experimentName
   * @param type
   * @return true if and only if signals have been removed from the list.
   */
  public boolean removeSignals(String experimentName, SignalType type) {
    boolean ret = false;
    if (signals==null) return ret;
    for (Signal signal : signals) {
      if (signal.getType().equals(type) && signal.getName().equals(experimentName)) {
        ret|=signals.remove(signal);
      }
    }
    return ret;
  }
  
  /**
   * Remove a signal from the signals list.
   * @param intensity
   * @param experimentName
   * @param type
   * @return
   */
  public boolean removeSignal(Number intensity, String experimentName, SignalType type) {
    if (signals==null) return false;
    Signal toRemove = new Signal(intensity, experimentName, type);
    return signals.remove(toRemove);
  }
  
  /**
   * @return the name.
   * @see #name
   */
  public String getName() {
    return name;
  }
  
  /**
   * @return the list of signals.
   * @see #signals
   */
  public List<Signal> getSignals() {
    return signals;
  }
  
  /**
   * Returns a list of available Signal Names and Types.
   * @return
   */
  public Collection<ValuePair<String, SignalType>> getSignalNames() {
    Set<ValuePair<String, SignalType>> sn = new HashSet<ValuePair<String, SignalType>>();
    for (Signal sig : signals) {
      sn.add(new ValuePair<String, SignalType>(sig.getName(), sig.getType()));
    }
    return sn;
  }
  
  public boolean hasSignals() {
    return signals!=null && signals.size()>0;
  }
  
  /**
   * Returns a specific signal (the first in the list,
   * that matches these specifications).
   * @param type
   * @param experimentName
   * @return
   */
  public Signal getSignal(SignalType type, String experimentName) {
    if (signals==null) return null;
    // Iterating is ok, because usually there are no more than ~5 signals in the list.
    for (Signal signal : signals) {
      if (signal.getType().equals(type) && signal.getName().equals(experimentName)) {
        return signal;
      }
    }
    return null;
  }
  
  /**
   * Return the {@link Signal}s intensity.
   * @param type
   * @param experimentName
   * @return the intensity or {@link Double#NaN} if no matching
   * signal could be found.
   */
  public Number getSignalValue(SignalType type, String experimentName) {
    Signal sig = getSignal(type, experimentName);
    return sig!=null?sig.getSignal():Double.NaN;
  }
  
  /**
   * Initializes {@link #signals}, if it is null.
   */
  private void initializeSignals() {
    if (signals==null) signals = new ArrayList<Signal>();
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object o) {
    // Compares ONLY THE NAME! Not the signal(s). and NOT the additional data.
    int r;
    if (o instanceof NameAndSignals) {
      NameAndSignals ot = (NameAndSignals) o;
      r = this.name.compareTo(ot.name);
    } else {
      r=this.name.compareTo(o.toString());
    }
    
    return r;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return name.hashCode();
  }

  
  
  /**
   * Returns a CLONED, gene-centered collection of the given {@link NameAndSignals}s.
   * @param nameAndSignals
   * @return
   */
  public static <T extends NameAndSignals> Collection<T> geneCentered(Collection<T> nameAndSignals, MergeType m) {
    
    // Group data by name
    Map<String, Collection<T>> group = group_by_name(nameAndSignals);
    
    Collection<T> toReturn = new ArrayList<T>();
    for (String mi : group.keySet()) {
      Collection<T> col = group.get(mi);
      toReturn.add(merge(col, m));
    }
      
      
    return toReturn;
  }
  
  
  /**
   * Merge a collection of objects to one, e.g., by concatenating strings
   * and taking the {@link MergeType} (e.g. mean) of all numeric values.
   * @param <T> the implementing class
   * @param c collection of objects to merge
   * @param m {@link MergeType} describing how to merge the signals.
   * @return
   */
  @SuppressWarnings("unchecked")
  public static <T extends NameAndSignals> T merge(Collection<T> c, MergeType m) {
    // Create a new instance of T and collect all names and signals
    Set<String> names = new HashSet<String>();
    List<Signal> signals = new ArrayList<Signal>();
    Map<String, List<Object>> add_data = new HashMap<String, List<Object>>();
    T newObject=null;
    
    // Collect all Signals, Names and additional data
    for (T ns : c) {
      // Create a new instance of T (if not yet done so)
      if (newObject==null) try {
        newObject = (T) ns.clone();
      } catch (CloneNotSupportedException e) {
        log.log(Level.SEVERE,"Could not clone extending object.", e);
      }
      
      // Collect names and signals
      names.add(ns.getName());
      if (ns.hasSignals()) {
        signals.addAll(ns.getSignals());
      }
      
      // Remember all additional data
      if (ns.additional_data!=null) {
        for (Map.Entry<String, Object> ad : ns.additional_data.entrySet()) {
          List<Object> list = add_data.get(ad.getKey());
          if (list==null) {
            list = new ArrayList<Object>();
            add_data.put(ad.getKey(), list);
          }
          list.add(ad.getValue());
        }
      }
    }
    
    // Set new name and signal to object
    if (newObject!=null) {
      newObject.name = ArrayUtils.implode(names.toArray(new String[0]), ", ");
      newObject.signals = Signal.merge(signals, m);
      
      // Set merged additional data
      newObject.initializeAdditionalData();
      for (Entry<String, List<Object>> ad : add_data.entrySet()) {
        newObject.addData(ad.getKey(), NameAndSignals.mergeAbstract(ad.getValue(), m));
      }
      
      // Let the extending classes also merge their private variables
      newObject.merge(c, newObject, m);
    }
    
    return newObject;
  }
  
  /**
   * Abstract merge functionality that can merge strings, numbers
   * and extensions of {@link NameAndSignals}.
   * @param c collection that contains object of exactly one class!
   * @param m {@link MergeType} that defines how to merge numbers.
   * @return
   */
  @SuppressWarnings("unchecked")
  public static Object mergeAbstract(Collection c, MergeType m) {
    if (c==null ||c.size()<1) return null;
    Object o = c.iterator().next();
    
    if (NameAndSignals.class.isAssignableFrom(o.getClass()) || o instanceof NameAndSignals) {
      // Case 1: Mergeable NameAndSignals
      return NameAndSignals.merge(c, m);

    } else if (Signal.class.isAssignableFrom(o.getClass()) || o instanceof Signal) {
      // Case 2: Numeric values
      return Signal.merge(c, m);
      
    } else if (Ratio.class.isAssignableFrom(o.getClass()) || o instanceof Ratio) {
      // Case 3: Numeric values
      return Ratio.merge(c,m);
      
    } else if (Number.class.isAssignableFrom(o.getClass()) || o instanceof Number) {
      // Case 4: Numeric values
      return Signal.calculate(m, c);
      
    } else {
      // Case 5: Strings or anything else.
      Set<Object> names = new HashSet<Object>();
      for (Object object : c) {
       names.add(object) ;
      }
      if (o instanceof String) {
        return ArrayUtils.implode(names.toArray(new String[0]), ", ");
      } else {
        return names.toArray();
      }
    }
  }
  
  
  /**
   * Merge a collection of objects to one, e.g., by concatenating strings
   * and taking the {@link MergeType} (e.g. mean) of all numeric values.
   * 
   * It is called from {@link NameAndSignals#merge(Collection, MergeType)}, please
   * implement a merging, only of your private variables. The variables in
   * {@link NameAndSignals} have already been merged in the target object.
   * 
   * <p>Note:<br/>This is a deep implementation. If you want to use the method,
   * please use {@link #merge(Collection, MergeType)}!
   * 
   * @see #merge(Collection, MergeType)
   * @param <T> ENSURES that this is the implementing class
   * @param source collection of objects to merge
   * @param target pre-prepared object, that already contains a merged {@link #name} and {@link Signal}s.
   * Other attributes from the source list should be written into this object
   * @param m {@link MergeType} describing how to merge numeric values.
   */
  protected abstract <T extends NameAndSignals> void merge(Collection<T> source, T target, MergeType m);

  /**
   * Groups the given {@link NameAndSignals}s by name.
   * @param miRNA collection of {@link NameAndSignals}s.
   * @return E.g. a map with miRNA names ({@link miRNA#getName()}) as keys and all belonging {@link miRNA}s as values.
   * Or anything else extending {@link NameAndSignals}.
   */
  public static <T extends NameAndSignals> Map<String, Collection<T>> group_by_name(Collection<T> miRNA) {
    // Group by miRNA name
    Map<String, Collection<T>> ret = new HashMap<String, Collection<T>>();
    
    T mi = null;
    Iterator<T> it = miRNA.iterator();
    while (it.hasNext()) {
      mi = it.next();
      
      Collection<T> col = ret.get(mi.getName());
      if (col==null) {
        col = new ArrayList<T>();
        ret.put(mi.getName(), col);
      }
      col.add(mi);
    }
    
    return ret;
  }
  
  /**
   * Should only be used as a helper method for {@link #clone()} methods of implementing
   * classes. This provides a base functionality to clone all fields in this abstract
   * implementation.
   * @param <T>
   * @param target
   * @param source
   * @throws CloneNotSupportedException
   */
  @SuppressWarnings("unchecked")
  protected <T extends NameAndSignals> void cloneAbstractFields(T target, T source) throws CloneNotSupportedException {
    target.name = new String(source.name);
    target.signals = (List<Signal>) cloneCollection(source.signals);
    target.additional_data = (Map<String, Object>) ((HashMap)additional_data).clone();
  }
  
  /**
   * Tries to clone a collection. If this is not possible, a new
   * {@link ArrayList} is created, all elements of the source collection
   * are being put there and a cloned instance of this list is returned.
   * @param <T>
   * @param col
   * @return
   */
  @SuppressWarnings("unchecked")
  protected static <T> Collection<T> cloneCollection(Collection<T> col) {
    if (col==null) return null;
    Object clone = Reflect.invokeIfContains(col, "clone");
    if (clone==null) {
      Collection<T> c2 = new ArrayList<T>();
      c2.addAll(col);
      return (Collection<T>) ((ArrayList)c2).clone();
    } else {
      return (Collection<T>) clone;
    }
  }
  

  /**
   * Add any additional data objects (such as more identifier) to this
   * mRNA.
   * @param key
   * @param value
   */
  public void addData(String key, Object value) {
    if (additional_data==null) initializeAdditionalData();
    additional_data.put(key, value);
  }

  private void initializeAdditionalData() {
    additional_data = new HashMap<String, Object>();
  }
  
  /**
   * Returns previously saved additional data (with {@link #addData(String, Object)}).
   * @param key
   * @return
   */
  public Object getData(String key) {
    if (additional_data==null) return null;
    else return additional_data.get(key);
  }
  
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return toString(null);
  }
  
  /**
   * Providing an extendable {@link #toString()} implementation, that allows
   * extending classes to add their private variables.
   * @param append String summary of private variables
   * @return the appropriately merged String of {@link #toString()} and append.
   */
  protected String toString(String append) {
    StringBuffer r = new StringBuffer();
    
    String className = getClass().getName();
    if (className.contains(".")) // Trim packages
      className = className.substring(className.lastIndexOf('.')+1);
    
    // Output primary identifiers
    r.append("[" + className + " " + name + " ");
    
    // Append customizable string.
    if (append!=null && append.length()>0) {
      r.append(append);
      if (!append.endsWith(" "))
        r.append(" ");
    }
    
    // Output additional data
    if (additional_data!=null && additional_data.size()>0) {
      r.append("Additional_data:[");
      Iterator<String> it = additional_data.keySet().iterator();
      while (it.hasNext()) {
        String key = it.next();
        r.append(key + ":" + additional_data.get(key));
        if (it.hasNext()) r.append(", ");
      }
      r.append("] ");
    }
    
    // Output signals
    if (getSignals()!=null && getSignals().size()>0) {
      r.append("Signals:");
      for (Signal signal : getSignals()) {
        r.append(signal.toString());
      }
    }
    
    r.append("]");
    return r.toString();
  }
  
  public int getNumberOfAdditionalData() {
    return additional_data==null?0:additional_data.size();
  }
  public int getNumberOfSignals() {
    return signals==null?0:signals.size();
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getColumnCount()
   */
  public int getColumnCount() {
    int c = 1; //Name
    c+=getNumberOfSignals(); //Signals
    c+=getNumberOfAdditionalData(); //Additional data
    return c;
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getColumnName(int)
   */
  public String getColumnName(int columnIndex) {
    return getColumnName(columnIndex, null);
  }
  
  /**
   * Allows extending classes to put their own objects between the {@link #name}
   * (always the first column) and {@link #signals}.
   * @param columnIndex
   * @param extensionNames column headers for extending columns.
   * @return column header at the given index.
   */
  public String getColumnName(int columnIndex, String[] extensionNames) {
    int signalStart=1+(extensionNames==null?0:extensionNames.length);
    int afterSignals = signalStart+getNumberOfSignals();
    if (columnIndex == 0) return "Name";
    else if (columnIndex >= 1 && columnIndex<signalStart)
      return StringUtil.formatOptionName(extensionNames[columnIndex-1]);
    else if (columnIndex >= signalStart && columnIndex<afterSignals) {
      Signal sig = signals.get(columnIndex-signalStart);
      return sig.getName() + " [" + sig.getType().toString() + "]";
    } else if (columnIndex >= afterSignals && columnIndex<afterSignals+getNumberOfAdditionalData()) {
      columnIndex-=afterSignals;
      Iterator<String> it = additional_data.keySet().iterator();
      int i=0;
      while (it.hasNext()) {
        if (columnIndex==i++) {
          return StringUtil.formatOptionName(it.next());
        } else it.next();
      }
    }
    return "";
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getObjectAtColumn(int)
   */
  public Object getObjectAtColumn(int columnIndex) {
    return getObjectAtColumn(columnIndex, null);
  }
  
  /**
   * Allows extending classes to put their own objects between the {@link #name}
   * (always the first column) and {@link #signals}.
   * @param columnIndex
   * @param extensions columns that should be put between name and signals
   * @return object at the given index
   */
  public Object getObjectAtColumn(int columnIndex, Object[] extensions) {
    int signalStart=1+(extensions==null?0:extensions.length);
    int afterSignals = signalStart+getNumberOfSignals();
    if (columnIndex == 0) return this.name;
    else if (columnIndex >= 1 && columnIndex<signalStart)
      return extensions[columnIndex-1];
    else if (columnIndex >= signalStart && columnIndex<afterSignals)
      return signals.get(columnIndex-signalStart);
    else if (columnIndex >= afterSignals && columnIndex<afterSignals+getNumberOfAdditionalData()) {
      columnIndex-=afterSignals;
      Iterator<String> it = additional_data.keySet().iterator();
      int i=0;
      while (it.hasNext()) {
        if (columnIndex==i++) {
          String key = it.next();
          return additional_data.get(key);
        } else it.next();
      }
    }
    return null;
  }

  /* (non-Javadoc)
   * @see de.zbit.io.CSVwriteable#fromCSV(java.lang.String[], int, int)
   */
  public void fromCSV(String[] elements, int elementNumber, int CSVversionNumber)
    throws CorruptInputStreamException {
    log.log(Level.SEVERE, "fromCSV() not supported in NameAndSignals.");
  }

  /* (non-Javadoc)
   * @see de.zbit.io.CSVwriteable#getCSVOutputVersionNumber()
   */
  public int getCSVOutputVersionNumber() {
    return 0;
  }

  /* (non-Javadoc)
   * @see de.zbit.io.CSVwriteable#toCSV(int)
   */
  public String toCSV(int elementNumber) {
    return toCSV(elementNumber, null);
  }
  public String toCSV(int elementNumber, Object[] extensions) {
    StringBuffer ret = new StringBuffer();
    Object o; int i=0;
    while ((o=getObjectAtColumn(i++, extensions))!=null) {
      if (i>1) ret.append("\t");
      ret.append(o.toString());
    }
    return ret.toString();
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getRowObject()
   */
  public Object getRowObject() {
    return this;
  }
  
  /**
   * Get the GeneIDs from <T extends NameAndSignals> or Integer (direct GeneID). 
   * Can also be mixed lists and arrays of these types.
   * @param o
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static Set<Integer> getGeneIds(Object o) {
    Set<Integer> geneIds = new HashSet<Integer>();
    if (o==null) return geneIds;
    
    if (o instanceof Iterable) {
      for (Object o2 : ((Iterable)o)) {
        geneIds.addAll(getGeneIds(o2));
      }
      
    } else if (o.getClass().isArray()) {
      for (int i=0; i<Array.getLength(o); i++) {
        geneIds.addAll(getGeneIds(Array.get(o, i)));
      }
      
    } else if (o instanceof mRNA) {
      geneIds.add(((mRNA)o).getGeneID());
      
    } else if (o instanceof miRNA) {
      miRNA mi = ((miRNA)o);
      if (!mi.hasTargets()) {
        log.warning("miRNA " +o+ " has no annotated targets.");
      } else {
        for (miRNAtarget t: mi.getTargets()) {
          geneIds.add(t.getTarget());
        }
      }
      
    } else if (o instanceof Integer) {
      geneIds.add((Integer)o);
      
    } else if (o instanceof EnrichmentObject) {
      for (Object o2: ((EnrichmentObject)o).getGenesInClass()) {
        geneIds.addAll(getGeneIds(o2));
      }
      
    } else {
      log.severe("Please implement 2GeneID for " + o.getClass());
    }
    
    return geneIds;
  }
  
  /**
   * @param o
   * @return
   */
  public static Collection<NameAndSignals> getGenes(Object o) {
    Set<NameAndSignals> geneIds = new HashSet<NameAndSignals>();
    if (o==null) return geneIds;
    
    if (o instanceof Iterable) {
      for (Object o2 : ((Iterable<?>)o)) {
        geneIds.addAll(getGenes(o2));
      }
      
    } else if (o.getClass().isArray()) {
      for (int i=0; i<Array.getLength(o); i++) {
        geneIds.addAll(getGenes(Array.get(o, i)));
      }
      
    } else if (o instanceof NameAndSignals || NameAndSignals.class.isAssignableFrom(o.getClass())) {
      geneIds.add(((NameAndSignals)o));
            
    } else {
      log.severe("Cannot get NameAndSignals for " + o.getClass());
    }
    
    return geneIds;
  }
  
  /**
   * 
   * @param <T>
   * @param nsList
   * @return an array of [0=minSignal] and [1=maxSignal]
   */
  public static <T extends NameAndSignals> double[] getMinMaxSignalGlobal(Iterable<T> nsList) {
    double[] minMax = new double[]{Double.MAX_VALUE, Double.MIN_VALUE};
    for (NameAndSignals ns: nsList) {
      if (ns.hasSignals()) {
        for (Signal sig: ns.getSignals()) {
          Number signal = sig.getSignal();
          minMax[0] = Math.min(minMax[0], signal.doubleValue());
          minMax[1] = Math.max(minMax[1], signal.doubleValue());
        }
      }
    }
    return minMax;
  }
  
  /**
   * 
   * @param <T>
   * @param nsList
   * @param experimentName
   * @param type
   * @return minimum and maximum for a specific signal in a list of {@link NameAndSignals}.
   */
  public static <T extends NameAndSignals> double[] getMinMaxSignalGlobal(Iterable<T> nsList, String experimentName, SignalType type) {
    double[] minMax = new double[]{Double.MAX_VALUE, Double.MIN_VALUE};
    for (NameAndSignals ns: nsList) {
      Number sig = ns.getSignalValue(type, experimentName);
      if (sig!=null && !Double.isNaN(sig.doubleValue())) {
        minMax[0] = Math.min(minMax[0], sig.doubleValue());
        minMax[1] = Math.max(minMax[1], sig.doubleValue());
      }
    }
    return minMax;
  }
  
  /**
   * @param col any iterable.
   * @return true if and only if this iterable contains {@link NameAndSignals} objects.
   */
  public static boolean isNameAndSignals(Iterable<?> col) {
    if (col==null) return false; // Empty list
    Iterator<?> it = col.iterator();
    if (!it.hasNext()) return false; // Empty list
    
    if (it.next() instanceof NameAndSignals) return true;
    
    return false;
  }
    
}

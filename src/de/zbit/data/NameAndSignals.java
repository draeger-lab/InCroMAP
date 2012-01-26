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
package de.zbit.data;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.RandomAccess;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.tree.TreeNode;

import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAandTarget;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.data.protein.ProteinModificationExpression;
import de.zbit.exception.CorruptInputStreamException;
import de.zbit.gui.IntegratorUITools;
import de.zbit.io.CSVwriteable;
import de.zbit.util.ArrayUtils;
import de.zbit.util.Reflect;
import de.zbit.util.StringUtil;
import de.zbit.util.Utils;
import de.zbit.util.ValuePair;

/**
 * An abstract class that handles something
 * with a name and (multiple or one) Signal(s).
 * <p>When extending this class, please don't forger to
 * implement a public {@link #clone()} method. This is required for
 * many functionalities.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public abstract class NameAndSignals implements Serializable, Comparable<Object>, Cloneable, CSVwriteable, TableResult, TreeNode  {
  private static final long serialVersionUID = 732610412438240532L;
  public static final transient Logger log = Logger.getLogger(NameAndSignals.class.getName());

  /**
   * If only one signal is added, this is the name of the
   * experiment of the default signal.
   */
  public final static String defaultExperimentName = "default";
  
  /**
   * If multiple strings must be merged, this is the separator that
   * is put between all strings to create one.
   */
  public final static String implodeString = ", ";
  
  /**
   * Any best-matching name.<p>Please access using {@link #getName()}, because
   * this may be <code>NULL</code> but {@link #getName()} guarantees to return
   * a non-null String.
   */
  protected String name;
  
  /**
   * The final processed Signal(s).
   * String is the name of the experiment (defaults to {@link #defaultExperimentName}).
   */
  protected List<Signal> signals=null;
  
  /**
   * Allows to build a tree of NameAndSignals.
   */
  private TreeNode parent = null;
  
  /**
   * Can hold any additional data (Mostly strings, such as gene descriptions
   * or additional identifiers).
   */
  private Map<String, Object> additional_data = null;
  
  /**
   * Is linked to {@link #additional_data} and contains true, if this data should
   * NOT be shown to the user (e.g., in columns of tables).
   */
  public static Set<String> additional_data_is_invisible = new HashSet<String>();
  
  /**
   * Simply sets the name of this instance.
   * @param name may only be null if you overwrite the {@link #getName()}
   * method and return a non-null string. Else, this is not allowed to
   * be <code>NULL</code>.
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
    for (int i=0; i<signals.size(); i++) {
      Signal signal = signals.get(i);
      if (signal.getType().equals(type) && signal.getName().equals(experimentName)) {
        boolean removed = signals.remove(signal);
        ret|=removed;
        if (removed) i--;
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
   * Should always return a non-null string, but better don't rely on that.
   * @return the name.
   * @see #name
   */
  public String getName() {
    return name;
  }
  
  /**
   * Be careful, this returns the internal data structure
   * and changes to it also change this ns!!!
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
    return getSignalNames(signals);
  }
  
  /**
   * Returns a list of available Signal Names and Types.
   * @param signals
   * @return
   */
  public static Collection<ValuePair<String, SignalType>> getSignalNames(Iterable<Signal> signals) {
    Set<ValuePair<String, SignalType>> sn = new HashSet<ValuePair<String, SignalType>>();
    if (signals==null) return sn;
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
      if ((type==null || signal.getType().equals(type)) && 
          (experimentName==null || signal.getName().equals(experimentName))) {
        return signal;
      }
    }
    return null;
  }
  
  /**
   * @param type if null, this parameter will be ignored.
   * @param experimentName if null, this parameter will be ignored.
   * @param m {@link MergeType}, determine how to merge the signal values.
   * @return merged value of all signals, matching the input parameters.
   */
  public double getSignalMergedValue(SignalType type, String experimentName, MergeType m) {
    if (signals==null) return Double.NaN;
    List<Signal> matches = new LinkedList<Signal>();
    for (Signal signal : signals) {
      if ((type==null || signal.getType().equals(type)) && 
          (experimentName==null || signal.getName().equals(experimentName))) {
        matches.add(signal);
      }
    }
    
    return Signal.mergeAll(matches, m);
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
    String name = getName();
    if (o instanceof NameAndSignals) {
      NameAndSignals ot = (NameAndSignals) o;
      r = name.compareTo(ot.getName());
    } else {
      r= name.compareTo(o.toString());
    }
    
    return r;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  
  /**
   * Returns a CLONED, gene-centered collection of the given {@link NameAndSignals}s.
   * <p>Gene centering is performed by name, thus, {@link miRNA}s are centered by the
   * miRNA, and not by the target.
   * @param nameAndSignals
   * @return gene centered list (merged and cloned items).
   * If list was already gene-centered, simply returns the same list.
   */  
  public static <T extends NameAndSignals> Collection<T> geneCentered(Collection<T> nameAndSignals) {
    return geneCentered(nameAndSignals, MergeType.AskUser);
  }
  
  /**
   * Returns a CLONED, gene-centered collection of the given {@link NameAndSignals}s.
   * <p>Gene centering is performed by GeneID or name. Thus, {@link miRNA}s are centered by the
   * miRNA name, and not by the target. mRNAs and other {@link GeneID}s are centered by geneID and
   * {@link ProteinModificationExpression} are centered by their {@link #getUniqueLabel()}, i.e.
   * a mixture of protein name and modification name.
   * @param nameAndSignals
   * @param m {@link MergeType}
   * @return gene centered list (merged and cloned items).
   * If list was already gene-centered, simply returns the same list.
   */
  public static <T extends NameAndSignals> Collection<T> geneCentered(Collection<T> nameAndSignals, MergeType m) {
//    if (m==null || m.equals(MergeType.AskUser)) m = IntegratorGUITools.getMergeType();
//    
//    // Group data by name (or gene ID)
//    Map<String, Collection<T>> group = group_by_name(nameAndSignals);
//    
//    Collection<T> toReturn = new ArrayList<T>();
//    for (String mi : group.keySet()) {
//      Collection<T> col = group.get(mi);
//      toReturn.add(merge(col, m));
//    }
//      
//      
//    return toReturn;
    return geneCentered(nameAndSignals, null, m);
  }
  
  /**
   * A special implementation to merge certain objects only.
   * 
   * @param <T> any {@link NameAndSignals}
   * @param nameAndSignals list to merge certain items
   * @param groupIdentifiersToMerge a collection with arrays of identifiers that should be merged. Identifier Type is at follow:
   * <ul><li>GeneID (Integer) for {@link mRNA}s</li>
   * <li>Recursive for genesInClass() for {@link EnrichmentObject}s</li>
   * <li>{@link #getUniqueLabel()} for {@link ProteinModificationExpression}</li>
   * <li>{@link #name} for all others (e.g., {@link miRNA}s)</li></ul>
   * @param m
   * @return collection with the given items merged (and cloned), <b>and all other items untouched</b>!
   * If list was already gene-centered, simply returns the same list.
   */
  public static <T extends NameAndSignals> Collection<T> geneCentered(Collection<T> nameAndSignals, Collection<Object[]> groupIdentifiersToMerge, MergeType m) {
    if (m==null || m.equals(MergeType.AskUser)) m = IntegratorUITools.getMergeType();
    // Automatic should be accepted by all merge and calculate methods.
    //if (m.equals(MergeType.Automatic)) m = IntegratorUITools.autoInferMergeType(null); // Returns mean.
    
    // Group data by name (or gene ID)
    Map<String, Collection<T>> group = group_by_name(nameAndSignals, false, true);
    
    // Eventually return already gene-centered list
    if (group==null) {
      // Set gene-centered mark
      for (NameAndSignals ns : nameAndSignals) {
        if (ns instanceof NSwithProbes) {
          ((NSwithProbes) ns).setGeneCentered(true);
        } else {
          break; // Mixed content is not allowed. => all not NSwithProbes
        }
      }
      
      return nameAndSignals;
    }
    
    Collection<T> toReturn = new ArrayList<T>();
    if (groupIdentifiersToMerge==null) {
      // => Merge data gene-centered
      for (String mi : group.keySet()) {
        Collection<T> col = group.get(mi);
        T merged = merge(col, m);
        if (merged instanceof NSwithProbes) {
          ((NSwithProbes) merged).setGeneCentered(true);
        }
        toReturn.add(merged);
      }
      
    } else {
      
      // Only merge geneIDs, as specified by geneIDsToMerge.
      Set<String> unprocessedItems = new HashSet<String>(group.keySet());
      for (Object[] merge : groupIdentifiersToMerge) {
        Collection<T> col = new LinkedList<T>();
        for (Object mi : merge) {
          Collection<T> col2 = group.get(mi.toString());
          if (col2!=null) {
            col.addAll(col2);
            unprocessedItems.remove(mi.toString());
          }
        }
        // XXX: Gene IDs are erased when merging probes with different gene ids.
        if (col.size()>0) toReturn.add(merge(col, m));
      }
      
      // Add all unprocessedItems at the end (items, that should not be merged at all).
      for (String item: unprocessedItems) {
        Collection<T> col = group.get(item);
        // do NOT merge these items.
        if (col!=null) {
          toReturn.addAll(col);
        }
      }
    }
      
      
    return toReturn;
  }
  
  /**
   * FORCES to gene-center any NS dataset by geneID.
   * <p>You should use {@link #geneCentered(Collection)} if you are not sure which method
   * to use!</p>
   * @param <T>
   * @param nameAndSignals
   * @see #geneCentered(Collection, MergeType)
   * @return
   */
  public static <T extends NameAndSignals> Collection<T> geneCenteredByGeneID(Collection<T> nameAndSignals, MergeType m) {
    // Group data by name (or gene ID)
    Map<String, Collection<T>> group = group_by_name(nameAndSignals, true, false);
    
    Collection<T> toReturn = new ArrayList<T>();
    // => Merge data gene-centered
    for (String mi : group.keySet()) {
      Collection<T> col = group.get(mi);
      T merged = merge(col, m);
      if (merged instanceof NSwithProbes) {
        ((NSwithProbes) merged).setGeneCentered(true);
      }
      toReturn.add(merged);
    }
    
    return toReturn;
  }
  
  /**
   * Merge a collection of objects to one, e.g., by concatenating strings
   * and taking the {@link MergeType} (e.g. mean) of all numeric values.
   * <p>CAREFULL: FAILS for classes that have private variables OTHER
   * than signals and additional data  (e.g. {@link EnrichmentObject})!
   * @param <T> the implementing class ({@link NameAndSignals} derived)
   * @param c collection of objects to merge
   * @param m {@link MergeType} describing how to merge the signals.
   * Accepts {@link MergeType#Automatic}.
   * @return
   */
  @SuppressWarnings("unchecked")
  public static <T extends NameAndSignals> T merge(Collection<T> c, MergeType m) {
    // Create a new instance of T and collect all names and signals
    Set<String> names = new HashSet<String>();
    List<Signal> signals = new ArrayList<Signal>();
    Map<String, List<Object>> add_data = new HashMap<String, List<Object>>();
    T newObject=null;
    // FIXME: Double-Signals are being converted to floats here.... try with a collection of size 1 and debug!
    // Collect all Signals, Names and additional data
    for (T ns : c) {
      // Create a new instance of T (if not yet done so)
      if (newObject==null) {
        try {
          newObject = (T) ns.clone();
        } catch (CloneNotSupportedException e) {
          log.log(Level.SEVERE,"Could not clone extending object.", e);
        }
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
      newObject.name = ArrayUtils.implode(names.toArray(new String[0]), implodeString);
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
   * Creates a list that maps {@link NameAndSignals} identifiers (geneID or name) to the actual {@link NameAndSignals}
   * <p>Note: If the identifier is a String, it will be <b>UPPERCASED AND TRIMMED</b>!.
   * @return a list that maps {@link NameAndSignals} identifiers (geneID or name) to the actual {@link NameAndSignals}
   * @see #getIdentifier(NameAndSignals)
   */
  public static <T extends NameAndSignals> Map<Object, List<T>> getNSIdentifierToNSmap(Collection<T> nsList) {
    Map<Object, List<T>> ret = new HashMap<Object, List<T>>();
    for (T ns: nsList) {
      Object id = NameAndSignals.getIdentifier(ns);
      List<T> list = ret.get(id);
      if (list==null) {
        list = new ArrayList<T>();
        if (id instanceof String) id=id.toString().toUpperCase().trim();
        ret.put(id, list);
      }
      list.add(ns);
    }
    
    return ret;
  }
  
  /**
   * @return geneID for {@link mRNA}s and {@link #getName()} for others.
   * @see #getIdentifierType(NameAndSignals)
   */
  public static <T extends NameAndSignals> Object getIdentifier(T ns) {
    int idType = getIdentifierType(ns);
    if (idType==1) {
      return ((GeneID)ns).getGeneID();
    } else if (idType==3) {
      return ns.getData(EnrichmentObject.idKey);
    } else {
      return ns.getName();
    }
  }
  
  /**
   * Get the identifier that should be used to gene-center this
   * NameAndSignal.
   * @param <T>
   * @param ns
   * @return 1 for GeneID (mRNA).getGeneID() or 0 for {@link #getName()}
   * @see #getType(Object)
   */
  public static <T extends NameAndSignals>  int getIdentifierType(T ns) {
    return getIdentifierType(getType(ns));
  }

  /**
   * Get the identifier that should be used to gene-center this
   * NameAndSignal.
   * @param <T>
   * @param nsClass class of the {@link NameAndSignals}.
   * @return 1 for GeneID ({@link GeneID#getGeneID()}) or 0 for {@link #getName()}
   * @see #getType(Object)
   */
  public static int getIdentifierType(Class<? extends NameAndSignals> nsClass) {
    if (GeneID.class.isAssignableFrom(nsClass) &&
        !miRNA.class.isAssignableFrom(nsClass)) {
      // Label
      return 1;
      // Method is called to identify corresponding genes in a pathway
      // => We need the geneID for ProteinModificationExpression
//    } else if (ProteinModificationExpression.class.isAssignableFrom(nsClass)) {
//      return 2;
    } else if (EnrichmentObject.class.isAssignableFrom(nsClass)) {
      // enrichment (e.g. kegg) id
      return 3;
    } else {
      // GeneID
      return 0;
    }
  }
  
  /**
   * Sorts and returns the given {@link NameAndSignals} by {@link #getUniqueLabel()}.
   * @param <T>
   * @param nsList
   * @return
   */
  public static <T extends NameAndSignals> List<T> sortByUniqueLabel(Collection<T> nsList) {
    List<T> l;
    if (nsList instanceof List) {
      l = (List<T>) nsList;
    } else {
      l = new ArrayList<T>(nsList);
    }
    
    // Create comparator that does the desired sorting
    Comparator<T> sortByUniqueLabel = new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        return o1.getUniqueLabel().compareTo(o2.getUniqueLabel());
      }
    };
    
    // Sort and return list
    Collections.sort(l,sortByUniqueLabel);
    return l;
  }
  
  /**
   * Sorts and returns the given {@link NameAndSignals} by
   * AbsoluteMax for Fold changes,
   * Min for p-values, and
   * Max for others.
   * @param <T>
   * @param nsList
   * @return
   */
  public static <T extends NameAndSignals> List<T> sortBySignificance(Iterable<T> nsList, final String experimentName, final SignalType type) {
    List<T> l = Utils.IterableToList(nsList);
    
    // Create comparator that does the desired sorting
    Comparator<T> sortByUniqueLabel = new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        double sig1 = o1.getSignalValue(type, experimentName).doubleValue();
        double sig2 = o2.getSignalValue(type, experimentName).doubleValue();
        if (type.equals(SignalType.FoldChange)) {
          // Return absolute max
          sig1 = Math.abs(sig1);
          sig2 = Math.abs(sig2);
          return (sig1<sig2 ? 1 : (sig1==sig2 ? 0 : -1));
        } else if (type.equals(SignalType.pValue)) {
          // Return min
          return (sig1<sig2 ? -1 : (sig1==sig2 ? 0 : 1));
        } else {
          // Return max
          return (sig1<sig2 ? 1 : (sig1==sig2 ? 0 : -1));
        }
      }
    };
    
    // Sort and return list
    Collections.sort(l,sortByUniqueLabel);
    return l;
  }
  
  /**
   * Abstract merge functionality that can merge strings, numbers
   * and extensions of {@link NameAndSignals}.
   * @param c collection that contains object of exactly one class!
   * @param m {@link MergeType} that defines how to merge numbers.
   * Accepts also {@link MergeType#Automatic}.
   * @return
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static Object mergeAbstract(Collection c, MergeType m) {
    if (c==null ||c.size()<1) return null;
    Object o = c.iterator().next();
    if (o==null) return null;
    
    // TODO: Add File, Character and Color (not important - unused currently)
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
      // Case 4: Numeric values (XXX: Integers may get converted to doubles here!)
      return Signal.calculate(m, c);
      
    } else if (Boolean.class.isAssignableFrom(o.getClass()) || o instanceof Boolean) {
      boolean ret = true;
      for (Object o2: c) {
        ret &= NSwithProbes.getBooleanValue(o2);
        if (!ret) break;
      }
      return ret;
      
    } else {
      // Case 5: Strings or anything else.
      Set<Object> names = new HashSet<Object>();
      for (Object object : c) {
       names.add(object) ;
      }
      Object toReturn = null;
      if (o instanceof String) {
        toReturn = ArrayUtils.implode(names.toArray(new String[0]), implodeString);
      } else {
        toReturn = names.toArray();
      }
      if (!toReturn.getClass().equals(o.getClass())) {
        log.warning("Merged " + o.getClass().getSimpleName() + " to " + toReturn.getClass().getSimpleName());
      }
      return toReturn;
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
   * @param m {@link MergeType} describing how to merge numeric values. Please also accept {@link MergeType#Automatic}.
   */
  protected abstract <T extends NameAndSignals> void merge(Collection<T> source, T target, MergeType m);

  /**
   * Groups the given {@link NameAndSignals}s by <ul>
   * <li>GeneID for {@link mRNA}s (or {@link GeneID}s)</li>
   * <li>Recursive for genesInClass() for {@link EnrichmentObject}s</li>
   * <li>{@link ProteinModificationExpression#getUniqueLabel()}</li>
   * <li>Name for all others (e.g., {@link miRNA}s)</li></ul>
   * 
   * @param nsList collection of {@link NameAndSignals}s.
   * @param forceGroupByGeneID forces grouping by GeneID for all items.
   * @param returnNullIfListIsGeneCentered returns null if the list was already gene-centered.
   * A list is gene-centered, if each group has a size of exactly 1.
   * @return A map with group identifier (as described above) to all belonging {@link NameAndSignals}.
   * E.g., a map with miRNA names ({@link miRNA#getName()}) as keys and all belonging {@link miRNA}s as values.
   * Or anything else extending {@link NameAndSignals}.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <T extends NameAndSignals> Map<String, Collection<T>> group_by_name(Collection<T> nsList,
    boolean forceGroupByGeneID, boolean returnNullIfListIsGeneCentered) {
    boolean atLeastOneListHasMoreThanOneItem=false;
    
    // Group by miRNA name
    Map<String, Collection<T>> ret = new HashMap<String, Collection<T>>();
    
    T mi = null;
    Iterator<T> it = nsList.iterator();
    while (it.hasNext()) {
      mi = it.next();
      
      // Center objects by name and mRNAs be geneId.
      String name = mi.getName();
//      if (!(mi instanceof miRNA) && mi instanceof GeneID) {
//        name = Integer.toString(((GeneID)mi).getGeneID());
//      } else 
      if (forceGroupByGeneID && mi instanceof GeneID) {
        name = Integer.toString(((GeneID)mi).getGeneID());
      }
      else if (mi instanceof EnrichmentObject) {
        // A little bit more complicated for EnrichmentObjects,
        // return a list of genes in the class here!
        Iterator gic = ((EnrichmentObject)mi).getGenesInClass().iterator();
        if (!gic.hasNext()) continue;
        Object listType = gic.next();
        if (listType instanceof NameAndSignals) {
          ret.putAll(group_by_name(((EnrichmentObject)mi).getGenesInClass(), forceGroupByGeneID, returnNullIfListIsGeneCentered));
        } else {
          log.severe("Cannot gene center list of: " + listType.getClass());
        }
        continue;
      } else if (mi instanceof ProteinModificationExpression){
        // Important: since we have multiple phosphoforms for one
        // protein, do not group them by GeneID! AnalyteID maps to
        // multiple GeneIDs!
        name = mi.getUniqueLabel();
        
      } else {
        name = getIdentifier(mi).toString();
      }
      
      // Put into map
      Collection<T> col = ret.get(name);
      if (col==null) {
        col = new ArrayList<T>();
        ret.put(name, col);
      } else {
        atLeastOneListHasMoreThanOneItem=true;
      }
      col.add(mi);
    }
    
    // If list was already gene-centered, return this information.
    if (!atLeastOneListHasMoreThanOneItem && returnNullIfListIsGeneCentered) {
      return null;
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
  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected <T extends NameAndSignals> void cloneAbstractFields(T target, T source) {
    target.name = new String(source.getName());
    target.signals = (List<Signal>) cloneCollection(source.signals);
    if (additional_data==null) {
      target.additional_data = null;
    } else {
      target.additional_data = (Map<String, Object>) ((HashMap)additional_data).clone();
    }
  }
  
  /**
   * Tries to clone a collection. If this is not possible, a new
   * {@link ArrayList} is created, all elements of the source collection
   * are being put there and a cloned instance of this list is returned.
   * @param <T>
   * @param col
   * @return
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
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
  
  /**
   * Remove any additional data object
   * @param key
   */
  public void removeData(String key) {
    if (additional_data==null) return;
    additional_data.remove(key);
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
    r.append("[" + className + " " + getName() + " ");
    
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
        //if (additional_data_is_invisible.contains(key)) continue; // Output everything here
        r.append(key + ":" + additionalDataToString(key, additional_data.get(key)));
        if (it.hasNext()) r.append(implodeString);
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
    //return additional_data==null?0:additional_data.size();
    int size=0;
    if (additional_data!=null) {
      for (String key : additional_data.keySet()) {
        if (additional_data_is_invisible.contains(key)) continue;
        size++;
      }
    }
    return size;
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
      return signal2columnName(sig);
    } else if (columnIndex >= afterSignals && columnIndex<afterSignals+getNumberOfAdditionalData()) {
      columnIndex-=afterSignals;
      Iterator<String> it = additional_data.keySet().iterator();
      int i=0;
      while (it.hasNext()) {
        String name = it.next();
        if (additional_data_is_invisible.contains(name)) continue;
        if (columnIndex==i++) {
          return StringUtil.formatOptionName(name);
        };
      }
    }
    return "";
  }

  /**
   * Makes a string out of a signal. Usually "SignalName [type]"
   * @param sig
   * @return
   */
  public static String signal2columnName(Signal sig) {
    return sig.getName() + " [" + sig.getType().toString() + "]";
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
    if (columnIndex == 0) return this.getName();
    else if (columnIndex >= 1 && columnIndex<signalStart)
      return extensions[columnIndex-1];
    else if (columnIndex >= signalStart && columnIndex<afterSignals)
      return signals.get(columnIndex-signalStart);
    else if (columnIndex >= afterSignals && columnIndex<afterSignals+getNumberOfAdditionalData()) {
      columnIndex-=afterSignals;
      Iterator<String> it = additional_data.keySet().iterator();
      int i=0;
      while (it.hasNext()) {
        
        String key = it.next();
        if (additional_data_is_invisible.contains(key)) continue;
        if (columnIndex==i++) {
          Object add =  additional_data.get(key);
          add = additionalDataToString(key, add);
          // Do not return null. Number of cols must be solid and thus,
          // must not be null for unset additional data.
          return (add!=null?add:"");
        };
        
      }
    }
    return null;
  }

  /**
   * Use this method to create a separate toString() method
   * for all items you might have put into {@link #additional_data}.
   * Simply return <code>value</code> again to use the normal
   * toString() method of the object.
   * @param key
   * @param value
   * @return
   */
  protected Object additionalDataToString(String key, Object value) {
    // Intentionally left blank
    return value;
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
  
  /**
   * Allows to build a tree of {@link NameAndSignals}.
   * <p>Note: This does not register this node as a Child on the parent!
   * You have to do this manually.
   * @param parent
   */
  public void setParent(TreeNode parent) {
    this.parent = parent;
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
      
    } else if (o instanceof miRNA) {
      miRNA mi = ((miRNA)o);
      if (!mi.hasTargets()) {
        //log.warning("miRNA " +o+ " has no annotated targets.");
      } else {
        // XXX: We now have also geneIDs for miRNAs... when to use
        // target and when to use real geneIDs? (e.g., Targets for most enrichments)
        // => Currently, we still take the name for miRNAs (problem with mapping
        // those "systematic name"-String on miRNAs (*, -3p, etc.).
        for (miRNAtarget t: mi.getTargets()) {
          geneIds.add(t.getTarget());
        }
      }
      
    } else if (o instanceof miRNAtarget) {
      geneIds.add(((miRNAtarget)o).getTarget());
      
    } else if (o instanceof miRNAandTarget) {
      geneIds.add(((miRNAandTarget)o).getTarget().getTarget());
      
    } else if (o instanceof Integer) {
      geneIds.add((Integer)o);
      
    } else if (o instanceof EnrichmentObject) {
      for (Object o2: ((EnrichmentObject)o).getGenesInClass()) {
        geneIds.addAll(getGeneIds(o2));
      }
      
    } else if (o instanceof GeneID) {
      // This must be below miRNAs !
      geneIds.add(((GeneID)o).getGeneID());
      
    } else {
      log.severe("Please implement 2GeneID for " + o.getClass());
    }
    
    return geneIds;
  }
  
  /**
   * Get {@link NameAndSignals} from o
   * @param o any iterable, array or combinations of both over {@link NameAndSignals} or derived classes.
   * @return
   */
  public static Collection<NameAndSignals> getGenes(Object o) {
    Set<NameAndSignals> geneIds = new HashSet<NameAndSignals>();
    if (o==null) return geneIds; // return an empty set
    
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
   * Counts the number of unique genes in the list and returns them (as geneIDs).
   * For {@link miRNA}s, all target gene ids are taken and for everything else
   * (that implements {@link GeneID}, the gene id is taken.
   * @param nsList any gene list. Makes especially sense for <b>heterogeneous</b> lists!
   * @return
   */
  public static Collection<Integer> getAllUniqueGenes(Collection<? extends NameAndSignals> nsList) {
    Set<Integer> ret = new HashSet<Integer>();
    if (nsList==null) return ret;
    
    // Count all geneIDs
    Iterator<? extends NameAndSignals> it = nsList.iterator();
    while (it.hasNext()) {
      NameAndSignals ns = it.next();
      if (ns instanceof miRNA) {
        if (((miRNA) ns).hasTargets()) {
          for (miRNAtarget target: ((miRNA) ns).getTargets()) {
            ret.add(target.getTarget());
          }
        }
      } else if (ns instanceof GeneID) {
        ret.add(((GeneID) ns).getGeneID());
      }
    }
    
    // Remove default gene id (= n/a)
    ret.remove(GeneID.default_geneID);
    
    return ret;
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
   * 
   * @param <T>
   * @param nsList
   * @param experimentName
   * @param type
   * @param quantile
   * @return
   */
  public static <T extends NameAndSignals> double[] getMinMaxSignalQuantile(Collection<T> nsList, String experimentName, SignalType type, int quantile) {
    List<Number> signalValues = new ArrayList<Number>(nsList.size());
    for (NameAndSignals ns: nsList) {
      Number sig = ns.getSignalValue(type, experimentName);
      signalValues.add(sig);
    }
    
    double upperQuant = Utils.quantile(signalValues, quantile, false);
    double lowerQuant = Utils.quantile(signalValues, 100-quantile, true);
    return new double[]{lowerQuant, upperQuant};
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
  
  /**
   * @param col any iterable of a contant data type (no mixed classes in the Iterable are allowed).
   * @return true if and only if this iterable contains {@link miRNA} OR EXTENDED objects.
   * Thus, it is also true for {@link miRNAandTarget}s.
   */
  public static boolean isMicroRNA(Iterable<?> col) {
    // XXX: One could generalize these isMiRNA methods and make
    // isGeneID annotated in dataset or similar.
    if (col==null) return false; // Empty list
    Iterator<?> it = col.iterator();
    if (!it.hasNext()) return false; // Empty list
    
    if (it.next() instanceof miRNA) return true;
    
    return false;
  }
  
  /**
   * @param col
   * @return true if and only if <code>col</code> contains any instance
   * of {@link miRNA}.
   */
  public static boolean containsMicroRNA(Iterable<?> col) {
    if (col==null) return false; // Empty list
    Iterator<?> it = col.iterator();
    while (it.hasNext()) {
      if (it.next() instanceof miRNA) return true;
    }
    return false;
  }
  
  /**
   * Get the class for o. In case of {@link EnrichmentObject}s the type of enriched objects
   * (<code>getGenesInClass()</code>) is returned.
   * @param o any iterable, array or combinations of both over {@link NameAndSignals} or derived classes.
   * @return any {@link NameAndSignals} derived class or {@link NameAndSignals#getClass()} if unknown.
   */
  @SuppressWarnings({ "unchecked" })
  public static Class<? extends NameAndSignals> getType(Object o) {
    
    if (o==null) {
      return NameAndSignals.class;
      
    } else if (o instanceof Iterable) {
      for (Object o2 : ((Iterable<?>)o)) {
        return (getType(o2));
      }
      
    } else if (o.getClass().isArray()) {
      if (Array.getLength(o)>0) {
        return (getType(Array.get(o, 0)));
      }
      
      // EnrichmentObjects now can be used to color pathway-reference nodes
//    } else if (o instanceof EnrichmentObject || EnrichmentObject.class.isAssignableFrom(o.getClass())) {
//      // Recurse into enriched objects
//      return getType(((EnrichmentObject)o).getGenesInClass());
      
    } else if (o instanceof NameAndSignals || NameAndSignals.class.isAssignableFrom(o.getClass())) {
      return (Class<? extends NameAndSignals>) o.getClass();
      
    } else {
      log.severe("Cannot get NameAndSignals Class for " + o.getClass());
    }
    
    return NameAndSignals.class;
  }
  

  /**
   * @return a nice {@link String} identifier that can be used, e.g. for nodes
   * and as uniquely and as short as possible describes this element. E.g. for
   * mRNAs, the probe name should be included next to the GeneSymbol.
   */
  public abstract String getUniqueLabel();
    
  
  /*
   * From TreeNode, everything is implemented in a way that this NameAndSignals
   * is a leaf node.
   */

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeNode#getChildAt(int)
   */
  @Override
  public TreeNode getChildAt(int childIndex) {
    List<? extends TreeNode> childs = getChildrenList();
    return (childs==null||childIndex>=childs.size())?null:childs.get(childIndex);
  }

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeNode#getChildCount()
   */
  @Override
  public int getChildCount() {
    List<? extends TreeNode> list = getChildrenList();
    return list==null?0:list.size();
  }

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeNode#getParent()
   */
  @Override
  public TreeNode getParent() {return parent;}

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
   */
  @Override
  public int getIndex(TreeNode node) {
    // Inefficient default implementation
    List<? extends TreeNode> childs = getChildrenList();
    return childs==null?-1:childs.indexOf(node);
  }

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeNode#getAllowsChildren()
   */
  @Override
  public boolean getAllowsChildren() {return !isLeaf();}

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeNode#isLeaf()
   */
  @Override
  public boolean isLeaf() {return getChildCount()<=0;}

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeNode#children()
   */
  @Override
  public Enumeration<?> children() {
    List<? extends TreeNode> childs = getChildrenList();
    return childs==null?null:Collections.enumeration(childs);
  }
  
  /**
   * If this node has children, overwrite this method and return them!
   * @return a {@link RandomAccess} list!
   */
  public List<? extends TreeNode> getChildrenList() {
    return null;
  }

  /**
   * @return a {@link Comparator} that compares {@link GeneID}s.
   */
  public static Comparator<GeneID> getGeneIdComparator() {
    return new Comparator<GeneID>() {
      @Override
      public int compare(GeneID o1, GeneID o2) {
        return o1.getGeneID()-o2.getGeneID();
      }
    };
  }

  /**
   * Extract a specified {@link Signal} from a list of {@link NameAndSignals}.
   * @param <T>
   * @param nsList
   * @param experimentName
   * @param type
   * @return List with {@link Signal}s from all {@link NameAndSignals}
   * in <code>nsList</code>.
   */
  public static <T extends NameAndSignals> List<Signal> getSignals(Collection<T> nsList,
    String experimentName, SignalType type) {
    List<Signal> ret = new ArrayList<Signal>(nsList!=null?nsList.size():1);
    if (nsList==null) return ret;
    
    Iterator<T> it = nsList.iterator();
    while (it.hasNext()) {
      Signal signal = it.next().getSignal(type, experimentName);
      if (signal!=null) ret.add(signal);
    }
    
    return ret;
  }

  /**
   * Removes all objects that implement {@link GeneID}, but have an
   * unset or invalid GeneID.
   * @param newList
   */
  public static void removeGenesWithoutGeneID(Iterable<? extends NameAndSignals> newList) {
    Iterator<? extends NameAndSignals> it = newList.iterator();
    while (it.hasNext()) {
      NameAndSignals o = it.next();
      if (o instanceof GeneID) {
        int geneID = ((GeneID) o).getGeneID();
        if (geneID<=0) {
          it.remove();
        }
      }
    }
  }
  
}

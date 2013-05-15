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
package de.zbit.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.analysis.enrichment.AbstractEnrichment;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.math.EnrichmentPvalue;
import de.zbit.math.HypergeometricTest;
import de.zbit.math.MathUtils;
import de.zbit.util.ArrayUtils;
import de.zbit.util.StringUtil;

/**
 * An EnrichmentObject is a result of an enrichment analysis
 * {@link AbstractEnrichment}.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class EnrichmentObject<EnrichIDType> extends NameAndSignals {
  private static final long serialVersionUID = -803654750176179631L;
  public static final transient Logger log = Logger.getLogger(EnrichmentObject.class.getName());

  /**
   * Enriched genes from GeneList in current Class (e.g., pathway or GO term)
   */
  private int c_enriched;
  
  /**
   * Total genes in source GeneList.
   */
  private int c_total;
  
  /**
   * Total genes in current Class (e.g., pathway or GO term)
   */
  private int b_subset;
  
  /**
   * Total genes in genome
   */
  private int b_total;
  
  /**
   * All genes from the source list in this class.
   */
  private Collection<?> genesInClass;
  
  /**
   * The pValue calculator
   */
  private EnrichmentPvalue pValCalculator=null;
  
  /**
   * This is the key, under which the identifier is stored in the {@link #addData(String, Object)} list.
   */
  public final static String idKey = "id";
  
  /**
   * This name is used as experiment name in signals.
   */
  public final static String signalNameForPvalues = "Enrichment";
  
  /**
   * @see #EnrichmentObject(String, String, int, int, int, int, double, double, Collection)
   * @param name
   */
  private EnrichmentObject(String name) {
    super(name);
  }
  
  /**
   * Special constructor that allows loading external enrichment results
   * @param name
   * @param identifier
   */
  public EnrichmentObject(String name, EnrichIDType identifier) {
    this(name);
    if (identifier!=null) addData(idKey, identifier);
  }
  
  /**
   * @see #EnrichmentObject(String, String, int, int, int, int, double, double, Collection)
   * @param name
   * @param c_enriched
   * @param c_total
   */
  private EnrichmentObject(String name, int c_enriched, int c_total) {
    this(name);
    this.c_enriched=c_enriched;
    this.c_total=c_total;
  }
  
  /**
   * Initialize a new enrichment holder object.
   * This will initialize a new {@link EnrichmentPvalue} calculator and thus, is slower
   * than using another constructor with a fixed pValue calculator.
   * @see #EnrichmentObject(String, String, int, int, int, int, double, double, Collection)
   * @param name
   * @param identifier
   * @param c_enriched
   * @param c_total
   * @param b_subset
   * @param b_total
   * @param genesInClass
   */
  public EnrichmentObject(String name, EnrichIDType identifier, int c_enriched, int c_total, 
    int b_subset, int b_total, Collection<?> genesInClass) {
    this(name, identifier, c_enriched, c_total, b_subset, b_total, Double.NaN, Double.NaN, genesInClass);
    
  }
  
  /**
   * Initialize a new enrichment holder object.
   * @see #EnrichmentObject(String, String, int, int, int, int, double, double, Collection)
   * @param name
   * @param identifier
   * @param c_enriched
   * @param c_total
   * @param b_subset
   * @param b_total
   * @param pValCalculator Calculator class that is used to calculate the pValue. This has to
   * be initialized for this problem size (i.e. c_total and b_total!)
   * @param genesInClass
   */
  public EnrichmentObject(String name, EnrichIDType identifier, int c_enriched, int c_total, 
    int b_subset, int b_total, EnrichmentPvalue pValCalculator, Collection<?> genesInClass) {
    this(name, identifier, c_enriched, c_total, b_subset, b_total, Double.NaN, Double.NaN, genesInClass);
    
    this.setPValCalculator(pValCalculator);
  }
  
  
  /**
   * Initialize a new enrichment holder object.
   * @param name Enrichment class name (e.g., pathway name)
   * @param identifier Enrichment class identifier (e.g., kegg identifier)
   * @param c_enriched Enriched genes from GeneList in current Class (e.g., pathway or GO term)
   * @param c_total Total genes in source GeneList.
   * @param b_subset Total genes in current Class (e.g., pathway or GO term)
   * @param b_total Total genes in Genome
   * @param pValue the pValue
   * @param qValue The qValue (after, e.g., {@link PathwayEnrichment#BenjaminiHochberg_pVal_adjust(java.util.List)}
   * statistical correction).
   * @param genesInClass a collection of all {@link mRNA}s (or other gene identifiers) of all genes
   * from the source list in this enrichment class.
   */
  public EnrichmentObject(String name, EnrichIDType identifier, int c_enriched, int c_total, 
    int b_subset, int b_total, double pValue, double qValue, Collection<?> genesInClass) {
    this(name, c_enriched, c_total);
    this.b_subset = b_subset;
    this.b_total=b_total;
    this.genesInClass = genesInClass;
    if (identifier!=null) addData(idKey, identifier);
    
    if (!Double.isNaN(qValue)) setQValue(qValue);
    if (!Double.isNaN(pValue)) setPValue(pValue);
  }
  
  
  /**
   * @see #pValue
   * @return the pValue
   */
  public Number getPValue() {
    // Try to get the cached pValue
    Signal sig = getSignal(SignalType.pValue, signalNameForPvalues);
    Double pVal = sig!=null?sig.getSignal().doubleValue():null;
    
    // Calculate the pValue if it is not cached
    if (pVal == null || Double.isNaN(pVal)) {
      if (pValCalculator==null) initDefaultPvalueCalculator();
      
      if (pValCalculator!=null) {
        pVal = pValCalculator.getPvalue(b_subset, c_enriched);
        setPValue(pVal);
      }
    }
    
    return pVal;
  }

  /**
   * Init a new hypergeometric test
   */
  private void initDefaultPvalueCalculator() {
    if (b_total>0 && c_total>0) {
      pValCalculator = new HypergeometricTest(b_total, c_total);
    }
  }

  /**
   * @see #pValue
   * @param pValue the pValue to set
   */
  private void setPValue(double pValue) {
    unsetPValue();
    addSignal(pValue, signalNameForPvalues, SignalType.pValue);
  }

  private void unsetPValue() {
    removeSignals(signalNameForPvalues, SignalType.pValue);
  }
  private void unsetQValue() {
    removeSignals(signalNameForPvalues, SignalType.qValue);
  }
  
  /**
   * Set the {@link EnrichmentPvalue} calculator object. This automatically
   * invokes a re-calculation of the current pValue.
   * @param pValCalculator
   */
  public void setPValCalculator(EnrichmentPvalue pValCalculator) {
    // Check if the calculator is valid.
    if (pValCalculator.getGeneListSize()!=c_total || pValCalculator.getGenomeSize()!=b_total) {
      log.log(Level.WARNING, "Tried to set a wrong calculator (total list sizes don't match).");
    } else {
      if (this.pValCalculator==null || !this.pValCalculator.equals(pValCalculator)) {
        this.pValCalculator = pValCalculator;
        
        // Recalculate the pValue
        unsetPValue();
        getPValue();
      }
    }
  }

  /**
   * @return the {@link EnrichmentPvalue} calculator that is used to calculate the pValues
   * in this object.
   */
  public EnrichmentPvalue getPValCalculator() {
    return pValCalculator;
  }

  /**
   * @see #qValue
   * @return the qValue
   */
  public Number getQValue() {
    return getSignalValue(SignalType.qValue, signalNameForPvalues);
  }

  /**
   * @see #qValue
   * @param value the qValue to set
   */
  public void setQValue(double qValue) {
    unsetQValue();
    addSignal(qValue, signalNameForPvalues, SignalType.qValue);
  }

  /**
   * @see #genesInClass
   * @return the genesInClass
   */
  public Collection<?> getGenesInClass() {
    return genesInClass;
  }

  /**
   * @see #genesInClass
   * @param genesInClass the genesInClass to set
   */
  public void setGenesInClass(Collection<?> genesInClass) {
    this.genesInClass = genesInClass;
  }

  /**
   * @see #c_enriched
   * @return the NumberOfEnrichedGenesInClass
   */
  public int getNumberOfEnrichedGenesInClass() {
    return c_enriched;
  }

  /**
   * @see #c_total
   * @return the total number of genes in source list
   */
  public int getTotalGenesInSourceList() {
    return c_total;
  }

  /**
   * @see #b_subset
   * @return the total number of genes in current class
   */
  public int getTotalGenesInClass() {
    return b_subset;
  }

  /**
   * @see #b_total
   * @return the total number of genes in the genome
   */
  public int getTotalGenesInGenome() {
    return b_total;
  }
  
  /**
   * @return the identifier
   */
  @SuppressWarnings("unchecked")
  public EnrichIDType getIdentifier() {
    EnrichIDType o = (EnrichIDType) getData(idKey);
    return (o!=null?o:null);
  }

  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignal#toString()
   */
  @Override
  public String toString() {
    return getSignals().toString();
    //return Arrays.deepToString(toArray());
  }
  
  public Object[] toArray() {
    
    // If you change something here, remember to also change #getColumnName(int).
    Object[] ret = new Object[7];
    
    int i=0;
    ret[i] = getIdentifier();
    ret[++i] = getName();
    if (getNumberOfEnrichedGenesInClass()<=0 && getTotalGenesInSourceList()<=0) {
      //ret[++i] = "N/A";
    } else {
      ret[++i] = new Ratio(getNumberOfEnrichedGenesInClass(), getTotalGenesInSourceList());
    }
    if (getTotalGenesInClass()<=0 && getTotalGenesInGenome()<=0) {
      //ret[++i] = "N/A";
    } else {
      ret[++i] = new Ratio(getTotalGenesInClass(), getTotalGenesInGenome());
    }
    ret[++i] = getPValue();
    ret[++i] = getQValue();
    if (getGenesInClass()==null || getGenesInClass().size()<1) {
      ret[++i] = "Unknown";
    } else {
      ret[++i] = getGenesInClass();
    }
    
    // If array is smaller, resize it.
    if (i<6) {
      return ArrayUtils.resize(ret, i+1, null);
    }
    
    return ret;
  }
  

  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignal#merge(java.util.Collection, de.zbit.data.NameAndSignal, de.zbit.data.Signal.MergeType)
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  protected <T extends NameAndSignals> void merge(Collection<T> source,
    T target, MergeType m) {
    EnrichmentObject t = (EnrichmentObject) target;
    
    Object anyObjectFromGenesInClass = null;
    
    List<Integer> c_enriched = new ArrayList<Integer> (source.size());
    List<Integer> c_total = new ArrayList<Integer> (source.size());
    List<Integer> b_subset = new ArrayList<Integer> (source.size());
    List<Integer> b_total = new ArrayList<Integer> (source.size());
    Set<Object> genesInClass = new HashSet<Object>();
    for (T o: source) {
      EnrichmentObject e = (EnrichmentObject) o;
      c_enriched.add(e.c_enriched);
      c_total.add(e.c_total);
      b_subset.add(e.b_subset);
      b_total.add(e.b_total);
      genesInClass.addAll(e.genesInClass);
      if (anyObjectFromGenesInClass==null && e.genesInClass.size()>0)
        anyObjectFromGenesInClass = e.genesInClass.iterator().next();
    }
    
    t.c_enriched = (int) MathUtils.round(Signal.calculate(m, c_enriched), 0);
    t.c_total = (int) MathUtils.round(Signal.calculate(m, c_total), 0);
    t.b_subset = (int) MathUtils.round(Signal.calculate(m, b_subset), 0);
    t.b_total = (int) MathUtils.round(Signal.calculate(m, b_total), 0);
    
    // Do Not always call mergeAbstract. If list contains geneIDs, they will
    // be averaged, what is really senseless. Though make a type checking!
    if (anyObjectFromGenesInClass instanceof NameAndSignals) {
      Object merged = NameAndSignals.mergeAbstract(genesInClass, m);
      if (merged instanceof Collection) {
        t.genesInClass=(Collection<?>)merged;
      } else {
        t.genesInClass = new LinkedList();
        t.genesInClass.add(merged);
      } 
    } else {
      t.genesInClass=genesInClass;
    }
    
    // Remark: pValue will be dynamically re-calculated.
    // QValue is LOST and has to be re-calculated!
    // XXX: One could also check if this equals (or is close to) the mean!
    // In this case, this section could be removed.
    t.unsetPValue();
    t.unsetQValue();
    t.initDefaultPvalueCalculator(); // b_toatl changed!
    
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return toArray().length;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#toCSV(int)
   */
  @Override
  public String toCSV(int elementNumber) {
    return StringUtil.implode(toArray(), "\t");
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getObjectAtColumn(int)
   */
  @Override
  public Object getObjectAtColumn(int columnIndex) {
    Object[] a = toArray();
    if (columnIndex>=0 && columnIndex<a.length)
      return a[columnIndex];
    else
      return null;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getColumnName(int)
   */
  @Override
  public String getColumnName(int columnIndex) {
    int i=0;
    if (columnIndex==i) return "ID";
    else if (columnIndex==++i) return "Name";
    
    // If enrichments have been loaded, no ratios are available
    else if ((getNumberOfEnrichedGenesInClass()>0 || getTotalGenesInSourceList()>0) &&
        columnIndex==++i) return "List ratio";
    else if ((getTotalGenesInClass()>0 || getTotalGenesInGenome()>0) &&
        columnIndex==++i) return "BG ratio";
    
    else if (columnIndex==++i) return "P-value";
    else if (columnIndex==++i) return "Q-value";// Number 5 is hard coded in "setDefaultInitialSelectionOfJTableFilter()"
    else if (columnIndex==++i) return "Genes/Compounds";
    else return null;
  }

  /**
   * Merge all {@link #getGenesInClass()} to one list.
   * @param geneList
   * @return
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static Collection<?> mergeGeneLists(Iterable<EnrichmentObject> geneList) {
    Set unique = new HashSet();
    for (EnrichmentObject e : geneList) {
      unique.addAll(e.getGenesInClass());
    }
    return unique;
  }

  /**
   * @param geneList
   * @return
   */
  public static <T> Collection<T> getUniqueListOfIdentifiers(List<EnrichmentObject<T>> geneList) {
    Set<T> ret = new HashSet<T>();
    for (EnrichmentObject<T> e : geneList) {
      ret.add(e.getIdentifier());
    }
    return ret;
  }

  /**
   * @return a {@link HashSet} with unique geneIDs from genes in this {@link EnrichmentObject}.
   */
  public Collection<Integer> getGeneIDsFromGenesInClass() {
    /*Set<Integer> unique = new HashSet<Integer>();
    if (genesInClass==null || genesInClass.size()<1) return unique;
    
    for (Object o : genesInClass) {
      if (o instanceof mRNA) {
        unique.add(((mRNA)o).getGeneID());
      } else if (o instanceof Integer) {
        unique.add((Integer)o);
      } else {
        log.log(Level.SEVERE, "Please implement 2GeneId for " + o.getClass());
      }
    }
    
    return unique;*/
    return NameAndSignals.getGeneIds(genesInClass);
  }
  
  /**
   * @param col any iterable.
   * @return true if and only if this iterable contains {@link EnrichmentObject} objects.
   */
  public static boolean isEnrichmentObjectList(Iterable<?> col) {
    if (col==null) return false; // Empty list
    Iterator<?> it = col.iterator();
    if (!it.hasNext()) return false; // Empty list
    
    if (it.next() instanceof EnrichmentObject) return true;
    
    return false;
  }
  
  /**
   * @param col any iterable.
   * @return true if and only if this iterable contains {@link EnrichmentObject} objects.
   */
  @SuppressWarnings("rawtypes")
  public static boolean containsNameAndSignals(Iterable<? extends EnrichmentObject> col) {
    if (col==null) return false; // Empty list
    Iterator<?> it = col.iterator();
    if (!it.hasNext()) return false; // Empty list
    
    EnrichmentObject o = (EnrichmentObject) it.next();
    return (NameAndSignals.isNameAndSignals(o.getGenesInClass()));
  }

  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getUniqueLabel()
   */
  @Override
  public String getUniqueLabel() {
    return getName();
  }

  /**
   * @return true if this is a kegg pathway enrichment.
   */
  public boolean isKEGGenrichment() {
    return getIdentifier().toString().startsWith("path:");
  }
  
}

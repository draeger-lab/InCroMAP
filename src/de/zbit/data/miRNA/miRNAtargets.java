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
package de.zbit.data.miRNA;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import de.zbit.data.ObjectAndScore;
import de.zbit.exception.CorruptInputStreamException;
import de.zbit.io.csv.CSVwriteable;
import de.zbit.math.MathUtils;
import de.zbit.util.ArrayUtils;
import de.zbit.util.SortedArrayList;

/**
 * A class to hold {@link miRNA} and {@link Collection}s of targets.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class miRNAtargets implements Serializable, CSVwriteable, Comparable<miRNAtargets> {
  public static final transient Logger log = Logger.getLogger(miRNAtargets.class.getName());
  private static final long serialVersionUID = -7099618219166758343L;
  
  /**
   * A mapping from miRNAs (official identifiers as string) to
   * a list of targets.
   */
  private Map<String, Collection<miRNAtarget>> targets;
  
  /**
   * Collection of algorithms (important to know if pValue or score is used).
   */
  //private Collection<miRNAtargetPredictionAlgorithm> algorithms;
  
  
  /** Intermediate variable that is required by {@link #toCSV(int)}. */
  private Iterator<String> currentCSVElement = null;
  
  
  /**
   * From http://en.wikipedia.org/wiki/MicroRNA#Nomenclature:
   * 
   * <p>Pre-miRNAs that lead to 100% identical mature miRNAs but that are
   * located at different places in the genome are indicated with an
   * additional dash-number suffix. For example, the pre-miRNAs
   * hsa-mir-194-1 and hsa-mir-194-2 lead to an identical mature miRNA
   * (hsa-miR-194) but are located in different regions of the genome.
   */
  static Pattern identicalMiRNAs = Pattern.compile(".*(MIR|LET)-\\d+[A-Z]{0,1}-\\d");
  
  /**
   * From http://en.wikipedia.org/wiki/MicroRNA#Nomenclature:
   * 
   * <p>miRNAs with nearly identical sequences bar one or two nucleotides
   * are annotated with an additional lower case letter. For example,
   * miR-123a would be closely related to miR-123b.
   */
  static Pattern similarMiRNAs = Pattern.compile(".*(MIR|LET)-\\d+?[A-Z]");
  
  /**
   * Watch out for targets of different organisms! Take care of them
   * before adding them to this list.
   */
  public miRNAtargets() {
    super();
  }
  

  /**
   * Adds a target to the miRNA.  
   * @param miRNA
   * @param target - NCBI Gene ID (Entrez).
   */
  public void addTarget(miRNA miRNA, miRNAtarget target) {
    addTarget(miRNA.getName(), target);
  }

  /**
   * Adds a target to the miRNA.  
   * @param miRNA - official name of the miRNA.
   * @param target - NCBI Gene ID (Entrez).
   */
  public void addTarget(String miRNA, miRNAtarget target) {
    if (targets==null) initializeTargets();
    miRNA = format_miRNA(miRNA);
    
    Collection<miRNAtarget> ctargets = targets.get(miRNA);
    if (ctargets==null) {
      ctargets = initializeTargetCollection();
      targets.put(miRNA, ctargets);
    }
    
    if (!ctargets.contains(target)) {
      ctargets.add(target);
    }
  }
  
  /**
   * This class defines the miRNA uniform name processing
   * and should be called before EVERY call to the {@link #targets}
   * list.
   * @param miRNA
   * @return
   */
  public static String format_miRNA(String miRNA) {
    // Standard formatting: Uppercase and trim
    // XXX: Not good. "miR" is precursor of "MIR".
    miRNA = miRNA.toUpperCase().trim();
    
    // Remove brackets
    if (miRNA.startsWith("["))
      miRNA = miRNA.substring(1);
    if (miRNA.endsWith("]"))
      miRNA = miRNA.substring(0, miRNA.length()-1);
    
    // Replace "STAR" with a *
    if (miRNA.endsWith("STAR")) {
      miRNA = miRNA.substring(0, miRNA.length()-4)+"*";
      if (miRNA.endsWith("-*")) {
        miRNA = miRNA.substring(0, miRNA.length()-2)+"*";
      }
    }
    
    return miRNA;
  }

  /**
   * @return a Collection<miRNAtarget>.
   */
  private Collection<miRNAtarget> initializeTargetCollection() {
    Collection<miRNAtarget> ret = new SortedArrayList<miRNAtarget>();
    return ret;
  }

  /**
   * Inizializes {@link #targets}, if it is null.
   */
  private void initializeTargets() {
    if (targets==null) targets = new HashMap<String, Collection<miRNAtarget>>();
  }


  /**
   * @return Returns the number of miRNAs for which targets are available.
   */
  public int size() {
    return targets==null?0:targets.size();
  }
  
  /**
   * @return Returns the number of targets for all miRNAs in this list.
   * Does NOT skip duplicate targets (coming from different prediction
   * approaches).
   */
  public int sizeOfTargets() {
    if (targets==null) return 0;
    int sum = 0;
    for (Collection<miRNAtarget> targetList: targets.values()) {
      sum+=targetList.size();
    }
    return sum;
  }
  
  /**
   * @return Returns the number of target genes for all miRNAs in this list.
   * Skips duplicate targets.
   */
  public int sizeOfUniqueTargets() {
    return sizeOfUniqueTargets(false);
  }
  /**
   * @param onlyExperimentallyVerified - only count experimentally verified
   * targets.
   * @return Returns the number of target genes for all miRNAs in this list.
   * Skips duplicate targets.
   */
  public int sizeOfUniqueTargets(boolean onlyExperimentallyVerified) {
    if (targets==null) return 0;
    
    Set<Integer> geneIDs = new HashSet<Integer>();
    for (Collection<miRNAtarget> targetList: targets.values()) {
      for (miRNAtarget target: targetList) {
        if (onlyExperimentallyVerified && !target.isExperimental()) continue;
        geneIDs.add(target.getTarget());
      }
    }
    
    return geneIDs.size();
  }
  
  
  /**
   * Returns the list of targets for the miRNA. The miRNA name is
   * case sensitive and must be exactly equal!
   * @param miRNA
   * @return
   */
  public Collection<miRNAtarget> getTargets(String miRNA) {
    if (targets==null) return null;
    
    String preMiRNAname = format_miRNA(miRNA);
    Collection<miRNAtarget> t = targets.get(preMiRNAname);
    
    // If no targets found, try to remove "-1" or "a"
    // but NOT "-3p" / "-5p"
    // XXX: we could make an option here, if we only want unique exact matches
    if ((t==null || t.size()<1) &&  (preMiRNAname.endsWith("*"))) {
      // First, remove the *, indicating an expression level
      preMiRNAname = preMiRNAname.substring(0, preMiRNAname.length()-1);
      t = targets.get(preMiRNAname);
    }
    
    // Removed "(t==null || t.size()<1) &&  ", because here we really have identical miRNAs
    if (identicalMiRNAs.matcher(preMiRNAname).matches()) {
      // Identical miRNAs, coming from transcripts that are located
      // in different parts of the genome
      preMiRNAname = preMiRNAname.substring(0, preMiRNAname.lastIndexOf("-"));
      Collection<miRNAtarget> t2 = targets.get(preMiRNAname);
      
      if (t==null || t.size()<1) {
        t = t2;
      } else if (t2!=null && t2.size()>0){
        t.addAll(t2);
      }
    }
    
    // Removed "(t==null || t.size()<1) &&  ", because if source gives "let-7" we
    // just don't know if they mean "let-7c" or "7f", etc. => take for all.
    if ( ((similarMiRNAs.matcher(preMiRNAname).matches())) ) {
      // remove acdef suffix
      preMiRNAname = preMiRNAname.substring(0, preMiRNAname.length()-1);
      Collection<miRNAtarget> t2 = targets.get(preMiRNAname);
      
      if (t==null || t.size()<1) {
        t = t2;
      } else if (t2!=null && t2.size()>0){
        t.addAll(t2);
      }
    }
    
    return t;
  }


  /**
   * @param t
   */
  public void addAll(miRNAtargets t) {
    if (t==null || t.targets==null) return;
    if (targets==null) initializeTargets();
    
    // This is WRONG because it replaces the collections instead appending the collection content.
    //targets.putAll(t.targets);
    for (String miRNA: t.targets.keySet()) {
      
      Collection<miRNAtarget> ctargets = targets.get(miRNA);
      if (ctargets==null) { // we do not have this miRNA currently
        targets.put(miRNA, t.targets.get(miRNA));
      } else { // Append targets to miRNA
        ctargets.addAll(t.targets.get(miRNA));
      }
      
    }
  }


  /**
   * WARNING, CURRENTLY UNUSED AND EXPERIMENTAL IMPLEMENTATION!
   * USE OTHER METHODS.
   * @param miRNAsymbol
   * @return 
   */
  @SuppressWarnings("unchecked")
  public ObjectAndScore<String>[] findTargets(String miRNAsymbol) {
    if (targets==null) return null;
    String[] miRNAs = targets.keySet().toArray(new String[0]);
    miRNAsymbol = format_miRNA(miRNAsymbol);
    
    /*
     * String distance metric. See
     * http://staffwww.dcs.shef.ac.uk/people/sam.chapman@k-now.co.uk/simmetrics/index.html
     */
//    AbstractStringMetric metric = new Levenshtein();
//    float[] score = metric.batchCompareSet(miRNAs, miRNAsymbol);
    float[] score = new float[miRNAs.length]; // XXX: TEMP because method is unused
    
    // Simple output. TODO: Computationally take best or such...
    ObjectAndScore<String>[] hits = new ObjectAndScore[miRNAs.length];
    for (int i=0; i<miRNAs.length; i++) {
      // XXX: Comment the following 2 lines.
      //Collection<miRNAtarget> col = targets.get(miRNAs[i]);
      //System.out.println(miRNAs[i]+"\t"+score[i]+"\t"+col.iterator().next());
      
      hits[i] = new ObjectAndScore<String>(miRNAs[i], score[i]);
    }
    
    // Sort by score
    Arrays.sort(hits); // Sorts ascending
    ArrayUtils.reverse(hits); // Place the best item on top.
    
    return hits;
  }


  /**
   * As this method returns the internal data structure,
   * be VERY CAREFUL with that.
   * @return 
   */
  public Map<String, Collection<miRNAtarget>> getTargetList() {
    return targets;
  }


  /* (non-Javadoc)
   * @see de.zbit.io.CSVwriteable#fromCSV(java.lang.String[], int, int)
   */
  public void fromCSV(String[] elements, int elementNumber, int CSVversionNumber)
    throws CorruptInputStreamException {
    // Core for CSVversionNumber 0
    
    // Build array for miRNAtarget
    String[] elementsNew = new String[elements.length-2];
    System.arraycopy(elements, 2, elementsNew, 0, elementsNew.length);
    
    // Read version number of miRNAtarget
    int miRNAtargetCSVversionNumber = Integer.parseInt(elements[1]);
    
    // Build miRNAtarget
    miRNAtarget t = new miRNAtarget(0);
    t.fromCSV(elementsNew, 0, miRNAtargetCSVversionNumber);
    
    // Add to list
    addTarget(elements[0], t);
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
    if (targets==null) return null;
    
    // Init output Iterator
    if (currentCSVElement==null) {
      currentCSVElement = targets.keySet().iterator();
    }
    // Terminate when iterator is through
    if (!currentCSVElement.hasNext()) {
      currentCSVElement=null;
      return null;
    }
    
    // Return next element
    StringBuffer ret = new StringBuffer();
    String miRNA = currentCSVElement.next();
    Collection<miRNAtarget> col = targets.get(miRNA);
    int i=0;
    for (miRNAtarget t: col) {
      if (i>0) ret.append("\n");
      ret.append(miRNA + "\t" + t.getCSVOutputVersionNumber() + "\t" + t.toCSV((i++)));
    }
    
    return ret.toString();
  }


  /**
   * Filter targets by pValue
   * @param predictionAlgorithm
   * @param threshold
   * @param removeAllBelow
   */
  public void filterTargets(String predictionAlgorithm, double threshold, boolean removeAllBelow) {
    if (targets==null) return;
    int numTargets=0;
    if (log.isLoggable(Level.FINE)) {
      numTargets = sizeOfTargets();
    }
    // Filter all miRNAs
    Iterator<Map.Entry<String, Collection<miRNAtarget>>> tit = targets.entrySet().iterator();
    while (tit.hasNext()) {
      Map.Entry<String, Collection<miRNAtarget>> e = tit.next();
      Collection<miRNAtarget> targetList = e.getValue();
      
      // Filter list of targets
      Iterator<miRNAtarget> it = targetList.iterator();
      while (it.hasNext()) {
        miRNAtarget t = it.next();
        if (t.getSource()!=null && t.getSource().equals(predictionAlgorithm)) {
          boolean remove = false;
          if (removeAllBelow  && t.getPValue()<threshold) remove=true;
          if (!removeAllBelow && t.getPValue()>threshold) remove=true;
          if (remove) it.remove();
        }
      }
      
      // Remove miRNAs without targets
      if (targetList.size()<1) tit.remove();
    }
    if (log.isLoggable(Level.FINE)) {
      int curTargets=sizeOfTargets();
      log.fine(String.format("Filter results for '%s' all %s %s:", predictionAlgorithm, removeAllBelow?"below":"above", threshold));
      log.fine(String.format("Removed %s targets (old:%s now:%s => %s%%).",
        (numTargets-curTargets), numTargets, curTargets, MathUtils.round(100-(curTargets/(double)numTargets*100.0), 2) ));
    }
  }
  
  /**
   * Removes all predicted targets.
   */
  public void filterTargetsOnlyExperimental() {
    if (targets==null) return;
    // Filter all miRNAs
    Iterator<Map.Entry<String, Collection<miRNAtarget>>> tit = targets.entrySet().iterator();
    while (tit.hasNext()) {
      Map.Entry<String, Collection<miRNAtarget>> e = tit.next();
      Collection<miRNAtarget> targetList = e.getValue();
      
      // Filter list of targets
      Iterator<miRNAtarget> it = targetList.iterator();
      while (it.hasNext()) {
        miRNAtarget t = it.next();
        if (!t.isExperimental()) it.remove();
      }
      
      // Remove miRNAs without targets
      if (targetList.size()<1) tit.remove();
    }
  }


  /**
   * Prints a summary of the number of contained elements.
   */
  public void printSummary() {
    System.out.println("miRNAs:" + size() + " targets:"+ sizeOfTargets() + " unique_targets:"+ sizeOfUniqueTargets() + " unique_experimental_targets:"+ sizeOfUniqueTargets(true));
  }


  /**
   * Removes all targets from a specific source
   * @param sourceNameStartingWith remove all targets whose source-string
   * is starting with this string.
   */
  public void removeTargetsFrom(String sourceNameStartingWith) {
    if (targets==null) return;
    // Filter all miRNAs
    Iterator<Map.Entry<String, Collection<miRNAtarget>>> tit = targets.entrySet().iterator();
    while (tit.hasNext()) {
      Map.Entry<String, Collection<miRNAtarget>> e = tit.next();
      Collection<miRNAtarget> targetList = e.getValue();
      
      // Filter list of targets
      Iterator<miRNAtarget> it = targetList.iterator();
      while (it.hasNext()) {
        miRNAtarget t = it.next();
        if (t.getSource()!=null && t.getSource().startsWith(sourceNameStartingWith)) {
          it.remove();
        }
      }
      
      // Remove miRNAs without targets
      if (targetList.size()<1) tit.remove();
    }
  }


  @Override
  public int compareTo(miRNAtargets o) {
    return toString().compareTo(o.toString());
  }
  
}

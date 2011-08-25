/**
 *
 * @author Clemens Wrzodek
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

import de.zbit.data.ObjectAndScore;
import de.zbit.exception.CorruptInputStreamException;
import de.zbit.io.CSVwriteable;
import de.zbit.util.ArrayUtils;
import de.zbit.util.SortedArrayList;
import de.zbit.util.Utils;

/**
 * A class to hold miRNA and collections of targets.
 * @author Clemens Wrzodek
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
    return targets.get(format_miRNA(miRNA));
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
        (numTargets-curTargets), numTargets, curTargets, Utils.round(100-(curTargets/(double)numTargets*100.0), 2) ));
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

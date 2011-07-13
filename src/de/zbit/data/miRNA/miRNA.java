/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.data.miRNA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;

/**
 * A generic class to hold miRNAs with Signals and Targets.
 * @author Clemens Wrzodek
 */
public class miRNA extends NameAndSignals {
  private static final long serialVersionUID = -6645485135779082585L;
  public static final transient Logger log = Logger.getLogger(miRNA.class.getName());
  
  /**
   * miRNA targets, identified by the NCBI Gene ID (Integer)
   * and a pValue (Float)
   */
  private List<miRNAtarget> targets=null;
  
  /**
   * Probe names are added to {@link #addData(String, Object)} with this key.
   */
  public final static String probeNameKey = "probe_name";
  
  /**
   * @param name - The SystematicName (e.g. "mmu-miR-384-3p")
   */
  public miRNA(String name) {
    super(name);
  }
  
  public miRNA(String name, String probeName) {
    this(name);
    addData(probeNameKey, probeName);
  }

  /**
   * Link miRNAs to their targets.
   * @param targets
   * @param col
   * @return number of miRNAs for which targets could be found.
   */
  public static int link_miRNA_and_targets(miRNAtargets targets, Collection<miRNA> col) {
    int matched=0;
    for (miRNA m: col) {
      Collection<miRNAtarget> targets2 = targets.getTargets(m.getName());
      if (targets2!=null) {
        m.setTargets(targets2);
        matched++;
      } else {
        // Remove all old targets
        m.setTargets(null);
      }
    }
    return matched;
  }
  


  /**
   * Set the targets of this miRNA. Ensures that this is always a
   * sorted list.
   * @param targets
   */
  public void setTargets(Collection<miRNAtarget> targets) {
    if (targets!=null) {
      if (!(targets instanceof List)) {
        // Ensure that we have a list.
        targets = new ArrayList<miRNAtarget>(targets);
      }
      Collections.sort((List<miRNAtarget>)targets);
    }
    this.targets = (List<miRNAtarget>) targets;
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object o) {
    if (o==null) return -1;
    int r = super.compareTo(o); // Compare miRNA name
    if (o instanceof miRNA || miRNA.class.isAssignableFrom(o.getClass())) {
      miRNA ot = (miRNA) o;
      if (r==0) {
        Object probe_name1 = getProbeName();
        Object probe_name2 = ot.getProbeName();
        if (probe_name1==null && probe_name2==null) r = 0;
        else if (probe_name1==null || probe_name2==null) r = -1;
        else r = probe_name1.toString().compareTo(probe_name2.toString());
      }
    /*} else {
      if (r==0) r=-1; // do not accept other superclasses*/
    }
    
    return r;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    Object probeName = getProbeName();
    return super.hashCode() + (probeName!=null?probeName.hashCode():0);
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer r = new StringBuffer();
    
    // Probe name is appended in super.toString()
    if (targets!=null) { // raw = non_unique
      r.append("registered_raw_targets:"+targets.size()+" ");
    }
    
    return super.toString(r.toString());
  }

  /**
   * @return list of targets for this miRNA
   */
  public List<miRNAtarget> getTargets() {
    if (targets==null) return null;
    return Collections.unmodifiableList(targets);
  }

  /**
   * @param geneID
   * @return the {@link miRNAtarget} for the given <code>geneID</code> or null
   * if not found.
   */
  public miRNAtarget getTarget(int geneID) {
    if (targets==null || targets.size()<1) return null;
    miRNAtarget search = new miRNAtarget(geneID);
    int pos = Collections.binarySearch(targets, search, miRNAtarget.compareOnlyTargetGeneID());
    return pos>=0?targets.get(pos):null;
  }
  
  /**
   * @return true if and only if this miRNA has annotated targets.
   */
  public boolean hasTargets() {
    return targets!=null&&targets.size()>0;
  }

  /**
   * @return
   */
  public String getProbeName() {
    Object probeName = getData(probeNameKey);
    return probeName==null?null:probeName.toString();
  }

  /**
   * @return cloned and unique targets, by merging all targets with the same gene id.
   */
  public Collection<miRNAtarget> getUniqueTargets() {
    if (!hasTargets()) return null;
    
    Map<Integer, miRNAtarget> id2target = new HashMap<Integer, miRNAtarget>();
    for (miRNAtarget target: targets) {
      miRNAtarget et = id2target.get(target.getTarget());
      if (et==null) id2target.put(target.getTarget(), target.clone());
      else {
        // Merge sources and make experimental
        et.setExperimental(target.isExperimental() || et.isExperimental());
        // Same algorithms sometimes contain duplicate targets
        if (!et.getSource().contains(target.getSource()))
          et.setSource(et.getSource() + "; " + target.getSource());
      }
    }
    
    return id2target.values();
  }

  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignal#merge(java.util.Collection, de.zbit.data.NameAndSignal, de.zbit.data.Signal.MergeType)
   */
  @Override
  protected <T extends NameAndSignals> void merge(Collection<T> source,
    T target, MergeType m) {
    
    Set<miRNAtarget> unique_targets = new HashSet<miRNAtarget>();
    for (T o :source) {
      miRNA mi = (miRNA)o;
      if (mi.hasTargets()) {
        unique_targets.addAll(mi.getTargets());
      }
    }
    
    setTargets(unique_targets);
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    miRNA nm = new miRNA(name, getProbeName());
    super.cloneAbstractFields(nm, this); // Copies also the probeName
    nm.targets = (List<miRNAtarget>) NameAndSignals.cloneCollection(targets);
    return nm;
  }
  
  @Override
  public int getColumnCount() {
    // hasTargets() check is here wrong, because if the first item of
    // n has no targets, no target column will be used for any!
    return super.getColumnCount() + 1;
  }
  
  @Override
  public String getColumnName(int columnIndex) {
    if (columnIndex<super.getColumnCount()) {
      return super.getColumnName(columnIndex);
    } else {
      return "Targets";
    }
  }
  
  @Override
  public Object getObjectAtColumn(int columnIndex) {
    if (columnIndex<super.getColumnCount()) {
      return super.getObjectAtColumn(columnIndex);
    } else {
      if (hasTargets()) {
        return getUniqueTargets();
      } else {
        return null;
      }
    }
    
  }
  


}

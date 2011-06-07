/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.data.mRNA;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;


/**
 * A generic class to hold mRNA probes with Signals and geneIDs.
 * @author Clemens Wrzodek
 */
public class mRNA extends NameAndSignals {
  private static final long serialVersionUID = -5897878964584853247L;
  
  /**
   * Means gene id has not been set or mRNA has no associated gene id.
   */
  private final static int default_geneID = -1;
  
  
  /**
   * Corresponding NCBI Gene ID (Entrez).
   */
  private int geneID;
  
  /**
   * Initialize a new mRNA with the given name.
   * @param name
   */
  public mRNA(String name) {
    super(name);
    geneID=default_geneID;
  }
  
  /**
   * @param name - the probe name.
   * @param geneID - Corresponding NCBI Gene ID (Entrez).
   */
  public mRNA(String name, int geneID) {
    this(name);
    setGeneID(geneID);
  }
  
  /**
   * Set the corresponding NCBI Gene ID.
   * @param geneID
   */
  public void setGeneID(int geneID) {
    this.geneID = geneID;
  }
  
  /**
   * @return associated NCBI Gene ID.
   */
  public int getGeneID() {
    return geneID;
  }
  
  

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object o) {
    int r = super.compareTo(o); // Compare mRNA name
    if (o instanceof mRNA) {
      mRNA ot = (mRNA) o;
      if (r==0) r = geneID-ot.geneID;
    }
    
    return r;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return super.hashCode() + geneID;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String add = null;
    if (geneID>=0) add="gene_id:"+geneID;
    return super.toString(add);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignal#merge(java.util.Collection, de.zbit.data.NameAndSignal, de.zbit.data.Signal.MergeType)
   */
  @Override
  protected <T extends NameAndSignals> void merge(Collection<T> source, T target, MergeType m) {
    
    // Merge private variables to target (=> GeneID)
    Set<Integer> geneIDs = new HashSet<Integer>();
    for (T o :source) {
      mRNA mi = (mRNA)o;
      geneIDs.add(mi.getGeneID());
    }
    
    // Set gene id, if same or unset if they differ
    ((mRNA)target).geneID = geneIDs.size()==1?geneIDs.iterator().next():default_geneID;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    mRNA nm = new mRNA(name, geneID);
    super.cloneAbstractFields(nm, this);
    return nm;
  }
  
}

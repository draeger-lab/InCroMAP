/**
 * @author Clemens Wrzodek
 */
package de.zbit.data.protein;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import de.zbit.data.GeneID;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;

/**
 * A generic class to hold protein modification
 * expression data with Signals and geneIDs.
 * 
 * @author Clemens Wrzodek
 */
public class ProteinModificationExpression extends NameAndSignals implements GeneID {
  private static final long serialVersionUID = -574351682526309872L;
  public static final transient Logger log = Logger.getLogger(ProteinModificationExpression.class.getName());
  
  /**
   * Key to store the analyte short name as {@link #addData(String, Object)}
   */
  public final static String analyte_short_key = "ANALYTE_SHORT";
  
  /**
   * Key to store the modification name as {@link #addData(String, Object)}
   */
  public final static String modification_key = "MODIFICATION";
  
  /**
   * @param geneName
   */
  private ProteinModificationExpression(String geneName) {
    super(geneName);
  }
  
  /**
   * @param geneName the gene name
   * @param analyteShortName
   * @param modification
   * @param geneID Corresponding NCBI Gene ID (Entrez).
   */
  public ProteinModificationExpression(String geneName, String analyteShortName, String modification, int geneID) {
    this(geneName);
    setAnalyteShortName(analyteShortName);
    setModification(modification);
    setGeneID(geneID);
  }
  
  
  public String getAnalyteShortName() {
    Object s = super.getData(analyte_short_key);
    return s==null?null:s.toString();
  }
  
  public void setAnalyteShortName(String shortName) {
    super.addData(analyte_short_key, shortName);
  }
  
  public String getModification() {
    Object s = super.getData(modification_key);
    return s==null?null:s.toString();
  }
  
  public void setModification(String modName) {
    super.addData(modification_key, modName);
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.data.GeneID#getGeneID()
   */
  @Override
  public int getGeneID() {
    Integer i = (Integer) super.getData(gene_id_key);
    return i==null?default_geneID:i;
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.data.GeneID#setGeneID(int)
   */
  @Override
  public void setGeneID(int geneID) {
    super.addData(gene_id_key, new Integer(geneID));
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignal#merge(java.util.Collection, de.zbit.data.NameAndSignal, de.zbit.data.Signal.MergeType)
   */
  @Override
  protected <T extends NameAndSignals> void merge(Collection<T> source, T target, MergeType m) {
    // Merge private variables to target (=> GeneID)
    Set<Integer> geneIDs = new HashSet<Integer>();
    for (T o :source) {
      GeneID mi = (GeneID)o;
      geneIDs.add(mi.getGeneID());
    }
    
    // Set gene id, if same or unset if they differ
    ((GeneID)target).setGeneID(geneIDs.size()==1?geneIDs.iterator().next():default_geneID);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getUniqueLabel()
   */
  @Override
  public String getUniqueLabel() {
    String a = getAnalyteShortName();
    // If data is gene-centered, symbol should be unique.
    // do NOT use getShortProbeName() here. Others merge signals
    // by this return value!
    if (a!=null) return a;
    
    // Maybe the modification is set. A unique label would then be
    // Protein name + modification name
    if (getModification()!=null) {
      a = String.format("%s_%s", getName(), getModification());
    }
    
    
    return getName();
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#hashCode()
   */
  @Override
  public int hashCode() {
    return super.hashCode() + getUniqueLabel().hashCode();
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(Object o) {
    int r = super.compareTo(o);
    if (r==0 && o instanceof ProteinModificationExpression) {
      r = getUniqueLabel().compareTo(((ProteinModificationExpression)o).getUniqueLabel());
    }
    return r;
  }
  
}

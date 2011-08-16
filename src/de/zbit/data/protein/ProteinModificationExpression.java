/**
 * @author Clemens Wrzodek
 */
package de.zbit.data.protein;

import java.util.Collection;
import java.util.logging.Logger;

import de.zbit.data.GeneID;
import de.zbit.data.NSwithProbes;
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
  
  /*TODO: Required attributes:
   * - Analyte Short Name (Save as probe and override getHeader()).
   * - Modification
   * 
   * - Signals
   * - And anyID 2 GenID identifier
   * 
   * TODO: Try to extend the mRNAReader and
   * simply change createObject or such.
   */
  
  public final static String analyte_short_key = "ANALYTE_SHORT";
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
   * @see de.zbit.data.NameAndSignals#merge(java.util.Collection, de.zbit.data.NameAndSignals, de.zbit.data.Signal.MergeType)
   */
  @Override
  protected <T extends NameAndSignals> void merge(Collection<T> source,
    T target, MergeType m) {
    // TODO Auto-generated method stub
    
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getUniqueLabel()
   */
  @Override
  public String getUniqueLabel() {
    String a = getAnalyteShortName();
    // If data is gene-centered, symbol should be unique.
    if (a!=null) return NSwithProbes.getShortProbeName(a);
    
    return getName();
  }
  
}

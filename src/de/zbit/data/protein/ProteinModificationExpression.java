/**
 * @author Clemens Wrzodek
 */
package de.zbit.data.protein;

import java.util.Collection;
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
  
  /*TODO: Required attributes:
   * - Analyte Short Name (Save as probe and override getHeader()).
   * - Modification
   * 
   * -Signals
   * - And anyID 2 GenID identifier
   * 
   * TODO: Try to extend the mRNAReader and
   * simply change createObject or such.
   */
  /**
   * @param name
   */
  private ProteinModificationExpression(String name) {
    super(name);
    // TODO Auto-generated constructor stub
  }
  
  
  
  /* (non-Javadoc)
   * @see de.zbit.data.GeneID#setGeneID(int)
   */
  @Override
  public void setGeneID(int geneID) {
    // TODO Auto-generated method stub
    
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.GeneID#getGeneID()
   */
  @Override
  public int getGeneID() {
    // TODO Auto-generated method stub
    return 0;
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
    // TODO Auto-generated method stub
    return null;
  }
  
}

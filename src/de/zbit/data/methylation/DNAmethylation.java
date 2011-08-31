/**
 * @author Clemens Wrzodek
 */
package de.zbit.data.methylation;

import java.util.Collection;
import java.util.logging.Logger;

import de.zbit.data.GeneID;
import de.zbit.data.NSwithProbes;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.protein.ProteinModificationExpression;

/**
 * TODO: Eventuell besser von {@link NSwithProbes} erben?
 * @author Clemens Wrzodek
 */
public class DNAmethylation extends NameAndSignals implements GeneID {
  private static final long serialVersionUID = -6002300790004775432L;
  public static final transient Logger log = Logger.getLogger(DNAmethylation.class.getName());

  /**
   * @param name
   */
  public DNAmethylation(String name) {
    super(name);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param geneID
   * @param position
   */
  public DNAmethylation(int geneID, int position) {
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
  protected <T extends NameAndSignals> void merge(Collection<T> source, T target, MergeType m) {
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
  
  // TODO: Overwrite hashcode and compareTo with unique comparisons!
  
}

/**
 * @author Clemens Wrzodek
 */
package de.zbit.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.tree.TreeNode;

/**
 * Customizes for {@link HeterogeneousData}. This is to build a tree structure
 * of {@link NameAndSignals}.
 * @author Clemens Wrzodek
 */
public class HeterogeneousNS extends NSwithProbes {
  private static final long serialVersionUID = 5799877513587363704L;
  
  /**
   * List of various datasets
   *   List of all instances that match to this one inside those datasets
   */
  List<NameAndSignals> childs;
  
  /**
   * Any {@link HeterogeneousNS} with this GeneID is defined to be the
   * (fake) parent node of all others.
   */
  public final static Integer geneIDofRootNode = -3;

  /**
   * @param geneName
   * @param geneID
   */
  public HeterogeneousNS(String geneName, Integer geneID) {
    super(null, geneName, geneID);
    unsetProbeName(); // Does not match the Heterogenious idea.
    childs = null;
  }
  public HeterogeneousNS(String geneName, Integer geneID, List<NameAndSignals> childs) {
    this(geneName, geneID);
    this.childs = childs;
  }
  
  /**
   * Adds data to this heterogeneous NS. This method should be called once
   * for all instances in the same order.
   * <p>Example: first add all mRNA probes that map to this gene,
   * second add all miRNAs that have this gene as target and
   * third, add all Methylation regions in the promoter of this gene.
   * @param nsOfThisGene
   */
  public void addChild(NameAndSignals nsOfThisGene) {
    if (childs==null) childs = new ArrayList<NameAndSignals>();
    childs.add(nsOfThisGene);
    nsOfThisGene.setParent(this);
  }
  
  public void addChilds(Collection<NameAndSignals> nsOfThisGene) {
    if (childs==null) childs = new ArrayList<NameAndSignals>();
    childs.addAll(nsOfThisGene);
    for(NameAndSignals child: nsOfThisGene) {
      child.setParent(this);
    }
  }
  
  

  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getChildrenList()
   */
  @Override
  public List<? extends TreeNode> getChildrenList() {
    return childs;
  }
  
}

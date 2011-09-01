/**
 * @author Clemens Wrzodek
 */
package de.zbit.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import de.zbit.data.miRNA.miRNA;

/**
 * @author Clemens Wrzodek
 *
 */
public class HeterogeniousData extends DefaultTreeModel {
  public static final transient Logger log = Logger.getLogger(HeterogeniousData.class.getName());
  
//  class GeneBasedList <T extends NameAndSignals & GeneID> extends ArrayList<T> {
//    GeneBasedList(final List<T> list) {
//      this.addAll(list);
//      Collections.sort(this, NameAndSignals.getGeneIdComparator());
//      NAme
//    }
//  }
  
  List<List<? extends NameAndSignals>> data;

  /**
   * @param root
   */
  public HeterogeniousData(TreeNode root) {
    super(root);
    // TODO Auto-generated constructor stub
  }
  
  
  public void buildTree() {
    
    /* TODO: Each node must have the same signals and stuff!
     * - Probably best to create one HeterogeneousNS instance for EVERY node...
     */
    TreeNode root = new HeterogeneousNS("Genome", HeterogeneousNS.geneIDofRootNode);
    
    /* TODO: - Get input datasets
     * - From each dataset, get next Gene, look for match in others.
     * 
     * 
     * group_by_identifier() returns map geneID => (mRNAs)
     * TODO: Create custom 
     * 
     */
    
    // Group all lists by geneID
    List<Map<String,?>> maps = new ArrayList<Map<String,?>>(data.size());
    List<String> dataTypeName = new ArrayList<String>(data.size());
    for (List<? extends NameAndSignals> d: data) {
      Class<? extends NameAndSignals> type = NameAndSignals.getType(d);
      if (miRNA.class.isAssignableFrom(type)) {
        maps.add((Map<String,?>)miRNA.groupByTargetAndReturnKeysAsString((Iterable<? extends miRNA>) d));
        dataTypeName.add(PairedNS.getTypeNameFull(type));
      } else if (GeneID.class.isAssignableFrom(type)) {
        maps.add(NameAndSignals.group_by_name(d, false));
        dataTypeName.add(PairedNS.getTypeNameFull(type));
      } else {
        log.warning("Can not group by gene_id: " + type.getSimpleName());
      }
    }
    
    String defaultGeneID = Integer.toString(GeneID.default_geneID);
    for (int i=0; i<maps.size(); i++) {
      for (String geneID: maps.get(i).keySet()) {
        String geneName = "Default"; // TODO: Try to get from mapper, else, take any other.
        HeterogeneousNS gene = new HeterogeneousNS(geneName, Integer.parseInt(geneID));
        
        
        // TODO: Add dummy items if i>0
        HeterogeneousNS type = new HeterogeneousNS(dataTypeName.get(i), -2, (List<NameAndSignals>) maps.get(i).get(geneID));
        gene.addChild(type);
        for (int j=i+1; j<maps.size(); j++) {
          // TODO: convert list to lust of HeterogeneousNS(?)
          type = new HeterogeneousNS(dataTypeName.get(i), -2, (List<NameAndSignals>) maps.get(j).get(geneID));
          gene.addChild(type);
        }
        
        // TODO: Signals 
        
         
        

        
        if (geneID.equals(defaultGeneID)) {
          // TODO: Global create default node and add all
        } else {
          
        }
        /* TODO: 
         * If default geneID
         *   createHeterogenousNS instances
         * 
         * 
         */
      }
    }
                                  
    
    
  }
  
  miRNA
  
}

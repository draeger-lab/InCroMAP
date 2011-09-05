/**
 * @author Clemens Wrzodek
 */
package de.zbit.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.tree.TreeNode;
import javax.swing.treetable.AbstractTreeTableModel;
import javax.swing.treetable.TreeTableModel;

import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.miRNA.miRNA;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.customcomponents.TableResultTableModel;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.parser.Species;
import de.zbit.util.ValuePair;

/**
 * Builds a tree of various, Gene-Based {@link NameAndSignals}.
 * The tree has one root (the genome), which has one child for each gene.
 * Each gene has one child for each data type (e.g., mRNA, miRNA, etc.)
 * and each data type has all of its {@link NameAndSignals} as
 * child (possibly probes).
 * <p>Every {@link NameAndSignals} is re-created as {@link HeterogeneousNS},
 * containing only the given {@link Signal}(s), the name and childs.
 * @author Clemens Wrzodek
 */
public class HeterogeneousData extends AbstractTreeTableModel<HeterogeneousNS> implements TreeTableModel {
  // TODO: Let me implement Collection. (or even list?)
  public static final transient Logger log = Logger.getLogger(HeterogeneousData.class.getName());
  
  /**
   * GeneID for nodes, that represent a specific data type
   * (e.g., mRNA node, etc.)
   */
  private final static int dataTypeNodeGeneID=-2;
  
  /**
   * Data to build a tree of.
   * (List of data types(mRNA), that contain all {@link NameAndSignals} of this type).
   */
  List<List<? extends NameAndSignals>> data;
  
  /**
   * Signals to take for each data type
   * (Matching indices with {@link #data}).
   */
  List<ValuePair<String, SignalType>> signalFromData;
  
  /**
   * {@link MergeType} to specify how to merge each data type individually.
   * (Matching indices with {@link #data}).
   */
  List<MergeType> mergeTypeForData;
  
  /**
   * Total number of nodes.
   */
  int size=0;

  /**
   */
  public HeterogeneousData() {
    super(null);
    data = new ArrayList<List<? extends NameAndSignals>>();
    signalFromData = new ArrayList<ValuePair<String, SignalType>>();
    mergeTypeForData = new ArrayList<Signal.MergeType>();
  }
  
  /**
   * Add a data type that should be considered when creating the tree with {@link #buildTree(Species)}.
   * @param nsList
   * @param signal
   * @param mergeType
   */
  public void addDataType(List<? extends NameAndSignals> nsList, ValuePair<String, SignalType> signal, MergeType mergeType) {
    data.add(nsList);
    signalFromData.add(signal);
    mergeTypeForData.add(mergeType);
  }
  
  /**
   * Builds the tree structure.
   * @param species required for the GeneSymbol mapping
   * @return TreeModel
   */
  public TreeTableModel buildTree(Species species) {
    GeneID2GeneSymbolMapper gsMap = IntegratorUITools.get2GeneSymbolMapping(species);
    
    // Every tree needs exactly one root. This will not be visible later on.
    HeterogeneousNS root = new HeterogeneousNS("Genome", HeterogeneousNS.geneIDofRootNode);
    size=1;
    
    // Group all lists by geneID
    List<Map<String,?>> maps = new ArrayList<Map<String,?>>(data.size());
    List<String> dataTypeName = new ArrayList<String>(data.size());
    for (List<? extends NameAndSignals> d: data) {
      Class<? extends NameAndSignals> type = NameAndSignals.getType(d);
      if (miRNA.class.isAssignableFrom(type)) {
        maps.add((Map<String,?>)miRNA.groupByTargetAndReturnKeysAsString((Iterable<? extends miRNA>) d));
        dataTypeName.add(PairedNS.getTypeNameFull(type));
      } else if (GeneID.class.isAssignableFrom(type)) {
        maps.add(NameAndSignals.group_by_name(d, true, false));
        dataTypeName.add(PairedNS.getTypeNameFull(type));
      } else {
        log.warning("Can not group by gene_id: " + type.getSimpleName());
        // we need to have matching indices => add dummy map
        maps.add(new HashMap<String, NameAndSignals>());
      }
    }
    
    // Add all genes to the root node
    String defaultGeneID = Integer.toString(GeneID.default_geneID);
    for (int i=0; i<maps.size(); i++) {
      Iterator<String> mapIgeneIDs = maps.get(i).keySet().iterator();
      while (mapIgeneIDs.hasNext()) {
        // Get geneID and symbol
        String geneID = mapIgeneIDs.next();
        Integer geneIDint = Integer.parseInt(geneID);
        String geneName=null;
        try {
          geneName = gsMap.map(geneIDint);
        } catch (Exception e) {}
        if (geneName==null) geneName = geneID;
        if (geneName.equals(defaultGeneID)) geneName="Unknown";
          
        // Create gene as child of root
        HeterogeneousNS gene = new HeterogeneousNS(geneName, geneIDint);
        root.addChild(gene);
        size++;
        
        // One node per data type (mRNA, miRNA,...)
        
        // We need to have a consistent number of Signals (add dummy items if i>0)
        for (int l=0; l<i; l++) {
          size+=addDataTypeToGeneNode(gene, dataTypeName.get(l), (List<NameAndSignals>) maps.get(l).get(geneID),
            signalFromData.get(l), mergeTypeForData.get(l));
          maps.get(l).remove(geneID); // Remove after it has been added
        }
        
        // Current node (i)
        size+=addDataTypeToGeneNode(gene, dataTypeName.get(i), (List<NameAndSignals>) maps.get(i).get(geneID),
          signalFromData.get(i), mergeTypeForData.get(i));
        mapIgeneIDs.remove(); // Remove after it has been added
        
        // All other data types that have signals for this gene
        for (int j=i+1; j<maps.size(); j++) {
          size+=addDataTypeToGeneNode(gene, dataTypeName.get(j), (List<NameAndSignals>) maps.get(j).get(geneID),
            signalFromData.get(j), mergeTypeForData.get(j));
          maps.get(j).remove(geneID); // Remove after it has been added
        }
      }
    }
    
    
    setRoot(root);
    return this;
  }


  /**
   * Adds a data type node to a gene Node (called <code>dataTypeName</code>) and
   * calculates a gene-centered {@link Signal} from all items in <code>list</code>.
   * <br/>The signal is specified by <code>signalToGeneCenter</code> and will
   * be merged according to <code>mergeTypeForSignals</code>.
   * <p>Afterwards, a kind of probe is generated and added to the type node for
   * each {@link NameAndSignals} in <code>list</code>, containing just a label
   * and the corresponding, given, signal.
   * 
   * @param gene parent Node.
   * @param dataTypeName e.g., mRNA. Will be the name of the type node (added to the <code>gene</code>).
   * @param list list with probes to add as child to the type node and to calculate the
   * Signal for the type node for.
   * @param signalToGeneCenter Specify the Signal that should be taken from the {@link NameAndSignals}
   * from the <code>list</code>.
   * @param mergeTypeForSignals how to merge all Signals from the <code>list</code>.
   * @return total number of {@link HeterogeneousNS} instances, that have been created.
   */
  private int addDataTypeToGeneNode(HeterogeneousNS gene, String dataTypeName, List<NameAndSignals> list,
    ValuePair<String, SignalType> signalToGeneCenter, MergeType mergeTypeForSignals) {
    
    // Add type-Signal to gene
    Double signal = Double.NaN;
    if (list!=null && list.size()>0) {
      // Gene-center signal
      signal = Signal.merge(list, mergeTypeForSignals, signalToGeneCenter.getA(), signalToGeneCenter.getB()).getSignal().doubleValue();
    }
    gene.addSignal(signal, dataTypeName, signalToGeneCenter.getB());
    
    
    // Add a child with data type and all probes from data type to the gene
    int createdNodes=0;
    if (list!=null && list.size()>0) {
      HeterogeneousNS type = new HeterogeneousNS(dataTypeName, dataTypeNodeGeneID);
      createdNodes++;
      type.addSignal(signal, dataTypeName, signalToGeneCenter.getB());
      gene.addChild(type);
      
      // Create all probes (miRNAs or whatever)
      for (NameAndSignals ns: list) {
        Integer geneID = gene.getGeneID();
        if (ns instanceof GeneID) {
          // e.g., for miRNAs this is the miRNA geneID and not the targets(gene.getGeneID()).
          geneID = ((GeneID) ns).getGeneID();
        }
        HeterogeneousNS probe = new HeterogeneousNS(String.format("%s (%s)", ns.getName(), ns.getUniqueLabel()), geneID);
        createdNodes++;
        probe.addSignal(ns.getSignal(signalToGeneCenter.getB(), signalToGeneCenter.getA()));
        type.addChild(probe);
      }
    }
    return createdNodes;
  }

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
   */
  @Override
  public Object getChild(Object parent, int index) {
    return ((TreeNode)parent).getChildAt(index);
  }

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
   */
  @Override
  public int getChildCount(Object parent) {
    return ((TreeNode)parent).getChildCount();
  }
  
  /* (non-Javadoc)
   * @see javax.swing.treetable.AbstractTreeTableModel#isCellEditable(java.lang.Object, int)
   */
  @Override
  public boolean isCellEditable(Object node, int column) {
    return false;
  }
  
  /* (non-Javadoc)
   * @see javax.swing.treetable.AbstractTreeTableModel#getColumnClass(int)
   */
  @Override
  public Class<?> getColumnClass(int column) {
    if (column==0) return TreeTableModel.class; // Required to display the tree renderer
    
    // Get first non-null object in column
    Object o = null;
    Iterator<HeterogeneousNS> it = iterator();
    while (o==null && it.hasNext()) {
      o = it.next().getObjectAtColumn(column);
    }
    
    Class<?> c = TableResultTableModel.getColumnClass(o);
    if (c==null) return super.getColumnClass(column);
    return c;
  }

  /* (non-Javadoc)
   * @see javax.swing.treetable.TreeTableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return 1+data.size(); //+1 for GeneName
  }

  /* (non-Javadoc)
   * @see javax.swing.treetable.TreeTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    if (column==0) return "Gene";
    else if ((column-1)<data.size()) {
      Class<? extends NameAndSignals> type = NameAndSignals.getType(data.get(column-1));
      return PairedNS.getTypeNameFull(type);
    } else {
      return "";
    }
  }

  /* (non-Javadoc)
   * @see javax.swing.treetable.TreeTableModel#getValueAt(java.lang.Object, int)
   */
  @Override
  public Object getValueAt(Object node, int column) {
    return TableResultTableModel.getValueAt((TableResult)node, column);
  }

  /* (non-Javadoc)
   * @see java.util.AbstractCollection#size()
   */
  @Override
  public int size() {
    return size;
  }
  

  /* (non-Javadoc)
   * @see java.util.AbstractCollection#iterator()
   */
  @Override
  public Iterator<HeterogeneousNS> iterator() {
    // Man koennte einen TreePath bis zum lezten Knoten erzeugen lassen...
    // TODO TEST THIS METHOD
    return new Iterator<HeterogeneousNS>() {
        TreeNode currentNode = (TreeNode) getRoot();

        @Override
        public boolean hasNext() {
          // TODO !currentNode.isLeaf(); is wrong.
          // Make ! equals lastNode() or something...
          return currentNode!=null&&!currentNode.isLeaf();
        }
        
        
        private TreeNode getNextNode(TreeNode node, int nextIndex) {
          // Tiefensuche
          if (node.isLeaf()||nextIndex>=node.getChildCount()) {
            TreeNode parent = node.getParent();
            if (parent==null) return null;
            else {
              int index = parent.getIndex(node);
              return getNextNode(parent, index+1);
            }
          } else {
            return node.getChildAt(nextIndex);
          }
        }

        @Override
        public HeterogeneousNS next() {
          // It is intended that the root is never returned.
          return (HeterogeneousNS) getNextNode(currentNode, 0);
        }

        @Override
        public void remove() {
          System.err.println("REMOVE NOT SUPPORTED!");
        }
    };
  }
  
  
  
}

/**
 * @author Clemens Wrzodek
 */
package de.zbit.integrator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import y.base.DataMap;
import y.base.Node;
import y.base.NodeMap;
import y.view.Graph2D;
import y.view.NodeRealizer;
import y.view.hierarchy.HierarchyManager;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.miRNA.miRNA;
import de.zbit.gui.IntegratorGUITools;
import de.zbit.kegg.TranslatorTools;
import de.zbit.kegg.ext.GraphMLmaps;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.util.ValuePairUncomparable;
import de.zbit.util.ValueTriplet;
import de.zbit.visualization.VisualizeMicroRNAdata;


/**
 * Common tools to map the {@link NameAndSignals} on KEGG Pathways
 * from KEGGtranslator. The data is being mapped on pathway nodes,
 * nodes are being splitted, annotated, maps are updated, etc.
 * 
 * @author Clemens Wrzodek
 */
public class NameAndSignal2PWTools {
 public static final transient Logger log = Logger.getLogger(NameAndSignal2PWTools.class.getName());
  
  /* The following rules apply to all nodes:
   * 1. After data visualization (coloring), the graph may have group
   *   nodes, containing variants of the same gene.
   *   
   *   o The resulting Group node (for references to maps, see GraphMLmapsExtended.java)
   *   + may have signals
   *   + NODE_IS_COPY must be false
   *   + NODE_NUMBER_OF_CHILDREN must be set and >=2
   *   - NODE_NAME_AND_SIGNALS must NOT be set (=null!)
   *   - NODE_BELONGS_TO must NOT be set (=null!)
   *   
   *   o If you somehow map data to / retrieve nodes, and the node has the
   *   NODE_IS_COPY flag set to true, you mught also want to consider the parent!
   * 
   */
  
  /**
   * A graph on which operations are performed.
   */
  protected Graph2D graph;
  
  /**
   * Common tools.
   */
  protected TranslatorTools tools;
  
  
  /**
   * Create a new instance of the {@link NameAndSignal2PWTools}
   * that map data to nodes, split nodes, etc.
   * 
   * @param graph
   */
  public NameAndSignal2PWTools(Graph2D graph){
    super();
    this.graph=graph;
    if (this.graph==null) log.warning("Graph is null!");
    tools = new TranslatorTools(graph);
  }
  
  

  /**
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName to describe the signal
   * @param type to describe the signal
   * @return all {@link Node}s in the current {@link #graph} belonging to the
   * data, described by the three given attributes.
   */
  @SuppressWarnings("unchecked") 
  public Set<Node> getAllNodesForExperiment(String tabName, String experimentName, SignalType type) {
    DataMap isCopyMap = tools.getMap(GraphMLmapsExtended.NODE_IS_COPY);
    DataMap parent = tools.getMap(GraphMLmapsExtended.NODE_BELONGS_TO);
    Set<Node> nodeList = new HashSet<Node>();
    if (parent==null) return nodeList;
    
    ValueTriplet<String, String, SignalType> lookFor = new ValueTriplet<String, String, SignalType>(tabName, experimentName, type);
    for (Node n: graph.getNodeArray()) {
      ValueTriplet<String, String, SignalType> visSignal = (ValueTriplet<String, String, SignalType>) parent.get(n);
      if (visSignal!=null && visSignal.equals(lookFor)) {
        nodeList.add(n);
        // If it is a replicated copy node, also add parent to return set
        if (isCopyMap!=null && isCopyMap.get(n)!=null && isCopyMap.getBool(n)) {
          nodeList.add(graph.getHierarchyManager().getParentNode(n));
        }
      }
    }
    return nodeList;
  }
  

  /**
   * Adds an object with a list of nodes to a map from nodes to a list of objects. 
   * @param <T> object type
   * @param ns object
   * @param nsList list of nodes
   * @param map map from node to list of objects.
   */
  private <T> void addNStoNodeList(T ns, List<Node> nsList, Map<Node,Set<T>> map) {
    DataMap isCopyMap = tools.getMap(GraphMLmapsExtended.NODE_IS_COPY);
    
    // look if parents must also be considered adding to list
    if (isCopyMap!=null) {
      ListIterator<Node> it = nsList.listIterator();
      while (it.hasNext()) {
        Node n = it.next();
        Object isCopy = isCopyMap.get(n);
        if (isCopy!=null && ((Boolean)isCopy)) {
          it.add(graph.getHierarchyManager().getParentNode(n));
        }
      }
    }
    
    for (Node n: nsList) {
      Set<T> tList = map.get(n);
      if (tList==null) {
        tList = new HashSet<T>();
        map.put(n, tList);
      }
      // Add current node to list
      tList.add(ns);
    }
  }
  

  /**
   * Prerequisite: The node MUST be a cloned node, i.e., {@link GraphMLmapsExtended#NODE_IS_COPY}
   * must be <code>TRUE</code> .
   * @param n a cloned node.
   * @return the (old) parent node, if this was a child. (The parent may now not be a parent anymore!)
   */
  public Node removeNode(Node n) {
    // remove node and if it is a cloned node, eventually un-group parent.
    Node parent = null;
    
    Object isCopy = tools.getInfo(n, GraphMLmapsExtended.NODE_IS_COPY);
    if (isCopy!=null && ((Boolean)isCopy) ) {
      parent = graph.getHierarchyManager().getParentNode(n);
    }
    
    // Remove Node
    graph.removeNode(n);
    
    // Adjust number of nodes map of parent
    if (parent!=null) {
      Object children = tools.getInfo(parent, GraphMLmapsExtended.NODE_NUMBER_OF_CHILDREN);
      if (children!=null && (children instanceof Integer)) {
        Integer childs = ((Integer) children)-1;
        tools.setInfo(parent, GraphMLmapsExtended.NODE_NUMBER_OF_CHILDREN, childs);
        if (childs==1) {
          // Group is not required to be a group node now. Merge with only child.
          mergeGroupNodeToFirstChild(parent);
        }
      }
    }
    return parent;
  }
  
  /**
   * Merges a group node with only one child to a single node.
   * @param group a Node that is a group node and has exactly one child.
   */
  private void mergeGroupNodeToFirstChild(Node group) {
    // - Get (first) child of node
    Node child = graph.getHierarchyManager().getChildren(group).node();
    NodeRealizer cr = graph.getRealizer(child);
    NodeRealizer gr = graph.getRealizer(group);
    
    // -- Remove child, UngroupNode and restore original state (e.g. w/h)
    // - Copy Signals from child ? No, because signal must also be in groupNode.
    tools.setInfo(group, GraphMLmapsExtended.NODE_NUMBER_OF_CHILDREN, null);
    tools.setInfo(group, GraphMLmapsExtended.NODE_IS_COPY, null);
    tools.setInfo(group, GraphMLmapsExtended.NODE_BELONGS_TO, tools.getInfo(child, GraphMLmapsExtended.NODE_BELONGS_TO));
    tools.setInfo(group, GraphMLmapsExtended.NODE_NAME_AND_SIGNALS, tools.getInfo(child, GraphMLmapsExtended.NODE_NAME_AND_SIGNALS));
    
    
    // Remove node from all maps
    NodeMap[] maps = graph.getRegisteredNodeMaps();
    for (NodeMap map: maps) {
      Object o = map.get(child);
      if (o!=null) {
        map.set(child, null);
      }
    }
    
    // Remember original x and y, ungroup node.
    cr.setX(gr.getX());
    cr.setY(gr.getY());
    graph.getHierarchyManager().convertToNormalNode(group);
    
    // Remove child and Set realizer of child to parent group node.
    graph.removeNode(child);
    graph.setRealizer(group, cr);
    
    // Reset node look and feel to original one
    tools.resetWidthAndHeight(group);
    tools.resetColorAndLabel(group);
    // TODO: Also reset shape in resetWidthAndHeight()
    
  }
  

  /**
   * Generates a map from Nodes to a list of {@link NameAndSignals} that map to this node.
   * 
   * <p><b>May only be called for exactly the <code>nsList</code> that has already been
   * visualized in the graph</b> (I really mean exactly the same list! No equal list, but the
   * same pointer!).
   * @param <T> any {@link NameAndSignals}
   * @param nsList the list to map on nodes.
   * @return map from Nodes to a list of {@link NameAndSignals} or an empty map. Never null.
   */
  public <T extends NameAndSignals> Map<Node, Set<T>> getAnnotatedNodes(Iterable<T> nsList) {
    Map<Node, Set<T>> map = new HashMap<Node, Set<T>>();
    if (nsList==null || !nsList.iterator().hasNext()) {
      return map;
    }
    
    // First priority: Link directly by annotated corresponding NameAndSignals.
    Map<Object, List<Node>> ns2n_map = tools.getReverseMap(GraphMLmapsExtended.NODE_NAME_AND_SIGNALS);
    if (ns2n_map!=null && ns2n_map.size()>0) {
      Iterator<T> it = nsList.iterator();
      while (it.hasNext()) {
        T ns = it.next();
        List<Node> nsNodes = ns2n_map.get(ns);
        if (nsNodes!=null && nsNodes.size()>0) {
          addNStoNodeList(ns, nsNodes, map);
        }
      }
    }
    return map;
  }
    
    /* Old idea:
     * 
     *  Map by
     *  a) NS map -> break if found, also add parent node if isVariant.
     * 
     *  b) - if miRNA then map by
     *    1) unique name (may come from another table with same probes)
     *       if (isVariant) also addToParent
     *    2) Name
     *     
     *     - if NOT miRNA then map by GENE-ID
     *      Bei mehreren, suche ob ein knoten isVariant=true ist und
     *      uniqueName().equals(ns).
     *      As always, also add to parent.
     * 
     * New decision: only make a).
     *  Ein Knoten darf nur für einen Tab stehen.
     *  Dieser muss entweder überschrieben (re-color) oder gesplitted werden).
     */
        
    // XXX: Is this still required here?

    
    // It seems this is the first time, these NameAnsSignals are processed.
    // 2nd Protority => Map by GeneID and miRNA name.
  /*  Map<Integer, List<Node>> gi2n_map = tools.getGeneID2NodeMap();
    Map<String, List<Node>> mi2n_map = tools.getRNA2NodeMap();
    
    // Get Node(s) for current NameAndSignals
    Iterator<T> it = nsList.iterator();
    while (it.hasNext()) {
      T ns = it.next();
      
      List<Node> node;
      if (ns instanceof miRNA) {
        if (ns.getName()==null) continue;
        Node miNode = VisualizeMicroRNAdata.getMicroRNAnode(mi2n_map, (miRNA) ns, graph);
        if (miNode==null) continue; // Contains no node in the current graph 
        node = Arrays.asList(new Node[]{miNode});
      } else {
        // Get Node(s) for mRNA
        node = getNodesForNameAndSignal(gi2n_map, ns);
      }
    }
    
    
  }*/
  

  /**
   * Get nodes, corresponding to the given <code>ns</code>. Uses the annotated
   * Gene-IDs of {@link NameAndSignals} to identify the target nodes!
   * <p>Does not require any extended map.
   * @param map obtained from {@link TranslatorTools#getGeneID2NodeMap()}
   * @param ns
   * @return a list of all Nodes for a {@link NameAndSignals}.
   */
  private static List<Node> getNodesForNameAndSignal(Map<Integer, List<Node>> map, NameAndSignals ns) {
    List<Node> node = new LinkedList<Node>();
    Collection<Integer> gis = NameAndSignals.getGeneIds(ns);
    for (Integer gi : gis) {
      if (gi!=null && gi.intValue()>0) {
        Collection<Node> nList = map.get(gi);
        if (nList!=null) {
          node.addAll(nList);
        }
      }
    }
    return node;
  }
  

  /**
   * Prepares a graph for later processing with the given dataset. This means,
   * it maps all {@link NameAndSignals} from the list on the graph. For all nodes,
   * it annotated all Keys in {@link GraphMLmapsExtended} (especially the
   * {@link GraphMLmapsExtended#NODE_NAME_AND_SIGNALS} key) to the nodes.
   * <p>If the signals are not gene-centered OR the graph contains already
   * visualized data, the nodes are being either split and a group node is
   * created around all replicates OR if this node is already a splitted node,
   * a further child will be added to the group and annotated with the {@link NameAndSignals}.
   * 
   * <p>MAIN OUTCOME:<br/>After calling this method, you may iterate over
   * all nodes of the graph and process nodes simply according to the
   * {@link GraphMLmapsExtended#NODE_NAME_AND_SIGNALS}. You must not bother with
   * multiple probes that are available for one gene.
   * @param <T>
   * @param nsList list with {@link NameAndSignals} to map to the graph.
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName to describe the signal
   * @param type to describe the signal
   */
  public <T extends NameAndSignals> void prepareGraph(Collection<T> nsList, String tabName, String experimentName, SignalType type, boolean pathwayCentered) {
    // Only if multiple times the same signal is available, which is actually impossible...
    MergeType sigMerge = IntegratorGUITools.getMergeTypeSilent();
    
    // Prepare a key to uniquely describe the visualized data.
    ValueTriplet<String, String, SignalType> key = new ValueTriplet<String, String, SignalType>(tabName, experimentName, type);
    
    // Get GeneID 2 Node map
    Map<Integer, List<Node>> gi2n_map = tools.getGeneID2NodeMap();
    Map<String, List<Node>> mi2n_map = tools.getRNA2NodeMap();
    
    // If pathway-centered, preprocess
    if (pathwayCentered) {
      Map<Node, Set<T>> n2ns = getNodeToNameAndSignalMapping(nsList);
      /* TODO: 
       * 1. Merge all lists accoring to mergeType to get one NS for each node
       * 2. Remember map and change nsList pointer. Use map for "Get Node(s) for current NameAndSignals"
       * 
       */
    }
    
    // Map each NameAndSignal to a list of nodes
    Set<ValuePairUncomparable<Node, T>> alreadyTreated = new HashSet<ValuePairUncomparable<Node, T>>();
    for (T ns : nsList) {
      // Get Node(s) for current NameAndSignals
      List<Node> node;
      if (ns instanceof miRNA) {
        if (ns.getName()==null) continue;
        Node miNode = VisualizeMicroRNAdata.getMicroRNAnode(mi2n_map, (miRNA) ns, graph);
        if (miNode==null) continue; // Contains no node in the current graph 
        node = Arrays.asList(new Node[]{miNode});
      } else {
        // Get Node(s) for mRNA
        node = getNodesForNameAndSignal(gi2n_map, ns);
      }
      
      // Annotate and modify graph for all NS with valid signals.
      if (node!=null && node.size()>0) {
        List<Signal> signals = getSignals(ns);
        Collection<Signal> unique = Signal.merge(signals, sigMerge);
        for (Signal sig: unique) {
          if (sig.getName().equals(experimentName) && sig.getType().equals(type)) {

              for (Node n: node) {
                // ---------------------------------
                // Look if the node already has been colored during this run
                Object nIsCopy = tools.getInfo(n, GraphMLmapsExtended.NODE_IS_COPY);
                //Object nNameAndSignal = tools.getInfo(n, GraphMLmapsExtended.NODE_NAME_AND_SIGNALS);
                // Take "NODE_BELONGS_TO", because raw miRNA have 1)NameAndSignal and 2)isMiRNA
                // annotated after addition, BUT they dont't belong to any visualized signal.
                Object nBelongsTo = tools.getInfo(n, GraphMLmapsExtended.NODE_BELONGS_TO);
                if (nBelongsTo!=null || nIsCopy!=null) {
                  // Node already belongs to any nameAndSignal or even is already a splitted group node
                  Node groupNode = n;
                  if (nIsCopy!=null && ((Boolean)nIsCopy) ) {
                    // If node is a child, get the parent node that represents this gene.
                    groupNode = graph.getHierarchyManager().getParentNode(n);
                  }
                  
                  // Avoid splitting the same node for all already splitted instances.
                  // => Remember splitted node and corresponding ns and only treat once.
                  ValuePairUncomparable<Node, T> id = new ValuePairUncomparable<Node, T>(groupNode,ns);
                  if (alreadyTreated.contains(id)) continue;
                  else alreadyTreated.add(id);
                  
                  // Convert to group node and create node, representing the previous NameAndSignals that mapped to the node
                  if (!graph.getHierarchyManager().isGroupNode(groupNode)) {
                    convertToGroupNode(groupNode);
                  }
                  
                  // Create a node in this group node, that corresponds to the current NameAndSignals
                  // First, create a virgin child node copy.
                  Node copy = graph.getHierarchyManager().getChildren(groupNode).node().createCopy(graph);
                  
                  tools.resetWidthAndHeight(copy);
                  removeAllUnregisteredMaps(copy);
                  prepareChildNode(groupNode, copy, ns);
                  n=copy; // Change color of copy, not of original node.
                } else {
                  // Node is a simple node (not a member of a splitted node)
                  tools.setInfo(n, GraphMLmapsExtended.NODE_IS_COPY, null);
                }
                
                // Simply set keys that node now belongs to input dataset.
                tools.setInfo(n, GraphMLmapsExtended.NODE_BELONGS_TO, key);
                tools.setInfo(n, GraphMLmapsExtended.NODE_NAME_AND_SIGNALS, ns);
                tools.setInfo(n, GraphMLmapsExtended.NODE_NUMBER_OF_CHILDREN, null);
                // ---------------------------------
              }

          }
        }
        
      }
    }
    

    // TODO: Make a nice layout (e.g. by layouting novel group nodes only).
    // And layouting internals of novel group nodes.
//    for (Node newGroupNode: groupNodeRepresentative.keySet()) {
//      
//    }
//    tools.layoutNodeSubset(groupNodeRepresentative.keySet());
    
  }


  /**
   * Get a list of {@link NameAndSignals} for every {@link Node}.
   * @param <T> any {@link NameAndSignals} derived class
   * @param nsList
   * @return Map<Node, Set<T>>
   */
  public <T extends NameAndSignals>  Map<Node, Set<T>> getNodeToNameAndSignalMapping(Collection<T> nsList) {
    Map<Node, Set<T>> n2ns = new HashMap<Node, Set<T>>();
    
    // Group NS by identifier (somewhat gene-centered).
    int desiredIdentifier = NameAndSignals.getIdentifierType(NameAndSignals.getType(nsList));
    Map<Object, List<T>> id2NSmapper = NameAndSignals.getNSIdentifierToNSmap(nsList);
    
    for (Node n: graph.getNodeArray()) {
      // Get all identifiers for the node
      List<Object> identifiers = new ArrayList<Object>();
      
      // Get comma and space separated id list
      Object identifier = null;
      if (desiredIdentifier==1) { //GeneIDs
        identifier = tools.getInfo(n, GraphMLmaps.NODE_GENE_ID);
      } else if (desiredIdentifier==0) { //Names        
        identifier = tools.getInfo(n, GraphMLmaps.NODE_LABEL);
      }
      
      // Split and and to identifiers list
      if (identifier!=null) {
        String[] ids = identifier.toString().split(",|\\s"); // comma or space separated.
        for (String id: ids) {
          if (id==null || id.trim().length()<1) continue;
          try {
            Object toAdd = id;
            if (desiredIdentifier==1) { //GeneIDs 
              toAdd = Integer.parseInt(id);
            }
            identifiers.add(toAdd);
          } catch (NumberFormatException e) {
            log.log(Level.WARNING, "Could not get geneID for node.", e);
          }
        }
      }
      
      // Get all NS for identifiers
      Set<T> nsListForNode = new HashSet<T>();
      for (Object object : identifiers) {
        List<T> list = id2NSmapper.get(object);
        if (list!=null && list.size()>0) {
          nsListForNode.addAll(list);
        }
      }
      n2ns.put(n, nsListForNode);
      
    }
      
    
    return n2ns;
  }



  /**
   * Collect all signals in the given list.
   * Automatically processes {@link EnrichmentObject}s and takes
   * the corresponding genes in enrichment object.
   * @param <T>
   * @param ns
   * @return a list with signals conained in <code>ns</code>
   */
  @SuppressWarnings("rawtypes")
  public static <T extends NameAndSignals> List<Signal> getSignals(T ns) {
    List<Signal> signals = ns.getSignals();
    if (ns instanceof EnrichmentObject) {
      // In case of enrichment objects, get signals of source (mRNAs).
      Collection c = ((EnrichmentObject)ns).getGenesInClass();
      if (c!=null && c.size()>0 && c.iterator().next() instanceof NameAndSignals) {
        signals.clear();
        for (Object nas : c) {
          signals.addAll(((NameAndSignals)nas).getSignals());
        }
      }
    }
    return signals;
  }
  

  /**
   * Converts the given node to a group node and adds a previously cloned instance of it
   * as first child. 
   * @param groupNode the future group node.
   * @return the child node (which corresponds to a node, the input node previously looked like).
   */
  private Node convertToGroupNode(Node groupNode) {
    Node copy = groupNode.createCopy(graph);
    NameAndSignals groupNameAndSignals = (NameAndSignals)tools.getInfo(groupNode, GraphMLmapsExtended.NODE_NAME_AND_SIGNALS);
    Object parent = tools.getInfo(groupNode, GraphMLmapsExtended.NODE_BELONGS_TO);
    
    NodeRealizer nr = KEGG2yGraph.setupGroupNode(graph.getRealizer(groupNode).getLabel(), graph.getLabelText(copy));
    graph.getHierarchyManager().convertToGroupNode(groupNode);
    graph.setRealizer(groupNode, nr);
    tools.setInfo(groupNode, GraphMLmapsExtended.NODE_IS_COPY, false);
    tools.setInfo(groupNode, GraphMLmapsExtended.NODE_NAME_AND_SIGNALS, null); // May belong to multiples => iterate childs.
    tools.setInfo(groupNode, GraphMLmapsExtended.NODE_BELONGS_TO, null); // May belong to multiples => iterate childs.
    tools.setInfo(groupNode, GraphMLmapsExtended.NODE_NUMBER_OF_CHILDREN, 0);
    
    // Paint above other nodes.
    graph.moveToLast(groupNode);
    
    prepareChildNode(groupNode, copy, groupNameAndSignals);
    tools.setInfo(copy, GraphMLmapsExtended.NODE_BELONGS_TO, parent); // Ensure a real replicate of the old node.
    
    return copy;
  }
  
  /**
   * Removes all annotated signals from a node.
   * @param node
   */
  private void removeAllUnregisteredMaps(Node node) {
    // This is actually a dirty workaround for the
    // removeSignals() method in VisualizeDataInPathway().
    Collection<String> allDescriptors = tools.getMapDescriptors();
    if (allDescriptors==null) return;
    
    for (String descriptor : allDescriptors) {
      if (!GraphMLmapsExtendedContainsMap(descriptor)) {
        tools.getMap(descriptor).set(node, null);
      }
    }
  }
  
  /**
   * Check if {@link GraphMLmapsExtended} contains a map with the
   * given descriptor.
   * @param descriptor
   * @return true if and only the the given descriptor corresponds
   * to a registered map.
   */
  public static boolean GraphMLmapsExtendedContainsMap(String descriptor) {
    try {
      for (Field f: GraphMLmapsExtended.class.getFields()) {
        // Get field value (for static fields, object is null) and
        // compare with given descriptor.
        if (f.get(null).equals(descriptor)) {
          return true;
        }
      }
    } catch (Exception e) {}
    return false;
  }
  
  

  /**
   * Prepare a child node to arrange in the parent group node and set all
   * attributes as given by the {@link NameAndSignals}.
   * <p>This method is intended for one node, splitted into multiple other
   * ones (corresponding to various isoforms or probes of the parent node).
   * <p><b>This method does NOT set the {@link GraphMLmapsExtended#NODE_BELONGS_TO}
   * attribute! Please set it manually on the child.</b> All other attributes 
   * are set on the child. The group node gets the number of children increased.
   * @param groupNode
   * @param child
   * @param ns
   */
  private void prepareChildNode(Node groupNode, Node child, NameAndSignals ns) {
    int cols = 2;
    double inset=5.0;
    HierarchyManager hm = graph.getHierarchyManager();
    NodeRealizer cr = graph.getRealizer(child);
    
    // Set a uniquely describing label
    if (ns!=null) {
      String unique = ns.getUniqueLabel();
      if (unique.endsWith("more)")) {
        int pos = unique.lastIndexOf("(");
        if (pos>0) {
          unique = unique.substring(0, pos).trim() + "\n" + unique.substring(pos);
        }
      }
      graph.setLabelText(child, unique);
    }
    
    // Setup hierarchy
    hm.setParentNode(child, groupNode);
    
    // Doesn't work unfortunately!
    //int nodesInGroup = hm.getInnerGraph(groupNode).N();
    
    // Get and increase number of children of group node
    Object children = tools.getInfo(groupNode, GraphMLmapsExtended.NODE_NUMBER_OF_CHILDREN);
    if (children==null || !(children instanceof Integer)) {
      children = 0;
    }    
    Integer nodesInGroup = ((Integer)children)+1;
    tools.setInfo(groupNode, GraphMLmapsExtended.NODE_NUMBER_OF_CHILDREN, nodesInGroup);
    
    /* Parent node must not change x and y or width and height!
     * Child node X and Y determine coordinates of parent group node automatically!
     * => Only set child x and y.
     */
    // TODO: Not for miRNA nodes.
    double w = Math.max(cr.getWidth(), cr.getLabel().getWidth()-inset);
    
    double x = ((nodesInGroup-1) %cols); // column
    x = cr.getX() + (x*(w+inset));
    double y = ((nodesInGroup-1) /cols); // row
    y = cr.getY() + (y*(cr.getHeight()+inset));
    
    cr.setX(x);
    cr.setY(y);
    
    // Make node as big as the label, don't change size of miRNAs.
    if (!tools.getBoolInfo(groupNode, GraphMLmapsExtended.NODE_IS_MIRNA)) {
      cr.setWidth(Math.max(cr.getWidth(), cr.getLabel().getWidth()+2));
      cr.setHeight(Math.max(cr.getHeight(), cr.getLabel().getHeight()+2));
    }
    
    // Clone map contents of group node
    NodeMap[] maps = graph.getRegisteredNodeMaps();
    for (NodeMap map: maps) {
      Object o = map.get(groupNode);
      if (o!=null) {
        map.set(child, o);
      }
    }
    
    // Set new coordinates
    tools.setInfo(child, GraphMLmaps.NODE_POSITION, (int) cr.getX() + "|" + (int) cr.getY());
    tools.setInfo(child, GraphMLmapsExtended.NODE_IS_COPY, true);
    tools.setInfo(child, GraphMLmapsExtended.NODE_NAME_AND_SIGNALS, ns);
    tools.setInfo(child, GraphMLmapsExtended.NODE_NUMBER_OF_CHILDREN, null);
    
    // Paint above other nodes.
    graph.moveToLast(child);
  }

  /**
   * @return current {@link TranslatorTools} instance.
   */
  public TranslatorTools getTranslatorTools() {
    return tools;
  }
  
}

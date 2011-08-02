/**
 * @author Clemens Wrzodek
 */
package de.zbit.integrator;

import java.awt.Color;
import java.io.File;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

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
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorGUITools;
import de.zbit.gui.IntegratorTab;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.NameAndSignalsTab;
import de.zbit.gui.prefs.PathwayVisualizationOptions;
import de.zbit.kegg.Translator;
import de.zbit.kegg.TranslatorTools;
import de.zbit.kegg.ext.GenericDataMap;
import de.zbit.kegg.ext.GraphMLmaps;
import de.zbit.kegg.gui.KGMLSelectAndDownload;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.parser.Species;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.StringUtil;
import de.zbit.util.Utils;
import de.zbit.util.ValuePair;
import de.zbit.util.ValuePairUncomparable;
import de.zbit.util.ValueTriplet;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.utils.SignalColor;
import de.zbit.visualization.VisualizeMicroRNAdata;

/**
 * This class is intended to provide tools for connecting {@link Signal}s
 * to KEGG Pathways in {@link Graph2D}.
 * <p>This includes methods to color nodes according to signals, write signals
 * to node annotations, split nodes to visualize e.g. protein-variants, etc.
 * @author Clemens Wrzodek
 */
public class Signal2PathwayTools {
  public static final transient Logger log = Logger.getLogger(Signal2PathwayTools.class.getName());
  
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
   * SBPreferences object to store all preferences for this class.
   */
  protected SBPreferences prefs = SBPreferences.getPreferencesFor(PathwayVisualizationOptions.class);
  
  /**
   * A graph on which operations are performed.
   */
  protected Graph2D graph;
  
  /**
   * The panel, that contains the {@link #graph}.
   */
  protected TranslatorPanel panelContainingGraph;
  
  /**
   * Common tools.
   */
  protected TranslatorTools tools;
  
  /**
   * Key to store the {@link IntegratorTab#hashCode()} of all tabs
   * that have been visualized in {@link TranslatorPanel#setData(String, Object)}.
   * @see #colorNodesAccordingToSignals(Collection, String, SignalType, Color...)
   */
  protected final static String VISUALIZED_DATA_KEY = "VISUALIZED_DATA";
  
  /**
   * Key to store the {@link IntegratorTab#hashCode()} of all tabs
   * of which the Signals have been annotated to the nodes.
   * @see #writeSignalsToNodes(Iterable) 
   */
  protected final static String ANNOTATED_DATA_KEY = "ANNOTATED_DATA";
  
  
  
  /**
   * Create a new instance of this class to visualize/ perform various
   * {@link Signal} related visualization and annotation operations
   * on the {@link Graph2D} of the given {@link TranslatorPanel}.
   * @param tp
   */
  public Signal2PathwayTools(TranslatorPanel tp){
    this(tp.isGraphML()?(Graph2D) tp.getDocument():null);
    panelContainingGraph = tp;
  }
  
  /**
   * This constructor is private because with a direct graph, we can
   * not track which data has already been visualized.
   * <p>Still, this is needed for a non-graphical (e.g., batch)
   * data processing.
   * @param graph
   */
  private Signal2PathwayTools(Graph2D graph){
    super();
    this.graph=graph;
    panelContainingGraph = null;
    if (this.graph==null) log.warning("Graph is null!");
    tools = new TranslatorTools(graph);
  }
  
  /**
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName to describe the signal
   * @param type to describe the signal
   * @return true if and only if the data described by these three attributes 
   * is currently visualized within the given {@link TranslatorPanel}.
   */
  public boolean isDataVisualized(String tabName, String experimentName, SignalType type) {
    List<ValueTriplet<String, String, SignalType>> visualizedData = getVisualizedData();
    if (visualizedData==null) return false;
    else return visualizedData.contains(new ValueTriplet<String, String, SignalType>(tabName, experimentName, type));
  }

  /**
   * @return list with currently visualized data. Or null if none.
   */
  private List<ValueTriplet<String, String, SignalType>> getVisualizedData() {
    return getVisualizedData(false);
  }
  
  /**
   * @param createIfNotExists if true, a new and empty list will be created if it does not already exist.
   * @return list with currently visualized data. Or null if none.
   */
  @SuppressWarnings("unchecked")
  private List<ValueTriplet<String, String, SignalType>> getVisualizedData(boolean createIfNotExists) {
    List<ValueTriplet<String, String, SignalType>> myList = 
      (List<ValueTriplet<String, String, SignalType>>) panelContainingGraph.getData(VISUALIZED_DATA_KEY);
    if (myList==null && createIfNotExists) {
      myList = new ArrayList<ValueTriplet<String, String, SignalType>>();
      panelContainingGraph.setData(VISUALIZED_DATA_KEY, myList);
    }
    return myList;
  }
  
  /**
   * @return list with currently annotated signals. Or null if none.
   */
  private Map<ValueTriplet<String, String, SignalType>, NodeMap> getAnnotatedSignals() {
    return getAnnotatedSignals(false);
  }
  
  /**
   * @param createIfNotExists if true, a new and empty map will be created if it does not already exist.
   * @return list with currently annotated signals. Or null if none.
   */
  @SuppressWarnings("unchecked")
  private Map<ValueTriplet<String, String, SignalType>, NodeMap> getAnnotatedSignals(boolean createIfNotExists) {
    Map<ValueTriplet<String, String, SignalType>, NodeMap> myMap = 
      (Map<ValueTriplet<String, String, SignalType>, NodeMap>) panelContainingGraph.getData(ANNOTATED_DATA_KEY);
    if (myMap==null && createIfNotExists) {
      myMap = new HashMap<ValueTriplet<String, String, SignalType>, NodeMap>();
      panelContainingGraph.setData(ANNOTATED_DATA_KEY, myMap);
    }
    return myMap;
  }
  
  
  /**
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName to describe the signal
   * @param type to describe the signal
   * @return true if and only if the data described by these three attributes 
   * is currently annotated to the nodes within the given {@link TranslatorPanel}.
   */
  public boolean isSignalAnnotated(String tabName, String experimentName, SignalType type) {
    Map<ValueTriplet<String, String, SignalType>, NodeMap> annotatedSignals = getAnnotatedSignals();
    if (annotatedSignals==null) return false;
    else return annotatedSignals.containsKey(new ValueTriplet<String, String, SignalType>(tabName, experimentName, type));
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
  private Set<Node> getAllNodesForExperiment(String tabName, String experimentName, SignalType type) {
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
   * Remove a visualized dataset from the graph. (Un-visualize dataset).
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName to describe the signal
   * @param type to describe the signal
   */
  public void removeVisualization(String tabName, String experimentName, SignalType type) {
    // 1. Get all nodes for experiment
    Collection<Node> nodes = getAllNodesForExperiment(tabName, experimentName, type);
    if (nodes.size()<1) return;
    DataMap isCopyMap = tools.getMap(GraphMLmapsExtended.NODE_IS_COPY);
    
    // 2. Remove cloned nodes and ungroup parents (if group size = 1)
    if (isCopyMap!=null) {
      for (Node n: nodes) {
        
        // 2.1 If it's a clone, remove it and if parent is empty group, un-group parent.
        Object o = isCopyMap.get(n);
        if (o!=null) {
          if ((Boolean)o) {
            //  is a copy node
            removeNode(n);
            continue;
          } // Group and simple nodes will be treated later.
        }
      }
    }
    
    // 3. Iterate again over nodes and reset visualized data.
    // old group nodes may not be group nodes anymore after removing childs
    nodes = getAllNodesForExperiment(tabName, experimentName, type);
    for (Node n: nodes) {
      //3.2 Reset Color, Label and remove Signal annotation
      if (!graph.getHierarchyManager().isGroupNode(n)) {
        tools.resetColorAndLabel(n);
      } else {
        // TODO: Re-layout content of node.
      }
      tools.setInfo(n, GraphMLmapsExtended.NODE_BELONGS_TO, null);
      tools.setInfo(n, GraphMLmapsExtended.NODE_NAME_AND_SIGNALS, null);
    }
    
    // 4. Remove signal from all nodes and also remove signal map
    removeSignal(tabName, experimentName, type);
    
    // 5. Remove from list of visualized datasets.
    ValueTriplet<String, String, SignalType> dataID = new ValueTriplet<String, String, SignalType>(tabName, experimentName, type);
    List<ValueTriplet<String, String, SignalType>> visualizedData = getVisualizedData();
    if (visualizedData!=null) visualizedData.remove(dataID);
    
  }
  
  /**
   * Prerequisite: The node MUST be a cloned node, i.e., {@link GraphMLmapsExtended#NODE_IS_COPY}
   * must be <code>TRUE</code> .
   * @param n a cloned node.
   * @return the (old) parent node, if this was a child. (The parent may now not be a parent anymore!)
   */
  private Node removeNode(Node n) {
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
   * <p>May only be called for exactly the <code>nsList</code> that has been visualized in the
   * graph (I really mean exactly the same list! No eqal list, but the same pointer!).
   * @param <T> any {@link NameAndSignals}
   * @param nsList the list to map on nodes.
   * @return map from Nodes to a list of {@link NameAndSignals} or an empty map. Never null.
   */
  private <T extends NameAndSignals> Map<Node, Set<T>> getAnnotatedNodes(Iterable<T> nsList) {
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
   * Write all signals into the node annotation list.
   * <p><b>This method only works for visualized datasets, i.e 
   * {@link #prepareGraph(Collection, String, String, SignalType)} must have been called before.
   * @param nsList
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   */
  @SuppressWarnings("unused")
  private <T extends NameAndSignals> void writeSignalsToNodes(Iterable<T> nsList, String tabName) {
    writeSignalsToNodes(nsList, tabName, null, null);
  }
  
  /**
   * Write the signals into the node annotation list.
   * <p><b>This method only works for visualized datasets, i.e 
   * {@link #prepareGraph(Collection, String, String, SignalType)} must have been called before.   * @param nsList
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName
   * @param type
   */
  @SuppressWarnings("unchecked")
  private <T extends NameAndSignals> void writeSignalsToNodes(Iterable<T> nsList, String tabName, String experimentName, SignalType type) {
    Map<Node, Set<T>> node2nsMap = getAnnotatedNodes(nsList);
    // Q:Add a public writeSignalstoNodes method AND a public removeSignalsFrom Nodes method that uses names and GENE IDs.
    // overwrite geneID field of splitted node with ns.GeneId(). && check uniqueLabel() for splitted=true.
    
    // A: Zu viel Aufwand... (wenn label und id equal, aber doch 2 probes, kann nicht unterscheiden. 
    // 2) wie verfahren wenn nur ein teil (nänlich die gene mit nur einer sonde) der NameAndSignals aus den
    // DataMaps sich mappen lässt?
    
    // Remove previous signals
    removeSignal(tabName, experimentName, type);
    
    // Write signals to nodes
    Map<ValueTriplet<String, String, SignalType>, NodeMap> signalMaps = getAnnotatedSignals(true);
    for (Node n : graph.getNodeArray()) {
      Set<T> n_nsList = node2nsMap.get(n);
      
      // Do we have associated signals?
      if (n_nsList!=null && n_nsList.size()>0) {
        
        // Concatenate signals of all NameAndSignals associated with this node.
        Map <ValueTriplet<String, String, SignalType>, StringBuffer> signalIdentifier2text = 
          new HashMap <ValueTriplet<String, String, SignalType>, StringBuffer>();
        
        for (T ns : n_nsList) {
          List<Signal> signals = getSignals(ns);
          for (Signal sig: signals) {
            // Filter signals
            if ((experimentName==null || sig.getName().equals(experimentName)) &&
                (type==null || sig.getType().equals(type))) {
              // Get existing NodeMap for signal
              ValueTriplet<String, String, SignalType> key = new ValueTriplet<String, String, SignalType>(tabName, sig.getName(), sig.getType());
              
              // Write signal to textBuffer
              StringBuffer buff = signalIdentifier2text.get(key);
              if (buff==null) {
                buff = new StringBuffer();
                signalIdentifier2text.put(key, buff);
              }
              if (buff.length()>0) buff.append(StringUtil.newLine());
              buff.append(String.format("%s: %s", ns.getUniqueLabel(), sig.getSignal().toString()));
            }
          }
        }
        
        // Now we have a text for each signal we want to write to the node.
        for (ValueTriplet<String, String, SignalType> key : signalIdentifier2text.keySet()) {
          NodeMap sigMap = signalMaps.get(key);
          if (sigMap==null) {
            // Create new
            sigMap = graph.createNodeMap();
            signalMaps.put(key, sigMap);
          }
          // Set signal annotation
          sigMap.set(n, signalIdentifier2text.get(key).toString());
        }
        //----------
      }
    }
    

    // Register signalMaps in graph
    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(KEGG2yGraph.mapDescription);
    for (Entry<ValueTriplet<String, String, SignalType>, NodeMap> signalSet : signalMaps.entrySet()) {
      String v = getSignalMapIdentifier(signalSet.getKey());
      mapDescriptionMap.set(signalSet.getValue(), v);
    }
  }

  /**
   * Generates an identifier for a SignalTriplet (file/tabname, experimentName, SignalType).
   * @param identifier
   * @return generated key to identify the map.
   */
  private String getSignalMapIdentifier(
    ValueTriplet<String, String, SignalType> identifier) {
    String key = String.format("[%s] %s (from \"%s\")", 
      identifier.getC().toString(), identifier.getB(), identifier.getA());
    return key;
  }
  
  /**
   * Remove annotated signals from the nodes
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName to describe the signal
   * @param type to describe the signal
   */
  private void removeSignal(String tabName, String experimentName, SignalType type) {
    Map<ValueTriplet<String, String, SignalType>, NodeMap> signalMaps = getAnnotatedSignals();
    if (signalMaps==null) return;
    Iterator<Entry<ValueTriplet<String, String, SignalType>, NodeMap>> it = signalMaps.entrySet().iterator();
    while (it.hasNext()) {
      Entry<ValueTriplet<String, String, SignalType>, NodeMap> signalSet = it.next();
      if (signalSet.getKey().getA().equals(tabName)) {
        if ((experimentName==null || signalSet.getKey().getB().equals(experimentName)) &&
            (type==null || signalSet.getKey().getC().equals(type))) {
          // Remove map from all data providers
          tools.removeMap(getSignalMapIdentifier(signalSet.getKey()));
          // Remove from internal listing
          it.remove();
        }
      }
    }
  }
    

  @SuppressWarnings("rawtypes")
  private static <T extends NameAndSignals> List<Signal> getSignals(T ns) {
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
   * Get nodes, corresponding to the given <code>ns</code>. Uses the annotated
   * Gene-IDs of {@link NameAndSignals} to identify the target nodes!
   * <p>Does not require any extended map.
   * @param map obtained from {@link TranslatorTools#getGeneID2NodeMap()}
   * @param ns
   * @return a list of all Nodes for a {@link NameAndSignals}.
   */
  private static List<Node> getNodesForNameAndSignal(Map<Integer, List<Node>> map,
    NameAndSignals ns) {
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
   * Visualize a dataset in the pathway. This includes the following steps:
   * <ol><li>Add NS to nodes and perform splits
   * </li><li>color nodes
   * </li><li>write signals to nodes
   * </li><li>change shape
   * </ol>
   * 
   * @param <T>
   * @param tab
   * @param experimentName
   * @param type
   * @return number of nodes that changed its color.
   */
  public int visualizeData(NameAndSignalsTab tab, String experimentName, SignalType type) {
    return visualizeData(tab.getData(), tab.getName(), experimentName, type, (MergeType)null);
  }
  
  /**
   * Visualize a dataset in the pathway. This includes the following steps:
   * <ol><li>Remove previous visualizations of the same input data.
   * </li><li>Annotate nodes and perform required splits
   * </li><li>color nodes
   * </li><li>write signals to nodes
   * </li><li>change shape
   * </ol>
   * 
   * @param <T>
   * @param nsList
   * @param tabName
   * @param experimentName may not be null.
   * @param type may not be null.
   * @param mt Only if you want to have just one value per node (!=gene centric,
   * because one node can stand for multiple genes!). Set to null to split nodes.
   * @return number of nodes that changed its color.
   */
  public <T extends NameAndSignals> int visualizeData(Collection<T> nsList, String tabName, String experimentName, SignalType type, MergeType mt) {
    // TODO: use "mt" and create a one-value-per-node question dialog and functionality.
    // Do this in a simple pre-processing steps. (getSignals(Node) and Merge).
    
    // 0. Remove previous visualizations of the same data.
    if (isDataVisualized(tabName, experimentName, type)) {
      removeVisualization(tabName, experimentName, type);
      return 0;
    }
    
    // 1. Add NS to nodes and perform splits
    prepareGraph(nsList, tabName, experimentName, type);
    
    // 2. color nodes
    SignalColor recolorer = new SignalColor(nsList, experimentName, type);
    int nodesColored = colorNodesAccordingToSignals(recolorer, tabName, experimentName, type);
    
    // 3. write signals to nodes
    writeSignalsToNodes(nsList, tabName, experimentName, type);
    
    // 4. change shape
    // TODO: Similar to writeSignals
    
    // Remember that we have visualized this data.
    getVisualizedData(true).add(new ValueTriplet<String, String, SignalType>(tabName, experimentName, type));
    
    return nodesColored;
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
  private <T extends NameAndSignals> void prepareGraph(Collection<T> nsList, String tabName, String experimentName, SignalType type) {
    // Only if multiple times the same signal is available, which is actually impossible...
    MergeType sigMerge = IntegratorGUITools.getMergeTypeSilent();
    
    // Prepare a key to uniquely describe the visualized data.
    ValueTriplet<String, String, SignalType> key = new ValueTriplet<String, String, SignalType>(tabName, experimentName, type);
    
    // Get GeneID 2 Node map
    Map<Integer, List<Node>> gi2n_map = tools.getGeneID2NodeMap();
    Map<String, List<Node>> mi2n_map = tools.getRNA2NodeMap();
    
    
//    Map<Node, NameAndSignals> node2ns = new HashMap<Node, NameAndSignals>();
//    Map<Node, Node> groupNodeRepresentative = new HashMap<Node, Node>(); // GroupNode 2 AnyContentNode-toBeCloned
//    Map<Node, Integer> groupNodeChildCount = new HashMap<Node, Integer>(); // GroupNode 2 GroupNodeChildrenCount
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
                  removeSignals(copy);
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
//    for (Node newGroupNode: groupNodeRepresentative.keySet()) {
//      
//    }
//    tools.layoutNodeSubset(groupNodeRepresentative.keySet());
    
  }
  
  /**
   * Removes all annotated signals from a node.
   * @param node
   */
  private void removeSignals(Node node) {
    Map<ValueTriplet<String, String, SignalType>, NodeMap> signalMaps = getAnnotatedSignals();
    if (signalMaps==null) return;
    Iterator<Entry<ValueTriplet<String, String, SignalType>, NodeMap>> it = signalMaps.entrySet().iterator();
    while (it.hasNext()) {
      Entry<ValueTriplet<String, String, SignalType>, NodeMap> signalSet = it.next();
      if (signalSet.getValue().get(node)!=null) {
        signalSet.getValue().set(node, null);
      }
    }
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
   * Color nodes according to signals.
   * 
   * <p><b>This method only works for visualized datasets, i.e 
   * {@link #prepareGraph(Collection, String, String, SignalType)} must have been called before.
   * 
   * @param recolorer {@link SignalColor} instance to determine how to change the graphs color.
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName to describe the signal
   * @param type to describe the signal
   * @return number of nodes, colored according to the signal, or -1 if an error occured.
   */
  @SuppressWarnings("unchecked")
  public int colorNodesAccordingToSignals(SignalColor recolorer, String tabName, String experimentName, SignalType type) {
//    ValueTriplet<String, String, SignalType> signalKey = new ValueTriplet<String, String, SignalType>(tabName, experimentName, type);
    
    MergeType sigMerge = IntegratorGUITools.getMergeTypeSilent();
    
    DataMap nsMapper     = tools.getMap(GraphMLmapsExtended.NODE_NAME_AND_SIGNALS);
    DataMap parentMapper = tools.getMap(GraphMLmapsExtended.NODE_BELONGS_TO);
    Set<Node> nodesToResetColor = new HashSet<Node>(Arrays.asList(graph.getNodeArray()));
    for (Node n: graph.getNodeArray()) {
      // Decide if we want to re-color this node
      ValueTriplet<String, String, SignalType> parent = (ValueTriplet<String, String, SignalType>) parentMapper.get(n);
      if (parent==null) {
        continue; // not belonging to any parent...
      } else if ((tabName==null || parent.getA().equals(tabName)) &&
          (experimentName==null || parent.getB().equals(experimentName)) &&
          (type==null || parent.getC().equals(type))) {
        
        // Get the actual signal to consider when recoloring
        NameAndSignals ns = (NameAndSignals) nsMapper.get(n);
        if (ns==null) continue;
        double signalValue = ns.getSignalMergedValue(type, experimentName, sigMerge);
        if (Double.isNaN(signalValue)) continue;
        Color newColor = recolorer.getColor(signalValue);
        
        // Recolor node and remember to don't gray it out.
        graph.getRealizer(n).setFillColor(newColor);
        nodesToResetColor.remove(n);
        // ---------------------------------
        
      } else if (parent!=null) {
        // Node has been crreated for a specific parent, but it is
        // not this one => don't recolor it, simply skip it.
        nodesToResetColor.remove(n);
      }
    }
    
    // Set unaffected color for all other nodes but reference nodes.
    Color colorForUnaffectedNodes = PathwayVisualizationOptions.COLOR_FOR_NO_VALUE.getValue(prefs);
    if (colorForUnaffectedNodes==null) colorForUnaffectedNodes = Color.LIGHT_GRAY;
    for (Node n: nodesToResetColor) {
      Object kgId = TranslatorTools.getKeggIDs(n);
      // kgId is defined for all KEGG nodes, but NULL for all miRNA nodes.
      if (kgId!=null && kgId.toString().toLowerCase().trim().startsWith("path:")) continue; // Title node
      
      // TODO: If input is miRNA, remove all non-miRNA nodes from colorForUnaffectedNodes and via versa.
      
      graph.getRealizer(n).setFillColor(colorForUnaffectedNodes);
    }
    
    return graph.getNodeArray().length-nodesToResetColor.size();
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
    double w = Math.max(cr.getWidth(), cr.getLabel().getWidth()-inset);
    
    double x = ((nodesInGroup-1) %cols); // column
    x = cr.getX() + (x*(w+inset));
    double y = ((nodesInGroup-1) /cols); // row
    y = cr.getY() + (y*(cr.getHeight()+inset));
    
    cr.setX(x);
    cr.setY(y);
    
    // Make node as big as the label
    cr.setWidth(Math.max(cr.getWidth(), cr.getLabel().getWidth()+2));
    cr.setHeight(Math.max(cr.getHeight(), cr.getLabel().getHeight()+2));
    
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
   * Batch create pathway pictures with visualized signals (nodes
   * colored accordings to signals).
   * @param observations <code>ValuePair<NameAndSignalsTab, ValuePair<String, SignalType>></code>
   * which uniquely describes the observations to take.
   * @param pathwayIDs reference pathway identifiers
   * identifier (organism unspecific).
   * @param outputFormat e.g., "graphml", "jpg", ... the output file extension!
   * @param outputDir
   */
  public static void batchCreatePictures(
    final ValuePair<NameAndSignalsTab, ValuePair<String, SignalType>>[] observations,
    final String[] pathwayIDs, final String outputFormat, final File outputDir) {
    
    SwingWorker<Integer, Void> batchPicCreator = new SwingWorker<Integer, Void>() {
      @Override
      protected Integer doInBackground() throws Exception {

        // Prepare KEGGtranslator
        log.info("Batch creating pathway pictures...");
        KEGG2yGraph translator = (KEGG2yGraph) BatchKEGGtranslator.getTranslator(Format.GraphML, Translator.getManager());
        if (translator == null) {
          GUITools.showErrorMessage(null, "Could not instantiate KEGGtranslator.");
          return 0;
        }
        
        // Color every pathway and signal combination
        Map<String, Graph2D> pathwayCache = new HashMap<String, Graph2D>();
        AbstractProgressBar bar = IntegratorUI.getInstance().getStatusBar().showProgress();
        bar.setNumberOfTotalCalls(pathwayIDs.length*observations.length);
        int success=0;
        for (String pw : pathwayIDs) {
          String pwNumber = Utils.getNumberFromStringRevAsString(pw.length(), pw);
          for (ValuePair<NameAndSignalsTab, ValuePair<String, SignalType>> observation : observations) {
            bar.DisplayBar();
            
            try {
              // Get species and complete kegg pathway id.
              Species species = observation.getA().getSpecies();
              String keggAbbr = "ko";
              if (species!=null && species.getKeggAbbr()!=null) keggAbbr = species.getKeggAbbr();
              
              // Get pathway graph
              String pathwayID = keggAbbr+pwNumber;
              Graph2D graph = pathwayCache.get(pathwayID);
              if (graph==null) {
                String inputFile;
                try {
                  inputFile = KGMLSelectAndDownload.downloadPathway(pathwayID, false);
                  if (inputFile==null) throw new Exception("Failed to download pathway.");
                } catch (Throwable t) {
                  GUITools.showErrorMessage(null, t);
                  // Do not display error again for every observation.
                  bar.setCallNr(bar.getCallNumber()+(observations.length-1));
                  break;
                }
                try {
                  graph = (Graph2D) translator.translate(new File(inputFile));
                } catch (Throwable e) {
                  log.log(Level.SEVERE, "Could not translate graph for pathway " + pathwayID, e);
                  continue;
                }
                if (graph==null) {
                  log.severe("Could not get graph for pathway " + pathwayID);
                  continue;
                }
                pathwayCache.put(pathwayID, graph);
              }
              
              // Color nodes
              String obsExpName = observation.getB().getA();
              SignalType obsExpType = observation.getB().getB();
              Signal2PathwayTools instance = new Signal2PathwayTools(graph);
//              instance.colorNodesAccordingToSignals((Collection<? extends NameAndSignals>)observation.getA().getData(),
//                obsExpName, obsExpType, (Color[]) null);
              instance.visualizeData(observation.getA(), obsExpName, obsExpType);
              
              // Adjust title node
              Node n = TranslatorTools.getTitleNode(graph, pathwayID);
              double oldHeight = 0; String oldText = "";
              if (n!=null) {
                NodeRealizer nr = graph.getRealizer(n);
                oldHeight = nr.getHeight();
                nr.setHeight(oldHeight*2);
                nr.setY(nr.getY()-oldHeight);
                
                oldText = graph.getLabelText(n);
                graph.setLabelText(n, String.format("%s\n%s [%s]", oldText, obsExpName, obsExpType));
              }
              
              // Save graph.
              try {
                String outFile = Utils.ensureSlash(outputDir.getPath()) +
                StringUtil.removeAllNonFileSystemCharacters(
                  pathwayID + '.' + obsExpName + '.' + obsExpType + '.' + outputFormat);
                translator.writeToFile(graph, outFile, outputFormat);
                success++;
              } catch (Throwable e) {
                GUITools.showErrorMessage(null, e);
              }
              
              // Revert modifications for usage with next observation
              if (n!=null) {
                NodeRealizer nr = graph.getRealizer(n);
                nr.setHeight(oldHeight);
                nr.setY(nr.getY()+oldHeight);
                graph.setLabelText(n, oldText);
              }
              instance.removeVisualization(observation.getA().getName(), obsExpName, obsExpType);
            } catch (Throwable t) {
              t.printStackTrace();
              GUITools.showErrorMessage(null, t);
            }
          }
        }
        return success;
      }
      
      @Override
      protected void done() {
        super.done();
        Integer success;
        try {
          success = get();
        } catch (Exception e) {
          success=0;
        }
        String successMessage = String.format("Created %s pathway pictures.", success);
        log.info(successMessage);
        if (success>0) {
          GUITools.showMessage(successMessage, IntegratorUI.appName);
        }
        IntegratorUI.getInstance().getStatusBar().reset();
      }
      
    };
    batchPicCreator.execute();

  }
  
}

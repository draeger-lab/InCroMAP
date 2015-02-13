/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.integrator;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import de.zbit.data.VisualizedData;
import de.zbit.data.compound.Compound;
import de.zbit.data.miRNA.miRNA;
import de.zbit.graph.io.def.GraphMLmaps;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.prefs.PathwayVisualizationOptions;
import de.zbit.util.TranslatorTools;
import de.zbit.util.objectwrapper.ValueTriplet;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.visualization.VisualizeMicroRNAdata;


/**
 * Common tools to map the {@link NameAndSignals} on KEGG Pathways
 * from KEGGtranslator. The data is being mapped on pathway nodes,
 * nodes are being splitted, annotated, maps are updated, etc.
 * 
 * <p><i>Note:<br/>
 * Due to yFiles license requirements, we have to obfuscate this class
 * in the JAR release of this application. Thus, this class
 * can not be found by using the class name.<br/> If you can provide us
 * with a proof of possessing a yFiles license yourself, we can send you
 * an unobfuscated release of Integrator.</i></p>
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
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
   * The Hierarchy Manager of the {@link #graph}.
   */
  protected HierarchyManager hm;
  
  /**
   * Common tools.
   */
  protected TranslatorToolsExtended tools;
  
  
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
    this.hm = graph.getHierarchyManager();
    if (hm == null) {
      hm = new HierarchyManager(graph);
      graph.setHierarchyManager(hm);
    }
    tools = new TranslatorToolsExtended(graph);
  }
  
  

  /**
   * <p><b>NOTE: Only wokrs for mRNA!</b></p>
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
          nodeList.add(hm.getParentNode(n));
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
          it.add(hm.getParentNode(n));
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
    log.finest("--Retrieving parent of "+ n + " from HM and removing node.");
    
    Object isCopy = tools.getInfo(n, GraphMLmapsExtended.NODE_IS_COPY);
    if (isCopy!=null && ((Boolean)isCopy) ) {
      parent = hm.getParentNode(n);
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
          log.finest("  Merging parent group node to first child.");
          // Group is not required to be a group node now. Merge with only child.
          mergeGroupNodeToFirstChild(parent);
        } else {
          log.finest("  Updating parent child count to " + childs);
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
    Node child = hm.getChildren(group).node();
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
    hm.convertToNormalNode(group);
    
    // Remove child and Set realizer of child to parent group node.
    graph.removeNode(child);
    graph.setRealizer(group, cr);
    
    // Reset node look and feel to original one
    // NOPE! With setting realizer to "cr", this is allright!
    // Could still be a visualization from another signal!
//    tools.resetWidthAndHeight(group);
//    tools.resetColorAndLabel(group);
    
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
   * it annotates all Keys in {@link GraphMLmapsExtended} (especially the
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
   * @param pathwayCentered if true, the method calculates one value for each pathway 
   * node and merges multiples {@link NameAndSignals} belonging to one node
   * @return <code>nsList</code> is returned. This list is modified only when
   * <code>pathwayCentered</code> is true. 
   */
  public <T extends NameAndSignals> Collection<T> prepareGraph(Collection<T> nsList, String tabName, String experimentName, SignalType type, boolean pathwayCentered) {
    // Only if multiple times the same signal is available, which is actually impossible...
    MergeType sigMerge = IntegratorUITools.getMergeTypeSilent(type);
    
    // Prepare a key to uniquely describe the visualized data.
    ValueTriplet<String, String, SignalType> key = new ValueTriplet<String, String, SignalType>(tabName, experimentName, type);
    
    // Get GeneID 2 Node map
    Map<Integer, List<Node>> gi2n_map = tools.getGeneID2NodeMap();
    Map<String, List<Node>> ci2n_map = null; // CompoundID2Node mapping
    Map<String, List<Node>> mi2n_map = tools.getRNA2NodeMap();
    Map<String, List<Node>> pw2n_map = tools.getPathwayReferenceNodeMap();
    
    // If pathway-centered, preprocess data
    Map<T, Node> ns2n = null;
    if (pathwayCentered && !EnrichmentObject.class.isAssignableFrom(NameAndSignals.getType(nsList))) {
      // On node may stand for multiple genes => Pathway center them.
      // We can NOT merge EnrichmentObjects...
      
      // 1. Merge all lists according to mergeType to get one NS for each node
      ns2n = new HashMap<T, Node>();
      Map<Node, Set<T>> n2ns_raw = getNodeToNameAndSignalMapping(nsList);
      for (Entry<Node, Set<T>> e : n2ns_raw.entrySet()) {
        ns2n.put(NameAndSignals.merge(e.getValue(), sigMerge), e.getKey());
      }
      // 2. Remember map and change nsList pointer. Use map for "Get Node(s) for current NameAndSignals"
      nsList = ns2n.keySet();
    }
    
    
    // Map each NameAndSignal to a list of nodes
    Set<Node> nodesToLayout = new HashSet<Node>();
    for (T ns : nsList) {
      // Get Node(s) for current NameAndSignals
      List<Node> node;
      if (ns2n!=null) {
        node = Arrays.asList(new Node[]{ns2n.get(ns)});
      } else if (ns instanceof miRNA) {
        if (ns.getName()==null) continue;
        Node miNode = VisualizeMicroRNAdata.getMicroRNAnode(mi2n_map, (miRNA) ns, graph);
        if (miNode==null) continue; // Contains no node in the current graph 
        node = Arrays.asList(new Node[]{miNode});
      } else if (ns instanceof Compound) {
        // We do not always need the 2CompoundID mapping, so we don't pre-init it.
        if (ci2n_map==null) {
          ci2n_map = tools.getInChIKey2NodeMap();
        }
        String cpdID = ((Compound) ns).getID();
        if (cpdID!=null && !cpdID.equals(((Compound) ns).getDefaultID())) {
          node = ci2n_map.get(cpdID);
        } else {
          node = null;
        }
      } else if (ns instanceof EnrichmentObject) {
        Object keggID = ns.getData(EnrichmentObject.idKey);
        if (keggID==null) continue;
        node = pw2n_map.get(keggID.toString().toLowerCase().trim());
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
                // Node is a simple node (not a member of a splitted node)
                tools.setInfo(n, GraphMLmapsExtended.NODE_IS_COPY, null);
                
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
    

    // Make a nice layout (e.g. by layouting novel group nodes only).
    if (nodesToLayout!=null && nodesToLayout.size()>0) {
      tools.layoutChildsAndGroupNodes(nodesToLayout);
    }
    // And layouting internals of novel group nodes.
    // => Is performed ad stacked layout in layoutChildsAndGroupNodes().
    
    return nsList;
  }

  /**
   * Get a list of {@link NameAndSignals} for every {@link Node}.
   * @param <T> any {@link NameAndSignals} derived class
   * @param nsList
   * @return Map<Node, Set<T>>
   */
  public <T extends NameAndSignals> Map<Node, Set<T>> getNodeToNameAndSignalMapping(Collection<T> nsList) {
    Map<Node, Set<T>> n2ns = new HashMap<Node, Set<T>>();
    
    // Group NS by identifier (somewhat gene-centered).
    int desiredIdentifier = NameAndSignals.getIdentifierType(NameAndSignals.getType(nsList));
    Map<Object, List<T>> id2NSmapper = NameAndSignals.getNSIdentifierToNSmap(nsList);
    
    // If we want to map compounds we HAVE TO create our mapping manually first
    if (desiredIdentifier==2 && tools.getMap(GraphMLmapsExtended.NODE_COMPOUND_ID)==null) {
      tools.createNode2InChIKeymapping();
    }
    
    for (Node n: graph.getNodeArray()) {
      // Get all identifiers for the node
      List<Object> identifiers = new ArrayList<Object>();
      
      // Get comma and space separated id list
      Object identifier = null;
      if (desiredIdentifier==1) { //GeneIDs
        identifier = tools.getInfo(n, GraphMLmaps.NODE_GENE_ID);
      } else if (desiredIdentifier==2) { //InChIKeys
        identifier = tools.getInfo(n, GraphMLmapsExtended.NODE_COMPOUND_ID);
      } else if (desiredIdentifier==0) { //Names
        identifier = tools.getInfo(n, GraphMLmaps.NODE_LABEL);
      } else if (desiredIdentifier==3) { //Names        
        identifier = tools.getInfo(n, GraphMLmaps.NODE_KEGG_ID);
        if (identifier!=null) identifier = identifier.toString().toLowerCase().trim();
      }
      
      // Split and and to identifiers list
      if (identifier!=null) {
        String[] ids = identifier.toString().split(",|\\s|\\|"); //comma, space, or  "|" separated
        for (String id: ids) {
          if (id==null || id.trim().length()<1) continue;
          try {
            Object toAdd = id;
            if (desiredIdentifier==1) { //GeneIDs
              toAdd = Integer.parseInt(id);
            } else if (desiredIdentifier==2) {// InChIKey
            	toAdd = id.toUpperCase().trim();
            } else if (desiredIdentifier==3) { //KEGG Ids lowercased and trimmed 
              toAdd = id.toLowerCase().trim();
            }
            identifiers.add(toAdd);
          } catch (NumberFormatException e) {
            log.log(Level.WARNING, "Could not get geneID for node.", e);
          }
        }
      }
      
      // Get all NS for identifiers
      Set<T> nsListForNode = new HashSet<T>();
      for (Object id : identifiers) {
        if (id instanceof String) id=id.toString().toUpperCase().trim();
        List<T> list = id2NSmapper.get(id);
        if (list!=null && list.size()>0) {
          nsListForNode.addAll(list);
        }
      }
      if (nsListForNode.size()>0) {
        n2ns.put(n, nsListForNode);
      }
      
    }
      
    
    return n2ns;
  }

  /**
   * Collect all signals in the given list.
   * Automatically processes {@link EnrichmentObject}s and takes
   * the corresponding genes in enrichment object.
   * @param <T>
   * @param ns
   * @return a list with signals contained in <code>ns</code>
   */
  public static <T extends NameAndSignals> List<Signal> getSignals(T ns) {
    List<Signal> signals = new ArrayList<Signal>(ns.getSignals()); // CLONE the list!
    Collections.sort(signals);
//    if (ns instanceof EnrichmentObject) {
//      // In case of enrichment objects, get signals of source (mRNAs) - in addition.
//      Collection c = ((EnrichmentObject)ns).getGenesInClass();
//      if (c!=null && c.size()>0 && c.iterator().next() instanceof NameAndSignals) {
////        signals.clear();
//        for (Object nas : c) {
//          signals.addAll(((NameAndSignals)nas).getSignals());
//        }
//      }
//    }
    return signals;
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
   * Puts all {@link NameAndSignals} matching a node into
   * the node annotation with Key <code>vd</code>.
   * You can retrieve them later with {@link GraphMLmapsExtended#NODE_VISUALIZED_RAW_NS}.
   * @param <T>
   * @param nsList
   * @param vd
   */
  public <T extends NameAndSignals> void writeRawNStoNodeAnnotation(Collection<T> nsList, VisualizedData vd) {
    DataMap rawNsMap = tools.getMap(GraphMLmapsExtended.NODE_VISUALIZED_RAW_NS);
    if (rawNsMap==null ) {
      rawNsMap = tools.createMap(GraphMLmapsExtended.NODE_VISUALIZED_RAW_NS, true);
    }
    
    Map<Node, Set<T>> node2Ns = getNodeToNameAndSignalMapping(nsList);
    for (Node node: node2Ns.keySet()) {
      Map<VisualizedData, Collection<?>> rawNs = (Map<VisualizedData, Collection<?>>) rawNsMap.get(node);
      if (rawNs==null) {
        rawNs = new HashMap<VisualizedData, Collection<?>>();
        rawNsMap.set(node, rawNs);
      }
      rawNs.put(vd, node2Ns.get(node));
    }

  }



  /**
   * @return current {@link TranslatorTools} instance.
   */
  public TranslatorTools getTranslatorTools() {
    return tools;
  }



  /**
   * @param tools
   * @return <code>TRUE</code> if graph contains any miRNA node
   * that has annotated expression data and is not differentially
   * expressed.
   */
  public static boolean containsNotDifferentiallyExpressedMiRNA(TranslatorTools tools) {
    return containsNotDifferentiallyExpressedMiRNA(tools, false);
  }
  /**
   * @see #containsNotDifferentiallyExpressedMiRNA(TranslatorTools)
   * @param tools
   * @param remove if true, removes all those nodes.
   * @return
   */
  public static boolean containsNotDifferentiallyExpressedMiRNA(TranslatorTools tools, boolean remove) {
    Graph2D graph = tools.getGraph();
    
      // Get Type map
      NodeMap typeMap = (NodeMap) tools.getMap(GraphMLmaps.NODE_TYPE);
      DataMap rawNsMap = tools.getMap(GraphMLmapsExtended.NODE_VISUALIZED_RAW_NS);
      if (typeMap==null || rawNsMap==null) {
        return false;
      }
      
      // Get color for no fc
      SBPreferences prefs = SBPreferences.getPreferencesFor(PathwayVisualizationOptions.class);
      Color forNothing = PathwayVisualizationOptions.COLOR_FOR_NO_FOLD_CHANGE.getValue(prefs);
      
      // Check all nodes
      Set<Node> toRemove = new HashSet<Node>();
      for (Node n : tools.getGraph().getNodeArray()) {
        Object type = typeMap.get(n);
        if (type!=null && type.equals(TranslatorTools.RNA_TYPE)) {
          // Has any signal?
          Map<?,?> rawNS = (Map<?, ?>) rawNsMap.get(n);
          if (rawNS!=null && rawNS.size()>0) {
            // Has noFoldChange color
            if (graph.getRealizer(n).getFillColor().equals(forNothing)) {
              if (remove) {
                toRemove.add(n);
              } else {
                return true;
              }
            }
          }
        }
      }
      
      // Maybe Remove given nodes
      synchronized (graph) {
        if (remove) {
          for (Node n: toRemove) {
            graph.removeNode(n);
          }
          return (toRemove.size()>0);
        }
      }
      
      return false;
  }
  
}

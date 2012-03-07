/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/Integrator> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.visualization;

import java.awt.Color;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import y.base.DataMap;
import y.base.Node;
import y.base.NodeCursor;
import y.base.NodeMap;
import y.view.Graph2D;
import y.view.NodeLabel;
import y.view.NodeRealizer;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.PairedNS;
import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.VisualizedData;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.protein.ProteinModificationExpression;
import de.zbit.graph.LineNodeRealizer;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.actions.TranslatorTabActions;
import de.zbit.gui.actions.listeners.KEGGPathwayActionListener;
import de.zbit.gui.prefs.PathwayVisualizationOptions;
import de.zbit.gui.prefs.SignalOptions;
import de.zbit.gui.tabs.IntegratorTab;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.integrator.GraphMLmapsExtended;
import de.zbit.integrator.NameAndSignal2PWTools;
import de.zbit.kegg.Translator;
import de.zbit.kegg.gui.KGMLSelectAndDownload;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.math.rescale.AbstractRescale;
import de.zbit.math.rescale.LinearRescale;
import de.zbit.math.rescale.LogarithmicRescale;
import de.zbit.parser.Species;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.ArrayUtils;
import de.zbit.util.FileTools;
import de.zbit.util.StringUtil;
import de.zbit.util.TranslatorTools;
import de.zbit.util.Utils;
import de.zbit.util.ValuePair;
import de.zbit.util.ValueTriplet;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.utils.SignalColor;

/**
 * This class is intended to provide tools for connecting and
 * visualizing {@link Signal}s to KEGG Pathways in {@link Graph2D}.
 * <p>This includes methods to color nodes according to signals, write signals
 * to node annotations, split nodes to visualize e.g. protein-variants, etc.
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
public class VisualizeDataInPathway {
  public static final transient Logger log = Logger.getLogger(VisualizeDataInPathway.class.getName());
  
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
  protected TranslatorPanel<Graph2D> panelContainingGraph;
  
  /**
   * Common tools.
   */
  protected TranslatorTools tools;

  /**
   * Common tools to map the {@link NameAndSignals} on nodes,
   * split nodes, etc.
   */
  protected NameAndSignal2PWTools nsTools;

  /**
   * If false, no SWING Dialog messages will be issued
   */
  protected boolean isInteractive;
  
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
   * @param tp any {@link TranslatorPanel}
   */
  public VisualizeDataInPathway(TranslatorPanel<Graph2D> tp){
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
  private VisualizeDataInPathway(Graph2D graph){
    this(graph, true);
  }
  
  /**
   * @param graph
   * @param isInteractive if false, no SWING Dialog messages will be issued
   */
  private VisualizeDataInPathway(Graph2D graph, boolean isInteractive) {
    super();
    this.graph=graph;
    this.isInteractive = isInteractive;
    panelContainingGraph = null;
    if (this.graph==null) log.warning("Graph is null!");
    this.nsTools = new NameAndSignal2PWTools(graph);
    tools = nsTools.getTranslatorTools();
  }

  /**
   * @param visData class, containing various properties to
   * re-identify visualized data
   * @return true if and only if the data described by these three attributes 
   * is currently visualized within the given {@link TranslatorPanel}.
   */
  public boolean isDataVisualized(VisualizedData visData) {
    List<VisualizedData> visualizedData = getVisualizedData();
    if (visualizedData==null) return false;
    else return visualizedData.contains(visData);
  }

  /**
   * @return list with currently visualized data. Or null if none.
   */
  private List<VisualizedData> getVisualizedData() {
    return getVisualizedData(false);
  }
  
  /**
   * Get information which data types are currently visualized in the graph
   * @return boolean array of size 4:
   * <ol><li>mRNA (or unknown)</li>
   * <li>miRNA</li>
   * <li>ProteinModificationExpression</li>
   * <li>DNA methylation</li></ol>
   */
  public boolean[] getVisualizedDataTypes() {
    // Init array with default values
    boolean[] visualizedDataTypes = new boolean[5];
    Arrays.fill(visualizedDataTypes, Boolean.FALSE);
    
    // Get list of visualized data
    List<VisualizedData> visData = getVisualizedData(false);
    if (visData==null || visData.size()<1) return visualizedDataTypes;
    
    for (VisualizedData visualizedData : visData) {
      Class<?> t = visualizedData.getNsType();
      if (t==null) continue;
      else if (mRNA.class.isAssignableFrom(t)) {
        visualizedDataTypes[0]=true;
      } else if (miRNA.class.isAssignableFrom(t)) {
          visualizedDataTypes[1]=true;
      } else if (ProteinModificationExpression.class.isAssignableFrom(t)) {
        visualizedDataTypes[2]=true;
      } else if (DNAmethylation.class.isAssignableFrom(t)) {
        visualizedDataTypes[3]=true;
      } else if (EnrichmentObject.class.isAssignableFrom(t)) {
        visualizedDataTypes[4]=true;
      } else {
        // EVERYTHING ELSE IS ALSO VISUALIZED AS NODE COLOR
        visualizedDataTypes[0]=true;
      }
    }
    
    return visualizedDataTypes;
  }
  
  /**
   * Test if a certain data type is already visualized in the pathway.
   * @param tab
   * @return true if and only if already any dataset of the same
   * type as in the given tab is currently visualized in the pathway.
   */
  public boolean isDataTypeVisualized(NameAndSignalsTab tab) {
    Class<? extends NameAndSignals> dt = NameAndSignals.getType(tab.getData());
    return isDataTypeVisualized(dt);
  }
  
  /**
   * Test if a certain data type is already visualized in the pathway.
   * @param <T>
   * @param dt e.g.,  or miRNA, etc.
   * @return true if and only if already any dataset of this type is
   * currently visualized in the pathway.
   */
  public <T extends NameAndSignals> boolean isDataTypeVisualized(Class<T> dt) {
    boolean[] visualizedDataTypes = getVisualizedDataTypes();
    if (dt==null || visualizedDataTypes==null) return false;
    
    if (mRNA.class.isAssignableFrom(dt)) {
      return visualizedDataTypes[0];
    } else if (miRNA.class.isAssignableFrom(dt)) {
      return visualizedDataTypes[1];
    } else if (ProteinModificationExpression.class.isAssignableFrom(dt)) {
      return visualizedDataTypes[2];
    } else if (DNAmethylation.class.isAssignableFrom(dt)) {
      return visualizedDataTypes[3];
    } else if (EnrichmentObject.class.isAssignableFrom(dt)) {
      return visualizedDataTypes[4];
    } else {
      // In doubt, return false.
      //return false;
      // EVERYTHING ELSE IS ALSO VISUALIZED AS NODE COLOR
      return visualizedDataTypes[0];
    }
    
  }
  
  /**
   * @param createIfNotExists if true, a new and empty list will be created if it does not already exist.
   * @return list with currently visualized data. Or null if none.
   */
  @SuppressWarnings("unchecked")
  private List<VisualizedData> getVisualizedData(boolean createIfNotExists) {
    if (panelContainingGraph==null) return createIfNotExists?new ArrayList<VisualizedData>():null;
    List<VisualizedData> myList = 
      (List<VisualizedData>) panelContainingGraph.getData(VISUALIZED_DATA_KEY);
    if (myList==null && createIfNotExists) {
      myList = new ArrayList<VisualizedData>();
      panelContainingGraph.setData(VISUALIZED_DATA_KEY, myList);
    }
    return myList;
  }
  
  /**
   * @return list with currently annotated signals. Or null if none.
   */
  private Map<VisualizedData, NodeMap> getAnnotatedSignals() {
    return getAnnotatedSignals(false);
  }
  
  /**
   * @param createIfNotExists if true, a new and empty map will be created if it does not already exist.
   * @return list with currently annotated signals. Or null if none.
   */
  @SuppressWarnings("unchecked")
  private Map<VisualizedData, NodeMap> getAnnotatedSignals(boolean createIfNotExists) {
    if (panelContainingGraph==null) return createIfNotExists?new HashMap<VisualizedData, NodeMap>():null;
    Map<VisualizedData, NodeMap> myMap = 
      (Map<VisualizedData, NodeMap>) panelContainingGraph.getData(ANNOTATED_DATA_KEY);
    if (myMap==null && createIfNotExists) {
      myMap = new HashMap<VisualizedData, NodeMap>();
      panelContainingGraph.setData(ANNOTATED_DATA_KEY, myMap);
    }
    return myMap;
  }
  
  
  /**
   * @param visData class, containing various properties to
   * re-identify visualized data
   * @return true if and only if the data described by these three attributes 
   * is currently annotated to the nodes within the given {@link TranslatorPanel}.
   */
  public boolean isSignalAnnotated(VisualizedData visData) {
    Map<VisualizedData, NodeMap> annotatedSignals = getAnnotatedSignals();
    if (annotatedSignals==null) return false;
    else return annotatedSignals.containsKey(visData);
  }
  
  /**
   * Removes all visualizations for a certain data type.
   * @param toRemove may be {@link mRNA}, {@link miRNA},
   * {@link ProteinModificationExpression}, DNA methylation or
   * any other {@link NameAndSignals} derived datatype.
   * @return true if at least one visualized dataset has been
   * removed from the current visualization.
   */
  public <T extends NameAndSignals> boolean removeVisualization(Class<T> toRemove) {
    List<VisualizedData> visData = getVisualizedData(false);
    if (visData==null || visData.size()<1) return false;
    
    // We have to put them in a separate list to avoid ConcurrentModificationException s.
    List<VisualizedData> dataToRemove = new ArrayList<VisualizedData>();
    
    // Collect everthing to remove
    for (VisualizedData visualizedData : visData) {
      Class<?> vdc = visualizedData.getNsType();
      if (vdc==null) continue;
      if (toRemove.isAssignableFrom(vdc)) {
        dataToRemove.add(visualizedData);
      }
    }
    
    // Now remove it
    for (VisualizedData visualizedData : dataToRemove) {
      if (ProteinModificationExpression.class.isAssignableFrom(toRemove)
          || DNAmethylation.class.isAssignableFrom(toRemove)) {
        removeVisualizedLabels(visualizedData.getTabName().toString(), 
          visualizedData.getExperimentName(), visualizedData.getSigType());          
      } else {
        removeVisualization(visualizedData.getTabName().toString(), 
          visualizedData.getExperimentName(), visualizedData.getSigType());
      }
    }
    
    return dataToRemove.size()>0;
  }
  
  /**
   * Remove data that has been visualized as additional node label. I.e.
   * {@link ProteinModificationExpression} or DNA methylation data.
   * <p>See {@link #removeVisualization(String, String, SignalType)}
   * for, e.g, {@link mRNA} or {@link miRNA} data.
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName to describe the signal
   * @param type to describe the signal
   * @see #removeVisualization(String, String, SignalType)
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void removeVisualizedLabels(String tabName, String experimentName, SignalType type) {
    log.finer("Removing node labels for a given visualization.");
    
    // Inspect all labels of all nodes
    int removedCounter=0;
    DataMap rawNsMap = tools.getMap(GraphMLmapsExtended.NODE_VISUALIZED_RAW_NS);
    for (NodeCursor nc = graph.nodes(); nc.ok(); nc.next()) {
      Node n = nc.node();  
      NodeRealizer nr = graph.getRealizer(n);
      
      double shiftSideLabelsBy = 0;
      for (int i=0; i<nr.labelCount(); i++) {
        NodeLabel label = nr.getLabel(i);
        if (shiftSideLabelsBy>0 && label.getModel()==NodeLabel.SIDES) {
          // If we remove those stacking protein boxes, we might need to shift exising ones
          label.setDistance(Math.max(label.getDistance() - shiftSideLabelsBy, 0));
        }
        Object userData = label.getUserData();
        if (userData==null) continue;
        
        // Try to get the information, what has been visualized here
        VisualizedData vd = null;
        if (userData instanceof VisualizedData) {
          vd = (VisualizedData) userData;
        } else if (userData instanceof ValuePair) {
          // Try to get it out of a value pair
          if (((ValuePair)userData).getA() instanceof VisualizedData) {
            vd = (VisualizedData) ((ValuePair)userData).getA();
          } else if (((ValuePair)userData).getB() instanceof VisualizedData) {
            vd = (VisualizedData) ((ValuePair)userData).getB();
          }
        }
        
        
        // Look if we should remove this
        if (vd!=null && vd.matches(tabName, experimentName, type)) {
          if (label.getModel()==NodeLabel.SIDES) {
            shiftSideLabelsBy+=label.getHeight();
          }
          // Remove raw NS from nodes
          if (rawNsMap!=null) {
              Map<VisualizedData, Collection<?>> rawNs = (Map<VisualizedData, Collection<?>>) rawNsMap.get(n);
              if (rawNs!=null) rawNs.remove(vd);
          }
          nr.removeLabel(label); // <- main
          
          i--;
          removedCounter++;
        }
      }
      
    }
    log.finer("Removed " + removedCounter + " node labels.");
    
    // Remove signal from all nodes and also remove signal map
    log.finer("Removing signals and annotation keys.");
    removeSignal(tabName, experimentName, type);
    
    // Remove from list of visualized datasets.
    VisualizedData dataID = new VisualizedData(tabName, experimentName, type);
    List<VisualizedData> visualizedData = getVisualizedData();
    if (visualizedData!=null) visualizedData.remove(dataID);
  }
  
  /**
   * Remove a visualized dataset from the graph. (Un-visualize dataset).
   * <p>Only for data, visualized as background in main-node (mRNA).
   * Not for additional node labels!
   * <p>See {@link #removeVisualizedLabels(String, String, SignalType)}
   * for, e.g., {@link ProteinModificationExpression} or
   * {@link DNAmethylation} data.
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName to describe the signal
   * @param type to describe the signal
   * @see #removeVisualizedLabels(String, String, SignalType)
   */
  @SuppressWarnings("unchecked")
  private void removeVisualization(String tabName, String experimentName, SignalType type) {
    // 1. Get all nodes for experiment
    log.finer("Getting all nodes for given experiment and NODE_IS_COPY map.");
    Collection<Node> nodes = nsTools.getAllNodesForExperiment(tabName, experimentName, type);
    if (nodes.size()<1) return;
    DataMap isCopyMap = tools.getMap(GraphMLmapsExtended.NODE_IS_COPY);
    DataMap rawNsMap = tools.getMap(GraphMLmapsExtended.NODE_VISUALIZED_RAW_NS);
    VisualizedData vd = new VisualizedData(tabName, experimentName, type);
    
    // 2. Remove cloned nodes and ungroup parents (if group size = 1)
    // ONLY IF NOT PATHWAY_CENTRIC!
    log.finer("Removing cloned nodes and ungrouping parent nodes.");
    Set<Node> toLayout = new HashSet<Node>();
    if (isCopyMap!=null) {
      for (Node n: nodes) {
        if (n==null)continue;
        
        // 2.0 remember groups to re-layout
        try {
          Node parent = graph.getHierarchyManager().getParentNode(n);
          if (parent!=null) toLayout.add(parent);
        } catch (Throwable t) {}
        
        // 2.1 If it's a clone, remove it and if parent is empty group, un-group parent.
        Object o = isCopyMap.get(n);
        if (o!=null) {
          if ((Boolean)o) {
            //  is a copy node
            nsTools.removeNode(n);
          } // Group and simple nodes will be treated later.
        }
      }
    }
    
    // 3. Iterate again over nodes and reset visualized data.
    // old group nodes may not be group nodes anymore after removing childs
    log.finer("Re-getting all nodes for given experiment and resetting visualization");
    nodes = nsTools.getAllNodesForExperiment(tabName, experimentName, type);
    for (Node n: nodes) {
      //3.2 Reset Color, Label and remove Signal annotation
      //if (!graph.getHierarchyManager().isGroupNode(n)) {
      if (!graph.getHierarchyManager().isGroupNode(n)) {
        tools.resetColorAndLabel(n);
        /* Next line is disabled, because pathway-based is default
         * and it is annoying to have the node size resetted all
         * the time. */
        //tools.resetWidthAndHeight(n);
        // TODO: If shape-changing is introduced, reset shape here.
      } else {
        toLayout.add(n);
      }
      tools.setInfo(n, GraphMLmapsExtended.NODE_BELONGS_TO, null);
      tools.setInfo(n, GraphMLmapsExtended.NODE_NAME_AND_SIGNALS, null);
      
      // Remove raw NS from nodes
      if (rawNsMap!=null) {
        Map<VisualizedData, Collection<?>> rawNs = (Map<VisualizedData, Collection<?>>) rawNsMap.get(n);
        if (rawNs!=null) rawNs.remove(vd);
      }
    }
    
    // 3.5 layout group node contents. Does not move the group node,
    // but only stacks the inner nodes.
    for (Node node : toLayout) {
      if (graph.contains(node)) {
        tools.layoutGroupNode(node);
      }
    }
    
    // 4. Remove signal from all nodes and also remove signal map
    log.finer("Removing signals and annotation keys.");
    removeSignal(tabName, experimentName, type);
    
    // 5. Remove from list of visualized datasets.
    VisualizedData dataID = new VisualizedData(tabName, experimentName, type);
    List<VisualizedData> visualizedData = getVisualizedData();
    if (visualizedData!=null) visualizedData.remove(dataID);
    
  }
  
  
  /**
   * Write all signals into the node annotation list.
   * @param nsList
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   */
  @SuppressWarnings("unused")
  private <T extends NameAndSignals> void writeSignalsToNodes(Collection<T> nsList, String tabName) {
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
  private <T extends NameAndSignals> void writeAnnotatedSignalsToNodes(Iterable<T> nsList, String tabName, String experimentName, SignalType type) {
    Map<Node, Set<T>> node2nsMap = nsTools.getAnnotatedNodes(nsList);
    // Q:Add a public writeSignalstoNodes method AND a public removeSignalsFrom Nodes method that uses names and GENE IDs.
    // overwrite geneID field of splitted node with ns.GeneId(). && check uniqueLabel() for splitted=true.
    
    // A: Zu viel Aufwand... (wenn label und id equal, aber doch 2 probes, kann nicht unterscheiden. 
    // 2) wie verfahren wenn nur ein teil (nänlich die gene mit nur einer sonde) der NameAndSignals aus den
    // DataMaps sich mappen lässt?
    
    // Remove previous signals
    removeSignal(tabName, experimentName, type);
    
    // Write signals to nodes
    //Map<VisualizedData, NodeMap> signalMaps = getAnnotatedSignals(true);
    for (Node n : graph.getNodeArray()) {
      Collection<T> n_nsList = node2nsMap.get(n);
      // Do we have associated signals?
      if (n_nsList!=null && n_nsList.size()>0) {
        writeSignalsToNode(n, n_nsList, tabName, experimentName, type);
        //----------
      }
    }
    

    // Register signalMaps in graph
    // Is already done by writeSignalsToNode() !
//    for (Entry<VisualizedData, NodeMap> signalSet : signalMaps.entrySet()) {
//      String v = getSignalMapIdentifier(signalSet.getKey());
//      tools.addMap(v, signalSet.getValue());
//    }
  }
  
  /**
   * Write all signals into the node annotation list.
   * @param <T>
   * @param nsList any {@link NameAndSignals} list that will be mapped on nodes
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName
   * @param type
   */
  @SuppressWarnings("unchecked")
  private <T extends NameAndSignals> void writeSignalsToNodes(Collection<T> nsList, String tabName, String experimentName, SignalType type) {
    Map<Node, Set<T>> node2nsMap = nsTools.getNodeToNameAndSignalMapping(nsList);
    
    // Remove previous signals
    removeSignal(tabName, experimentName, type);
    
    // Write signals to nodes
    //Map<VisualizedData, NodeMap> signalMaps = getAnnotatedSignals(true);
    //boolean isMRNAlist = NameAndSignals.getType(nsList).equals(mRNA.class);
    Class<? extends NameAndSignals> nsType = NameAndSignals.getType(nsList);
    MergeType mt = IntegratorUITools.getMergeTypeSilent(type);
    for (Node n : graph.getNodeArray()) {
      Collection<T> n_nsList = node2nsMap.get(n);
      // Do we have associated signals?
      if (n_nsList!=null && n_nsList.size()>0) {
        
        // Build a merged-fake ns to show the merged signal
        boolean fakeAdded = false;
        if (n_nsList.size()>1) { // isMRNAlist && 
          n_nsList = new ArrayList<T>(n_nsList);
          //mRNA fake = new mRNA("#"+ mt.toString());
          try {
            NameAndSignals fake = nsType.getConstructor(String.class).newInstance("#"+ mt.toString());
            fake.addSignal(Signal.merge(n_nsList, mt, experimentName, type));
            ((List<T>)n_nsList).add(0, (T) fake); // add to beginning
            fakeAdded = true;
          } catch (Exception e) {}; // not important...
        }

        writeSignalsToNode(n, n_nsList, tabName, experimentName, type);
        
        // Remove the fake signal
        if (fakeAdded) {
          ((List<T>)n_nsList).remove(0); // remove from beginning
        }
        //----------
      }
    }
  }

  /**
   * Write signal to ONE node.
   * @param <T>
   * @param n the node for all given {@link NameAndSignals}
   * @param nsList NameAndSignal(s) to write to the node
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName
   * @param type
   */
  private <T extends NameAndSignals> void writeSignalsToNode(Node n, Iterable<T> nsList,
    String tabName, String experimentName, SignalType type) {
    if (nsList==null) return;
    
    // Get internal annotation map
    Map<VisualizedData, NodeMap> signalMaps = getAnnotatedSignals(true);
    
    // Concatenate signals of all NameAndSignals associated with this node.
    Map <ValueTriplet<String, String, SignalType>, StringBuffer> signalIdentifier2text = 
      new HashMap <ValueTriplet<String, String, SignalType>, StringBuffer>();
    
    // Sort list by significance
    List<T> sorted = NameAndSignals.sortBySignificance(nsList, experimentName, type);
    int MAX_SIGNALS_TO_WRITE=5;
    
    // Summarize all signals for all experiment and type combinations in one string
    int counter=0;
    for (T ns : sorted) {
      List<Signal> signals = NameAndSignal2PWTools.getSignals(ns);
      for (Signal sig: signals) {
        // Filter signals
        if ((experimentName==null || sig.getName().equals(experimentName)) &&
            (type==null || sig.getType().equals(type))) {
          counter++;
          // Get existing NodeMap for signal
          ValueTriplet<String, String, SignalType> key = new ValueTriplet<String, String, SignalType>(tabName, sig.getName(), sig.getType());
          
          // Write signal to textBuffer
          StringBuffer buff = signalIdentifier2text.get(key);
          if (buff==null) {
            buff = new StringBuffer();
            signalIdentifier2text.put(key, buff);
          }
          if (buff.length()>0) buff.append(StringUtil.newLine());
          buff.append(String.format("%s: %s", ns.getUniqueLabel(), sig.getNiceSignalString() ));
          
          // Break if we have many signals, but avoid "and 0 more".
          if (counter>=MAX_SIGNALS_TO_WRITE && ((sorted.size()-counter)>0) ) {
            if (buff.length()>0) buff.append(StringUtil.newLine());
            buff.append(String.format("... and %s more.", (sorted.size()-counter) ));
            break;
          }
        }
      }
      if (counter>=MAX_SIGNALS_TO_WRITE) break;
    }
    
    // Now we have a text for each signal we want to write to the node.
    for (ValueTriplet<String, String, SignalType> key : signalIdentifier2text.keySet()) {
      NodeMap sigMap = signalMaps.get(key);
      if (sigMap==null) {
        // Create new map for this signal
        sigMap = graph.createNodeMap();
        // Register this map also in internal map-lists
        signalMaps.put(new VisualizedData(key), sigMap);
        tools.addMap(getSignalMapIdentifier(key), sigMap);
      }
      // Set signal annotation
      sigMap.set(n, signalIdentifier2text.get(key).toString());
    }
    
  }
  
  

  /**
   * Generates an identifier for a SignalTriplet (file/tabname, experimentName, SignalType).
   * @param identifier
   * @return generated key to identify the map.
   * @see #getSignalMapIdentifier(VisualizedData)
   */
  public static String getSignalMapIdentifier(
    ValueTriplet<String, String, SignalType> identifier) {
    String key = String.format("[%s] %s (from \"%s\")", 
      identifier.getC().toString(), identifier.getB(), identifier.getA());
    return key;
  }
  
  /**
   * Generates an identifier for a SignalTriplet (file/tabname, experimentName, SignalType).
   * @param identifier
   * @return generated key to identify the map.
   * @see #getSignalMapIdentifier(ValueTriplet)
   */
  private String getSignalMapIdentifier(VisualizedData identifier) {
    String key = String.format("[%s] %s (from \"%s\")", 
      identifier.getSigType().toString(), identifier.getExperimentName(), identifier.getTabName().toString());
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
    Map<VisualizedData, NodeMap> signalMaps = getAnnotatedSignals();
    if (signalMaps==null) return;
    Iterator<Entry<VisualizedData, NodeMap>> it = signalMaps.entrySet().iterator();
    while (it.hasNext()) {
      Entry<VisualizedData, NodeMap> signalSet = it.next();
      if (signalSet.getKey().getTabName().equals(tabName)) {
        if ((experimentName==null || signalSet.getKey().getExperimentName().equals(experimentName)) &&
            (type==null || signalSet.getKey().getSigType().equals(type))) {
          // Remove map from all data providers
          tools.removeMap(getSignalMapIdentifier(signalSet.getKey()));
          // Remove from internal listing
          it.remove();
        }
      }
    }
  }
  
  /**
   * Removes all annotated signals from a node.
   * @param node
   */
  @SuppressWarnings("unused")
  private void removeSignals(Node node) {
    Map<VisualizedData, NodeMap> signalMaps = getAnnotatedSignals();
    if (signalMaps==null) return;
    Iterator<Entry<VisualizedData, NodeMap>> it = signalMaps.entrySet().iterator();
    while (it.hasNext()) {
      Entry<VisualizedData, NodeMap> signalSet = it.next();
      if (signalSet.getValue().get(node)!=null) {
        signalSet.getValue().set(node, null);
      }
    }
  }

  
  /**
   * Visualize a dataset in the pathway. This includes the following steps:
   * <ol><li>Add {@link NameAndSignals} to nodes and perform splits
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
    return visualizeData(tab.getData(), tab.getName(), experimentName, type);
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
  public <T extends NameAndSignals> int visualizeData(Collection<T> nsList, String tabName, String experimentName, SignalType type) {
    SBPreferences prefs = SBPreferences.getPreferencesFor(SignalOptions.class);
    // If explicitly PATHWAY_CENTERED is set or nothing is set
    boolean pwCentered = SignalOptions.PATHWAY_CENTERED.getValue(prefs) ||
      (!SignalOptions.PROBE_CENTERED.getValue(prefs) && !SignalOptions.GENE_CENTERED.getValue(prefs));
    Class<? extends NameAndSignals> inputType = NameAndSignals.getType(nsList);
    
    // First, disallow invalid data selections
  //Keyword: DNAm-pValue
//    if (DNAmethylation.class.isAssignableFrom(inputType) && !type.equals(SignalType.pValue)) {
    // && if (isInteractive) ???
//      GUITools.showErrorMessage(null, "Only p-values of DNA methylation data can be visualized.");
//      return -1;
//    }
    
    // 0. Remove previous visualizations of the same data.
    VisualizedData visData = new VisualizedData(tabName, experimentName, type, inputType);
    if (isDataVisualized(visData)) {
      removeVisualization(tabName, experimentName, type);
    }
    
    // Write the ns probe-based to annotation
    nsTools.writeRawNStoNodeAnnotation(nsList, visData);
    
    // 0.5 Preprocessing: GENE-CENTER data if requested.
    if (!SignalOptions.PROBE_CENTERED.getValue(prefs)) {
      MergeType merge = IntegratorUITools.getMergeTypeSilent(prefs, type);
      if (DNAmethylation.class.isAssignableFrom(inputType) && type.equals(SignalType.pValue)) {
        // Override with a fixed merge type for DNA methylation data
        merge = MergeType.NormalizedSumOfLog2Values;
      }
      
      // Gene-center everything BUT enrichment objects.
      if (!EnrichmentObject.class.isAssignableFrom(inputType)) {
        nsList = NameAndSignals.geneCentered(nsList, merge);
      }
    }
    
    // Branch between mRNA and miRNA (=> Node color) and other types (=> labels)
    int nodesColored = 0;
    if (ProteinModificationExpression.class.isAssignableFrom(inputType)) {
      // Protein modifications as boxes (node labels) below nodes
      nodesColored=addBoxedLabelsBelowNodes(nsList, tabName, experimentName, type);
      
    } else if (DNAmethylation.class.isAssignableFrom(inputType)) {
      // DNA methylation as black box with varied width (node labels) left of nodes
      nodesColored=addBlackBoxLeftOfNodes(nsList, tabName, experimentName, type);
      
    } else {
      if (!(mRNA.class.isAssignableFrom(inputType) || miRNA.class.isAssignableFrom(inputType)
          || PairedNS.class.isAssignableFrom(inputType) || EnrichmentObject.class.isAssignableFrom(inputType))) {
        log.warning("No visualization method implemented for " + inputType.getSimpleName() + ". Using node-color-visualization.");
      }
    
      // 1. Add NS to nodes and perform splits
      Collection<T> oldNsList = nsList;
      nsList = nsTools.prepareGraph(nsList, tabName, experimentName, type, pwCentered);
      
      // 2. color nodes
      SignalColor recolorer = new SignalColor(nsList, experimentName, type);
      nodesColored = colorNodesAccordingToSignals(recolorer, tabName, experimentName, type);
      
      
      // 3. write signals to nodes
      writeSignalsToNodes(oldNsList, tabName, experimentName, type);
      
      // 4. change shape
      // TODO: If shape-changing is introduced, make it similar to writeSignals
       
    }
//    } else {
//      log.warning("No visualization method implemented for " + inputType.getSimpleName());
//    }
    
    // Remember that we have visualized this data.
    if (nodesColored>0) {
      getVisualizedData(true).add(visData);
    } else {
      // Mostly not the same species...
      if(!miRNA.class.isAssignableFrom(inputType)) { // miRNA has its own warning.
        String noNodesMatchString = "Could not find any graph nodes that match to the input data.";
        log.warning(noNodesMatchString);
        if (isInteractive) {
          GUITools.showErrorMessage(null, noNodesMatchString);
        }
      }
    }
    
    
    return nodesColored;
  }
  
  /**
   * Add boxes below nodes that match any of the given {@link NameAndSignals}
   * that are colored according to given signal and contain the unique label
   * of the {@link NameAndSignals}.
   * <p>This method is intended for {@link ProteinModificationExpression} data.
   * @param <T>
   * @param nsList list of {@link NameAndSignals} for which boxes should be added
   * @param tabName unique identifier to re-identify the given <code>nsList</code>
   * @param experimentName filter for certain signals from <code>nsList</code>
   * @param type filter for certain signals from <code>nsList</code>
   * @return number of nodes that have been changed
   */
  public <T extends NameAndSignals> int addBoxedLabelsBelowNodes(Collection<T> nsList, 
    String tabName, String experimentName, SignalType type) {
    // Read box height from preferences (Default:8)
    SBPreferences prefs = SBPreferences.getPreferencesFor(PathwayVisualizationOptions.class);
    int boxHeight = PathwayVisualizationOptions.PROTEIN_MODIFICATION_BOX_HEIGHT.getValue(prefs);
    
    // Prepare maps and required classes
    MergeType sigMerge = IntegratorUITools.getMergeTypeSilent(type);
    Map<Node, Set<T>> n2ns = nsTools.getNodeToNameAndSignalMapping(nsList);
    SignalColor recolorer = new SignalColor(nsList, experimentName, type);
    Float ignoreFC = PathwayVisualizationOptions.DONT_VISUALIZE_FOLD_CHANGES.getValue(prefs);
    Color forNothing = PathwayVisualizationOptions.COLOR_FOR_NO_FOLD_CHANGE.getValue(prefs);
    if (ignoreFC==null||Double.isNaN(ignoreFC.doubleValue())) ignoreFC=0f;
    Double ignorePV = PathwayVisualizationOptions.DONT_VISUALIZE_P_VALUES.getValue(prefs);
    if (ignorePV==null||Double.isNaN(ignorePV.doubleValue())) ignorePV=1d;
    VisualizedData visData = new VisualizedData(tabName, experimentName, type, NameAndSignals.getType(nsList));
    
    int changedNodes = 0;
    for (Node n: n2ns.keySet()) {
      Set<String> addedAnalytes = new HashSet<String>(); // required to hide multiple values coming from the same analyte.
      Set<T> nsForNode = n2ns.get(n);
      if (nsForNode==null) continue;
      
      int i=tools.getNumberOfLabels(n, NodeLabel.SIDES); // Calc offset for further labels
      NodeRealizer nr = graph.getRealizer(n);
      double maxW = nr.getWidth();
      List<T> sorted = NameAndSignals.sortByUniqueLabel(nsForNode);
      for (T ns: sorted) {
        double signalValue = ns.getSignalMergedValue(type, experimentName, sigMerge);
        if (Double.isNaN(signalValue)) continue;
        
        // Get the label
        String label = ns.getUniqueLabel();
        if (ns instanceof ProteinModificationExpression) {
          String analyteID = ((ProteinModificationExpression) ns).getAnalyteID();
          // Add each analyte ID to a set and visualize it only per node once,
          // even if the analyte matches to multiple IDs.
          if (analyteID!=null && !addedAnalytes.add(analyteID)) continue;
          label = ((ProteinModificationExpression) ns).getAnalyteShortName();
        }
        
        NodeLabel nl = nr.createNodeLabel();
        nl.setText(label);
        nr.addLabel(nl);
        
        // Remember what we visualized
        nl.setUserData(new ValuePair<VisualizedData, NameAndSignals>(visData, ns));
        
        nl.setModel(NodeLabel.SIDES);
        nl.setPosition(NodeLabel.S);
        Color color;
        if (!considerSignal(signalValue, type, ignoreFC, ignorePV)) {
          color = forNothing;
        } else {
          color = recolorer.getColor(signalValue);
        }
        nl.setBackgroundColor(color);
        nl.setAutoSizePolicy(NodeLabel.AUTOSIZE_NODE_WIDTH);
        nl.setContentHeight(boxHeight);
        nl.setContentWidth(nr.getWidth());
        nl.setLineColor(Color.BLACK);
        nl.setFontSize(boxHeight);
        nl.setInsets(new Insets(-1,0,0,0));
        nl.setDistance(i++ * boxHeight);
        nl.calculateSize();
        maxW = Math.max(maxW, nl.getWidth());
      }
      // Make node at least as big as biggest label
      nr.setWidth(maxW);
      
      // Write all those signals to node annotations
      writeSignalsToNode(n, sorted, tabName, experimentName, type);
      changedNodes++;
    }
    // TODO: Change eventual DNA methylation box-heights. If so,
    // Change also on REMOVAL of prot mod data.
    return changedNodes;
  }
  
  /**
   * Adds a black box left of the node, that changes it size, i.e.,
   * scales from minSignalValue to maxSignalValue.
   * <p>This isintended to represent changes in {@link DNAmethylation}
   * in promoter regions.
   * @param <T>
   * @param nsList list of {@link NameAndSignals} for which boxes should be added
   * @param tabName unique identifier to re-identify the given <code>nsList</code>
   * @param experimentName filter for certain signals from <code>nsList</code>
   * @param type filter for certain signals from <code>nsList</code>
   * @return number of nodes that have been changed
   */
  public <T extends NameAndSignals> int addBlackBoxLeftOfNodes(Collection<T> nsList, 
    String tabName, String experimentName, SignalType type) {
    boolean showBorderForDNAmethylationBox = true;
    // Read max. box width from preferences (Default:10)
    SBPreferences prefs = SBPreferences.getPreferencesFor(PathwayVisualizationOptions.class);
    double maxWidth = PathwayVisualizationOptions.DNA_METHYLATION_MAXIMUM_BOX_WIDTH.getValue(prefs);
    double halfWidth = ((double)maxWidth/2);
    // The protein mod. box height is required to calc. the dna methylation bar height
    int protBoxHeight = PathwayVisualizationOptions.PROTEIN_MODIFICATION_BOX_HEIGHT.getValue(prefs);
    float maxFC = PathwayVisualizationOptions.FOLD_CHANGE_FOR_MAXIMUM_COLOR.getValue(prefs);
    double minPV = PathwayVisualizationOptions.P_VALUE_FOR_MAXIMUM_COLOR.getValue(prefs);
    Float ignoreFC = PathwayVisualizationOptions.DONT_VISUALIZE_FOLD_CHANGES.getValue(prefs);
    if (ignoreFC==null||Double.isNaN(ignoreFC.doubleValue())) ignoreFC=0f;
    Double ignorePV = PathwayVisualizationOptions.DONT_VISUALIZE_P_VALUES.getValue(prefs);
    if (ignorePV==null||Double.isNaN(ignorePV.doubleValue())) ignorePV=1d;
    
    // Prepare maps and required classes
    // XXX: Fixed MergeType for DNA-m data. Must be pValues in here.
    // TODO: Consider writing the "top-10" pValues to ToolTip or similar instead of this value.
    MergeType sigMerge =  MergeType.NormalizedSumOfLog2Values; // IntegratorUITools.getMergeTypeSilent();
    Map<Node, Set<T>> n2ns = nsTools.getNodeToNameAndSignalMapping(nsList);
    // XXX: All "-1" geneIds are summed up to a very great number ing global min max...
    //double[] minMax = NameAndSignals.getMinMaxSignalGlobal(nsList, experimentName, type);
    // Better take 90% value as max.
    // TODO: Observe results of the implemented normalized quantile max calculation...
    double[] minMax = NameAndSignals.getMinMaxSignalQuantile(nsList, experimentName, type, 90);
//    System.out.println(Arrays.toString(minMax));
    // TODO: Test a few to know good max values. Test setting min to global min?
    log.config(String.format("Min/max values for box to visualize data: %s to %s.", minMax[0], minMax[1]));
    double maxSignalValue = minMax[1]+minMax[0];
    VisualizedData visData = new VisualizedData(tabName, experimentName, type, NameAndSignals.getType(nsList));
    
    // TODO: Better show pValue AND fold-change!
    // (via position: left/right of node OR color).
    
    // Rescaler only required for fold change visualization
    AbstractRescale rescale;
    if (type.equals(SignalType.FoldChange)) {
      rescale = new LinearRescale(0, maxFC, 0d, halfWidth);
    } else {
      // p-values
      rescale = new LogarithmicRescale(minPV, ignorePV, 2, Arrays.asList(0d, maxWidth));
    }
    
    int changedNodes=0;
    for (Node n: n2ns.keySet()) {
      Set<T> nsForNode = n2ns.get(n);
      if (nsForNode==null) continue;
      
      NodeRealizer nr = graph.getRealizer(n);
      // TODO: Also stretch to prot. mod. labels?
      double barHeight = nr.getHeight();// + tools.getNumberOfLabels(n, NodeLabel.SIDES) * boxHeight;
      for (T ns: nsForNode) {
        double signalValue = ns.getSignalMergedValue(type, experimentName, sigMerge);
        if (Double.isNaN(signalValue)) continue;
        
        // TODO: Write signals to nodes?
        
        // Remember what we visualized
        ValuePair<VisualizedData, NameAndSignals> visNS = new ValuePair<VisualizedData, NameAndSignals>(visData, ns);
        
        NodeLabel nl = null;
        if (considerSignal(signalValue, type, ignoreFC, ignorePV)) {
          nl = nr.createNodeLabel();
          nl.setUserData(visNS);
          
          nl.setModel(NodeLabel.FREE);
          nl.setPosition(NodeLabel.W);
          nl.setBackgroundColor(nr.getLineColor());
          nl.setLineColor(nl.getBackgroundColor());
          nl.setAutoSizePolicy(NodeLabel.AUTOSIZE_NONE);
          nl.setContentHeight(barHeight);
          nl.setDistance(0); // Adjacent to node
          
          // Set box size dependent on signal value
          if (type.equals(SignalType.FoldChange)) {
            double rescaledSignal;
            if (Math.abs(signalValue)<ignoreFC) {
              rescaledSignal = 0;
            } else {
              rescaledSignal = rescale.rescale(Math.abs(signalValue)).doubleValue();
            }
            nl.setContentWidth(rescaledSignal);
            
            if (signalValue>=0) {
              // negative fc => draw left of middle
              nl.setFreeOffset(-halfWidth, 0); // = left border
            } else {
              // positive fc => draw right of middle
              nl.setFreeOffset(-halfWidth-rescaledSignal, 0); // = left border
            }
          } else {
            // pValues
            //nl.setContentWidth(Math.min(Math.max((signalValue+minMax[0])/maxSignalValue, 0), 1)*maxWidth); // =width
            nl.setFreeOffset(-maxWidth, 0); // =left border
            nl.setContentWidth(maxWidth - rescale.rescale(signalValue).doubleValue()); // =width
          }
        }
        
        if (showBorderForDNAmethylationBox) {
          NodeLabel border = nr.createNodeLabel(); //(NodeLabel) nl.clone();
          border.setModel(NodeLabel.FREE);
          border.setBackgroundColor(Color.WHITE);
          border.setContentWidth(maxWidth);
          border.setFreeOffset(-border.getContentWidth(), 0);
          border.setUserData(visNS); // We need to do that to allow removal of the border.
          
          
          border.setPosition(NodeLabel.W);
          border.setAutoSizePolicy(NodeLabel.AUTOSIZE_NONE);
          border.setContentHeight(barHeight);
          border.setDistance(0);
          border.setLineColor(Color.BLACK);
          
          nr.addLabel(border);
        }
        
        if (nl!=null) nr.addLabel(nl);
      }
      
      // Write all those signals to node annotations
      // TODO: Observe behaviour on probe-based data; write only top-5 dysregulated/min-pVal
      writeSignalsToNode(n, nsForNode, tabName, experimentName, type);
      changedNodes++;
    }
    
    return changedNodes;
  }

  /**
   * Checks if we should consider or ignore a signal, according
   * to signal type/value and defined cutoffs.
   * <p>Method does NOT auto-detect unlogged fold-changes!
   * @param signalValue actual value
   * @param type {@link SignalType} of actual value
   * @param ignoreFC symmetric log2 fold-change 
   * @param ignorePV pvalue upper threshold
   * @return true if methods should consider/paint this signal.
   * False, if the signal should be skipped.
   */
  public static boolean considerSignal(double signalValue, SignalType type,
    float ignoreFC, double ignorePV) {
    if (type!=null) {
      if (type.equals(SignalType.FoldChange)) {
        // Absolute value must be greater than threshold.
        // This does NOT work for unlogged fold-changes!
        return Math.abs(signalValue)>=ignoreFC;
      } else if (type.equals(SignalType.pValue)||type.equals(SignalType.qValue)) {
        // There are methods that sum up the -log2 p-values. Thus, this is no
        // actual p-value anymore and we should not filter those values.
        // This should not be a problem, since no real p-value is >1
        if (signalValue>1) return true;
        // Else, simply look if p-value is below our defined threshold.
        return signalValue<=ignorePV;
      }
    }
    return true;
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
    boolean inputContainedMicroRNAnodes=false;
    boolean inputContainedmRNAnodes=false;
    MergeType sigMerge = IntegratorUITools.getMergeTypeSilent(type);
    Float ignoreFC = PathwayVisualizationOptions.DONT_VISUALIZE_FOLD_CHANGES.getValue(prefs);
    Color forNothing = PathwayVisualizationOptions.COLOR_FOR_NO_FOLD_CHANGE.getValue(prefs);
    Double ignorePV = PathwayVisualizationOptions.DONT_VISUALIZE_P_VALUES.getValue(prefs);
    if (ignorePV==null||Double.isNaN(ignorePV.doubleValue())) ignorePV=1d;
    if (ignoreFC==null||Double.isNaN(ignoreFC.doubleValue())) ignoreFC=0f;
    
    DataMap nsMapper     = tools.getMap(GraphMLmapsExtended.NODE_NAME_AND_SIGNALS);
    DataMap parentMapper = tools.getMap(GraphMLmapsExtended.NODE_BELONGS_TO);
    Set<Node> nodesToResetColor = new HashSet<Node>(Arrays.asList(graph.getNodeArray()));
    for (Node n: graph.getNodeArray()) {
      // Decide if we want to re-color this node
      ValueTriplet<String, String, SignalType> parent = parentMapper==null?null:(ValueTriplet<String, String, SignalType>) parentMapper.get(n);
      if (parent==null) {
        continue; // not belonging to any parent...
      } else if ((tabName==null || parent.getA().equals(tabName)) &&
          (experimentName==null || parent.getB().equals(experimentName)) &&
          (type==null || parent.getC().equals(type))) {
        
        // Get the actual signal to consider when recoloring
        NameAndSignals ns = (NameAndSignals) nsMapper.get(n);
        if (ns==null) continue;
        if (ns instanceof miRNA) inputContainedMicroRNAnodes=true;
        else inputContainedmRNAnodes=true;
        
        // Novel: Also consider referenced things
        List<Signal> signals = NameAndSignal2PWTools.getSignals(ns);
        Signal sig = Signal.mergeSignal(signals, sigMerge, experimentName, type);
        if (sig==null) continue;
        double signalValue = sig.getSignal().doubleValue();
        //---
        
        //double signalValue = ns.getSignalMergedValue(type, experimentName, sigMerge);
        if (Double.isNaN(signalValue)) continue;
        Color newColor;
        if (!considerSignal(signalValue, type, ignoreFC, ignorePV)) {
          newColor = forNothing;
          if (graph.getRealizer(n) instanceof LineNodeRealizer && forNothing.equals(Color.WHITE)) {
            // Lines are completely invisible when white...
            newColor = Color.GRAY;
          }
        } else {
          newColor = recolorer.getColor(signalValue);
        }
        
        // Recolor node and remember to don't gray it out.
        graph.getRealizer(n).setFillColor(newColor);
        nodesToResetColor.remove(n);
        // ---------------------------------
        
      } else if (parent!=null) {
        // Node has been created for a specific parent, but it is
        // not this one => don't recolor it, simply skip it.
        nodesToResetColor.remove(n);
      }
    }
    
    // Set unaffected color for all other nodes but reference nodes.
    Color colorForUnaffectedNodes = PathwayVisualizationOptions.COLOR_FOR_NO_VALUE.getValue(prefs);
    if (colorForUnaffectedNodes==null) colorForUnaffectedNodes = Color.LIGHT_GRAY;
    for (Node n: nodesToResetColor) {
      // kgId is defined for all KEGG nodes, but NULL for all miRNA nodes.
      Object kgId = TranslatorTools.getKeggIDs(n);
      // Look if it is the Title/ a PW-reference node
      boolean isPathwayReferenceNode = (kgId!=null && kgId.toString().toLowerCase().trim().startsWith("path:"));
      
      if (isPathwayReferenceNode) {
        // Node is a pw-reference, but we just want to color genes
        if (!experimentName.equals(EnrichmentObject.signalNameForPvalues)) continue;
      } else {
        // Node is a gene, but we just want to color pw-references
        if (experimentName.equals(EnrichmentObject.signalNameForPvalues)) continue;
      }
      
      // If input is miRNA, remove all non-miRNA nodes from colorForUnaffectedNodes and via versa.
      if (!inputContainedMicroRNAnodes && tools.getBoolInfo(n, GraphMLmapsExtended.NODE_IS_MIRNA)) {
        continue;
      } if (!inputContainedmRNAnodes && !tools.getBoolInfo(n, GraphMLmapsExtended.NODE_IS_MIRNA)) {
        continue;
      }
      
      graph.getRealizer(n).setFillColor(colorForUnaffectedNodes);
    }
    
    return graph.getNodeArray().length-nodesToResetColor.size();
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
        
        // Group observations into compatible groups
        Collection<List<ValueTriplet<NameAndSignalsTab, String, SignalType>>> grouped = 
          IntegratorUITools.groupCompatibleSignals(Arrays.asList(observations));
        
        // Color every pathway and signal combination
        Map<String, Graph2D> pathwayCache = new HashMap<String, Graph2D>();
        AbstractProgressBar bar = IntegratorUI.getInstance().getStatusBar().showProgress();
        bar.setNumberOfTotalCalls(pathwayIDs.length*grouped.size());
        int success=0;
        boolean askedToAnnotateMiRNAs = false;
        for (String pw : pathwayIDs) {
          String pwNumber = Utils.getNumberFromStringRevAsString(pw.length(), pw);
          for (List<ValueTriplet<NameAndSignalsTab, String, SignalType>> obs: grouped) {
            bar.DisplayBar();
            
            try {
              // Get species and complete kegg pathway id.
              Species species = obs.get(0).getA().getSpecies();
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
                  bar.setCallNr(bar.getCallNumber()+(grouped.size()-1));
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
              String inputFileName = "multiple";
              if (obs.size()<2) {
                inputFileName = FileTools.trimExtension(obs.get(0).getA().getName());
              }
              String obsExpName = obs.get(0).getB();
              if (obsExpName.toLowerCase().trim().endsWith("_foldchange")) {
                // Dirty hardcoded solution for johannes column headers ;-)
                obsExpName = obsExpName.substring(0, obsExpName.length()-11);
              }
              
              String obsExpType = mergeTypeStrings(obs);
              VisualizeDataInPathway instance = new VisualizeDataInPathway(graph, false);
              boolean addedMiRNAnode = false;
              for (ValueTriplet<NameAndSignalsTab, String, SignalType> vt : obs) {
                // Visualize each dataset
                if (NameAndSignals.isMicroRNA(vt.getA().getData())) {
                  if (!askedToAnnotateMiRNAs) {
                    addedMiRNAnode = KEGGPathwayActionListener.addMicroRNAs(graph, vt.getA());
                  } else {
                    addedMiRNAnode = KEGGPathwayActionListener.addMicroRNAs(graph, (Collection<? extends miRNA>) vt.getA().getData(), true)>0;
                  }
                  askedToAnnotateMiRNAs = true;
                }
                
                instance.visualizeData(vt.getA(), vt.getB(), vt.getC());
              }
              
              // Adjust title node
              Node n = TranslatorTools.getTitleNode(graph, pathwayID);
              double oldHeight = 0; String oldText = ""; double oldCY = 0;
              if (n!=null) {
                NodeRealizer nr = graph.getRealizer(n);
                oldHeight = nr.getHeight();
                oldCY = nr.getCenterY();
                nr.setHeight(oldHeight*2);
                nr.setCenterY(oldCY-oldHeight/2); // /2 is intentionally (even though not logically).
                
                oldText = graph.getLabelText(n);
                graph.setLabelText(n, String.format("%s\n%s [%s]", oldText, obsExpName, obsExpType));
              }
              graph.unselectAll();
              
              // Save graph.
              try {
                String outFile = Utils.ensureSlash(outputDir.getPath()) +
                StringUtil.removeAllNonFileSystemCharacters(
                  pathwayID + '.' + obsExpName + '.' + obsExpType  + '.' + inputFileName + '.' + outputFormat);
                // The next line will also eventually download a KEGG picture from online
                translator.writeToFile(graph, outFile, outputFormat);
                success++;
              } catch (Throwable e) {
                GUITools.showErrorMessage(null, e);
              }
              
              // Revert modifications for usage with next observation
              if (n!=null) {
                NodeRealizer nr = graph.getRealizer(n);
                nr.setHeight(oldHeight);
                nr.setCenterY(oldCY);
                graph.setLabelText(n, oldText);
              }
              
              // Remove modifications
              for (ValueTriplet<NameAndSignalsTab, String, SignalType> vt : obs) {
                // Only works for mRNA (or any unknown)
                instance.removeVisualization(vt.getA().getName(),vt.getB(), vt.getC());
                // Only works for Phospho or DNA methylation
                instance.removeVisualizedLabels(vt.getA().getName(),vt.getB(), vt.getC());
              }
              // Doesn't work if we don't initialize with TranslatorPanel
              //instance.removeVisualization(NameAndSignals.getType(observation.getA().getData()));
              if (addedMiRNAnode) {
                TranslatorTabActions.removeMicroRNAnodes(instance.tools);
              }
            } catch (Throwable t) {
              t.printStackTrace();
              GUITools.showErrorMessage(null, t);
            }
          }
        }
        return success;
      }
      
      /**
       * Concatenates all SignalTypes with a plus symbol.
       * @param obs
       * @return
       */
      private String mergeTypeStrings(
        List<ValueTriplet<NameAndSignalsTab, String, SignalType>> obs) {
        Set<SignalType> st = new HashSet<Signal.SignalType>();
        for (ValueTriplet<NameAndSignalsTab, String, SignalType> vt : obs) {
          st.add(vt.getC());
        }
        return ArrayUtils.implode(st, "+");
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

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
import java.util.List;
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
import de.zbit.gui.prefs.SignalOptions;
import de.zbit.kegg.Translator;
import de.zbit.kegg.gui.KGMLSelectAndDownload;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.parser.Species;
import de.zbit.util.AbstractProgressBar;
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
 * @author Clemens Wrzodek
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
  protected TranslatorPanel panelContainingGraph;
  
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
  public VisualizeDataInPathway(TranslatorPanel tp){
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
    super();
    this.graph=graph;
    panelContainingGraph = null;
    if (this.graph==null) log.warning("Graph is null!");
    this.nsTools = new NameAndSignal2PWTools(graph);
    tools = nsTools.getTranslatorTools();
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
    if (panelContainingGraph==null) return createIfNotExists?new ArrayList<ValueTriplet<String, String, SignalType>>():null;
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
    if (panelContainingGraph==null) return createIfNotExists?new HashMap<ValueTriplet<String, String, SignalType>, NodeMap>():null;
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
   * Remove a visualized dataset from the graph. (Un-visualize dataset).
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName to describe the signal
   * @param type to describe the signal
   */
  public void removeVisualization(String tabName, String experimentName, SignalType type) {
    // 1. Get all nodes for experiment
    log.finer("Getting all nodes for given experiment and NODE_IS_COPY map.");
    Collection<Node> nodes = nsTools.getAllNodesForExperiment(tabName, experimentName, type);
    if (nodes.size()<1) return;
    DataMap isCopyMap = tools.getMap(GraphMLmapsExtended.NODE_IS_COPY);
    
    // 2. Remove cloned nodes and ungroup parents (if group size = 1)
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
        tools.resetWidthAndHeight(n);
     // TODO: Also reset shape in resetWidthAndHeight()
      } else {
        toLayout.add(n);
      }
      tools.setInfo(n, GraphMLmapsExtended.NODE_BELONGS_TO, null);
      tools.setInfo(n, GraphMLmapsExtended.NODE_NAME_AND_SIGNALS, null);
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
    ValueTriplet<String, String, SignalType> dataID = new ValueTriplet<String, String, SignalType>(tabName, experimentName, type);
    List<ValueTriplet<String, String, SignalType>> visualizedData = getVisualizedData();
    if (visualizedData!=null) visualizedData.remove(dataID);
    
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
  private <T extends NameAndSignals> void writeSignalsToNodes(Iterable<T> nsList, String tabName, String experimentName, SignalType type) {
    Map<Node, Set<T>> node2nsMap = nsTools.getAnnotatedNodes(nsList);
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
          List<Signal> signals = NameAndSignal2PWTools.getSignals(ns);
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
    for (Entry<ValueTriplet<String, String, SignalType>, NodeMap> signalSet : signalMaps.entrySet()) {
      String v = getSignalMapIdentifier(signalSet.getKey());
      tools.addMap(v, signalSet.getValue());
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
  
  /**
   * Removes all annotated signals from a node.
   * @param node
   */
  @SuppressWarnings("unused")
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
    
    // 0. Remove previous visualizations of the same data.
    if (isDataVisualized(tabName, experimentName, type)) {
      removeVisualization(tabName, experimentName, type);
    }
    
    // 0.5 Preprocessing: GENE-CENTER data if requested.
    if (!SignalOptions.PROBE_CENTERED.getValue(prefs)) {
      nsList = NameAndSignals.geneCentered(nsList, IntegratorGUITools.getMergeTypeSilent(prefs));
    }
    
    // 1. Add NS to nodes and perform splits
    nsList = nsTools.prepareGraph(nsList, tabName, experimentName, type, pwCentered);
    
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
    boolean inputContainedMicroRNAnodes=false;
    boolean inputContainedmRNAnodes=false;
    MergeType sigMerge = IntegratorGUITools.getMergeTypeSilent();
    
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
        double signalValue = ns.getSignalMergedValue(type, experimentName, sigMerge);
        if (Double.isNaN(signalValue)) continue;
        Color newColor = recolorer.getColor(signalValue);
        
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
      Object kgId = TranslatorTools.getKeggIDs(n);
      // kgId is defined for all KEGG nodes, but NULL for all miRNA nodes.
      if (kgId!=null && kgId.toString().toLowerCase().trim().startsWith("path:")) continue; // Title node
      
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
              VisualizeDataInPathway instance = new VisualizeDataInPathway(graph);
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

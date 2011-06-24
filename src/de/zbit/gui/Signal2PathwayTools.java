/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import y.base.DataMap;
import y.base.Node;
import y.base.NodeMap;
import y.view.Graph2D;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.kegg.TranslatorTools;
import de.zbit.kegg.ext.GenericDataMap;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.math.LinearRescale;
import de.zbit.util.ValuePair;

/**
 * This class is intended to provide tools for connecting {@link Signal}s
 * to KEGG Pathways in {@link Graph2D}.
 * @author Clemens Wrzodek
 */
public class Signal2PathwayTools {
  public static final transient Logger log = Logger.getLogger(Signal2PathwayTools.class.getName());
  
  /**
   * A graph on which operations are performed.
   */
  Graph2D graph;
  
  public Signal2PathwayTools(TranslatorPanel tp){
    this(tp.isGraphML()?(Graph2D) tp.getDocument():null);
  }
  
  public Signal2PathwayTools(Graph2D graph){
    super();
    this.graph=graph;
    if (this.graph==null) log.warning("Graph is null!");
  }
  
  /**
   * Write the signals into the node annotation list.
   * @param nsList
   */
  public <T extends NameAndSignals> void writeSignalsToNodes(Iterable<T> nsList) {
    writeSignalsToNodes(nsList, null,null);
  }
  /**
   * Write the signals into the node annotation list. 
   * @param nsList
   * @param experimentName
   * @param type
   */
  @SuppressWarnings("unchecked")
  public <T extends NameAndSignals> void writeSignalsToNodes(Iterable<T> nsList, String experimentName, SignalType type) {
    
    // Get GeneID 2 Node map
    TranslatorTools tools = new TranslatorTools(graph);
    Map<Integer, Node> map = tools.getGeneID2NodeMap();
    
    // Write signals to nodes
    Map<ValuePair<String, SignalType>, NodeMap> signalMaps = new HashMap<ValuePair<String, SignalType>, NodeMap>();
    for (NameAndSignals ns : nsList) {
      // Get Node(s) for mRNA
      List<Node> node = getNodesForNameAndSignal(map, ns);
      
      // Get Signals
      if (node!=null && node.size()>0) {
        List<Signal> signals = getSignals(ns);
        
        // Add each signal to corresponding nodes.
        for (Signal sig: signals) {
          // Filter signals
          if ((experimentName==null || sig.getName().equals(experimentName)) &&
              (type==null || sig.getType().equals(type))) {
            // Get NodeMap for signal
            ValuePair<String, SignalType> key = new ValuePair<String, SignalType>(sig.getName(), sig.getType());
            NodeMap sigMap = signalMaps.get(key);
            if (sigMap==null) {
              sigMap = graph.createNodeMap();
              signalMaps.put(key, sigMap);
            }
            
            // Write signal to NodeMap
            for (Node n:node)  {
              sigMap.set(n, sig.getSignal().toString());
            }
          }
        }
      }
    }
    
    // Register signalMaps in graph
    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(KEGG2yGraph.mapDescription);
    for (Entry<ValuePair<String, SignalType>, NodeMap> signalSet : signalMaps.entrySet()) {
      String v = "["+signalSet.getKey().getB().toString() + "] " + signalSet.getKey().getA();
      mapDescriptionMap.set(signalSet.getValue(), v);
    }
  }

  @SuppressWarnings("rawtypes")
  private static List<Signal> getSignals(NameAndSignals ns) {
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

  private static List<Node> getNodesForNameAndSignal(Map<Integer, Node> map,
    NameAndSignals ns) {
    List<Node> node = new LinkedList<Node>();
    Collection<Integer> gis = NameAndSignals.getGeneIds(ns);
    for (Integer gi : gis) {
      if (gi!=null && gi.intValue()>0) {
        Node n = map.get(gi);
        if (n!=null) {
          node.add(n);
        }
      }
    }
    return node;
  }

  /**
   * @param data
   */
  public <T extends NameAndSignals> void colorNodesAccordingToSignals(Iterable<T> nsList, MergeType sigMerge, String experimentName, SignalType type, Color... gradientColors) {

    // Get GeneID 2 Node map
    TranslatorTools tools = new TranslatorTools(graph);
    Map<Integer, Node> map = tools.getGeneID2NodeMap();
    
    // TODO: Make a symmetric min and max (-3 to +3) instead of -2.9 to + 3.2 because of better coloring then.
    // Get min max signals for linear rescale models
    double[] minMax = NameAndSignals.getMinMaxSignalGlobal(nsList); // TODO: add experiment Name and Type in max search!
    // TODO Implement local gradient and make selection via Options.
    // TODO Implement color choosers via options
    
    // Initiate color rescaler
    LinearRescale lred = new LinearRescale(minMax[0], minMax[1], getColorChannel(0, gradientColors));
    LinearRescale lgreen = new LinearRescale(minMax[0], minMax[1], getColorChannel(1, gradientColors));
    LinearRescale lblue = new LinearRescale(minMax[0], minMax[1], getColorChannel(2, gradientColors));
    
    for (NameAndSignals ns : nsList) {
      // Get Node(s) for mRNA
      List<Node> node = getNodesForNameAndSignal(map, ns);
      
      // Get Signals
      if (node!=null && node.size()>0) {
        List<Signal> signals = getSignals(ns);
        Collection<Signal> unique = Signal.merge(signals, sigMerge);
        for (Signal sig: unique) {
          if (sig.getName().equals(experimentName) && sig.getType().equals(type)) {
            Number v = sig.getSignal();
            if (v!=null && (!(v instanceof Double ) || !Double.isNaN((Double)v))) {
              double d = v.doubleValue();
              Color newColor = new Color(lred.rescale(d).intValue(),lgreen.rescale(d).intValue(),lblue.rescale(d).intValue());
              for (Node n: node) {
                graph.getRealizer(n).setFillColor(newColor);
              }
            }
          }
        }
        
        
      }
    }
  }

  /**
   * @param i 0=red, 1=green, 2=blue
   * @param gradientColors list of colors
   * @return a list if the corresponding color-intensity in the given list of colors.
   */
  private List<Integer> getColorChannel(int i, Color... gradientColors) {
    List<Integer> red = new LinkedList<Integer>();
    for (Color c: gradientColors) {
      int f = 0;
      if (i==0) f = c.getRed();
      if (i==1) f = c.getGreen();
      if (i==2) f = c.getBlue();
      red.add(f);
    }
    
    return red;
  }
  
}

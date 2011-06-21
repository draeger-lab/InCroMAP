/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import y.base.DataMap;
import y.base.Node;
import y.base.NodeMap;
import y.view.Graph2D;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.kegg.TranslatorTools;
import de.zbit.kegg.ext.GenericDataMap;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.kegg.io.KEGG2yGraph;
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
  public void writeSignalsToNodes(Iterable<mRNA> nsList) {
    writeSignalsToNodes(nsList, null,null);
  }
  /**
   * Write the signals into the node annotation list. 
   * @param nsList
   * @param experimentName
   * @param type
   */
  @SuppressWarnings("unchecked")
  public void writeSignalsToNodes(Iterable<mRNA> nsList, String experimentName, SignalType type) {
    
    // Get GeneID 2 Node map
    TranslatorTools tools = new TranslatorTools(graph);
    Map<Integer, Node> map = tools.getGeneID2NodeMap();
    
    // Write signals to nodes
    Map<ValuePair<String, SignalType>, NodeMap> signalMaps = new HashMap<ValuePair<String, SignalType>, NodeMap>();
    for (mRNA ns : nsList) {
      // Get Node for mRNA
      Node node = null;
      if (ns.getGeneID()>0) node = map.get(ns.getGeneID());
      if (node!=null) {
        List<Signal> signals = ns.getSignals();
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
            sigMap.set(node, sig.getSignal().toString());
          }
        }
      }
    }
    System.out.println("Created " + signalMaps.size() + "maps.");
    // Register signalMaps in grapg
    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(KEGG2yGraph.mapDescription);
    for (Entry<ValuePair<String, SignalType>, NodeMap> signalSet : signalMaps.entrySet()) {
      String v = "["+signalSet.getKey().getB().toString() + "] " + signalSet.getKey().getA();
      mapDescriptionMap.set(signalSet.getValue(), v);
      System.out.println("Set map to mapDesc");
    }
  }
  
}

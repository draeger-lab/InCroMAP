/*
  * $Id:  TranslatorToolsExtended.java 11:17:49 rosenbaum $
  * $URL: TranslatorToolsExtended.java $
  * ---------------------------------------------------------------------
  * This file is part of Integrator, a program integratively analyze
  * heterogeneous microarray datasets. This includes enrichment-analysis,
  * pathway-based visualization as well as creating special tabular
  * views and many other features. Please visit the project homepage at
  * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
  * obtain the latest version of Integrator.
  *
  * Copyright (C) 2011-2013 by the University of Tuebingen, Germany.
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import y.base.Node;
import y.base.NodeMap;
import y.view.Graph2D;
import y.view.NodeRealizer;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.graph.io.def.GraphMLmaps;
import de.zbit.gui.IntegratorUITools;
import de.zbit.mapper.compounds.KeggCompound2InChIKeyMapper;
import de.zbit.util.TranslatorTools;

/**
 * A class that extends translator tools and adds compound functionality that is not naturally
 * available for Kegg
 * @author Lars Rosenbaum
 * @version $Rev$
 */

public class TranslatorToolsExtended extends TranslatorTools{
	public static final transient Logger log = Logger.getLogger(TranslatorToolsExtended.class.getName());
	  
	  
	  public TranslatorToolsExtended(TranslatorPanel<Graph2D> tp){
	    super(tp.getDocument());
	  }
	  
	  public TranslatorToolsExtended(Graph2D graph){
	    super(graph);
	  }
	
	/**
   * Highlight all given CompoundIDs in YELLOW color. And selects these nodes.
   * @param graph translated pathway with annotated geneIDs
   * @param compoundIDs compoundIDs (InChIKeys) to color in YELLOW.
   */
  public void highlightCompounds(Iterable<String> compoundIDs) {
    highlightCompounds(compoundIDs, Color.YELLOW, Color.LIGHT_GRAY, true);
  }
  
  public void highlightCompounds(Iterable<String> compoundIDs, Color highlightColor, Color forAllOthers, boolean changeSelection) {
    if (forAllOthers!=null) {
      setColorOfAllCompoundNodes(forAllOthers);
    }
    if (changeSelection) graph.unselectAll();
    Map<String, List<Node>> id2node = getInChIKey2NodeMap();
    for (String s : compoundIDs) {
      List<Node> nList = id2node.get(s);
      if (nList!=null) {
        for (Node node : nList) {
          graph.getRealizer(node).setFillColor(highlightColor);
          if (changeSelection) {
            graph.getRealizer(node).setSelected(true);
          }
        }
      } else {
        log.info("Could not get a Node for " + s);
      }
    }
  }
  
  /**
   * Return a map from InChIKey to corresponding {@link Node}.
   * @return map from InChIKey to List of nodes.
   */
  public Map<String, List<Node>> getInChIKey2NodeMap() {
    // If we want to map compounds we HAVE TO create our mapping manually first
    if (this.getMap(GraphMLmapsExtended.NODE_COMPOUND_ID)==null) {
      createNode2InChIKeymapping();
    }

    // Build a map from InChIKey 2 Node
    Map<String, List<Node>> id2node = new HashMap<String, List<Node>>();
    
    NodeMap inchi = (NodeMap) this.getMap(GraphMLmapsExtended.NODE_COMPOUND_ID);
    for (Node n : graph.getNodeArray()) {
      Object inchiIds = inchi.get(n);
      if (inchiIds!=null && inchiIds.toString().length()>0) {
        String[] ids = inchiIds.toString().split("\\|"); // space separated.
        for (String id: ids) {
          if (id==null || id.trim().length()<1) continue;
          try {
            // Get Node collection for InChIKey
            List<Node> list = id2node.get(id);
            if (list==null) {
              list = new LinkedList<Node>();
              id2node.put(id, list);
            }
            // Add node to list.
            list.add(n);
          } catch (NumberFormatException e) {
            log.log(Level.WARNING, "Could not get compoundID for node.", e);
          }
        }
      }
    }
    
    return id2node;
  }



  /**
   * This will initialize the {@link GraphMLmapsExtended#NODE_COMPOUND_ID} mapping.
   */
  public void createNode2InChIKeymapping() {
    KeggCompound2InChIKeyMapper map = IntegratorUITools.getKegg2InChIKeyMapping();
 
    // Assign a space-separated HMDB-id-string to each node
    for (Node n: graph.getNodeArray()) {
      Object KEGG_id = this.getInfo(n, GraphMLmaps.NODE_KEGG_ID);
      StringBuffer idString = new StringBuffer();
      if (KEGG_id!=null) {
        String[] ids = KEGG_id.toString().split("\\|"); // "|" separated.
        for (String id: ids) {
          if (id==null || id.trim().length()<1) continue;
          try {
          	 Set<String> inchikeys = map.map(id);
             if (inchikeys!=null) {
            	 for(String ikey: inchikeys){
            		 if (idString.length()>0) {
            			 idString.append('|');
            		 		}
            		 idString.append(ikey);
            	 }
            }
          } catch (Exception e) {
            log.log(Level.WARNING, "Could not get InChIKey for node.", e);
          }
        }
      }
      
      // Set identifiers
      if (idString.length()>0) {
        this.setInfo(n, GraphMLmapsExtended.NODE_COMPOUND_ID, idString.toString());
      }
    }
  }
  
  /**
   * Set a unique {@link Color} to all COMPOUND nodes, that are no pathway references. 
   * @param colorForUnaffectedNodes
   */
  public void setColorOfAllCompoundNodes(Color colorForUnaffectedNodes) {
    // Set unaffected color for all compound nodes.
    for (Node n: graph.getNodeArray()) {
      String id = getKeggIDs(n);
      String nodeType = getNodeType(n);
      if(!nodeType.equals("compound") && !nodeType.equals("small molecule")) continue;
      id = id==null?null:id.toLowerCase().trim();
      if (id!=null && id.startsWith("path:")) continue;
      NodeRealizer realizer = graph.getRealizer(n);
      
      // Only change BGcolor if nodes have a border (KEGG BRITE nodes have no border)
      if (realizer.getLineColor()!=null) {
        realizer.setFillColor(colorForUnaffectedNodes);
      }
    }
  }
  
  /**
  * @param n
  * @return node ids
  */
  public static String getNodeType(Node n){
  return getNodeInfoIDs(n, GraphMLmaps.NODE_TYPE);
  }
}

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
package de.zbit.io;

import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import y.base.DataMap;
import y.base.Node;
import y.base.NodeMap;
import y.io.GraphMLIOHandler;
import y.io.graphml.KeyScope;
import y.io.graphml.KeyType;
import y.view.Graph2D;
import y.view.NodeRealizer;
import y.view.hierarchy.HierarchyManager;
import de.zbit.graph.io.Graph2Dwriter;
import de.zbit.graph.io.def.GenericDataMap;
import de.zbit.graph.io.def.GraphMLmaps;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUITools;
import de.zbit.mapper.GeneSymbol2GeneIDMapper;
import de.zbit.util.NotifyingWorker;
import de.zbit.util.Species;
import de.zbit.util.StringUtil;

/**
 * A {@link SwingWorker} that handles imports of GraphML and GML files.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class GraphMLimporter extends NotifyingWorker<Graph2D> {
  public static final transient Logger log = Logger.getLogger(GraphMLimporter.class.getName());

  
  /**
   * The original input file.
   */
  private File graphmlFile;
  
  
  /**
   * Imports the given <code>graphmlFile</code>.
   * @param graphmlFile
   */
  public GraphMLimporter(File graphmlFile) {
    super();
    this.graphmlFile = graphmlFile;
  }
  

  /* (non-Javadoc)
   * @see javax.swing.SwingWorker#doInBackground()
   */
  @Override
  protected Graph2D doInBackground() throws Exception {
    try {
      // 1. Read the file
      publish(new ActionEvent(this, 3, null));
      log.info(String.format("Reading GraphML file '%s'...", graphmlFile.getName()));
      GraphMLIOHandler handler = new GraphMLIOHandler();
      
      Graph2D graph = new Graph2D();
      graph.setHierarchyManager(new HierarchyManager(graph));
      NodeMap entrezIds = graph.createNodeMap();
      handler.getGraphMLHandler().addInputDataAcceptor(GraphMLmaps.NODE_GENE_ID, entrezIds, KeyScope.NODE, KeyType.INT);
      handler.read(graph, new BufferedInputStream(new FileInputStream(graphmlFile)));
      if (graph.isEmpty()) {
        GUITools.showErrorMessage(null, "Could not read the model. Is it a valid GraphML file? Is the graph non-empty?");
        return null;
      }
      
      // 2. Get species (Ask user)
      Species spec = letUserSelectSpecies();
      
      // 3. Map stuff to entrez
      boolean containsEntrez = mapNodesToEntrez(graph, entrezIds, spec.getCommonName());
      GenericDataMap<DataMap, String> mapDescriptionMap = Graph2Dwriter.addMapDescriptionMapToGraph(graph);
      mapDescriptionMap.set(entrezIds, GraphMLmaps.NODE_GENE_ID);
      
      
      // 4. Ensure that we have entrez ids
      if (!containsEntrez) {
        showWarningThatNoEntrezIDsAreInThePathway();
      }
      
      
      // Reading done. Send some infos to the underlying listeners
      // Recommended ToolTip for this tab
      publish(new ActionEvent(this, 12, String.format("GraphML graph from file '%s'.", graphmlFile.getName())));
      
      // The species
      if (spec!=null) {
        publish(new ActionEvent(spec, 13, null));
      }
      
      publish(new ActionEvent(graph, 4, null));
      return graph;
      
    } catch (Exception e) {
      GUITools.showErrorMessage(null, e, "Could not import GraphML pathway.");
      publish(new ActionEvent(this, 2, null)); // Remove this tab
      publish(new ActionEvent(this, 5, null)); // Remove this from list of listeners
      return null;
    }
  }
  
  /**
   * Uses a {@link GeneSymbol2GeneIDMapper} to map the node labels
   * to entrez gene ids.
   * @param graph
   * @param entrezIds
   * @param organism @param organism in non-scientific format ("human", "mouse" or "rat").
   * @return {@code TRUE} if the graph cotains at least one entrez gene identifier.
   * @throws Exception 
   */
  public static boolean mapNodesToEntrez(Graph2D graph, NodeMap entrezIds, String organism) throws Exception {
    GeneSymbol2GeneIDMapper mapper = new GeneSymbol2GeneIDMapper(organism);
    boolean containsEntrez = false;
    
    for (Node n: graph.getNodeArray()) {
      // Does it already contain a mapping?
      if (entrezIds.get(n)!=null) {
        containsEntrez = true;
        continue;
      }
      
      // Try to map node labels to entrez
      NodeRealizer nr = graph.getRealizer(n);
      String l = nr==null?null:nr.getLabelText();
      
      if (l!=null && l.length()>0) {
        Integer entrez = mapper.map(l);
        if (entrez==null) entrez = mapper.map(StringUtil.firstLetterUpperCase(l));
        if (entrez==null) {
          // A little customized for MM graphs: "SYMBOL (SYMBOL, SYMBOL,....)".
          String[] split = l.split("[\\s,\\(\\)]");
          for (int i=0; i<split.length; i++) {
            split[i] = split[i].trim(); 
            if (split[i].length()<1) continue;
            entrez = mapper.map(split[i]);
            if (entrez==null) entrez = mapper.map(StringUtil.firstLetterUpperCase(split[i]));
            if (entrez!=null) break;
          }
        }
        if (entrez!=null) {
          entrezIds.set(n, entrez.toString());
          containsEntrez = true;
        }
      }
    }
    
    return containsEntrez;
  }
  
  /*
   * ALL FOLLOWING METHODS CAN BE OVERWRITTEN BY EXTENDING CLASSES.
   */

  /**
   * This is called if the pathway does not contain any entrez gene
   * ids and no entrez ids could be mapped from other ids.
   */
  protected void showWarningThatNoEntrezIDsAreInThePathway() {
    GUITools.showWarningMessage(null, "The GraphML file did not contain any labels that could be mapped to Entrez Gene identifiers.\nYou will not be able to visualize data within this graph.");
  }

  /**
   * Overwrite or implement this method to ask the
   * user for a species if none could be infered from
   * the selected file.
   * @return
   */
  protected Species letUserSelectSpecies() {
    return IntegratorUITools.showOrganismSelectorDialog(null);
  }

}

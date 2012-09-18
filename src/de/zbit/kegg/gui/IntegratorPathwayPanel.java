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
package de.zbit.kegg.gui;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

import y.base.DataMap;
import y.base.Edge;
import y.base.Node;
import y.base.NodeMap;
import y.view.Graph2D;
import y.view.HitInfo;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.VisualizedData;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.graph.io.Graph2Dwriter;
import de.zbit.graph.io.def.GenericDataMap;
import de.zbit.graph.io.def.GraphMLmaps;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.customcomponents.TableResultTableModel;
import de.zbit.gui.layout.LayoutHelper;
import de.zbit.gui.tabs.IntegratorChartTab;
import de.zbit.integrator.GraphMLmapsExtended;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.math.MathUtils;
import de.zbit.util.Species;
import de.zbit.util.StringUtil;
import de.zbit.util.TranslatorTools;

/**
 * The panel that is used to display pathways.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class IntegratorPathwayPanel extends TranslatorGraphPanel {
  private static final long serialVersionUID = -981908109792103420L;
  /**
   * {@link Species} for this panel.
   */
  private Species species=null;
  
  /**
   * 
   * @param pathwayID
   * @param format
   */
  public IntegratorPathwayPanel(String pathwayID, Format format, ActionListener translationResult) {
    super(pathwayID, format, translationResult);
    TranslatorPanelTools.setupBackgroundImage(this);
  }
  
  /**
   * @param pathwayID
   * @param translationResult
   */
  public IntegratorPathwayPanel(String pathwayID, ActionListener translationResult) {
    this (pathwayID, Format.JPG, translationResult);
  }
  
  /**
   * @param keggPathway
   * @param translationResult
   */
  public IntegratorPathwayPanel(Pathway keggPathway, ActionListener translationResult) {
    super(keggPathway, Format.JPG, translationResult);
    TranslatorPanelTools.setupBackgroundImage(this);
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#isDetailPanelAvailable()
   */
  @Override
  public boolean isDetailPanelAvailable() {
    return true;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.kegg.gui.TranslatorGraphLayerPanel#updateDetailPanel(javax.swing.JScrollPane, y.view.HitInfo)
   */
  @Override
  protected void updateDetailPanel(JScrollPane detailPanel, HitInfo clickedObjects) {
    
    if (clickedObjects==null || !clickedObjects.hasHits()) {
      synchronized (detailPanel) {
        ((JScrollPane) detailPanel).setViewportView(null);
      }
    } else {
      Set<Object> hits = TranslatorTools.getHitEdgesAndNodes(clickedObjects, true);
      JPanel p = new JPanel();
      LayoutHelper lh = new LayoutHelper(p);
      for (Object nodeOrEdge: hits) {
        if (Thread.currentThread().isInterrupted()) return;
        // Try to get actual SBML-element
        JComponent base = createDetailPanel(nodeOrEdge);
        
        // Add a detail panel if we have the element.
        if (base!=null) {
          lh.add(base);
        }
        
        // Add final panel
        if (Thread.currentThread().isInterrupted()) return;
        synchronized (detailPanel) {
          if (p.getComponentCount()>0) {
            
            ((JScrollPane) detailPanel).setViewportView(p);
            
            // Scroll to top.
            GUITools.scrollToTop(detailPanel);
          } else {
            // Remove an eventual loading bar
            ((JScrollPane) detailPanel).setViewportView(null);
          }
        }
      }
    }
  }


  /**
   * @param nodeOrEdge
   * @return
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private JComponent createDetailPanel(Object nodeOrEdge) {
    if (nodeOrEdge instanceof Edge) return null; // Disabled...
    Graph2D graph = getDocument();
    // Show a nice ToolTipText for every node.
    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(Graph2Dwriter.mapDescription);
    if (nodeOrEdge==null || mapDescriptionMap==null) return null;
    
    // Create a box plot of all visualized experimental data.
    Map<Class<? extends NameAndSignals>, List<Number>> experimentalData = new HashMap<Class<? extends NameAndSignals>, List<Number>>();
    StringBuilder boxPlotYaxisLabel = new StringBuilder();
    
    // Get nodeLabel, description and eventually an image for the ToolTipText
    int numberOfGenesInNode = 1;
    String nodeLabel = null;
    String description = null;
    String image = "";
    JComponent additional = new JPanel();
    LayoutHelper lh = new LayoutHelper(additional);
    NodeMap[] nm;
    try {
      nm = graph.getRegisteredNodeMaps();
    } catch  (Exception e) {
      log.log(Level.FINE, "Could not get node maps.", e);
      return null;
    }
    if (nm!=null) {
      for (int i=0; i<nm.length;i++) {
        if (Thread.currentThread().isInterrupted()) return null;
        Object c = nm[i].get(nodeOrEdge);
        if (c==null || c.toString().length()<1) continue;
        String mapDescription = mapDescriptionMap.getV(nm[i]);
        if (mapDescription==null) continue;
        
        // Get Node label, description and pictures
        if (mapDescription.equals(GraphMLmaps.NODE_LABEL)) {
          if (nodeOrEdge instanceof Node && TranslatorTools.isPathwayReference((Node)nodeOrEdge)) {
            // Not multiples, just a reference
          } else if (c.toString().contains(", ")) { // the space is important!
            // Multiple genes in one node
            String[] splitt = c.toString().split(", ");
            numberOfGenesInNode = splitt.length;
            StringBuilder sb = new StringBuilder();
            for (String s: splitt) {
              formatGeneSynonyms(sb, s);
            }
            c = sb.toString();
          } else {
            // Single gene
            c = formatGeneSynonyms(new StringBuilder(), c.toString()).toString();
          }
          nodeLabel = "<font size=\"4\">"+c.toString()+"</font><br/>"; // .replace(",", ",<br/>")
          
        } else if (mapDescription.equals(GraphMLmaps.NODE_NAME) && nodeLabel==null) {
          nodeLabel = c.toString();
            
        } else if (mapDescription.equals(GraphMLmaps.EDGE_TYPE)) {
          nodeLabel = "<b><h2>asd"+c.toString().replace(",", ",<br/>")+"</h2></b><br/>";
        } else if (mapDescription.equals(GraphMLmaps.NODE_DESCRIPTION)) {
          // Nice idea, unfortunately descriptions come before names...
//          String[] descriptions = c.toString().split(",");
//          if (numberOfGenesInNode>1 && descriptions.length==numberOfGenesInNode) {
//            // Show each description below the name
//            nodeLabel = appendDescriptions(nodeLabel, descriptions);
//          } else {
            description = "<i><font size=\"-1\">"+c.toString().replace(",", ",<br/>")+"</font></i><br/>";
//          }
          
        } else if (mapDescription.equals(GraphMLmaps.NODE_KEGG_ID)) {
          for (String s: c.toString().split(",")) {
            s=s.toUpperCase().trim();
            if (s.startsWith("PATH:")) {
              image+=Pathway.getPathwayPreviewPicture(s);
            } else if (s.startsWith("CPD:")) {
              image+=Pathway.getCompoundPreviewPicture(s);
            }
          }
          
          // Integrator-novel options
        } else if (mapDescription.equals(GraphMLmapsExtended.NODE_VISUALIZED_RAW_NS)) {
          //DataMap rawNsMap = (DataMap) c;
          Map<VisualizedData, Collection<?>> data = (Map<VisualizedData, Collection<?>>) c;//((DataMap)c).get(nodeOrEdge);
          if (data!=null) {
            for (Map.Entry<VisualizedData,Collection<?>> e : data.entrySet()) {
              Collection<?> ns = e.getValue();
              VisualizedData vd = e.getKey();
              if (ns!=null && ns.size()>0) {
                // Get Signals, if 1 show, else summary.
                List<Signal> signals = NameAndSignals.getSignals(
                  (Collection<? extends NameAndSignals>)ns, vd.getExperimentName(), vd.getSigType());
                if (signals!=null && signals.size()>0) {
                  List<Number> signalNumbers = Signal.toNumberList(signals);
                  // Well... we put pValues and fold-changes all in one list... maybe not that good!
                  experimentalData.put(vd.getNsType(), signalNumbers);
                  String niceSigName = vd.getNiceSignalName();
                  if (boxPlotYaxisLabel.indexOf(niceSigName)<0) {
                    if (boxPlotYaxisLabel.length()>0) boxPlotYaxisLabel.append("\n");
                    boxPlotYaxisLabel.append(niceSigName);
                  }
                  
                  JLabel label = new JLabel(String.format("<html><body><b><h3>%s:</h3></b><font size=\"-1\">%s</font></body></html>",
                    vd.toNiceString(), (signals.size()==1?signals.get(0).toNiceString():MathUtils.summary(signalNumbers, 2)) ));
                  label.setHorizontalAlignment(SwingConstants.LEFT);
                  lh.addSpacer();
                  lh.add(label);
                  
                  // show probe-based table
                  JTable table;
                  if (ns instanceof List) {
                    table = TableResultTableModel.buildJTable(new TableResultTableModel((List) ns, false));
                  } else {
                    table = TableResultTableModel.buildJTable(new TableResultTableModel(new ArrayList(ns), false));
                  }
                  lh.add(new JScrollPane(table));
                  
                  // show graph
                  if (DNAmethylation.class.isAssignableFrom(vd.getNsType())) {
                    GeneID2GeneSymbolMapper mapper = null;
                    if (species!=null) {
                      mapper = IntegratorUITools.get2GeneSymbolMapping(species);
                    }
                    
                    // Create one graph for every gene in this node
                    Map<String, Collection<NameAndSignals>> geneGroupedList = NameAndSignals.group_by_name((Collection<NameAndSignals>)ns, false, false);
                    for (java.util.Map.Entry<String, Collection<NameAndSignals>> entry : geneGroupedList.entrySet()) {
                      
                      // Try to get a nice name
                      String gene = entry.getKey();
                      if (mapper!=null) {
                        try {
                          int geneID = Integer.parseInt(gene);
                          if (geneID>0) {
                            gene = mapper.map(geneID);
                          }
                        } catch (Exception e1 ){};
                      }
                      String seriesName = String.format("Gene \"%s\".", gene);
                      
                      // Create plot
                      final ChartPanel chartPanel = new ChartPanel(IntegratorChartTab.createChart(seriesName,
                        entry.getValue(), vd.getSignalAndType(), IntegratorChartTab.INCLUDE_OTHER_SERIES_WITH_LIGHT_COLORS, species));
                      
                      // Add to panel
                      chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
                      lh.add(chartPanel);
                    }
                  }
                  
                }
              }
            }
          }
          
//        } else if (mapDescription.startsWith("[")) {
//          // I know, it is a dirty solution, but it allows other applications
//          // that use KEGGtranslator to include something into the tooltip.
//          if (additional.length()>0) additional.append("<br/>");
//          additional.append(String.format(
//            "<p><b>%s:</b><br/>%s</p>",
//            mapDescription, c.toString().replace("\n", "<br/>")));
        }
      }
    }
    
    // Merge the three strings to a single tooltip
    StringBuffer tooltip = new StringBuffer();
    if (numberOfGenesInNode>1) {
      tooltip.append("<h2>Node consists of " + numberOfGenesInNode + " elements:</h2>");
    } else {
      // XXX: Change element to real "thing", based on KGID and isMiRNA.
      tooltip.append("<h2>Node consists of one element:</h2>");
    }
    if (nodeLabel!=null) {
      tooltip.append(nodeLabel);
    }
    if (description!=null) {
      tooltip.append(StringUtil.insertLineBreaks(description, StringUtil.TOOLTIP_LINE_LENGTH*3, "<br/>"));
    }
    if (image!=null && image.length()>0) {
      tooltip.append("<div align=\"center\">"+image+"</div>");
    }
//    if (additional!=null && additional.length()>0) {
//      tooltip.append("<p>&nbsp;</p>");
//      tooltip.append(StringUtil.insertLineBreaks(additional.toString(), StringUtil.TOOLTIP_LINE_LENGTH, "<br/>"));
//    }
    
    

    
    // Append html and return toString.
    JPanel p = new JPanel();
    LayoutHelper ph = new LayoutHelper(p);
    JLabel label = new JLabel(String.format("<html><body>%s</body></html>", tooltip.toString()));
    ph.add(label);
    
    // Create summary box plots
    if (experimentalData!=null && experimentalData.size()>0) {
      //DefaultBoxAndWhiskerXYDataset dataset = new DefaultBoxAndWhiskerXYDataset("Microarray data assigned to this node");
      DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
      boolean includePlot = false;
      for (Entry<Class<? extends NameAndSignals>, List<Number>> exp : experimentalData.entrySet()) {
        String dataLabel = IntegratorUI.getShortTypeNameForNS(exp.getKey());
        if (dataLabel==null) dataLabel = exp.getKey().getSimpleName();
        dataset.add(exp.getValue(), dataLabel, dataLabel);
        includePlot |= exp.getValue().size()>1;
      }
      includePlot |= experimentalData.size()>1;

      if (includePlot) {
        CategoryAxis xAxis = new CategoryAxis("Type");
        NumberAxis yAxis = new NumberAxis(boxPlotYaxisLabel.toString());
        yAxis.setAutoRange(true);
        yAxis.setAutoRangeIncludesZero(false);

        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        renderer.setMeanVisible(false);
        CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
        
        final ChartPanel chartPanel = new ChartPanel(new JFreeChart(plot));
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 300));

      
        ph.add(new JLabel("<html><body><b><h3>Microarray data assigned to this node:</h3></b></body></html>"));
        ph.add(chartPanel);
      }
    }
    
    if (additional!=null && additional.getComponentCount()>0) {
      ph.add(additional);
    }
    
    // return centered p
    JPanel centered = new JPanel();
    centered.add(p);
    return centered;
  }

  /**
   * @param sb
   * @param s
   * @return
   */
  protected StringBuilder formatGeneSynonyms(StringBuilder sb, String s) {
    s = s.trim();
    int pos = s.indexOf(' ');
    if (sb.length()>0) sb.append("<br/>");
    if (pos<=0) {
      sb.append("<b>");
      sb.append(s);
      sb.append("</b>");
    } else {
      sb.append("<b>");
      sb.append(s.substring(0, pos));
      sb.append("</b> (other names: ");
      sb.append(s.substring(pos+1).replace(" ", ", "));
      sb.append(")");
    }
    return sb;
  }
  
  /**
   * @param nodeLabel String formatted by {@link #createDetailPanel(Object)}
   * @param descriptions description strings with exactly the same length
   * as if one would split nodeLable by "&ltbr/>".
   * @return one string containing both
   */
  @SuppressWarnings("unused")
  private String appendDescriptions(String nodeLabel, String[] descriptions) {
    StringBuilder newLabel = new StringBuilder();
    int pos = -1;
    int lastPos = 0;
    int i=0;
    while ((pos=nodeLabel.indexOf("<br/>", ++pos))>=0) {
      newLabel.append(nodeLabel.substring(lastPos, pos));
      newLabel.append("<br/>");
      newLabel.append("<i><font size=\"-1\">");
      newLabel.append(descriptions[i++]);
      newLabel.append("</font></i>");
      
      lastPos = pos;
    }
    newLabel.append(nodeLabel.substring(lastPos));
    
    return newLabel.toString();
  }
  
  /**
   * 
   * @param species
   */
  public void setSpecies(Species species) {
    this.species = species;
  }

  /**
   * @return
   */
  public Species getSpecies() {
    return species;
  }
  
}

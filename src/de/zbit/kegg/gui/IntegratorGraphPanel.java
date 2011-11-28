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
package de.zbit.kegg.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Stroke;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.ui.Layer;

import y.base.DataMap;
import y.base.Edge;
import y.base.NodeMap;
import y.view.Graph2D;
import y.view.HitInfo;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.VisualizedData;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.LayoutHelper;
import de.zbit.gui.customcomponents.TableResultTableModel;
import de.zbit.integrator.GraphMLmapsExtended;
import de.zbit.kegg.ext.GenericDataMap;
import de.zbit.kegg.ext.GraphMLmaps;
import de.zbit.kegg.io.KEGG2jSBML;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.parser.Species;
import de.zbit.util.StringUtil;
import de.zbit.util.TranslatorTools;
import de.zbit.util.Utils;
import de.zbit.util.ValuePair;

/**
 * The panel that is used to display pathways.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class IntegratorGraphPanel extends TranslatorGraphPanel {
  private static final long serialVersionUID = -981908109792103420L;
  /**
   * {@link Species} for this panel.
   */
  private Species species=null;
  
  /**
   * @param pathwayID
   * @param translationResult
   */
  public IntegratorGraphPanel(String pathwayID, ActionListener translationResult) {
    super(pathwayID, Format.JPG, translationResult);
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
      }
      
      // Add final panel
      if (Thread.currentThread().isInterrupted()) return;
      if (p.getComponentCount()>0) {
        synchronized (detailPanel) {
          ((JScrollPane) detailPanel).setViewportView(p);

          // Scroll to top.
          GUITools.scrollToTop(detailPanel);
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
    GenericDataMap<DataMap, String> mapDescriptionMap = (GenericDataMap<DataMap, String>) graph.getDataProvider(KEGG2yGraph.mapDescription);
    if (nodeOrEdge==null || mapDescriptionMap==null) return null;
    
    // Get nodeLabel, description and eventually an image for the ToolTipText
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
          nodeLabel = "<b><h2>"+c.toString().replace(",", ",<br/>")+"</h2></b><br/>";
        } else if (mapDescription.equals(GraphMLmaps.EDGE_TYPE)) {
          nodeLabel = "<b><h2>asd"+c.toString().replace(",", ",<br/>")+"</h2></b><br/>";
        } else if (mapDescription.equals(GraphMLmaps.NODE_DESCRIPTION)) {
          description = "<i><font size=\"-1\">"+c.toString().replace(",", ",<br/>")+"</font></i><br/>";
        } else if (mapDescription.equals(GraphMLmaps.NODE_KEGG_ID)) {
          for (String s: c.toString().split(",")) {
            s=s.toUpperCase().trim();
            if (s.startsWith("PATH:")) {
              image+=KEGG2jSBML.getPathwayPreviewPicture(s);
            } else if (s.startsWith("CPD:")) {
              image+=KEGG2jSBML.getCompoundPreviewPicture(s);
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
                List<Signal> signals = NameAndSignals.getSignals((Collection<? extends NameAndSignals>)ns, vd.getExperimentName(), vd.getSigType());
                if (signals!=null && signals.size()>0) {
                  JLabel label = new JLabel(String.format("<html><body><b><h3>%s:</h3></b><font size=\"-1\">%s</font></body></html>",
                    vd.toNiceString(), (signals.size()==1?signals.get(0).toNiceString():Utils.summary(Signal.toNumberList(signals), 2)) ));
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
                    
                    // Create marker at 0 or 0.05
                    double makerPosition = 0;
                    if (vd.getSigType().equals(SignalType.pValue) || vd.getSigType().equals(SignalType.qValue)) {
                      makerPosition = 0.05;
                    }
                    Marker zeroMarker = new ValueMarker(makerPosition,Color.LIGHT_GRAY, new BasicStroke(3f, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {10.0f, 6.0f}, 0.0f));
                    
                    //XYSplineRenderer graph = new XYSplineRenderer();
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
                      String seriesName = String.format("Gene \"%s\" (%s).", gene, vd.getNiceSignalName());
                      
                      // Create data series
                      DefaultXYDataset dataset = new DefaultXYDataset();
                      double[][] XYdata = getXYdata(entry.getValue(), vd);
                      dataset.addSeries(seriesName, XYdata);
                      
                      final NumberAxis signalValueAxis = new NumberAxis(vd.getNiceSignalName());
                      //domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
                      final NumberAxis xAxis = new NumberAxis("Location");
                      xAxis.setAutoRange(true);
                      xAxis.setAutoRangeIncludesZero(false);
                      final XYSplineRenderer renderer = new XYSplineRenderer();
                      
                      final XYPlot plot = new XYPlot(dataset, xAxis, signalValueAxis, renderer);
                      
                      // Add line at 0
                      plot.addRangeMarker(zeroMarker, Layer.BACKGROUND);
                      
                      final ChartPanel chartPanel = new ChartPanel(new JFreeChart(plot));
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
    if (nodeLabel!=null) {
      tooltip.append(nodeLabel);
    }
    if (description!=null) {
      tooltip.append(StringUtil.insertLineBreaks(description, GUITools.TOOLTIP_LINE_LENGTH*3, "<br/>"));
    }
    if (image!=null && image.length()>0) {
      tooltip.append("<div align=\"center\">"+image+"</div>");
    }
//    if (additional!=null && additional.length()>0) {
//      tooltip.append("<p>&nbsp;</p>");
//      tooltip.append(StringUtil.insertLineBreaks(additional.toString(), GUITools.TOOLTIP_LINE_LENGTH, "<br/>"));
//    }
    
    // Append html and return toString.
    JPanel p = new JPanel();
    LayoutHelper ph = new LayoutHelper(p);
    JLabel label = new JLabel(String.format("<html><body>%s</body></html>", tooltip.toString()));
    //label.setHorizontalAlignment(SwingConstants.LEFT);
    ph.add(label);
    if (additional!=null && additional.getComponentCount()>0) {
      ph.add(additional);
    }
    
    // return centered p
    JPanel centered = new JPanel();
    centered.add(p);
    return centered;
  }


  /**
   * @param ns
   * @param vd
   * @return
   */
  private double[][] getXYdata(Collection<?> ns, VisualizedData vd) {
    double[][] XYdata = new double[2][];
    XYdata[0] = new double[ns.size()];
    XYdata[1] = new double[ns.size()];
    
    // Get Location and signal
    Iterator<?> it = ns.iterator();
    int i=-1;
    List<ValuePair<Double, Double>> xy = new ArrayList<ValuePair<Double,Double>>(ns.size());
    while (it.hasNext()) {
      i++;
      double x = i;
      NameAndSignals n = (NameAndSignals) it.next();
      if (n instanceof DNAmethylation) {
        Integer start = ((DNAmethylation) n).getProbeStart();
        Integer end = ((DNAmethylation) n).getProbeStart();
        if (start!=null && start.intValue()>0 &&
            end!=null && end.intValue()>0) {
          x = start+end/2; // build mean
        } else if (start!=null && start.intValue()>0) {
          x = start;
        } else if (end!=null && end.intValue()>0) {
          x = end;
        }
      }
      
      double y = n.getSignalMergedValue(vd.getSigType(), vd.getExperimentName(), MergeType.Mean);
      
      xy.add(new ValuePair<Double, Double>(x, y));
    }
    
    // Sort by location
    if (i>0) {
      Collections.sort(xy, xy.iterator().next().getComparator_OnlyCompareA());
    }
    
    // Write to array
    i=-1;
    for (ValuePair<Double, Double> vp : xy) {
      i++;
      XYdata[0][i] = vp.getA();
      XYdata[1][i] = vp.getB();
    }
    return XYdata;
  }
  
  /**
   * 
   * @param species
   */
  public void setSpecies(Species species) {
    this.species = species;
  }
  
}

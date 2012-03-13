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

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.Range;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.xy.DefaultIntervalXYDataset;
import org.jfree.data.xy.XYDataset;

import de.zbit.data.GeneID;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.genes.GenericGene;
import de.zbit.data.miRNA.miRNA;
import de.zbit.graph.SymbolAxisWithArbitraryStart;
import de.zbit.graph.XYGenesBarRenderer;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.dialogs.VisualizeDataInPathwayDialog;
import de.zbit.gui.prefs.PathwayVisualizationOptions;
import de.zbit.gui.prefs.SignalOptions;
import de.zbit.gui.tabs.IntegratorChartTab;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.sequence.region.Region;
import de.zbit.util.Species;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.utils.SignalColor;

/**
 * Provides methods to add a separate view, showing
 * coding regions (genes) in a {@link Region}-based
 * {@link JFreeChart}. Used, e.g. in {@link IntegratorChartTab}.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class VisualizeGenesInChartTab {
  public static final transient Logger log = Logger.getLogger(VisualizeGenesInChartTab.class.getName());


  /**
   * Adds a panel showing codining regions (genes) to an existing {@link XYPlot}.
   * <p>Starts a new thread, i.e. will return before finished!
   * @param chart a Chart, containing an {@link XYPlot}. Will throw exceptions for other plot types!
   * @param target target genomic region, shown in the given plot
   * @param species
   * @return a NEW INSTANCE of a {@link JFreeChart} containing now also the additional gene panel.
   */
  public static JFreeChart addGenes(JFreeChart chart, final Region target, final Species species) {
    // Allow adding a second plot with combined x-xis
    CombinedDomainXYPlot localCombinedDomainXYPlot = new CombinedDomainXYPlot(chart.getXYPlot().getDomainAxis());
    localCombinedDomainXYPlot.setGap(0);
    localCombinedDomainXYPlot.add(chart.getXYPlot(),85);
    
    JFreeChart jfc = new JFreeChart(localCombinedDomainXYPlot);
    jfc.setTitle(chart.getTitle());

    new Thread(addGenes(localCombinedDomainXYPlot, target, species)).start();
    return jfc;
  }
  
  /**
   * Creates a {@link Runnable} that adds a panel showing
   * codining regions (genes) to an existing {@link CombinedDomainXYPlot}.
   * 
   * @param plot
   * @param target target genomic region, shown in the given plot
   * @param species
   * @return a (not yet started) runnable
   */
  public static Runnable addGenes(final CombinedDomainXYPlot plot, final Region target, final Species species) {
    Runnable r = new Runnable() {
      @Override
      public void run() {
        XYPlot other = (XYPlot) plot.getSubplots().get(0);
        
        List<GenericGene> genesInRegion;
        try {
          genesInRegion = GenericGene.getAllGenesForRegion(target, species);
        } catch (Exception e) {
          log.log(Level.SEVERE, "Could not add gene-view.", e);
          return;
        }
        if (Thread.currentThread().isInterrupted() || genesInRegion==null) return;
        
        // Domain is X, Range is Y
        double[][] genesAsArray = new double[6][genesInRegion.size()];
        
        // Convert formatting and add series
        DefaultIntervalXYDataset genes = new DefaultIntervalXYDataset();
        for (int i=0; i<genesInRegion.size(); i++) {
          GenericGene g = genesInRegion.get(i);
          genesAsArray[0][i] = g.getStart();
          genesAsArray[1][i] = g.getStart();
          genesAsArray[2][i] = g.getEnd();
          if (g.isOnForwardStrand()) {
            genesAsArray[3][i] = 2;
            genesAsArray[4][i] = 0;
            genesAsArray[5][i] = genesAsArray[3][i];
          } else {
            genesAsArray[3][i] = -2;
            genesAsArray[4][i] = 0;
            genesAsArray[5][i] = genesAsArray[3][i];
          }          
        }
        genes.addSeries(genesInRegion.size()==1?
            String.format("Gene: \"%s\"", genesInRegion.get(0).getName()):"Genes", genesAsArray);
        
        // Configure Axes
        XYGenesBarRenderer localXYBarRenderer = new XYGenesBarRenderer(genesInRegion);
        if (genesInRegion.size()!=1) {
          localXYBarRenderer.setSeriesVisibleInLegend(0, false);
        }
        localXYBarRenderer.setUseYInterval(true);
        
        ValueAxis xAxis = other.getDomainAxis();
        ValueAxis xxAxis = new NumberAxis(xAxis.getLabel());
        xxAxis.setAutoRange(false);
        xxAxis.setRange(xAxis.getRange());

        SymbolAxis yyAxis = new SymbolAxisWithArbitraryStart("Strand", new String[] {"Reverse", "", "Forward"},-1);
        yyAxis.setGridBandsVisible(false);
        yyAxis.setTickMarksVisible(false);
        Range yTarget = new Range(-2,2);
        yyAxis.setAutoRange(false);
        yyAxis.setRangeWithMargins(yTarget);
        yyAxis.setDefaultAutoRange(yTarget);
        yyAxis.setFixedAutoRange(yTarget.getLength());
        yyAxis.setLowerMargin(yyAxis.getUpperMargin());
        //---
        
        XYPlot geneBoxesPlot = new XYPlot(genes,(ValueAxis) xxAxis, yyAxis, localXYBarRenderer);
        geneBoxesPlot.setRangeGridlinesVisible(false);
        geneBoxesPlot.addDomainMarker(new ValueMarker(0,Color.BLACK, new BasicStroke(3f)));
        
        // Center X-Axis on meth-data, not on genes (only if genes available)
        xAxis.setAutoRange(false);
        XYDataset dataset = other.getDataset();
        Range targetRange = new Range(dataset.getXValue(0, 0), dataset.getXValue(0, dataset.getItemCount(0)-1));
        if (targetRange.getLength()==0) targetRange = new Range(dataset.getXValue(0, 0)-.5d, dataset.getXValue(0, 0)+.5d ); 
        xAxis.setRangeWithMargins(targetRange);
        xAxis.setDefaultAutoRange(targetRange);
        //xAxis.setFixedAutoRange(targetRange.getLength());
        
        
        //JFreeChart jfc = new JFreeChart(localCombinedDomainXYPlot);
        plot.add(geneBoxesPlot,15);
        //----
      }
    
    };
    return r;
  }

  /**
   * @param chart {@link JFreeChart}, containing genes to color
   * @param dataSource to take from the given {@link NameAndSignalsTab}
   * @param experimentName name of the observation to color
   * @param signalType signal type of the observation (usually fold change)
   */
  public synchronized static void visualizeData(final IntegratorChartTab chart, final NameAndSignalsTab dataSource, final String experimentName, final SignalType signalType) {

    // Ask the user to set all required options
    SBPreferences prefs = SBPreferences.getPreferencesFor(SignalOptions.class);
    if (!SignalOptions.REMEMBER_GENE_CENTER_DECISION.getValue(prefs)) {
      if (!VisualizeDataInPathwayDialog.showDialog(new VisualizeDataInPathwayDialog(), "Visualize data in pathway")) return;// 0;
      // (All options are automatically processed in the VisualizeData method)
    }
    
    // Ensure that graph is available
    if (!chart.containsGenesView()) { // TODO: Check genes series length >0
      GUITools.showErrorMessage(null, "There are no genes in your graph.");
      return;
    }
    
    // Perform operations in another thread
    SwingWorker<Integer, Void> visData = new SwingWorker<Integer, Void>() {
      @Override
      protected Integer doInBackground() throws Exception {
          try {
            int coloredNodes=0;
            synchronized (chart.getChartPanel()) {              
              coloredNodes = visualizeDataNow(chart, dataSource, experimentName,signalType);
              
              // Some very dirty coded stuff, though a try and catch...
              try {
                XYPlot genesPlot = ((XYPlot)((CombinedDomainXYPlot)chart.getData().getXYPlot()).getSubplots().get(1));
                
                // Force to repaint the series
                ((DefaultIntervalXYDataset)genesPlot.getDataset()).seriesChanged(new SeriesChangeEvent(chart));
                
                // TODO: Update legend (color) and remove gradient from series paint
                // This call us not working.
                ((XYBarRenderer)genesPlot.getRenderer()).setGradientPaintTransformer(null);
              } catch (Throwable t) {}
            }
            return coloredNodes;
          } catch (Exception e) {
            throw e;
          }
          //return -1;
        
      }
      
      @Override
      protected void done() {
        try {
          // Check for execution errors
          int coloredNodes = get();
          if (coloredNodes<=0) {
            GUITools.showMessage("Could not match any gene to your data.", IntegratorUI.appName);
          }
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
          e.printStackTrace();
          GUITools.showErrorMessage(null, e);
        } finally {
          synchronized (chart) {
            chart.repaint();
            chart.getChartPanel().repaint();
            IntegratorUI.getInstance().updateButtons();
          }
        }
      }
    };
    visData.execute();
    
    return;
  }

  /**
   * This method should not be called directly, but rather {@link #visualizeData(IntegratorChartTab, NameAndSignalsTab, String, SignalType)
   * should be used.
   * @param dataSource
   * @param experimentName
   * @param signalType
   * @return
   */
  protected static int visualizeDataNow(final IntegratorChartTab chart, NameAndSignalsTab dataSource, String experimentName, SignalType signalType) {
    // Get XYGenesBarRenderer
    Plot combplot = chart.getData().getPlot();
    if (!(combplot instanceof CombinedDomainXYPlot) || ((CombinedDomainXYPlot) combplot).getSubplots().size()<2) {
      GUITools.showErrorMessage(null, "Could not color genes without special genes-plot.");
      return -1;
    }
    XYPlot genesPlot = (XYPlot) ((CombinedDomainXYPlot) combplot).getSubplots().get(1);
    
    if (!(genesPlot.getRenderer() instanceof XYGenesBarRenderer)) {
      GUITools.showErrorMessage(null, "Could not color genes without special gene renderer.");
      return -1;
    }
    XYGenesBarRenderer rend = (XYGenesBarRenderer) genesPlot.getRenderer();
    
    // Create a lookup-table
    Map<Integer, GenericGene> geneIDLookUp = new HashMap<Integer, GenericGene>();
    for (GenericGene g: rend.getGenesInRegion()) {
      geneIDLookUp.put(g.getGeneID(), g);
    }
    
    // Now match all NS to its gene
    Map<GenericGene, List<NameAndSignals>> gene2Ns = new HashMap<GenericGene, List<NameAndSignals>>();
    for (NameAndSignals ns : dataSource.getData()) {
      if (ns instanceof GeneID) {
        // we also WANT miRNA here to map to its geneID, not the target!
        int gId = ((GeneID) ns).getGeneID();
        if (gId>0) {
          GenericGene match = geneIDLookUp.get(gId);
          if (match!=null) {
            miRNA.addToList(gene2Ns, match, ns);
          }
        }
      }
    }
    
    // Convert signals to colors
    Set<GenericGene> recoloredGenes = new HashSet<GenericGene>();
    if (gene2Ns.size()>0) {
      // init recolor and get preferences
      SignalColor recolorer = new SignalColor(dataSource.getData(), experimentName, signalType);
      
      SBPreferences prefs = SBPreferences.getPreferencesFor(PathwayVisualizationOptions.class);
      MergeType sigMerge = IntegratorUITools.getMergeTypeSilent(signalType);
      Color forNothing = PathwayVisualizationOptions.COLOR_FOR_NO_FOLD_CHANGE.getValue(prefs);
      Float ignoreFC = PathwayVisualizationOptions.DONT_VISUALIZE_FOLD_CHANGES.getValue(prefs);
      Double ignorePV = PathwayVisualizationOptions.DONT_VISUALIZE_P_VALUES.getValue(prefs);
      if (ignorePV==null||Double.isNaN(ignorePV.doubleValue())) ignorePV=1d;
      if (ignoreFC==null||Double.isNaN(ignoreFC.doubleValue())) ignoreFC=0f;

      
      for (Entry<GenericGene, List<NameAndSignals>> e : gene2Ns.entrySet()) {
        // Get one signal value
        Signal sig = Signal.mergeSignal(NameAndSignals.getSignals(e.getValue(), experimentName, signalType), sigMerge, experimentName, signalType);
        if (sig==null) continue;
        double signalValue = sig.getSignal().doubleValue();
        //---
        
        //double signalValue = ns.getSignalMergedValue(type, experimentName, sigMerge);
        if (Double.isNaN(signalValue)) continue;
        Color newColor;
        if (!VisualizeDataInPathway.considerSignal(signalValue, signalType, ignoreFC, ignorePV)) {
          newColor = forNothing;
        } else {
          newColor = recolorer.getColor(signalValue);
        }
        
        // Recolor node and remember to don't gray it out.
        e.getKey().addData(XYGenesBarRenderer.COLOR_KEY, newColor);
        recoloredGenes.add(e.getKey());
      }
    }
    
    // Reset others
    for (GenericGene g: rend.getGenesInRegion()) {
      if (!recoloredGenes.contains(g)) {
        g.removeData(XYGenesBarRenderer.COLOR_KEY);
      }
    }
    
    return recoloredGenes.size();
  }

  
  
  
}

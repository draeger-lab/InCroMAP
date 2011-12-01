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
package de.zbit.gui.tabs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.ui.Layer;

import de.zbit.data.GeneID;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.VisualizedData;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.gui.BaseFrame.BaseAction;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.LayoutHelper;
import de.zbit.io.SBFileFilter;
import de.zbit.parser.Species;
import de.zbit.sequence.region.BasicRegion;
import de.zbit.sequence.region.Chromosome;
import de.zbit.sequence.region.Region;
import de.zbit.util.ValuePair;

/**
 * Region-based plot of {@link NameAndSignals} data, implementing the
 * {@link Region} interface.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class  IntegratorChartTab extends IntegratorTab<JFreeChart> {
  private static final long serialVersionUID = 4674576376048996302L;

  /**
   * A set of very light colors
   */
  private static Color[] veryLightColors = new Color[]{new Color(220,220,255), 
    new Color(220,255,220), new Color(220,255,255),
    new Color(255,220,255), new Color(255,255,220), new Color(255,220,220)};
  
  /**
   * Flag for including also other series with light colors.
   */
  public final static byte INCLUDE_OTHER_SERIES_WITH_LIGHT_COLORS = 1;
  
  /**
   * Flag for including also other series with normal colors.
   */
  public final static byte INCLUDE_OTHER_SERIES = 2;
  
  /**
   * Do not include other series.
   */
  public final static byte DO_NOT_INCLUDE_OTHER_SERIES = 0;
  
  /**
   * @param parent
   * @param data
   * @param species
   */
  public IntegratorChartTab(IntegratorUI parent, JFreeChart data, Species species) {
    super(parent, data, species);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrameTab#saveToFile()
   */
  @Override
  public File saveToFile() {
    final File f = GUITools.showSaveFileChooser(this, IntegratorUI.saveDir, 
      SBFileFilter.createJPEGFileFilter(), SBFileFilter.createPNGFileFilter());
    if (f==null) return null;
    
    final int width = 1000;
    final int height= 500;
    Runnable r = new Runnable() {
      @Override
      public void run() {
        try {
          //CSVwriteableIO.write(getData(), f.getAbsolutePath());
          if (f.getName().toLowerCase().endsWith("png")) {
            ChartUtilities.saveChartAsPNG(f, data, width, height);
          } else {
            ChartUtilities.saveChartAsJPEG(f, data, width, height);
          }
          GUITools.showMessage("Saved chart successfully to \"" + f.getPath() + "\".", IntegratorUI.appName);
        } catch (Throwable e) {
          GUITools.showErrorMessage(IntegratorUI.getInstance(), e);
        }
      }
    };
    IntegratorUITools.runInSwingWorker(r);
    
    // Unfortunately we can not check anymore wether it failed or succeeded.
    return f;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrameTab#updateButtons(javax.swing.JMenuBar, javax.swing.JToolBar)
   */
  @Override
  public void updateButtons(JMenuBar menuBar, JToolBar toolbar) {
    // Update the toolbar.
    if (toolbar!=null) {
      createJToolBarItems(toolbar);
    }
    
    // Enable and disable items
    if (isReady()) {
      GUITools.setEnabled(true, menuBar, BaseAction.FILE_SAVE, BaseAction.FILE_CLOSE);
    } else {
      GUITools.setEnabled(false, menuBar, BaseAction.FILE_SAVE, BaseAction.FILE_CLOSE);
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.tabs.IntegratorTab#createJToolBarItems(javax.swing.JToolBar)
   */
  @Override
  public void createJToolBarItems(JToolBar bar) {
    // TODO Add genes / color gene markers / Save ....
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.tabs.IntegratorTab#getVisualization()
   */
  @Override
  public JComponent getVisualization() {
    final ChartPanel chartPanel = new ChartPanel(data);
    chartPanel.setMinimumSize(new java.awt.Dimension(640, 640/2));
    return chartPanel;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.tabs.IntegratorTab#getObjectAt(int)
   */
  @Override
  public Object getObjectAt(int i) {
    return null;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.tabs.IntegratorTab#getSelectedIndices()
   */
  @Override
  public int[] getSelectedIndices() {
    return null;
  }
  
  
  /**
   * Create a XY chart. Usefull, e.g. for {@link DNAmethylation} data.
   * @param name any name that identifies the SERIES
   * @param nsList {@link NameAndSignals} to visualize
   * @param signalAndName signal out of <code>nsList</code> to visualize
   * @param includeOthers include other signals of the given
   * <code>nsList</code> that have the same {@link SignalType}.
   * Please use one of the declared flags, e.g. {@link #INCLUDE_OTHER_SERIES_WITH_LIGHT_COLORS}.
   * @return
   */
  public static JFreeChart createChart(String name, Collection<? extends NameAndSignals> nsList,
    ValuePair<String, SignalType> signalAndName, byte includeOthers) {
    final SignalType sigType = signalAndName.getB();
    
    // Create marker at 0 or 0.05
    double makerPosition = 0;
    if (sigType.equals(SignalType.pValue) || sigType.equals(SignalType.qValue)) {
      makerPosition = 0.05;
    }
    Marker zeroMarker = new ValueMarker(makerPosition,Color.LIGHT_GRAY, new BasicStroke(3f, BasicStroke.JOIN_ROUND, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {10.0f, 6.0f}, 0.0f));
    
    // Try to get a nice name
    String seriesName = signalAndName.getA();//getSeriesName(null, signalAndName);
    
    // Create data series
    final XYSplineRenderer renderer = new XYSplineRenderer();
    DefaultXYDataset dataset = new DefaultXYDataset();
    double[][] XYdata = getXYdata(nsList, signalAndName);
    dataset.addSeries(seriesName, XYdata);
    int seriesNumber=1;
    if (includeOthers!=DO_NOT_INCLUDE_OTHER_SERIES && nsList.size()>0) {
      Collection<ValuePair<String, SignalType>> availableSignals = nsList.iterator().next().getSignalNames();
      for (ValuePair<String, SignalType> sVd:availableSignals) {
        // Not twice and only same type.
        if (sVd.equals(signalAndName) || !sVd.getB().equals(sigType)) continue;
        
        dataset.addSeries(sVd.getA(), getXYdata(nsList, sVd)); // getSeriesName(null, sVd)
        if (includeOthers==INCLUDE_OTHER_SERIES_WITH_LIGHT_COLORS) {
          renderer.setSeriesPaint(seriesNumber, veryLightColors[(seriesNumber-1)%veryLightColors.length] );
        }
        seriesNumber++;
      }
    }
    
    // Configure both axes
    final NumberAxis signalValueAxis = new NumberAxis(signalAndName.getB().toString());
    if (sigType.equals(SignalType.FoldChange)) {
      signalValueAxis.setAutoRangeMinimumSize(2);
    }
    signalValueAxis.setAutoRangeIncludesZero(true);
    //domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    final NumberAxis xAxis = new NumberAxis("Location");
    xAxis.setAutoRange(true);
    xAxis.setAutoRangeIncludesZero(false);
    
    // Create plot, add marker baseline and return chart
    final XYPlot plot = new XYPlot(dataset, xAxis, signalValueAxis, renderer);
    plot.addRangeMarker(zeroMarker, Layer.BACKGROUND);
    
    JFreeChart jfc = new JFreeChart(plot);
    jfc.setTitle(getChartName(name, nsList.iterator().hasNext()?nsList.iterator().next():null));
    return jfc;
  }
  
  /**
   * Create a nice name for the chart.
   * @param name
   * @param ns
   * @return
   */
  private static String getChartName(String name, NameAndSignals ns) {
    if (name!=null) {
      return String.format("%s\n%s", name, IntegratorUI.getShortTypeNameForNS(ns.getClass()));
    } else {
      return IntegratorUI.getShortTypeNameForNS(ns.getClass());  
    }
  }

  /**
   * @param name
   * @param signalAndName
   * @return
   */
  protected static String getSeriesName(String name,
    ValuePair<String, SignalType> signalAndName) {
    boolean includeDot = false;
    if (name==null) name="";
    if (name.endsWith(".")) {
      includeDot = true;
      name=name.substring(0, name.length()-1);
    }
    if (name.length()>0) {
      name = String.format("%s (%s)", name, VisualizedData.getNiceSignalName(signalAndName));
    } else {
      name = String.format("%s", VisualizedData.getNiceSignalName(signalAndName));
    }
    if (includeDot) name += '.';
    return name;
  }
  
  
  /**
   * @param ns
   * @param vd
   * @return
   */
  private static double[][] getXYdata(Collection<? extends NameAndSignals> ns, ValuePair<String, SignalType> vd) {
    double[][] XYdata = new double[2][];
    XYdata[0] = new double[ns.size()];
    XYdata[1] = new double[ns.size()];
    
    // Get Location and signal
    Iterator<? extends NameAndSignals> it = ns.iterator();
    int i=-1;
    List<ValuePair<Double, Double>> xy = new ArrayList<ValuePair<Double,Double>>(ns.size());
    while (it.hasNext()) {
      i++;
      double x = i;
      NameAndSignals n = (NameAndSignals) it.next();
      if (n instanceof Region) {
        int start = ((Region) n).getStart();
        int end = ((Region) n).getEnd();
        if (start!=Region.DEFAULT_START && end!=Region.DEFAULT_START) {
          x = start+end/2; // build mean
        } else if (start!=Region.DEFAULT_START) {
          x = start;
        } else if (end!=Region.DEFAULT_START) {
          x = end;
        }
      }
      
      double y = n.getSignalMergedValue(vd.getB(), vd.getA(), MergeType.Mean);
      
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
   * Create a region-based XY plot for the given {@link IntegratorTab}.
   * Currently, only {@link DNAmethylation} data is supported, but actually, all
   * {@link NameAndSignals} implementing {@link Region} could be used!
   * @param parent
   * @return
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static JFreeChart createAndShowDialog(final IntegratorTab<?> parent) {
    JPanel panel = new JPanel();
    LayoutHelper lh = new LayoutHelper(panel);
    
    // Region based
    final JRadioButton region = new JRadioButton("Plot a custom region", true);
    lh.add(region);
    
    JPanel regionPanel = new JPanel();
    GUITools.createTitledPanel(regionPanel, "");
    LayoutHelper rpLH = new LayoutHelper(regionPanel);
    lh.add(regionPanel);
    final JLabeledComponent chr = new JLabeledComponent("Chromosome", true);
    chr.setSortHeaders(true);
    rpLH.add(chr);
    final JLabeledComponent start = new JLabeledComponent("Start", false, true);
    rpLH.add(start);
    final JLabeledComponent end = new JLabeledComponent("End", false, true);
    rpLH.add(end);
    
    // Enable only if region is selected.
    region.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        boolean enabled = (region.isSelected());
        GUITools.setEnabledForAll(enabled, chr, start, end);
      }
    });
    
    
    // Gene based
    final JRadioButton geneBased = new JRadioButton("Plot a region, associated to a gene", false);
    lh.add(geneBased);
    JPanel genePanel = new JPanel();
    GUITools.createTitledPanel(genePanel, "");
    LayoutHelper gpLH = new LayoutHelper(genePanel);
    lh.add(genePanel);
    
    final String[] tempHeaders = new String[]{"Please wait..."};
    final JLabeledComponent gene = new JLabeledComponent("Gene", true, tempHeaders);
    gene.setSortHeaders(true);
    gpLH.add(gene);
    
    // Enable only if region is selected.
    geneBased.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        boolean enabled = (geneBased.isSelected() && !Arrays.deepEquals(gene.getHeaders(), tempHeaders));
        gene.setEnabled(enabled);
      }
    });
    
    // Parse gene names
    Runnable collectGeneNames = new Runnable() {
      @Override
      public void run() {
        Set<String> names = new HashSet<String>();
        Set<String> chromosomes = new HashSet<String>();
        Iterator<NameAndSignals> l = ((Iterable<NameAndSignals>)parent.getData()).iterator();
        while (l.hasNext()) {
          NameAndSignals ns = l.next();
          names.add(ns.getName());
          if (ns instanceof Chromosome) {
            chromosomes.add(((Chromosome) ns).getChromosome());
          }
        }
        
        gene.setHeaders(names);
        gene.setEnabled(geneBased.isSelected());
        if (chromosomes.size()>0) {
          chr.setHeaders(chromosomes);
          chr.setEnabled(region.isSelected());
        }
      }
    };
    IntegratorUITools.runInSwingWorker(collectGeneNames);
    
    GUITools.createButtonGroup(region, geneBased);
    JLabeledComponent selExp = IntegratorUITools.createSelectExperimentBox((NameAndSignals)parent.getExampleData());
    lh.add(selExp);
    
    JRadioButton noOthers = new JRadioButton("Do not include other experiments", false);
    JRadioButton onOthers = new JRadioButton("Include other experiments with same signal type with light colors", true);
    JRadioButton alOthers = new JRadioButton("Include other experiments with same signal type", false);
    GUITools.createButtonGroup(noOthers, onOthers, alOthers);
    lh.add(noOthers);
    lh.add(onOthers);
    lh.add(alOthers
      );
    
    int ret = JOptionPane.showConfirmDialog(IntegratorUI.getInstance(), panel, "Select data to visualize", JOptionPane.OK_CANCEL_OPTION);
    if (ret==JOptionPane.OK_OPTION) {
      
      String name;
      List nsList = new ArrayList();
      if (region.isSelected()) {
        // REGION-BASED
        String chro; int starto=0; int endo=0;
        try {
          chro = chr.getSelectedItem().toString();
          starto = Integer.parseInt(start.getSelectedItem().toString());
          endo = Integer.parseInt(end.getSelectedItem().toString());
          name = String.format("%s:%s-%s", chro, starto, endo);
        } catch (Exception e) {
          GUITools.showErrorMessage(null, String.format("Invalid genome region \"%s-%s\".", 
            start.getSelectedItem().toString(), end.getSelectedItem().toString()));
          return null;
        }
        
        nsList = (List<? extends NameAndSignals>) BasicRegion.getAllIntersections((Iterable<Region>)parent.getData(), new BasicRegion(chro, starto, endo));
        
      } else {
        // GENE-BASED
        String geneName = gene.getSelectedItem().toString();
        name = String.format("Gene \"%s\".", geneName);
        
        nsList = getAllNSbelongingToSameGene((Iterable<NameAndSignals>)parent.getData(), geneName);
      }
      
      // Warn when nslist is 0
      if (nsList.size()<1) {
        GUITools.showErrorMessage(null, "No probes for the selected region!");
        return null;
      }
      
      // Create Chart
      byte includeOthers=0;
      if (onOthers.isSelected()) includeOthers = INCLUDE_OTHER_SERIES_WITH_LIGHT_COLORS;
      if (alOthers.isSelected()) includeOthers = INCLUDE_OTHER_SERIES;
      return createChart(name, nsList, (ValuePair<String, SignalType>) selExp.getSelectedItem(), includeOthers);
    }
    
    return null;
  }
  
  /**
   * Gene-based method to create a chart. Plots all probes, that
   * are associated with the same geneId or name (if template does
   * not implement {@link GeneID}) as the <code>template</code>.
   * @param <T>
   * @param allNS all {@link NameAndSignals}
   * @param template one probe of the GeneSet that should get visualized.
   * @return genome region plot
   */
  public static <T extends NameAndSignals> JFreeChart createChart(final Iterable<T> allNS, NameAndSignals template) {
    List<T> nsList = getAllNSbelongingToSameGene(allNS, template);
    if (nsList.size()<1) {
      return null;
    }
    String name = String.format("Gene \"%s\".", template.getName());
    return createChart(name, nsList, template.getSignals().get(0).getSignalAndName(), INCLUDE_OTHER_SERIES);
  }
  
  /**
   * Get all {@link NameAndSignals} from <code>allNS</code> that belong to
   * the same gene as <code>template</code>. Take the {@link GeneID} if the
   * interface is implemented and valid for <code>template</code>, else the name.
   * @param <T>
   * @param allNS
   * @param template
   * @return
   */
  public static <T extends NameAndSignals> List<T> getAllNSbelongingToSameGene(final Iterable<T> allNS, NameAndSignals template) {
    List<T> nsList = new ArrayList<T>();
    if (template==null || allNS==null) return nsList;
    String templateName = template.getName();
    
    int geneID=-1;
    if (template instanceof GeneID) {
      geneID = ((GeneID) template).getGeneID();
    }
    boolean hasGeneID = geneID>0;
    
    Iterator<T> l = allNS.iterator();
    while (l.hasNext()) {
      T ns = l.next();
      if (hasGeneID) {
        if (geneID==((GeneID)ns).getGeneID()) {
          nsList.add(ns);
        }
      } else {
        if (ns.getName().equals(templateName)) {
          nsList.add(ns);
        }
      }
    }
    
    return nsList;
  }
  
  /**
   * Simple wrapper for {@link #getAllNSbelongingToSameGene(Iterable, NameAndSignals)}.
   * @param <T>
   * @param allNS
   * @param name
   * @return
   */
  public static <T extends NameAndSignals> List<T> getAllNSbelongingToSameGene(final Iterable<T> allNS, String name) {
    return getAllNSbelongingToSameGene(allNS, new NameAndSignals(name) {
      private static final long serialVersionUID = 1L;
      @Override
      protected <L extends NameAndSignals> void merge(Collection<L> source,
        L target, MergeType m) {}
      @Override
      public String getUniqueLabel() {return getName();}
    });
  }

  
}

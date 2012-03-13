import javax.swing.JFrame;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.IntervalXYItemLabelGenerator;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.DefaultIntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.TextAnchor;

import de.zbit.sequence.region.SimpleRegion;
import de.zbit.sequence.region.Region;

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

/**
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class JFreeChartTests extends JFrame {
  
  public JFreeChartTests() {
    createChart();
  }
  
  
  /**
   * 
   */
  private void createChart() {
    // Create plot
    final ChartPanel chartPanel = new ChartPanel(createJFC());
    
    // Add to panel
    chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
    add(chartPanel);
    pack();
  }


  /**
   * @return
   */
  private JFreeChart createJFC() {
    String xAxisLabel = "Location";
    final NumberAxis xAxis = new NumberAxis(xAxisLabel);
    xAxis.setAutoRange(true);
    xAxis.setAutoRangeIncludesZero(false);

    
    // TODO: DEmo
    Region gene1 = new SimpleRegion((byte)10, 150000000, 150080000);
    Region gene2 = new SimpleRegion((byte)10, 150082000, 150083000);
    // Domain is X, Range is Y
    DefaultIntervalXYDataset genes = new DefaultIntervalXYDataset();
//    genes.addSeries("Genes", new double[][]{{gene1.getStart(),gene1.getStart(),gene1.getEnd(),1,0,1},
//        {gene2.getStart(),gene2.getStart(),gene2.getEnd(),-1,-1,0}});
    genes.addSeries("Genes", new double[][]{
        {gene1.getStart(),gene2.getStart()},
        {gene1.getStart(),gene2.getStart()},
        {gene1.getEnd(),gene2.getEnd()},
//        {1.5,-1.5},
//        {0.5,-1.5},
//        {1.5,-0.5}});
      {2  , -2  },
      {0  ,0  },
      {2  ,-2  }});
    // TODO: hide series in legend
    
    XYBarRenderer localXYBarRenderer = new XYBarRenderer();
    //localXYBarRenderer.setUseYInterval(true);

    
    localXYBarRenderer.setBaseItemLabelGenerator(new IntervalXYItemLabelGenerator()
    {
      private static final long serialVersionUID = 1L;
      /* (non-Javadoc)
       * @see org.jfree.chart.labels.AbstractXYItemLabelGenerator#generateLabelString(org.jfree.data.xy.XYDataset, int, int)
       */
      @Override
      public String generateLabelString(XYDataset dataset, int series, int item) {
        return "HALLO";
      }
    }
    );
    localXYBarRenderer.setBaseToolTipGenerator(new XYToolTipGenerator() {
      @Override
      public String generateToolTip(XYDataset dataset, int series, int item) {
        return "MyToolTip";
      }
    });
    localXYBarRenderer.setBaseItemLabelsVisible(true);
    //localXYBarRenderer.setBaseItemLabelFont(new Font("Dialog", Font.BOLD, 14));
    /*
     * Wenn start sichtbar
     *   Wenn Ende sichtbar, dann center
     *   sonst links
     * Else Wenn Ende sichtbar
     *   dann rechts
     * else
     *   CENTER //Hopefully... 
     */
    ItemLabelPosition pos = new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.CENTER);
    localXYBarRenderer.setBaseNegativeItemLabelPosition(pos);
    localXYBarRenderer.setBasePositiveItemLabelPosition(pos);


    
    
    XYPlot localXYPlot = new XYPlot(genes,
      (ValueAxis) xAxis, new NumberAxis(), localXYBarRenderer);
    
  //<<>>----
    
    
    
    JFreeChart jfc = new JFreeChart(localXYPlot);
    
    
    
    return jfc;
  }


  /**
   * @param args
   */
  public static void main(String[] args) {
    new JFreeChartTests().show();
  }
  
}

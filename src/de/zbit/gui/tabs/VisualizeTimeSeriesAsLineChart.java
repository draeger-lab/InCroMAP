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

package de.zbit.gui.tabs;

import java.awt.Color;
import java.awt.Paint;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import de.zbit.gui.BaseFrameTab;
import de.zbit.math.TimeSeriesModel;
import de.zbit.util.ArrayUtils;
import de.zbit.visualization.VisualizeGenesInChartTab;

/**
 * Provides methods to add a separate view, showing {@link TimeSeriesModel) in a
 * {@link JFreeChart}.
 * 
 * @author
 * @version $Rev$
 */

public class VisualizeTimeSeriesAsLineChart extends JPanel implements
    BaseFrameTab {
  /**
   * 
   */
  private static final long serialVersionUID = -3773389969892026694L;
  public static final transient Logger log = Logger
      .getLogger(VisualizeGenesInChartTab.class.getName());
  private static final Paint[] CHART_COLORS = new Paint[] {
      new Color(228, 26, 28), new Color(55, 126, 184), new Color(77, 175, 74),
      new Color(152, 78, 163), new Color(255, 127, 0), new Color(166, 86, 40),
      new Color(247, 129, 191), new Color(153, 153, 153) };

/**
	 * Generate a visualization of the {@link TimeSeriesModel).
	 * @param selectedModels The model to visualize.
	 * @param n Number of points to draw
	 */
  public VisualizeTimeSeriesAsLineChart(List<TimeSeriesModel> selectedModels,
      int n, String unit) {
    int size = selectedModels.size();
    // ArrayList containing the names of the models. Check if there are models
    // with the same name.
    // If yes, append a number to the model name, e.g "xyz (2)" if the model
    // "xyz" already exists
    String[] names = new String[size];
    for (int i = 0; i < size; i++) {
      names[i] = selectedModels.get(i).getName();
    }
    // Now check how many duplicates there are for each name
    for (int i = 0; i < size; i++) {
      int[] indices = ArrayUtils.indicesOf(names, names[i]);
      int occurrences = indices.length;
      // Change the name of the duplicates
      for (int j = 1; j < occurrences; j++) {
        names[indices[j]] = names[indices[j]] + " (" + (j + 1) + ")";
      }
    }

    // At first, collect the models
    // The xValues and yValues
    TimeSeriesModel m = selectedModels.get(0);
    double[] xModel = m.getDistributedTimePoints(n);
    ArrayList<double[]> yModel = new ArrayList<double[]>(size);
    for (int i = 0; i < size; i++) {
      // Compute the yValues
      m = selectedModels.get(i);
      double[] y = new double[n];
      for (int j = 0; j < n; j++) {
        y[j] = m.computeValueAtTimePoint(xModel[j], false);
      }
      yModel.add(y);
    }

    // The modelled functions, which will be plotted. Each consists of 200
    // equidistand
    // distributed points linked by a line.
    ArrayList<double[][]> pointsModel = new ArrayList<double[][]>(size);
    ArrayList<XYSeries> seriesModel = new ArrayList<XYSeries>(size);
    for (int i = 0; i < size; i++) {
      pointsModel.add(new double[][] { xModel, yModel.get(i) });
      seriesModel.add(new XYSeries(names[i]));
    }
    // Populate the series
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < n; j++) {
        seriesModel.get(i).add(xModel[j], yModel.get(i)[j]);
      }
    }
    // The dataset of the lines. Add all series to the dataset
    XYSeriesCollection models = new XYSeriesCollection();
    for (int i = 0; i < size; i++) {
      models.addSeries(seriesModel.get(i));
    }

    // The dataset of the measured time points. This are the measured time
    // points,
    // NOT the modelled ones. So the user can see how good the model fits to the
    // data.
    // They are not connected by a line, just the points are plotted.
    double[] xOriginal = m.getOriginalTimePoints();
    ArrayList<double[]> yOriginal = new ArrayList<double[]>(size);
    for (int i = 0; i < size; i++) {
      // Compute the yValues
      m = selectedModels.get(i);
      yOriginal.add(m.getY());
    }
    ArrayList<double[][]> pointsOriginal = new ArrayList<double[][]>(size);
    ArrayList<XYSeries> seriesOriginal = new ArrayList<XYSeries>(size);
    for (int i = 0; i < size; i++) {
      pointsOriginal.add(new double[][] { xOriginal, yOriginal.get(i) });
      seriesOriginal.add(new XYSeries(names[i]));
    }
    // Populate the series
    for (int i = 0; i < size; i++) {
      for (int j = 0; j < xOriginal.length; j++) {
        seriesOriginal.get(i).add(xOriginal[j], yOriginal.get(i)[j]);
      }
    }
    // The data set of the lines. Add all series to the data set
    XYSeriesCollection originals = new XYSeriesCollection();
    for (int i = 0; i < size; i++) {
      originals.addSeries(seriesOriginal.get(i));
    }

    // The renderer. The model is plotted as line, the original points are
    // plotted as points.
    XYLineAndShapeRenderer rendererModel = new XYLineAndShapeRenderer(true,
        false);
    XYLineAndShapeRenderer rendererOriginal = new XYLineAndShapeRenderer(false,
        true);
    // Set the original series (the points) invisible for the legend, because
    // they would have the same name
    // as the corresponding lines.
    for (int i = 0; i < size; i++) {
      rendererOriginal.setSeriesVisibleInLegend(i, false);
    }

    NumberAxis yax = new NumberAxis(selectedModels.get(0).getSignalType()
        .toString());

    // We need a logarithmized x-axes if the time points are exponentially
    // distributed
    ValueAxis xax;
    if (selectedModels.get(0).isExponentiallyDistributed()) {
      xax = new LogAxis(selectedModels.get(0).getSignalType().toString());
      ((LogAxis) xax).setBase(10);
    } else {
      xax = new NumberAxis(unit);
    }

    // The default colors used to color the series. Force both renderer to use
    // the same
    // color for the same series. Series with the same id belonging together.
    for (int i = 0; i < size; i++) {
      rendererModel.setSeriesPaint(i, CHART_COLORS[i % CHART_COLORS.length]);
      rendererOriginal.setSeriesPaint(i, CHART_COLORS[i % CHART_COLORS.length]);
    }

    // The plot of the models
    XYPlot plot = new XYPlot(models, xax, yax, rendererModel);
    // Add the original points to the plot
    plot.setDataset(1, originals);
    plot.setRenderer(1, rendererOriginal);

    // The panel showing the plot
    JFreeChart chart = new JFreeChart(plot);
    // ApplicationFrame frame = new
    // ApplicationFrame("Visualization of the models");
    ChartPanel chartPanel = new ChartPanel(chart);
    add(chartPanel);
  }

  @Override
  public File saveToFile() {
    return null;
  }

  @Override
  public void updateButtons(JMenuBar menuBar, JToolBar... toolbar) {
  }
}
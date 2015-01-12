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

package de.zbit.gui.tabs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import de.zbit.gui.BaseFrameTab;
import de.zbit.math.TimeSeriesModel;
import de.zbit.sequence.region.Region;
import de.zbit.visualization.VisualizeGenesInChartTab;

/**
 * Provides methods to add a separate view, showing
 * {@link TimeSeriesModel) in a {@link JFreeChart}.
 * 
 * @author 
 * @version $Rev$
 */

public class VisualizeTimeSeriesAsLineChart extends JPanel implements BaseFrameTab {
	public static final transient Logger log = Logger.getLogger(VisualizeGenesInChartTab.class.getName());
	
	/**
	 * Generate a visualization of the {@link TimeSeriesModel).
	 * @param selectedModels The model to visualize.
	 * @param n Number of points to draw
	 */
	public VisualizeTimeSeriesAsLineChart(List<TimeSeriesModel> selectedModels, int n, String unit) {
		int size = selectedModels.size();
		// The xValues and yValues
		TimeSeriesModel m = selectedModels.get(0);
		double[] xValues = m.getDistributedTimePoints(n);
		ArrayList<double[]> yValues = new ArrayList<double[]>(size);
		for(int i=0; i<size; i++) {
			// Compute the yValues
			m = selectedModels.get(i);
			double[] y = new double[n];
			for(int j=0; j<n; j++) {
				y[j] = m.computeValueAtTimePoint(xValues[j]);
			}
			yValues.add(y);
		}
		
		// The serieses, which will be plotted.
		ArrayList<double[][]> points = new ArrayList<double[][]>(size);
		ArrayList<XYSeries> serieses = new ArrayList<XYSeries>(size);
		for(int i=0; i<size; i++) {
			points.add(new double[][] {xValues, yValues.get(i)});
			serieses.add(new XYSeries(selectedModels.get(i).getName()));
		}
		
		// Populate the serieses
		for(int i=0; i<size; i++) {
			for(int j=0; j<n; j++) {
				serieses.get(i).add(xValues[j], yValues.get(i)[j]);
			}
		}
		
		// The dataset. Add all serieses to the dataset
		XYSeriesCollection dataset = new XYSeriesCollection();
		for(int i=0; i<size; i++) {
			dataset.addSeries(serieses.get(i));
		}
		
		// The renderer
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
		NumberAxis xax = new NumberAxis(unit);
		NumberAxis yax = new NumberAxis(selectedModels.get(0).getSignalType().toString());
		
		// The plot
		XYPlot plot = new XYPlot(dataset, xax, yax, renderer);
		JFreeChart chart = new JFreeChart(plot);
			
		// The panel showing the plot
		ApplicationFrame frame = new ApplicationFrame("Visualization of the models");
		ChartPanel chartPanel = new ChartPanel(chart);
		add(chartPanel);
	}

	@Override
	public File saveToFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateButtons(JMenuBar menuBar, JToolBar... toolbar) {
		// TODO Auto-generated method stub
		
	}
}

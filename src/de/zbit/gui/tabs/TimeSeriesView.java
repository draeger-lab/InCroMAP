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

import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import y.base.Node;
import y.base.NodeCursor;
import y.view.Graph2D;
import y.view.Graph2DView;
import y.view.Graph2DViewMouseWheelZoomListener;
import y.view.HitInfo;
import de.zbit.graph.RestrictedEditMode;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.JDropDownButton;
import de.zbit.gui.customcomponents.FilmControlPanel;
import de.zbit.io.FileDownload;
import de.zbit.kegg.gui.IntegratorPathwayPanel;
import de.zbit.kegg.gui.TranslatorPanelTools;
import de.zbit.util.Species;
import de.zbit.util.progressbar.AbstractProgressBar;
import de.zbit.visualization.VisualizeTimeSeries;
import de.zbit.visualization.VisualizeTimeSeriesListener;
import de.zbit.visualization.VisualizeTimeSeriesListener.VTSAction;

/**
 * The Panel that is used to display and manipulate a film of a pathway.
 * So this class corresponds to the view of the MVC-Pattern.
 * The film is generated in the class {@link VisualizeTimeSeries}.
 * 
 * @author Felix Bartusch
 * @version $Rev$
 */

public class TimeSeriesView extends IntegratorTab<Graph2D> {
	private static final long serialVersionUID = -5829484604586665170L;

	/**
	 * Generates the film, that is displayed in this panel.
	 * Acts as model in the MVC-Pattern.
	 */
	VisualizeTimeSeries model;

	/**
	 * Responds to user input.
	 * Acts as controller in the MVC-Pattern.
	 */
	VisualizeTimeSeriesListener controller;

	/** The graphView of the visualized graph */
	Graph2DView graphView;
	
	/**  */
	IntegratorPathwayPanel pathwayPanel = null;

	/** Panel to control the film and select arbitrary frames */
	private FilmControlPanel controlPanel;
	
	/** ToolBar buttons to change the visualized enrichment data */
	private JMenuItem pValueButton;
	private JMenuItem qValueButton;
	JMenuItem BH_cor;
	JMenuItem BFH_cor;
	JMenuItem BO_cor;
	
	/**
	 * 
	 * @param keggPathway
	 * @param model
	 * @param controller
	 * @param integratorUI
	 * @param species
	 */
	public TimeSeriesView(String keggPathway, VisualizeTimeSeries model,
			VisualizeTimeSeriesListener controller, IntegratorUI integratorUI, Species species) {
		super(integratorUI, null, species); // There's no film at this point, so data is null
		this.controller = controller;
		this.model = model;
		
		//Never display scrollbars. The graphView has its own scrollbars.
		setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	}

	@Override
	public JComponent getVisualization() {
		// If the film isn't generated yet, there is nothing to visualize.
		// So return an empty panel
		if(controller == null || !controller.isFilmGenerated()) {
			JPanel panel = new JPanel();
			return panel;	
		}
		
		// Set the background image
		TranslatorPanelTools.setupBackgroundImage(pathwayPanel);
		
		// The viewport visualizes the IntegratorPathwayPanel, which has a Graph2DView
		// to show the pathway and a detail panel to show detailed information about nodes.
		this.viewport.setView(pathwayPanel);
		
		// Build the control panel. We are showing the first frame, so set the secondsToTimeUnit
		// label and disable the previous frame button
		controlPanel = new FilmControlPanel(controller);
		controlPanel.setFrameToTimeUnit(1, model.mapFrameToTimePoint(1), model.getTimeUnit());
		controlPanel.enablePrevButton(false);
		
		// The control panel is shown as the column header of this scroll pane
		setColumnHeaderView(controlPanel);
				
		return viewport;
	}

	@Override
	public File saveToFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateButtons(JMenuBar menuBar, JToolBar... toolbar) {
		// Just update the toolbar
		createJToolBarItems(toolbar[0]);
	}

	@Override
	public void createJToolBarItems(JToolBar bar) {		
    String uniqueName = parent.getClass().getSimpleName() + parent.hashCode();
    bar.removeAll();
    bar.setName(uniqueName);
    
    // Visualization option: Visualize pValue or qValue of the KEGG Pathway Enrichment?
    JPopupMenu visualizationOptions = new JPopupMenu("Visualization options");
    pValueButton = GUITools.createJMenuItem(controller, VTSAction.VISUALIZE_ENRICHMENT_PVAL,
    		UIManager.getIcon("ICON_PREFS_16"),null,null,JCheckBoxMenuItem.class,true);
    qValueButton = GUITools.createJMenuItem(controller, VTSAction.VISUALIZE_ENRICHMENT_QVAL,
    		UIManager.getIcon("ICON_PREFS_16"),null,null,JCheckBoxMenuItem.class,true);
    visualizationOptions.add(pValueButton);
    visualizationOptions.add(qValueButton);
    JDropDownButton visualizationOptionsButton = new JDropDownButton("Visualization options",
    		UIManager.getIcon("ICON_PREFS_16"), visualizationOptions);
    bar.add(visualizationOptionsButton);
    pValueButton.setSelected(true);
    
    // FDR correction method option
    JPopupMenu fdr = new JPopupMenu("FDR correction");
    BH_cor = GUITools.createJMenuItem(controller,
        VTSAction.FDR_CORRECTION_BH, UIManager.getIcon("ICON_MATH_16"),null,null,JCheckBoxMenuItem.class,true);
    fdr.add(BH_cor);
    BFH_cor = GUITools.createJMenuItem(controller,
        VTSAction.FDR_CORRECTION_BFH, UIManager.getIcon("ICON_MATH_16"),null,null,JCheckBoxMenuItem.class,true);
    fdr.add(BFH_cor);
    BO_cor = GUITools.createJMenuItem(controller,
        VTSAction.FDR_CORRECTION_BO, UIManager.getIcon("ICON_MATH_16"),null,null,JCheckBoxMenuItem.class,true);
    fdr.add(BO_cor);
    JDropDownButton fdrButton = new JDropDownButton("FDR correction", 
        UIManager.getIcon("ICON_MATH_16"), fdr);
    fdrButton.setToolTipText("Change the false-discovery-rate correction method.");
    bar.add(fdrButton);
    BH_cor.setSelected(true);
    
    // Export as video button
    JButton export = GUITools.createJButton(controller,
    		VTSAction.EXPORT_AS_VIDEO, UIManager.getIcon("ICON_SAVE_16"));
    bar.add(export);
    
    GUITools.setOpaqueForAllElements(bar, false); 
	}

	@Override
	public Object getObjectAt(int i) {
		return null;
	}

	@Override
	public int[] getSelectedIndices() {
		return null;
	}

	/**
	 * Show a loading panel with a prograss bar and a short message attached to the progress bar.
	 * @param message to attach.
	 * @return the progress bar
	 */
	public AbstractProgressBar showTemporaryLoadingPanel(String message) {

		// Show progress-bar
		final AbstractProgressBar pb = generateLoadingPanel(this, message);
		FileDownload.ProgressBar = pb;
		getViewport().repaint();

		return pb;
	}
	  

	public void showGraph(Graph2D graph, int curFrame) {
		showGraph(graph, curFrame, false);
	}
	
	/**
	 * Testing the visualization of a graph. So I can see the result of manipulating the graph
	 * in the class VisualizeTimeSeries
	 * @param graph
	 */
	public void showGraph(Graph2D graph, int curFrame, boolean resizeGraph) {
		
		// Look for a selected node. If there is one, the detail panel has to be updated
		// to show the new information (e.g. enrichment p-value) of the selected node
		Node selectedNode = null;
		for (NodeCursor nc = graph.nodes(); nc.ok(); nc.next())  {  
		  if (graph.isSelected(nc.node()))
		  	selectedNode = nc.node();	  	
		}
	
		// Set the new graph
		pathwayPanel.getGraph2DView().setGraph2D(graph);
		
		// Update the detailPanel of the pathwayPanel if a node was selected.
		// For that, simulate that there is an click on the previous selected node
		if(selectedNode != null) {
			// Get the position of the selected node and create a fake HitInfo
			double x = graph.getCenterX(selectedNode);
			double y = graph.getCenterY(selectedNode);
			HitInfo hi = new HitInfo(graph, x, y, HitInfo.NODE, false);
			
			// The selected node was 'clicked' again, so update the detail panel
			pathwayPanel.updateDetailPanel(hi);
		}
		
		if(resizeGraph) {		
			try {
				graphView.fitContent(true);
			} catch (Throwable t) {} // Not really a problem
		}
	
		// Update the control panel
		controlPanel.setFrameToTimeUnit(curFrame,
				model.mapFrameToTimePoint(curFrame), model.getTimeUnit());
		controlPanel.setSliderValue(curFrame);
		
		repaint();
		}

	/**
	 * Create the {@link Graph2DView} which displays the colored pathway (graph)
	 * and define the {@link Graph2DView}'s behaviour.
	 */
	public void initializeGraphView() {
		// Create the graph view
		graphView = new Graph2DView();
		
		// User can zoom in / out the viewport
		graphView.getCanvasComponent().addMouseWheelListener(new Graph2DViewMouseWheelZoomListener());
		
		// Because there is a also resizable panel with additional information
		// don't let the graph resize.
		graphView.setFitContentOnResize(false);
		
		// Show Navigation and Overview on the left side of the graph
		RestrictedEditMode.addOverviewAndNavigation(graphView);	
	}

	/**
	 * Enables / Disables the functionality to show the next frame.
	 * Is called, when the model cannot deliver the next frame
	 * (i.e view shows currently the last frame)
	 * @param b can the model deliver a next frame?
	 */
	public void enableNextFrameFunctionality(boolean b) {
		// Gray out the next button of the film panel
		controlPanel.enableNextButton(b);
	}
	
	/**
	 * Enables / Disables the functionality to show the previous frame.
	 * Is called, when the model cannot deliver the previous frame
	 * (i.e view shows currently the first frame)
	 * @param b can the model deliver a previous frame?
	 */
	public void enablePrevFrameFunctionality(boolean b) {
		// Gray out the next button of the film panel
		controlPanel.enablePrevButton(b);
	}

	/**
	 * Enables / Disables the functionality of the play button.
	 * Is called, when the user pushes the play/pause-button.
	 * @param b, if the play button should be enabled?
	 */
	public void enablePlayFunctionality(boolean b) {
		controlPanel.enablePlayButton(b);
		validate();
	}
	
	/**
	 * Selects/deselects the menu items for visualization of p-values and q-values in the
	 * options menu of the toolbar.
	 * @param b		if true, the p-value button is selected and the q-value button is deselected.</br>
	 *            if false, the p-value button is deselected and the q-value button is selected.
	 */
	public void selectPValueButton(boolean b) {
		pValueButton.setSelected(b);
		qValueButton.setSelected(!b);
	}
	
	/**
	 * Select the False Discovery Rate method for correcting the p-value.
	 * @param string
	 */
	public void selectFDR(String fdr) {
		if(fdr == VTSAction.FDR_CORRECTION_BH.toString()) {
			BH_cor.setSelected(true);
			BFH_cor.setSelected(false);
			BO_cor.setSelected(false);
		} else if (fdr == VTSAction.FDR_CORRECTION_BFH.toString()) {
			BH_cor.setSelected(false);
			BFH_cor.setSelected(true);
			BO_cor.setSelected(false);
		} else if (fdr == VTSAction.FDR_CORRECTION_BO.toString()) {
			BH_cor.setSelected(false);
			BFH_cor.setSelected(false);
			BO_cor.setSelected(true);
		}
	}

	/**
	 * Set the pathway panel.
	 * @param pathwayPanel to set
	 */
	public void setPathwayPanel(IntegratorPathwayPanel pathwayPanel) {
		this.pathwayPanel = pathwayPanel;		
	}

	public IntegratorPathwayPanel getPathwayPanel() {
		return pathwayPanel;
	}
}

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
package de.zbit.visualization;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.filechooser.FileFilter;

import y.io.IOHandler;
import y.io.JPGIOHandler;
import y.io.ViewPortConfigurator;
import y.view.Graph2D;
import y.view.Graph2DView;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;

import de.zbit.analysis.enrichment.KEGGPathwayEnrichment;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNATimeSeries;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.gui.GUITools;
import de.zbit.gui.dialogs.FilmSettingsDialog;
import de.zbit.gui.tabs.NSTimeSeriesTab;
import de.zbit.gui.tabs.TimeSeriesView;
import de.zbit.math.TimeSeriesModel;
import de.zbit.util.NotifyingWorker;
import de.zbit.util.Species;
import de.zbit.utils.SignalColor;
import de.zbit.visualization.VisualizeTimeSeriesListener.VTSAction;
import de.zbit.kegg.io.KEGGImporter;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;

/**
 * Creates a film of a pathway. The film is displayed in {@link TimeSeriesView}.
 * This class acts as model and controller in the MVC-Pattern,
 * TimeSeriesView acts as View.
 * 
 * @author Felix Bartusch
 * @version $Rev$
 */
public class VisualizeTimeSeries {
	public static final transient Logger log = Logger.getLogger(TranslatorPanel.class.getName());

	/**
	 * The view showing the film.
	 * Acts as view in the MVC-Pattern.
	 */
	private TimeSeriesView view;
	
	/**
	 * Respond to all events while downloading the pathway.
	 * Acts as controller in the MVC-Pattern.
	 */
	VisualizeTimeSeriesListener controller;
	
	// Data to model genes and compute enrichments
	private NSTimeSeriesTab source;
	private List<TimeSeriesModel> models;
	private String pathwayID;
	private KEGGImporter keggImporter;
	private Species species;
	private double cutoff;
	private KEGGPathwayEnrichment enrich = null;
	/**
	 * The signal type of the modelled data.
	 */
	private SignalType signalType;
	
	// Information for the film
	private Graph2D pathway;
	private String timeUnit = "";
	private int frameRate;
	private int duration;
	// Dimension of the resulting film.
	private static Dimension dimension;
	// Filename of the generated film.
	private final String outputFilename = "./testFilm.mp4";
	// Plays the film. Is visualized in TimeSeriesView
	private IMediaWriter writer = null;
	
	// Holds 'visualization' of the colored graph. This panel is never shown to the user.
	// A buffered image is generated from the colored graph. The image is then encoded into the film.
	private TranslatorPanel<Graph2D> transPanel;
	
	/**
	 * This container holds the finished film.
	 */
	private IContainer film;
	
	
	public VisualizeTimeSeries(NSTimeSeriesTab parent) {
		// Get the required information from the parent tab
		this.source = parent;
		this.timeUnit = parent.getTimeUnit();
		this.species = parent.getSpecies();
		this.models = (List<TimeSeriesModel>) parent.getGeneModels();
		this.controller = new VisualizeTimeSeriesListener(this);
		this.signalType = parent.getSignalType();
		
		// Ask user for film settings
		FilmSettingsDialog settingsDialog = new FilmSettingsDialog(species, controller, this);
		//int dialogConfirmed = 
		GUITools.showAsDialog(source, settingsDialog, "Choose film settings", false);
		System.out.println("Cutoff: " + settingsDialog.getCutoff()); // for testing
		System.out.println("duration: " + settingsDialog.getDuration()); // for testing
		
		// download pathway and generate film, if ok button was pressed. Otherwise, do nothing
		if(settingsDialog.okPressed()) {
			// Get the user input and filter genes
			this.pathwayID = settingsDialog.getSelectedPathwayID();
			this.duration = settingsDialog.getDuration();
			this.frameRate = settingsDialog.getFrameRate();
			this.cutoff = settingsDialog.getCutoff();
			this.models = filterNullGeneModels(parent.getGeneModels(), settingsDialog.getCutoff());
			
			// Initialize the view and the controller
			this.view = new TimeSeriesView(pathwayID, this, controller, parent.getIntegratorUI(), species);
			controller.setView(view);
			
			// Download pathway
			this.keggImporter = new KEGGImporter(pathwayID, Format.JPG);
			keggImporter.addActionListener(controller);		
			// Build and show the view
			parent.getIntegratorUI().addTab(view, "Film of " + parent.getName());
		  keggImporter.execute();
		}
	}
	
	/**
	 * Generate the film. Generate single colored images of the pathway and encode them.
	 */
	void generateFilm()  {
		createInvisibleTranslatorPanel();		
		
		// At which timePoints should the pathway be modeled?
		final int numFrames = duration * frameRate;
		final double[] timePoints = computeTimePoints(numFrames);
		
		// This worker generates the film.
		NotifyingWorker<IMediaWriter, Void> filmGenerator = new NotifyingWorker<IMediaWriter, Void>() {

			@Override
			protected IMediaWriter doInBackground() throws Exception {				
				// Call controller to set progress bar
				fireActionEvent(new ActionEvent(this, numFrames, VTSAction.START_GENERATE_FILM.toString()));
								
				// Create an KEGGPathway enrichment
				try {
					enrich = new KEGGPathwayEnrichment(species, true, false, null);
				} catch (IOException e) {
					e.printStackTrace();
				}
								
				// Initialize IMediaWriter to encode the film.
				writer = initializeWriter();
				film = writer.getContainer();

				// Generate all single images and encode them
				for(int i = 0; i < numFrames; i++) {
					BufferedImage image = generatePathwayImage(timePoints[i]);
					writer.encodeVideo(0, image, (long) (i * 1000)/frameRate, TimeUnit.MILLISECONDS);
					fireActionEvent(new ActionEvent(transPanel.getDocument(), i, VTSAction.IMAGE_GENERATED.toString()));					
				}
				
				// for testing generate ONE image
				//generatePathwayImage(timePoints[0]);
				// for testing, write movie to file
				writer.close();
				
				// return the film to the Controller
				return null;
			}
			
			@Override
			protected void done() {
				// Tell the listener, that film was created
				fireActionEvent(new ActionEvent(writer.getContainer(), 0, VTSAction.END_GENERATE_FILM.toString()));
			}

			// Initialize writer with the width and height of the graph
			private IMediaWriter initializeWriter() {
				final IMediaWriter writer = ToolFactory.makeWriter(outputFilename);
				// Get a example image, width and height
				BufferedImage example = generatePathwayImage(timePoints[0]);
				int width = example.getWidth();
				int height = example.getHeight();
			
				writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, width, height);
				return writer;
			}		
		};

		// Generate film
		filmGenerator.addActionListener(controller);
		try{
			filmGenerator.execute();			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generate an image of the pathway at a certain time point. The nodes are colored due to
	 * a KEGG pathway enrichment, which is also computed in this function.
	 * @param timePoint for that a pathway image is computed
	 * @return a pathway image
	 */
	private BufferedImage generatePathwayImage(double timePoint) {
		// compute model values at the given time point
		ArrayList<mRNATimeSeries> modelValues = computeModelValues(timePoint);
		// sort model values by their signal
		Collections.sort(modelValues, Signal.getComparator("val", signalType));
		modelValues = filterGenes(modelValues, cutoff, signalType);
				
		// compute the pathway enrichment for the given time point
		List<EnrichmentObject<String>> l=null;
		try {
			// first option to speed up enrichment computing: never compute the exact p-value.
			System.out.println("Compute Enrichment on " + modelValues.size() + " genes."); // for testing
      l = enrich.getEnrichments(modelValues, null, null, false);
    } catch (Throwable e) {
      GUITools.showErrorMessage(null, e);
    }
		
		// Color the graph according to the enrichment
		colorPathway(l);

		// TODO: Maybe you can create just one Graph2DView, and change the graph every time
		// Take image of the colored graph (taken from yFiles Developers Guide)
		// Setting up the graphs view. So that an image of the whole graph is made
		Graph2D graph = transPanel.getDocument();
		Graph2DView graphView = new Graph2DView(graph);
		try {
			graphView.fitContent(true);
		} catch (Throwable t) {}		
		graphView.setFitContentOnResize(true);
		graph.setCurrentView(graphView);

		ViewPortConfigurator vpc = new ViewPortConfigurator();          
		// Register the graph to be exported with the configurator instance.   
		// Depending on the other settings (see below) the graph will be used to   
		// determine the image size, for example.   
		vpc.setGraph2D(graph);  
		// The complete graph should be exported, hence set the clipping type   
		// accordingly.   
		vpc.setClipType(ViewPortConfigurator.CLIP_GRAPH);  
		// The graph's bounding box should determine the size of the image.   
		vpc.setSizeType(ViewPortConfigurator.SIZE_USE_ORIGINAL);  
		// Configure the export view using mainly default values, i.e., zoom level   
		// 100%, and 15 pixel margin around the graph's bounding box.   
		vpc.configure((Graph2DView) graph.getCurrentView());  
 
	  IOHandler ioh = new JPGIOHandler();  
	  // Set the image quality to 90%. This yields a good compromise between small   
	  // file size and high quality.   
	  ((JPGIOHandler)ioh).setQuality(0.9f);
	  	 	  	  
	  BufferedImage image = null;
	  
	  try {
	  	// TODO: Maybe two pipedStreams in two Threads are faster than ByteArrays
	  	//PipedOutputStream outputStream = new PipedOutputStream();
	  	//PipedInputStream inputStream = new PipedInputStream(outputStream);
	  	// Graph -> ByteArrayOutputStream -> ByteArray -> ByteArrayInputStream -> BufferedImage
	  	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	  	ioh.write(graph, outputStream);
			ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
	  	image = ImageIO.read(inputStream);			
		} catch (IOException e) {
			e.printStackTrace();
		}
	  
	  // for testing. Save image to file
	  try {
			ioh.write(graph, "TestImageAt_" + timePoint);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	 		
		return image;
	}

	/**
	 * Filter the genes according to their signal type and a cutoff value choosen previously by the user.</br>
	 * - p-values: filter out genes with an higher p-value than the cutoff value</br>
	 * - fold changes: filter out genes with an lower absolute fold change than the cutoff
	 * @param cutoff value
	 * @param signalType
	 * @return filtered genes
	 */
	private ArrayList<mRNATimeSeries> filterGenes(ArrayList<mRNATimeSeries> genes,
			double cutoff, SignalType signalType) {
		// list containing the filtered genes
		ArrayList<mRNATimeSeries> filteredGenes = new ArrayList<mRNATimeSeries>();
		
		// filter the genes
		if(signalType == SignalType.pValue) {
			for(mRNATimeSeries m : genes) {
				if(m.getSignal(SignalType.pValue, "val").getSignal().doubleValue() < cutoff)
					filteredGenes.add(m);
			}
		} else if(signalType == SignalType.FoldChange) {
			for(mRNATimeSeries m : genes) {
				if(Math.abs(m.getSignal(SignalType.FoldChange, "val").getSignal().doubleValue()) > cutoff)
					filteredGenes.add(m);
			}
		}

		return filteredGenes;
	}

	/**
	 * Compute the values of the given {@link TimeSeriesModel}s at the given time point.
	 * @param timePoint for which values are computed
	 * @return a List of mRNATimeSeries objects with one data column
	 */
	private ArrayList<mRNATimeSeries> computeModelValues(double timePoint) {		
		ArrayList<mRNATimeSeries> values = new ArrayList<>(models.size());
		
		// for each model, compute the value at the time point and build a mRNATimeSeries object
		for(TimeSeriesModel m : models) {
			double val = m.computeValueAtTimePoint(timePoint);
			mRNATimeSeries mRNA = new mRNATimeSeries(m.getName(), m.getGeneID());
			mRNA.addSignal(val, "val" , m.getSignalType()); // name of SignalColumn is arbitrary
			
			values.add(mRNA);
		}
		
		return values;
	}


	/**
	 * Color pathway according to p-values or q-values of the given enrichment.
	 * @param l 
	 */
	private void colorPathway(List<EnrichmentObject<String>> l) {
		// analoge to KEGGPathwayActionListener
		createInvisibleTranslatorPanel(); // reset transPanel to an uncolored graph
		VisualizeDataInPathway visData = new VisualizeDataInPathway(transPanel);
		Class<? extends NameAndSignals> dataType = NameAndSignals.getType(l);
		
		// remove existing visualization
		if(visData.isDataTypeVisualized(dataType)) {
			visData.removeVisualization(dataType);
		}
		
		// Default signal colore
		// TODO Find a better recolorer?
		SignalColor recolorer = new SignalColor(null, null, SignalType.pValue);

		// color the graph
		visData.visualizeData(l, "", "Enrichment", SignalType.pValue, recolorer);
		}


	/**
	 * @param numFrames 
	 * @return the equidistance time points for which the pathway should be modeled. 
	 */
	private double[] computeTimePoints(int numFrames) {
		double[] timePoints = new double[numFrames];
		// get a sample TimeSeriesModel, first and last time point to model
		TimeSeriesModel m = models.toArray(new TimeSeriesModel[1])[0];
		double begin = m.getFirstTimePoint();
		double end = m.getLastTimePoint();
		double step = (end - begin) / (numFrames-1); // numFrames-1: first and last frame show first and last data column
		
		for(int i = 0; i < timePoints.length; i++) {
			timePoints[i] = begin + (i * step);
			System.out.println("timePoint " + i + ": "+ timePoints[i]);
		}
		return timePoints;
	}


	/**
	 * Filter out null gene models. E.g models which has no GeneID.
	 * @param geneModels to be filtered
	 * @return filtered gene models
	 */
	private List<TimeSeriesModel> filterNullGeneModels(Collection<TimeSeriesModel> geneModels, double cutoff) {
		ArrayList<TimeSeriesModel> filteredModels = new ArrayList<TimeSeriesModel>();
		
		System.out.println("Filter: Genes before: " + geneModels.size()); // for testing
		
		// filter the models
		for(TimeSeriesModel model : geneModels) {
			// filter out null models
			if(model != null)
				filteredModels.add(model);
		}	
					
		System.out.println("Filter: Genes after: " + filteredModels.size()); // for testing
		
		return filteredModels;
	}

	/**
	 * The method generates a {@link TranslatorPanel}. The panel is never shown to the user.
	 * The panel is just used to manipulate easily the graph of the pathway with existing functions.
	 */
	private void createInvisibleTranslatorPanel() {
		transPanel = new TranslatorPanel<Graph2D>(null, null, null, pathway) {
			private static final long serialVersionUID = -1372432690962944528L;

			@Override
			protected void createTabContent() throws Exception {
				return;	
			}
			@Override
			public List<FileFilter> getOutputFileFilter() {
				return null;
			}
			@Override
			protected boolean writeToFileUnchecked(File file, String format)
					throws Exception {
				return false;
			}
		};
	}
	
	/**
	 * 
	 * @return the species of the model.
	 */
	public Species getSpecies() {
		return species;
	}


	public String getTimeUnit() {
		return timeUnit;
	}


	public String getPathwayID() {
		return pathwayID;
	}


	public int getFramerate() {
		return frameRate;
	}


	public int getDuration() {
		return duration;
	}


	public String getOutputfilename() {
		return outputFilename;
	}


	public static Dimension getDimension() {
		return dimension;
	}

	public Graph2D getPathway() {
		return pathway;
	}

	public void setPathway(Graph2D pathway) {
		this.pathway = pathway;
	}


	public KEGGImporter getKeggImporter() {
		return keggImporter;
	}
	
	public double getFirstTimePoint() {
		return models.get(0).getFirstTimePoint();
	}
	
	
	public double getLastTimePoint() {
		return models.get(0).getLastTimePoint()	;
	}

	public SignalType getSignalType() {
		return signalType;
	}

	public IContainer getFilm() {
		return film;
	}

}

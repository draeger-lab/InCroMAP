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
package de.zbit.visualization;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import y.io.IOHandler;
import y.io.JPGIOHandler;
import y.io.ViewPortConfigurator;
import y.view.Graph2D;
import y.view.Graph2DView;
import de.zbit.analysis.enrichment.KEGGPathwayEnrichment;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.gui.GUIOptions;
import de.zbit.gui.GUITools;
import de.zbit.gui.dialogs.FilmSettingsDialog;
import de.zbit.gui.tabs.NSTimeSeriesTab;
import de.zbit.gui.tabs.TimeSeriesView;
import de.zbit.io.FileTools;
import de.zbit.math.BenjaminiHochberg;
import de.zbit.math.Bonferroni;
import de.zbit.math.BonferroniHolm;
import de.zbit.math.TimeSeriesModel;
import de.zbit.util.NotifyingWorker;
import de.zbit.util.Species;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.utils.GifSequenceWriter;
import de.zbit.utils.SignalColor;
import de.zbit.visualization.VisualizeTimeSeriesListener.VTSAction;
import de.zbit.kegg.gui.IntegratorPathwayPanel;
import de.zbit.kegg.io.KEGGImporter;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;

/**
 * Creates a film of a pathway. The film is displayed in {@link TimeSeriesView}.
 * This class acts as model in the MVC-Pattern,
 * TimeSeriesView acts as View.
 * VisualizeTimeSeriesListener acts as controller.
 * 
 * @author Felix Bartusch
 * @version $Rev$
 */
public class VisualizeTimeSeries {
	private static final int TIME_PER_FRAME = 1000;

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

	/** The parent tab */
	private NSTimeSeriesTab source;
	/** The models to interpolate mRNA data at arbitrary time points */
	private List<TimeSeriesModel> models;
	/** The time points which shall be visualized */
	private double[] timePoints;
	/** The pathway enrichments for all time points */
	private List<List<EnrichmentObject<String>>> enrichments;
	/** The modelled mRNA data for each time point */
	private List<List<mRNA>> mRNA;
	/** The KEGG pathwayID of the visualized pathway */
	private String pathwayID;
	/** What enrichment result shall be visualized? The enrichment p-value or q-value?
	 *  And what correction method shall be used? **/
	private boolean visualizePValue = true;
	/** The importer for the KEGG pathway, downloads pathway information from KEGG*/
	private KEGGImporter keggImporter;
	/** The treated species from which data was obtained */
	private Species species;
	/** The cutoff value which is used to filter the interpolated mRNA data */
	private double cutoff;
	/** This object computes pathway enrichments from given mRNA data */
	private KEGGPathwayEnrichment enrich = null;
	/** The signal type of the modelled data */
	private SignalType signalType;

	private Graph2D pathway;
	/** The time unit of the visualized data (e.g. 'day') */
	private String timeUnit = "";
	private int frameRate;
	private int duration; // this one is the user input! Don't confuse with filmDuration
	private int numFrames;
	// Dimension of the resulting film.
	private static Dimension dimension;

	// Holds 'visualization' of the colored graph. This panel is never shown to the user.
	// A buffered image is generated from the colored graph. The image is then encoded into the film.
	private TranslatorPanel<Graph2D> transPanel;
	private VisualizeDataInPathway visData; 

	IntegratorPathwayPanel pathwayPanel;

	private boolean useOriginalData;


	public VisualizeTimeSeries(NSTimeSeriesTab parent) {
		// Get the required information from the parent tab
		this.source = parent;
		this.timeUnit = parent.getTimeUnit();
		this.species = parent.getSpecies();
		this.models = filterNullGeneModels(parent.getGeneModels());
		this.controller = new VisualizeTimeSeriesListener(this);
		this.signalType = parent.getSignalType();

		// Ask user for settings
		FilmSettingsDialog settingsDialog = new FilmSettingsDialog(species, this);
		GUITools.showAsDialog(source, settingsDialog, "Please choose visualization settings", false);

		// download pathway and generate film, if ok button was pressed. Otherwise, do nothing
		if(settingsDialog.okPressed()) {
			// Get the user input and filter genes
			this.pathwayID = settingsDialog.getSelectedPathwayID();
			this.cutoff = settingsDialog.getCutoff();
			
			// testing
//			System.out.println("Test: Chosen pathway: " + settingsDialog.getSelectedPathwayID());
//			System.out.println("Test: Chosen cutoff: " + settingsDialog.getCutoff());
//			System.out.println("Test: Chosen numFrames: " + settingsDialog.getNumFrames());
//			System.out.println("Test: NumDataPoints: " + parent.getNumObservations());
//			System.out.println("Test: timePoints: " + models.get(0).getDistributedTimePoints(settingsDialog.getNumFrames()));
			
			// How many frames should be created? And what are their time points?
			if(settingsDialog.getJustVisualizeDate()) {
				// Number of frames = number of observations
				numFrames = parent.getNumObservations();
				timePoints = models.get(0).getOriginalTimePoints();
				useOriginalData = true;
			} else {
				// Take the number of observations from the dialog
				numFrames = settingsDialog.getNumFrames();
				timePoints = models.get(0).getDistributedTimePoints(numFrames);
				useOriginalData = false;
			}
			
//			for(int i=0; i<timePoints.length; i++) {
//				System.out.println("TimePoint" + i + ": " + timePoints[i]);
//			}

			// Initialize the view and the controller
			this.view = new TimeSeriesView(pathwayID, this, controller, parent.getIntegratorUI(), species);
			controller.setView(view);

			// Download pathway
			this.keggImporter = new KEGGImporter(pathwayID, Format.JPG);
			keggImporter.addActionListener(controller);		
			// Build and show the view
			parent.getIntegratorUI().addTab(view, "Film of " + parent.getName());
			
			//for the results of the thesis
			//exportModelledValues();
			

			// Create the pathwayPanel in which the colored pathway (graph) is visualized
			// Later, the pathwayPanel is given to the view to display it in the viewPort
			// The constructor of IntegratorPathwayPanel will run the keggImporter.
			pathwayPanel = new IntegratorPathwayPanel(keggImporter, controller);
		}
	}

	/**
	 * Generate the film. Generate single colored images of the pathway and encode them.
	 */
	void generateFilm()  {

		// This classes are used for coloring the pathway according to an arbitrary enrichment
		createInvisibleTranslatorPanel(pathway); // With that TranslatorPanel and we can use existing coloring methods
		visData = new VisualizeDataInPathway(transPanel, false);	 // VisualizeDataInPathway objects we can use already existing coloring methods

		// This worker generates the film.
		NotifyingWorker<List<List<EnrichmentObject<String>>>> filmGenerator = new NotifyingWorker<List<List<EnrichmentObject<String>>>>() {

			@Override
			protected List<List<EnrichmentObject<String>>> doInBackground() throws Exception {				
				// Call controller to set progress bar
				publish(new ActionEvent(this, numFrames, VTSAction.START_GENERATE_FILM.toString()));

				// Create an KEGGPathway enrichment
				try {
					enrich = new KEGGPathwayEnrichment(species, true, false, null);
				} catch (IOException e) {
					e.printStackTrace();
				}

				// Compute modelled mRNA values and their enrichment for each time point
				mRNA = new ArrayList<List<mRNA>>(numFrames);
				enrichments = new ArrayList<List<EnrichmentObject<String>>>(numFrames);
				
				try {
					for(int i = 0; i < numFrames; i++) {
						// Model mRNA data
						ArrayList<mRNA> modelValues = computeModelValues(timePoints[i]);
						mRNA.add(i, modelValues);
						
						// Compute the enrichment
						List<EnrichmentObject<String>> e = computeEnrichment(mRNA.get(i), mapFrameToTimePoint(i+1)); // because frame is 1 indexed
						enrichments.add(i, e);
						
						// Update the progress bar
						publish(new ActionEvent(transPanel.getDocument(), i, VTSAction.IMAGE_GENERATED.toString()));					
					}					
				} catch(Exception e) {
					e.printStackTrace();
				}

				// initialize the dimension
				colorPathway(enrichments.get(0), mRNA.get(0), mapFrameToTimePoint(1));
				Graph2D test = transPanel.getDocument();

				Rectangle rect = test.getBoundingBox();
				Double width = new Double(rect.getWidth());
				Double height = new Double(rect.getHeight());

				dimension = new Dimension(width.intValue(), height.intValue());

				return null;
			}



			@Override
			protected void done() {
				// Tell the listener, that film was created
				publish(new ActionEvent(this, 0, VTSAction.END_GENERATE_FILM.toString()));
			}	
		};

		// Generate film
		filmGenerator.addActionListener(controller);
		try{
			filmGenerator.execute();			
		} catch(Exception e) {
			// If there is an error, inform the user about that
			GUITools.showErrorMessage(view, "Cannot generate time series visualization.");
			controller.actionPerformed(new ActionEvent(e, 0, VTSAction.SHOW_VIDEO_FAILED.toString()));
		}
	}

	/**
	 * Compute a pathway enrichment at a certain time point.
	 * @param timePoint for which a pathway enrichment is computed
	 * @return the pathway enrichment
	 */
	private List<EnrichmentObject<String>> computeEnrichment(List<mRNA> modelValues, double timePoint) {

		// sort model values by their signal
		Collections.sort(modelValues, Signal.getComparator(generateExperimentName(timePoint), signalType));
		modelValues = filterGenes(modelValues, generateExperimentName(timePoint), cutoff, signalType);

		// compute the pathway enrichment for the given time point
		List<EnrichmentObject<String>> l=null;
		try {
			if(modelValues.size() != 0) {
				// TODO something throws a NullPointer exception here
//				if(enrich == null)
//					System.out.println("Enrich null");
				l = enrich.getEnrichments(modelValues, null, null, false);
			} else {
				GUITools.showErrorMessage(view, "There is no differential expressed gene for timepoint "
						+ String.format("%.2f", timePoint) + timeUnit + ". Maybe the cutoff value is too high.");
			}		
		} catch (Throwable e) {
			GUITools.showErrorMessage(null, e);
		}
		return l;
	}

	/**
	 * Generate an image of the pathway at a certain time point. The nodes are colored due to
	 * a KEGG pathway enrichment, which is also computed in this function.
	 * @param timePoint for that a pathway image is computed
	 * @return a pathway image
	 */
	private BufferedImage generatePathwayImage(int frame) {

		// Color the graph according to the current frame
		colorPathway(enrichments.get(frame-1), mRNA.get(frame-1), mapFrameToTimePoint(frame));

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

			// Add some time information to the image
			Graphics g = image.getGraphics();
			g.setColor(Color.BLACK);
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, image.getHeight() / 40));
			g.drawString(String.format("%.2f", mapFrameToTimePoint(frame)) + " " + timeUnit, 8 * image.getWidth() / 10, image.getHeight()/40);
		} catch (IOException e) {
			controller.actionPerformed(new ActionEvent(e, 0, VTSAction.SHOW_VIDEO_FAILED.toString()));
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
	private ArrayList<mRNA> filterGenes(List<mRNA> modelValues, String experimentName,
			double cutoff, SignalType signalType) {
		// list containing the filtered genes
		ArrayList<mRNA> filteredGenes = new ArrayList<mRNA>();

		// filter the genes
		if(signalType == SignalType.pValue) {
			for(mRNA m : modelValues) {
				if(m.getSignal(SignalType.pValue, experimentName).getSignal().doubleValue() < cutoff)
					filteredGenes.add(m);
			}
		} else if(signalType == SignalType.FoldChange) {
			for(mRNA m : modelValues) {
				if(Math.abs(m.getSignal(SignalType.FoldChange, experimentName).getSignal().doubleValue()) > cutoff)
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
	private ArrayList<mRNA> computeModelValues(double timePoint) {		
		ArrayList<mRNA> values = new ArrayList<mRNA>(models.size());

		// Generate the experiment name for the mRNA for the given time point
		// testing
//		System.out.println("Caller time point: " + timePoint);
		String experimentName = generateExperimentName(timePoint);

		// for each model, compute the value at the time point and build a mRNATimeSeries object
		// If the user wants to display the original data, just the original data is returned.
		for(TimeSeriesModel m : models) {
			double val = m.computeValueAtTimePoint(timePoint, useOriginalData);
			mRNA mrna = new mRNA(m.getName(), m.getGeneID());
			mrna.addSignal(val, experimentName , m.getSignalType()); // name of SignalColumn is arbitrary

			values.add(mrna);
		}

		return values;
	}

	/**
	 * Generate an experiment name. Can also be used to get the experiment name of an given
	 * timePoint to access a signal of a {@mRNA} object.
	 * @param timePoint
	 * @return
	 */
	private String generateExperimentName(double timePoint) {
		return "Modelled value at " + String.format("%.3f", timePoint);
	}

	/**
	 * Color pathway according to p-values or q-values of the given enrichment and the
	 * modelled expression p-values or foldChanges.
	 * @param e The enrichment data to visualize
	 * @param mRNA The mRNA data to visualize
	 * @param timePoint of the data
	 */
	private void colorPathway(List<EnrichmentObject<String>> e, List<mRNA> mRNA, double timePoint) {

		// Of which type is the data we want to visualize in the pathway?
		Class<? extends NameAndSignals> enrichDataType = NameAndSignals.getType(e);
		Class<? extends NameAndSignals> mRNADataType = null;
		if(mRNA != null) {
			mRNADataType = NameAndSignals.getType(mRNA);
		}

		// If the data type we want to visualize is already visualized in the graph,
		// we have to remove it. Otherwise there are incorrect additional information
		// while the mouse is over a node.
		// Also remove visualization  and color the pathway for one data type, after that
		// remove visualization and color the pathway for the other data type. Otherwise,
		// the data isn't correct visualized when playing the film.
		if(visData.isDataTypeVisualized(enrichDataType)) {
			visData.removeVisualization(enrichDataType); // int is for testing
		}

		// Use the default signal colorer for p-value or q-value
		SignalColor recolorer;
		if(visualizePValue)
			recolorer = new SignalColor(null, null, SignalType.pValue);
		else
			recolorer = new SignalColor(null, null, SignalType.qValue);
		
		// A describing string, which is shown if the mouse is over a colored node:
		// [Name of source file] at [time point] [time unit] (e.g. PB_single.csv at 24h)
		String description = source.getTabName() + " at " + mapFrameToTimePoint(controller.getCurFrame()) + timeUnit;

		// Color the graph. If there is no enriched pathway, color nothing.
		if(e != null) {
			if(visualizePValue)
				visData.visualizeData(e, description, "Enrichment", SignalType.pValue, recolorer);
			else
				visData.visualizeData(e, description, "Enrichment", SignalType.qValue, recolorer);
		}

		// Now deal with the modelled mRNA data! Remove the old visualization.
		if(visData.isDataTypeVisualized(mRNADataType)) {
			visData.removeVisualization(mRNADataType);
		}

		// And now color the graph according to the new data.
		visData.visualizeData(mRNA, source.getTabName(), generateExperimentName(timePoint), getSignalType());
	}


	/**
	 * Filter out null gene models. E.g models without valid GeneID.
	 * @param geneModels to be filtered
	 * @return filtered gene models
	 */
	private List<TimeSeriesModel> filterNullGeneModels(Collection<TimeSeriesModel> geneModels) {
		ArrayList<TimeSeriesModel> filteredModels = new ArrayList<TimeSeriesModel>();

		// filter the models
		for(TimeSeriesModel model : geneModels) {
			// filter out null models
			if(model != null)
				filteredModels.add(model);
		}	

		return filteredModels;
	}

	/**
	 * The method generates a {@link TranslatorPanel}. The panel is never shown to the user.
	 * The panel is just used to manipulate easily the graph of the pathway with existing functions.
	 * @param pathway to visualize
	 */
	private void createInvisibleTranslatorPanel(Graph2D pathway) {
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

	public void setVisualizePValue(boolean b) {
		visualizePValue = b;
	}

	/**
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

	public int getNumFrames() {
		return numFrames;
	}

	public Dimension getDimension() {
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

	/**
	 * Get the i-th frame of the film. 
	 * @param i the frame number
	 * @return the i-th frame of the film
	 */
	public Graph2D getFrame(int curFrame) {
		return getFrame(curFrame, true);
	}

	/**
	 * Get the i-th frame of the film. 
	 * @param i the frame number
	 * @param useNewContainer, if true use a new container (i.e. a previous frame should be returned)
	 * @return the i-th frame of the film
	 */
	public Graph2D getFrame(int i, boolean useNewContainer) {
		// Color the graph according to the enrichment
		colorPathway(enrichments.get(i-1), mRNA.get(i-1), mapFrameToTimePoint(i));

		return transPanel.getDocument();
	}

	/**
	 * Map second to the corresponging time point.
	 * (e.g mapSecondToTimePoint(0) will return the same value as getFirstTimePoint() )
	 * @param second, for which the corresponding time point is sought
	 * @return the corresponding time point according to the time unit
	 */
	public double mapSecondToTimePoint(double second) {
		// Do the map
		Double frame = new Double(second * frameRate);
		return timePoints[frame.intValue()];
	}

	/**
	 * Map a frame number to the corresponging time point.
	 * (e.g mapFrameToTimePoint(1) will return the same value as getFirstTimePoint() )
	 * @param frame, for which the corresponding time point is sought
	 * @return the corresponding time point according to the frame number
	 */
	public double mapFrameToTimePoint(int frame) {
		// Do the map. Frames are numbered from 1 to numFrames. But the timePoints
		// array is 0-indexed
		return timePoints[frame-1];
	}

	/**
	 * Map a frame number to the corresponging second in the film.
	 * (e.g mapFrameToTimePoint(0) will return 0 )
	 * @param frame, for which the corresponding second is sought
	 * @return the corresponding second according to the frame number
	 */
	public double mapFrameToSecond(int frame) {
		double frame2 = frame;
		double frameRate2 = frameRate; // with ints there are rounding errors

		return frame2 / frameRate2;
	}

	/**
	 * Return the pathway panel
	 */
	public IntegratorPathwayPanel getPathwayPanel() {
		return pathwayPanel;
	}
	
	/**
	 * Produce a tab separated output so one can take a look on the
	 * interpolated values
	 */
	public void exportModelledValues() {
		
		File f = new File("./output.txt");
		FileWriter fw = null;
		try {
			fw = new FileWriter(f);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// The header
		String s = "";
		s = s + "NCBIGeneID";
		for(int i = 1; i <= numFrames; i++) {
			s += "\t" + String.format("%.2f", mapFrameToTimePoint(i)) + timeUnit;
		}
		s += "\n";
		try {
			fw.write(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// For every model, print the name, geneID and the modeled values
		for(TimeSeriesModel m : models) {
			if(m != null) {
				s = m.getName();
				s += "\t" + String.valueOf(m.getGeneID());
				for(int i = 1; i <= numFrames; i++){
					s += "\t" + m.computeValueAtTimePoint(mapFrameToTimePoint(i), false);
				}
				s += "\n";
				try {
					fw.write(s);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}		
		}
		
		try {
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Export the generated film to file.
	 */
  public void exportFilm() {
    // You can add all codecs, which are supported by xuggler.
    // http://stackoverflow.com/questions/9727590/what-codecs-does-xuggler-support
    // The extension has to be in round brackets (the extension is parsed from
    // that)
    NotifyingWorker<Void> exporter = new NotifyingWorker<Void>() {

      @Override
      protected Void doInBackground() throws Exception {
        publish(new ActionEvent(this, numFrames,
            VTSAction.START_GENERATE_FILM.toString()));

        String format = ".gif";

        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            format + " files", format.substring(1)); // substring: because first
                                               // character is '.'

        // Let the user choose the output destination
        File saveDir = GUIOptions.SAVE_DIR.getValue(SBPreferences
            .getPreferencesFor(GUIOptions.class));
        JFileChooser fc = GUITools.createJFileChooser(saveDir.toString(),
            false, false, JFileChooser.FILES_ONLY, filter);

        if (fc.showSaveDialog(view) != JFileChooser.APPROVE_OPTION) {
          publish(new ActionEvent(this, 0, VTSAction.END_EXPORT_FILM.toString()));
          return null;
        }

        // Check file
        File f = fc.getSelectedFile();

        // Attach (correct) file extension.
        String path = FileTools.trimExtension(f.getPath()) + format;
        f = new File(path);

        // Check if file exists and is writable
        boolean showOverride = f.exists();
        if (!f.exists())
          try {
            f.createNewFile();
          } catch (IOException e) {
            GUITools.showErrorMessage(view, e);
            publish(new ActionEvent(this, 0,
                VTSAction.END_EXPORT_FILM.toString()));
            return null;
          }
        if (!f.canWrite() || f.isDirectory()) {
          GUITools.showNowWritingAccessWarning(view, f);
          publish(new ActionEvent(this, 0, VTSAction.END_EXPORT_FILM.toString()));
          return null;
        }

        // Overwrite existing file?
        if (showOverride && !GUITools.overwriteExistingFile(view, f)) {
          publish(new ActionEvent(this, 0, VTSAction.END_EXPORT_FILM.toString()));
          return null;
        }

        // generate first image to infer image type
        BufferedImage pathwayImage;
        pathwayImage = generatePathwayImage(1);
        ImageOutputStream outputStream = new FileImageOutputStream(f);
        // File exists, we can now write to it!
        GifSequenceWriter writer = new GifSequenceWriter(outputStream,
            pathwayImage.getType(), TIME_PER_FRAME, false);
        writer.writeToSequence(pathwayImage);

        // Generate frame for frame and encode them. The last frame has to be
        // handled manually. Why?
        // Read the comment a few lines below.
        for (int i = 2; i < numFrames; i++) {
          pathwayImage = generatePathwayImage(i); // first frame has the number
                                                  // 1

          writer.writeToSequence(pathwayImage);
          publish(new ActionEvent(this, i, VTSAction.IMAGE_GENERATED.toString()));
        }

        // Close the writer. Film is now succesfully exported.
        writer.close();
        outputStream.close();

        publish(new ActionEvent(this, 0, VTSAction.END_EXPORT_FILM.toString()));

        return null;
      }
    };
    exporter.addActionListener(controller);
    exporter.execute();
  }

	
	/**
	 * Select the BH correction method for multiple hypothesis testing
	 */
	public void setBH() {
		// Recompute the q-values for each time point
		BenjaminiHochberg bh = new BenjaminiHochberg();
		for(int i=0; i< numFrames; i++) {
			bh.setQvalue(enrichments.get(i));			
		}
	}

	
	/**
	 * Select the BFH correction method for multiple hypothesis testing
	 */
	public void setBFH() {
		// Recompute the q-values for each time point
		BonferroniHolm bfh = new BonferroniHolm();
		for(int i=0; i< numFrames; i++) {
			bfh.setQvalue(enrichments.get(i));			
		}
	}

	
	/**
	 * Select the BO correction method for multiple hypothesis testing
	 */
	public void setBO() {
		// Recompute the q-values for each time point
		Bonferroni bo = new Bonferroni();
		for(int i=0; i< numFrames; i++) {
			bo.setQvalue(enrichments.get(i));			
		}
	}
}

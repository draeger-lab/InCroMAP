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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
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

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

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
import de.zbit.util.objectwrapper.ValueTriplet;
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
	private double[] timePoints;
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
	private int duration; // this one is the user input! Don't confuse with filmDuration
	private int numFrames;
	// Dimension of the resulting film.
	private static Dimension dimension;
	// temporary film file
	private File tempFilmFile = new File("./longTestFilm.mp4"); // for testing porpuse.
	
	// Holds 'visualization' of the colored graph. This panel is never shown to the user.
	// A buffered image is generated from the colored graph. The image is then encoded into the film.
	private TranslatorPanel<Graph2D> transPanel;
	
	/**
	 * This container holds the finished film.
	 */
	private IContainer film;
	private IStreamCoder coder;
	private IConverter converter = null;
	private IVideoResampler resampler = null;

	private int videoStreamIndex;
	private double timeBase;
	// This one is the duration of the video stream (respective to the video streams time base)!
	private long filmDuration;
	
	
	
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
		
		// download pathway and generate film, if ok button was pressed. Otherwise, do nothing
		if(settingsDialog.okPressed()) {
			// Get the user input and filter genes
			this.pathwayID = settingsDialog.getSelectedPathwayID();
			this.duration = settingsDialog.getNumFrames();
			this.frameRate = settingsDialog.getFrameRate();
			this.cutoff = settingsDialog.getCutoff();
			this.models = filterNullGeneModels(parent.getGeneModels(), settingsDialog.getCutoff());
			
			// How many frames should be created? And what are their time points?
			if(settingsDialog.getJustVisualizeDate()) {
				// Number of frames = number of observations
				numFrames = parent.getNumObservations();
				timePoints = computeTimePoints(numFrames, true);
			} else {
				// Take the number of observations from the dialog
				numFrames = settingsDialog.getNumFrames();
				timePoints = computeTimePoints(numFrames, false);
			}
			
			
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
		
		// How many frames have we to generate and what time point corresponds to them?
		//final int numFrames = duration * frameRate;
		//final double[] timePoints = computeTimePoints(numFrames);
		
		// This worker generates the film.
		NotifyingWorker<IMediaWriter, Void> filmGenerator = new NotifyingWorker<IMediaWriter, Void>() {
			
			private IMediaWriter writer;

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
								
				// Compute the dimension of the film and initialize IMediaWriter which encodes the images.
				dimension = initializeDimension();
				writer = initializeWriter(dimension);
				
				// Generate frame for frame and encode them. The last frame has to be handled manually. Why?
				// Read the comment a few lines below.
				for(int i = 0; i < numFrames-1; i++) {
					long timestamp = (i * 1000)/frameRate;  // Timestamp, when image should be displayed in MILLIseconds
					System.out.println("Generate image: " + i + " timestamp: " + timestamp);
					
					BufferedImage image = generatePathwayImage(timePoints[i]);
					
					writer.encodeVideo(0, image, timestamp, TimeUnit.MILLISECONDS);
					fireActionEvent(new ActionEvent(transPanel.getDocument(), i, VTSAction.IMAGE_GENERATED.toString()));					
				}
				
				// Because the damn decoder fails to decode the last two frames, encode the last image
				// three times, so we can decode it later one time -.-
				// See also: https://groups.google.com/forum/#!topic/xuggler-users/FXgmW4dViF8
				int numLastFrame = numFrames-1;
				BufferedImage lastFrame = generatePathwayImage(timePoints[numLastFrame]);
				for(int j = 0; j < 3; j++) {
					long timestamp = ((numFrames-1) * 1000)/frameRate + j;  // Timestamp, when image should be displayed in MILLIseconds
					
					writer.encodeVideo(0, lastFrame, timestamp, TimeUnit.MILLISECONDS);
					System.out.println("Encoded image");
					
					// Set the progress bar when the last of the last frames was encoded.
					if(j == 2)
						fireActionEvent(new ActionEvent(transPanel.getDocument(), numLastFrame, VTSAction.IMAGE_GENERATED.toString()));
				}
						
				// Close the writer. Film is now saved in the temporary film file.
				writer.close();
				
				return null;
			}

			/**
			 * Create one test image and return its dimension.
			 * @return The dimension of the test image (and thus of the film)
			 */
			private Dimension initializeDimension() {
				BufferedImage example = generatePathwayImage(timePoints[0]);
				int width = example.getWidth();
				int height = example.getHeight();
				
				return new Dimension(width, height);
			}

			@Override
			protected void done() {
				// Tell the listener, that film was created
				fireActionEvent(new ActionEvent(this, 0, VTSAction.END_GENERATE_FILM.toString()));
			}

			// Initialize writer with the width and height of the graph
			private IMediaWriter initializeWriter(Dimension dimension) {			
				// Create the temporary film file
				try {
					tempFilmFile = File.createTempFile("tempFilmFile", ".mp4");
				} catch (IOException e) {
					// If the file can't be created, stop creating film
					controller.actionPerformed(new ActionEvent(e, 0, VTSAction.SHOW_VIDEO_FAILED.toString()));
				}
				
				// Create the writer, which encodes the single images. Also get an example image to obtain width and height
				// and add the video stream to the writer. The resulting film is in .mp4 format.
				final IMediaWriter writer = ToolFactory.makeWriter(tempFilmFile.getAbsolutePath());
				int width = new Double(dimension.getWidth()).intValue();
				int height = new Double(dimension.getHeight()).intValue();
				
				writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, width, height);
				
				return writer;
			}		
		};

		// Generate film
		filmGenerator.addActionListener(controller);
		try{
			filmGenerator.execute();			
		} catch(Exception e) {
			// If there is an error, inform the user about that
			System.out.println("There was an exception");
			e.printStackTrace();
			controller.actionPerformed(new ActionEvent(e, 0, VTSAction.SHOW_VIDEO_FAILED.toString()));
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
      l = enrich.getEnrichments(modelValues, null, null, true);
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
	  	
	  	// Add some time information to the image
	  	Graphics g = image.getGraphics();
	  	g.setColor(Color.BLACK);
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, image.getHeight() / 40));
			g.drawString(String.format("%.2f", timePoint) + " " + timeUnit, 10, image.getHeight()/40);
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
		SignalColor recolorer = new SignalColor(null, null, SignalType.pValue);

		// color the graph
		visData.visualizeData(l, "", "Enrichment", SignalType.pValue, recolorer);
		}


	/**
	 * @param numFrames the number of frames
	 * @param justObservations if true, the time points are the same as the time points of the observations.
	 * @return the time points for which the pathway should be modeled. 
	 */
	private double[] computeTimePoints(int numFrames, boolean justObservations) {
		timePoints = new double[numFrames];
		
		// If just the observations gets visualized, the result is an array of the
		// observation time points
		if(justObservations) {
			// The list of time points of the observations
			List<ValueTriplet<Double, String, SignalType>> vp = source.getTimePoints();
			
			// For each observation, get the time point
			for(int i=0; i < vp.size(); i++)
			 timePoints[i] = vp.get(i).getA();
			
		} else { // compute equidistant time points

			// get a sample TimeSeriesModel, first and last time point to model
			TimeSeriesModel m = models.toArray(new TimeSeriesModel[1])[0];
			double begin = m.getFirstTimePoint();
			double end = m.getLastTimePoint();
			double step = (end - begin) / (numFrames-1); // numFrames-1: first and last frame show first and last data column

			for(int i = 0; i < timePoints.length; i++) {
				timePoints[i] = begin + (i * step);
			}
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

	public IContainer getFilm() {
		return film;
	}

	/**
	 * Get the i-th frame of the film. 
	 * @param i the frame number
	 * @return the i-th frame of the film
	 */
	public BufferedImage getFrame(int curFrame) {
		return getFrame(curFrame, true);
	}
	
	/**
	 * Get the i-th frame of the film. 
	 * @param i the frame number
	 * @param useNewContainer, if true use a new container (i.e. a previous frame should be returned)
	 * @return the i-th frame of the film
	 */
	public BufferedImage getFrame(int i, boolean useNewContainer) {

		if(useNewContainer) {
		// try to reopen film (workaround, because using original opened film container failed)
			// close container and open it again
			// Or save one container, which is not used.
			film.close();
			if (film.open(tempFilmFile.getAbsolutePath(), IContainer.Type.READ, null) < 0)
				controller.actionPerformed(new ActionEvent(this, 0, VTSAction.SHOW_VIDEO_FAILED.toString()));
			
		// Compute (approximated) timestamp of the wanted frame
			// Seek to the frame before the wanted frame, so
			// that we don't miss the wanted frame in the seeking (because seeking finds frame i or i+1)
			//long approxedTimestamp = getTimestampOfFrame(i-1); // TODO Maybe just i ?
			
			// for testing
			//System.out.println("Approximated timestamp: " + approxedTimestamp);
			
			// Seek to the computed timestamp
			// Convert the approximated timestamp to a timestamp based on the time base
			// of the video stream
	  	//long seekTo = (long) (approxedTimestamp/1000.0/timeBase);
	            
	  	//System.out.println("Seek to: " + seekTo);
	  	//film.seekKeyFrame(videoStreamIndex, seekTo, IContainer.SEEK_FLAG_BACKWARDS); // TODO: Maybe another flag?
	  	IStream stream = film.getStream(videoStreamIndex);
	  	coder = stream.getStreamCoder();		 
	  	
	  	// Try to open the coder
	  	System.out.println("Try to open coder"); // for testing
	  	if (coder.open(null, null) < 0)
	  		throw new RuntimeException("could not open video decoder for container: "
	  				+controller.getFilePath());
	  	System.out.println("Opened Coder"); // for testing
			
		} else {// use the old container (i.e. show the next frame)
		}

		// The resampler for the video
		if(resampler == null) {
			if (coder.getPixelType() != IPixelFormat.Type.BGR24) {
				// if this stream is not in BGR24, we're going to need to
				// convert it. The VideoResampler does that for us.
				resampler = IVideoResampler.make(coder.getWidth(),
						coder.getHeight(), IPixelFormat.Type.BGR24,
						coder.getWidth(), coder.getHeight(), coder.getPixelType());
				if (resampler == null)
					throw new RuntimeException("could not create color space " +
							"resampler for: " + controller.getFilePath());
			}			
		}
	
		// walk through the container and build the IVideoPicture
		IPacket packet = IPacket.make();
		
		// Flag if we completely decoded the i-th frame as an IVideoPicture object
		boolean targetFrameComplete = false;
		
		// IVideoPicture timestamps are always in microseconds. An IVideoPicture with a
		// timestamp of 500000 will be displayed after a half second. There is some inacurracy
		// in the timestamps of IVideoPictures. A IVideoPicture which should be shown after
		// a half second (frame rate = 2) has a timestamp of 499992 (0,499992 s).
		// So we have to consider a certain inacurracy.
		long expectedPictureTimeStamp =  ((i-1) * 1000000) / frameRate; // in MICROseconds
		System.out.println("Expected IVideoPicture timestamp: " + expectedPictureTimeStamp);
		
		int count = 0;
		
		// Look at each packet		
		//while(e >= 0 && !targetFrameComplete) {
		while(!targetFrameComplete) {

			int e = film.readNextPacket(packet);
			
			// Get some information about the packet: for testing
			System.out.println("Read packet number " + (count++)); // for testing
			System.out.println(packet.toString());
					
			// As I said in a previous newsgroup post when a seek operation is requested via Container.seekKeyFrame AFTER Container.readNextPacket returns some negative number, it starts reading again. 
			// https://groups.google.com/forum/#!topic/xuggler-users/X0Z9YEmjZFw
			// TODO: Try: If once failed, reopen the container and try again?
			if(e < 0)  {
				System.out.println("Packet error occured: " + IError.make(e).getType());
				break;
			}
						
			if (packet.getStreamIndex() == videoStreamIndex) {
				// Build new picture
				System.out.println("Try to build IVideoPicture"); // for testing
				IVideoPicture picture = IVideoPicture.make(coder.getPixelType(),
						coder.getWidth(), coder.getHeight());
				System.out.println("Build IVideoPicture"); // for testing
				
				int offset = 0;
				
				while(offset < packet.getSize()) {
					// Decode the video.
					System.out.println("\tTry to decode packet");
					int bytesDecoded = coder.decodeVideo(picture, packet, offset); // <- FAILES HERE !!!
					System.out.println("\t Decoded " + bytesDecoded + " bytes"); // for testing
					if (bytesDecoded < 0)
						throw new RuntimeException("got error decoding video in: "
								+ controller.getFilePath());
					offset += bytesDecoded;

					/*
					 * Some decoders will consume data in a packet, but will not be able to construct
					 * a full video picture yet. Therefore you should always check if you
					 * got a complete picture from the decoder
					 */
					System.out.println("\t Picture complete? " + picture.isComplete());
					System.out.println("\t Picture timestamp: " + picture.getTimeStamp());
										
					// We don't have to check picture.isComplete(), because picture.getTimeStamp() returns Global.NO_PTS
					if((Math.abs(picture.getTimeStamp() - expectedPictureTimeStamp) < 100)
							&& picture.isComplete()) {
						targetFrameComplete = true;
						System.out.println("Target frame complete");
					} else {
						System.out.println("Target frame isn't complete");
					}
					
					//if (picture.isComplete() && picture.getTimeStamp() == accurateTimestamp) {
					if (targetFrameComplete) {
						IVideoPicture newPic = picture;
						/*
						 * If the resampler is not null, that means we didn't get the
						 * video in BGR24 format and
						 * need to convert it into BGR24 format.
						 */
						if (resampler != null) {
							// Test how much time it takes to resample the IVideoPicture
							long begin = System.currentTimeMillis();
							// we must resample
							newPic = IVideoPicture.make(resampler.getOutputPixelFormat(),
									picture.getWidth(), picture.getHeight());
							if (resampler.resample(newPic, picture) < 0)
								throw new RuntimeException("could not resample video from: "
										+ controller.getFilePath());
							long end = System.currentTimeMillis();
							System.out.println("\tTime to resample IVideoPicture: " + (end-begin) + " ms");
						}
						if (newPic.getPixelType() != IPixelFormat.Type.BGR24)
							throw new RuntimeException("could not decode video" +
									" as BGR 24 bit data in: " + controller.getFilePath());
						

						// And finally, convert the BGR24 to an Java buffered image
						BufferedImage frame = new BufferedImage(coder.getWidth(), coder.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
						long begin = System.currentTimeMillis();
						
						if(converter == null)
							converter = ConverterFactory.createConverter(frame, IPixelFormat.Type.BGR24);
						
						frame = converter.toImage(newPic);
						long end = System.currentTimeMillis();
						System.out.println("\tTime to convert image: " + (end-begin) + " ms)");
						
						System.out.println("Return the " + i + "-th frame.");
						return frame;
					}
				}
			} else {
				System.out.println("Packet doesn't belong to stream or timestamp"); // for testing
			}
		}
		
		
		return null;
	}

//	/**
//	 * Compute the approximated timestamp of the i-th frame in milliseconds.
//	 * Returns always non-negativa values.
//	 * @param i the sought after i-th frame
//	 * @return timestamp in milliseconds when the i-th frame is shown in the film.
//	 */
//	private long getTimestampOfFrame(int i) {
//		// Compute timestamp in MILLIsecons, when the i-th frame should appear
//		long timestamp = (i * 1000) / frameRate;
//
//		// Don't return a negative time stamp
//		if(timestamp > 0)
//			return timestamp;
//		else // negative time stamp
//			return 0;
//	}

	/**
	 * Load the film from the temporary film file.
	 */
	public void loadFilmFromTempFile() {
		film = IContainer.make();
		// we make an attempt to open up the container
		if (film.open(tempFilmFile.getAbsolutePath(), IContainer.Type.READ, null) < 0)
		//if (film.open("./longTestFilm.mp4", IContainer.Type.READ, null) < 0)
			controller.actionPerformed(new ActionEvent(this, 0, VTSAction.SHOW_VIDEO_FAILED.toString()));
		
		// for testing: print information about the streams in the container.
		testIContainer(film);
	}
	
	/**
	 * Print all available information of an IContainer object and its streams
	 * to the console.
	 * Taken from: http://www.javacodegeeks.com/2011/02/introduction-xuggler-video-manipulation.html
	 */
	private void testIContainer(IContainer container) {

		// query how many streams the call to open found
		int numStreams = container.getNumStreams();

		// query for the total duration
		long duration = container.getDuration();

		// query for the file size
		long fileSize = container.getFileSize();

		// query for the bit rate
		long bitRate = container.getBitRate();

		System.out.println("Number of streams: " + numStreams);
		System.out.println("Duration (ms): " + duration);
		System.out.println("File Size (bytes): " + fileSize);
		System.out.println("Bit Rate: " + bitRate);

		// iterate through the streams to print their meta data
		for (int i=0; i<numStreams; i++) {

			// find the stream object
			IStream stream = container.getStream(i);

			// get the pre-configured decoder that can decode this stream;
			IStreamCoder coder = stream.getStreamCoder();

			System.out.println("*** Start of Stream Info ***");

			System.out.printf("stream %d: ", i);
			System.out.printf("type: %s; ", coder.getCodecType());
			System.out.printf("codec: %s; ", coder.getCodecID());
			System.out.printf("duration: %s; ", stream.getDuration());
			System.out.printf("start time: %s; ", container.getStartTime());
			System.out.printf("timebase: %d/%d; ",
					stream.getTimeBase().getNumerator(),
					stream.getTimeBase().getDenominator());
			System.out.printf("coder tb: %d/%d; ",
					coder.getTimeBase().getNumerator(),
					coder.getTimeBase().getDenominator());
			System.out.println();

			if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
				System.out.printf("sample rate: %d; ", coder.getSampleRate());
				System.out.printf("channels: %d; ", coder.getChannels());
				System.out.printf("format: %s", coder.getSampleFormat());
			} 
			else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
				System.out.printf("width: %d; ", coder.getWidth());
				System.out.printf("height: %d; ", coder.getHeight());
				System.out.printf("format: %s; ", coder.getPixelType());
				System.out.printf("frame-rate: %5.2f; ", coder.getFrameRate().getDouble());
			}

			System.out.println();
			System.out.println("*** End of Stream Info ***");
		}
	}

	/**
	 * Delete the temporary film file, because we don't need that any more.
	 */
	public void deleteTempFilmFile() {
		// If that is not succesful, the film will be deleted in the future, because
		// its a temporary file (... I hope so)
		tempFilmFile.delete();
	}

	/**
	 * Get some important information of the film like</br>
	 * - index of the video stream in the container</br>
	 * - duration and time base of the video stream
	 */
	public void lookupStreamInformation() {
		// Get the number of streams in the IContainer
		// (usually there is just one stream, but strange things happen)
		int numStreams = film.getNumStreams();
		
		// Search for the first video stream and remember its index
		int videoStreamId = -1;
		IStreamCoder videoCoder = null;
		for(int i = 0; i < numStreams; i++) {
			// Find the stream object
			IStream stream = film.getStream(i);
			// Get the pre-configured decoder that can decode this stream;
			IStreamCoder coder = stream.getStreamCoder();

			if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO)
			{
				videoStreamId = i;
				videoCoder = coder;
				break;
			}
		}
		
		// If theres no video stream, we have nothing to show :(
		if (videoStreamId == -1)
			controller.actionPerformed(new ActionEvent(this, 0, VTSAction.SHOW_VIDEO_FAILED.toString()));
		else
			videoStreamIndex = videoStreamId;
		
		// Now get the duration of the video stream
		IStream stream = film.getStream(videoStreamIndex);
		filmDuration = stream.getDuration();		
		
		// At last the time base of the video stream (how many 'ticks' in a second)
		timeBase = stream.getTimeBase().getDouble();
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
	 * (e.g mapFrameToTimePoint(0) will return the same value as getFirstTimePoint() )
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
		System.out.println("Map Frame to Second: " + frame2 / frameRate2);
		System.out.println("Framerate to compute: " + frameRate2);
		return frame2 / frameRate2;
	}


}

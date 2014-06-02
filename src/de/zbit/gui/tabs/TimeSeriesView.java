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

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import y.view.Graph2D;
import y.view.Graph2DView;
import y.view.Graph2DViewMouseWheelZoomListener;

import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.ICodec;
import de.zbit.graph.RestrictedEditMode;
import de.zbit.gui.IntegratorUI;
import de.zbit.io.FileDownload;
import de.zbit.util.Species;
import de.zbit.util.progressbar.AbstractProgressBar;
import de.zbit.visualization.VisualizeTimeSeries;
import de.zbit.visualization.VisualizeTimeSeriesListener;

/**
 * The Panel that is used to display and manipulate a film of a pathway.
 * So this class corresponds to the view of the MVC-Pattern.
 * The film is generated in the class {@link VisualizeTimeSeries}.
 * 
 * @author Felix Bartusch
 * @version $Rev$
 */

public class TimeSeriesView extends IntegratorTab<IContainer> {
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
	
	Graph2DView graphView;
	
	public TimeSeriesView(String keggPathway, VisualizeTimeSeries filmGenerator,
			VisualizeTimeSeriesListener controller, IntegratorUI integratorUI, Species species) {
		super(integratorUI, filmGenerator.getFilm(), species);
		this.controller = controller;	
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


	@Override
	public JComponent getVisualization() {
		JPanel panel = new JPanel();
		panel.add(new JTextField("Test for getVisualization in Time SeriesView."));
		
		return panel;
	}

	@Override
	public void createJToolBarItems(JToolBar bar) {
		// TODO Auto-generated method stub	
	}

	@Override
	public Object getObjectAt(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getSelectedIndices() {
		// TODO Auto-generated method stub
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
  
  /**
   * Testing the visualization of a graph. So I can see the result of manipulating the graph
   * in the class VisualizeTimeSeries
   * @param graph
   */
  public void showGraph(Graph2D graph) {
  	// set the graph
  	graphView = new Graph2DView(graph);
  	
  	// show the graph in the view of the ScrollPanel
  	
    // Show Navigation and Overview
    RestrictedEditMode.addOverviewAndNavigation(graphView);
    
    graphView.setSize(getSize());
    //ViewMode mode = new NavigationMode();
    //pane.addViewMode(mode);
    //ActionListener listener = getUIActionListener();
    
    //EditMode editMode = new RestrictedEditMode(listener, this);
    //editMode.showNodeTips(true);
    //graphView.addViewMode(editMode);
    
    graphView.getCanvasComponent().addMouseWheelListener(new Graph2DViewMouseWheelZoomListener());
    try {
    	graphView.fitContent(true);
    } catch (Throwable t) {} // Not really a problem
    graphView.setFitContentOnResize(true);
  	
  	this.viewport.setView(graphView);
  	repaint();
  }

	public void initializeAndShowFilm(IContainer film) {
		// Set the data (the film)
		this.data = film;
		
		// TODO: Show the film (work with the IContainer)
		// Try to open file
		
    // first we create a Xuggler container object
    IContainer container = IContainer.make();
    
    // we attempt to open up the container
    int result = container.open(controller.getFilePath(), IContainer.Type.READ, null);
    
    // check if the operation was successful
    if (result<0)
        throw new RuntimeException("Failed to open media file");
    
    // print information about the streams in the container
    testIContainer(container);
    
//    // search first video stream
//    int numStreams = container.getNumStreams();
//    int videoStreamId = -1;
//    IStreamCoder videoCoder = null;
//    for(int i = 0; i < numStreams; i++)
//    {
//      // Find the stream object
//      IStream stream = container.getStream(i);
//      // Get the pre-configured decoder that can decode this stream;
//      IStreamCoder coder = stream.getStreamCoder();
//
//      if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO)
//      {
//        videoStreamId = i;
//        videoCoder = coder;
//        break;
//      }
//    }
//    if (videoStreamId == -1)
//      throw new RuntimeException("could not find video stream in container: "
//          +controller.getFilePath());
//
//    /*
//    * Now we have found the video stream in this file. Let's open up our decoder so it can
//    * do work.
//    */
//    if (videoCoder.open() < 0)
//    	throw new RuntimeException("could not open video decoder for container: "
//    			+controller.getFilePath());
//
//    IVideoResampler resampler = null;
//    if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
//      // if this stream is not in BGR24, we're going to need to
//      // convert it. The VideoResampler does that for us.
//      resampler = IVideoResampler.make(videoCoder.getWidth(),
//          videoCoder.getHeight(), IPixelFormat.Type.BGR24,
//          videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
//      if (resampler == null)
//        throw new RuntimeException("could not create color space " +
//         "resampler for: " + controller.getFilePath());
//    }
//    
//    /*
//    * And once we have that, we draw a window on screen
//    */
//    openJavaWindow();

    
        
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
}

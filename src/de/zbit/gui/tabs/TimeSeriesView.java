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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import y.view.Graph2D;
import y.view.Graph2DView;
import y.view.Graph2DViewMouseWheelZoomListener;

import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.Utils;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import de.zbit.graph.RestrictedEditMode;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.customcomponents.FilmControlPanel;
import de.zbit.gui.customcomponents.FilmPanel;
import de.zbit.io.FileDownload;
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
	
	private JLabel curFrame;

	Graph2DView graphView;

	private FilmPanel filmPanel;
	private FilmControlPanel controlPanel;

	public TimeSeriesView(String keggPathway, VisualizeTimeSeries model,
			VisualizeTimeSeriesListener controller, IntegratorUI integratorUI, Species species) {
		super(integratorUI, null, species); // There's no film at this point, so data is null
		this.controller = controller;
		this.model = model;
		
		// We always need scroll panes TODO: ?
		setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	}

	@Override
	public JComponent getVisualization() {
		// If the film isn't generated yet, return an empty panel.
		if(data == null) {
			JPanel panel = new JPanel();
			return panel;	
		}

		// Otherwise, set up the view. For that, build a JPanel which shows the film images
		filmPanel = new FilmPanel(model.getDimension());

		// Build the control panel. It is shown as the column header of this scroll pane
		controlPanel = new FilmControlPanel(controller);
		setColumnHeaderView(controlPanel);
		
		return filmPanel;
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
	public void createJToolBarItems(JToolBar bar) {
		// TODO Auto-generated method stub	
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

	/**
	 * 
	 * @param film
	 */
	public void initializeAndShowFilm(final Dimension filmDimension) {
		
		// TODO: Show the film (work with the IContainer)
		IContainer container = model.getFilm();
		// we attempt to open up the container
//		if (container.open(controller.getFilePath(), IContainer.Type.READ, null) < 0)
//			throw new RuntimeException("Failed to open media file");
		
		this.data = container;

		// print information about the streams in the container. for testing
		testIContainer(container);

		// What is the dimension of the film?
		getVisualization();
		final BufferedImage firstFrame = getFirstFrame(container);
		
		// Set the ViewportView of this ScrollPanel and show first frame in ORIGINAL size
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {
				// Show the first frame
				filmPanel.showNextFrame(firstFrame);
				// Enable the previous frame functionality because view shows the first frame
				controlPanel.enablePrevButton(false);
				System.out.println("After setting the first original frame:"); // for testing: is this valid?
				filmPanel.isPanelValid();
				
				// Set the ViewportView
				setViewportView(filmPanel);
				validate(); // validate the this panel
				System.out.println("Is the scrollPane valid? " + isValid()); // for testing: is the viewport valid?
				filmPanel.isPanelValid();
				
				System.out.println(filmPanel.getVisibleRect()); // for testing: Get the width and height of the frames visible area
				System.out.println(getViewport().getExtentSize()); // for testing: Same as filmPanel.getVisibleRect();
			}
		});

		// The filmPanel also reacts on MouseWheelEvents (zooms in/out)
		addMouseWheelListener(filmPanel);
		
		// Resize the original image, so that the whole graph is visible.
		EventQueue.invokeLater(new Runnable() {	
			@Override
			public void run() {				
				// At first, get the width and height of the visible area
				//Rectangle rect = filmPanel.getVisibleRect();
				//System.out.println(rect);
				//Dimension viewportSize = new Dimension(rect.getWidth(), rect.getHeight());
				Dimension visibleArea = getViewport().getExtentSize();
				System.out.println(visibleArea); // for testing
				System.out.println("imageSize: " + filmDimension);
				
				// The ratio between the viewport size and the frame size.
				double widthRatio = (visibleArea.getWidth() - 20) / filmDimension.getWidth();
				double heightRatio = (visibleArea.getHeight() - 20) / filmDimension.getHeight();
				
				// Zoom factor is 1, if the original frame can be displayed in the viewport, < 1 otherwise
				double zoomFactor = Math.min(1, Math.min(widthRatio, heightRatio));
				
				System.out.println("WidthRatio: " + widthRatio);
				System.out.println("HeightRatio: " + heightRatio);
				System.out.println("ZoomFactor: " + zoomFactor);
				
				filmPanel.showNextFrame(firstFrame, zoomFactor);
				
				System.out.println("After setting the resized frame:");
				filmPanel.isPanelValid();
				System.out.println("Is scrollPane valid? " + isValid());
				
				System.out.println(filmPanel.getVisibleRect());
				
				filmPanel.printImageDimension();
			}
		});
		
		


//		// Search for the first video stream and its coder
//		int numStreams = container.getNumStreams();
//		int videoStreamId = -1;
//		IStreamCoder videoCoder = null;
//		for(int i = 0; i < numStreams; i++) {
//			// Find the stream object
//			IStream stream = container.getStream(i);
//			// Get the pre-configured decoder that can decode this stream;
//			IStreamCoder coder = stream.getStreamCoder();
//
//			if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO)
//			{
//				videoStreamId = i;
//				videoCoder = coder;
//				break;
//			}
//		}
//		if (videoStreamId == -1)
//			throw new RuntimeException("could not find video stream in container: "
//					+controller.getFilePath());
//
//		System.out.println("Video stream found: streamID " + videoStreamId); // for testing
//
//		System.out.println("Try to open video coder."); // for testing
//		
//		// Now try to open the coder
//		if (videoCoder.open(null, null) < 0)
//			throw new RuntimeException("could not open video decoder for container: "
//					+controller.getFilePath());
//		
//		System.out.println("Opened video coder."); // for testing
//		System.out.println("Try to generate resampler."); // for testing
//
//		IVideoResampler resampler = null;
//		if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24)
//		{
//			// if this stream is not in BGR24, we're going to need to
//			// convert it. The VideoResampler does that for us.
//			resampler = IVideoResampler.make(videoCoder.getWidth(),
//					videoCoder.getHeight(), IPixelFormat.Type.BGR24,
//					videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
//			if (resampler == null)
//				throw new RuntimeException("could not create color space " +
//						"resampler for: " + controller.getFilePath());
//		}
//		System.out.println("Generated resampler."); // for testing
//		
//		System.out.println("Try to build image."); // for testing
//
//		// walk through the container and build the first image
//		IPacket packet = IPacket.make();
//		long firstTimestampInStream = Global.NO_PTS;
//		long systemClockStartTime = 0;
//		// Look at each packet
//		int counter = 0;
//		
//		boolean isFirstFrame = true;
//		
//		while(container.readNextPacket(packet) >= 0 && isFirstFrame) {
//			System.out.println("Read packet number " + ++counter); // for testing
//			
//			// Does the packet belongs to our stream?
//			if (packet.getStreamIndex() == videoStreamId) {
//				System.out.println("Packet belongs to our stream."); // for testing
//				// Build new picture
//				System.out.println("Try to build IVideoPicture object."); // for testing
//				IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
//						videoCoder.getWidth(), videoCoder.getHeight());
//				System.out.println("Build IVideoPicture object. "); // for testing
//
//				int offset = 0;
//				System.out.println("offset: " + offset + " Bytes");
//				System.out.println("Packet size : " + packet.getSize() + " Bytes");
//				while(offset < packet.getSize()) {
//					// Decode the video. Check for errors.
//					System.out.println("Try to decode packet."); // for testing
//					int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
//					System.out.println("\t Decoded " + bytesDecoded + " Bytes."); // for testing
//					if (bytesDecoded < 0)
//						throw new RuntimeException("got error decoding video in: "
//								+ controller.getFilePath());
//					offset += bytesDecoded;
//					System.out.println("\t New offset: " + offset + " Bytes.");
//
//					/*
//					 * Some decoders will consume data in a packet, but will not be able to construct
//					 * a full video picture yet. Therefore you should always check if you
//					 * got a complete picture from the decoder
//					 */
//					System.out.println("\t Picture complete? " + picture.isComplete());
//					if (picture.isComplete()) {
//						System.out.println("Picture is complete."); // for testing
//						IVideoPicture newPic = picture;
//						/*
//						 * If the resampler is not null, that means we didn't get the
//						 * video in BGR24 format and
//						 * need to convert it into BGR24 format.
//						 */
//						if (resampler != null) {
//							// we must resample
//							System.out.println("Try to resample IVideoPicture."); // for testing
//							newPic = IVideoPicture.make(resampler.getOutputPixelFormat(),
//									picture.getWidth(), picture.getHeight());
//							if (resampler.resample(newPic, picture) < 0)
//								throw new RuntimeException("could not resample video from: "
//										+ controller.getFilePath());
//						}
//						if (newPic.getPixelType() != IPixelFormat.Type.BGR24)
//							throw new RuntimeException("could not decode video" +
//									" as BGR 24 bit data in: " + controller.getFilePath());
//
//						if (firstTimestampInStream == Global.NO_PTS) {
//							// This is our first time through
//							firstTimestampInStream = picture.getTimeStamp();
//							// get the starting clock time so we can hold up frames
//							// until the right time.
//							systemClockStartTime = System.currentTimeMillis();
//						} else {
//							long systemClockCurrentTime = System.currentTimeMillis();
//							long millisecondsClockTimeSinceStartofVideo =
//									systemClockCurrentTime - systemClockStartTime;
//							// compute how long for this frame since the first frame in the
//							// stream.
//							// remember that IVideoPicture and IAudioSamples timestamps are
//							// always in MICROSECONDS,
//							// so we divide by 1000 to get milliseconds.
//							long millisecondsStreamTimeSinceStartOfVideo =
//									(picture.getTimeStamp() - firstTimestampInStream)/1000;
//							final long millisecondsTolerance = 50; // and we give ourselfs 50 ms of tolerance
//							final long millisecondsToSleep =
//									(millisecondsStreamTimeSinceStartOfVideo -
//											(millisecondsClockTimeSinceStartofVideo +
//													millisecondsTolerance));
//							if (millisecondsToSleep > 0) {
//								try {
//									Thread.sleep(millisecondsToSleep);
//								} catch (InterruptedException e) {
//									return;
//								}
//							}
//						}
//
//						// And finally, convert the BGR24 to an Java buffered image
//						System.out.println("Try build converter."); // for testing
//						//IConverter converter = ConverterFactory.createConverter("converter", newPic); // <- Error!
//						BufferedImage javaImage = new BufferedImage(videoCoder.getWidth(), videoCoder.getHeight(), BufferedImage.TYPE_3BYTE_BGR); 
//				    IConverter converter = ConverterFactory.createConverter(javaImage, IPixelFormat.Type.BGR24);
//						
//						System.out.println("Try convert to BufferedImage."); // for testing
//						javaImage = converter.toImage(newPic);
//						//BufferedImage javaImage = Utils.videoPictureToImage(newPic);
//
//						System.out.println("Show next Frame."); // for testing
//						//setImage(javaImage);
//						isFirstFrame = false;
//						filmPanel.showNextFrame(javaImage);
//						setViewportView(filmPanel);
//						//getViewport().getView().repaint();
//						//repaint();
//					}
//				}
//			} else {
//				/*
//				 * This packet isn't part of our video stream, so we just
//				 * silently drop it.
//				 */
//				do {} while(false);
//			}
//		}
//
//
//


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

		// for testing
		// getViewport().getExtentSize() -> 0;0
		// this.getViewport().getViewSize() gives size of the whole image
		// this.getViewport().getSize(); -> 0;0
		// this.getViewport().getPreferredSize() -> gives size of the whole image
		// this.getSize() -> 0;0
		// mit rect: 0;0
		//getPreferredScrollableViewportSize
//		System.out.println("Is filmPanel valid? " + filmPanel.isValid());
//		Rectangle rect = null;
//		EventQueue.invokeLater(new Runnable() {		
//			@Override
//			public void run() {
//				Rectangle rect = filmPanel.getVisibleRect();
//				System.out.println("Wdth: " + rect.width);
//				System.out.println("Height: " + rect.height);
//			}
//		});
//		//Rectangle rect = this.getVisibleRect();
//		

		System.out.println("End initialize and Show Film");
		
		
	}

	/**
	 * Return the first picture of the film.
	 * @param container which contains the film
	 * @return
	 */
	private BufferedImage getFirstFrame(IContainer container) {
		BufferedImage javaImage = null;
		
		int numStreams = container.getNumStreams();
		int videoStreamId = -1;
		IStreamCoder videoCoder = null;
		for(int i = 0; i < numStreams; i++) {
			// Find the stream object
			IStream stream = container.getStream(i);
			// Get the pre-configured decoder that can decode this stream;
			IStreamCoder coder = stream.getStreamCoder();

			if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO)
			{
				videoStreamId = i;
				videoCoder = coder;
				break;
			}
		}
		if (videoStreamId == -1)
			throw new RuntimeException("could not find video stream in container: "
					+controller.getFilePath());
		
		// Now try to open the coder
		if (videoCoder.open(null, null) < 0)
			throw new RuntimeException("could not open video decoder for container: "
					+controller.getFilePath());
		
		// for testing
		System.out.println("\tCoder codec: " + videoCoder.getCodec());
		System.out.println("\tCoder codec ID: " + videoCoder.getCodecID());
		System.out.println("\tCoder height: " + videoCoder.getHeight());
		System.out.println("\tCoder width: " + videoCoder.getWidth());
		System.out.println("\tCoder direction: " + videoCoder.getDirection());			

		IVideoResampler resampler = null;
		if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24)
		{
			// if this stream is not in BGR24, we're going to need to
			// convert it. The VideoResampler does that for us.
			resampler = IVideoResampler.make(videoCoder.getWidth(),
					videoCoder.getHeight(), IPixelFormat.Type.BGR24,
					videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
			if (resampler == null)
				throw new RuntimeException("could not create color space " +
						"resampler for: " + controller.getFilePath());
		}
		System.out.println("Generated resampler."); // for testing
		
		System.out.println("Try to build image."); // for testing

		// walk through the container and build the first image
		IPacket packet = IPacket.make();
		long firstTimestampInStream = Global.NO_PTS;
		long systemClockStartTime = 0;
		// Look at each packet
		
		boolean isFirstFrame = true;
		
		while(container.readNextPacket(packet) >= 0 && isFirstFrame) {
			
			System.out.println(packet.toString());
			
			// Does the packet belongs to our stream?
			if (packet.getStreamIndex() == videoStreamId) {
				// Build new picture
				IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(),
						videoCoder.getWidth(), videoCoder.getHeight());
				
				int offset = 0;

				while(offset < packet.getSize()) {
					// Decode the video. Check for errors.
					int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
					if (bytesDecoded < 0)
						throw new RuntimeException("got error decoding video in: "
								+ controller.getFilePath());
					offset += bytesDecoded;
					System.out.println("\t New offset: " + offset + " Bytes.");

					/*
					 * Some decoders will consume data in a packet, but will not be able to construct
					 * a full video picture yet. Therefore you should always check if you
					 * got a complete picture from the decoder
					 */
					System.out.println("\t Picture complete? " + picture.isComplete());
					if (picture.isComplete()) {
						IVideoPicture newPic = picture;
						/*
						 * If the resampler is not null, that means we didn't get the
						 * video in BGR24 format and
						 * need to convert it into BGR24 format.
						 */
						if (resampler != null) {
							// we must resample
							newPic = IVideoPicture.make(resampler.getOutputPixelFormat(),
									picture.getWidth(), picture.getHeight());
							if (resampler.resample(newPic, picture) < 0)
								throw new RuntimeException("could not resample video from: "
										+ controller.getFilePath());
						}
						if (newPic.getPixelType() != IPixelFormat.Type.BGR24)
							throw new RuntimeException("could not decode video" +
									" as BGR 24 bit data in: " + controller.getFilePath());

						if (firstTimestampInStream == Global.NO_PTS) {
							// This is our first time through
							firstTimestampInStream = picture.getTimeStamp();
							// get the starting clock time so we can hold up frames
							// until the right time.
							systemClockStartTime = System.currentTimeMillis();
						} else {
							long systemClockCurrentTime = System.currentTimeMillis();
							long millisecondsClockTimeSinceStartofVideo =
									systemClockCurrentTime - systemClockStartTime;
							// compute how long for this frame since the first frame in the
							// stream.
							// remember that IVideoPicture and IAudioSamples timestamps are
							// always in MICROSECONDS,
							// so we divide by 1000 to get milliseconds.
							long millisecondsStreamTimeSinceStartOfVideo =
									(picture.getTimeStamp() - firstTimestampInStream)/1000;
							final long millisecondsTolerance = 50; // and we give ourselfs 50 ms of tolerance
							final long millisecondsToSleep =
									(millisecondsStreamTimeSinceStartOfVideo -
											(millisecondsClockTimeSinceStartofVideo +
													millisecondsTolerance));
							if (millisecondsToSleep > 0) {
								try {
									Thread.sleep(millisecondsToSleep);
								} catch (InterruptedException e) {
								}
							}
						}

						// And finally, convert the BGR24 to an Java buffered image
						System.out.println("Try build converter."); // for testing
						//IConverter converter = ConverterFactory.createConverter("converter", newPic); // <- Error!
						javaImage = new BufferedImage(videoCoder.getWidth(), videoCoder.getHeight(), BufferedImage.TYPE_3BYTE_BGR); 
				    IConverter converter = ConverterFactory.createConverter(javaImage, IPixelFormat.Type.BGR24);
						
						javaImage = converter.toImage(newPic);
						//BufferedImage javaImage = Utils.videoPictureToImage(newPic);

						isFirstFrame = false;
					}
				}
			} else {
				/*
				 * This packet isn't part of our video stream, so we just
				 * silently drop it.
				 */
				do {} while(false);
			}
		}
		
		return javaImage;
	}

	/**
	 * Get the dimension of the first video stream in the {@link IContainer}
	 * @param data
	 * @return The dimension of the first video stream
	 */
	private Dimension getDimensionOfFirstVideoStream(IContainer container) {
		Dimension res = null;

		// query how many streams the call to open found
		int numStreams = container.getNumStreams();

		// Search for the first video stream and get its dimension
		for (int i=0; i<numStreams; i++) {

			// find the stream object
			IStream stream = container.getStream(i);

			// get the pre-configured decoder that can decode this stream;
			IStreamCoder coder = stream.getStreamCoder();

			// Is this a video codec?
			if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
				int width = coder.getWidth();
				int height = coder.getHeight();

				// Is the coder able to tell us width and height?
				if(width < 0 || height < 0)
					GUITools.showErrorMessage(this, "Can't detect dimension of the film.");
				else
					res = new Dimension(width, height);
			}
		}

		return res;
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
	 * Show the next Frame.
	 * @param nextFrame
	 */
	public void showNextFrame(BufferedImage nextFrame) {
		filmPanel.showNextFrame(nextFrame);
	}
	
	public void setImage(Image image) {
    //SwingUtilities.invokeLater(new ImageRunnable(image));
		ImageIcon imageIcon = new ImageIcon(image);
		curFrame = new JLabel(imageIcon);
		setViewportView(curFrame);
  }

	/**
	 * Enables / Disables the functionality to show the next frame.
	 * Is called, when model cannot deliver the next frame
	 * (i.e view shows currently the last frame)
	 * @param b can the model deliver a next frame?
	 */
	public void enableNextFrameFunctionality(boolean b) {
		// Gray out the next button of the film panel
		controlPanel.enableNextButton(b);
	}
	
	/**
	 * Enables / Disables the functionality to show the previous frame.
	 * Is called, when model cannot deliver the previous frame
	 * (i.e view shows currently the first frame)
	 * @param b can the model deliver a previous frame?
	 */
	public void enablePrevFrameFunctionality(boolean b) {
		// Gray out the next button of the film panel
		controlPanel.enablePrevButton(b);
	}
}

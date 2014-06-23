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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import y.view.Graph2D;
import y.view.Graph2DView;
import y.view.Graph2DViewMouseWheelZoomListener;

import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.ICodec;
import de.zbit.graph.RestrictedEditMode;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.customcomponents.FilmControlPanel;
import de.zbit.gui.customcomponents.FilmPanel;
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

	private FilmPanel filmPanel;
	private FilmControlPanel controlPanel;
	
	private Point prevMousePosition = null;

	public TimeSeriesView(String keggPathway, VisualizeTimeSeries model,
			VisualizeTimeSeriesListener controller, IntegratorUI integratorUI, Species species) {
		super(integratorUI, null, species); // There's no film at this point, so data is null
		this.controller = controller;
		this.model = model;
		
		// Always display scrollbars. So the control panel is always visible.
		setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		
		// Add some mouse functionality to the viewport: drag navigation
		addMouseListener();	
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

		// Build the control panel. We are showing the first frame, so set the secondsToTimeUnit
		// label and disable the previous button
		controlPanel = new FilmControlPanel(controller);
		controlPanel.setFrameToTimeUnit(1, model.mapFrameToTimePoint(1), model.getTimeUnit());
		controlPanel.enablePrevButton(false);
		// The control panel is shown as the column header of this scroll pane
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
	public void initialize(final Dimension filmDimension) {
		
		// TODO: Show the film (work with the IContainer)
		// we attempt to open up the container
//		if (container.open(controller.getFilePath(), IContainer.Type.READ, null) < 0)
//			throw new RuntimeException("Failed to open media file");
		
		this.data = model.getFilm();

		// print information about the streams in the container. for testing
		testIContainer(data);

		// What is the dimension of the film?
		getVisualization();
		
		
		// set the FilmPanel
		setViewportView(filmPanel);
		validate();
		// The filmPanel also reacts on MouseWheelEvents (zooms in/out)
		addMouseWheelListener(filmPanel);		
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
	 * Show the next frame.
	 * @param nextFrame to show
	 * @param curSecond the current second in the film
	 */
	public void showNextFrame(BufferedImage nextFrame, int curFrame) {
		showNextFrame(nextFrame, curFrame, false);
	}
	
	/**
	 * Show the next Frame.
	 * @param nextFrame to show
	 * @param curSecond the current second in the film
	 * @param resizeFrame true, if nextFrame should be resized that it fits into the Viewport
	 */
	public void showNextFrame(BufferedImage nextFrame, int curFrame, boolean resizeFrame) {
		if(resizeFrame)	 {
			// Resize the frame, so that the whole frame is visible in the view
			// Get the visible area of the JViewport
			Dimension visibleArea = getViewport().getExtentSize();
			
			// The ratio between the viewport size and the frame size.
			double widthRatio = (visibleArea.getWidth() - 20) / model.getDimension().getWidth();
			double heightRatio = (visibleArea.getHeight() - 20) / model.getDimension().getHeight();
			
			// Zoom factor is 1, if the original frame can be displayed in the viewport, < 1 otherwise
			double zoomFactor = Math.min(1, Math.min(widthRatio, heightRatio));
			
			// Show the next frame with the computed zoomFactor
			filmPanel.showNextFrame(nextFrame, zoomFactor);
	
		} else {
			filmPanel.showNextFrame(nextFrame);
		}
		
		// Update the control panel
		controlPanel.setFrameToTimeUnit(curFrame,
				model.mapFrameToTimePoint(curFrame), model.getTimeUnit());
		controlPanel.setSliderValue(curFrame);
	}
	
//	/**
//	 * Shows the first frame.
//	 * @param firstFrame
//	 */
//	public void showFirstFrame(BufferedImage firstFrame) {
//		// Show the first frame
//		filmPanel.showNextFrame(firstFrame);
//		
//		// Disable the previous button
//		controlPanel.enablePrevButton(false);
//	}
	
//	public void setImage(Image image) {
//    //SwingUtilities.invokeLater(new ImageRunnable(image));
//		ImageIcon imageIcon = new ImageIcon(image);
//		curFrame = new JLabel(imageIcon);
//		setViewportView(curFrame);
//  }

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
	 * @param b should the play button be enabled?
	 */
	public void enablePlayFunctionality(boolean b) {
		controlPanel.enablePlayButton(b);
		validate();
	}
	
	/**
	 * Add a MouseMotionListener and a MouseListener to the Viewport to
	 * support drag navigation.
	 */
	private void addMouseListener() {
		
		// Add the MouseMotionListener. Computes differences between two points and
		// changes the values of the scrollbars according to the difference.
		getViewport().addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent e) {
				// intentially left blank
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				// Where is the mouse?
				Point mousePosition = e.getPoint();

				// Is the mouse in the JViewport?
				Component mouseComponent = getComponentAt(mousePosition);
				if(mouseComponent == null || mouseComponent.getClass() != JViewport.class) {
					// Mouse is not in the JViewport. Do nothing.
					System.out.println("Mouse is not in the Viewport"); // for testing
					return;
				} else {
					System.out.println("Mouse is in the Viewport"); // for testing
					// If there was no previous point, set this point as reference
					if(prevMousePosition == null)
						prevMousePosition = e.getPoint();

					// Compute difference of mouse current and previous mouse position
					// Change the position of the visible area according to the difference
					double diffX = prevMousePosition.getX() - e.getX();
					double diffY = prevMousePosition.getY() - e.getY();

					double curScrollX = getHorizontalScrollBar().getValue();
					double curScrollY = getVerticalScrollBar().getValue();

					Double newScrollX = new Double(curScrollX - diffX / 3);
					Double newScrollY = new Double(curScrollY - diffY / 3);

					getHorizontalScrollBar().setValue(newScrollX.intValue());
					getVerticalScrollBar().setValue(newScrollY.intValue());
				}
			}
		});
		
		// Add the MouseListener
		getViewport().addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
				// Set the previous mouse position to null.
				// So the user can start a new drag navigation at a new point
				prevMousePosition = null;
			}
			
			@Override
			public void mousePressed(MouseEvent e) {		
				// Intentially left blank
			}	
			@Override
			public void mouseExited(MouseEvent e) {
			// Intentially left blank		
			}
			@Override
			public void mouseEntered(MouseEvent e) {
			// Intentially left blank	
				}		
			@Override
			public void mouseClicked(MouseEvent e) {
			// Intentially left blank	
			}
		});
	}
}

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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import y.view.Graph2D;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.gui.tabs.IntegratorTab;
import de.zbit.gui.tabs.TimeSeriesView;
import de.zbit.util.StringUtil;
import de.zbit.util.progressbar.AbstractProgressBar;

/**
 * This class is the controller for VisualizeTimeSeries and TimeSeriesView.
 * 
 * @author Felix Bartusch
 * @version $Rev$
 */

public class VisualizeTimeSeriesListener implements ActionListener {
	public static final transient Logger log = Logger.getLogger(TranslatorPanel.class.getName());
	
	/** The view which is controlled by this class. */
	TimeSeriesView view;
	
	private Component originalViewportView;
	
	/** The modell which is visualized by the view */
	VisualizeTimeSeries model;
	
	/** The number of the current frame shown in the view. */
	int curFrame = 1; //always begin with the first frame, which has the number 1 ;)
	
	/** The current progress bar shown by the view (e.g. while film is generated) */
	AbstractProgressBar progBar;

	/** Is the play button activated? That means, runs the film?*/
	private boolean isPlayButtonActive = false;

	/** Is the film generated yet? */
	private boolean filmGenerated = false;
	
	/**
   * All actions and events for {@link VisualizeTimeSeries} are defined here.
   * This are events fired by the KEGGImporter and actions to control the current visualized
   * frame. 
   * @author Felix Bartusch
   */
  public static enum VTSAction implements ActionCommand {
  	/** Is fired, when {@link VisualizeTimeSeries} starts the generation of the film. */
		START_GENERATE_FILM,
		
		/** Is fired, when {@link VisualizeTimeSeries} generated an image of the film. */
		IMAGE_GENERATED,
		
		/** Is fired, when {@link VisualizeTimeSeries} ends the generation of the the film. */
		END_GENERATE_FILM,
		
		/** Play the film frame by frame */
		PLAY_FILM,
		
		/** Stop film and show the previous frame */
		SHOW_PREVIOUS_FRAME,
		
		/** Stop the film and show the next frame */
		SHOW_NEXT_FRAME,
		
		/** Pause the film. */
		PAUSE_FILM,
		
		/** Something went wrong while showing the film */
		SHOW_VIDEO_FAILED,
		
		/** Go to a specific position in the film */
		GO_TO_POSITION,
		
		/** Export the generated film to a file */
		EXPORT_AS_VIDEO,
		
		/** Film was succesfully exported */
		END_EXPORT_FILM,
		
		/** Visualization options*/
		// Switch between p-value and q-value
		VISUALIZE_ENRICHMENT_PVAL, VISUALIZE_ENRICHMENT_QVAL,
		
		/** The three FDR correction methods*/
		FDR_CORRECTION_BH, FDR_CORRECTION_BFH, FDR_CORRECTION_BO;
		
		@Override
		public String getName() {
			switch (this) {
			case VISUALIZE_ENRICHMENT_PVAL:
				return "Visualize enrichment p-value";
			case VISUALIZE_ENRICHMENT_QVAL:
				return "Visualize enrichment q-value";
			case FDR_CORRECTION_BH:
				return "Benjamini Hochberg";
			case FDR_CORRECTION_BFH:
				return "Bonferroni Holm";
			case FDR_CORRECTION_BO:
				return "Bonferroni";
			default:
				return StringUtil.firstLetterUpperCase(toString().toLowerCase().replace('_', ' '));
      }
		}

		@Override
		public String getToolTip() {
			switch (this) {
			case VISUALIZE_ENRICHMENT_PVAL:
				return "Visualize the enrichment p-value.";
			case VISUALIZE_ENRICHMENT_QVAL:
				return "Visualize the enrichment q-value.";
			case FDR_CORRECTION_BH:
				return "Correct p-values with the Benjamini and Hochberg method.";
			case FDR_CORRECTION_BFH:
				return "Correct p-values with the Holm–Bonferroni method.";
			case FDR_CORRECTION_BO:
				return "Correct p-values with the Bonferroni method.";
			default:
				return StringUtil.firstLetterUpperCase(toString().toLowerCase().replace('_', ' '));
			}
		}
  }

	/**
	 * @param model
	 */
  public VisualizeTimeSeriesListener(VisualizeTimeSeries model) {
		this(null, model);
	}
  
	/**
	 * All other constructors call this constructor.
	 * @param view
	 * @param model
	 */
	public VisualizeTimeSeriesListener(TimeSeriesView view, VisualizeTimeSeries model) {
		this.view = view;
		this.model = model;
	}

	/* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public synchronized void actionPerformed(ActionEvent e) {
  	// if ActionCommand is null, the action came from the KEGGImporter
  	// for testing
  	System.out.println("VisualizeTimeSeriesListener: " + e.toString());
  	System.out.println("ActionEvent ID: " + e.getID());

  	if(e.getActionCommand() == null) {
  		switch(e.getID()) {
  		// the cases 1 - 5 were copied from KEGGPathwayActionListener
  		case 1:	
  			/*
  			 * SHOULD BE FIRED BEFORE A PATHWAY DOWNLOAD IS INITIATED COMPLETE
  			 * Any Pathway name can optionally be submitted as ActionCommand.
  			 */
  			String message = "Downloading '" + model.getPathwayID() + "'" + " for '" + model.getSpecies().getKeggAbbr() + "'";
  			
  			// generate new ProgressBar, set the ProgressBar into the view and the KeggImporter
  			// TODO Progress bar does not show progress
  			model.getKeggImporter().setProgressBar(view.showTemporaryLoadingPanel(message));
  			
  			System.out.println("Show loading panel case1");
  			
  			break;
  			
  		case 2:
  			/*
  			 * SHOULD BE FIRED WHEN A PATHWAY DOWNLOAD IS COMPLETE
  			 * The downloaded File should be given as ActionCommend
  			 */
  			String downloadedFile = e.getActionCommand();
  			if (downloadedFile!=null) {
  				log.info("Pathway download successful.");
  			} else {
  				log.warning("Pathway download failed.");
  				// Remove the tab
  				view.getIntegratorUI().closeTab(view);
  			}
  			break;
  			
  		case 3:
  			/*
  			 * SHOULD BE FIRED BEFORE A PATHWAY IS TRANSLATED
  			 * The KEGGTranslator MUST BE the source.
  			 * Any loading string can OPTIONALLY be in the ActionCommand.
  			 */
  			  			
  			// Save the originalViewport and set the pathwayPanel. The pathwayPanel shows a nice ProgressBar for
  			// generating the pathway
  			originalViewportView = view.getViewport().getView();
  			view.setViewportView(model.pathwayPanel);
  			
  			System.out.println("Show loading panel case3");
  			break;
  			
  		case 4:
  			/*
  			 * SHOULD BE FIRED AFTER A PATHWAY TRANSLATION IS COMPLETE.
  			 * The translated document should be in the action source.
  			 */
  			log.info("Pathway translation complete.");
  			try {
  				// Restore the original viewport, so that the film generating process is shown
  				view.setViewportView(originalViewportView);
  				model.setPathway((Graph2D) e.getSource());
  				model.generateFilm();  					
  				
  			} catch (Throwable e2) {
  				if (!Thread.currentThread().isInterrupted()) {
  					// Don't show errors for interrupted threads
  					GUITools.showErrorMessage(null, e2);
  					
  				}
  			}
  			break;
  			
  		case 5:
  			/*
  			 * MEANS: "REMOVE ME FROM YOUR LISTENERS LIST".
  			 */
  			// intentially left blank
  			break;
  			
  		default:
  			log.warning("Unkown Action Command: " + e);
  		}	
  	}
  	
  	// Otherwise, the action came from an instance of VisualizeTimeSeries.
  	else {
  		String command = e.getActionCommand();
  		
  		if (command.equals(VTSAction.START_GENERATE_FILM.toString())) {
  			System.out.println("This part of the code is reaches");
  			// Set the progress bar in the view
  			//AbstractProgressBar bar = view.showTemporaryLoadingPanel("Generating film of pathway " + model.getPathwayID());
  			try {
  				AbstractProgressBar bar = IntegratorTab.generateLoadingPanel(view, "Generating visualization of the data for pathway " + model.getPathwayID());
  				bar.setNumberOfTotalCalls(e.getID()); // number of images to generate
  				setProgBar(bar);  				
  			} catch(Exception ex) {
  				ex.printStackTrace();
  			}
				
  		} else if(command.equals(VTSAction.IMAGE_GENERATED.toString())) {
  			// Increment the counter of the progress bar
  			progBar.DisplayBar();
  			view.getViewport().repaint();
  			
  		} else if(command.equals(VTSAction.END_GENERATE_FILM.toString())) {  			
  			// From now on, one can play the film
  			filmGenerated = true;
  			
  			// Get the pathwayPanel from the model and hand it to the view
  			view.setPathwayPanel(model.getPathwayPanel());
  			
  			// Update the toolbar
  			view.createJToolBarItems(IntegratorUI.getInstance().getJToolBar());
  			
  			// Get the first frame from the model
  			curFrame = 1;
  			Graph2D firstFrame = model.getFrame(curFrame);
  			
  			// Initialize the view and show the first frame
  			try {
  				// Initialze the view
  				view.getVisualization();
  				
  				// Show the first frame and resize it
  				view.showGraph(firstFrame, curFrame, true);
  			} catch (Exception ex){
  				ex.printStackTrace();
  			}
  			
  		} else if(command.equals(VTSAction.PLAY_FILM.toString())) {
  			playButtonActivated(true);
  			
  			// Play the film. Start at the current frame.
  			playFilm(curFrame);
  			
  		} else if(command.equals(VTSAction.PAUSE_FILM.toString())) {
  			playButtonActivated(false); 		
  			
  		} else if(command.equals(VTSAction.SHOW_NEXT_FRAME.toString())) {
  			// If film is running, stop the film
  			playButtonActivated(false);
  			
  			// Remember: first frame has index 1, last frame has index numFrames
  			if(curFrame != model.getNumFrames()) {
  				// Increment the frame counter. Do nothing, if curFrame is the last frame.
  				incrementCurFrame();

  				// Get the next frame from the model
  				Graph2D nextFrame = model.getFrame(curFrame);

  				// Show the next frame
  				view.showGraph(nextFrame, curFrame);
  				// Because view shows a next frame, we can also deliver a previous frame no
  				view.enablePrevFrameFunctionality(true);
  			} else {
  				// View shows already the last frame, do nothing
  			}
  			
  			// Disable the next frame functionality, if the view shows the last frame now
  			if(curFrame == model.getNumFrames())
  				view.enableNextFrameFunctionality(false);
  			
  		} else if(command.equals(	VTSAction.SHOW_PREVIOUS_FRAME.toString())) {
  			// If film is running, stop the film
  			playButtonActivated(false);
  			
  			// Remember: first frame has index 1, last frame has index numFrames
  			if(curFrame != 1) {
  				// Decrement the frame counter
  				decrementCurFrame();
  				
  				// Read packages until the next frame is complete
  				Graph2D nextFrame = model.getFrame(curFrame);
  				
  				// Show the next frame
  				view.showGraph(nextFrame, curFrame);
  				
  				// Because view shows a previous frame, we can also deliver a next frame now
  				view.enableNextFrameFunctionality(true);			
  			} else {
  				// View shows already the first frame, do nothing
  			}
  			
  			// Disable the previous frame functionality, if the view shows the first frame
  			if(curFrame == 1)
  				view.enablePrevFrameFunctionality(false);
  			
  		} else if(command.equals(VTSAction.GO_TO_POSITION.toString())) {
  			// In this case, the ID of the ActionEvent is the slider's value.
  			// That means, the frame to jump to
  			int goToFrameNum = e.getID();
  			
  			// If the wanted frame is already visualized, do nothing
  			if(goToFrameNum == curFrame) {
  				// do nothing
  			} else { 	// go to the wanted frame
  				
  				// Use a new container, if the wanted frame lies before the current frame,
  				// because we can't seek backwards
  				boolean isBackwards = goToFrameNum < curFrame ? true : false;
  				
  				// Get the new frame
  				curFrame = goToFrameNum;				
  				Graph2D nextFrame = model.getFrame(curFrame, isBackwards);
  				
  				// Show the new frame
  				view.showGraph(nextFrame, curFrame);
  				
  				// Update the next- / previous-functionality
  				if(curFrame == model.getNumFrames()) { // last frame is visualized
  					view.enableNextFrameFunctionality(false);
  					view.enablePrevFrameFunctionality(true);
  				} else if (curFrame == 1) { // first frame is visualized
  					view.enablePrevFrameFunctionality(false);
  					view.enableNextFrameFunctionality(true);
  				} else { // model can deliver a next/previous frame
  					view.enableNextFrameFunctionality(true);
  					view.enablePrevFrameFunctionality(true);
  				}
  			}
				
			} else if(command.equals(VTSAction.EXPORT_AS_VIDEO.toString())) {
				
				// Start exporting the film
				if(isFilmGenerated())
					model.exportFilm();
				
			} else if(command.equals(VTSAction.END_EXPORT_FILM.toString())) {
				// Show the last visualized frame
				view.showGraph(model.getFrame(curFrame), curFrame);
				view.setViewportView(view.getPathwayPanel());
				
  		} else if(command.equals(VTSAction.SHOW_VIDEO_FAILED.toString())) {
  			// TODO stop computing film, if film is generated, close visualization tab
  			
  			// TODO more specific error message
  			GUITools.showErrorMessage(view.getParent(), "Something went wrong while generating/showing the film.");
  		
  		} else if(command.equals(VTSAction.VISUALIZE_ENRICHMENT_PVAL.toString())) {
  			// Change the option button selection
  			view.selectPValueButton(true);
  			
  			// Change the visualization
  			model.setVisualizePValue(true);
  			Graph2D nextFrame = model.getFrame(curFrame);
  			view.showGraph(nextFrame, curFrame);
  			
  		} else if(command.equals(VTSAction.VISUALIZE_ENRICHMENT_QVAL.toString())) {
  			// Change the option button selection
  			view.selectPValueButton(false);
  			
  			// Change the visualization
  			model.setVisualizePValue(false);
  			Graph2D nextFrame = model.getFrame(curFrame);
  			view.showGraph(nextFrame, curFrame);
  		
  		// TODO 
  		} else if(command.equals(VTSAction.FDR_CORRECTION_BH.toString())) {
  			// Change the option button selection
  			view.selectFDR(VTSAction.FDR_CORRECTION_BH.toString());
  			// Change the visualization
  			model.setBH();
  			Graph2D nextFrame = model.getFrame(curFrame);
  			view.showGraph(nextFrame, curFrame);
  		} else if(command.equals(VTSAction.FDR_CORRECTION_BFH.toString())) {
  			// Change the option button selection
  			view.selectFDR(VTSAction.FDR_CORRECTION_BFH.toString());
  			// Change the visualization
  			model.setBFH();
  			Graph2D nextFrame = model.getFrame(curFrame);
  			view.showGraph(nextFrame, curFrame);
    	} else if(command.equals(VTSAction.FDR_CORRECTION_BO.toString())) {
    		// Change the option button selection
    		view.selectFDR(VTSAction.FDR_CORRECTION_BO.toString());
  			// Change the visualization
  			model.setBO();
  			Graph2D nextFrame = model.getFrame(curFrame);
  			view.showGraph(nextFrame, curFrame);
    	}
  		
  	}
  }

  /**
   * Call this method to make changes if the play button was activated / deactivated.
   * @param activated, true if the play button was activated
   */
  private void playButtonActivated(boolean activated) {
  	isPlayButtonActive = activated;
  	
  	// If the play-button was activated, disable the play functionality (e.g. the play-button)
  	// in the view.
		view.enablePlayFunctionality(!activated);	
	}

	/**
   * Play the film from the given start frame in a new thread.
   * @param i
   */
	private void playFilm(final int startFrame) {
		// TODO Rewrite this with timer
		// This runnable will be executed in a new thread. It tests the time since the start
		// and displays new images when needed.
		// If the user uses the pause-, previous- or next-button the film pauses.
		Runnable r = new Runnable() {
		
			@Override
			public void run() {
				// The time between two frames in milliseconds
				int timeBetweenTwoFrames = 1000; 
				Graph2D nextFrame = null;
				
				// Run film, as long as the no one stops the film.
				while(isPlayButtonActive && curFrame != model.getNumFrames()) {
					
					// load the next frame
					incrementCurFrame();
					long begin2 = System.currentTimeMillis();
					nextFrame = model.getFrame(curFrame, false);
					long end2 = System.currentTimeMillis();
				
					// Sleep, until the next frame should be displayed.
					try {
						long sleepTime = timeBetweenTwoFrames - (end2-begin2);
						if(sleepTime > 0)
							TimeUnit.MILLISECONDS.sleep(timeBetweenTwoFrames);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					// Test, if the play-button is still activated. If that is the fact,
					// show the next frame. Otherwise decrement the frame counter.
					if(isPlayButtonActive) {						

						// Show the next frame
						begin2 = System.currentTimeMillis();
						view.showGraph(nextFrame, curFrame);
						view.repaint();
						end2 = System.currentTimeMillis();

						// Because view shows a next frame, we can also deliver a previous frame
						view.enablePrevFrameFunctionality(true);	
						
	
					} else { // pause-button was activated, stop the film
						// We read one image too much. So decrement the curFrame counter
						decrementCurFrame();
						break;
					}
				}
				
				// If the thread quits the while-loop, the film has stopped.
				playButtonActivated(false);
				
				// If the last frame was reached, disable the next-button
				if(curFrame == model.getNumFrames())
					view.enableNextFrameFunctionality(false);
			}
		};
		
		// Start the film thread.
		Thread filmThread = new Thread(r, "filmThread");
		filmThread.start();
	}

	/**
   * Set the view, which is controlled by this class.
   * @param view which is controlled.
   */
	public void setView(TimeSeriesView view) {
		this.view = view;		
	}

	public void setProgBar(AbstractProgressBar progBar) {
		this.progBar = progBar;
	}
	
	public String getFilePath() {
		return "./longTestFilm.mp4";
	}

	/**
	 * @return The duration of the film. Return -1 if film isn't
	 * generated yet.
	 */
	public int getDuration() {
		return model.getDuration();
	}

	/**
	 * Increment the frame counter.
	 */
	private void incrementCurFrame() {
		curFrame++;
	}

	/**
	 * Decrement the frame counter.
	 */
	private void decrementCurFrame() {
		curFrame--;
	}

	/**
	 * @return the number of frames
	 */
	public int getNumFrames() {
		return model.getNumFrames();
	}

	/**
	 * Is the film generated yet?
	 * @return
	 */
	public boolean isFilmGenerated() {
		return filmGenerated;
	}

	/**
	 * Return the number of the current visualized frame
	 * @return
	 */
	public int getCurFrame() {
		return curFrame;
	}
}

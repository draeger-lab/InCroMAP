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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import y.view.Graph2D;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.gui.GUITools;
import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.gui.tabs.TimeSeriesView;
import de.zbit.util.StringUtil;
import de.zbit.util.progressbar.AbstractProgressBar;

/**
 * This class is the controller for VisualizeTimeSeries and TimeSeriesView.
 * @author Felix Bartusch
 * @version $Rev$
 */

public class VisualizeTimeSeriesListener implements ActionListener {
	public static final transient Logger log = Logger.getLogger(TranslatorPanel.class.getName());
	
	/**
	 * The view which is controlled by this class.
	 */
	TimeSeriesView view;
	
	/**
	 * The modell which is visualized by the view
	 */
	VisualizeTimeSeries model;
	
	/** The number of the current frame shown in the view. */
	int curFrame = 0; //always begin with the first frame, which has the number 0 ;)
	
	/**
	 * The current progress bar shown by the view (e.g. while film is generated)
	 */
	AbstractProgressBar progBar;
	
	private boolean isFilmGenerated = false;
	
	/**
	 * All other constructors call this constructor.
	 * @param view
	 * @param model
	 */
	VisualizeTimeSeriesListener(TimeSeriesView view, VisualizeTimeSeries model) {
		this.view = view;
		this.model = model;
	}
	
	/**
   * All actions for {@link VisualizeTimeSeries} are defined here.
   * @author Felix Bartusch
   */
  public static enum VTSAction implements ActionCommand {
  	/**
  	 * Is fired, when {@link VisualizeTimeSeries} starts the generation of the film.
  	 */
		START_GENERATE_FILM,
		
		/**
		 * Is fired, when {@link VisualizeTimeSeries} generated an image of the film.
		 */
		IMAGE_GENERATED,
		
		/**
		 * Is fired, when {@link VisualizeTimeSeries} ends the generation of the the film.
		 */
		END_GENERATE_FILM,
		
		/**
		 * Play the film frame by frame
		 */
		PLAY_FILM,
		
		/**
		 * Stop film and show the previous frame
		 */
		SHOW_PREV_FRAME,
		
		/**
		 * Stop the film and show the next frame
		 */
		SHOW_NEXT_FRAME,
		
		/**
		 * Pause the film.
		 */
		PAUSE_FILM,
		
		/**
		 * Something went wrong while showing the film
		 */
		SHOW_VIDEO_FAILED,
		
		/**
		 * Go to a specific position in the film
		 */
		GO_TO_POSITION
		;
		
		@Override
		public String getName() {
			switch (this) {
        
      default:
        return StringUtil.firstLetterUpperCase(toString().toLowerCase().replace('_', ' '));
      }
		}

		@Override
		public String getToolTip() {
			switch (this) {
        
      default:
      	return StringUtil.firstLetterUpperCase(toString().toLowerCase().replace('_', ' '));
      }
		}
  	
  }

	/**
	 * 
	 * @param model
	 */
  public VisualizeTimeSeriesListener(VisualizeTimeSeries model) {
		this(null, model);
	}

	/* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public synchronized void actionPerformed(ActionEvent e) {
  	// if ActionCommand is null, the action came from the KEGGImporter
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
  			model.getKeggImporter().setProgressBar(view.showTemporaryLoadingPanel(message));
  			
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
  			message = "Reconstructing pathway with online information from KEGG...";
  			
  			// generate new ProgressBar, set the ProgressBar into the view and the KeggImporter
  			model.getKeggImporter().setProgressBar(view.showTemporaryLoadingPanel(message));
  			break;
  			
  		case 4:
  			/*
  			 * SHOULD BE FIRED AFTER A PATHWAY TRANSLATION IS COMPLETE.
  			 * The translated document should be in the action source.
  			 */
  			log.info("Pathway translation complete.");
  			try {
  				// Get the resulting document and check and handle eventual errors.
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
  	// otherwise, the action came from an instance of VisualizeTimeSeries
  	else {
  		String command = e.getActionCommand();
  		
  		if (command.equals(VTSAction.START_GENERATE_FILM.toString())) {
  			// set the progress bar in the view
  			AbstractProgressBar bar = view.showTemporaryLoadingPanel("Generating film of pathway " + model.getPathwayID());
				bar.setNumberOfTotalCalls(e.getID()); // number of images to generate
				setProgBar(bar);
							
  		} else if(command.equals(VTSAction.IMAGE_GENERATED.toString())) {
  			// increment the counter of the progress bar
  			// view.showGraph((Graph2D) e.getSource()); for testing
  			progBar.DisplayBar();
  			
  		} else if(command.equals(VTSAction.END_GENERATE_FILM.toString())) {
  			// Set flag, that film is succesfully generated
  			isFilmGenerated = true;
  			
  			// Read the temporary film file
  			model.loadFilmFromTempFile();
  			
  			// Delete the temporary film file
  			//model.deleteTempFilmFile();
  			
  			// Get some important information of the film like
  			// - index of the video stream in the container
  			// - duration and time base of the video stream
  			model.lookupStreamInformation();
  			
  			// Initialize the view and show the film
  			try {
  				view.initializeAndShowFilm(model.getDimension());
  			} catch (Exception ex){
  				ex.printStackTrace();
  			}
  			
  		} else if(command.equals(VTSAction.PLAY_FILM.toString())) {
  			// TODO
  			System.out.println("Play button works!");
  		} else if(command.equals(VTSAction.PAUSE_FILM.toString())) {
  			// TODO
  			System.out.println("Pause button works!"); 		
  			
  		} else if(command.equals(VTSAction.SHOW_NEXT_FRAME.toString())) {
  			// Remember: first frame has index 0, last frame has index numFrames -1
  			if(curFrame != model.getNumFrames() - 1) {
  				// Increment the frame counter. Do nothing, if curFrame is the last frame.
  				curFrame++;

  				// Read packages until the next frame is complete
  				BufferedImage nextFrame = model.getFrame(curFrame);

  				// Show the next frame
  				view.showNextFrame(nextFrame);
  				
  				// Because view shows a next frame, we can also deliver a previous frame no
  				view.enablePrevFrameFunctionality(true);
  			} else {
  				// View shows already the last frame, do nothing
  			}
  			
  			// Disable the next frame functionality, if the view shows the last frame now
  			if(curFrame == model.getNumFrames()-1)
  				view.enableNextFrameFunctionality(false);
  			
  		} else if(command.equals(	VTSAction.SHOW_PREV_FRAME.toString())) {
  			// Remember: first frame has index 0, last frame has index numFrames -1
  			if(curFrame != 0) {
  				// Decrement the frame counter
  				curFrame--;
  				
  				// Read packages until the next frame is complete
  				BufferedImage nextFrame = model.getFrame(curFrame);
  				
  				// Show the next frame
  				view.showNextFrame(nextFrame);
  				
  				// Because view shows a previous frame, we can also deliver a next frame now
  				view.enableNextFrameFunctionality(true);
  			} else {
  				// View shows already the first frame, do nothing
  			}
  			
  			// Disable the previous frame functionality, if the view shows the first frame
  			if(curFrame == 0)
  				view.enablePrevFrameFunctionality(false);
  			
  		} else if(command.equals(VTSAction.GO_TO_POSITION.toString())) {
  			// TODO
  			System.out.println("Go to action works!");
  			 			
  		} else if(command.equals(VTSAction.SHOW_VIDEO_FAILED.toString())) {
  			// TODO stop computing film, if film is generated, close visualization tab
  			System.out.println("Go to action works!");
  			
  			// TODO more specific error message
  			GUITools.showErrorMessage(view.getParent(), "Something went wrong while generating/showing the film.");
  		}
  	}
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
	
	public boolean isFilmGenerated() {
		return isFilmGenerated ;
	}
	
	public String getFilePath() {
		return "./longTestFilm.mp4";
	}

	/**
	 * @return The duration of the film. Return -1 if film isn't
	 * generated yet.
	 */
	public int getDuration() {
		if(!isFilmGenerated())
			return -1;
		else {
			return model.getDuration();
		}
	}
}

package de.zbit.visualization;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import com.xuggle.xuggler.IContainer;

import y.view.Graph2D;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.gui.GUITools;
import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.gui.actions.NameAndSignalTabActions.NSAction;
import de.zbit.gui.tabs.IntegratorTabWithTable;
import de.zbit.gui.tabs.TimeSeriesView;
import de.zbit.kegg.io.KEGGtranslator;
import de.zbit.util.Species;
import de.zbit.util.StringUtil;
import de.zbit.util.progressbar.AbstractProgressBar;

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
	
	/**
	 * The current progress bar shown by the view (e.g. while film is generated)
	 */
	AbstractProgressBar progBar;
	
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
		END_GENERATE_FILM
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
        return null;
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
  @SuppressWarnings("unchecked")
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
  			// show the film and the controller panel (play, pause, ...)
  			// TODO
  			
  			view.initializeAndShowFilm((IContainer) e.getSource());
  			
  			// Print information of the film
  			//IContainer f = model.getFilm();
  			//System.out.println("Number of streams in container: " + f.getNumStreams());
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
	
	public String getFilePath() {
		return "./testFilm.mp4";
	}
}

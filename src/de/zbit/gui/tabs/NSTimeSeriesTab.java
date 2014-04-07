package de.zbit.gui.tabs;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.openmbean.CompositeData;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import de.zbit.data.NameAndSignals;
import de.zbit.data.mRNA.mRNA;
import de.zbit.gui.IntegratorUI;
import de.zbit.integrator.ReaderCache;
import de.zbit.io.FileTools;
import de.zbit.io.NameAndSignalReader;
import de.zbit.util.Species;

public class NSTimeSeriesTab extends NameAndSignalsTab implements PropertyChangeListener{
	private static final long serialVersionUID = 1L;
	public static final transient Logger log = Logger.getLogger(NameAndSignalsTab.class.getName());
	
	/**
	 *  String describing the timeUnit, e.g. "sec", "day", "hour", "mol", etc.
	 */
	private String timeUnit = "";
	
	/**
	 * This List holds the time information. The first entry belongs to the first SignalColumn
	 * and so on. There must be as many timePoints as SignalColumns.
	 */
	private List<Float> timePoints;
	

	// Most of the constructor bodies were copied from NameAndSignalTab.
	public NSTimeSeriesTab(IntegratorUI parent, Object data) {
		super(parent, data, null);
	}

	public NSTimeSeriesTab(IntegratorUI parent, Object data, Species species) {
		super(parent, data, species);
	}

	public NSTimeSeriesTab(final IntegratorUI parent, final NameAndSignalReader<? extends NameAndSignals> nsreader, final String inFile) {
			this(parent, null);
    
	    // Create intermediate loading tab
	    setIntermediateBar(IntegratorTab.generateLoadingPanel(this, "Reading file " + FileTools.getFilename(inFile)));
	    nsreader.setProgressBar(getIntermediateBar());
	    
	    // Create worker. The worker has to read also the timeUnit and timePoints information
	    final NSTimeSeriesTab thiss = this;
	    SwingWorker<Collection<? extends NameAndSignals>, Void> worker = new SwingWorker<Collection<? extends NameAndSignals>, Void>() {
	      @Override
	      protected Collection<? extends NameAndSignals> doInBackground() throws Exception {
	        nsreader.setSecondaryProgressBar(IntegratorUI.getInstance().getStatusBar().showProgress());
	        Collection<? extends NameAndSignals> col = nsreader.importWithGUI(parent, inFile, ReaderCache.getCache());
	        thiss.species = nsreader.getSpecies();
	        //TODO: implement in mRNATimeSeriesReader
	        //thiss.timeUnit = nsreader.getTimeUnit();
	        //thiss.timePoints = nsreader.getTimePoints();
	        return col; // col==null if cancel button pressed
	      }
	    };
	    worker.addPropertyChangeListener(this);
	    worker.execute();
	}
	
  /**
   * Change this (intermediate) panel to a real {@link NSTimeSeriesTab} or
   * close it if the worker did fail.
   * @param worker
   */
  @SuppressWarnings("rawtypes")
  private void swingWorkerDone(SwingWorker worker) {
    Object data=null;
    if (getIntermediateBar()!=null) getIntermediateBar().finished();
    parent.getStatusBar().reset();
    try {
      data = worker.get();
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error during worker execution.", e);
    }
    
    if (data==null || worker.isCancelled()) {
      parent.closeTab(this);
    } else {
      setData(data);
      // This would couse to place buttons for this tab on the bar,
      // even if this tab is not currently visible!
      //updateButtons(parent.getJMenuBar(), parent.getJToolBar());
      parent.updateButtons(); 
      parent.setIconForTab(this);
    }
  }
 

  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */

 @SuppressWarnings("rawtypes")
  public void propertyChange(PropertyChangeEvent evt) {
    // Checks if intermediate swing workers are done.
    String name = evt.getPropertyName();
    if ((evt.getSource() instanceof SwingWorker) && name.equals("state")) {
      if (evt.getNewValue().equals(StateValue.DONE)) {
        swingWorkerDone((SwingWorker) evt.getSource());
      }
    }
  }
  

	/**
	 * @return the timeUnit
	 */
	public String getTimeUnit() {
		return timeUnit;
	}

	/**
	 * @param timeUnit the timeUnit to set
	 */
	public void setTimeUnit(String timeUnit) {
		this.timeUnit = timeUnit;
	}

	/**
	 * @return the timePoints
	 */
	public List<Float> getTimePoints() {
		return timePoints;
	}

	/**
	 * @param timePoints the timePoints to set
	 */
	public void setTimePoints(List<Float> timePoints) {
		this.timePoints = timePoints;
	}


	
}

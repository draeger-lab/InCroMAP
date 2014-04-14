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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.openmbean.CompositeData;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.mRNA.mRNATimeSeries;
import de.zbit.gui.IntegratorUI;
import de.zbit.integrator.ReaderCache;
import de.zbit.io.FileTools;
import de.zbit.io.NameAndSignalReader;
import de.zbit.io.mRNATimeSeriesReader;
import de.zbit.math.TimeSeriesModel;
import de.zbit.util.Species;
import de.zbit.util.objectwrapper.ValueTriplet;

/**
 * A special tab for the {@link IntegratorUI} for
 * {@link Collections} of {@link mRNATimeSeries} and {@link TimeSeriesModels}.
 * @author Felix Bartusch
 * @version $Rev$
 */
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
	private List<ValueTriplet<Double, String, SignalType>> timePoints;
	
	/**
	 * What method models the time series? User sets model after loading time series data and clicking on
	 * 'Model time series'
	 */
	private Class<? extends TimeSeriesModel> modelMethod = null;

	/**
	 * For each gene, this collection holds the information to model the gene with the given modelMethod.
	 * After clicking on 'Model time series' the model Information for the given modelMethod is computed.
	 */
	private Collection<?> modelInformation = null;
		
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
	        thiss.timeUnit = ((mRNATimeSeriesReader) nsreader).getTimeUnit();
	        thiss.timePoints = ((mRNATimeSeriesReader) nsreader).getTimePoints();
	        System.out.println(species);
	        System.out.println(timeUnit);
	        for(int i=0; i<timePoints.size(); i++) {
	        	System.out.println(timePoints.get(i));
	        }
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
      
      Collection<mRNATimeSeries> mRNA = (Collection<mRNATimeSeries>) data;
      
      // How one can get hands on the data?
      // Maybe the other toArray method is better?
      //System.out.println(mRNA.size()); // 45101
      //Object[] mRNAArray = mRNA.toArray();
      //System.out.println(mRNAArray.getClass()); // Object
      //System.out.println("Num Genes: " + mRNAArray.length); // 45101
      //System.out.println(mRNAArray[1].getClass()); //mRNATimeSeries
      
      // mRNATimeSeries
      //mRNATimeSeries m = (mRNATimeSeries) mRNAArray[0];
      //System.out.println("ColumnCount: " + m.getColumnCount()); //5
      //System.out.println("GeneSymbol :" +m.getGeneSymbol()); //Copg (Name in Tab)
      //System.out.println("Number of Signals: " + m.getNumberOfSignals()); //2
      //System.out.println(m.getSignalNames(m.getSignals())); // Liste von Signal Names and Types
      //m.getSig
      
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
	public List<ValueTriplet<Double, String, SignalType>> getTimePoints() {
		return timePoints;
	}

	/**
	 * @param timePoints the timePoints to set
	 */
	public void setTimePoints(List<ValueTriplet<Double, String, SignalType>> timePoints) {
		this.timePoints = timePoints;
	}


	
}

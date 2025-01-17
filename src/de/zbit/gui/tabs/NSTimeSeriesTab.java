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
package de.zbit.gui.tabs;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.TableResult;
import de.zbit.data.mRNA.mRNATimeSeries;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.dialogs.ModelSettingsDialog;
import de.zbit.integrator.ReaderCache;
import de.zbit.io.FileTools;
import de.zbit.io.NameAndSignalReader;
import de.zbit.io.mRNATimeSeriesReader;
import de.zbit.math.CubicSplineInterpolation;
import de.zbit.math.TimeFit;
import de.zbit.math.TimeSeriesModel;
import de.zbit.util.NotifyingWorker;
import de.zbit.util.Species;
import de.zbit.util.objectwrapper.ValueTriplet;
import de.zbit.util.progressbar.AbstractProgressBar;

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
	 * After clicking on 'Model time series' the model information is computed for each gene.
	 */
	private List<TimeSeriesModel> geneModels = null;
		
	// Most of the constructor bodies were copied from NameAndSignalTab.
	public NSTimeSeriesTab(IntegratorUI parent, Object data) {
		super(parent, data, null);
	}

	public NSTimeSeriesTab(IntegratorUI parent, Object data, Species species) {
		super(parent, data, species);
	}

	public NSTimeSeriesTab(final IntegratorUI parent, final NameAndSignalReader<? extends NameAndSignals> nsreader, final String inFile) {
			this(parent, null);
    
	    // Create a loading tab
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
	  * @return the model method or null, if data isn't modelled yet
	  */
	 public Class<? extends TimeSeriesModel> getModelMethod() {
		 return modelMethod;
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
	 * @return the gene models
	 */
	public Collection<TimeSeriesModel> getGeneModels() {
		return geneModels;
	}
	
	/**
	 * @return the timePoints
	 */
	public List<ValueTriplet<Double, String, SignalType>> getTimePoints() {
		return timePoints;
	}
	
	/**
	 * {@link SignalType} is inferred from the time points. (SignalType.FoldChange or SignalType.pValue)
	 * @return the signal type of the data
	 */
	public SignalType getSignalType() {
		return timePoints.get(0).getC();
	}

	/**
	 * @param timePoints the timePoints to set
	 */
	public void setTimePoints(List<ValueTriplet<Double, String, SignalType>> timePoints) {
		this.timePoints = timePoints;
	}
	

	public void setModelMethod(Class<? extends TimeSeriesModel> modelMethod) {
		this.modelMethod = modelMethod;
	}
	
	/**
	 * Is the {@link de.zbit.data.Signal.SignalType} pValue?
	 */
	public boolean isPValue() {
		return this.getSignalType() == SignalType.pValue;
	}
	
	/**
	 * @return the number of observations
	 */
	public int getNumObservations() {
		// Assumption: Every observation is dedicated to a time point
		return timePoints.size();
	}
	
	@SuppressWarnings("unchecked")
	public void modelTimeSeries(final Class<? extends TimeSeriesModel> classs) {
		
		this.modelMethod = classs;
		this.geneModels = new ArrayList<TimeSeriesModel>(this.data.size());

		// Show a dialog where the user can choose
		// - if the time points are exponentially distributed (common for concentrations 0.1, 0.01, 0.001 .... )
		// - a cutoff value. Model just genes that are over the cutoff value at least at one time point (good for the timeFit algorithm, not for the CubicSplineInterpolation)
		// Fill the cutoff field with an reasonable initial value. Because CubicSplineInterpolation
		// models each gene indepentend from other genes, we don't have to filter here. So set the cutoff field
		// to zero.
		double cutoffGuess = 0.0;
		if(classs == CubicSplineInterpolation.class) {
			cutoffGuess = 0.0;
		} else if(classs == TimeFit.class) {
			// Here a pre-filtering of the genes is reasonable, because the genes are clustered together
			// and genes in the same cluster affect the model of other genes in the same cluster.
			// So house holding genes would blur the models
			if(this.getSignalType() == Signal.SignalType.FoldChange)
				cutoffGuess = 1;
			else if(this.getSignalType() == Signal.SignalType.pValue) {
				cutoffGuess = 0.05;
			}
		}
		
		// A instance of the model method. Used to get an individualPanel
		// for the settings dialog and to compute model parameters
		// (e.g in the TimeFit method)
		ModelSettingsDialog settingsDialog = null;
		TimeSeriesModel model = null;
		try {
			model = classs.newInstance();
			settingsDialog = new ModelSettingsDialog(this, model, cutoffGuess);
		} catch (Exception e2) {
			GUITools.showErrorMessage(this, "Cannot build model settings dialog");
		}
				
		// Ask user for settings
		if(GUITools.showAsDialog(this, settingsDialog, "Please choose settings for the model", true) == JOptionPane.OK_OPTION) {
			final boolean isExponentiallyDistributed = settingsDialog.isExponentiallyDistributed();
			final double cutoff = settingsDialog.getCutoff();
			
			// The modelling step is different for the two methods.
			if(classs == CubicSplineInterpolation.class) {
				// Create a loading tab
				setIntermediateBar(IntegratorTab.generateLoadingPanel(this, "Computing time series models ..."));
				
				try {
					// Cast the data, so that a model can be build from it	
					NotifyingWorker<Collection<? extends TableResult>> worker = new NotifyingWorker<Collection<? extends TableResult>>() {

						@Override
						protected Collection<? extends TableResult> doInBackground() throws Exception {
							AbstractProgressBar pb = getIntermediateBar();
							pb.setNumberOfTotalCalls(data.size());
							for(Object o : data) {
								pb.DisplayBar();
								if(o instanceof mRNATimeSeries) {
									mRNATimeSeries mRNA = (mRNATimeSeries) o;
									
									// Remove additional data columns of the TimeFit method not needed for this method
									mRNA.removeData("Cluster");
																		
									mRNA.addData("Modeled?", "No");
									mRNA.addData("Model", null);
									NameAndSignals.additional_data_is_invisible.add("Model");

									// if mRNA has no NCBI geneID, geneModel for the mRNA is null. Add also a new additional data column,
									// with information whether the mRNA was modeled or not.
									// If the mRNA doesn't fulfill the cutoff value, the mRNA is not modelled.
									if(mRNA.getID() == -1 || !TimeSeriesModel.geneFulfillsCutoff(mRNA, getSignalType(), cutoff)) {
										mRNA.addData("Modeled?", "No");
										mRNA.addData("Model", null);
										
									} else {
										try {
											// generate a new model. ! Important: set name and idType manually ! They are needed later.
											TimeSeriesModel m = classs.newInstance();
											m.setName(mRNA.getName());
											m.setGeneID(mRNA.getID());
											m.setSignalType(getSignalType());
											m.generateModel(mRNA, timePoints, cutoff, isExponentiallyDistributed);

											geneModels.add(m);
											mRNA.addData("Modeled?", "Yes");
											mRNA.addData("Model", m);
											NameAndSignals.additional_data_is_invisible.add("Model");
										} catch (Exception e) {
											e.printStackTrace();
											GUITools.showErrorMessage(parent, "Exception while generating CubicSplineInterpolation model for " + mRNA.getName());
										}
									}
								}
							}
							
							rebuildTable();
							return data;
						}
					};
					worker.addPropertyChangeListener(this);
					worker.execute();
				} catch(Exception e) {
					e.printStackTrace();
					GUITools.showErrorMessage(parent, "Exception computing the TimeFit model parameters");
				}
				
				// Repaint table of the tab to show new 'Modeled?' column
				this.rebuildTable();
				
			} else if(classs == TimeFit.class) {
				// Cast the model to the TimeFit class. So that the parameter chosen by the user can
				// be used.
				final TimeFit tf = (TimeFit) model;
				// Create a loading tab
				setIntermediateBar(IntegratorTab.generateLoadingPanel(this, "Computing time series models ..."));
				
				// Compute the parameters for the single gene models
				final ArrayList<mRNATimeSeries> castedData = (ArrayList<mRNATimeSeries>) data;
				final NSTimeSeriesTab parent = this;
				
				try {
					// Cast the data, so that a model can be build from it	
					NotifyingWorker<Collection<? extends TableResult>> worker = new NotifyingWorker<Collection<? extends TableResult>>() {

						@Override
						protected Collection<? extends TableResult> doInBackground() throws Exception {
							AbstractProgressBar pb = getIntermediateBar();
							tf.generateModel(parent, castedData, timePoints, 5, cutoff, isExponentiallyDistributed, pb);
							if(tf.isModelled()) {
								getTimeFitModels(castedData, tf, cutoff);							
							}
//							else {
//								GUITools.showErrorMessage(parent, "Not enough genes remained after filtering.");
//							}
							return data;
						}
					};
					worker.addPropertyChangeListener(this);
					worker.execute();
					
				} catch(Exception e) {
					e.printStackTrace();
					GUITools.showErrorMessage(parent, "Exception computing the TimeFit model parameters");
				}
				//this.rebuildTable();
			}
		}
		
	}


	/**
	 * 
	 */
	private void getTimeFitModels(ArrayList<mRNATimeSeries> castedData, TimeFit tf, double cutoff) {
		mRNATimeSeries mRNA = null;
		// Now generate the model for each single gene
		for(int i=0; i<castedData.size(); i++) {
			mRNA = castedData.get(i);
			// if mRNA has no NCBI geneID, geneModel for the mRNA is null. Add also a new additional data column,
			// with information whether the mRNA was modeled or not
			if(mRNA.getID() == -1 || !TimeSeriesModel.geneFulfillsCutoff(mRNA, getSignalType(), cutoff)) {
				mRNA.addData("Modeled?", "No");
				mRNA.addData("Cluster", "-");
				mRNA.addData("Model", null);
			} else {
				try {
					// generate a new model. ! Important: set name and idType manually ! They are needed later.
					// TODO Don't add null models
					TimeSeriesModel m = tf.getGeneModel(mRNA, timePoints, i);
					int classs = tf.getClassOfGene(mRNA, i);
					if(m == null) {
						mRNA.addData("Modeled?", "No");
						mRNA.addData("Cluster", "-");
						mRNA.addData("Model", null);
						continue;
					}	
					m.setName(mRNA.getName());
					m.setGeneID(mRNA.getID());
					m.setSignalType(getSignalType());

					geneModels.add(m);
					mRNA.addData("Modeled?", "Yes");
					mRNA.addData("Cluster", Integer.toString(classs));
					mRNA.addData("Model", m);
					NameAndSignals.additional_data_is_invisible.add("Model");
				} catch (Exception e) {
					e.printStackTrace();
					GUITools.showErrorMessage(parent, "Exception assigning TimeFitModel to gene to " + mRNA.getName());
				}
			}
		}
		// Repaint table of the tab to show new 'Modeled?' column
		this.rebuildTable();
	}


	/**
	 * Visualize the selected model(s) in a line chart.
	 */
	public void visualizeSelectedModelInChart() {
		// The selected entries
		@SuppressWarnings("unchecked")
    List<mRNATimeSeries> selectedTimeSeries = (List<mRNATimeSeries>) this.getSelectedItems();

		// The corresponding selected models
		List<TimeSeriesModel> selectedModels = new ArrayList<TimeSeriesModel>();
		for(int i=0; i<selectedTimeSeries.size(); i++) {
			if(selectedTimeSeries.get(i).getData("Model") != null) {
				selectedModels.add((TimeSeriesModel) selectedTimeSeries.get(i).getData("Model"));
			}
		}

		// If no row is selected, don't do anything
		if(selectedModels.size() == 0)
			return;
		
//		System.out.println(selectedModels);
		parent.addTab(new VisualizeTimeSeriesAsLineChart(selectedModels, 200, timeUnit), "Models");
	}
	
	
	/**
	 * How many genes remain after filtering?
	 */
	public int genesRemainingAfterFiltering(double cutoff) {
		int res = 0;
		for(Object o : this.data) {
			if(TimeSeriesModel.geneFulfillsCutoff((mRNATimeSeries) o, getSignalType(), cutoff))
				res++;
		}
		return res;
	}
}
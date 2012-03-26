///*
// * $Id$
// * $URL$
// * ---------------------------------------------------------------------
// * This file is part of Integrator, a program integratively analyze
// * heterogeneous microarray datasets. This includes enrichment-analysis,
// * pathway-based visualization as well as creating special tabular
// * views and many other features. Please visit the project homepage at
// * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
// * obtain the latest version of Integrator.
// *
// * Copyright (C) 2011 by the University of Tuebingen, Germany.
// *
// * Integrator is free software; you can redistribute it and/or 
// * modify it under the terms of the GNU Lesser General Public License
// * as published by the Free Software Foundation. A copy of the license
// * agreement is provided in the file named "LICENSE.txt" included with
// * this software distribution and also available online as
// * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
// * ---------------------------------------------------------------------
// */
//package de.zbit.io.dna_methylation.importer;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Iterator;
//import java.util.List;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//
//import org.apache.commons.math.stat.inference.TTest;
//import org.apache.commons.math.stat.inference.TTestImpl;
//
//import de.zbit.data.Signal.SignalType;
//import de.zbit.data.methylation.DNAmethylation;
//import de.zbit.util.Utils;
//
///**
// * Unused.
// * @author Clemens Wrzodek
// * @version $Rev$
// */
//public class DNAmethRawData {
//  public static final transient Logger log = Logger.getLogger(DNAmethRawData.class.getName());
//  
//  /**
//   * A list of all raw probes.
//   */
//  List<DNAmethRawProbe> raw_data;
//  
//  /**
//   * Defines the size of one bin in bps.
//   */
//  private static int windowSize = 500;
//  
//  /**
//   * Observation names, matched to the instances
//   * of {@link DNAmethRawProbe#matched_controls} and
//   * {@link DNAmethRawProbe#matched_treatments}.
//   */
//  List<String> matched_observation_names;
//  
//  /**
//   * Logarithmize data before calculating values?
//   * Note: This is not a choice, but an option that must be set correctly.
//   * Data MUST BE logarithmized to calculate the correct fold change.
//   * Thus, if data is not yet logarithmized (contains no values <0),
//   * this should be true!
//   */
//  boolean logData=false;
//  
//  /**
//   * Bin-pack data and calculate p-values and fold changes for each bin.
//   * @param rawData A list of all raw probes.
//   */
//  public DNAmethRawData(List<DNAmethRawProbe> rawData) {
//    super();
//    this.raw_data = rawData;
//    
//    // Infere the log-data attribute
//    this.logData = !containsValuesBelowZero(rawData);
//    
//    // Set default observation names
//    int numObservations = rawData.iterator().next().getNumberOfObservations();
//    matched_observation_names = new ArrayList<String>(numObservations);
//    for (int i=1; i<=numObservations; i++) {
//      matched_observation_names.add(String.format("Observation %s", i));
//    }
//  }
//  
//  /**
//   * Bin-pack data and calculate p-values and fold changes for each bin.
//   * @param rawData A list of all raw probes.
//   * @param matched_observation_names Observation names, matched to the instances
//   * of {@link DNAmethRawProbe#matched_controls} and {@link DNAmethRawProbe#matched_treatments}.
//   * @param logData Logarithmize data before calculating values?
//   * Note: This is not a choice, but an option that must be set correctly.
//   * Data MUST BE logarithmized to calculate the correct fold change.
//   * Thus, if data is not yet logarithmized (contains no values <0),
//   * this should be true!
//   */
//  public DNAmethRawData(List<DNAmethRawProbe> rawData, List<String> matched_observation_names, boolean logData) {
//    // Do not call this() here, because this() takes a lot of time ;-)
//    super();
//    this.raw_data = rawData;
//    this.matched_observation_names = matched_observation_names;
//    this.logData = logData;
//  }
//  
//
//
//  /**
//   * @param rawData
//   * @return true if rawData contains any probe collection with a signal below 0.
//   */
//  private boolean containsValuesBelowZero(List<DNAmethRawProbe> rawData) {
//    Iterator<DNAmethRawProbe> it = rawData.iterator();
//    while (it.hasNext()) {
//      if (it.next().containsValuesBelowZero()) return true;
//    }
//    return false;
//  }
//
//
//
//  public List<DNAmethylation> generateProcessedData() {
//    List<DNAmethylation> toReturn = new ArrayList<DNAmethylation>();
//    /*
//     * Verfahren:
//     * 1. Alles einlesen, werte als float(?)
//     * 2. Comparator: GeneID primär und position sekundär
//     * 3. Anhand position fenster berechnen (<=OPTION für FENSTERBREITE)
//     * 4. für alle gepaarten werte in fenster t-test und fc berechnen
//     * 5. für result aufheben */
//    
//    Collections.sort(raw_data);
//    
//    // Process bins and calculate pVales, etc.
//    TTest ttest = new TTestImpl();
//    while (raw_data.size()>0) {
//      DNAmethRawProbe window = getNextWindow();
//      // GeneID, start-position, Signals(pValue, qValue, fc)
//      
//      // Generate instance and calc. values
//      // TODO: Setting "null" to name IS TEMPORARY AND *MUST* BE CHANGED
//      DNAmethylation bin = new DNAmethylation(null, window.geneID);
//      // TODO: window.position may not be null!
//      bin.setStart(window.position);
//      for (int i=0; i<matched_observation_names.size(); i++) {
//        
//        // Get treatment/ control and eventually logarithmize
//        double[] treatments = window.getObservationAsDoubleArray(i,true);
//        double[] controls   = window.getObservationAsDoubleArray(i,false);
//        if (logData) {
//          for (int c=0; c<treatments.length; c++) {
//            treatments[c] = DNAmethRawProbe.getLog2(treatments[c]);
//          }
//          for (int c=0; c<controls.length; c++) {
//            controls[c] = DNAmethRawProbe.getLog2(controls[c]);
//          }
//        }
//        
//        
//        // Calc. pValue
//        Double pValue = Double.NaN;
//        try {
//          pValue = ttest.tTest(treatments, controls);
//        } catch (Exception e) {
//          log.log(Level.WARNING,"Error while calculating p-value.", e);
//        }
//        bin.addSignal(pValue, matched_observation_names.get(i), SignalType.pValue);
//        
//        // Calc. FoldChange (required: logarithmized data)
//        bin.addSignal((Utils.average(treatments) - Utils.average(controls)), matched_observation_names.get(i), SignalType.FoldChange);
//      }
//      
//      // Add bin to returned list
//      toReturn.add(bin);
//    }
//    
//    return toReturn;
//  }
//
//
//
//  /**
//   * Returns the next window ("bin") from {@link #raw_data} and removes all
//   * returned elements from this list. Respects the
//   * maximum {@link #windowSize} and, of course, does not mix data for
//   * different genes.
//   * @param currentIndex
//   * @return
//   */
//  private DNAmethRawProbe getNextWindow() {
//    //List<DNAmethRawProbe> current = new ArrayList<DNAmethRawProbe>(windowSize/30);
//    Iterator<DNAmethRawProbe> it = raw_data.iterator();
//    
//    // Define entrez id and start position of current window
//    DNAmethRawProbe current = new DNAmethRawProbe();
//    while (it.hasNext()) {
//      DNAmethRawProbe probe = it.next();
//      if (current.geneID<=0) {
//        // Define variables on window start
//        current.geneID = probe.geneID;
//        current.position = probe.position;
//      } else {
//        // Check if probe falls within current window
//        if (probe.geneID!=current.geneID) break; // New Gene
//        if ((probe.position-current.position)>windowSize) break; // Current window is full
//      }
//      
//      // Add to current window
//      if (current.geneID>0) { // Simply throws away probes, without an associated geneID.
//        current.addSignals(probe);
//      }
//      it.remove();
//    }
//    
//    return current;
//  }
//  
//  
//}

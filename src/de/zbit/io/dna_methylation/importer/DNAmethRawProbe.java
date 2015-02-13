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
package de.zbit.io.dna_methylation.importer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.zbit.util.ArrayUtils;

/**
 * Unused.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class DNAmethRawProbe implements Serializable, Comparable<DNAmethRawProbe>{
  private static final long serialVersionUID = 211979178107090014L;

  /**
   * The natural logarithm of 2.
   */
  private final static double ln2 = Math.log(2);
  
  /**
   * Create a raw instance, with an unset geneID and
   * start position of zero.
   */
  public DNAmethRawProbe() {
    super();
    geneID=-1; position=0;
    matched_treatments = new ArrayList<List<Float>>();
    matched_treatments = new ArrayList<List<Float>>();
  }
  
  public DNAmethRawProbe(int geneID, int position) {
    this();
    this.geneID = geneID;
    this.position = position;
  }
  
  /**
   * NCBI Enrez Gene ID
   */
  int geneID;
  
  /**
   * Probe position
   */
  int position;
  
  // TODO: Test difference to doubles
  /**
   * For each observation, a list of contained signals,
   * coming from a treated probe.
   * Note: Indices must be matched with {@link #matched_controls}
   */
  List<List<Float>> matched_treatments;
  /**
   * For each observation, a list of contained signals,
   * coming from a control probe.
   * Note: Indices must be matched with {@link #matched_treatments}
   */
  List<List<Float>> matched_controls;
  
  
  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(DNAmethRawProbe o) {
    // IMPORTANT: Compare by geneID primarily and position secondary
    if (o==null) return -1;
    if (o==this) return 0;
    else {
      int ret = geneID - o.geneID;
      if (ret==0) {
        return position-o.position;
      } else {
        return ret;
      }
    }
  }

  /**
   * @param probe any probe of the same dataset. This means, the number
   * and indices of {@link #matched_controls} and {@link #matched_treatments}
   * must be exactly the same!
   */
  public void addSignals(DNAmethRawProbe probe) {
    for (int i=0; i<matched_treatments.size(); i++) {
      matched_treatments.get(i).addAll(probe.matched_treatments.get(i));
    }
    for (int i=0; i<matched_controls.size(); i++) {
      matched_controls.get(i).addAll(probe.matched_controls.get(i));
    }
  }

  /**
   * @param i observation index
   * @param treatment if true, returnes treatmens, if false, controls will be returned
   * @return
   */
  public double[] getObservationAsDoubleArray(int i, boolean treatment) {
    List<Float> list;
    if (treatment) list = matched_treatments.get(i);
    else list = matched_controls.get(i);
    
    return ArrayUtils.toDoubleArray(list);
  }

  /**
   * Logarithmizes all signals in this probe, to base 2.
   */
  public void log2TransformSignals() {
    for (int i=0; i<matched_treatments.size(); i++) {
      for (int j=0; j<matched_treatments.get(i).size(); j++) {
        double newVal = Math.log(matched_treatments.get(i).get(j).doubleValue())/ln2;
        matched_treatments.get(i).set(j, new Float(newVal) );
      }
    }
    for (int i=0; i<matched_controls.size(); i++) {
      for (int j=0; j<matched_controls.get(i).size(); j++) {
        double newVal = Math.log(matched_controls.get(i).get(j).doubleValue())/ln2;
        matched_controls.get(i).set(j, new Float(newVal) );
      }
    }
  }
  
  public static double getLog2(double val) {
    return Math.log(val)/ln2;
  }
  
  public static <T extends Number> double getLog2(T val) {
    return Math.log(val.doubleValue())/ln2;
  }

  /**
   * @return number of observations (equals size of {@link #matched_controls}).
   */
  public int getNumberOfObservations() {
    return matched_controls.size();
  }

  /**
   * @return true if this probe collection contains any signal below 0.
   */
  public boolean containsValuesBelowZero() {
    for (int i=0; i<matched_treatments.size(); i++) {
      for (int j=0; j<matched_treatments.get(i).size(); j++) {
        if ((matched_treatments.get(i).get(j))<0) return true;
      }
    }
    for (int i=0; i<matched_controls.size(); i++) {
      for (int j=0; j<matched_controls.get(i).size(); j++) {
        if ((matched_controls.get(i).get(j))<0) return true;
      }
    }
    return false;
  }
  
  
}

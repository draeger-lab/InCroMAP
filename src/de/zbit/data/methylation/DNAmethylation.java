/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/Integrator> to
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
package de.zbit.data.methylation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import de.zbit.data.GeneID;
import de.zbit.data.NSwithProbes;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.util.Utils;

/**
 * A generic class to hold DNA methylation data with annotated genes
 * (gene-based, with geneID), current probe position and Signals. 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class DNAmethylation extends NSwithProbes {
  private static final long serialVersionUID = -6002300790004775432L;
  public static final transient Logger log = Logger.getLogger(DNAmethylation.class.getName());
  
  /**
   * Probe start is added to {@link #addData(String, Object)} with this key.
   */
  public final static String probeStartKey = "Probe_position_start";
  
  /**
   * Probe end is added to {@link #addData(String, Object)} with this key.
   */
  public final static String probeEndKey = "Probe_position_end";
  
  public DNAmethylation(String geneName) {
    this (geneName, GeneID.default_geneID);
  }
  
  /**
   * @param geneName
   * @param geneID
   */
  public DNAmethylation(String geneName, Integer geneID) {
    // Yes, we could have included the probe name.
    // But since it is not used anywhere, we removed it.
    super(null, geneName, geneID);
    unsetProbeName();
  }
  
  /**
   * @return the probe start (or null).
   */
  public Integer getProbeStart() {
    Object probeStart = getData(probeStartKey);
    return probeStart==null?null:(Integer)probeStart;
  }
  
  /**
   * Set the corresponding probe start.
   */
  public void setProbeStart(Integer probeStart) {
    super.addData(probeStartKey, probeStart);
  }
  
  /**
   * Remove the probe Start
   */
  public void unsetProbeStart() {
    super.removeData(probeStartKey);
  }
  
  /**
   * @return the probe end (or null).
   */
  public Integer getProbeEnd() {
    Object probeEnd = getData(probeEndKey);
    return probeEnd==null?null:(Integer)probeEnd;
  }
  
  /**
   * Set the corresponding probe end.
   */
  public void setProbeEnd(Integer probeEnd) {
    super.addData(probeEndKey, probeEnd);
  }
  
  /**
   * Remove the probe end
   */
  public void unsetProbeEnd() {
    super.removeData(probeEndKey);
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    DNAmethylation nm = new DNAmethylation(name, getGeneID());
    return super.clone(nm);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NSwithProbes#hashCode()
   */
  @Override
  public int hashCode() {
    Integer start = getProbeStart();
    return super.hashCode() + (start==null?3:start);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NSwithProbes#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(Object o) {
    int r = super.compareTo(o); // compares name, GeneID, (and the currently unused probe id)
    if (o instanceof DNAmethylation) {
      DNAmethylation ot = (DNAmethylation) o;
      if (r==0) {
        r = Utils.compareIntegers(getProbeStart(), ot.getProbeStart());
        if (r==0) {
          r = Utils.compareIntegers(getProbeEnd(), ot.getProbeEnd());
        }
      }
    } else {
      return -2;
    }
    
    return r;
  }
  
  
  protected <T extends NameAndSignals> void merge(Collection<T> source, T target, Signal.MergeType m) {
    super.merge(source, target, m);
    
    // This is required to ensure having (min of) integers as start and end positions.
    List<Integer> positions = new ArrayList<Integer>(source.size());
    for (T o : source) {
      Integer s = ((DNAmethylation) o).getProbeStart();
      if (s!=null) positions.add(s);
    }
    if (positions.size()>0) {
      double averagePosition = Utils.min(positions);
      ((DNAmethylation) target).setProbeStart((int)(averagePosition));
    }
    
    // End pos (max and integer)
    positions = new ArrayList<Integer>(source.size());
    for (T o : source) {
      Integer s = ((DNAmethylation) o).getProbeEnd();
      if (s!=null) positions.add(s);
    }
    if (positions.size()>0) {
      double averagePosition = Utils.max(positions);
      ((DNAmethylation) target).setProbeEnd((int)(averagePosition));
    }
    
    
  }
  
  
}

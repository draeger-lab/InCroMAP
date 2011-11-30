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
package de.zbit.io;

import java.util.LinkedList;
import java.util.List;

import de.zbit.data.Chromosome;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.gui.csv.ExpectedColumn;


/**
 * Basic reader to read {@link DNAmethylation} data.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class DNAMethylationReader extends AbstractGeneBasedNSreader<DNAmethylation> {
  
  /**
   * Probe position start (or just "position").
   */
  private int probeStartCol=-1;
  
  /**
   * Probe position end (in doubt, better set {@link #probeStartCol}).
   */
  private int probeEndCol=-1;
  
  /**
   * Actually not required, but leads to nicer captions for each NS.
   */
  private int probeNameCol=-1;
  
  /**
   * Column containining chromosome information
   */
  private int chromosomeCol=-1;
  
  /**
   * Integer to set if {@link #probeStartCol} or {@link #probeEndCol}
   * is >=0 (set), but the column contains invalid data.
   */
  private final static Integer invalidOrMissingPosition = -1;
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#getAdditionalExpectedColumns()
   */
  @Override
  protected List<ExpectedColumn> getAdditionalExpectedColumns() {
    List<ExpectedColumn> list = new LinkedList<ExpectedColumn>();
    list.add(new ExpectedColumn("Probe name", false));
    list.add(new ExpectedColumn("Probe position (start)", false));
    list.add(new ExpectedColumn("Probe position (end)", false));
    list.add(new ExpectedColumn("Chromosome", false, Chromosome.chromosome_regex.pattern()));
    return list;
  }

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#processAdditionalExpectedColumns(java.util.List)
   */
  @Override
  protected void processAdditionalExpectedColumns(List<ExpectedColumn> additional) {
    ExpectedColumn probeName = additional.get(0);
    if (probeName.hasAssignedColumns()) {
      probeNameCol = probeName.getAssignedColumn();
    }    
    
    // Assign probe start/ end. If only one is assigned, assign to start!
    ExpectedColumn probeStart = additional.get(1);
    if (probeStart.hasAssignedColumns()) {
      probeStartCol = probeStart.getAssignedColumn();
    }
    
    ExpectedColumn probeEnd = additional.get(2);
    if (probeEnd.hasAssignedColumns()) {
      if (probeStartCol<0) {
        probeStartCol = probeEnd.getAssignedColumn();
      } else {
        probeEndCol = probeEnd.getAssignedColumn();
      }
    }
    
    // Assign probe start/ end. If only one is assigned, assign to start!
    ExpectedColumn chromosome = additional.get(3);
    if (chromosome.hasAssignedColumns()) {
      chromosomeCol = chromosome.getAssignedColumn();
    }
  }

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#createObject(java.lang.String, java.lang.Integer, java.lang.String[])
   */
  @Override
  protected DNAmethylation createObject(String name, Integer geneID,
    String[] line) {
    
    // Eventually change the name.
    if (probeNameCol>=0) {
      name = line[probeNameCol];
    }
    
    // Set name and geneID
    DNAmethylation m = new DNAmethylation(name, geneID);
    
    // Set probe start
    if (probeStartCol>=0) {
      Integer s = invalidOrMissingPosition;
      try {
        s = Integer.parseInt(line[probeStartCol]);
      } catch (NumberFormatException e) {
        s=invalidOrMissingPosition;
      }
      m.setProbeStart(s);
    } else {
      m.unsetProbeStart();
    }
    
    // Set probe end
    if (probeEndCol>=0) {
      Integer s = invalidOrMissingPosition;
      try {
        s = Integer.parseInt(line[probeEndCol]);
      } catch (NumberFormatException e) {
        s=invalidOrMissingPosition;
      }
      m.setProbeEnd(s);
    } else {
      m.unsetProbeEnd();
    }
    
    // Set Chromosome
    if (chromosomeCol>=0) {
      m.setChromosome(line[chromosomeCol]);
    } else {
      m.unsetChromosome();
    }
    
    return m;
  }

}

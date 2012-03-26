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
package de.zbit.io;

import java.util.LinkedList;
import java.util.List;

import de.zbit.data.NameAndSignals;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.sequence.region.Chromosome;
import de.zbit.sequence.region.Region;
import de.zbit.util.Species;

/**
 * An Abstract extension of {@link AbstractGeneBasedNSreader} to read
 * gene- and region-based data, that might contain various gene identifiers
 * (such as Ensembl, KEGG, entrez gene ids), signals, chromosome, start, end
 * and further additional columns.
 * @author Clemens Wrzodek
 * @version $Rev$
 * @param <T>
 */
public abstract class AbstractGeneAndRegionBasedNSreader<T extends NameAndSignals & Region> extends AbstractGeneBasedNSreader<T> {

  /**
   * Probe position start (or just "position").
   */
  protected int probeStartCol=-1;
  
  /**
   * Probe position end (in doubt, better set {@link #probeStartCol}).
   */
  protected int probeEndCol=-1;
  
  /**
   * Column containining chromosome information
   */
  protected int chromosomeCol=-1;
  
  /**
   * Integer to set if {@link #probeStartCol} or {@link #probeEndCol}
   * is >=0 (set), but the column contains invalid data.
   */
  private final static Integer invalidOrMissingPosition = Region.DEFAULT_START;
  
  
  /**
   * This is ONLY for use in combination with {@link #importWithGUI(String)} afterwards.
   */
  public AbstractGeneAndRegionBasedNSreader() {
    super();
  }
  

  /**
   * 
   * @param identifierCol
   * @param idType
   * @param species
   * @param startCol
   * @param endCol
   * @param chromosomeCol
   */
  public AbstractGeneAndRegionBasedNSreader(int identifierCol, IdentifierType idType, Species species,
    int startCol, int endCol, int chromosomeCol) {
    super(identifierCol, idType, species);
    this.probeStartCol = startCol;
    this.probeEndCol = endCol;
    this.chromosomeCol = chromosomeCol;
  }
  
  
  /**
   * You may overwrite this method to set the required flag on range properties.
   * @return
   */
  protected boolean isRangeRequired() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#getAdditionalExpectedColumns()
   */
  @Override
  protected List<ExpectedColumn> getAdditionalExpectedColumns() {
    boolean required = isRangeRequired();
    List<ExpectedColumn> list = new LinkedList<ExpectedColumn>();
    list.add(new ExpectedColumn("Probe position (start)", required));
    list.add(new ExpectedColumn("Probe position (end)", false)); // we can always build a range with just start
    list.add(new ExpectedColumn("Chromosome", required, Chromosome.chromosome_regex_with_forced_prefix));
    return list;
  }
  
  /**
   * @param exps
   * @return true if start and chromosome columns are assigned.
   */
  protected static boolean isStartAndChromosomeAssigned(List<ExpectedColumn> exps) {
    boolean startIsAssigned = false;
    boolean chromosomeIsAssigned = false;
    for (ExpectedColumn e : exps) {
      if (e.getName().equals("Probe position (start)") && e.hasAssignedColumns()) {
        startIsAssigned = true;
      } else if (e.getName().equals("Chromosome") && e.hasAssignedColumns()) {
        chromosomeIsAssigned = true;
      }
    }
    return chromosomeIsAssigned && startIsAssigned;
  }

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#processAdditionalExpectedColumns(java.util.List)
   */
  @Override
  protected void processAdditionalExpectedColumns(List<ExpectedColumn> additional) {
    
    // Assign probe start/ end. If only one is assigned, assign to start!
    ExpectedColumn probeStart = additional.get(0);
    if (probeStart.hasAssignedColumns()) {
      probeStartCol = probeStart.getAssignedColumn();
    }
    
    ExpectedColumn probeEnd = additional.get(1);
    if (probeEnd.hasAssignedColumns()) {
      if (probeStartCol<0) {
        probeStartCol = probeEnd.getAssignedColumn();
      } else {
        probeEndCol = probeEnd.getAssignedColumn();
      }
    }
    
    // Assign probe start/ end. If only one is assigned, assign to start!
    ExpectedColumn chromosome = additional.get(2);
    if (chromosome.hasAssignedColumns()) {
      chromosomeCol = chromosome.getAssignedColumn();
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#createObject(java.lang.String, java.lang.Integer, java.lang.String[])
   */
  @Override
  protected T createObject(String name, Integer geneID, String[] line) {
    
    // Parse probe start
    Integer start = null;
    if (probeStartCol>=0) {
      start = invalidOrMissingPosition;
      try {
        start = Integer.parseInt(line[probeStartCol]);
      } catch (NumberFormatException e) {
        start=invalidOrMissingPosition;
      }
    } 
    
    // Parse probe end
    Integer end = null;
    if (probeEndCol>=0) {
      end = invalidOrMissingPosition;
      try {
        end = Integer.parseInt(line[probeEndCol]);
      } catch (NumberFormatException e) {
        end=invalidOrMissingPosition;
      }
    } 
    
    // Set Chromosome
    String chromosome = null;
    if (chromosomeCol>=0) {
      chromosome = (line[chromosomeCol]);
    }
    
    return createObject(name, geneID, start, end, chromosome, line);
  }


  /**
   * Create your instance of T from the CSV file. Most properties
   * are already parsed and also the signals is being taken care of.
   * Additional values from the CSV file can be parsed here.
   * Else, the return is simply
   * <pre>
   * return new MyClass(name, geneID);
   * </pre> 
   * @param name most human readable gene name
   * @param geneID <b>might be <code>NULL</code></b>, if unknown!
   * @param start null if optional column has not been associated, else
   * the starting position or {@link #invalidOrMissingPosition}.
   * @param end null if optional column has not been associated, else
   * the starting position or {@link #invalidOrMissingPosition}.
   * @param chromosome null if optional column has not been associated, else
   * the raw chromosome string.
   * @param line current line of CSV file
   * @return instance of T
   */
  protected abstract T createObject(String name, Integer geneID, Integer start, Integer end, String chromosome, String[] line);
  
}

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
package de.zbit.io;

import java.io.IOException;
import java.util.List;

import de.zbit.data.methylation.DNAmethylation;
import de.zbit.gui.GUITools;
import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.csv.ExpectedColumn;


/**
 * Basic reader to read {@link DNAmethylation} data.
 * 
 * <p>Can also map data to gene or gene promoters during reading.</p>
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class DNAmethylationReader extends AbstractGeneAndRegionBasedNSreader<DNAmethylation> {

  /**
   * Column containining chromosome and position information (REGEX: "CHR.{1,2}?FS\\d+").
   */
  protected int chromosomeAndPositionCol=-1;
  
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneAndRegionBasedNSreader#createObject(java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String[])
   */
  @Override
  protected DNAmethylation createObject(String name, Integer geneID,
    Integer start, Integer end, String chromosome, String[] line) {
    
    // Initialize instance
    DNAmethylation m = new DNAmethylation(name, geneID, start, end, chromosome);
    
    // Set Chromosome and Position, if they are in one column
    if (chromosomeAndPositionCol>=0 && chromosomeAndPositionCol<line.length) {
      try {
        chromosome = (line[chromosomeAndPositionCol]);
        int positionPos = (line[chromosomeAndPositionCol].indexOf("FS")+2);
        m.setChromosome(chromosome);
        m.setStart(Integer.parseInt(line[chromosomeAndPositionCol].substring(positionPos)));
      } catch (Exception e) {
        String warning = String.format("Could not parse ChromosomeAndPosition column '%s'. It must be in format \"CHRxxFS0000\".", line[chromosomeAndPositionCol]);
        logWarning(warning);
      }
    }
    
    // Eventually assign a gene ID
    if (nameCol<0 && toGeneMapper!=null) {
      m = toGeneMapper.map(m);
    }
    
    return m;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneAndRegionBasedNSreader#getAdditionalExpectedColumns()
   */
  @Override
  protected List<ExpectedColumn> getAdditionalExpectedColumns() {
    List<ExpectedColumn> exp = super.getAdditionalExpectedColumns();
   
    ExpectedColumn cp = new ExpectedColumn("Chr. & Position",false, "CHR.{1,2}?FS\\d+");
    // This should be done in additionalColumnAssignmentCheck().
    exp.add(cp);
    
    return exp;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#getExpectedColumns()
   */
  @Override
  public ExpectedColumn[] getExpectedColumns() {
    ExpectedColumn[] exp = super.getExpectedColumns();
    // Mark all as non-required, because we can't express
    // "either gene identifier or probe positions".
    for (ExpectedColumn e : exp) {
      e.setRequired(false);
    }
    return exp;
  }
  
  /**
   * @param exps
   * @return
   */
  private static boolean isChrAndPositionAssigned(List<ExpectedColumn> exps) {
    for (ExpectedColumn e : exps) {
      if (e.getName().equals("Chr. & Position") && e.hasAssignedColumns()) {
        return true;
      }
    }
    return false;
  }
  
  private boolean checkRegEXmatchForChrAndPosition(CSVImporterV2 c) {
    for (ExpectedColumn e : c.getExpectedColumns()) {
      if (e.getName().equals("Chr. & Position") && e.hasAssignedColumns()) {
        
        // Get some content
        String[] anyContentLine;
        try {
          anyContentLine = c.getCSVReader().getNextLine();
        } catch (IOException e1) {
          return false;
        }
        
        // Check agains
        if (!e.regEXmatches(anyContentLine)) {
          return false;
        }
        
        
        return true;
      }
    }
    return false;
  }
  
  // c.getCSVReader().getNextLine()
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneAndRegionBasedNSreader#processAdditionalExpectedColumns(java.util.List)
   */
  @Override
  protected void processAdditionalExpectedColumns(List<ExpectedColumn> additional) {
    super.processAdditionalExpectedColumns(additional);
    
    // Assign probe start/ end. If only one is assigned, assign to start!
    ExpectedColumn chrAndPos = additional.get(additional.size()-1); // = 3
    if (chrAndPos.hasAssignedColumns()) {
      chromosomeAndPositionCol = chrAndPos.getAssignedColumn();
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#getName(java.lang.String[])
   */
  @Override
  protected String getName(String[] line) {
    if (nameCol<0) {
      if (chromosomeAndPositionCol>=0) {
        if (chromosomeAndPositionCol>=line.length) {
          return null;
        }
        return line[chromosomeAndPositionCol];
      }
    }
    return super.getName(line);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#additionalColumnAssignmentCheck(de.zbit.gui.csv.CSVImporterV2)
   */
  @Override
  protected boolean additionalColumnAssignmentCheck(CSVImporterV2 c) {
    List<ExpectedColumn> expCols = c.getExpectedColumns();
    
    /* Checks if at least the IDENTIFER column or
     * CHROMOSOME and START columns, or both are set.
     */
    
    //Note: identifier is expected to be the first column!
    if (expCols.get(0).hasAssignedColumns()) {
      // A) We already have a mapping to genes
      return true;
    } else if (isStartAndChromosomeAssigned(expCols)) {
      // B) We have separate chromosome and position columns
      // We need to avoid warnings, trying to parse non-existing geneIDs
      setSupressWarnings(true);
      return initializeGenomicMapper();
    }
    else if (isChrAndPositionAssigned(expCols)) {
      // C) We have one merged chromosome and position column
      // We need to avoid warnings, trying to parse non-existing geneIDs
      if (!checkRegEXmatchForChrAndPosition(c)) {
        String message = "The \"Chr. and position\" column must contain information in this format: \"CHR{chromosome}FS{position}\". Example: CHR12FS1234";
        GUITools.showErrorMessage(null, message);
        return false;
      }
      setSupressWarnings(true);
      return initializeGenomicMapper();
    }
    else {
      String message = "You must assign at least one of the following columns:\n"+
      "- any gene \"Identifier\"\n"+
      "- the \"Chromosome\" and \"" + posStartText + "\" columns\n" +
      "- the \"Chromosome & Position\" column.\n"+
      "\n"+
      "If a gene-identifer is explicitly given, no mapping will be performed. In the latter two cases, all data will be mapped on genes or gene promoters.\n"+
      "In any case, it is recommended to have chromosomal positions with your data.";
      GUITools.showErrorMessage(null, message);
      return false;
    }
  }
  

}

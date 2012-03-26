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

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import de.zbit.data.GeneID;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.snp.SNP;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.integrator.ReaderCache;
import de.zbit.integrator.ReaderCacheElement;
import de.zbit.io.csv.CSVReader;
import de.zbit.mapper.AbstractMapper;
import de.zbit.mapper.SNPid2GeneIDmapper;
import de.zbit.util.Species;

/**
 * A generic reader to read {@link SNP} data.
 * @author Finja Büchel
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class SNPReader extends NameAndSignalReader<SNP> {
  public static final transient Logger log = Logger.getLogger(SNPReader.class.getName());
  
  /**
   * Required to initialize the mapper.
   */
  private Species species;
  
  /**
   * Map the ID to an NCBI gene id.
   */
  private AbstractMapper<Integer, Integer> mapper = null;

  /**
   * Remember already issued warnings to not issue it multiple times.
   */
  private Set<String> issuedWarnings = new HashSet<String>();
  
  /**
   * This is ONLY for use in combination with {@link #importWithGUI(String)} afterwards.
   */
  public SNPReader() {
    super(-1);
  }
  
  public SNPReader(int dbSNPidColumn, Species species) {
    super(dbSNPidColumn);
    this.species = species;
  }
  
  /**
   * @return This method returns all {@link ExpectedColumn}s required
   * to read a new file with the {@link CSVImporterV2}.
   */
  public ExpectedColumn[] getExpectedColumns() {
    List<ExpectedColumn> list = new ArrayList<ExpectedColumn>();

    ExpectedColumn e = new ExpectedColumn("dbSNP identifier", true);
    e.setRegExPatternForInitialSuggestion("rs|ss\\d+");
    list.add(e);
    
    list.addAll(NameAndSignalReader.getExpectedSignalColumns(1));
    
    return list.toArray(new ExpectedColumn[0]);
  }

  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#importWithGUI(java.awt.Component, java.lang.String, de.zbit.integrator.ReaderCache)
   */
  @Override
  public Collection<SNP> importWithGUI(Component parent, String file, ReaderCache cache) {

    // Create a new panel that allows selection of species
    JLabeledComponent spec = IntegratorUITools.getOrganismSelector();
    
    // Create and show the import dialog
    try {
      // Definitions of required and optional columns
      ExpectedColumn[] exCol = getExpectedColumns();
      CSVReader inputReader = loadConfigurationFromCache(cache, file, exCol, spec);
      
      // Show the CSV Import dialog
      final CSVImporterV2 c = new CSVImporterV2(inputReader, exCol);
      boolean dialogConfirmed = IntegratorUITools.showCSVImportDialog(parent, c, spec);
      
      // Process user input and read data
      if (dialogConfirmed) {
        // Store in cache
        this.species = (Species) spec.getSelectedItem();
        if (cache!=null) cache.add(ReaderCacheElement.createInstance(c, species));
        
        // Set dbSNP id column
        nameCol = exCol[0].getAssignedColumn();
        
        // Signal columns
        int offset = 1;
        for (int i=offset; i<exCol.length; i++) {
          if (exCol[i].hasAssignedColumns()) {
            for (int j=0; j<exCol[i].getAssignedColumns().size(); j++) {
              addSignalColumn(exCol[i].getAssignedColumns().get(j), 
                (SignalType) exCol[i].getAssignedType(j), exCol[i].getName().toString());
            }
          }
        }
        
        try {
          return read(c.getApprovedCSVReader());
        } catch (Exception e) {
          GUITools.showErrorMessage(parent, e, "Could not read input file.");
        }
      }
      
    } catch (IOException e) {
      GUITools.showErrorMessage(parent, e);
    }

    
    // Only errors or canceled
    return null;
  }


  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#getSpecies()
   */
  @Override
  public Species getSpecies() {
    return species;
  }

  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#read(de.zbit.io.CSVReader)
   */
  @Override
  public Collection<SNP> read(CSVReader inputCSV) throws IOException, Exception {
    // Init Mapper
    log.info("Reading dbSNP2GeneID mapping file...");
    mapper = new SNPid2GeneIDmapper(getSecondaryProgressBar(), species.getNCBITaxonID());
    
    // Read file
    Collection<SNP> ret =  super.read(inputCSV);
    
    // Free resources
    mapper = null;
    return ret;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#read(java.lang.String[])
   */
  @Override
  public Collection<SNP> read(String[] identifiers) throws IOException, Exception {
    // Init Mapper
    log.info("Reading dbSNP2GeneID mapping file...");
    mapper = new SNPid2GeneIDmapper(getSecondaryProgressBar(), species.getNCBITaxonID());
    
    // Read file
    Collection<SNP> ret =  super.read(identifiers);
    
    // Free resources
    mapper = null;
    return ret;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#createObject(java.lang.String, java.lang.String[])
   */
  @Override
  protected SNP createObject(String name, String[] line) throws Exception {
    // Map to GeneID
    Integer geneID = null;
    
    // Trim rs or ss from SNP id
    if (name==null || name.length()<3) return null;
    String start = name.substring(0, 2);
    if (start.equalsIgnoreCase("rs") || start.equalsIgnoreCase("ss")) {
      try {
        geneID = mapper.map(Integer.parseInt(name.substring(2)));
        if (geneID!=null && geneID<=0) geneID=null;
      } catch (NumberFormatException e) {
        String warning = String.format("Could not parse GeneID from String '%s'.", name);
        if (!issuedWarnings.contains(warning)) {
          log.warning(warning);
          issuedWarnings.add(warning);
        }
        
        geneID=null;
      }
    } 
    
    // Create SNP instance
    if (geneID==null || geneID<=0) geneID=GeneID.default_geneID;
    SNP snp = new SNP(name, geneID);
    
    return snp;
  }
  
}

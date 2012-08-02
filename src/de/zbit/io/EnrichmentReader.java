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
 * Copyright (C) 2011-2012 by the University of Tuebingen, Germany.
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
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.zbit.analysis.enrichment.AbstractEnrichment;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.Signal.SignalType;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.integrator.ReaderCache;
import de.zbit.integrator.ReaderCacheElement;
import de.zbit.io.csv.CSVReader;
import de.zbit.mapper.AbstractMapper;
import de.zbit.mapper.KeggPathwayID2PathwayName;
import de.zbit.math.BenjaminiHochberg;
import de.zbit.util.DatabaseIdentifiers;
import de.zbit.util.Species;
import de.zbit.util.Utils;
import de.zbit.util.objectwrapper.ValueTriplet;

/**
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class EnrichmentReader extends NameAndSignalReader<EnrichmentObject<String>> {
  
  /**
   * 
   */
  private static final long serialVersionUID = 4370876883388217923L;

  /**
   * Regex to parse a KEGG PATHWAY identifier
   */
  public final static String KEGG_ID_REGEX = 
    ".*?(path:)?(" + DatabaseIdentifiers.getPlainRegularExpressionForIdentifier(DatabaseIdentifiers.IdentifierDatabases.KEGG_Pathway) + ").*";
  
  /**
   * Compiled {@link #KEGG_ID_REGEX}.
   */
  private final Pattern KEGG_ID_PATTERN = Pattern.compile(KEGG_ID_REGEX);
  
  /**
   * Mapping from Enrichment class ID to Name (KEGG Pathway Name)
   */
  protected AbstractMapper<String, String> enrich_ID2Name=null;
  
  /**
   * This is ONLY for use in combination with {@link #importWithGUI(String)} afterwards.
   */
  public EnrichmentReader() {
    super(-1);
  }
  
  /**
   * @param nameCol
   * @param signalColumns
   */
  public EnrichmentReader(int nameCol,
    Collection<ValueTriplet<Integer, SignalType, String>> signalColumns) {
    super(nameCol, signalColumns);
    // TODO Auto-generated constructor stub
  }
  
  /**
   * @return This method returns all {@link ExpectedColumn}s required
   * to read a new file with the {@link CSVImporterV2}.
   */
  public ExpectedColumn[] getExpectedColumns() {
    List<ExpectedColumn> list = new ArrayList<ExpectedColumn>();

    // KEGG Pathway identifier by regex parsing
    // GSEA: "HSA03320_PPAR_SIGNALING_PATHWAY", DAVID: "hsa04010:MAPK signaling pathway", InCro: "path:hsa04010"
    ExpectedColumn e = new ExpectedColumn("KEGG PATHWAY identifier", true);
    e.setRegExPatternForInitialSuggestion(KEGG_ID_REGEX);
    list.add(e);
    
    // P-value column
    // DAVID: "PValue", GSEA: "NOM p-val", InCro: "P-value"
    // => First column wo ohne minuns ein "pval" enthalten ist.
    ExpectedColumn ep = new ExpectedColumn("P-value", true) {
      public int getInitialSuggestion(de.zbit.io.csv.CSVReader r) {
        int sug = r.getColumnContaining("p-val");
        if (sug<0) {
          sug = r.getColumnContaining("pval");
        }
        return sug;
      };
    };
    list.add(ep);
    
    return list.toArray(new ExpectedColumn[0]);
  }

  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#importWithGUI(java.awt.Component, java.lang.String, de.zbit.integrator.ReaderCache)
   */
  @Override
  public Collection<EnrichmentObject<String>> importWithGUI(
    Component parent, String file, ReaderCache cache) {
    
    // Create and show the import dialog
    try {
      // Definitions of required and optional columns
      ExpectedColumn[] exCol = getExpectedColumns();
      CSVReader inputReader = loadConfigurationFromCache(cache, file, exCol, null);
      
      // Show the CSV Import dialog
      final CSVImporterV2 c = new CSVImporterV2(inputReader, exCol);
      boolean dialogConfirmed = IntegratorUITools.showCSVImportDialog(parent, c, null);
      
      // Process user input and read data
      if (dialogConfirmed) {
        // Store in cache
        if (cache!=null) cache.add(ReaderCacheElement.createInstance(c, null));
        
        // Set KEGG ID column
        nameCol = exCol[0].getAssignedColumn();
        
        // P-Value column
        addSignalColumn(exCol[1].getAssignedColumn(), SignalType.pValue, EnrichmentObject.signalNameForPvalues);
        
        try {
          // Init the enriched id 2 readable name mapping (e.g. Kegg Pathway ID 2 Kegg Pathway Name mapping)
          if (enrich_ID2Name==null) {
            enrich_ID2Name = getDefaultEnrichmentID2NameMapping();
          }
          
          // Read data
          Collection<EnrichmentObject<String>> result = read(c.getApprovedCSVReader());
          
          // Calculate corrected p-value
          List<EnrichmentObject<String>> resultList = Utils.collectionToList(result);
          new BenjaminiHochberg().setQvalue(resultList);
          
          enrich_ID2Name = null;
          return resultList;
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
   * @see de.zbit.io.NameAndSignalReader#createObject(java.lang.String, java.lang.String[])
   */
  @Override
  protected EnrichmentObject<String> createObject(String name, String[] line) throws Exception {
    
    // Parse KEGG pathway identifier
    String id = null;
    Matcher m = KEGG_ID_PATTERN.matcher(name);
    if (m.find() && m.groupCount()>=2 && m.group(2)!=null) {
      id = "path:" + m.group(2).toLowerCase();
    } else {
      log.warning("Could not parse a KEGG PATHWAY identifier from '" + name + "'.");
      return null;
    }
    
    // Convert id to pathway name
    String pw_name = AbstractEnrichment.getEnrichedObjectName(id, enrich_ID2Name);
    if (pw_name==null) {
      pw_name = name;
    }
    
    
    return new EnrichmentObject<String>(pw_name, id);
  }
  
  protected AbstractMapper<String, String> getDefaultEnrichmentID2NameMapping() {
    KeggPathwayID2PathwayName pwID2name_mapper=null;
    try {
      pwID2name_mapper = new KeggPathwayID2PathwayName(getSecondaryProgressBar());
    } catch (IOException e) { // not severe, will leave the id field blank.
      log.log(Level.WARNING, "Could not read KEGG pathway mapping file.", e);
    }
    return pwID2name_mapper;
  }

  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#getSpecies()
   */
  @Override
  public Species getSpecies() {
    // TODO Is it ok to return null here?
    // => This will lead to a separate dialog, asking the user for the species.
    return null;
  }
  
}

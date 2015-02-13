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

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtargets;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.integrator.ReaderCache;
import de.zbit.integrator.ReaderCacheElement;
import de.zbit.io.csv.CSVReader;
import de.zbit.mapper.MicroRNAsn2GeneIDMapper;
import de.zbit.util.Species;
import de.zbit.util.objectwrapper.ValueTriplet;

/**
 * A generic reader to read {@link miRNA} data.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class miRNAReader extends NameAndSignalReader<miRNA> {
  /**
   * 
   */
  private static final long serialVersionUID = -8978055155758978755L;

  public static final transient Logger log = Logger.getLogger(miRNAReader.class.getName());
  
  /**
   * Optional: Column with probe name
   */
  private int probeNameCol=-1;
  
  /**
   * This is not required! It is an optional information.
   */
  private Species species=null;
  
  /**
   * Annotate miRNAs with an entrez gene id.
   */
  private MicroRNAsn2GeneIDMapper miRNA2GeneIdMapper;
  
  /**
   * @return  This method returns all {@link ExpectedColumn}s required
   * to read a new file with the {@link CSVImporterV2}. This is
   * [0] miRNA identifier, [1] probeNames and [2-10] signal columns.
   */
  public static ExpectedColumn[] getExpectedColumns() {
    List<ExpectedColumn> list = new ArrayList<ExpectedColumn>();
    
    list.add(new ExpectedColumn("miRNA systematic name",null,true,false,false,false,miRNATargetReader.miRNAidentifierRegEx));
    list.add(new ExpectedColumn("Probe name",false));
    
    list.add(NameAndSignalReader.getCustomAnnotationColumn());
    list.addAll(NameAndSignalReader.getExpectedSignalColumns(15));
    
    return list.toArray(new ExpectedColumn[0]);
  }
  

  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#importWithGUI(java.awt.Component, java.lang.String, de.zbit.integrator.ReaderCache)
   */
  @Override
  public Collection<miRNA> importWithGUI(Component parent, String file, ReaderCache cache) {
    
    // Create a new panel that allows selection of species (Not required! Optional)
    JLabeledComponent spec = IntegratorUITools.getOrganismSelector();
    
    // Create and show the import dialog
    try {
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
        
        // Read all columns and types
        nameCol = exCol[0].getAssignedColumn();
        probeNameCol = exCol[1].getAssignedColumn();
        parseCustomAnnotationColumn(exCol[2]);
        for (int i=3; i<exCol.length; i++) {
          // Signals are left-over
          if (exCol[i].hasAssignedColumns()) {
            for (int j=0; j<exCol[i].getAssignedColumns().size(); j++) {
              addSignalColumn(exCol[i].getAssignedColumns().get(j), 
                (SignalType) exCol[i].getAssignedType(j), exCol[i].getName().toString());
            }
          }
        }
        
        try {
          //Utils.saveObject(FileTools.removeFileExtension(c.getApprovedCSVReader().getFilename())+"-reader.dat", this);
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
  
  /**
   * This is ONLY for use in combination with {@link #importWithGUI(String)} afterwards.
   */
  public miRNAReader() {
    this(-1);
  }
  
  public miRNAReader(int miRNAnameCol) {
    super(miRNAnameCol);
  }
  public miRNAReader(int miRNAnameCol, int probeNameCol) {
    super(miRNAnameCol);
    this.probeNameCol = probeNameCol;
  }
  public miRNAReader(int miRNAnameCol, int probeNameCol, Collection<ValueTriplet<Integer, SignalType, String>> signalColumns) {
    super(miRNAnameCol, signalColumns);
    this.probeNameCol = probeNameCol;
  }

  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#createObject(java.lang.String, java.lang.String[])
   */
  @Override
  protected miRNA createObject(String name, String[] line) {
    String probeName = probeNameCol>=0?line[probeNameCol]:null;
    miRNA m;
    if (probeName!=null)
      m = new miRNA(name, probeName);
    else
      m = new miRNA(name);
    
    // Eventually unset probe name
    if (probeNameCol<0) m.unsetProbeName();
    
    // Set gene ID
    if (miRNA2GeneIdMapper!=null) {
      try {
        Integer geneId = miRNA2GeneIdMapper.map(m.getPrecursorName());
        if (geneId!=null && !Double.isNaN(geneId) && geneId>0) {
          m.setID(geneId);
        } else {
          m.setID(mRNA.default_geneID);
        }
      } catch (Exception e) {
        // Ignore
        log.log(Level.FINE, "Could not map miRNA systematic name 2 GeneID.", e);
        m.setID(mRNA.default_geneID);
      }
    }
    
    return m;
  }
  

  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#init()
   */
  @Override
  protected void init() {
    super.init();
    // Initialized the miRNAname to gene id mapper.
    try {
      miRNA2GeneIdMapper = new MicroRNAsn2GeneIDMapper(getSecondaryProgressBar());
      miRNA2GeneIdMapper.initialize();
    } catch (IOException e) {
      GUITools.showErrorMessage(null, e);
      miRNA2GeneIdMapper=null;
    }
  }


  /**
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    // Read miRNA
    miRNAReader r = new miRNAReader(1,0);
    r.addSignalColumn(25, SignalType.FoldChange, "Ctnnb1"); // 25-28 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    r.addSignalColumn(29, SignalType.pValue, "Ctnnb1"); // 29-32 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    
    Collection<miRNA> c = r.read("miRNA_data.txt");
    
    
    // Read targets
    miRNAtargets t_all = (miRNAtargets) SerializableTools.loadGZippedObject("miRNAtargets_HC.dat");
    //miRNAtargets t_all = (miRNAtargets) CSVwriteableIO.read(new miRNAtargets(), "miRNAtargets_HC.txt");
    int matched = miRNA.link_miRNA_and_targets(t_all, c);
    
    // Output
    for (miRNA miRNA : c) {
      System.out.println(miRNA);
    }
    
    // Print stats
    System.out.println("Total miRNAs: " + c.size() + " miRNAs_with_targets: " + matched + " = " +(matched/(double)c.size()*100.0)+ "%");
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#getSpecies()
   */
  @Override
  public Species getSpecies() {
    return species;
  }
}

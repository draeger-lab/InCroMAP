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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.zbit.data.NSwithProbes;
import de.zbit.data.NameAndSignals;
import de.zbit.data.id.GeneID;
import de.zbit.gui.GUITools;
import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.io.csv.CSVReader;
import de.zbit.io.dna_methylation.DNAmethylationDataMapper;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.sequence.region.AbstractRegion;
import de.zbit.sequence.region.Chromosome;
import de.zbit.sequence.region.ChromosomeTools;
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
   * This mapper allows to assign gene-ids for {@link Region}s.
   */
  protected transient DNAmethylationDataMapper toGeneMapper = null;
  
  /**
   * Integer to set if {@link #probeStartCol} or {@link #probeEndCol}
   * is >=0 (set), but the column contains invalid data.
   */
  private final static Integer invalidOrMissingPosition = Region.DEFAULT_START;
  
  /**
   * 
   */
  public final static String posStartText = "Position (start)"; // "Probe position (start)"
  
  /**
   * 
   */
  public final static String posEndText = "Position (end)"; // "Probe position (end)"
  
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
   * @see de.zbit.io.AbstractGeneBasedNSreader#isIdentifierRequired()
   */
  @Override
  protected boolean isIdentifierRequired() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#getAdditionalExpectedColumns()
   */
  @SuppressWarnings("serial")
  @Override
  protected List<ExpectedColumn> getAdditionalExpectedColumns() {
    boolean required = isRangeRequired();
    List<ExpectedColumn> list = new LinkedList<ExpectedColumn>();
    list.add(new ExpectedColumn(posStartText, required) {
      /* (non-Javadoc)
       * @see de.zbit.gui.csv.ExpectedColumn#getInitialSuggestion(de.zbit.io.csv.CSVReader)
       */
      @Override
      public int getInitialSuggestion(CSVReader r) {
        int s = super.getInitialSuggestion(r);
        if (s<0) {
          s = r.getColumnContaining("start");
        }
        return s;
      }
    });
    list.add(new ExpectedColumn(posEndText, false) {
      /* (non-Javadoc)
       * @see de.zbit.gui.csv.ExpectedColumn#getInitialSuggestion(de.zbit.io.csv.CSVReader)
       */
      @Override
      public int getInitialSuggestion(CSVReader r) {
        int s = super.getInitialSuggestion(r);
        if (s<0) {
          s = r.getColumnContaining("end");
        }
        return s;
      }
    }); // we can always build a range with just start
    list.add(new ExpectedColumn("Chromosome", required, Chromosome.chromosome_regex_with_forced_prefix) {
      /* (non-Javadoc)
       * @see de.zbit.gui.csv.ExpectedColumn#getInitialSuggestion(de.zbit.io.csv.CSVReader)
       */
      @Override
      public int getInitialSuggestion(CSVReader r) {
        int s = super.getInitialSuggestion(r);
        if (s<0) {
          s = r.getColumnContaining("chr");
        }
        return s;
      }
    });
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
      if (e.getName().equals(posStartText) && e.hasAssignedColumns()) {
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
    
    // Map to the genome (Eventually assign a gene ID or discard)
    // Be careful here, as toGeneMapper might be null if an existing gene identifier column is specified!
    if (geneID==null && toGeneMapper!=null) {
      if (chromosome!=null && start!=null) {
        geneID = toGeneMapper.map(AbstractRegion.createRegion(chromosome, start, end));
      }
    }
    
    // Create the object
    T obj = createObject(name, geneID, start, end, chromosome, line);
    
    // Check if we should discard it. We may NOT discard it earlier, since
    // overwriting methods may implement their own additional means to
    // map the object to a geneID!
    if (geneID==null && toGeneMapper!=null && toGeneMapper.isDiscardNonAssignableProbes() &&
        (obj instanceof GeneID && ((GeneID)obj).getID()==GeneID.default_geneID)) {
      obj = null;
    }
    
    return obj;
  }
  
  protected String getName(String[] line) {
    // Eventually there is no identifier assigned and we need another name.
    // WARNING: It is REQUIRED to specify either an identifier (nameCol)
    // OR chromosome and start position!
    if (nameCol<0) {
      if (chromosomeCol>=line.length || probeStartCol>=line.length) {
        return null;
      }
      // chromosomeCol <0 || probeStartCol <0 would be an invalid configuration and
      // should lead to an exception! This is intended.
      return String.format("CHR%sFS%s", ChromosomeTools.parseChromosomeFromString(line[chromosomeCol]), line[probeStartCol]);
    } else {
      return line[nameCol];
    }
  }
  

  /**
   * Initialize the Genomic mapper WITH A GUI.
   * @return true if the user confirmed the dialog.
   */
  protected boolean initializeGenomicMapper() {
    toGeneMapper = DNAmethylationDataMapper.createInstanceWithGUI();
    return toGeneMapper!=null;
  }

  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#done(java.util.Collection)
   */
  @Override
  protected void done(Collection<T> ret) {
    super.done(ret);
    
    // For convenience, show gene syambols to the user
    if (nameCol<0) {
      try {
        NSwithProbes.convertNamesToGeneSymbols(ret, species);
      } catch (Exception e) {
        // It was anyways just for convenience...
        e.printStackTrace();
      }
    }
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#init()
   */
  @Override
  protected void init() {
    super.init();
    
    // Read the genomic coordinates
    if (nameCol<0 && toGeneMapper!=null) {
      toGeneMapper.initialize(species);
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#additionalColumnAssignmentCheck(de.zbit.gui.csv.CSVImporterV2)
   */
  @Override
  protected boolean additionalColumnAssignmentCheck(CSVImporterV2 c) {
    List<ExpectedColumn> expCols = c.getExpectedColumns();
    
    /* If chr and start is configured, show and initialize a
     * genomic mapper.
     */
    
    if (isStartAndChromosomeAssigned(expCols)) {
      // We need to avoid warnings, trying to parse non-existing geneIDs
      setSupressWarnings(true);
      return initializeGenomicMapper();
    }
    else if (!expCols.get(0).hasAssignedColumns()) {
      //Note: identifier is expected to be the first column!
      String message = "You must assign at least one of the following columns:\n"+
      "- any gene \"Identifier\"\n"+
      "- the \"Chromosome\" and \"" + posStartText + "\" columns.";
      GUITools.showErrorMessage(null, message);
      return false;
    }
    return true;
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

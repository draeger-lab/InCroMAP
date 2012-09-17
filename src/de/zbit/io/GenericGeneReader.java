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

import java.util.List;

import de.zbit.data.genes.GenericGene;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.sequence.region.Region;
import de.zbit.util.Species;

/**
 * A reader for {@link GenericGene}.
 * This can read any region-based data types and perform a mapping onto
 * specific genes.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class GenericGeneReader extends AbstractGeneAndRegionBasedNSreader<GenericGene> {
  
  /**
   * Index to re-identify the "Strand" column.
   */
  private int indexOfStrandColumnInExColumnList = -1;
  
  /**
   * Column to read strand orientation
   */
  private int strandCol=-1;
  
  /**
   * Column to read gene descriptions
   */
  private int descriptionCol=-1;
  
  /**
   * Allows to read only data for a certain chromosome / region
   */
  private Region filterForRegion = null;
  
  /**
   * It is better to define separate generic readers, a default
   * one (primarily for internal use) that can handle all information,
   * one that performs a gene identifier based mapping to the genome
   * (e.g., by entrez id) and another one that performs a region-based
   * mapping (by chr:start-end).
   * @author Clemens Wrzodek
   * @version $Rev$
   */
  public enum readerType {
    DEFAULT, GENE, REGION
  };
  
  /**
   * Specify the type of this reader. See {@link readerType}.
   */
  private readerType type = readerType.DEFAULT;
  
  
  /**
   * This is ONLY for use in combination with {@link #importWithGUI(String)} afterwards.
   */
  public GenericGeneReader() {
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
   * @param strandCol
   */
  public GenericGeneReader(int identifierCol, IdentifierType idType, Species species,
    int startCol, int endCol, int chromosomeCol, int strandCol) {
    super(identifierCol, idType, species, startCol,endCol, chromosomeCol);
    this.strandCol = strandCol;
  }
  
  /**
   * Specify the type of this reader. See {@link readerType}.
   * @param type
   */
  public void setReaderType(readerType type) {
    this.type = type;
  }
  

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneAndRegionBasedNSreader#isRangeRequired()
   */
  @Override
  protected boolean isRangeRequired() {
    if (type==readerType.GENE) {
      return false;
    }
    return true;
  }
  
  /**
   * @param col
   */
  public void setDescriptionColumn(int col) {
    descriptionCol = col;
  }
  
  /**
   * Only read data for a certain chromosome / region
   * @param ChromsomeByteRepresentation
   */
  public void setFilterForRegion(Region region) {
    filterForRegion = region;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneAndRegionBasedNSreader#getAdditionalExpectedColumns()
   */
  @Override
  protected List<ExpectedColumn> getAdditionalExpectedColumns() {
    List<ExpectedColumn> l = super.getAdditionalExpectedColumns();
    indexOfStrandColumnInExColumnList = l.size();
    l.add(new ExpectedColumn("Strand", false));
    l.add(new ExpectedColumn("Description", false));
    return l;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneAndRegionBasedNSreader#processAdditionalExpectedColumns(java.util.List)
   */
  @Override
  protected void processAdditionalExpectedColumns(List<ExpectedColumn> additional) {
    super.processAdditionalExpectedColumns(additional);
    if (indexOfStrandColumnInExColumnList>=0) {
      ExpectedColumn strandExCol = additional.get(indexOfStrandColumnInExColumnList);
      if (strandExCol.hasAssignedColumns()) {
        strandCol = strandExCol.getAssignedColumn();
      }
      
      ExpectedColumn descCol = additional.get(indexOfStrandColumnInExColumnList+1);
      if (descCol.hasAssignedColumns()) {
        descriptionCol = descCol.getAssignedColumn();
      }
    }
    
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneAndRegionBasedNSreader#createObject(java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String[])
   */
  @Override
  protected GenericGene createObject(String name, Integer geneID,
    Integer start, Integer end, String chromosome, String[] line) {
    
    // Parse probe end
    Byte strand = null;
    if (strandCol>=0) {
      try {
        strand = Byte.parseByte(line[strandCol]);
      } catch (NumberFormatException e) {
        strand=null;
      }
    }
    
    GenericGene gg = new GenericGene(name, geneID, chromosome, start, end, strand);
    
    if (filterForRegion!=null && 
        !gg.intersects(filterForRegion)) {
      return null;
    }
    
    if (descriptionCol>=0 && descriptionCol<line.length) {
      gg.setDescription(line[descriptionCol]); 
    }
    
    return gg;
  }

  
}

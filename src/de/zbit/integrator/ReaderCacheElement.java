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
package de.zbit.integrator;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;

import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.io.CSVReader;
import de.zbit.parser.Species;

/**
 * On element, describing all properties required by a file.
 * This is used in {@link Reader}.
 * 
 * @see ReaderCache
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class ReaderCacheElement implements Serializable, Comparable<ReaderCacheElement> {
  private static final long serialVersionUID = 8354927528072966373L;
  
  /**
   * Used to re-identify the file.
   */
  private File describingFile;
  
  /**
   * Timestamp for this item
   */
  private long timestamp = System.currentTimeMillis();
  
  /**
   * Additional item to cache
   */
  private Species organism;
  
  /**
   * Final, configured {@link ExpectedColumn}s
   */
  private Collection<? extends ExpectedColumn> expectedColumns = null;
  
  /*
   * CSV Reader options, in order as they appear on the option panel.
   */
  private boolean containsHeader;
  private char separatorChar;
  private boolean treatMultiSeparatorsAsOne;
  private int skipLines;
  
  /**
   * @return a comparator that compares the elemts by their {@link #timestamp}
   */
  public static Comparator<ReaderCacheElement> getAgeComparator() {
    return new Comparator<ReaderCacheElement>() {
      @Override
      public int compare(ReaderCacheElement o1, ReaderCacheElement o2) {
        //return (int) (o1.timestamp-o2.timestamp);
        if(o1.timestamp > o2.timestamp)
          return 1;
        else if(o1.timestamp < o2.timestamp)
          return -1;
        else
          return 0;  
      }
    };
  }
  
  /**
   * @return the describingFile
   */
  public File getDescribingFile() {
    return describingFile;
  }

  /**
   * @param describingFile the describingFile to set
   */
  protected void setDescribingFile(File describingFile) {
    this.describingFile = describingFile;
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(ReaderCacheElement o) {
    return describingFile.compareTo(o.getDescribingFile());
  }
  
  /**
   * Resets the timestamp of this element.
   */
  public void resetTime() {
    timestamp = System.currentTimeMillis();
  }

  /**
   * Configures and resets the reader (separator char, contains headers, etc.)
   * but does NOT change the file that is read by the reader.
   * @param inputReader
   */
  public void configureReader(CSVReader inputReader) {
    if (inputReader==null) return;
    
    // Set all variables
    inputReader.setContainsHeaders(containsHeader);
    inputReader.setSeparatorChar(separatorChar);
    inputReader.setTreatMultipleConsecutiveSeparatorsAsOne(treatMultiSeparatorsAsOne);
    inputReader.setSkipLines(skipLines);
    
    // Reset
    try {
      inputReader.open();
    } catch (IOException e) {e.printStackTrace();}
  }

  /**
   * Configures expected columns as set in cache. This is done
   * by replacing pointers in the array. This is important, as 
   * it only works if you use the array pointer and NOT pointers
   * to single instances inside the array.
   * @param exCol template to configure and replace instances.
   */
  public void configureExpectedColumns(ExpectedColumn[] exCol) {
    if (exCol==null) return;
    for (int i=0; i<exCol.length; i++) {
      for (ExpectedColumn cache : this.expectedColumns) {
        if (cache.getOriginalName().equals(exCol[i].getOriginalName())) {
          //exCol[i] = cache;
          exCol[i].copyAssignedValuesFrom(cache);
          break;
        }
      }
    }
  }

  /**
   * @param spec
   */
  public void configureOrganismSelector(JLabeledComponent spec) {
    if (spec==null || organism==null) return;
    spec.setSelectedItem(organism);
  }

  /**
   * @param c
   * @return {@link ReaderCacheElement} instance, matching current
   * configuration in {@link CSVImporterV2} dialog.
   */
  public static ReaderCacheElement createInstance(CSVImporterV2 c) {
    if (c==null) return null;
    CSVReader r = c.getApprovedCSVReader();
    if (r==null) return null;
    
    // Set all variables
    ReaderCacheElement element = new ReaderCacheElement();
    element.setDescribingFile(new File(r.getFilename()));
    // Reader configuration
    element.containsHeader = r.getContainsHeaders();
    element.separatorChar = r.getSeparatorChar();
    element.treatMultiSeparatorsAsOne = r.getTreatMultipleConsecutiveSeparatorsAsOne();
    element.skipLines = r.getContentStartLine();
    // Expected columns
    element.expectedColumns = c.getExpectedColumns();
    return element;    
  }
  
  /**
   * @param c
   * @param species additional cache element
   * @return {@link ReaderCacheElement} instance, matching current
   * configuration in {@link CSVImporterV2} dialog.
   */
  public static ReaderCacheElement createInstance(CSVImporterV2 c, Species species) {
    ReaderCacheElement element = createInstance(c);
    if (c!=null) {
      // Additional stuff (species)
      element.organism = species;
    }
    return element;
  }
  

}

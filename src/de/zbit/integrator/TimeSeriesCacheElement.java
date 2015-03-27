/*
 * $Id: TimeSeriesCacheElement.java 132 2012-03-26 13:40:15Z wrzodek $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/Integrator/trunk/src/de/zbit/integrator/TimeSeriesCacheElement.java $
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2015 by the University of Tuebingen, Germany.
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

import javax.swing.JTable;

import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.io.mRNATimeSeriesReader;
import de.zbit.io.csv.CSVReader;
import de.zbit.util.Species;

/**
 * On element, describing all properties required by a file containing
 * time series data.
 * This is used in {@link mRNATimeSeriesReader}.
 * 
 * @see TimeSeriesCache
 * @author Felix Bartusch
 * @version $Rev: 132 $
 */
public class TimeSeriesCacheElement implements Serializable, Comparable<TimeSeriesCacheElement> {
  
  /**
   * Used to re-identify the file.
   */
  private File describingFile;
  
  /**
   * Timestamp for this item
   */
  private long timestamp = System.currentTimeMillis();
  
  /**
   * Time points for this item
   */
  double[] timePoints;
  
  /**
   * Time unit for this item
   */
  String timeUnit;
  
  /**
   * @return a comparator that compares the elemts by their {@link #timestamp}
   */
  public static Comparator<TimeSeriesCacheElement> getAgeComparator() {
    return new Comparator<TimeSeriesCacheElement>() {
      @Override
      public int compare(TimeSeriesCacheElement o1, TimeSeriesCacheElement o2) {
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

  /**
	 * @return the timePoints
	 */
	public double[] getTimePoints() {
		return timePoints;
	}

	/**
	 * @return the timeUnit
	 */
	public String getTimeUnit() {
		return timeUnit;
	}

	/* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(TimeSeriesCacheElement o) {
    return describingFile.compareTo(o.getDescribingFile());
  }
  
  /**
   * Resets the timestamp of this element.
   */
  public void resetTime() {
    timestamp = System.currentTimeMillis();
  }


  /**
   * Store the time series information in the cache.
   * @param timePoints Time points to save
   * @param timeUnit Time unit to save
   * @param c CSVImporter from which the file is taken
   * @return {@link TimeSeriesCacheElement} instance, matching current
   * configuration in {@link CSVImporterV2} dialog.
   */
  public static TimeSeriesCacheElement createInstance(double[] timePoints,
  		String timeUnit, CSVImporterV2 c) {
    if (timePoints==null || timeUnit==null) return null;
    CSVReader r = c.getApprovedCSVReader();
    if (r==null) return null;
    
    // Set all variables
    TimeSeriesCacheElement element = new TimeSeriesCacheElement();
    element.setDescribingFile(new File(r.getFilename()));
    // Time series configuration
    element.timeUnit = timeUnit;
    element.timePoints = timePoints;
    
    return element;    
  }
}

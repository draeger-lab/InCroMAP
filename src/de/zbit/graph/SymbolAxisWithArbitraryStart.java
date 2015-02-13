package de.zbit.graph;
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

import java.io.Serializable;

import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueAxisPlot;
import org.jfree.data.Range;

/**
 * A standard linear value axis that replaces integer values with symbols. It
 * extends {@link SymbolAxis} by the feature, that you can assign the starting
 * position (lower bound) for the first symbol (in {@link SymbolAxis} this is
 * set to fixed 0, then the next is 1, etc).
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class SymbolAxisWithArbitraryStart extends SymbolAxis implements Serializable {
  private static final long serialVersionUID = -8549571726541076093L;
  
  /**
   * The lower bound where our labels start
   */
  private double start;
  
  /**
   * Constructs a symbol axis, using default attribute values where necessary.
   * 
   * @param label the axis label (<code>null permitted).
   * @param sv the list of symbols to display instead of the numeric values.
   */
  public SymbolAxisWithArbitraryStart(String label, String[] sv) {
    this(label, sv, 0);
    
  }
  
  /**
   * 
   * @param label
   * @param sv
   * @param startingPosition the decimal number where the first label starts.
   */
  public SymbolAxisWithArbitraryStart(String label, String[] sv, double startingPosition) {
    super(label,sv);
    setStartingPosition(startingPosition);
  }
  
  /**
   * Rescales the axis to ensure that all data is visible.
   */
  protected void autoAdjustRange() {
    
    Plot plot = getPlot();
    if (plot == null) { return; // no plot, no data
    }
    
    if (plot instanceof ValueAxisPlot) {
      
      // ensure that all the symbols are displayed
      double upper = getSymbols().length - 1;
      double lower = getStartingPosition();
      double range = upper - lower;
      
      // ensure the autorange is at least <minRange> in size...
      double minRange = getAutoRangeMinimumSize();
      if (range < minRange) {
        upper = (upper + lower + minRange) / 2;
        lower = (upper + lower - minRange) / 2;
      }
      
      // this ensure that the grid bands will be displayed correctly.
      double upperMargin = 0.5;
      double lowerMargin = 0.5;
      
      if (getAutoRangeIncludesZero()) {
        if (getAutoRangeStickyZero()) {
          if (upper <= 0.0) {
            upper = 0.0;
          } else {
            upper = upper + upperMargin;
          }
          if (lower >= 0.0) {
            lower = 0.0;
          } else {
            lower = lower - lowerMargin;
          }
        } else {
          upper = Math.max(0.0, upper + upperMargin);
          lower = Math.min(0.0, lower - lowerMargin);
        }
      } else {
        if (getAutoRangeStickyZero()) {
          if (upper <= 0.0) {
            upper = Math.min(0.0, upper + upperMargin);
          } else {
            upper = upper + upperMargin * range;
          }
          if (lower >= 0.0) {
            lower = Math.max(0.0, lower - lowerMargin);
          } else {
            lower = lower - lowerMargin;
          }
        } else {
          upper = upper + upperMargin;
          lower = lower - lowerMargin;
        }
      }
      
      setRange(new Range(lower, upper), false, false);
      
    }
    
  }
  
  /**
   * @return the starting position
   */
  public double getStartingPosition() {
    return start;
  }
  
  /**
   * Set the position where the first symbol starts.
   * 
   * @param start
   */
  public void setStartingPosition(double start) {
    this.start = start;
  }
  
  /**
   * 
   */
  public boolean equals(Object obj) {
    if (obj != null && obj instanceof SymbolAxis) {
      boolean b = super.equals(obj);
      if (b) {
        if (obj instanceof SymbolAxisWithArbitraryStart) {
          return start == ((SymbolAxisWithArbitraryStart) obj)
              .getStartingPosition();
        } else {
          return start == 0;
        }
      }
    }
    return false;
  }
  
  /**
   * Converts a value to a string, using the list of symbols.
   * @param value value to convert.
   * @return The symbol.
   */
  public String valueToString(double value) {
    return super.valueToString((value - getStartingPosition()));
  }
}

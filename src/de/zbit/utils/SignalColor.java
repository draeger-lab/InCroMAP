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
package de.zbit.utils;

import java.awt.Color;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.gui.prefs.PathwayVisualizationOptions;
import de.zbit.math.rescale.AbstractRescale;
import de.zbit.math.rescale.LinearRescale;
import de.zbit.math.rescale.LogarithmicRescale;
import de.zbit.util.prefs.SBPreferences;

/**
 * This class can be used to generate a {@link Color} gradient, based on the values
 * of various {@link Signal}s.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class SignalColor {
  /**
   * SBPreferences object to store all preferences for this class.
   */
  protected SBPreferences prefs = SBPreferences.getPreferencesFor(PathwayVisualizationOptions.class);
  
  /**
   * Rescaler to rescale the red color component
   */
  AbstractRescale lred;
  /**
   * Rescaler to rescale the green color component
   */
  AbstractRescale lgreen;
  /**
   * Rescaler to rescale the blue color component
   */
  AbstractRescale lblue;
  
  /**
   * Initiate a color gradient scale, based on the {@link Signal} values from 
   * the given <code>nsList</code>. The colors and the value for the maximum
   * fold change are retrieved from the preferences.
   * @param <T> any {@link NameAndSignals} type
   * @param nsList list with {@link Signal}s
   * @param experimentName 
   * @param type
   */
  public <T extends NameAndSignals> SignalColor(Collection<T> nsList, String experimentName, SignalType type) {
    // Load default colors from config (e.g., blue-white-red).
    this(nsList, experimentName, type, (Float)null, (Color[]) null);
  }
  
  /**
   * Initiate a color rescaler, based only on settings, retrieved from preferences. 
   */
  public SignalColor() {
    this((Float)null, (Color)null);
  }
  /**
   * Initiate a generic color rescaler.
   * @param valueForMaximumColor all values >= this one will get the full color.
   * @param gradientColors colors for gradient.
   */
  public SignalColor(Float valueForMaximumColor, Color... gradientColors) {
    this(null, null, null, valueForMaximumColor, gradientColors);
  }
  
  /**
   * Initiate a color gradient scale, based on the {@link Signal} values from 
   * the given <code>nsList</code>. 
   * @param <T> any {@link NameAndSignals} type
   * @param nsList list with {@link Signal}s
   * @param experimentName
   * @param type
   * @param valueForMaximumColor signals, >= this value get the full color.
   * Set to null if you wish to load the value from preferences.
   * @param gradientColors Colors for the scale. Typically three values:
   * one for the negative fold change, one for the middle (nothing happend) and
   * one for the maximum fold change. Set to null to load from preferences.
   */
  public <T extends NameAndSignals> SignalColor(Collection<T> nsList, String experimentName, SignalType type, Float valueForMaximumColor, Color... gradientColors) {
    boolean isPvalue = (type.equals(SignalType.pValue) || type.equals(SignalType.qValue));
    
    // Load default from config (e.g., blue-white-red).
    if (gradientColors==null || gradientColors.length<1) {
      if (!isPvalue) {
        gradientColors = new Color[]{PathwayVisualizationOptions.COLOR_FOR_MINIMUM_FOLD_CHANGE.getValue(prefs),
            PathwayVisualizationOptions.COLOR_FOR_NO_FOLD_CHANGE.getValue(prefs),
            PathwayVisualizationOptions.COLOR_FOR_MAXIMUM_FOLD_CHANGE.getValue(prefs)};
      } else {
        gradientColors = new Color[]{PathwayVisualizationOptions.COLOR_FOR_MINIMUM_FOLD_CHANGE.getValue(prefs),
            PathwayVisualizationOptions.COLOR_FOR_NO_FOLD_CHANGE.getValue(prefs)};
      }
    }
    
    // Load value for full color
    Double maxFC = null;
    if (valueForMaximumColor==null || Float.isNaN(valueForMaximumColor)) {
      if (isPvalue) {
        maxFC = PathwayVisualizationOptions.P_VALUE_FOR_MAXIMUM_COLOR.getValue(prefs);
      } else {
        maxFC = PathwayVisualizationOptions.FOLD_CHANGE_FOR_MAXIMUM_COLOR.getValue(prefs).doubleValue();
      }
    } else {
      maxFC = valueForMaximumColor.doubleValue();
    }
    
    
    if (!isPvalue) {
      // Determine logarithmized (negative) fcs or not.
      double[] minMax;
      boolean dataIsLogarithmized = true;
      // DEACTIVATED. If one pathway only contains upregulated genes, the data is considered as
      // "not logarithmized". Thus, a static dataIsLogarithmized=true is more precise than this auto-detection.
//      if (nsList!=null) {
//        double[] realMinMax = NameAndSignals.getMinMaxSignalGlobal(nsList, experimentName, type);
//        if (realMinMax[0]<0) dataIsLogarithmized=true;
//        else dataIsLogarithmized=false;
//      }
      
      // Make a symmetric min and max (-3 to +3) instead of -2.9 to + 3.2 because of better coloring then.
      minMax = new double[]{-maxFC.doubleValue(), maxFC.doubleValue()};
      
      // Initiate color rescalers
      if (dataIsLogarithmized) {
        // input fold changes are logarithmized
        lred = new LinearRescale(minMax[0], minMax[1], getColorChannel(0, gradientColors));
        lgreen = new LinearRescale(minMax[0], minMax[1], getColorChannel(1, gradientColors));
        lblue = new LinearRescale(minMax[0], minMax[1], getColorChannel(2, gradientColors));
      } else {
        // input fold changes are not logarithmized
        minMax[0] = Math.pow(2, minMax[0]);
        minMax[1] = Math.pow(2, minMax[1]);
        lred = new LogarithmicRescale(minMax[0], minMax[1], 2, getColorChannel(0, gradientColors));
        lgreen = new LogarithmicRescale(minMax[0], minMax[1], 2, getColorChannel(1, gradientColors));
        lblue = new LogarithmicRescale(minMax[0], minMax[1], 2, getColorChannel(2, gradientColors));
      }
      
    } else {
      // Ensure a valid number for p-values 
      if (isPvalue && (maxFC<0 || maxFC>1)) {maxFC = 0.0005;}
      
      // Initiate color rescalers for P-values 
      lred = new LogarithmicRescale(maxFC, 1, 10, getColorChannel(0, gradientColors));
      lgreen = new LogarithmicRescale(maxFC, 1, 10, getColorChannel(1, gradientColors));
      lblue = new LogarithmicRescale(maxFC, 1, 10, getColorChannel(2, gradientColors));
    }
    
    

  }
  
  
  /**
   * 
   * @param d
   * @return a {@link Color} that corresponds to the Signal <code>d</code>,
   * based on the list that initiali
   */
  public Color getColor(double d) {
    return new Color(rescaleColorPart(lred, d),rescaleColorPart(lgreen, d),rescaleColorPart(lblue, d));
  }
  

  /**
   * Returns a value between 0 and 255.
   * @param lcolor configured rescaler for the color
   * @param oldValue old experimental value (e.g., fold change)
   * @return color code of the corresponding color part (r,g or b) of the new color.
   */
  public static int rescaleColorPart(AbstractRescale lcolor, double oldValue) {
    return Math.max(0, Math.min(255, lcolor.rescale(oldValue).intValue()));
  }
  
  /**
   * @param i 0=red, 1=green, 2=blue
   * @param gradientColors list of colors
   * @return a list if the corresponding color-intensity in the given list of colors.
   */
  private List<Integer> getColorChannel(int i, Color... gradientColors) {
    List<Integer> red = new LinkedList<Integer>();
    for (Color c: gradientColors) {
      int f = 0;
      if (i==0) f = c.getRed();
      if (i==1) f = c.getGreen();
      if (i==2) f = c.getBlue();
      red.add(f);
    }
    
    return red;
  }
}

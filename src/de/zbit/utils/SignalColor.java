/**
 * @author Clemens Wrzodek
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
import de.zbit.math.LinearRescale;
import de.zbit.util.prefs.SBPreferences;

/**
 * This class can be used to generate a {@link Color} gradient, based on the values
 * of various {@link Signal}s.
 * @author Clemens Wrzodek
 */
public class SignalColor {
  /**
   * SBPreferences object to store all preferences for this class.
   */
  protected SBPreferences prefs = SBPreferences.getPreferencesFor(PathwayVisualizationOptions.class);
  
  /**
   * Rescaler to rescale the red color component
   */
  LinearRescale lred;
  /**
   * Rescaler to rescale the green color component
   */
  LinearRescale lgreen;
  /**
   * Rescaler to rescale the blue color component
   */
  LinearRescale lblue;
  
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
    // Load default from config (e.g., blue-white-red).
    if (gradientColors==null || gradientColors.length<1) {
      gradientColors = new Color[]{PathwayVisualizationOptions.COLOR_FOR_MINIMUM_FOLD_CHANGE.getValue(prefs),
          PathwayVisualizationOptions.COLOR_FOR_NO_FOLD_CHANGE.getValue(prefs),
          PathwayVisualizationOptions.COLOR_FOR_MAXIMUM_FOLD_CHANGE.getValue(prefs)};
    }
    
    // Load value for full color
    Float maxFC = valueForMaximumColor;
    if (maxFC==null || Float.isNaN(maxFC)) {
      maxFC = PathwayVisualizationOptions.FOLD_CHANGE_FOR_MAXIMUM_COLOR.getValue(prefs);
    }
    
    
    // Get min max signals and determine logarithmized (negative) fcs or not.
    double[] minMax;
    if (nsList!=null) {
      minMax = NameAndSignals.getMinMaxSignalGlobal(nsList, experimentName, type);
    } else {
      minMax = new double[]{-maxFC, maxFC};
    }
    
    // Make a symmetric min and max (-3 to +3) instead of -2.9 to + 3.2 because of better coloring then.
    double minFCthreshold = minMax[0]<0?(maxFC*-1):1/maxFC.doubleValue();
    if (maxFC<minFCthreshold) {
      double temp = minFCthreshold;
      minFCthreshold = maxFC;
      maxFC = (float) temp;
    }
    minMax = new double[]{minFCthreshold,maxFC.doubleValue()};
    
    // Infere value for "nothing happend", (i.e. no fold change observed)
    Double middleValue=Double.NaN; //NaN means "auto infere"
    if (minFCthreshold<0) {
      middleValue = 0d; // log FCs
    } else if (minFCthreshold<1) {
      // >0 & <1 => non-logarithmmized FCs
      middleValue = 1d;
    }
    
    // Initiate color rescalers
    lred = new LinearRescale(minMax[0], minMax[1], getColorChannel(0, gradientColors), middleValue);
    lgreen = new LinearRescale(minMax[0], minMax[1], getColorChannel(1, gradientColors), middleValue);
    lblue = new LinearRescale(minMax[0], minMax[1], getColorChannel(2, gradientColors), middleValue);
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
  public static int rescaleColorPart(LinearRescale lcolor, double oldValue) {
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

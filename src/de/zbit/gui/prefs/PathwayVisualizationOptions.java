/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui.prefs;

import java.awt.Color;

import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * Any options for interactions with KEGGtranslator or pathway maps.
 * @author Clemens Wrzodek
 */
public interface PathwayVisualizationOptions extends KeyProvider {
  
  /**
   * Symmetric fold change (e.g., 2.0 and -2.0) that is being assigned the full/maximal
   * color. Higher fold changes have the same color, towards zero, the color is
   * getting lighter.
   */
  public static Option<Float> FC_FOR_MAX_COLOR = new Option<Float>("FC_FOR_MAX_COLOR", Float.class,
      "Min/Max (symmetric) fold change that is being assigned the full color.",
      new Range<Float>(Float.class, "{[1.0,100.0]}"),2.0f);
  
  public static Option<Color> COLOR_FOR_MIN_FC = new Option<Color>("COLOR_FOR_MIN_FC", Color.class,
      "Color for minimum fold change.", Color.BLUE);
  
  public static Option<Color> COLOR_FOR_NO_FC = new Option<Color>("COLOR_FOR_NO_FC", Color.class,
      "Color for no fold change.", Color.WHITE);
  
  public static Option<Color> COLOR_FOR_MAX_FC = new Option<Color>("COLOR_FOR_MAX_FC", Color.class,
      "Color for maximum fold change.", Color.RED);
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static final OptionGroup PATHWAY_COLORING_OPTIONS = new OptionGroup(
      "Pathway coloring options",
      "Define various options that control the fold-change dependent coloring of pathway nodes.",
      FC_FOR_MAX_COLOR, COLOR_FOR_MIN_FC, COLOR_FOR_NO_FC, COLOR_FOR_MAX_FC);
  
  
  
}

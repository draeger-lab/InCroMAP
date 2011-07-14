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
  public static Option<Float> FOLD_CHANGE_FOR_MAXIMUM_COLOR = new Option<Float>("FOLD_CHANGE_FOR_MAXIMUM_COLOR", Float.class,
      "Min/Max (symmetric) fold change that is being assigned the full color.",
      new Range<Float>(Float.class, "{[1.0,1000.0]}"),1.5f);
  
  public static Option<Color> COLOR_FOR_MINIMUM_FOLD_CHANGE = new Option<Color>("COLOR_FOR_MINIMUM_FOLD_CHANGE", Color.class,
      "Color for minimum fold change.", new Color(0,153,255)); // Color.BLUE.brighter() is too dark to read black captions
  
  public static Option<Color> COLOR_FOR_NO_FOLD_CHANGE = new Option<Color>("COLOR_FOR_NO_FOLD_CHANGE", Color.class,
      "Color for no fold change (log(fc)=0 or rawFC=1).", Color.WHITE);
  
  public static Option<Color> COLOR_FOR_MAXIMUM_FOLD_CHANGE = new Option<Color>("COLOR_FOR_MAXIMUM_FOLD_CHANGE", Color.class,
      "Color for maximum fold change.", Color.RED);

  public static Option<Color> COLOR_FOR_NO_VALUE = new Option<Color>("COLOR_FOR_NO_VALUE", Color.class,
      "Color for nodes, with no fold change in the input dataset (unaffected nodes).", Color.LIGHT_GRAY);
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static final OptionGroup PATHWAY_COLORING_OPTIONS = new OptionGroup(
      "Pathway coloring options",
      "Define various options that control the fold-change dependent coloring of pathway nodes.",
      FOLD_CHANGE_FOR_MAXIMUM_COLOR, COLOR_FOR_MINIMUM_FOLD_CHANGE, COLOR_FOR_NO_FOLD_CHANGE, COLOR_FOR_MAXIMUM_FOLD_CHANGE, COLOR_FOR_NO_VALUE);
  
  
  
}

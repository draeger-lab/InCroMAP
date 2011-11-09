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
package de.zbit.gui.prefs;

import java.awt.Color;

import de.zbit.gui.IntegratorUI;
import de.zbit.gui.customcomponents.NodeShapeSelector;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * Any options for interactions with KEGGtranslator or pathway maps.
 * These options should control how to visualize various data in pathways.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public interface PathwayVisualizationOptions extends KeyProvider {
  
  /**
   * Symmetric fold change (e.g., 2.0 and -2.0) that is being assigned the full/maximal
   * color. Higher fold changes have the same color, towards zero, the color is
   * getting lighter.
   */
  public static Option<Float> FOLD_CHANGE_FOR_MAXIMUM_COLOR = new Option<Float>("FOLD_CHANGE_FOR_MAXIMUM_COLOR", Float.class,
      "Min/Max (symmetric) fold change that is being assigned the full color.",
      new Range<Float>(Float.class, "{[0.0,1000.0]}"),1.5f);

  public static Option<Float> DONT_VISUALIZED_FOLD_CHANGES = new Option<Float>("DONT_VISUALIZED_FOLD_CHANGES", Float.class,
      "Fold changes between 0 and this fold change are not visualized at all.",
      new Range<Float>(Float.class, "{[0.0,1000.0]}"),0.5f, "Don't visualize fold changes between ±");

  
  
  public static Option<Color> COLOR_FOR_MINIMUM_FOLD_CHANGE = new Option<Color>("COLOR_FOR_MINIMUM_FOLD_CHANGE", Color.class,
      "Color for minimum fold change.", IntegratorUI.LIGHT_BLUE); // Color.BLUE.brighter() is too dark to read black captions
  
  public static Option<Color> COLOR_FOR_NO_FOLD_CHANGE = new Option<Color>("COLOR_FOR_NO_FOLD_CHANGE", Color.class,
      "Color for no fold change (log(fc)=0 or rawFC=1).", Color.WHITE);
  
  public static Option<Color> COLOR_FOR_MAXIMUM_FOLD_CHANGE = new Option<Color>("COLOR_FOR_MAXIMUM_FOLD_CHANGE", Color.class,
      "Color for maximum fold change.", Color.RED);

  public static Option<Color> COLOR_FOR_NO_VALUE = new Option<Color>("COLOR_FOR_NO_VALUE", Color.class,
      "Color for nodes, with no fold change in the input dataset (unaffected nodes).", Color.LIGHT_GRAY);
  
  
  public static Option<Byte> CHANGE_NODE_SHAPE = new Option<Byte>("CHANGE_NODE_SHAPE", Byte.class,
      "Change the shape of colored nodes to the selected shape.",Option.buildRange(NodeShapeSelector.validChoices),
      new Byte((byte)0), false);
  
  public static Option<Byte> PROTEIN_MODIFICATION_BOX_HEIGHT = new Option<Byte>("PROTEIN_MODIFICATION_BOX_HEIGHT", Byte.class,
      "Define a height (in pixel) of boxes that are added below nodes for various protein modifications.",
      new Range<Byte>(Byte.class, "{[5,100]}"), new Byte((byte) 8));
  
  public static Option<Byte> DNA_METHYLATION_MAXIMUM_BOX_WIDTH = new Option<Byte>("DNA_METHYLATION_MAXIMUM_BOX_WIDTH", Byte.class,
      "Define a maximum width (in pixel) of boxes that are added left of nodes to represent DNA methylation changes.",
      new Range<Byte>(Byte.class, "{[5,100]}"), new Byte((byte) 20), "DNA methylation maximum box width"); //should be dividable by 2
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static final OptionGroup PATHWAY_COLORING_OPTIONS = new OptionGroup(
    "Pathway coloring options",
    "Define various options that control the fold-change dependent coloring of pathway nodes.",
    FOLD_CHANGE_FOR_MAXIMUM_COLOR, DONT_VISUALIZED_FOLD_CHANGES,
    COLOR_FOR_MINIMUM_FOLD_CHANGE, COLOR_FOR_NO_FOLD_CHANGE, COLOR_FOR_MAXIMUM_FOLD_CHANGE, COLOR_FOR_NO_VALUE,
    CHANGE_NODE_SHAPE);
  
}

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

import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import de.zbit.gui.IntegratorUI;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.customcomponents.NodeShapeSelector;

/**
 * Enable an option tab for the {@link PathwayVisualizationOptions}.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class PathwayVisualizationOptionPanel extends PreferencesPanelForKeyProviders {
  private static final long serialVersionUID = 4574493372860651243L;

  /**
   * @param provider
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public PathwayVisualizationOptionPanel()
    throws IOException {
    super(IntegratorUI.appName + " options", SignalOptions.class, EnrichmentOptions.class, PathwayVisualizationOptions.class);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.prefs.PreferencesPanelForKeyProvider#init()
   */
  @Override
  public void init() {
    super.init();
    if (getComponentCount()>0) {
      changeFCSpinnerStepsize(this);
      changeNodeShapeSelector();
      SignalOptionPanel.performCommonSignalOptionPanelModifications(this);
    }
  }

  /**
   * 
   */
  private void changeNodeShapeSelector() {
    Object c = option2component.get(PathwayVisualizationOptions.CHANGE_NODE_SHAPE);
    if (c!=null && c instanceof JLabeledComponent) {
      Object c2 = ((JLabeledComponent)c).getColumnChooser();
      if (c2 instanceof JComboBox) {
        ((JComboBox)c2).setRenderer(new NodeShapeSelector());
      } else {
        // Huh? should be a ComboBox!
        System.err.println("Could not change renderer of node shape selector!");
      }
    }
  }

  /**
   * Tries to change the step size of the fold change spinner to 0.1
   */
  public static void changeFCSpinnerStepsize(PreferencesPanel panel) {
    Object c = panel.getComponentForOption(PathwayVisualizationOptions.FOLD_CHANGE_FOR_MAXIMUM_COLOR);
    if (c!=null && c instanceof JLabeledComponent) {
      Object c2 = ((JLabeledComponent)c).getColumnChooser();
      if (c2 instanceof JSpinner) {
        SpinnerModel model = ((JSpinner)c2).getModel();
        if (model instanceof SpinnerNumberModel) {
          ((SpinnerNumberModel)model).setStepSize(.1f);
        }
      }
    }
    
    c = panel.getComponentForOption(PathwayVisualizationOptions.DONT_VISUALIZE_FOLD_CHANGES);
    if (c!=null && c instanceof JLabeledComponent) {
      Object c2 = ((JLabeledComponent)c).getColumnChooser();
      if (c2 instanceof JSpinner) {
        SpinnerModel model = ((JSpinner)c2).getModel();
        if (model instanceof SpinnerNumberModel) {
          ((SpinnerNumberModel)model).setStepSize(.1f);
        }
      }
    }
  }
  
}

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
package de.zbit.gui.dialogs;

import java.io.IOException;
import java.util.prefs.BackingStoreException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.layout.LayoutHelper;
import de.zbit.gui.panels.ExpandablePanel;
import de.zbit.gui.prefs.PathwayVisualizationOptionPanel;
import de.zbit.gui.prefs.PathwayVisualizationOptions;
import de.zbit.gui.prefs.PreferencesPanel;
import de.zbit.gui.prefs.PreferencesPanelForKeyProvider;
import de.zbit.gui.prefs.SignalOptionPanel;


/**
 * A {@link JPanel} that has all required input options to 
 * Visualize Data in a Pathway. It asks the user for <ul>
 * <li>MergeDepth:
 * <ol><li>one value per node (pathway centered)</li>
 * <li>one value per gene (gene centered - default)</li>
 * <li>one node per value (probe centered - not recommended)</li></ol></li>
 * <li>MergeType if 1 or 2 are selected</li>
 * 
 * <li> Colors und threshold</li>
 * 
 * <li> Change Shape? ONLY IF YES, Shape</li>
 * </ul>
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class VisualizeDataInPathwayDialog extends JPanel {
  private static final long serialVersionUID = 7310214090839244643L;

  /**
   * Signal and merge depth options
   */
  private SignalOptionPanel mergeDepthAndType;
  
  /**
   * Pathway visualization options (color, threshold and node shape)
   */
  private PreferencesPanel colorThresholdAndShape;
  
  public VisualizeDataInPathwayDialog() {
    super ();
    try {
      init(new LayoutHelper(this));
    } catch (IOException e) {
      GUITools.showErrorMessage(getParent(), e);
    }
  }

  
  /**
   * Initializes the components on this panel.
   * @param lh
   * @throws IOException
   */
  public void init(LayoutHelper lh) throws IOException {
    
    // Put the SignalOptionPanel on here
    mergeDepthAndType = new SignalOptionPanel();
    lh.add(mergeDepthAndType);
    
    // Put the PathwayVisualizationOptionPanel on an collapsed, expandable panel.
    //colorThresholdAndShape = new PathwayVisualizationOptionPanel(); // <= also contains SignalOptionPanel().
    colorThresholdAndShape = new PreferencesPanelForKeyProvider(PathwayVisualizationOptions.class);
    
    PathwayVisualizationOptionPanel.changeFCSpinnerStepsize(colorThresholdAndShape);
    lh.add(new ExpandablePanel("Advanced visualization options", colorThresholdAndShape, true, true));
    
  }
  
  /**
   * Shows the {@link VisualizeDataInPathwayDialog} as Dialog. After pressing
   * ok, all options are made persistent.
   * @param c
   * @param title title for the dialog.
   * @return true, if the user confirmed the dialog with "ok".
   */
  public static boolean showDialog(final VisualizeDataInPathwayDialog c, String title) {
    if (title==null) title = IntegratorUI.appName;
    // Show asking dialog
    boolean confirmed = GUITools.showAsDialog(IntegratorUI.getInstance(), c, title, true)==JOptionPane.OK_OPTION;
    if (confirmed) {
      // Make options persistent.
      try {
        c.persist();
      } catch (Exception e) {
        GUITools.showErrorMessage(IntegratorUI.getInstance(), e);
      }
    }
    return confirmed;
  }

  /**
   * Persistently stores the currently set options, i.e., key-value pairs in the
   * user's configuration. Depending on the operating system, the way how this
   * information is actually stored can vary.
   * 
   * @throws BackingStoreException
   */
  public void persist() throws BackingStoreException {
    mergeDepthAndType.persist();
    colorThresholdAndShape.persist();
  }

  /**
   * Just for DEMO and testing purposes.
   */
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      JFrame parent = new JFrame();
      parent.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      VisualizeDataInPathwayDialog dialog = new VisualizeDataInPathwayDialog();
      showDialog(dialog, null);

      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
    
}

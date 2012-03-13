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
package de.zbit.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JToolBar;
import javax.swing.UIManager;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;

import de.zbit.data.Signal.SignalType;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.gui.tabs.IntegratorChartTab;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.util.StringUtil;
import de.zbit.util.objectwrapper.ValueTriplet;
import de.zbit.visualization.VisualizeGenesInChartTab;

/**
 * Actions for the {@link JToolBar} in {@link IntegratorChartTab}.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class ChartTabActions implements ActionListener {
  public static final transient Logger log = Logger.getLogger(ChartTabActions.class.getName());
  
  /**
   * The actual tab to perform actions on.
   */
  IntegratorChartTab parent;
  

  public ChartTabActions(IntegratorChartTab parent) {
    super();
    this.parent = parent;
  }
  
  
  /**
   * All actions for {@link IntegratorChartTab} are defined
   * here.
   * @author Clemens Wrzodek
   */
  public static enum ChartAction implements ActionCommand {
    RESET_VIEW,
    ZOOM_IN,
    ZOOM_OUT,
    ADD_GENES,
    REMOVE_GENES,
    COLOR_GENES;
    
    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getName()
     */
    public String getName() {
      switch (this) {
      default:
        return StringUtil.firstLetterUpperCase(toString().toLowerCase().replace('_', ' '));
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getToolTip()
     */
    public String getToolTip() {
      switch (this) {
        case RESET_VIEW:
          return "Reset the visible are of the chart to the default area.";
        case ADD_GENES:
          return "Add an additional view that shows coding regions to the graph.";
        case REMOVE_GENES:
          return "Remove additional view showing coding regions.";
        case COLOR_GENES:
          return "Color coding genes according to experimental values.";
          
        default:
          return null; // Deactivate
      }
    }
  }
  

  /**
   * Create the buttons on the toolbar.
   * @param bar
   */
  public void createJToolBarItems(JToolBar bar) {
    String uniqueName = parent.getClass().getSimpleName() + parent.hashCode();
    if (bar.getName().equals(uniqueName)) return;
    bar.removeAll();
    bar.setName(uniqueName);
    // If we have too much space, we could add "Save" ....
    
    bar.add(GUITools.createJButton(this,
      ChartAction.RESET_VIEW, UIManager.getIcon("ICON_REFRESH_16")));
    bar.add(GUITools.createJButton(this,
      ChartAction.ZOOM_IN, UIManager.getIcon("ICON_PLUS_16")));
    bar.add(GUITools.createJButton(this,
      ChartAction.ZOOM_OUT, UIManager.getIcon("ICON_MINUS_16")));
    
//    bar.add(GUITools.createJButton(this,
//      ChartAction.ADD_GENES, UIManager.getIcon("ICON_PENCIL_16")));
//    bar.add(GUITools.createJButton(this,
//      ChartAction.REMOVE_GENES, UIManager.getIcon("ICON_TRASH_16")));

    bar.add(GUITools.createJButton(this,
      ChartAction.COLOR_GENES, UIManager.getIcon("ICON_PENCIL_16")));
    
    
    GUITools.setOpaqueForAllElements(bar, false);
  }
  
  
  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();
    
    // Check if we have a plot
    if (!parent.isReady()) return;
    XYPlot plot = parent.getData().getXYPlot();
    ChartPanel chart = parent.getChartPanel();
    
    if (command.equals(ChartAction.RESET_VIEW.toString())) {
      Range r = plot.getDomainAxis().getDefaultAutoRange();
      if (r!=null) plot.getDomainAxis().setRangeWithMargins(r);

      chart.restoreAutoRangeBounds();
      
    } else if (command.equals(ChartAction.ZOOM_IN.toString())) {
      plot.getDomainAxis().resizeRange(0.5);
      //plot.getRangeAxis().resizeRange(0.5); is NULL in combined plot
      
    } else if (command.equals(ChartAction.ZOOM_OUT.toString())) {
      // TODO: We could (by default) sort the parent nsList by Range
      // and use the Range#getIntersections() method to update our
      // current series. This way, we would also add the additional
      // probes that become visible by zooming out.
      plot.getDomainAxis().resizeRange(2.0);
      //plot.getRangeAxis().resizeRange(2.0); is NULL in combined plot
      
    } else if (command.equals(ChartAction.ADD_GENES.toString())) {
    } else if (command.equals(ChartAction.REMOVE_GENES.toString())) {
    } else if (command.equals(ChartAction.COLOR_GENES.toString())) {
      ValueTriplet<NameAndSignalsTab, String, SignalType>  vt = IntegratorUITools.showSelectExperimentBox(null,
            "Please select an observation to visualize in this pathway.", parent.getSpecies());
       if (vt!=null) {
         VisualizeGenesInChartTab.visualizeData(parent,vt.getA(),vt.getB(), vt.getC());
       }
      //
    }
  }


  /**
   * @param toolbar
   */
  public void updateToolbarButtons(JToolBar toolbar) {
    boolean enableColorGenes = false;
    if (parent.containsGenesView()) {
      enableColorGenes = true;
    }
    GUITools.setEnabled(enableColorGenes, toolbar, ChartAction.COLOR_GENES);
  }
  
  
  
}

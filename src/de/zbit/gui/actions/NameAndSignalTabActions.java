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
package de.zbit.gui.actions;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.jfree.chart.JFreeChart;

import de.zbit.analysis.PairData;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.HeterogeneousNS;
import de.zbit.data.NSwithProbes;
import de.zbit.data.NameAndSignals;
import de.zbit.data.PairedNS;
import de.zbit.data.Signal;
import de.zbit.data.TableResult;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtargets;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUI.Action;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.JDropDownButton;
import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.gui.actions.listeners.EnrichmentActionListener;
import de.zbit.gui.actions.listeners.KEGGPathwayActionListener;
import de.zbit.gui.dialogs.IntegratedEnrichmentDialog;
import de.zbit.gui.dialogs.IntegrationDialog;
import de.zbit.gui.dialogs.MergedSignalDialog;
import de.zbit.gui.tabs.IntegratorChartTab;
import de.zbit.gui.tabs.IntegratorTab;
import de.zbit.gui.tabs.IntegratorTabWithTable;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.math.BenjaminiHochberg;
import de.zbit.math.Bonferroni;
import de.zbit.math.BonferroniHolm;
import de.zbit.sequence.region.Region;
import de.zbit.util.Species;
import de.zbit.util.StringUtil;
import de.zbit.util.objectwrapper.ValuePair;
import de.zbit.utils.FilterNSTable;

/**
 * Actions for the {@link JToolBar} in {@link IntegratorTabWithTable}
 * and {@link NameAndSignalsTab}s.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class NameAndSignalTabActions implements ActionListener {
  public static final transient Logger log = Logger.getLogger(NameAndSignalTabActions.class.getName());
  
  /**
   * The actual tab to perform actions on.
   */
  NameAndSignalsTab parent;
  
  /*
   * MenuItems for statistical corrections. They are toggled, thus
   * must be remembered to disable the others when one is selected.
   */
  JMenuItem BH_cor;
  JMenuItem BFH_cor;
  JMenuItem BO_cor;
  // Another button to toggle
  AbstractButton filter=null;
  
  public NameAndSignalTabActions(NameAndSignalsTab parent) {
    super();
    this.parent = parent;
  }
  
  
  /**
   * All actions for {@link IntegratorTabWithTable} are defined
   * here.
   * @author Clemens Wrzodek
   */
  public static enum NSAction implements ActionCommand {
    /**
     * {@link Action} similar to  {@link #NEW_PATHWAY} but
     * immediately colors nodes according fold changes.
     */
    VISUALIZE_IN_PATHWAY,
    /**
     * Same as {@link #VISUALIZE_IN_PATHWAY} but restricts
     * the data to the current table selection.
     */
    VISUALIZE_SELETED_DATA_IN_PATHWAY,
    ADD_OBSERVATION,
    SEARCH_TABLE,
    PAIR_DATA,
    ADD_GENE_SYMBOLS,
    /**
     * Plot genome region for all {@link Region} implementing
     * {@link NameAndSignals}. Please do not offer this action
     * to users if your ns does not contain any {@link Signal}s!
     */
    PLOT_REGION,
    ANNOTATE_TARGETS,
    REMOVE_TARGETS,
    FDR_CORRECTION_BH,
    FDR_CORRECTION_BFH,
    FDR_CORRECTION_BO,
    /**
     * This should be used with causion as not all further processing
     * methods mind a filetered table and not all tables are capable
     * to sync this with the underlying data structure!
     */
    FILTER_TABLE;
    
    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getName()
     */
    public String getName() {
      switch (this) {
      /**
       * Icon is enough in search table.
       */
      case SEARCH_TABLE:
        return "Search";
      case ADD_GENE_SYMBOLS:
        return "Show gene symbols";
      case PLOT_REGION:
        return "Plot genome region";
        
        
      case FDR_CORRECTION_BH:
        return "Benjamini Hochberg";
      case FDR_CORRECTION_BFH:
        return "Bonferroni Holm";
      case FDR_CORRECTION_BO:
        return "Bonferroni";
        
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
        case VISUALIZE_IN_PATHWAY:
          return "Show a KEGG pathway and visualize the selected data (e.g., color nodes accoring to fold changes).";
        case VISUALIZE_SELETED_DATA_IN_PATHWAY:
          return "Show a KEGG pathway and visualize only the selected items in this pathway.";
        case ADD_GENE_SYMBOLS:
          return "Show gene symbols as names, using a NCBI gene id to gene symbol converter.";
        case ADD_OBSERVATION:
          return "Calculate a new observation, based on two other ones.";
          
        case PAIR_DATA: // ToolTip is changes for miRNA in code later
          return "Integrate two different datasets by pairing matching genes.";
          
        case FDR_CORRECTION_BH:
          return "Correct p-values with the Benjamini and Hochberg method and save as q-values.";
        case FDR_CORRECTION_BFH:
          return "Correct p-values with the Holm–Bonferroni method and save as q-values.";
        case FDR_CORRECTION_BO:
          return "Correct p-values with the Bonferroni method and save as q-values.";
          
        default:
          return null; // Deactivate
      }
    }
  }
  
  
  
  
  public void createJToolBarItems(JToolBar bar) {
    // Not here just name, because button actions are linked to this instance!
    Class<?> tableContent = parent.getDataContentType();
    String uniqueName = parent.getClass().getSimpleName() + parent.hashCode() + tableContent.hashCode();
    if (bar.getName().equals(uniqueName)) return;
    bar.removeAll();
    bar.setName(uniqueName);
    
    
    
    // Search
    JButton search = GUITools.createJButton(this,
        NSAction.SEARCH_TABLE, UIManager.getIcon("ICON_SEARCH_16"));
    search.setPreferredSize(new Dimension(16,16));
    bar.add(search);
    
    EnrichmentActionListener al = new EnrichmentActionListener(parent, true);
    JPopupMenu enrichment = IntegratorUITools.createEnrichmentPopup(al);
    JDropDownButton enrichmentButton = new JDropDownButton("Enrichment", 
        UIManager.getIcon("ICON_GEAR_16"), enrichment);
    bar.add(enrichmentButton);
    
    // Filter table
    if (tableContent.equals(HeterogeneousNS.class)) {
      filter = GUITools.createJButton(this,
        NSAction.FILTER_TABLE, UIManager.getIcon("ICON_GEAR_16"), 
        null, JToggleButton.class);
    
      bar.add(filter);
    }
    
    // Visualize in Pathway
    ActionCommand pathwayCommand;
    if (tableContent.equals(EnrichmentObject.class)) {
      if (((EnrichmentObject<?>)parent.getExampleData()).isKEGGenrichment()) {
        pathwayCommand = KEGGPathwayActionListener.VISUALIZE_PATHWAY;
      } else {
        pathwayCommand=null;
      }
    } else if (tableContent.equals(HeterogeneousNS.class)) {
      pathwayCommand=null;
    } else {
      pathwayCommand = NSAction.VISUALIZE_IN_PATHWAY;
    }
    
    if (pathwayCommand!=null) {
      KEGGPathwayActionListener al2 = new KEGGPathwayActionListener(parent);
      JButton showPathway = GUITools.createJButton(al2,
        pathwayCommand, UIManager.getIcon("ICON_PATHWAY_16"));
      bar.add(showPathway);
    }
    
    // Integrate (pair) data button
    if (!tableContent.equals(EnrichmentObject.class) && 
        !tableContent.equals(HeterogeneousNS.class)) {      
      JPopupMenu integrate = new JPopupMenu("Integrate");
      JMenuItem pairData = GUITools.createJMenuItem(this,
          NSAction.PAIR_DATA, UIManager.getIcon("IntegratorIcon_16_straight"));
      if (miRNA.class.isAssignableFrom(tableContent)) {
        String toolTip = "Integrate miRNA data with other (e.g. mRNA data) by mapping the miRNA targets to the other dataset.";
        StringUtil.toHTML(toolTip, StringUtil.TOOLTIP_LINE_LENGTH);
        pairData.setToolTipText(toolTip);
      }
      integrate.add(pairData);

      integrate.add(GUITools.createJMenuItem(this,
        IntegratorUI.Action.INTEGRATED_ENRICHMENT, UIManager.getIcon("IntegratorIcon_16_straight")));
      integrate.add(GUITools.createJMenuItem(this,
          IntegratorUI.Action.INTEGRATED_TABLE, UIManager.getIcon("IntegratorIcon_16_straight")));
      integrate.add(GUITools.createJMenuItem(this,
          IntegratorUI.Action.INTEGRATED_HETEROGENEOUS_DATA_VISUALIZATION, UIManager.getIcon("ICON_PATHWAY_16")));

      JDropDownButton integrateButton = new JDropDownButton("Integrate", 
          UIManager.getIcon("IntegratorIcon_16_straight"), integrate);
      integrateButton.setToolTipText("Perform heterogeneous data integration.");
      
      bar.add(integrateButton);
    }
    
    // Datatype specific buttons:
    if (tableContent.equals(mRNA.class) || tableContent.equals(DNAmethylation.class)) {
      // Removed for miRNA, is done every time after annotating targets!
      //|| tableContent.equals(miRNAandTarget.class) || tableContent.equals(miRNA.class)) {
      bar.add(GUITools.createJButton(this,
          NSAction.ADD_GENE_SYMBOLS, UIManager.getIcon("ICON_PENCIL_16")));
    
    } if (Region.class.isAssignableFrom(tableContent)) {
      bar.add(GUITools.createJButton(this,
        NSAction.PLOT_REGION, UIManager.getIcon("ICON_PENCIL_16")));
      
    } if (tableContent.equals(miRNA.class)) {
      // Annotate and Remove targets
      JPopupMenu targets = IntegratorUITools.createMiRNAtargetPopup(this, null);
      JDropDownButton targetsButton = new JDropDownButton(targets.getLabel(), UIManager.getIcon("ICON_PENCIL_16"), targets);
      bar.add(targetsButton);
    
    } if (tableContent.equals(EnrichmentObject.class)) {
      
      JPopupMenu fdr = new JPopupMenu("FDR correction");
      BH_cor = GUITools.createJMenuItem(this,
          NSAction.FDR_CORRECTION_BH, UIManager.getIcon("ICON_MATH_16"),null,null,JCheckBoxMenuItem.class);
      fdr.add(BH_cor);
      BFH_cor = GUITools.createJMenuItem(this,
          NSAction.FDR_CORRECTION_BFH, UIManager.getIcon("ICON_MATH_16"),null,null,JCheckBoxMenuItem.class);
      fdr.add(BFH_cor);
      BO_cor = GUITools.createJMenuItem(this,
          NSAction.FDR_CORRECTION_BO, UIManager.getIcon("ICON_MATH_16"),null,null,JCheckBoxMenuItem.class);
      fdr.add(BO_cor);
      JDropDownButton fdrButton = new JDropDownButton("FDR correction", 
          UIManager.getIcon("ICON_MATH_16"), fdr);
      fdrButton.setToolTipText("Change the false-discovery-rate correction method.");
      
      bar.add(fdrButton);
      BH_cor.setSelected(true);
      
    } if (tableContent.equals(PairedNS.class)) {
      bar.add(GUITools.createJButton(this, NSAction.ADD_OBSERVATION, UIManager.getIcon("ICON_MATH_16")));
    }      
    
    
    /* XXX:
     * Consider adding features:
     * [- Add pathways] column with pathways for gene/ target (or GO terms, etc.)
     * 
     */
    
    GUITools.setOpaqueForAllElements(bar, false);    
  }




  @SuppressWarnings({ "unchecked" })
  @Override
  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();
    
    if (command.equals(NSAction.SEARCH_TABLE.toString())) {
      // Fire F3 Key for search
      if (parent.getVisualization()!=null) {
        KeyEvent F3 = new KeyEvent(parent.getVisualization(), 
            0, 0, 0, 114, (char)114);
        for (KeyListener l: parent.getVisualization().getKeyListeners()) {
          l.keyPressed(F3);
        }
      }
    
    } else if (command.equals(NSAction.FILTER_TABLE.toString())) {
      FilterNSTable.filterToggleTable(parent, filter);
      
    } else if (command.equals(NSAction.PAIR_DATA.toString())) {
      PairData pd = new PairData(parent);
      @SuppressWarnings("rawtypes")
      List pairedData = pd.showDialogAndPairData();
      if (pairedData!=null && pairedData.size()>0) {
        // Add tab
        NameAndSignalsTab nsTab = new NameAndSignalsTab(parent.getIntegratorUI(), pairedData, parent.getSpecies(false));
        parent.getIntegratorUI().addTab(nsTab, "PairedData",
          String.format("Integration of '%s' and '%s'.", parent.getTabName(), pd.getLastSelectedOtherTab().getTabName()));
        
        // If one of the input table changes (during to changes in NameAndSignals), the
        // derived tables must be changed, too!
        parent.addTableChangeListener(nsTab);
        pd.getLastSelectedOtherTab().addTableChangeListener(nsTab);
      }
      
    } else if (command.equals(NSAction.ADD_OBSERVATION.toString())) {
      MergedSignalDialog options = MergedSignalDialog.showDialog(parent, (PairedNS<?, ?>) parent.getExampleData());
      if (options==null) return;
      PairData.calculateMergedSignal((Iterable<PairedNS<?, ?>>) parent.getData(), options);
      parent.rebuildTable();
      parent.repaint();
      
    } else if (command.equals(IntegratorUI.Action.INTEGRATED_TABLE.toString())) {
      IntegrationDialog.defaultSelection = parent;
      IntegratorUITools.showIntegratedTreeTableDialog();

    } else if (command.equals(IntegratorUI.Action.INTEGRATED_ENRICHMENT.toString())) {
      IntegratedEnrichmentDialog.defaultSelection = parent;
      IntegratorUITools.showIntegratedEnrichmentDialog();
      
    } else if (command.equals(IntegratorUI.Action.INTEGRATED_HETEROGENEOUS_DATA_VISUALIZATION.toString())) {
      IntegrationDialog.defaultSelection = parent;
      IntegratorUITools.showIntegratedVisualizationDialog();
      
    } else if (command.equals(NSAction.ADD_GENE_SYMBOLS.toString())) {
      log.info("Converting GeneIDs to Gene symbols.");
      showGeneSymbols();

    } else if (command.equals(NSAction.PLOT_REGION.toString())) {
      JFreeChart chart=null;
      // By right-click popup => take directly clicked on gene
      if (e.getSource() instanceof JMenuItem) {
        List<?> sel = parent.getSelectedItems();
        if (sel.size()==1) {
          chart = IntegratorChartTab.createChart(parent.getData(), (NameAndSignals)sel.get(0), parent.getSpecies());
        }
      }
      // Get source data
      IntegratorTab<?> parent = this.parent;
      if (parent.getDataContentType().equals(EnrichmentObject.class)) {
        while (parent.getSourceTab()!=null) {
          parent = parent.getSourceTab();
        }
      }
      // Get species
      Species spec = parent.getSpecies();
      if (spec==null) {
        spec = IntegratorUITools.showOrganismSelectorDialog(null);
      }
      // Let use choose what to plot
      if (chart==null) {
        chart = IntegratorChartTab.createAndShowDialog(parent, spec);
      }
      if (chart!=null) {
        // If not canceled, show plot in a new tab
        String name = chart.getTitle().getText();
        if (name.indexOf("\n")>0) name = name.substring(0, name.indexOf("\n"));
        IntegratorChartTab newTab = new IntegratorChartTab(IntegratorUI.getInstance(), chart, spec);
        newTab.setSourceTab(parent);
        IntegratorUI.getInstance().addTab(newTab, name, null, UIManager.getIcon("ICON_PENCIL_16"));
      }
      
    } else if (command.equals(NSAction.ANNOTATE_TARGETS.toString())) {
      annotateMiRNAtargets();
      
    } else if (command.equals(NSAction.REMOVE_TARGETS.toString())) {
      removeMiRNAtargets();

    } else if (command.equals(NSAction.FDR_CORRECTION_BH.toString())) {
      BFH_cor.setSelected(false); BO_cor.setSelected(false); BH_cor.setSelected(true);
      new BenjaminiHochberg().setQvalue((List<EnrichmentObject<Object>>) parent.getData());
      parent.getVisualization().repaint();
      
    } else if (command.equals(NSAction.FDR_CORRECTION_BFH.toString())) {
      BFH_cor.setSelected(true); BO_cor.setSelected(false); BH_cor.setSelected(false);
      new BonferroniHolm().setQvalue((List<EnrichmentObject<Object>>) parent.getData());
      parent.getVisualization().repaint();
      
    } else if (command.equals(NSAction.FDR_CORRECTION_BO.toString())) {
      BFH_cor.setSelected(false); BO_cor.setSelected(true); BH_cor.setSelected(false);
      new Bonferroni().setQvalue((List<EnrichmentObject<Object>>) parent.getData());
      parent.getVisualization().repaint();
    }
  }




  /**
   * Converts geneIDs to gene symbols. For {@link mRNA}s, the name will be changed
   * from whatever it is now to a gene symbol. For {@link miRNA}s, the target
   * geneIDs will be displayed as gene symbols.
   */
  @SuppressWarnings("unchecked")
  public void showGeneSymbols() {
    IntegratorTab<?> parent = this.parent;
    if (parent.getDataContentType().equals(EnrichmentObject.class)) {
      while (parent.getSourceTab()!=null) {
        parent = parent.getSourceTab();
      }
    }
    /*List<? extends mRNA> list=null;
    if (parent.getDataContentType().equals(mRNA.class)) {
      list = (List<? extends mRNA>) parent.getData();
    } else if (pa)
    }*/
    try {
      NSwithProbes.convertNamesToGeneSymbols((Iterable<? extends NameAndSignals>) parent.getData(), parent.getSpecies());
    } catch (Exception e1) {
      GUITools.showErrorMessage(parent, e1);
    }
    
    if (parent instanceof IntegratorTabWithTable) {
      ((IntegratorTabWithTable)parent).rebuildTable();
    } else {
      parent.getVisualization().repaint();
    }
  }


  /**
   * Shows a dialog and performs annotation of {@link miRNA}s with {@link miRNAtargets}.
   * Will result in an exception if the underlying tab is no collection of miRNAs. 
   */
  @SuppressWarnings("unchecked")
  public void annotateMiRNAtargets() {
    ValuePair<miRNAtargets, Species> t_all = IntegratorUITools.loadMicroRNAtargets(parent.getSpecies(false));
    if (t_all==null || t_all.getA()==null) return;
    int annot = miRNA.link_miRNA_and_targets(t_all.getA(), (Collection<miRNA>) parent.getData());
    log.info(String.format("Annotated %s/%s microRNAs with targets.", annot, ((Collection<miRNA>)parent.getData()).size()));
    
    // Convert IN BACKGROUND (new thread) ids to symbols
    showGeneSymbols_InNewThread();
    
    parent.rebuildTable();
    parent.repaint();
  }




  /**
   * Calls the {@link #showGeneSymbols()} method in a new thread and repaints
   * the parent if this is done.
   */
  public void showGeneSymbols_InNewThread() {
    // Convert geneIDs to gene symbols.
    SwingWorker<Void, Void> convertToGeneSymbolsInBackground = new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() throws Exception {
        showGeneSymbols();
        return null;
      }
      @Override
      protected void done() {
        parent.repaint();
      }
    };
    convertToGeneSymbolsInBackground.execute();
  }
  
  /**
   * Removes all {@link miRNAtargets} from {@link miRNA} contained in the data in {@link #parent}.
   */
  public void removeMiRNAtargets() {
    Iterable<? extends TableResult> data = parent.getData();
    for (TableResult tr: data) {
      if (tr instanceof miRNA) {
        ((miRNA)tr).removeTargets();
      }
    }
    
    parent.rebuildTable();
    parent.repaint();
  }
  
  
  public synchronized void updateToolbarButtons(JToolBar toolBar) {
    boolean state = parent.getData()!=null;
    for (Component c: toolBar.getComponents()) {
      c.setEnabled(state);
    }
    // Example usage for single state change:
    //GUITools.setEnabled(state, toolBar, TPAction.HIGHLIGHT_ENRICHED_GENES);
    boolean enableRegionPlot = false;
    if (parent.getExampleData()!=null && parent.getExampleData() instanceof Region
        && parent.getExampleData() instanceof NameAndSignals && 
        ((NameAndSignals)parent.getExampleData()).hasSignals()) {
      enableRegionPlot = true;
    }
    GUITools.setEnabled(enableRegionPlot, toolBar, NSAction.PLOT_REGION);
    
    // Toggle the table filter button
    if (filter!=null && parent.getVisualization()!=null && 
        JTable.class.isAssignableFrom(parent.getVisualization().getClass())) {
      filter.setSelected(FilterNSTable.isTableFiltered((JTable)parent.getVisualization()));
    }
  }

  
  

}

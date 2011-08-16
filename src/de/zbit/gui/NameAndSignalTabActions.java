package de.zbit.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.NSwithProbes;
import de.zbit.data.NameAndSignals;
import de.zbit.data.TableResult;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtargets;
import de.zbit.gui.IntegratorUI.Action;
import de.zbit.math.BenjaminiHochberg;
import de.zbit.math.Bonferroni;
import de.zbit.math.BonferroniHolm;
import de.zbit.parser.Species;
import de.zbit.util.StringUtil;
import de.zbit.util.ValuePair;

/**
 * Actions for the {@link JToolBar} in {@link IntegratorTabWithTable}
 * and {@link NameAndSignalsTab}s.
 * @author Clemens Wrzodek
 */
public class NameAndSignalTabActions implements ActionListener {
  public static final transient Logger log = Logger.getLogger(NameAndSignalTabActions.class.getName());
  
  /**
   * The actual tab to perform actions on.
   */
  IntegratorTabWithTable parent;
  
  /*
   * MenuItems for statistical corrections. They are toggled, thus
   * must be remembered to disable the others when one is selected.
   */
  JMenuItem BH_cor;
  JMenuItem BFH_cor;
  JMenuItem BO_cor;
  
  public NameAndSignalTabActions(IntegratorTabWithTable parent) {
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
    SEARCH_TABLE,
    INTEGRATE,
    ADD_GENE_SYMBOLS,
    ANNOTATE_TARGETS,
    REMOVE_TARGETS,
    FDR_CORRECTION_BH,
    FDR_CORRECTION_BFH,
    FDR_CORRECTION_BO;
    
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
          return "Show a KEGG pathway and color nodes accoring to fold changes.";
        case ADD_GENE_SYMBOLS:
          return "Show gene symbols as names, using a NCBI gene id to gene symbol converter.";
          
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
    JPopupMenu enrichment = IntegratorGUITools.createEnrichmentPopup(al);
    JDropDownButton enrichmentButton = new JDropDownButton("Enrichment", 
        UIManager.getIcon("ICON_GEAR_16"), enrichment);
    bar.add(enrichmentButton);
    
    // Visualize in Pathway
    ActionCommand pathwayCommand;
    if (tableContent.equals(EnrichmentObject.class)) {
      pathwayCommand = KEGGPathwayActionListener.VISUALIZE_PATHWAY;
    } else {
      pathwayCommand = NSAction.VISUALIZE_IN_PATHWAY;
    }
    
    KEGGPathwayActionListener al2 = new KEGGPathwayActionListener(parent);
    JButton showPathway = GUITools.createJButton(al2,
        pathwayCommand, UIManager.getIcon("ICON_GEAR_16"));
    bar.add(showPathway);
    
    // Integrate (pair) data button
    if (!tableContent.equals(EnrichmentObject.class)) {
      JButton integrate = GUITools.createJButton(this,
          NSAction.INTEGRATE, UIManager.getIcon("ICON_GEAR_16"));
      bar.add(integrate);
    }
    
    // Datatype specific buttons:
    if (tableContent.equals(mRNA.class)) {
      // Removed fir miRNA, is done every time after annotating targets!
      //|| tableContent.equals(miRNAandTarget.class) || tableContent.equals(miRNA.class)) {
      bar.add(GUITools.createJButton(this,
          NSAction.ADD_GENE_SYMBOLS, UIManager.getIcon("ICON_GEAR_16")));
      
    } if (tableContent.equals(miRNA.class)) {
      // Annotate and Remove targets
      JPopupMenu targets = IntegratorGUITools.createMiRNAtargetPopup(this, null);
      JDropDownButton targetsButton = new JDropDownButton(targets.getLabel(), UIManager.getIcon("ICON_GEAR_16"), targets);
      bar.add(targetsButton);
    
    } if (tableContent.equals(EnrichmentObject.class)) {
      
      JPopupMenu fdr = new JPopupMenu("FDR correction");
      BH_cor = GUITools.createJMenuItem(this,
          NSAction.FDR_CORRECTION_BH, UIManager.getIcon("ICON_GEAR_16"),null,null,JCheckBoxMenuItem.class);
      fdr.add(BH_cor);
      BFH_cor = GUITools.createJMenuItem(this,
          NSAction.FDR_CORRECTION_BFH, UIManager.getIcon("ICON_GEAR_16"),null,null,JCheckBoxMenuItem.class);
      fdr.add(BFH_cor);
      BO_cor = GUITools.createJMenuItem(this,
          NSAction.FDR_CORRECTION_BO, UIManager.getIcon("ICON_GEAR_16"),null,null,JCheckBoxMenuItem.class);
      fdr.add(BO_cor);
      JDropDownButton fdrButton = new JDropDownButton("FDR correction", 
          UIManager.getIcon("ICON_GEAR_16"), fdr);
      fdrButton.setToolTipText("Change the false-discovery-rate correction method.");
      
      bar.add(fdrButton);
      BH_cor.setSelected(true);
    }
    
    /* TODO:
     * if (mRNA)
     * - Pair with miRNA
     * 
     * if (miRNA)
     * - Pair with mRNA
     * 
     * if (EnrichmentObject)
     * - Cutoff für > pValue, qValue, list ratio
     * 
     * Eventuell
     * [- Add pathways] column with pathways for gene/ target
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
      
      
    } else if (command.equals(NSAction.INTEGRATE.toString())) {
      // TODO: ...
      GUITools.showMessage("Not yet implemented.", "");
      
    } else if (command.equals(NSAction.ADD_GENE_SYMBOLS.toString())) {
      showGeneSymbols();
      
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
      NSwithProbes.convertNamesToGeneSymbols((List<? extends NameAndSignals>) parent.getData(), parent.getSpecies());
    } catch (Exception e1) {
      GUITools.showErrorMessage(parent, e1);
    }
    parent.getVisualization().repaint();
  }


  /**
   * Shows a dialog and performs annotation of {@link miRNA}s with {@link miRNAtargets}.
   * Will result in an exception if the underlying tab is no collection of miRNAs. 
   */
  @SuppressWarnings("unchecked")
  public void annotateMiRNAtargets() {
    ValuePair<miRNAtargets, Species> t_all = IntegratorGUITools.loadMicroRNAtargets(parent.getSpecies(false));
    if (t_all==null || t_all.getA()==null) return;
    int annot = miRNA.link_miRNA_and_targets(t_all.getA(), (Collection<miRNA>) parent.getData());
    log.info(String.format("Annotated %s/%s microRNAs with targets.", annot, parent.getData().size()));
    
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
    
    
    parent.rebuildTable();
    parent.repaint();
  }
  
  /**
   * Removes all {@link miRNAtargets} from {@link miRNA} contained in the data in {@link #parent}.
   */
  public void removeMiRNAtargets() {
    List<? extends TableResult> data = parent.getData();
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
  }

  
  

}

/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui.actions.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JTable;
import javax.swing.SwingWorker;

import de.zbit.analysis.enrichment.AbstractEnrichment;
import de.zbit.analysis.enrichment.GOEnrichment;
import de.zbit.analysis.enrichment.KEGGPathwayEnrichment;
import de.zbit.analysis.enrichment.MSigDB_GSEA_Enrichment;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.miRNA.miRNA;
import de.zbit.gui.ActionCommand;
import de.zbit.gui.GUITools;
import de.zbit.gui.customcomponents.ProgressWorker;
import de.zbit.gui.table.JTableFilter;
import de.zbit.gui.tabs.IntegratorTab;
import de.zbit.gui.tabs.IntegratorTabWithTable;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.util.StringUtil;

/**
 * Can handle enrichment actions for {@link IntegratorTab}s.
 * 
 * @author Clemens Wrzodek
 */
public class EnrichmentActionListener implements ActionListener {
  public static final transient Logger log = Logger.getLogger(EnrichmentActionListener.class.getName());
  
  /**
   * The source tab
   */
  IntegratorTabWithTable source;
  
  /**
   * Determins wether the {@link JTable} selection should
   * be taken (false) or a dialog should prompt the user
   * for genes (true).
   */
  boolean withDialog = false;
  
  /**
   * All available Enrichments are listed here.
   * @author Clemens Wrzodek
   */
  public static enum Enrichments implements ActionCommand {
    KEGG_ENRICHMENT,
    GO_ENRICHMENT,
    /**
     * Enrichments from the MSigDB (Molecular Signatures Database)
     * http://www.broadinstitute.org/gsea/
     */
    MSIGDB_ENRICHMENT;
    
    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getName()
     */
    public String getName() {
      switch (this) {
      case KEGG_ENRICHMENT:
        return "KEGG Pathway Enrichment";
      case GO_ENRICHMENT:
        return "GO Enrichment";
      case MSIGDB_ENRICHMENT:
        return "MSigDB Enrichment";
        
      default: // "Enrichment";
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
        case KEGG_ENRICHMENT:
          return "KEGG Pathway Enrichment";
        case GO_ENRICHMENT:
          return "Gene Ontology Enrichment";
        case MSIGDB_ENRICHMENT:
          return "Perform an enrichment, based on a gene set from "+
          "<a href=http://www.broadinstitute.org/gsea/>http://www.broadinstitute.org/gsea/</a>";
          
        default:
          return null; // Deactivate
      }
    }
  }
  
  public EnrichmentActionListener(IntegratorTabWithTable source) {
    this.source = source;
  }
  
  public EnrichmentActionListener(IntegratorTabWithTable source, boolean withDialog) {
    this(source);
    this.withDialog = withDialog;
    
  }
  
  
  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public synchronized  void actionPerformed(final ActionEvent e) {
    // Get selected items
    final List<?> geneList = getGeneList();
    if (geneList==null || geneList.size()<1) {
      GUITools.showErrorMessage(source, "No elements selected for enrichment analysis.");
      return;
    } else if (!checkGeneList(geneList)) return;
    final String loadingString = "Performing enrichment analysis...";
    
    // Log this action.
    log.fine(loadingString + e.getActionCommand() + " on " + Arrays.deepToString(geneList.toArray()));
    
    // Create a worker to execute everything in a new thread.
    SwingWorker<Collection<? extends NameAndSignals>, Void> worker = new ProgressWorker<Collection<? extends NameAndSignals>, Void>() {
      @Override
      protected Collection<? extends NameAndSignals> doInBackground() throws Exception {
        // Get Enrichment class
        final AbstractEnrichment<String> enrich;
        try {
          log.info("Downloading and reading enrichment file.");
          if (e.getActionCommand().equals(Enrichments.GO_ENRICHMENT.toString())) {
            enrich = new GOEnrichment(source.getSpecies(), progress);
          } else if (e.getActionCommand().equals(Enrichments.KEGG_ENRICHMENT.toString())) {
            enrich = new KEGGPathwayEnrichment(source.getSpecies(), progress);
          } else if (e.getActionCommand().equals(Enrichments.MSIGDB_ENRICHMENT.toString())) {
            enrich = new MSigDB_GSEA_Enrichment(source.getSpecies(), progress);
          } else {
            GUITools.showErrorMessage(source, String.format("Unknown enrichment command: %s", e.getActionCommand()));
            return null;
          }
        } catch (IOException e1) {
          GUITools.showErrorMessage(source, e1, "Could not read enrichment mapping.");
          return null;
        }
        
        // Perform analysis
        log.info(loadingString);
        List<EnrichmentObject<String>> l=null;
        /*if (geneList.get(0) instanceof mRNA) {
          l = enrich.getEnrichments((List<mRNA>)geneList);
        } else if (geneList.get(0) instanceof Integer) {
          // Assume geneIDs
          log.log(Level.INFO, "Received an Integer List for enrichment analysis. Assuming they are GeneIDs!");
          l = enrich.getEnrichments((List<Integer>)geneList, IdentifierType.NCBI_GeneID);
        } else if (geneList.get(0) instanceof EnrichmentObject) {
          l = enrich.getEnrichments(EnrichmentObject.mergeGeneLists((Iterable<EnrichmentObject>) geneList));
        } else {*/
          //GUITools.showErrorMessage(source, String.format("Enrichment for %s is not yet implemented.", geneList.get(0).getClass()));
        try {
          l = enrich.getEnrichments(geneList, null);
        } catch (Throwable e) {
          e.printStackTrace();
          GUITools.showErrorMessage(null, e);
        }
        //}
        
        // Inform user about results
        if (l!=null && l.size()<1) {
          GUITools.showMessage("Could not find any enriched objects.", enrich.getName());
          return null; // Will close the tab on null-result.
        } /*else {
          System.out.println(l.toString().replace("]], [", "]]\n["));
        }*/
        
        return l;
      }
    };
    
    // Create tab
    String eName = Enrichments.valueOf(e.getActionCommand()).getName();
    NameAndSignalsTab tab = new NameAndSignalsTab(source.getIntegratorUI(), worker, loadingString, source.getSpecies());
    String tip = eName + " for " + geneList.size() + " objects";
    if (source.getName()!=null) tip+= " from \"" + source.getName() + "\".";
    source.getIntegratorUI().addTab(tab, eName, tip);
    tab.setSourceTab(this.source);
  }
  
  
  /**
   * Checks the genelist e.g. if it contains miRNAs and
   * if these miRNAs contain targets.
   * <p>Errors are displayed by this method itself.
   * @param geneList
   * @return
   */
  private boolean checkGeneList(List<?> geneList) {
    boolean issueNoTargetMessage = true;
    for (Object o: geneList) {
      if (o instanceof miRNA) {
        if (((miRNA)o).hasTargets()) {
          issueNoTargetMessage = false;
          break;
        }
      } else {
        issueNoTargetMessage = false;
        break;
      }
    }
    
    if (issueNoTargetMessage) {
      GUITools.showMessage("None of the selected miRNAs has annotated targets. Please use the " +
      		"\"annotate targets\" button to annotate your miRNAs with targets.", "Can not continnue");
      return false;
    }
    
    return true;
  }

  /**
   * @return list of selected genes for the enrichment.
   */
  private List<?> getGeneList() {
    List<?> geneList = source.getSelectedItems();
    
    // Eventually ask user
    if (withDialog) {
      boolean showFilterDialog=true;
      if (geneList!=null && geneList.size()>1) {
        int ret = GUITools.showQuestionMessage(source, "Do you want to take the selected genes for the enrichment analysis?", "Enrichment analysis", "Yes", "No");
        if (ret==0) {
          showFilterDialog=false;
        }
      }
      if (showFilterDialog) {
        JTableFilter filt = JTableFilter.showDialog(source, (JTable) source.getVisualization(),
            "Apply filter to table to select genes for enrichment");
        if (filt==null) return null;
        geneList = source.getSelectedItems(filt.getSelectedRows());
      }

    }
    return geneList;
  }
  
}

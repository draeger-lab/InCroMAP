/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import de.zbit.analysis.enrichment.AbstractEnrichment;
import de.zbit.analysis.enrichment.GOEnrichment;
import de.zbit.analysis.enrichment.KEGGPathwayEnrichment;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.mRNA.mRNA;
import de.zbit.mapper.MappingUtils.IdentifierType;

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
  IntegratorTab<?> source;
  
  public final static String GO_ENRICHMENT = "GO";
  public final static String KEGG_ENRICHMENT = "KEGG";
  
  public EnrichmentActionListener(IntegratorTab<?> source) {
    this.source = source;
  }
  
  
  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public synchronized  void actionPerformed(final ActionEvent e) {
    // Get selected items
    final List<?> geneList = source.getSelectedItems();
    if (geneList==null || geneList.size()<1) {
      GUITools.showErrorMessage(source, "No elements selected for enrichment analysis.");
      return;
    }
    final String loadingString = "Performing enrichment analysis...";
    
    System.out.println(Arrays.deepToString(geneList.toArray()));

    
    
    // Create a worker to execute everything in a new thread.
    SwingWorker<Collection<? extends NameAndSignals>, Void> worker = new ProgressWorker<Collection<? extends NameAndSignals>, Void>() {
      @SuppressWarnings("unchecked")
      @Override
      protected Collection<? extends NameAndSignals> doInBackground() throws Exception {
        
        // Get Enrichment class
        final AbstractEnrichment<String> enrich;
        try {
          log.info("Downloading and reading enrichment file.");
          if (e.getActionCommand().equals(GO_ENRICHMENT)) {
            enrich = new GOEnrichment(source.getSpecies(), progress);
          } else if (e.getActionCommand().equals(KEGG_ENRICHMENT)) {
            enrich = new KEGGPathwayEnrichment(source.getSpecies(), progress);
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
        if (geneList.get(0) instanceof mRNA) {
          l = enrich.getEnrichments((List<mRNA>)geneList);
        } else if (geneList.get(0) instanceof Integer) {
          // Assume geneIDs
          log.log(Level.INFO, "Received an Integer List for enrichment analysis. Assuming they are GeneIDs!");
          l = enrich.getEnrichments((List<Integer>)geneList, IdentifierType.NCBI_GeneID);
        } else {
          GUITools.showErrorMessage(source, String.format("Enrichment for %s is not yet implemented.", geneList.get(0).getClass()));
        }
        return l;
      }
    };
    
    // Create tab
    NameAndSignalsTab tab = new NameAndSignalsTab(source.getIntegratorUI(), worker, loadingString, source.getSpecies());
    String tip = getEnrichmentName(e.getActionCommand()) + " for " + geneList.size() + " objects";
    if (source.getName()!=null) tip+= " from \"" + source.getName() + "\".";
    source.getIntegratorUI().addTab(tab, getEnrichmentName(e.getActionCommand()), tip);
  }
  
  
  /**
   * @param actionCommand
   * @return
   */
  protected String getEnrichmentName(String actionCommand) {
    if (actionCommand.equals(GO_ENRICHMENT)) {
      return "GO Enrichment";
    } else if (actionCommand.equals(KEGG_ENRICHMENT)) {
      return "KEGG Pathway Enrichment";
    } else {
      return "Enrichment";
    }
  }

}

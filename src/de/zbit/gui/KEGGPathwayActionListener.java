/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JTable;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.kegg.TranslatorTools;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.kegg.gui.TranslatorUI;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.ValueTriplet;

/**
 * An {@link ActionListener} that can visualize KEGG Pathways
 * using KEGGtranslator.
 * 
 * @author Clemens Wrzodek
 */
public class KEGGPathwayActionListener implements ActionListener {
  public static final transient Logger log = Logger.getLogger(KEGGPathwayActionListener.class.getName());
  
  /**
   * The source tab
   */
  IntegratorTab<?> source;
  
  public final static String VISUALIZE_PATHWAY = "VP";
  
  public KEGGPathwayActionListener(IntegratorTab<?> source) {
    this.source = source;
  }
  
  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public void actionPerformed(ActionEvent e) {
    
    if (e.getActionCommand().equals(VISUALIZE_PATHWAY)) {
      visualizePathway();
      
    } else if (e.getActionCommand().equals(TranslatorUI.Action.TRANSLATION_DONE.toString())) {
      TranslatorPanel source = (TranslatorPanel) e.getSource();
      
      // Process translation result
      if (e.getID() != JOptionPane.OK_OPTION) {
        // If translation failed, remove the tab. The error
        // message has already been issued by the translator.
        this.source.getIntegratorUI().closeTab(source);
      } else {
        int r = GUITools.showQuestionMessage(source, "Do you want to color the nodes accoring to an observation?", 
          IntegratorUI.appName, new String[]{"Yes", "No"});
        if (r==0) {
          // Let the user choose a signal to color the nodes
          Signal2PathwayTools tools2 = new Signal2PathwayTools(source);
          IntegratorTab st = this.source;
          while (st.getSourceTab()!=null) {
            st = st.getSourceTab();
          }
          ValueTriplet<NameAndSignalsTab, String, SignalType>  vt =
            IntegratorGUITools.showSelectExperimentBox(this.source.getIntegratorUI(), st);
          
          if (vt!=null) {
            // Write signals to nodes
            st = vt.getA();
            tools2.writeSignalsToNodes((Iterable) st.getData());
            
            // Color nodes
            tools2.colorNodesAccordingToSignals((Iterable) st.getData(), MergeType.Mean, 
              vt.getB(), vt.getC());
          } else {
            r = -1; // At least hightligh source gene nodes
          }
        } if (r!=0) {
          // Highlight source genes
          Collection<Integer> geneIDs = ((EnrichmentObject<?>)source.getData()).getGeneIDsFromGenesInClass();
          TranslatorTools tools = new TranslatorTools(source);
          tools.highlightGenes(geneIDs);
        }
      }
    }
  }
  
  
  /**
   * Download the pathways, selected in the source {@link JTable} and
   * add a {@link TranslatorPanel} for each one.
   */
  private void visualizePathway() {
    // Get selected items
    final List<?> geneList = source.getSelectedItems();
    if (geneList==null || geneList.size()<1) {
      GUITools.showErrorMessage(source, "No elements selected for pathway visualization.");
      return;
    }
    final String loadingString = "Downloading and visualizing pathway...";
    
    // Log this action.
    log.fine(loadingString + " on " + Arrays.deepToString(geneList.toArray()));
    
    // For all KEGG Pathway ids make a tab
    for (Object pw : geneList) {
      EnrichmentObject<?> pwo = (EnrichmentObject<?>) pw;
      String pwId = pwo.getIdentifier().toString();
      if (pwId.startsWith("path:")) pwId = pwId.substring(5);
      
      //Create the translator panel
      TranslatorPanel pwTab = new TranslatorPanel(source.getSpecies().getKeggAbbr(),pwId,Format.GraphML, this);
      pwTab.setData(pwo);
      source.getIntegratorUI().addTab(pwTab, pwo.getName(), "Pathway: '" + pwo.getName()+"' for " + source.species.getCommonName() + ".");
    }
  }
  
}

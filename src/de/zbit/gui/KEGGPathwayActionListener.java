/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.gui.IntegratorUI.Action;
import de.zbit.kegg.Translator;
import de.zbit.kegg.TranslatorTools;
import de.zbit.kegg.gui.PathwaySelector;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.kegg.gui.TranslatorUI;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.ValuePair;
import de.zbit.util.ValueTriplet;

/**
 * An {@link ActionListener} that can visualize KEGG Pathways
 * using KEGGtranslator.
 * 
 * <p>One instance is created for every compatible {@link TableResultTableModel},
 * which is actually just a list of {@link EnrichmentObject} of KEGG Pathways.
 * 
 * @author Clemens Wrzodek
 */
public class KEGGPathwayActionListener implements ActionListener, PropertyChangeListener {
  public static final transient Logger log = Logger.getLogger(KEGGPathwayActionListener.class.getName());
  
  /**
   * The source tab
   */
  IntegratorTab<?> source;
  
  /**
   * Action command for VISUALIZE_PATHWAY actions.
   */
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
      
    } else if (e.getActionCommand().equals(Action.VISUALIZE_IN_PATHWAY.toString())) {
      try {
        visualizeAndColorPathway();
      } catch (Exception e1) {
        GUITools.showErrorMessage(null, e1);
      }
      
    } else if (e.getActionCommand().equals(TranslatorUI.Action.OPEN_PATHWAY.toString())) {
      visualizePathway(e.getSource().toString(), null);
      
    } else if (e.getActionCommand().equals(TranslatorUI.Action.TRANSLATION_DONE.toString())) {
      TranslatorPanel source = (TranslatorPanel) e.getSource();
      IntegratorUI.getInstance().updateButtons();
      
      // Process translation result
      if (e.getID() != JOptionPane.OK_OPTION) {
        // If translation failed, remove the tab. The error
        // message has already been issued by the translator.
        
        IntegratorUI.getInstance().closeTab(source);
      } else if (this.source!=null) {
//        int r = GUITools.showQuestionMessage(source, "Do you want to color the nodes accoring to an observation?", 
//          IntegratorUI.appName, new String[]{"Yes", "No"});
        int r=1;
        if (r==0 || source.getData()!=null && 
            (source.getData() instanceof ValueTriplet<?, ?, ?>) ) {
          /* Color Pathway (for fold changes) and write singals to nodes.
           */
          ValueTriplet<NameAndSignalsTab, String, SignalType>  vt=null;
          if ( source.getData()!=null && (source.getData() instanceof ValueTriplet<?, ?, ?>)) {
            try {
              vt = (ValueTriplet<NameAndSignalsTab, String, SignalType>) source.getData();
            } catch (Exception ex) {}
          }
          if (vt==null || vt.getA()==null) {
            // Let the user choose a signal to color the nodes
            IntegratorTab st = this.source;
            while (st.getSourceTab()!=null) {
              st = st.getSourceTab();
            }
            vt = IntegratorGUITools.showSelectExperimentBox(IntegratorUI.getInstance(), st);
          }
          
          if (vt!=null) {
            colorPathway(source,(Iterable)vt.getA().getData(),vt.getB(), vt.getC());
          } else {
            r = -1; // At least highlight source gene nodes
          }
        } if (r!=0 && source.getData()!=null && (source.getData() instanceof EnrichmentObject<?>) ) {
          // Highlight source genes
          Collection<Integer> geneIDs = ((EnrichmentObject<?>)source.getData()).getGeneIDsFromGenesInClass();
          TranslatorTools tools = new TranslatorTools(source);
          tools.highlightGenes(geneIDs);
        }
      }
      
    } else if (e.getActionCommand().equals(TranslatorUI.Action.NEW_PROGRESSBAR.toString())) {
      IntegratorUI.getInstance().getStatusBar().showProgress((AbstractProgressBar)e.getSource());
        
    } else {
      log.warning("Unknown action command " + e.getActionCommand());
    }
  }

  /**
   * Color the pathway in <code>tp</code> accoring to the experiment
   * described by <code>experimentName</code> and <code>signalType</code>
   * contained in <code>dataSource</code>.
   * 
   * @param tp pathway to color
   * @param dataSource list with {@link NameAndSignals}
   * @param experimentName name of the observation to color
   * @param signalType signal type of the observation (usually fold change)
   */
  private void colorPathway(TranslatorPanel tp, Iterable<? extends NameAndSignals> dataSource, String experimentName, SignalType signalType) {
    Signal2PathwayTools tools2 = new Signal2PathwayTools(tp);
    
    tools2.writeSignalsToNodes(dataSource);
    
    // Color nodes
    tools2.colorNodesAccordingToSignals(dataSource, 
        MergeType.Mean, experimentName, signalType);
  }
  
  
  /**
   * Download the pathways, selected in the source {@link JTable} and
   * add a {@link TranslatorPanel} for each one.
   */
  private void visualizePathway() {
    if (source==null) return;
    
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
      String pwId; EnrichmentObject<?> pwo=null;
      if (pw instanceof EnrichmentObject<?>) {
        pwo = (EnrichmentObject<?>) pw;
        pwId = pwo.getIdentifier().toString();
      } else {
        pwId = pw.toString();
      }
      if (pwId.startsWith("path:")) pwId = pwId.substring(5);
      
      visualizePathway(pwId, pwo);
    }
  }
  
  /**
   * Download the given pathway and add a {@link TranslatorPanel} for each one.
   * @param pwId pathway id to visualized (e.g., "hsa00130").
   * @param pwo optional parent {@link EnrichmentObject} (if applicable, may be null).
   */
  private TranslatorPanel visualizePathway(String pwId, EnrichmentObject<?> pwo) {
    //Create the translator panel
    TranslatorPanel pwTab = new TranslatorPanel(pwId,Format.GraphML, this);
    String name = pwId;
    if (pwo!=null) {
      pwTab.setData(pwo);
      name = pwo.getName();
    }
    
    String extra = (source==null?"":" for " + source.species.getCommonName());
    IntegratorUI.getInstance().addTab(pwTab, name, "Pathway: '" + name + "'" + extra + ".");
    return pwTab;
  }
  
  /**
   * Builds a pathway selector and downloads and visualizes a
   * pathway.
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public void visualizeAndColorPathway() throws Exception {
    String organism = null;
    if (source.getSpecies(false)!=null) organism = source.getSpecies().getKeggAbbr();
    if (source.getData()!=null) { // Is Ready check
      // Create pathway and experiment selectors
      final PathwaySelector selector = new PathwaySelector(Translator.getFunctionManager(),null, organism);
      final JLabeledComponent expSel;
      if (source instanceof NameAndSignalsTab) {
        expSel = IntegratorGUITools.createSelectExperimentBox((NameAndSignals)source.getExampleData());
      } else {
        expSel = null;
      }
      
      // Make one panel of both selectors
      JPanel ret = new JPanel(new BorderLayout());
      ret.add(selector, BorderLayout.NORTH);
      ret.add(expSel, BorderLayout.SOUTH);

      // Let user chooser
      int val = JOptionPane.showConfirmDialog(source, ret, UIManager.getString("OptionPane.titleText"), JOptionPane.OK_CANCEL_OPTION);
      GUITools.disableOkButton(ret);
      
      // Evaluate and evetually open new tab.
      if (val==JOptionPane.OK_OPTION) {
        ValueTriplet<NameAndSignalsTab, String, SignalType> dataSource;
        if (expSel!=null) {
          ValuePair<String, SignalType> expSignal = (ValuePair<String, SignalType>) expSel.getSelectedItem();
          dataSource = new ValueTriplet<NameAndSignalsTab, String, SignalType>((NameAndSignalsTab) source,
              expSignal.getA(), expSignal.getB());
        } else {
          // Will show the selector later (in TranslationDone).
          dataSource = new ValueTriplet<NameAndSignalsTab, String, SignalType>(null, null, null);
        }
        
        // Open pathway and set experiment to visualize.
        TranslatorPanel tp = visualizePathway(selector.getSelectedPathwayID(), null);
        tp.setData(dataSource);
      }

      /* TODO:
       * - In SwingWorker und nach execute disableOkButton machen.
       * - Add (one or more) tabs for every pathway and !!color immediately!!.
       */

    }
  }

  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    // If the user has been presented a pathway selection dialog and he chooses
    // a pathway, rename this tab accordingly.
    if (evt.getPropertyName().equals("PATHWAY_NAME")) {
      if (evt.getSource() instanceof Component) {
        int idx = IntegratorUI.getInstance().getTabIndex((Component) evt.getSource());
        if (idx>=0) {
          IntegratorUI.getInstance().getTabbedPane().setTitleAt(idx, evt.getNewValue().toString());
        }
      }
    }
    if (evt.getPropertyName().equals("ORGANISM_NAME")) {
      // Could add as tooltip...
    }
  }
  
}

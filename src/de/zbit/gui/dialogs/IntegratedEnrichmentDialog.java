/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui.dialogs;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import de.zbit.data.LabeledObject;
import de.zbit.data.NameAndSignals;
import de.zbit.gui.ActionCommandRenderer;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.LayoutHelper;
import de.zbit.gui.actions.listeners.EnrichmentActionListener;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.kegg.Translator;
import de.zbit.kegg.gui.OrganismSelector;
import de.zbit.parser.Species;

/**
 * Shows a dialog that let's the user choose datasets and options
 * to perform an integrated, heterogeneous enrichment.
 * @author Clemens Wrzodek
 */
public class IntegratedEnrichmentDialog  extends JPanel implements ActionListener {
  private static final long serialVersionUID = 9089266583514540967L;

  /**
   * A dirty static way to set an default selection on the dialog.
   */
  public static NameAndSignalsTab defaultSelection = null;
  
  
  /**
   * Organism selector
   */
  private OrganismSelector orgSel=null;
  
  /**
   * A list of available datasets (dependent on {@link #orgSel}).
   */
  private JList datasets = null;
  
  /**
   * Enrichment type selection
   */
  private JComboBox enrichment = null;

  
  public IntegratedEnrichmentDialog() throws Exception {
    createDialog();
  }
  
  private void createDialog() throws Exception {
    LayoutHelper lh = new LayoutHelper(this);
    
    // Create organism selector
    orgSel = new OrganismSelector(Translator.getFunctionManager(), null, IntegratorUITools.organisms);
    orgSel.addOrganismBoxLoadedCompletelyListener(this);
    lh.add(orgSel);
    
    // Create experiments list
    datasets = new JList();
    datasets.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    JScrollPane expScroll = new JScrollPane(datasets);
    expScroll.setMaximumSize(new Dimension(320,240));
    // Localized name of the CONTROL/STRG-key
    String ctrl = InputEvent.getModifiersExText(InputEvent.CTRL_DOWN_MASK);
    expScroll.setBorder(BorderFactory.createTitledBorder(String.format("Select datasets (hold %s-key)", ctrl)));
    
    lh.add(expScroll);
    
    // Enrichment selection
    enrichment = new JComboBox(EnrichmentActionListener.Enrichments.values());
    enrichment.setRenderer(new ActionCommandRenderer(true));
    enrichment.setBorder(BorderFactory.createTitledBorder("Select enrichment"));
    lh.add(enrichment);
    
    // Write current data sets to selectors and update on species selection
    updateDatasetSelectionBox();
    
    // Listener on species box and others
    if (orgSel!=null) {
      orgSel.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          updateDatasetSelectionBox();
        }
      });
    }
  }
  
  /**
   * 
   */
  @SuppressWarnings("unchecked")
  private void updateDatasetSelectionBox() {
    Species species = getSpeciesFromSelector();
    
    // Create a list of available datasets  
    List<LabeledObject<NameAndSignalsTab>> datasets = IntegratorUITools.getNameAndSignalTabs(species, false, (Class<? extends NameAndSignals>) null);
    this.datasets.setListData(datasets.toArray());
  }
  
  /**
   * @return {@link Species} from current selector or null if none selected
   */
  private Species getSpeciesFromSelector() {
    String abbr = orgSel==null?null:orgSel.getSelectedOrganismAbbreviation();
    return Species.search(IntegratorUITools.organisms, abbr, Species.KEGG_ABBR);
  }
  
  
  
  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().toString().equals(OrganismSelector.LOADING_COMPLETE_ACTION_COMMAND)) {
      setAndEraseDefaultSelection();
    }
  }
  
  /**
   * Set all values to {@link #defaultSelection} and set
   * this variable to <code>NULL</code> afterwards.
   * <p>May only be called if all loading is complete
   * (especially the Organism box!)
   */
  private void setAndEraseDefaultSelection() {
    // Should we set an initial default selection?
    if (defaultSelection!=null && defaultSelection.getSpecies(false)!=null) {
      orgSel.setDefaultSelection(defaultSelection.getSpecies().getScientificName());
      
      int setId = LabeledObject.getIndexOfObject(datasets.getModel(), defaultSelection);
      if (setId>=0) datasets.setSelectedIndex(setId);
    }
    defaultSelection=null;
  }
  
  
  /**
   * Creates and shows an {@link IntegratedEnrichmentDialog}.
   * @return IntegratedEnrichmentDialog
   */
  public static IntegratedEnrichmentDialog showIntegratedEnrichmentDialog() {
    IntegratedEnrichmentDialog intEnrich;
    try {
      intEnrich = new IntegratedEnrichmentDialog();
    } catch (Exception e) {
      GUITools.showErrorMessage(null, e);
      return null;
    }
    
    // Show and evaluate dialog
    int ret = JOptionPane.showConfirmDialog(IntegratorUI.getInstance(), intEnrich, "Integrated enrichment", JOptionPane.OK_CANCEL_OPTION);
    if (ret!=JOptionPane.OK_OPTION) return null;
    else return intEnrich;
  }
  
  /**
   * Creates, shows this dialog and processes selections
   */
  public static void showAndEvaluateIntegratedEnrichmentDialog() {
    IntegratedEnrichmentDialog dialog = showIntegratedEnrichmentDialog();
    if (dialog!=null) {
      
      // Get selected tabs
      List<NameAndSignalsTab> selectedTabs = new ArrayList<NameAndSignalsTab>();
      for (Object tab: dialog.datasets.getSelectedValues()) {
        NameAndSignalsTab nsTab = ((LabeledObject<NameAndSignalsTab>)tab).getObject();
        selectedTabs.add(nsTab);
      }
      if (selectedTabs.size()<1) {
        GUITools.showErrorMessage(null, "No input data selected for enrichment.");
        return;
      }
      
      // Create enrichment-performer, set values and fire event
      EnrichmentActionListener al = new EnrichmentActionListener(null, true);
      al.setSpecies(dialog.getSpeciesFromSelector());
      al.actionPerformed(new ActionEvent(selectedTabs, 0, dialog.enrichment.getSelectedItem().toString()));
    }
  }
  
  
}

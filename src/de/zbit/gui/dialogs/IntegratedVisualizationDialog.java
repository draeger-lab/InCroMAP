/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui.dialogs;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import de.zbit.data.LabeledObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.PairedNS;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.protein.ProteinModificationExpression;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.LayoutHelper;
import de.zbit.gui.actions.TranslatorTabActions.TPAction;
import de.zbit.gui.actions.listeners.KEGGPathwayActionListener;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.kegg.Translator;
import de.zbit.kegg.gui.OrganismSelector;
import de.zbit.kegg.gui.PathwaySelector;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.parser.Species;
import de.zbit.util.ValuePair;
import de.zbit.util.ValueTriplet;

/**
 * Shows a dialog that lets the user chooser a Pathway
 * and one dataset per data type.
 * @author Clemens Wrzodek
 */
public class IntegratedVisualizationDialog extends JPanel {
  private static final long serialVersionUID = 7451686453282214876L;

  /**
   * Data types, that are at the same time in one pathway visualizable.
   * The user may choose one dataset each.
   */
  @SuppressWarnings("unchecked")
  private static Class<? extends NameAndSignals>[] toVisualize = new Class[]{
    mRNA.class, miRNA.class, DNAmethylation.class, ProteinModificationExpression.class
  };
  
  /**
   * Organism selector
   */
  private OrganismSelector orgSel;
  
  /**
   * Pathway selector
   */
  private PathwaySelector pwSel;

  /**
   * Should the data type be visualized?
   */
  private JCheckBox[] visDataType;

  /**
   * Selected datasets to visualize
   */
  private JLabeledComponent[] dataSelect;

  /**
   * Experiments from the selected datasets ({@link #dataSelect}) to visualize
   */
  private JLabeledComponent[] expSelect;
  
  
  public IntegratedVisualizationDialog() throws Exception {
    createIntegrationDialog();
  }
  
  private void createIntegrationDialog() throws Exception {
    //KEGGpwAL.visualizeAndColorPathway(); //<= examples
    LayoutHelper lh = new LayoutHelper(this);
    
    // Create organism and pathway selector
    pwSel = new PathwaySelector(Translator.getFunctionManager(),null, IntegratorUITools.organisms);
    orgSel = pwSel.getOrganismSelector();
    lh.add(pwSel);

    
    // Create selectors for each data type
    visDataType = new JCheckBox[toVisualize.length];
    dataSelect = new JLabeledComponent[toVisualize.length];
    expSelect = new JLabeledComponent[toVisualize.length];
    int i=-1;
    for (Class<? extends NameAndSignals> type : toVisualize) {
      i++;
      JPanel curDS = new JPanel();
      LayoutHelper ld = new LayoutHelper(curDS);
      final JCheckBox visData = new JCheckBox("Visualize " + PairedNS.getTypeNameFull(type) + " data");
      visDataType[i] = visData;
      ld.add(visDataType[i]);
      
      final JLabeledComponent dataSetSelector = new JLabeledComponent("Select a dataset",true,new String[]{"N/A"});
      dataSelect[i] = dataSetSelector;
      ld.add(dataSelect[i]);
      
      final JLabeledComponent experimentSelector = IntegratorUITools.createSelectExperimentBox(null);
      expSelect[i] = experimentSelector;
      ld.add(expSelect[i]);
      
      // Change experiment box upon dataset selection
      dataSelect[i].addActionListener(new ActionListener() {
        @SuppressWarnings("unchecked")
        @Override
        public void actionPerformed(ActionEvent e) {
          NameAndSignalsTab tab = ((LabeledObject<NameAndSignalsTab>)dataSetSelector.getSelectedItem()).getObject();
          IntegratorUITools.createSelectExperimentBox(experimentSelector, (NameAndSignals)tab.getExampleData());
        }
      });
      // Enable/ disable others upon CheckBox selection
      visDataType[i].addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          dataSetSelector.setEnabled(visData.isSelected());
          experimentSelector.setEnabled(visData.isSelected());
        }
      });
      
      curDS.setBorder(new TitledBorder(PairedNS.getTypeNameFull(type)));
      lh.add(curDS);
    }
    
    
    // Write current data sets to selectors and update on species selection
    updateExperimentSelectionBoxes();
    
    // Listener on species box and others
    orgSel.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateExperimentSelectionBoxes();
      }
    });
        
        
      /* TODO: Create the following dialog for each type :
       * 
       * [ ] Visualize (mRNA-PairNS.getTypeName()) data
       *     (________) datasets (same species as in box1 !?!?)
       *     (________) experiments
       *     
       * Disable by default if none is available [for any species].
       * 
       */
    }

  
  /**
   * @return {@link Species} from current selector or null if none selected
   */
  private Species getSpeciesFromSelector() {
    String abbr = orgSel==null?null:orgSel.getSelectedOrganismAbbreviation();
    return Species.search(IntegratorUITools.organisms, abbr, Species.KEGG_ABBR);
  }
  
  /**
   * 
   */
  @SuppressWarnings("unchecked")
  private void updateExperimentSelectionBoxes() {
    Species species = getSpeciesFromSelector();
    
    // Create a list of available datasets  
    int i=-1;
    for (Class<? extends NameAndSignals> type : toVisualize) {
      i++;
      List<LabeledObject<NameAndSignalsTab>> datasets = IntegratorUITools.getNameAndSignalTabsWithSignals(species, type);
      
      if (datasets==null || datasets.size()<1) {
        // Disable all and deselect checkbox
        visDataType[i].setSelected(false);
        visDataType[i].setEnabled(false);
      } else {
        visDataType[i].setEnabled(true);
        dataSelect[i].setHeaders(datasets);
        NameAndSignalsTab nsTab = ((LabeledObject<NameAndSignalsTab>)dataSelect[i].getSelectedItem()).getObject();
        IntegratorUITools.createSelectExperimentBox(expSelect[i], (NameAndSignals)nsTab.getExampleData());
        // TODO: does this also change the experiment box
        //dataSelect[i].fireACtionListeners();
      }
      
      dataSelect[i].setEnabled(visDataType[i].isSelected());
      expSelect[i].setEnabled(visDataType[i].isSelected());
    }
  }
  
  
  public static IntegratedVisualizationDialog showDialog() {
    return showDialog(IntegratorUI.getInstance());
  }
  public static IntegratedVisualizationDialog showDialog(Component parent) {
    IntegratedVisualizationDialog integratedVis;
    try {
      integratedVis = new IntegratedVisualizationDialog();
    } catch (Exception e) {
      GUITools.showErrorMessage(null, e);
      return null;
    }
    
    // Show and evaluate dialog
    int ret = JOptionPane.showConfirmDialog(IntegratorUI.getInstance(), integratedVis, "Integrated data visualization", JOptionPane.OK_CANCEL_OPTION);
    if (ret!=JOptionPane.OK_OPTION) return null;
    else return integratedVis;
  }
  
  /**
   * Shows the {@link IntegratedVisualizationDialog} and
   * evaluates the selection by adding a pathway tab
   * to the current {@link IntegratorUI#instance}.
   * Also sets all data to visualize.
   */
  public static void showAndEvaluateDialog() {
    IntegratorUI ui = IntegratorUI.getInstance();
    IntegratedVisualizationDialog dialog = showDialog(ui);
    if (dialog!=null) {
      // Evaluate and eventually open new tab.
      if (dialog.getPathwaySelector().getSelectedPathwayID()!=null) {
        List<ValueTriplet<NameAndSignalsTab, String, SignalType>> dataSource = new LinkedList<ValueTriplet<NameAndSignalsTab,String,SignalType>>();
        for (int i=0; i<dialog.visDataType.length; i++) {
          if (dialog.visDataType[i].isEnabled() && dialog.visDataType[i].isSelected()) {
            ValuePair<String, SignalType> expSignal = (ValuePair<String, SignalType>) dialog.expSelect[i].getSelectedItem();
            NameAndSignalsTab nsTab = ((LabeledObject<NameAndSignalsTab>)dialog.dataSelect[i].getSelectedItem()).getObject();
            dataSource.add(new ValueTriplet<NameAndSignalsTab, String, SignalType>(
                nsTab, expSignal.getA(), expSignal.getB()));
          }          
        }


        // Open pathway and set experiments to visualize.
        KEGGPathwayActionListener al = new KEGGPathwayActionListener(null);
        TranslatorPanel tp = al.visualizePathway(dialog.getPathwaySelector());
        tp.setData(TPAction.VISUALIZE_DATA.toString(), dataSource);
      }
    }
  }

  /**
   * @return
   */
  public PathwaySelector getPathwaySelector() {
    return pwSel;
  }
  
}

/**
 * @author Clemens Wrzodek
 */
package de.zbit.analysis;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.zbit.data.LabeledObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.PairedNS;
import de.zbit.data.miRNA.miRNA;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.tabs.IntegratorTab;
import de.zbit.gui.tabs.NameAndSignalsTab;

/**
 * Provides tools and methods two pair two Collections of {@link NameAndSignals}.
 * @author Clemens Wrzodek
 */
public class PairData {
  
  /**
   * Tab, containing the first part to pair with a second one.
   */
  private final NameAndSignalsTab firstPart;
  
  /**
   * The data of the tab in {@link #firstPart}.
   */
  private final Collection<? extends NameAndSignals> data;
  
  
  public PairData(NameAndSignalsTab firstPart) {
   super();
   this.firstPart = firstPart;
   this.data = firstPart.getData();
  }
  
  public PairData(Collection<? extends NameAndSignals> data) {
    super();
    this.firstPart = null;
    this.data = firstPart.getData();
  }
  
  

  /**
   * TODO: ...
   * @param <T>
   * @return
   */
  private <T extends NameAndSignals> NameAndSignalsTab showIntegrateWithDialog(NameAndSignalsTab parent) {
    final JPanel jp = new JPanel(new GridLayout(0,1));
    String dialogTitle = "Integrate datasets";
    
    // Create a list of available datasets and get initial selection.
    List<LabeledObject<IntegratorTab<?>>> datasets = IntegratorUITools.getNameAndSignalTabs(true, null, null);
    if (datasets.size()<1) {
      GUITools.showMessage("Could not find any second dataset for pairing.", IntegratorUI.appName);
      return null;
    } else {
      final JLabeledComponent dataSelect = new JLabeledComponent("Select other dataset",true,datasets);
      
      // Add the dataset selector to a panel
      jp.add (dataSelect, BorderLayout.CENTER);
      
      JCheckBox gene_center = new JCheckBox("Gene center data before pairing", true);
      jp.add(gene_center);
      
      jp.add(createMergedSignalDialog());
      
      // Show and evaluate dialog
      int ret = JOptionPane.showConfirmDialog(IntegratorUI.getInstance(), jp, dialogTitle, JOptionPane.OK_CANCEL_OPTION);
      if (ret==JOptionPane.OK_OPTION) {
//        ValuePair<String, SignalType> expSignal = (ValuePair<String, SignalType>) selExpBox.getSelectedItem();
//        return new ValueTriplet<NameAndSignalsTab, String, SignalType>(
//            (NameAndSignalsTab) ((LabeledObject)dataSelect.getSelectedItem()).getObject(),
//            expSignal.getA(), expSignal.getB());
        return  ((LabeledObject<NameAndSignalsTab>)dataSelect.getSelectedItem()).getObject();
      } else {
        return null;
      }
      
    }
  }
  
  
  
  
  
  /**
   * 
   *    * @param part1 (NameAndSignals)parent.getExampleData()
   * parent.getTabName()
   * @param part2 null
   * null
   * @param dataSelect {@link JLabeledComponent} containing {@link LabeledObject}s with {@link NameAndSignalsTab}s.
   * @return
   * 
   * @param part1
   * @param textForPart1
   * @param part2
   * @param textForPart2
   * @param dataSelect
   * @return
   */
  private static Component createMergedSignalDialog(NameAndSignals part1, String textForPart1, NameAndSignals part2, String textForPart2, final JLabeledComponent dataSelect) { 
    final JPanel jp = new JPanel(new GridLayout(0,1));
    
    final JCheckBox calc_signal = new JCheckBox("Calculate a merged observation", true);
    jp.add(calc_signal);
    
    final JLabeledComponent selExpBox1 = IntegratorUITools.createSelectExperimentBox(part1);
    selExpBox1.setTitle(String.format("Observation from '%s'", textForPart1));
    jp.add(selExpBox1);
    
    // Create second experiment-selection box, dependent on "dataSelect"
    final JLabeledComponent selExpBox2 = new JLabeledComponent("Select an observation",true,new String[]{""});
    if (dataSelect!=null && dataSelect.getSelectedValue()>=0) {
      NameAndSignalsTab nsTab = ((LabeledObject<NameAndSignalsTab>)dataSelect.getSelectedItem()).getObject();
      IntegratorUITools.createSelectExperimentBox(selExpBox2,(NameAndSignals)nsTab.getExampleData());
      selExpBox2.setTitle(String.format("Observation from '%s'", nsTab.getTabName()));
    } else if (part2!=null) {
      IntegratorUITools.createSelectExperimentBox(selExpBox2,part2);
      selExpBox2.setTitle(String.format("Observation from '%s'", textForPart2));
    } else {
      selExpBox2.setEnabled(false); selExpBox1.setEnabled(false); calc_signal.setEnabled(false);
    }
    jp.add(selExpBox2);
    
    // Change signal-related parts of dialog according to selection on "dataSelect"
    ActionListener enableSignalSelection = new ActionListener() {
      NameAndSignals lastItem = null;
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean enable = true;
        if (dataSelect!=null) {
          enable = false;
          NameAndSignalsTab tab = ((LabeledObject<NameAndSignalsTab>)dataSelect.getSelectedItem()).getObject();
          NameAndSignals exData = (NameAndSignals)tab.getExampleData();
          if ((exData).hasSignals()) {
            if (!exData.equals(lastItem)) {
              IntegratorUITools.createSelectExperimentBox(selExpBox2, (NameAndSignals)tab.getExampleData());
              selExpBox2.setTitle(String.format("Observation from '%s'", tab.getTabName()));
            }
            lastItem = exData;
            enable=true;
          }
        }
        calc_signal.setEnabled(enable);
        selExpBox2.setEnabled(enable && calc_signal.isSelected());
        selExpBox1.setEnabled(enable && calc_signal.isSelected());
      }
    };
    if (dataSelect!=null) dataSelect.addActionListener(enableSignalSelection);
    calc_signal.addActionListener(enableSignalSelection);
    
    // getMergeType()// TODO: MErgeTypes - ASkUser + Sum, Difference, |Sum|
    
    return jp;
  }

  /**
   * Shows a dialog that lets the user choose a second pair to match
   * the {@link #firstPart} and subsequently pairs the data.
   */
  public void showDialogAndPairData() {
    // Ask for other dataset
    NameAndSignalsTab other = showIntegrateWithDialog(firstPart);
    pairData(other, true);
  }
  
  /**
   * @param tab {@link NameAndSignalsTab} containing the data to pair current {@link #data} with
   * @param annotateTargetsAndRecurse if true, automatically promts the user to annotate his miRNA with
   * targets, if they are missing and recursively calls this method again.
   * @return 
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private List pairData(NameAndSignalsTab tab, boolean annotateTargetsAndRecurse) {
    if (tab==null) return null; // Aborted by user or application.
    
    List pairedData = pairWith_Unchecked(tab.getData());
    if (pairedData==null || pairedData.size()<1) {
      // Something went wrong...
      String message = "Could not detect any matching data pair.";
      
      if (firstPart!=null && !firstPart.getSpecies().equals(tab.getSpecies())) {
        // same species?
        message = "Can not pair data from different species.";
        
        // Are there missing annotated targets?
      } else if (firstPart!=null && NameAndSignals.isMicroRNA(firstPart.getData())) {
        if (annotateTargetsAndRecurse && !miRNA.hasTargets((Iterable<? extends miRNA>) firstPart)) {
          firstPart.getActions().annotateMiRNAtargets();
          return pairData(tab, false);
        }
          
      } else if (NameAndSignals.isMicroRNA(tab.getData())) {
        if (annotateTargetsAndRecurse && !miRNA.hasTargets((Iterable<? extends miRNA>) tab.getData())) {
          tab.getActions().annotateMiRNAtargets();
          return pairData(tab, false);
        }
      }
      
      // Show error
      GUITools.showErrorMessage(firstPart, message);
      return null;
    }
    
    return pairedData;
  }
  
  
  /**
   * Pair two {@link NameAndSignals} lists.
   * <p>Does not check the result or does any auto-correction.
   * @param <T2> any {@link NameAndSignals}
   * @param nsTwos list to pair the {@link #data} with
   * @return if exactly one of <code>nsOnes</code> or <code>nsTwos</code> 
   * is an instance of {@link miRNA}, returns a <code>List&lt;PairedNS&lt;miRNA, Other&gt;&gt;</code>
   * else, a <code>List&lt;PairedNS&lt;T1, T2&gt;&gt;</code> is returned.
   */
  @SuppressWarnings({ "rawtypes" })
  private <T2 extends NameAndSignals> List pairWith_Unchecked(Collection<T2> nsTwos) {
    return PairedNS.pair(data, nsTwos);
  }
  
}

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
package de.zbit.gui.dialogs;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.zbit.analysis.PairData;
import de.zbit.data.NameAndSignals;
import de.zbit.data.PairedNS;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.util.objectwrapper.LabeledObject;
import de.zbit.util.objectwrapper.ValuePair;

/**
 * A dialog that requests all information to
 * generate a merged {@link Signal} from two other signals.
 * 
 * <p>See {@link PairData} for methods to evaluate this dialog.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class MergedSignalDialog extends JPanel {
  private static final long serialVersionUID = -4702608085940232938L;

  /**
   * Defines the way how to merge exactly two signals.
   * @author Clemens Wrzodek
   */
  public static enum MergeTypeForTwo {
    Sum, Difference, AbsoluteSum, Mean, Minimum, Maximum, MaximumDistanceToZero;
  }
  

  /**
   * This is required and is any {@link NameAndSignals} that
   * contains {@link Signal}s to choose one from for the first
   * part of signals to merge.
   */
  private NameAndSignals part1;
  
  /**
   * Identifier for part1. E.g., the tab name or data type name, etc.
   */
  private String textForPart1;

  
  private JCheckBox calc_signal;
  private JLabeledComponent selExpBox1;
  private JLabeledComponent selExpBox2;
  private JLabeledComponent selMergeType;
  
  
  /**
   * Simply inits the layout and sets global variables.
   * @param part1
   * @param textForPart1
   */
  private MergedSignalDialog(NameAndSignals part1, String textForPart1) {
    super(new GridLayout(0,1));
    this.part1 = part1;
    this.textForPart1 = textForPart1==null?"Part 1": textForPart1;
  }
  
  /**
   * Create a dialog that requests all information to
   * generate a merged {@link Signal} from two other signals.
   * @param part1 This is required and is any {@link NameAndSignals} that
   * contains {@link Signal}s to choose one from for the first
   * part of signals to merge.
   * @param textForPart1 Identifier for part1. E.g., the tab name or data type name, etc.
   * @param part2 any {@link NameAndSignals} to choose signals for the second
   * part of signals to merge
   * @param textForPart2 any string identifer for <code>part2</code>.
   */
  public MergedSignalDialog(NameAndSignals part1, String textForPart1,  NameAndSignals part2, String textForPart2) {
    this(part1, textForPart1);
    initDialog(part2, textForPart2, null);
  }
  
  /**
   * Create a dialog that requests all information to
   * generate a merged {@link Signal} from two other signals.
   * @param part1 This is required and is any {@link NameAndSignals} that
   * contains {@link Signal}s to choose one from for the first
   * part of signals to merge.
   * @param textForPart1 Identifier for part1. E.g., the tab name or data type name, etc.
   * @param nsTabSelector must be a {@link JLabeledComponent} containing {@link LabeledObject}s with {@link NameAndSignalsTab}s.
   * This can be created, e.g., with {@link IntegratorUITools#getNameAndSignalTabs(boolean, java.util.Collection, java.util.Collection)}.
   */
  public MergedSignalDialog(NameAndSignals part1, String textForPart1, JLabeledComponent nsTabSelector) {
    this(part1, textForPart1);
    initDialog(null, null, nsTabSelector);
  }
  
  /**
   * Allows to remove the "Calculate a merged observation" box from
   * the dialog.
   */
  public void hideCalculateMergedObservationBox() {
    calc_signal.setEnabled(true);
    calc_signal.setSelected(true);
    calc_signal.setVisible(false);
  }
  
  
  
  
  /**
   * Initialize this dialog.
   * 
   * @param part2 only set this OR <code>dataSelect</code>.
   * @param textForPart2 only required if part2!=null
   * @param dataSelect only set this OR <code>part2</code>.
   * @return
   */
  @SuppressWarnings("unchecked")
  private void initDialog(NameAndSignals part2, String textForPart2, final JLabeledComponent dataSelect) { 
    
    calc_signal = new JCheckBox("Calculate a merged observation", true);
    add(calc_signal);
    
    selExpBox1 = IntegratorUITools.createSelectExperimentBox(part1);
    selExpBox1.setTitle(String.format("Observation from '%s'", textForPart1));
    add(selExpBox1);
    
    // Create second experiment-selection box, dependent on "dataSelect" or "part2"
    selExpBox2 = new JLabeledComponent("Select an observation",true,new String[]{""});
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
    add(selExpBox2);
    
    // MergeType (what to calculate?)
    selMergeType = new JLabeledComponent("Select calculation",true, MergeTypeForTwo.values());
    add(selMergeType);
    
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
        selMergeType.setEnabled(enable && calc_signal.isSelected());
      }
    };
    if (dataSelect!=null) dataSelect.addActionListener(enableSignalSelection);
    calc_signal.addActionListener(enableSignalSelection);
  }
  
  
  /**
   * Does the user wants to have a merged signal?
   * @return currently selected state
   */
  public boolean getCalculateMergedSignal() {
    return calc_signal.isSelected();
  }
  
  /**
   * @return selected signal1
   */
  @SuppressWarnings("unchecked")
  public ValuePair<String, SignalType> getSignal1() {
    if (selExpBox1==null) return null;
    return (ValuePair<String, SignalType>) selExpBox1.getSelectedItem();
  }
  
  /**
   * 
   * @return selected signal2
   */
  @SuppressWarnings("unchecked")
  public ValuePair<String, SignalType> getSignal2() {
    if (selExpBox2==null) return null;
    return (ValuePair<String, SignalType>) selExpBox2.getSelectedItem();
  }
  
  /**
   * @return Calculation, how both signals should be merged
   */
  public MergeTypeForTwo getMergeType() {
    if (selMergeType==null) return null;
    return (MergeTypeForTwo) selMergeType.getSelectedItem();
  }

  /**
   * @return
   */
  public NameAndSignals getPart1() {
    return part1;
  }

  /**
   * @param parent
   * @param part1
   * @param textForPart1
   * @param part2
   * @param textForPart2
   * @see MergedSignalDialog#MergedSignalDialog(NameAndSignals, String, NameAndSignals, String)
   * @return
   */
  public static MergedSignalDialog showDialog(Component parent, NameAndSignals part1, String textForPart1,  NameAndSignals part2, String textForPart2) {
    MergedSignalDialog mergeSignal = new MergedSignalDialog(part1, textForPart1, part2, textForPart2);
    mergeSignal.hideCalculateMergedObservationBox();
    
    // Show and evaluate dialog
    int ret = JOptionPane.showConfirmDialog(IntegratorUI.getInstance(), mergeSignal, "Add observation", JOptionPane.OK_CANCEL_OPTION);
    if (ret!=JOptionPane.OK_OPTION) return null;
    else return mergeSignal;
  }

  /**
   * @param parent
   * @param data
   * @return
   */
  public static MergedSignalDialog showDialog(NameAndSignalsTab parent, PairedNS<?,?> example) {
    return showDialog(parent, example.getNS1(), PairedNS.getTypeName(example.getNS1().getClass()),
      example.getNS2(), PairedNS.getTypeName(example.getNS2().getClass()));
  }
  
}

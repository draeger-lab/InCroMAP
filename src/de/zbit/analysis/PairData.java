/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/Integrator> to
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
package de.zbit.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.PairedNS;
import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.miRNA.miRNA;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.dialogs.MergedSignalDialog;
import de.zbit.gui.dialogs.MergedSignalDialog.MergeTypeForTwo;
import de.zbit.gui.layout.LayoutHelper;
import de.zbit.gui.tabs.IntegratorTab;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.util.Species;
import de.zbit.util.objectwrapper.LabeledObject;
import de.zbit.util.objectwrapper.ValuePair;

/**
 * Provides tools and methods two pair two Collections of {@link NameAndSignals}.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class PairData {
  public static final transient Logger log = Logger.getLogger(PairData.class.getName());
  
  /**
   * Tab, containing the first part to pair with a second one.
   */
  private final NameAndSignalsTab firstPart;
  
  /**
   * The data of the tab in {@link #firstPart}.
   */
  private final Collection<? extends NameAndSignals> data;

  /**
   * Just to remember the last tab, that has been selected.
   */
  private NameAndSignalsTab lastSelectedOtherTab;
  
  
  /**
   * @return the lastSelectedOtherTab
   */
  public NameAndSignalsTab getLastSelectedOtherTab() {
    return lastSelectedOtherTab;
  }

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
   * Shows a dialog that lets the user choose a second pair to match
   * the {@link #firstPart} and subsequently pairs the data.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public List showDialogAndPairData() {
    
    // Ask for other dataset
    final JPanel jp = new JPanel();
    LayoutHelper lh = new LayoutHelper(jp);
    
    // Create a list of available datasets and get initial selection.
    List<Class<?>> excludeDatatypes = new ArrayList<Class<?>>();
    excludeDatatypes.add(EnrichmentObject.class);
    List<LabeledObject<IntegratorTab<?>>> datasets = IntegratorUITools.getNameAndSignalTabs(true, excludeDatatypes, null);
    // Filter for species
    Species filter = firstPart!=null?firstPart.getSpecies(false):null;
    filterForSpecies(datasets, filter);
    if (datasets.size()<1) {
      GUITools.showMessage("Could not find any second dataset for pairing.", IntegratorUI.appName);
      return null;
    } else {
      final JLabeledComponent dataSelect = new JLabeledComponent("Select other dataset",true,datasets);
      
      // Add the dataset selector to a panel
      lh.add(dataSelect);
      
      // Add other options (gene-centere, merged-signal)
      JCheckBox gene_center = new JCheckBox("Gene center both datatsets before pairing", true);
      lh.add(gene_center);
      
      String firstPartName = firstPart!=null?firstPart.getTabName():data.iterator().next().getClass().getSimpleName();
      MergedSignalDialog mergeSignal = new MergedSignalDialog(data.iterator().next(), firstPartName, dataSelect);
      lh.add(mergeSignal);
      
      // Show and evaluate dialog
      int ret = JOptionPane.showConfirmDialog(IntegratorUI.getInstance(), jp, "Pair data", JOptionPane.OK_CANCEL_OPTION);
      if (ret!=JOptionPane.OK_OPTION) return null;
      
      // Perform data pairing
      lastSelectedOtherTab = ((LabeledObject<NameAndSignalsTab>)dataSelect.getSelectedItem()).getObject();
      List pairedData = pairData(lastSelectedOtherTab, gene_center.isSelected(), true);
      if (pairedData==null || pairedData.size()<1) return null; // Message already issued
      log.info(String.format("Created a total of %s pairs.", pairedData.size()));
      
      // Add eventually the selected signal
      PairData.calculateMergedSignal(pairedData, mergeSignal, true);
      
      return pairedData;
    }
    
    
  }

  /**
   * Filters the given datasets to return only those, that are for a
   * defined species.
   * @param datasets
   * @param filter
   */
  public static void filterForSpecies(List<LabeledObject<IntegratorTab<?>>> datasets,
    Species filter) {
    Iterator<LabeledObject<IntegratorTab<?>>> it = datasets.iterator();
    while (it.hasNext()) {
      LabeledObject<IntegratorTab<?>> lo = it.next();
      Species spec2 = lo.getObject().getSpecies(false);
      if (filter==null) continue;
      else if (spec2==null || !filter.equals(spec2)) {
        it.remove();
      }
    }
  }
  
  
  
  
  
  
  /**
   * Evaluate selections in a {@link MergedSignalDialog} and
   * calculate the merged signal.
   * @param pairedData
   * @param mergeSignal
   */
  public static void calculateMergedSignal(Iterable<PairedNS<?, ?>> pairedData,
    MergedSignalDialog mergeSignal) {
    calculateMergedSignal(pairedData, mergeSignal, false);
  }
  /**
   * 
   * @param pairedData
   * @param mergeSignal
   * @param generateUpDownColumn if true, a further column, containing
   * simply fold-change informatin like "up_down" is generated
   * (only if this is true and both signals are fold-changes).
   * @see #calculateMergedSignal(List, MergedSignalDialog)
   */
  @SuppressWarnings("rawtypes")
  public static void calculateMergedSignal(Iterable<PairedNS<?, ?>> pairedData,
      MergedSignalDialog mergeSignal, boolean generateUpDownColumn) {
    // Check if we should calculate
    if (!mergeSignal.getCalculateMergedSignal() ||
        pairedData==null || !pairedData.iterator().hasNext()) return;
    
    // Get properties
    ValuePair<String, SignalType> signal1 = mergeSignal.getSignal1();
    ValuePair<String, SignalType> signal2 = mergeSignal.getSignal2();
    MergeTypeForTwo mergeType = mergeSignal.getMergeType();
    
    // Eventually we have to switch signal1 and two, because miRNAs are always first
    // in PairedNS!
    // See also {@link PairedNS#pair(Collection, Collection, boolean)}
    PairedNS example = pairedData.iterator().next();
    if (!miRNA.class.isAssignableFrom(mergeSignal.getPart1().getClass()) &&
       miRNA.class.isAssignableFrom(example.getNS1().getClass())) {
      /* Ok... Originally, part1 was no miRNA, but after pairing,
       * the first part was an miRNA. => nsOne and two have switched
       * positions, thus, signal1 and signal2 must also switch positions.
       */
      ValuePair<String, SignalType> signalTemp = signal1;
      signal1 = signal2;
      signal2 = signalTemp;
    }
    
    // Ony generate up_down on fold changes
    generateUpDownColumn &= signal1.getB().equals(SignalType.FoldChange)  && signal2.getB().equals(SignalType.FoldChange);
    
    // Set new name and type (MergeType is NOT USED for strings).
    String newSigName = getNiceMergedSignalName(signal1.getA().toString(), signal2.getA().toString(), mergeType);
    
    // Perform calculation
    Double zero = new Double(0.0d);
    for (PairedNS<?, ?> pairedNS : pairedData) {
      Signal sig1 = pairedNS.getNS1().getSignal(signal1.getB(), signal1.getA());
      Signal sig2 = pairedNS.getNS2().getSignal(signal2.getB(), signal2.getA());
      Number value = calculate(sig1, sig2, mergeType);
      pairedNS.addSignal(value,newSigName, SignalType.Merged);
      
      if (generateUpDownColumn) {
        StringBuilder ud = new StringBuilder();
        if (sig1.compareTo(zero)>0) ud.append("Up_");
        else ud.append("Down_");
        if (sig2.compareTo(zero)>0) ud.append("Up");
        else ud.append("Down");
        pairedNS.addData(newSigName.concat(" relation"), ud.toString());
      }
    }
  }

  /**
   * Return a nice string, describing the mathematical operation
   * defined by <code>mergeType</code> on the given two experiment names.
   * @param experiment1
   * @param experiment2
   * @param mergeType
   * @return
   */
  public static String getNiceMergedSignalName(String experiment1,String experiment2, MergeTypeForTwo mergeType) {
    String newSigName;
    if (experiment1.equalsIgnoreCase(experiment2)) {
      newSigName = String.format("%s of %s", mergeType.toString(), experiment1);
    } else {
      if (mergeType.equals(MergeTypeForTwo.AbsoluteSum)) {
        newSigName = String.format("|%s| + |%s|", experiment1, experiment2);
      } else if (mergeType.equals(MergeTypeForTwo.Sum)) {
        newSigName = String.format("%s + %s", experiment1, experiment2);
      } else if (mergeType.equals(MergeTypeForTwo.Difference)) {
        newSigName = String.format("%s - %s", experiment1, experiment2);
      } else if (mergeType.equals(MergeTypeForTwo.Maximum)) {
        newSigName = String.format("max(%s, %s)", experiment1, experiment2);
      } else if (mergeType.equals(MergeTypeForTwo.MaximumDistanceToZero)) {
        newSigName = String.format("max(|%s|, |%s|)", experiment1, experiment2);
      } else if (mergeType.equals(MergeTypeForTwo.Minimum)) {
        newSigName = String.format("min(%s, %s)", experiment1, experiment2);
      } else {
        newSigName = String.format("%s of %s and %s", mergeType.toString(), experiment1, experiment2);
      }
    }
    return newSigName;
  }



  /**
   * Calculate a calculation, defined by <code>mergeType</code> on
   * <code>signal1</code> and <code>signal2</code>.
   * @param signal1
   * @param signal2
   * @param mergeType
   * @return
   */
  public static Number calculate(Signal signal1, Signal signal2, MergeTypeForTwo mergeType) {
    if ((signal1==null||Double.isNaN(signal1.getSignal().doubleValue())) ||
        (signal2==null||Double.isNaN(signal2.getSignal().doubleValue()))) return Double.NaN;
    double val1 = signal1.getSignal().doubleValue();
    double val2 = signal2.getSignal().doubleValue();
    
    if (mergeType.equals(MergeTypeForTwo.AbsoluteSum)) {
      return Math.abs(val1) + Math.abs(val2);
    } else if (mergeType.equals(MergeTypeForTwo.Sum)) {
      return val1 + val2;
    } else if (mergeType.equals(MergeTypeForTwo.Difference)) {
      return (val1) - (val2);
    } else {
      MergeType otherType = MergeType.valueOf(mergeType.toString());
      if (otherType!=null) {
        return Signal.calculate(otherType, val1, val2);
      } else {
        log.severe("Don't know how to calculate " + mergeType.toString());
        return Double.NaN;
      }
    }
  }

  /**
   * Interactive data pairing (shows messages and asks user to annotate {@link miRNA} targets).
   * @param tab {@link NameAndSignalsTab} containing the data to pair current {@link #data} with
   * @param geneCenter if true, lists will be gene-centered before pairing
   * @param annotateTargetsAndRecurse if true, automatically promts the user to annotate his miRNA with
   * targets, if they are missing and recursively calls this method again.
   * @return 
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private List pairData(NameAndSignalsTab tab, boolean geneCenter, boolean annotateTargetsAndRecurse) {
    if (tab==null) return null; // Aborted by user or application.
    
    List pairedData = pairWith_Unchecked(tab.getData(),geneCenter);
    if (pairedData==null || pairedData.size()<1) {
      // Something went wrong...
      String message = "Could not detect any matching data pair.";
      
      if (firstPart!=null && !firstPart.getSpecies().equals(tab.getSpecies())) {
        // same species?
        message = "Can not pair data from different species.";
        
        // Are there missing annotated targets?
      } else if (firstPart!=null && NameAndSignals.isMicroRNA(firstPart.getData())) {
        if (annotateTargetsAndRecurse && !miRNA.hasTargets((Iterable<? extends miRNA>) firstPart.getData())) {
          firstPart.getActions().annotateMiRNAtargets();
          return pairData(tab, geneCenter, false);
        }
          
      } else if (NameAndSignals.isMicroRNA(tab.getData())) {
        if (annotateTargetsAndRecurse && !miRNA.hasTargets((Iterable<? extends miRNA>) tab.getData())) {
          tab.getActions().annotateMiRNAtargets();
          return pairData(tab, geneCenter, false);
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
   * <p>Does not check the result or does any auto-correction (Uninteractive).
   * @param <T2> any {@link NameAndSignals}
   * @param nsTwos list to pair the {@link #data} with
   * @param geneCenter if true, lists will be gene-centered before pairing
   * @return if exactly one of <code>nsOnes</code> or <code>nsTwos</code> 
   * is an instance of {@link miRNA}, returns a <code>List&lt;PairedNS&lt;miRNA, Other&gt;&gt;</code>
   * else, a <code>List&lt;PairedNS&lt;T1, T2&gt;&gt;</code> is returned.
   */
  @SuppressWarnings({ "rawtypes" })
  private <T2 extends NameAndSignals> List pairWith_Unchecked(Collection<T2> nsTwos, boolean geneCenter) {
    return PairedNS.pair(data, nsTwos, geneCenter);
  }
  
}

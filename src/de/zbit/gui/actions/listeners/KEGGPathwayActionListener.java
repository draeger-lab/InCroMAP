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
package de.zbit.gui.actions.listeners;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import y.view.Graph2D;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.miRNA.miRNA;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.gui.BaseFrameTab;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.gui.actions.NameAndSignalTabActions.NSAction;
import de.zbit.gui.actions.TranslatorTabActions;
import de.zbit.gui.actions.TranslatorTabActions.TPAction;
import de.zbit.gui.customcomponents.SpeciesHolder;
import de.zbit.gui.customcomponents.TableResultTableModel;
import de.zbit.gui.dialogs.VisualizeDataInPathwayDialog;
import de.zbit.gui.prefs.PathwayVisualizationOptions;
import de.zbit.gui.prefs.SignalOptions;
import de.zbit.gui.tabs.IntegratorTab;
import de.zbit.gui.tabs.IntegratorTabWithTable;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.integrator.TranslatorToolsExtended;
import de.zbit.kegg.Translator;
import de.zbit.kegg.gui.IntegratorPathwayPanel;
import de.zbit.kegg.gui.PathwaySelector;
import de.zbit.kegg.gui.TranslatorUI;
import de.zbit.util.Species;
import de.zbit.util.TranslatorTools;
import de.zbit.util.objectwrapper.ValuePair;
import de.zbit.util.objectwrapper.ValueTriplet;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.progressbar.AbstractProgressBar;
import de.zbit.visualization.VisualizeDataInPathway;
import de.zbit.visualization.VisualizeMicroRNAdata;

/**
 * An {@link ActionListener} that can visualize KEGG Pathways
 * using KEGGtranslator.
 * 
 * <p>One instance is created for every compatible {@link TableResultTableModel},
 * which is actually just a list of {@link EnrichmentObject} of KEGG Pathways.
 * 
 * <p><i>Note:<br/>
 * Due to yFiles license requirements, we have to obfuscate this class
 * in the JAR release of this application. Thus, this class
 * can not be found by using the class name.<br/> If you can provide us
 * with a proof of possessing a yFiles license yourself, we can send you
 * an unobfuscated release of Integrator.</i></p>
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class KEGGPathwayActionListener implements ActionListener, PropertyChangeListener {
  public static final transient Logger log = Logger.getLogger(KEGGPathwayActionListener.class.getName());
  
  /**
   * The source tab
   */
  BaseFrameTab source;
  
  /**
   * Action command for VISUALIZE_PATHWAY actions.
   */
  public final static ActionCommand VISUALIZE_PATHWAY = new ActionCommand() {
    @Override
    public String getToolTip() {
      return "Download and visualize a KEGG pathway.";
    }
    @Override
    public String getName() {
      return "Visualize pathway";
    }
  };
  
  /**
   * 
   * @param source Optional: The source tab from which this one originates.
   * Mostly referring to the expression data or enrichment p-values that
   * should be visualized in the pathway. <code>NULL</code> permitted.
   */
  public KEGGPathwayActionListener(BaseFrameTab source) {
    this.source = source;
  }
  
  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public void actionPerformed(ActionEvent e) {
 
    if (e.getActionCommand().equals(VISUALIZE_PATHWAY.toString())) {
      visualizePathway();
      
    } else if (e.getActionCommand().equals(NSAction.VISUALIZE_IN_PATHWAY.toString())) {
      // Coming mostly from a click on the toolbar-button in NameAndSignalTabs
      try {
        visualizeAndColorPathway();
      } catch (Exception e1) {
        GUITools.showErrorMessage(null, e1);
      }
      
    } else if (e.getActionCommand().equals(NSAction.VISUALIZE_SELECTED_DATA_IN_PATHWAY.toString())) {
      try {
        if (source instanceof IntegratorTabWithTable) {
          // TODO: One could implement functionality to limit visualization to selected data/genes.
          @SuppressWarnings("unused")
          List<?> toVisualize = ((IntegratorTabWithTable) source).getSelectedItems();
        }
        visualizeAndColorPathway();
      } catch (Exception e1) {
        GUITools.showErrorMessage(null, e1);
      }
      
    } else if (e.getActionCommand().equals(TPAction.VISUALIZE_DATA.toString()) &&
        source instanceof TranslatorPanel) {
      // Coming mostly from a click on the toolbar-button in TranslatorPanelTabs
      Species spec = getSpecies();
      ValueTriplet<NameAndSignalsTab, String, SignalType>  vt =
        IntegratorUITools.showSelectExperimentBox(null,
        "Please select an observation to visualize in this pathway.", spec);
      if (vt!=null) {
        visualizeData((TranslatorPanel) source,vt.getA(),vt.getB(), vt.getC());
      }
      
    } else if (e.getActionCommand().equals(TPAction.VISUALIZE_ENRICHMENT_PVALUES.toString()) &&
        source instanceof TranslatorPanel) {
      // Coming mostly from a click on the toolbar-button in TranslatorPanelTabs
      Species spec = getSpecies();
      ValueTriplet<NameAndSignalsTab, String, SignalType>  vt =
        IntegratorUITools.showSelectPathwayEnrichmentBox(spec, "Please select an observation to visualize in this pathway.");
      if (vt!=null) {
        visualizeData((TranslatorPanel) source,vt.getA(),vt.getB(), vt.getC());
      }
      
    } else if (e.getActionCommand().equals(TPAction.HIGHLIGHT_ENRICHED_GENES_AND_COMPOUNDS.toString()) &&
        source instanceof TranslatorPanel) {
      TranslatorPanel source = (TranslatorPanel) this.source;
      if (source.getData(TPAction.HIGHLIGHT_ENRICHED_GENES_AND_COMPOUNDS.toString())!=null) {
        // Highlight source genes
        Collection<Integer> geneIDs = ((EnrichmentObject<?>)source.getData(TPAction.HIGHLIGHT_ENRICHED_GENES_AND_COMPOUNDS.toString())).getGeneIDsFromGenesInClass();
        Collection<String> compoundIDs = ((EnrichmentObject<?>)source.getData(TPAction.HIGHLIGHT_ENRICHED_GENES_AND_COMPOUNDS.toString())).getCompoundIDsFromCompoundsInClass();
        if(geneIDs.size()>0)
        	hightlightGenes(source, geneIDs);
        if(compoundIDs.size()>0)
        	highlightCompounds(source,compoundIDs);
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
        
      } else {
        // Should we immediately visualize data? OR highlight nodes?
        Object dataToVisualize = source.getData(TPAction.VISUALIZE_DATA.toString());
        if (dataToVisualize!=null) {
          // Color Pathway (for fold changes) and write signals to nodes.
          
          boolean askUser=false;
          ValueTriplet<NameAndSignalsTab, String, SignalType>  vt=null;
          try {
            if (dataToVisualize instanceof List) {
              // More than one type/set?
              askUser=true; // if list has size 0
              for(ValueTriplet<NameAndSignalsTab, String, SignalType> vt2: 
                (List<ValueTriplet<NameAndSignalsTab, String, SignalType>>) dataToVisualize) {
                visualizeData(source,vt2.getA(),vt2.getB(), vt2.getC());
                askUser=false;
              }
            } else {
              askUser=true;
              // Single instance to visualize (Eventually ask user)
              vt = (ValueTriplet<NameAndSignalsTab, String, SignalType>) dataToVisualize;
            }
            
          } catch (Exception ex) {log.log(Level.WARNING, "Error while visualizing data.", ex);}
          
          // Get valid data source
          if ((vt==null && askUser) || vt!=null && (vt.getA()==null || vt.getB()==null || vt.getC()==null)) {
            // If data should get visualized, but no valid information is available, ask user.
            // Let the user choose a signal to color the nodes
            IntegratorTab st = null;
            if (this.source!=null && this.source instanceof IntegratorTab) {
              st = (IntegratorTab) this.source;
              while (st.getSourceTab()!=null) {
                st = st.getSourceTab();
              }
            }
            Species spec = getSpecies(source);
            vt = IntegratorUITools.showSelectExperimentBox(st,
              "Please select an observation to visualize in this pathway.", spec);
          }
          
          // Visualize single data instance
          if (vt!=null) {
            visualizeData(source,vt.getA(),vt.getB(), vt.getC());
          } 
          
        } else if (source.getData(TPAction.HIGHLIGHT_ENRICHED_GENES_AND_COMPOUNDS.toString())!=null) {
          // At least highlight source gene nodes
        	Collection<Integer> geneIDs = ((EnrichmentObject<?>)source.getData(TPAction.HIGHLIGHT_ENRICHED_GENES_AND_COMPOUNDS.toString())).getGeneIDsFromGenesInClass();
          Collection<String> compoundIDs = ((EnrichmentObject<?>)source.getData(TPAction.HIGHLIGHT_ENRICHED_GENES_AND_COMPOUNDS.toString())).getCompoundIDsFromCompoundsInClass();
          if(geneIDs.size()>0)
          	hightlightGenes(source, geneIDs);
          if(compoundIDs.size()>0)
          	highlightCompounds(source,compoundIDs);
        }
        
      }
      
    } else if (e.getActionCommand().equals(TranslatorUI.Action.NEW_PROGRESSBAR.toString())) {
      IntegratorUI.getInstance().getStatusBar().showProgress((AbstractProgressBar)e.getSource());
      
    } else {
      log.warning("Unknown action command " + e.getActionCommand());
    }
  }

  /**
   * @return
   */
  public Species getSpecies() {
    return getSpecies(this.source);
  }
  
  /**
   * 
   * @param source
   * @return
   */
  public static Species getSpecies(BaseFrameTab source) {
    Species spec=null;
    if (source==null) return null;
    
    if (source instanceof SpeciesHolder) {
      spec = ((SpeciesHolder)source).getSpecies();
    }
    
    if (spec==null) {
      if (source instanceof TranslatorPanel<?>) {
        spec = TranslatorTabActions.getSpeciesOfPathway((TranslatorPanel<?>) source, IntegratorUITools.organisms);
      } else if (source instanceof IntegratorTab<?>) {
        spec = ((IntegratorTab<?>) source).getSpecies();
      }
        
    }
    return spec;
  }
  
  /**
   * Color the pathway in <code>tp</code> according to the experiment
   * described by <code>experimentName</code> and <code>signalType</code>
   * contained in <code>dataSource</code>.
   * 
   * <p>Taking this method instead of {@link #visualizeData(TranslatorPanel, Iterable, String, SignalType)}
   * allows asking the user to annotate miRNA data with targets, if it has not yet been done. 
   * 
   * @param tp pathway to color
   * @param dataSource to take from the given {@link NameAndSignalsTab}
   * @param experimentName name of the observation to color
   * @param signalType signal type of the observation (usually fold change)
   */
  private synchronized void visualizeData(final TranslatorPanel<Graph2D> tp, final NameAndSignalsTab dataSource, final String experimentName, final SignalType signalType) {
    // @return number of nodes, colored according to the signal, or -1 if an error occured.
    /* This method has the advantage to
     * - Ask the mergeType only once!
     * - Add nodes for miRNAs to visualize
     * And thus should always be preferred to the other colorPathway method.
     */
    
    // Ask the user to set all required options
    SBPreferences prefs = SBPreferences.getPreferencesFor(SignalOptions.class);
    if (!SignalOptions.REMEMBER_GENE_CENTER_DECISION.getValue(prefs)) {
      if (!VisualizeDataInPathwayDialog.showDialog(new VisualizeDataInPathwayDialog(), "Visualize data in pathway")) return;// 0;
      // (All options are automatically processed in the VisualizeData method)
    }
    
    // Ensure that graph is available
    if (tp.getDocument()==null) {
      GUITools.showErrorMessage(null, "Please wait for the graph to load completely.");
      return;// -1;
    } else {
      // Show temporary loading bar
      tp.showTemporaryLoadingBar("Visualizing data in pathway...");
    }
    
    // Perform operations in another thread
    SwingWorker<Integer, Void> visData = new SwingWorker<Integer, Void>() {
      @Override
      protected Integer doInBackground() throws Exception {
        try {
          int coloredNodes=0;
          synchronized (tp.getDocument()) { // don't visualize multiple data at the same time
            // Adds the microRNA NODES to the graph and automatically asks
            // the user to annotate targets to his miRNA data if not already done.
            if (miRNA.class.isAssignableFrom(dataSource.getDataContentType())) {
              if (!addMicroRNAs((Graph2D)tp.getDocument(), dataSource)) {
                log.warning("Could not detect any miRNA targets in the graph.");
                return 0;
              }
            }
            
            // Perform visualization
            VisualizeDataInPathway visData = new VisualizeDataInPathway(tp);
            Class<? extends NameAndSignals> dataType = NameAndSignals.getType(dataSource.getData());
            // Check if there is already this data type visualized and remove old visualization first.
            if (visData.isDataTypeVisualized(dataType)) {
              int answer = GUITools.showQuestionMessage(tp, "The pathway already contains visualized data of the same type (" + 
                dataType.getSimpleName() + "). Do you want to replace the currently visualized data with the given one?", "Visualize data", JOptionPane.YES_NO_OPTION);
              if (answer==JOptionPane.NO_OPTION) return 0;
              visData.removeVisualization(dataType);
            }
            
            coloredNodes = visData.visualizeData(dataSource, experimentName,signalType);
          }
          // Repaint and hide loading screens
          //tp.repaint();
          //tp.hideTemporaryLoadingBar();
          
//          // Let edges dock to labels
//          SBPreferences prefs = SBPreferences.getPreferencesFor(KEGGTranslatorPanelOptions.class);
//          if (TranslatorPanelOptions.LAYOUT_EDGES.getValue(prefs)) {
//            //new GraphTools((Graph2D)tp.getDocument()).layout(OrganicEdgeRouter.class);
//            // TODO:  This would be a great preferred feature!
//            // If last bend/source node below then dock to labels
//            // If last bend/source node left  then dock to labels
//            // Else, if dock > node bounds, remove dock.
//            new GraphTools((Graph2D)tp.getDocument()).adjustEdgesToLabels();
//          }
          
          
          return coloredNodes;
        } catch (Exception e) {
          throw e;
        }
        //return -1;
        
      }
      
      @Override
      protected void done() {
        try {
          // Check for execution errors
          get();
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
          e.printStackTrace();
          GUITools.showErrorMessage(null, e);
        } finally {
          synchronized (tp) {
            tp.hideTemporaryLoadingBar();
            tp.repaint();
            IntegratorUI.getInstance().updateButtons();
          }
        }
      }
    };
    visData.execute();
    
    return;
  }
  
  /**
   * Adds microRNAs to the given graph
   * @param graph
   * @param dataSource microRNA datasource
   * @return true if the graph should contain at least one miRNA that belongs
   * to the given input data
   */
  public static boolean addMicroRNAs(Graph2D graph, NameAndSignalsTab dataSource) {
    return addMicroRNAs(graph, dataSource, true);
  }
  
  @SuppressWarnings("unchecked")
  public static boolean addMicroRNAs(Graph2D graph, NameAndSignalsTab dataSource, boolean askUserToAnnotateDataset) {
    if (!miRNA.class.isAssignableFrom(dataSource.getDataContentType())) {
      log.severe("Can not add miRNA targets when source is " + dataSource.getDataContentType());
      return false;
    }
    
    int addedNodes = addMicroRNAs(graph, (Collection<? extends miRNA>) dataSource.getData());
    if (addedNodes==0 && askUserToAnnotateDataset) {
      // if no nodes have been colored, look if it was due to missing miRNA target annotations
      int a = GUITools.showQuestionMessage(IntegratorUI.getInstance(), "No microRNA had an annotated target within this graph. " +
        "Do you want to (re-)annotate your microRNA data with targets?", IntegratorUI.appName, JOptionPane.YES_NO_OPTION);
      if (a==JOptionPane.YES_OPTION) {
        dataSource.getActions().annotateMiRNAtargets();
        addedNodes = addMicroRNAs(graph, (Collection<? extends miRNA>) dataSource.getData());
      } else {
        return false;
      }
    }
    return addedNodes>0;
  }
  
  /**
   * Adds microRNAs to the given graph.
   * <p> Use preferred {@link #addMicroRNAs(TranslatorPanel, NameAndSignalsTab)}, because it
   * adds missing target annotations to the list. This method here does NOT add targets,
   * if they are missing.
   * @see #addMicroRNAs(TranslatorPanel, NameAndSignalsTab)
   * @param graph
   * @param dataSource Collection with {@link miRNA}s that MUST HAVE targets!
   * @return number of nodes created or -1 if an error occurred.
   */
  public static int addMicroRNAs(Graph2D graph, Collection<? extends miRNA> dataSource) {
    return addMicroRNAs(graph, dataSource, false);
  }
  /**
   * 
   * @param graph
   * @param dataSource
   * @param silent
   * @return
   * @see #addMicroRNAs(Graph2D, Collection)
   */
  public static int addMicroRNAs(Graph2D graph, Collection<? extends miRNA> dataSource,
      boolean silent) {
    if (!NameAndSignals.isMicroRNA(dataSource)) {
      log.severe("Can not add miRNA targets when source is not miRNA.");
      return -1;
    } if (graph==null) {
      log.severe("Graph is not ready (null).");
      return -1;      
    }
    
    VisualizeMicroRNAdata vis = new VisualizeMicroRNAdata(graph);
    int addedNodes = vis.addMicroRNAsToGraph((Collection<? extends miRNA>) dataSource, silent);
    if (addedNodes>0) {
      // The "Remove miRNA-nodes" button must be enabled.
      IntegratorUI.getInstance().updateButtons();
    }
    return addedNodes;
  }
  
  
  /**
   * Highlight nodes with given geneIDs.
   * @param source
   * @param geneIDs
   */
  private void hightlightGenes(TranslatorPanel<Graph2D> source, Iterable<Integer> geneIDs) {
    SBPreferences prefs = SBPreferences.getPreferencesFor(PathwayVisualizationOptions.class);
    Color colorForUnaffectedNodes = PathwayVisualizationOptions.COLOR_FOR_NO_VALUE.getValue(prefs);
    //Color affectedColor = PathwayVisualizationOptions.COLOR_FOR_MAXIMUM_FOLD_CHANGE.getValue(prefs);
    // It is confusing if the same color as for maxFC is used.
    Color affectedColor = Color.YELLOW;
    TranslatorTools tools = new TranslatorTools(source);
    tools.highlightGenes(geneIDs,affectedColor, colorForUnaffectedNodes, true);
    source.repaint();
  }
  
  /**
   * Highlight nodes with given compoundIDs
   * @param source
   * @param compoundIDs
   */
  private void highlightCompounds(TranslatorPanel<Graph2D> source, Iterable<String> compoundIDs){
  	SBPreferences prefs = SBPreferences.getPreferencesFor(PathwayVisualizationOptions.class);
    Color colorForUnaffectedNodes = PathwayVisualizationOptions.COLOR_FOR_NO_VALUE.getValue(prefs);

    Color affectedColor = Color.YELLOW;
    TranslatorToolsExtended tools = new TranslatorToolsExtended(source);
    tools.highlightCompounds(compoundIDs,affectedColor, colorForUnaffectedNodes, true);
    source.repaint();
  }
  
  /**
   * Download the pathways, selected in the source {@link JTable} and
   * add a {@link TranslatorPanel} for each one.
   * <p>This method is used in {@link EnrichmentObject}s.
   */
  private void visualizePathway() {
    if (source==null || !(source instanceof IntegratorTab)) return;
    IntegratorTab<?> source = (IntegratorTab<?>) this.source;
    
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
  TranslatorPanel<Graph2D> visualizePathway(String pwId, EnrichmentObject<?> pwo) {
    //Create the translator panel
    IntegratorPathwayPanel pwTab = new IntegratorPathwayPanel(pwId, this);
    String name = pwId;
    if (pwo!=null) {
      pwTab.setData(TPAction.HIGHLIGHT_ENRICHED_GENES_AND_COMPOUNDS.toString(), pwo);
      name = pwo.getName();
    }
    
    // Try to get species
    Species spec = getSpecies(pwTab);
    if (spec==null && source!=null && (source instanceof IntegratorTab)) {
      spec = getSpecies(source);
    }
    pwTab.setSpecies(spec);
    
    String extra = spec==null?"":" for " + spec.getCommonName();
    IntegratorUI.getInstance().addTab(pwTab, name, "Pathway: '" + name + "'" + extra + ".");
    return pwTab;
  }
  
  /**
   * Evaluates the {@link PathwaySelector} <code>pwSel</code> and
   * adds the corresponding tab to the current 
   * {@link IntegratorUI#instance}.
   * 
   * @param pwSel
   * @return the created tab or null, if selection was not valid.
   */
  public TranslatorPanel<Graph2D> visualizePathway(PathwaySelector pwSel) {
    //Create the translator panel
    String pwId = pwSel.getSelectedPathwayID();
    if (pwId==null) return null;
    IntegratorPathwayPanel pwTab = new IntegratorPathwayPanel(pwId, this);
    
    // Try to get species
    Species spec = IntegratorUITools.getSpeciesFromSelector(pwSel.getOrganismSelector());
    if (spec==null) {
     spec = getSpecies(pwTab);
    } if (spec==null && source!=null && (source instanceof IntegratorTab)) {
      spec = getSpecies(source);
    }
    pwTab.setSpecies(spec);
    
    // Add tab and create ToolTip
    String name = pwSel.getSelectedPathway();
    IntegratorUI.getInstance().addTab(pwTab, name,
      String.format("Pathway: '%s' for %s.", name, pwSel.getOrganismSelector().getSelectedOrganism()));
    return pwTab;
  }
  
  /**
   * Builds a pathway selector and downloads and visualizes a
   * pathway.
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public void visualizeAndColorPathway() throws Exception {
    // Preprocessing: Check if ready and get organism.
    String organism = null;
    if (source instanceof IntegratorTab) {
      IntegratorTab<?> source = (IntegratorTab<?>) this.source;
      if (source.getData()==null) return; // Is Ready check
      if (source.getSpecies(false)!=null) 
        organism = source.getSpecies().getKeggAbbr();
    } else if (source instanceof TranslatorPanel) {
      TranslatorPanel<?> source = (TranslatorPanel<?>) this.source;
      if (source.getDocument()==null) return; // Is Ready check
      organism = TranslatorTools.getOrganismKeggAbbrFromGraph((Graph2D) source.getDocument());
    }
    
    // Create pathway and experiment selectors
    final PathwaySelector selector = new PathwaySelector(Translator.getFunctionManager(),null, organism);
    JLabeledComponent expSel;
    boolean setA=false;
    if (source instanceof NameAndSignalsTab) {
      NameAndSignals ns = (NameAndSignals)((NameAndSignalsTab)source).getExampleData();
      if (ns.hasSignals()) {
        expSel = IntegratorUITools.createSelectExperimentBox(ns);
        // Only show p-values for DNA methylation data
        IntegratorUITools.modifyExperimentBoxForDNAMethylation(expSel, ns);
        if (expSel.getHeaders()==null || expSel.getHeaders().length<1) expSel = null;
      } else if (ns instanceof miRNA) {
        // Targets can still be visualized (even without signals).
        setA = true;
        expSel = null;
      } else {
        expSel = null;
      }
    } else {
      expSel = null;
    }
    
    // Make one panel of both selectors
    JPanel ret = new JPanel(new BorderLayout());
    ret.add(selector, BorderLayout.CENTER);
    if (expSel!=null && expSel.getHeaders()!=null && expSel.getHeaders().length>0) {
      ret.add(expSel, BorderLayout.SOUTH);
    }
    
    // Create a chooser for existing tabs (visualize data in and EXISTING pathway)
    /* If one has time, this could be implemented.
     * TO-DO then: Pathway-tabs need to be filtered to allow only
     * same species as data tab.*/
    //    JLabeledComponent tabSelect = IntegratorGUITools.createSelectPathwayTabBox(IntegratorUI.getInstance());
    //    if (tabSelect!=null && tabSelect.getColumnChooser()!=null && (tabSelect.getColumnChooser() instanceof JComboBox)
    //        && ((JComboBox) tabSelect.getColumnChooser()).getItemCount()>0) {
    //      JComboBox box = (JComboBox) tabSelect.getColumnChooser();
    //      box.addItem("<Create a new pathway tab>");
    //      box.addActionListener(new ActionListener() {
    //        @Override
    //        public void actionPerformed(ActionEvent e) {
    //          System.out.println(e);
    //        }
    //      });
    //      ret.add(tabSelect, BorderLayout.NORTH);
    //    }
    
    // Evaluate and eventually open new tab.
    final boolean setAfinal = setA;
    final JLabeledComponent expSelFinal = expSel;
    Runnable okAction = new Runnable() {
      @Override
      public void run() {
        if (selector.getSelectedPathwayID()!=null) {
          ValueTriplet<NameAndSignalsTab, String, SignalType> dataSource;
          if (expSelFinal!=null) {
            ValuePair<String, SignalType> expSignal = (ValuePair<String, SignalType>) expSelFinal.getSelectedItem();
            dataSource = new ValueTriplet<NameAndSignalsTab, String, SignalType>((NameAndSignalsTab) source,
                expSignal.getA(), expSignal.getB());
          } else {
            NameAndSignalsTab a = null;
            if (setAfinal) a = (NameAndSignalsTab) source;
            // Will show the selector later (in TranslationDone).
            dataSource = new ValueTriplet<NameAndSignalsTab, String, SignalType>(a, null, null);
          }
          
          // Open pathway and set experiment to visualize.
          TranslatorPanel<?> tp = visualizePathway(selector.getSelectedPathwayID(), null);
          tp.setData(TPAction.VISUALIZE_DATA.toString(), dataSource);
        }
      }
    };
    
    // Let user chooser
    GUITools.showOkCancelDialogInNewThread(ret, UIManager.getString("OptionPane.titleText"), okAction, null, null);
    selector.autoActivateOkButton(ret);
    
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

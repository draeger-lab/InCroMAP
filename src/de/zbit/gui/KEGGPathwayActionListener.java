/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

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
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import y.view.Graph2D;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.miRNA.miRNA;
import de.zbit.gui.NameAndSignalTabActions.NSAction;
import de.zbit.gui.TranslatorTabActions.TPAction;
import de.zbit.gui.prefs.PathwayVisualizationOptions;
import de.zbit.gui.prefs.SignalOptions;
import de.zbit.integrator.VisualizeDataInPathway;
import de.zbit.kegg.Translator;
import de.zbit.kegg.gui.PathwaySelector;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.kegg.gui.TranslatorUI;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.TranslatorTools;
import de.zbit.util.ValuePair;
import de.zbit.util.ValueTriplet;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.visualization.VisualizeMicroRNAdata;

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
      try {
        visualizeAndColorPathway();
      } catch (Exception e1) {
        GUITools.showErrorMessage(null, e1);
      }
            
    } else if (e.getActionCommand().equals(TPAction.VISUALIZE_DATA.toString()) &&
        source instanceof TranslatorPanel) {
      ValueTriplet<NameAndSignalsTab, String, SignalType>  vt
        = IntegratorGUITools.showSelectExperimentBox(IntegratorUI.getInstance(), null,
            "Please select an observation to color nodes accordingly.");
       if (vt!=null) {
         colorPathway((TranslatorPanel) source,vt.getA(),vt.getB(), vt.getC());
       }
       
    } else if (e.getActionCommand().equals(TPAction.HIGHLIGHT_ENRICHED_GENES.toString()) &&
        source instanceof TranslatorPanel) {
      TranslatorPanel source = (TranslatorPanel) this.source;
      if (source.getData(TPAction.HIGHLIGHT_ENRICHED_GENES.toString())!=null) {
        // Highlight source genes
        Collection<Integer> geneIDs = ((EnrichmentObject<?>)source.getData(TPAction.HIGHLIGHT_ENRICHED_GENES.toString())).getGeneIDsFromGenesInClass();
        hightlightGenes(source, geneIDs);
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
        if (r==0 || source.getData(TPAction.VISUALIZE_DATA.toString())!=null) {
          /* Color Pathway (for fold changes) and write singals to nodes.
           */
          ValueTriplet<NameAndSignalsTab, String, SignalType>  vt=null;
          if ( source.getData(TPAction.VISUALIZE_DATA.toString())!=null) {
            try {
              vt = (ValueTriplet<NameAndSignalsTab, String, SignalType>) source.getData(TPAction.VISUALIZE_DATA.toString());
            } catch (Exception ex) {}
          }
          if (vt==null || vt.getA()==null) {
            // Let the user choose a signal to color the nodes
            IntegratorTab st = null;
            if (this.source instanceof IntegratorTab) {
              st = (IntegratorTab) this.source;
              while (st.getSourceTab()!=null) {
                st = st.getSourceTab();
              }
            }
            vt = IntegratorGUITools.showSelectExperimentBox(IntegratorUI.getInstance(), st,
                "Please select an observation to color nodes accordingly.");
          }
          
          if (vt!=null) {
            colorPathway(source,vt.getA(),vt.getB(), vt.getC());
          } else {
            r = -1; // At least highlight source gene nodes
          }
        } if (r!=0 && source.getData(TPAction.HIGHLIGHT_ENRICHED_GENES.toString())!=null) {
          // Highlight source genes
          Collection<Integer> geneIDs = ((EnrichmentObject<?>)source.getData(TPAction.HIGHLIGHT_ENRICHED_GENES.toString())).getGeneIDsFromGenesInClass();
          hightlightGenes(source, geneIDs);
        }
      }
      
    } else if (e.getActionCommand().equals(TranslatorUI.Action.NEW_PROGRESSBAR.toString())) {
      IntegratorUI.getInstance().getStatusBar().showProgress((AbstractProgressBar)e.getSource());
        
    } else {
      log.warning("Unknown action command " + e.getActionCommand());
    }
  }

  /**
   * Color the pathway in <code>tp</code> according to the experiment
   * described by <code>experimentName</code> and <code>signalType</code>
   * contained in <code>dataSource</code>.
   * 
   * <p>Taking this method instead of {@link #colorPathway(TranslatorPanel, Iterable, String, SignalType)}
   * allows asking the user to annotate miRNA data with targets, if it has not yet been done. 
   * 
   * @param tp pathway to color
   * @param dataSource to take from the given {@link NameAndSignalsTab}
   * @param experimentName name of the observation to color
   * @param signalType signal type of the observation (usually fold change)
   */
  private void colorPathway(final TranslatorPanel tp, final NameAndSignalsTab dataSource, final String experimentName, final SignalType signalType) {
    // @return number of nodes, colored according to the signal, or -1 if an error occured.
    /* This method has the advantage to
     * - Ask the mergeType only once!
     * - Add nodes for miRNAs to visualize
     * And thus should always be preferred to the other colorPathway method.
     */
    
    // Ask the user to set all required options
    if (!SignalOptions.REMEMBER_GENE_CENTER_DECISION.getValue(SBPreferences.getPreferencesFor(SignalOptions.class))) {
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
        
        // Adds the microRNA NODES to the graph and automatically asks
        // the user to annotate targets to his miRNA data if not already done.
        if (miRNA.class.isAssignableFrom(dataSource.getDataContentType())) {
          addMicroRNAs(tp, dataSource);
        }
        
        // Perform visualization
        VisualizeDataInPathway visData = new VisualizeDataInPathway(tp);
        int coloredNodes = visData.visualizeData(dataSource, experimentName,signalType);
        
        // Repaint and hide loading screens
        //tp.repaint();
        //tp.hideTemporaryLoadingBar();
        return coloredNodes;
        } catch (Throwable t) {
          t.printStackTrace();
        }
        
        return -1;
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
          tp.hideTemporaryLoadingBar();
          tp.repaint();
          IntegratorUI.getInstance().updateButtons();
        }
      }
    };
    visData.execute();
    
    return;
  }
  
  /**
   * Adds microRNAs to the given graph
   * @param tp panel with graph
   * @param dataSource microRNA datasource
   */
  @SuppressWarnings("unchecked")
  public static void addMicroRNAs(TranslatorPanel tp, NameAndSignalsTab dataSource) {
    if (!miRNA.class.isAssignableFrom(dataSource.getDataContentType())) {
      log.severe("Can not add miRNA targets when source is " + dataSource.getDataContentType());
      return;
    }
    
    int addedNodes = addMicroRNAs(tp, (Collection<? extends miRNA>) dataSource.getData());
    if (addedNodes==0) {
      // if no nodes have been colored, look if it was due to missing miRNA target annotations
      int a = GUITools.showQuestionMessage(IntegratorUI.getInstance(), "No microRNA had an annotated target within this graph. " +
        "Do you want to (re-)annotate your microRNA data with targets?", IntegratorUI.appName, JOptionPane.YES_NO_OPTION);
      if (a==JOptionPane.YES_OPTION) {
        dataSource.getActions().annotateMiRNAtargets();
        addedNodes = addMicroRNAs(tp, (Collection<? extends miRNA>) dataSource.getData());
      }
    }
  }
  
  /**
   * Adds microRNAs to the given graph.
   * <p> Use preferred {@link #addMicroRNAs(TranslatorPanel, NameAndSignalsTab)}, because it
   * adds missing target annotations to the list. This method here does NOT add targets,
   * if they are missing.
   * @see #addMicroRNAs(TranslatorPanel, NameAndSignalsTab)
   * @param tp panel with graph
   * @param dataSource Collection with {@link miRNA}s that MUST HAVE targets!
   * @return number of nodes created or -1 if an error occurred.
   */
  public static int addMicroRNAs(TranslatorPanel tp, Collection<? extends miRNA> dataSource) {
    if (!NameAndSignals.isMicroRNA(dataSource)) {
      log.severe("Can not add miRNA targets when source is not miRNA.");
      return -1;
    }
    
    VisualizeMicroRNAdata vis = new VisualizeMicroRNAdata((Graph2D) tp.getDocument());
    int addedNodes = vis.addMicroRNAsToGraph((Collection<? extends miRNA>) dataSource);
    if (addedNodes>0) {
      // The "Remove miRNA-nodes" button must be enabled.
      IntegratorUI.getInstance().updateButtons();
    }
    return addedNodes;
  }
  
  
  /**
   * <b>PLEASE USE {@link #colorPathway(TranslatorPanel, NameAndSignalsTab, String, SignalType)}</b><p>
   * Color the pathway in <code>tp</code> according to the experiment
   * described by <code>experimentName</code> and <code>signalType</code>
   * contained in <code>dataSource</code>.
   * 
   * <p>Note: the other method with {@link NameAndSignalsTab} (see reference below)
   * should be preferred. This method does not add any nodes (e.g. for miRNAs!).
   * @see #colorPathway(TranslatorPanel, NameAndSignalsTab, String, SignalType)
   * @param tp pathway to color
   * @param dataSource list with {@link NameAndSignals}
   * @param experimentName name of the observation to color
   * @param signalType signal type of the observation (usually fold change)
   * @return number of nodes, colored according to the signal, or -1 if an error occurred.
   */
  @Deprecated
  private int colorPathway(TranslatorPanel tp, NameAndSignalsTab dataSource, String experimentName, SignalType signalType, MergeType mt) {
    VisualizeDataInPathway tools2 = new VisualizeDataInPathway(tp);
    
    if (tp.getDocument()==null) {
      GUITools.showErrorMessage(null, "Please wait for the graph to load completely.");
      return -1;
    }
    
    // Color nodes
    int coloredNodes = 0;
    if (experimentName!=null && signalType!=null) {
//      coloredNodes = tools2.colorNodesAccordingToSignals(dataSource, mt, experimentName, signalType);
      // Write the signals to the nodes
//    tools2.writeSignalsToNodes(dataSource, experimentName, signalType);
      
      coloredNodes = tools2.visualizeData(dataSource, experimentName, signalType);
    }
    
    tp.repaint();
    return coloredNodes;
  }
  
  
  /**
   * Highlight nodes with given geneIDs.
   * @param source
   * @param geneIDs
   */
  private void hightlightGenes(TranslatorPanel source, Iterable<Integer> geneIDs) {
    SBPreferences prefs = SBPreferences.getPreferencesFor(PathwayVisualizationOptions.class);
    Color colorForUnaffectedNodes = PathwayVisualizationOptions.COLOR_FOR_NO_VALUE.getValue(prefs);
    Color affectedColor = PathwayVisualizationOptions.COLOR_FOR_MAXIMUM_FOLD_CHANGE.getValue(prefs);
    TranslatorTools tools = new TranslatorTools(source);
    tools.highlightGenes(geneIDs,affectedColor, colorForUnaffectedNodes, true);
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
  private TranslatorPanel visualizePathway(String pwId, EnrichmentObject<?> pwo) {
    //Create the translator panel
    TranslatorPanel pwTab = new TranslatorPanel(pwId,Format.GraphML, this);
    String name = pwId;
    if (pwo!=null) {
      pwTab.setData(TPAction.HIGHLIGHT_ENRICHED_GENES.toString(), pwo);
      name = pwo.getName();
    }
    
    String extra = "";
    if (source!=null && (source instanceof IntegratorTab)) {
      extra = " for " + ((IntegratorTab<?>)source).species.getCommonName();
    }
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
    // Preprocessing: Check if ready and get organism.
    String organism = null;
    if (source instanceof IntegratorTab) {
      IntegratorTab<?> source = (IntegratorTab<?>) this.source;
      if (source.getData()==null) return; // Is Ready check
      if (source.getSpecies(false)!=null) 
        organism = source.getSpecies().getKeggAbbr();
    } else if (source instanceof TranslatorPanel) {
      TranslatorPanel source = (TranslatorPanel) this.source;
      if (source.getDocument()==null) return; // Is Ready check
      organism = TranslatorTools.getOrganismKeggAbbrFromGraph((Graph2D) source.getDocument());
    }


    // Create pathway and experiment selectors
    final PathwaySelector selector = new PathwaySelector(Translator.getFunctionManager(),null, organism);
    final JLabeledComponent expSel;
    boolean setA=false;
    if (source instanceof NameAndSignalsTab) {
      NameAndSignals ns = (NameAndSignals)((NameAndSignalsTab)source).getExampleData();
      if (ns.hasSignals()) {
        expSel = IntegratorGUITools.createSelectExperimentBox(ns);
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
    ret.add(selector, BorderLayout.NORTH);
    if (expSel!=null) {
      ret.add(expSel, BorderLayout.SOUTH);
    }

    // Let user chooser
    int val = JOptionPane.showConfirmDialog((Component)source, ret, UIManager.getString("OptionPane.titleText"), JOptionPane.OK_CANCEL_OPTION);
    GUITools.disableOkButton(ret);

    // Evaluate and evetually open new tab.
    if (val==JOptionPane.OK_OPTION && selector.getSelectedPathwayID()!=null) {
      ValueTriplet<NameAndSignalsTab, String, SignalType> dataSource;
      if (expSel!=null) {
        ValuePair<String, SignalType> expSignal = (ValuePair<String, SignalType>) expSel.getSelectedItem();
        dataSource = new ValueTriplet<NameAndSignalsTab, String, SignalType>((NameAndSignalsTab) source,
            expSignal.getA(), expSignal.getB());
      } else {
        NameAndSignalsTab a = null;
        if (setA) a = (NameAndSignalsTab) source;
        // Will show the selector later (in TranslationDone).
        dataSource = new ValueTriplet<NameAndSignalsTab, String, SignalType>(a, null, null);
      }

      // Open pathway and set experiment to visualize.
      TranslatorPanel tp = visualizePathway(selector.getSelectedPathwayID(), null);
      tp.setData(TPAction.VISUALIZE_DATA.toString(), dataSource);
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

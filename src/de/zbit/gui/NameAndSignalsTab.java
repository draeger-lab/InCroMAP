/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.UIManager;

import de.zbit.data.NameAndSignals;
import de.zbit.data.mRNA.mRNA;
import de.zbit.gui.IntegratorUI.Action;
import de.zbit.io.NameAndSignalReader;
import de.zbit.parser.Species;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.FileTools;
import de.zbit.util.Reflect;

/**
 * @author Clemens Wrzodek
 */
public class NameAndSignalsTab extends IntegratorTabWithTable implements PropertyChangeListener {
  private static final long serialVersionUID = -6373461312937415980L;
  public static final transient Logger log = Logger.getLogger(NameAndSignalsTab.class.getName());
  
  /**
   * For intermediate loading operations.
   */
  private AbstractProgressBar intermediateBar;
  
  
  /**
   * Ensures that data is a list, instead of a collection.
   * @param data a {@link List} or any other instance of {@link Iterable} or an array
   * containing the <code>? extends NameAndSignals</code> data.
   */
  public NameAndSignalsTab(IntegratorUI parent, Object data) {
    this(parent, data, null);
  }
  
  /**
   * @see #NameAndSignalsTab(IntegratorUI, Object)
   * @param parent
   * @param data
   * @param species
   */
  public NameAndSignalsTab(IntegratorUI parent, Object data, Species species) {
    super(parent, null, species);

    setData(data);
  }

  /**
   * Creates a new {@link SwingWorker} and starts reading the data in background.
   * @param integratorUI
   * @param nsreader
   * @param path
   */
  public NameAndSignalsTab(final IntegratorUI parent, final NameAndSignalReader<? extends NameAndSignals> nsreader, final String inFile) {
    this(parent, null);
    
    // Create intermediate loading tab
    intermediateBar = IntegratorTab.generateLoadingPanel(this, "Reading file " + FileTools.getFilename(inFile));
    nsreader.setProgressBar(intermediateBar);
    
    // Create worker
    final NameAndSignalsTab thiss = this;
    SwingWorker<Collection<? extends NameAndSignals>, Void> worker = new SwingWorker<Collection<? extends NameAndSignals>, Void>() {
      @Override
      protected Collection<? extends NameAndSignals> doInBackground() throws Exception {
        Collection<? extends NameAndSignals> col = nsreader.importWithGUI(parent, inFile);
        thiss.species = nsreader.getSpecies();
        return col; // col==null if cancel button pressed
      }
    };
    worker.addPropertyChangeListener(this);
    worker.execute();
    
  }
  
  public NameAndSignalsTab(final IntegratorUI parent, final SwingWorker<Collection<? extends NameAndSignals>, Void> nsworker, String loadingMessage) {
    this(parent, nsworker, loadingMessage, null);
  }
  
  /**
   * @param integratorUI
   * @param worker
   * @param string
   * @param species
   */
  public NameAndSignalsTab(IntegratorUI parent, SwingWorker<Collection<? extends NameAndSignals>, Void> nsworker, String loadingMessage, Species species) {
    this(parent, null, species);
    
    // Create intermediate loading tab
    intermediateBar = IntegratorTab.generateLoadingPanel(this, loadingMessage);
    // Give workers the chance to display a progress.
    Reflect.invokeIfContains(nsworker, "setProgressBar", AbstractProgressBar.class, intermediateBar);
    
    nsworker.addPropertyChangeListener(this);
    nsworker.execute();
  }

  /**
   * Creates a {@link SwingWorker} to read the given <code>genes</code> with the
   * given <code>nsreader</code>.
   * @see #NameAndSignalsTab(IntegratorUI, SwingWorker, String, Species)
   * @param integratorUI
   * @param nsreader
   * @param genes
   */
  public NameAndSignalsTab(IntegratorUI parent, final NameAndSignalReader<mRNA> nsreader, final String[] genes, Species spec) {
    this(parent, 
      
      new SwingWorker<Collection<? extends NameAndSignals>, Void>() {
        @Override
        protected Collection<? extends NameAndSignals> doInBackground() throws Exception {
          Collection<? extends NameAndSignals> col = nsreader.read(genes);
          return col; // col==null if cancel button pressed
        }
      },
    
    "Reading data...",
    spec);
  }

  /**
   * Change this (intermediate) panel to a real {@link NameAndSignalsTab} or
   * close it if the worker did fail.
   * @param worker
   */
  @SuppressWarnings("rawtypes")
  private void swingWorkerDone(SwingWorker worker) {
    Object data=null;
    if (intermediateBar!=null) intermediateBar.finished();
    parent.getStatusBar().reset();
    try {
      data = worker.get();
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error during worker execution.", e);
    }
    if (data==null || worker.isCancelled()) {
      parent.closeTab(this);
    } else {
      setData(data);
    }
    updateButtons(parent.getJMenuBar(), parent.getJToolBar());
  }
  

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void setData(Object data) {
    if (data==null) return;
    
    // Convert Iterables and arrays to list and store the list as internal data structure.
    if (data instanceof List) {
      this.data = (List<? extends NameAndSignals>) data;
    } else if (data instanceof Iterable) { // e.g. collections or sets
      List dataNew = new ArrayList();
      for (Object object : ((Iterable)data)) {
        dataNew.add(object);
      }
      this.data = (List<? extends NameAndSignals>) dataNew;
    } else if (data.getClass().isArray()) {
      List dataNew = new ArrayList();
      for (int i=0; i<Array.getLength(data); i++) {
        dataNew.add(Array.get(data, i));
      }
      this.data = (List<? extends NameAndSignals>) dataNew;
    } else {
      log.log(Level.SEVERE, "Implement representation for " + data.getClass().getName());
    }
    
    init();
  }
  
  public void createJToolBarItems(JToolBar bar) {
    if (bar.getName().equals(getClass().getSimpleName())) return; //Already done.
    bar.removeAll();
    bar.setName(getClass().getSimpleName());
    
    JButton showPathway = GUITools.createJButton(EventHandler.create(ActionListener.class, this, "showAndColorPathway"),
      Action.VISUALIZE_IN_PATHWAY, UIManager.getIcon("ICON_GEAR_16"));
    
    
    // TODO: Add these options also as additionalFileMenuEntries.
    
    bar.add(showPathway);
    
    GUITools.setOpaqueForAllElements(bar, false);    
  }
  
  public void showAndColorPathway() {
    if (data!=null) {
      /* TODO:
       * - if organism is unknown, show organism box
       * - Show pathway selection box (and accept also ,-separated kegg ids!)
       * - IntegratorGUITools.showSelectExperimentBox(ui, initialSelection, BOOLEAN RESTRICTED_TO_INITIAL_SELECTION)
       *   that returns immedeately if only 1 FC is available, else, lets the user chosse the fc
       *   
       * Add (one or more) tabs for every pathway and !!color immediately!!.
       */
      
    }
  }

  /* (non-Javadoc)
   * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
   */
  @SuppressWarnings("rawtypes")
  public void propertyChange(PropertyChangeEvent evt) {
    // Checks if intermediate swing workers are done.
    String name = evt.getPropertyName();
    if ((evt.getSource() instanceof SwingWorker) && name.equals("state")) {
      if (evt.getNewValue().equals(StateValue.DONE)) {
        swingWorkerDone((SwingWorker) evt.getSource());
      }
    }
  }
  
}

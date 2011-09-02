/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui.tabs;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenuBar;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import de.zbit.data.NameAndSignals;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.miRNA.miRNAandTarget;
import de.zbit.data.miRNA.miRNAtargets;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.actions.NameAndSignalTabActions;
import de.zbit.integrator.ReaderCache;
import de.zbit.io.NameAndSignalReader;
import de.zbit.parser.Species;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.FileTools;
import de.zbit.util.Reflect;

/**
 * A special tab for the {@link IntegratorUI} for
 * {@link Collections} of {@link NameAndSignals}.
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
   * Actions for the {@link JToolBar} to be performed on this data.
   */
  private NameAndSignalTabActions actions = new NameAndSignalTabActions(this);
  
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
    // Data is intentionally null in constructor.
    setData(data);
  }
  
  /**
   * @return the internal data.
   */
  @SuppressWarnings("unchecked")
  public List<? extends NameAndSignals> getData() {
    return (List<? extends NameAndSignals>) super.getData();
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
        nsreader.setSecondaryProgressBar(IntegratorUI.getInstance().getStatusBar().showProgress());
        Collection<? extends NameAndSignals> col = nsreader.importWithGUI(parent, inFile, ReaderCache.getCache());
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
          nsreader.setSecondaryProgressBar(IntegratorUI.getInstance().getStatusBar().showProgress());
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
      // This would couse to place buttons for this tab on the bar,
      // even if this tab is not currently visible!
      //updateButtons(parent.getJMenuBar(), parent.getJToolBar());
      parent.updateButtons();
    }
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
      
    } else if (data.getClass().equals(miRNAtargets.class)) {
      this.data = miRNAandTarget.getList((miRNAtargets) data);
      
    } else {
      log.log(Level.SEVERE, "Implement representation for " + data.getClass().getName());
    }
    
    init();
  }
  
  public void createJToolBarItems(JToolBar bar) {
    actions.createJToolBarItems(bar);
    
    // Not here just name, because button actions are linked to this instance!
    /*String uniqueName = getClass().getSimpleName() + hashCode();
    if (bar.getName().equals(uniqueName)) return;
    bar.removeAll();
    bar.setName(uniqueName);
    
    KEGGPathwayActionListener al2 = new KEGGPathwayActionListener(this);
    JButton showPathway = GUITools.createJButton(al2,
        Action.VISUALIZE_IN_PATHWAY, UIManager.getIcon("ICON_GEAR_16"));
    // TODO: Create a temporary batch button.
    
    
    bar.add(showPathway);
    
    GUITools.setOpaqueForAllElements(bar, false);  */  
  }
  
  @Override
  public void updateButtons(JMenuBar menuBar, JToolBar toolbar) {
    super.updateButtons(menuBar, toolbar);
    actions.updateToolbarButtons(toolbar);
  }
  
  /**
   * @return {@link NameAndSignalTabActions}.
   */
  public NameAndSignalTabActions getActions() {
    return actions;
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

/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import de.zbit.data.NameAndSignals;
import de.zbit.io.NameAndSignalReader;
import de.zbit.parser.Species;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.FileTools;
import de.zbit.util.Reflect;

/**
 * @author Clemens Wrzodek
 */
public class NameAndSignalsTab extends IntegratorTab<List<? extends NameAndSignals>> implements PropertyChangeListener, Comparable<NameAndSignalsTab> {
  private static final long serialVersionUID = -6373461312937415980L;
  public static final transient Logger log = Logger.getLogger(NameAndSignalsTab.class.getName());
  
  /**
   * The {@link JTable} holding visualized Names and Signals.
   */
  private JTable table;
  
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
    } catch (Exception e) {}
    if (data==null || worker.isCancelled()) {
      parent.closeTab(this);
    } else {
      setData(data);
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
    } else {
      log.log(Level.SEVERE, "Implement representation for " + data.getClass().getName());
    }
    
    init();
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.gui.IntegratorTab#getVisualization()
   */
  @Override
  public JComponent getVisualization() {
    if (data==null) return null;
    // Also adds the enrichment right mouse menu
    table = TableResultTableModel.buildJTable(this);
    
    return table;
  }

  /* (non-Javadoc)
   * @see de.zbit.gui.IntegratorTab#getSelectedIndices()
   */
  @Override
  public int[] getSelectedIndices() {
    // Get selected items
    int[] selRows = table.getSelectedRows();
    
    // Map to view rows (account for sorted tables!)
    for (int i=0; i<selRows.length; i++) {
      selRows[i] = table.convertRowIndexToModel(selRows[i]);
    }
    
    return selRows;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.IntegratorTab#getObjectAt(int)
   */
  @Override
  public Object getObjectAt(int i) {
    return data.get(i);
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

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(NameAndSignalsTab o) {
    // Just to make this class useable with ValuePair and ValueTriplet.
    return toString().compareTo(o.toString());
  }
  
}

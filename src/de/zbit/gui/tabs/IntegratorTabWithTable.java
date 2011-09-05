/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui.tabs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JTable;
import javax.swing.JToolBar;

import de.zbit.data.TableResult;
import de.zbit.gui.BaseFrame.BaseAction;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.customcomponents.TableResultTableModel;
import de.zbit.io.CSVWriter;
import de.zbit.io.SBFileFilter;
import de.zbit.parser.Species;
import de.zbit.util.ValuePairUncomparable;

/**
 * A generic Integrator tab with a table on top.
 * @author Clemens Wrzodek
 */
public class IntegratorTabWithTable extends IntegratorTab<Collection<? extends TableResult>> {
  private static final long serialVersionUID = -8876183417528573116L;
  public static final transient Logger log = Logger.getLogger(IntegratorTab.class.getName());
  
  /**
   * The {@link JTable} holding visualized Names and Signals.
   */
  protected JTable table=null;
  
  /**
   * Listeners that must be informed, if the current table changes.
   */
  private Set<IntegratorTabWithTable> tableChangeListeners=null;
  
  /**
   * Having an iterator, pointing at an indice of #data is
   * much faster, if data is no List (overwriting methods sometimes
   * also use a non-list here).
   */
  private ValuePairUncomparable<Iterator<? extends TableResult>, Integer> currentDataIterator=null;

  /**
   * @param parent
   * @param data
   */
  public IntegratorTabWithTable(IntegratorUI parent, List<? extends TableResult> data) {
    super(parent, data);
  }
  
  public IntegratorTabWithTable(IntegratorUI parent, List<? extends TableResult> data, Species species) {
    super(parent, data, species);
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.gui.IntegratorTab#saveToFile()
   */
  @Override
  public File saveToFile() {
    File f = GUITools.showSaveFileChooser(this, IntegratorUI.saveDir, SBFileFilter.createTSVFileFilter());
    if (f==null) return null;
    
    try {
      //CSVwriteableIO.write(getData(), f.getAbsolutePath());
      CSVWriter w = new CSVWriter();
      w.write(table, f);
      GUITools.showMessage("Saved table successfully to \"" + f.getPath() + "\".", IntegratorUI.appName);
      return f;
    } catch (Throwable e) {
      GUITools.showErrorMessage(this, e);
    }
    return null;
  }

  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrameTab#updateButtons(javax.swing.JMenuBar, javax.swing.JToolBar)
   */
  @Override
  public void updateButtons(JMenuBar menuBar, JToolBar toolbar) {
    // Update the toolbar.
    if (toolbar!=null) {
      createJToolBarItems(toolbar);
    }
    
    // Enable and disable items
    if (data!=null) {
      GUITools.setEnabled(true, menuBar, BaseAction.FILE_SAVE, BaseAction.FILE_CLOSE);
    } else {
      // Reading/Analyzing is still in progress.
      // If analyzing failed, tab will be closed automatically!
      GUITools.setEnabled(false, menuBar, BaseAction.FILE_SAVE, BaseAction.FILE_CLOSE);
    }
  }
  
  public void createJToolBarItems(JToolBar bar) {
    if (bar.getName().equals(getClass().getSimpleName())) return; //Already done.
    bar.setName(getClass().getSimpleName());
    createJToolBarItems(bar, true);
    GUITools.setOpaqueForAllElements(bar, false);
  }
  
  public void createJToolBarItems(JToolBar bar, boolean clearExistingToolbar) {
    if (clearExistingToolbar) bar.removeAll();
    //XXX: Place buttons here in overriding functions.
    // Overrider also updateButtons() with call to super() and enable /
    // disable buttons on toolbar.
  }
  

  /* (non-Javadoc)
   * @see de.zbit.gui.IntegratorTab#getVisualization()
   */
  @Override
  public JComponent getVisualization() {
    // null check is in createTable().
    //if (data==null) return null;
    
    if (table==null) {
      createTable();
    }
    
    return table;
  }

  /* (non-Javadoc)
   * @see de.zbit.gui.IntegratorTab#getObjectAt(int)
   */
  @Override
  public Object getObjectAt(int i) {
    if (data instanceof RandomAccess && data instanceof List) {
      return ((List<? extends TableResult>)data).get(i);
    } else {
      // Memorize an internal iterator (can only go forward)
      if (currentDataIterator==null || currentDataIterator.getB()>=i) {
        currentDataIterator = new ValuePairUncomparable<Iterator<? extends TableResult>, Integer>(data.iterator(), 0);
      }
      // Go to current element
      Iterator<? extends TableResult> it = currentDataIterator.getA();
      Integer index = currentDataIterator.getB();
      Object ret = null;
      while (it.hasNext()) {
        ret = it.next();
        index++;
        if (index==i) break;
      }
      // Store current iterator position and return object
      currentDataIterator.setB(index);
      if (index==i) return ret; else return null;
    }
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
  
  /**
   * Converts the given indices to the model and returns the actual underlying items.
   * @param selectedIndices
   * @return
   */
  public List<?> getSelectedItems(List<Integer> selectedIndices) {
    // Get selected rows
    if (selectedIndices==null) return null;
    
    List<Object> geneList = new ArrayList<Object>(selectedIndices.size());
    for (int i=0; i<selectedIndices.size(); i++) {
      geneList.add(getObjectAt(table.convertRowIndexToModel(selectedIndices.get(i))));
    }
    
    return geneList;
  }

  protected void createTable() {
    if (data==null) return;
    
    // Also adds the enrichment right mouse menu
    table = TableResultTableModel.buildJTable(this);
  }

  /**
   * Creates a new table for the given data.
   */
  public void rebuildTable() {
    createTable();
    super.init();
    fireTableChangeListeners();
  }
  
  /**
   * Invokes the {@link #rebuildTable()} method on all 
   * tabs in {@link #tableChangeListeners}.
   */
  private void fireTableChangeListeners() {
    /* If you wonder why this "might not work":
     * If data get's gene-centered, objects are COPIES! */
    if (tableChangeListeners == null) return;
    for (IntegratorTabWithTable tab : tableChangeListeners) {
      // Call rebuildTable() on listeners, avoid endless-loops.
      Set<IntegratorTabWithTable> otherListeners = tab.getTableChangeListeners();
      if (otherListeners==null || !(otherListeners.contains(this))) {
        tab.rebuildTable();
        tab.repaint();
      }
    }
  }

  /**
   * @return all tabs that listen to changes on this table.
   * May return null!
   */
  private Set<IntegratorTabWithTable> getTableChangeListeners() {
    return tableChangeListeners;
  }

  /**
   * If {@link #rebuildTable()} is invoked, on all <code>nsTab</code>s added
   * with this method, their {@link #rebuildTable()} is invoked, too.
   * <p>You must be careful not to create endless loops with this method!
   * @param nsTab
   */
  public void addTableChangeListener(IntegratorTabWithTable nsTab) {
    if (nsTab.equals(this)) return; // do not accept yourself
    if (tableChangeListeners == null) tableChangeListeners = new HashSet<IntegratorTabWithTable>();
    tableChangeListeners.add(nsTab);
  }
}

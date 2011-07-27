/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JTable;
import javax.swing.JToolBar;

import de.zbit.data.TableResult;
import de.zbit.gui.BaseFrame.BaseAction;
import de.zbit.io.CSVWriter;
import de.zbit.io.SBFileFilter;
import de.zbit.parser.Species;

/**
 * A generic Integrator tab with a table on top.
 * @author Clemens Wrzodek
 */
public class IntegratorTabWithTable extends IntegratorTab<List<? extends TableResult>> {
  private static final long serialVersionUID = -8876183417528573116L;
  public static final transient Logger log = Logger.getLogger(IntegratorTab.class.getName());
  
  /**
   * The {@link JTable} holding visualized Names and Signals.
   */
  private JTable table=null;

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
    if (data==null) return null;
    
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
    return data.get(i);
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

  private void createTable() {
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
  }
}

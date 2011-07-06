/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import de.zbit.data.TableResult;
import de.zbit.gui.BaseFrame.BaseAction;
import de.zbit.gui.IntegratorUI.Action;
import de.zbit.io.CSVWriter;
import de.zbit.io.SBFileFilter;
import de.zbit.parser.Species;

/**
 * A generic Integrator tab with a table on top.
 * @author Clemens Wrzodek
 */
public class IntegratorTabWithTable extends IntegratorTab<List<? extends TableResult>> implements Comparable<IntegratorTabWithTable> {
  private static final long serialVersionUID = -8876183417528573116L;
  public static final transient Logger log = Logger.getLogger(IntegratorTab.class.getName());
  
  /**
   * The {@link JTable} holding visualized Names and Signals.
   */
  private JTable table;

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
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(IntegratorTabWithTable o) {
    // Just to make this class useable with ValuePair and ValueTriplet.
    return toString().compareTo(o.toString());
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
    if (data!=null) {
      GUITools.setEnabled(true, menuBar, BaseAction.FILE_SAVE, BaseAction.FILE_CLOSE);
    } else {
      // Reading/Analyzing is still in progress.
      // If analysing failed, tab will be closed automatically!
      GUITools.setEnabled(false, menuBar, BaseAction.FILE_SAVE, BaseAction.FILE_CLOSE);
    }
  }
  
  private void createJToolBarItems(JToolBar bar) {
    if (bar.getName().equals(getClass().getSimpleName())) return; //Already done.
    if (true) return; // TODO: REMOVE THIS
    bar.removeAll();
    bar.setName(getClass().getSimpleName());
    
    /* TODO:
     * if (mRNA)
     * - Show gene symbols as names
     * - Pair with miRNA
     * 
     * if (miRNA)
     * - Annotate targets
     * - Pair with mRNA
     * 
     * Always
     * - Perform Enrichment => List...
     * - Search table
     * - Visualize in pathway
     * 
     * Eventuell
     * [- Add pathways] column with pathways for gene/ target
     * 
     */
    
    // Create several loadData buttons
    JPopupMenu load = new JPopupMenu("Load data");
    JMenuItem lmRNA = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openMRNAfile"),
      Action.LOAD_MRNA, UIManager.getIcon("ICON_OPEN_16"));
    load.add(lmRNA);
    JMenuItem lmiRNA = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openMiRNAfile"),
      Action.LOAD_MICRO_RNA, UIManager.getIcon("ICON_OPEN_16"));
    load.add(lmiRNA);
    JDropDownButton loadDataButton = new JDropDownButton("Load data", UIManager.getIcon("ICON_OPEN_16"), load);
    
    JButton genelist = GUITools.createJButton(EventHandler.create(ActionListener.class, this, "showInputGenelistDialog"),
      Action.INPUT_GENELIST, UIManager.getIcon("ICON_OPEN_16"));
    
    JButton miRNAtargets = GUITools.createJButton(EventHandler.create(ActionListener.class, this, "showMicroRNAtargets"),
      Action.SHOW_MICRO_RNA_TARGETS, UIManager.getIcon("ICON_GEAR_16"));
    
    JButton newPathway = GUITools.createJButton(EventHandler.create(ActionListener.class, this, "openPathwayTab"),
      Action.NEW_PATHWAY, UIManager.getIcon("ICON_GEAR_16"));
    
    // TODO: Add these options also as additionalFileMenuEntries.
    
    // TODO: UpdateButtons [öfter aufrufen] und in interface toolbar mitgeben.
    
    /* XXX: EnrichmentObject ideen
     * - Cutoff für > pValue, qValue, list ratio
     * - Change statistical correction
     * Generell: [-Suche]
     */
    
    bar.add(loadDataButton);
    bar.add(genelist);
    bar.add(miRNAtargets);
    bar.add(newPathway);
    
    GUITools.setOpaqueForAllElements(bar, false);
    
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
}

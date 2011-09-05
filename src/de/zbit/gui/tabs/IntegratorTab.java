/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui.tabs;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;

import de.zbit.data.mRNA.mRNA;
import de.zbit.data.miRNA.miRNA;
import de.zbit.gui.BaseFrameTab;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.ProgressBarSwing;
import de.zbit.gui.VerticalLayout;
import de.zbit.gui.actions.listeners.EnrichmentActionListener;
import de.zbit.parser.Species;
import de.zbit.util.AbstractProgressBar;

/**
 * An abstract class for all tabs, that are displayed
 * within the {@link IntegratorUI} application.
 * @author Clemens Wrzodek
 * @param <T> type of the data that is visualized in this tab.
 */
public abstract class IntegratorTab <T> extends JScrollPane implements BaseFrameTab, Comparable<IntegratorTab<?>> {
  private static final long serialVersionUID = 653444691629282605L;
  public static final transient Logger log = Logger.getLogger(IntegratorTab.class.getName());
  
  /**
   * The species for the data displayed in this tab
   */
  Species species = null;
  
  /**
   * The parent {@link IntegratorUI}
   */
  IntegratorUI parent;
  
  /**
   * The data associated with this tab.
   */
  protected T data;
  
  /**
   * If this is the result of data from another tab, this pointer
   * should refer to the other tab.
   */
  IntegratorTab<?> sourceTab=null;
  
  /**
   * Stores the data and initialized visualization.
   * @param parent
   * @param data
   */
  public IntegratorTab(IntegratorUI parent, T data) {
    this(parent, data, null);
  }
  
  /**
   * Stores the data and initialized visualization.
   * @param parent
   * @param data
   * @param species
   */
  public IntegratorTab(IntegratorUI parent, T data, Species species) {
    super();
    this.parent = parent;
    this.species = species;
    this.data = data;
    init();
  }
  
  /**
   * Puts the visualization on this {@link JComponent}.
   */
  protected void init() {
    JComponent jc = getVisualization();
    if (jc!=null) {
      setViewportView(jc);
      // When resizing, try to optimize table size.
      if (jc instanceof JTable) {
        applyTableConstraints((JTable)jc, this);
      }
    }
  }
  
  /**
   * Resize the table eventually according to the scrollpane size.
   * @param table
   * @param scrollPane
   */
  public static void applyTableConstraints(final JTable table, final JScrollPane scrollPane) {
    final int defaultWidth;
    if (table.getColumnModel().getColumnCount()>0) 
      defaultWidth = table.getColumnModel().getColumn(0).getWidth();
    else
      defaultWidth = 75;
    
    scrollPane.addComponentListener(new ComponentListener() {
      public void componentHidden(ComponentEvent e) {}
      public void componentMoved(ComponentEvent e) {}
      public void componentShown(ComponentEvent e) {}
      
      public void componentResized(ComponentEvent e) {
        if (table.getColumnCount()<5 && scrollPane.getWidth()>table.getColumnCount()*defaultWidth) {
          table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        } else {
          table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        }
      }
    });
  }
  
  
  /**
   * @return the sourceTab
   */
  public IntegratorTab<?> getSourceTab() {
    return sourceTab;
  }

  /**
   * @param sourceTab the sourceTab to set
   */
  public void setSourceTab(IntegratorTab<?> sourceTab) {
    this.sourceTab = sourceTab;
  }

  /**
   * @see #getSpecies(boolean)
   * @return {@link #getSpecies(true)}
   */
  public Species getSpecies() {
    return getSpecies(true);
  }
  
  /**
   * @param showDialogIfUnknown if true and {@link #species}==null, the user
   * will be asked to select a species.
   * @return the species or null if the user pressed cancel or the species
   * is unknown
   */
  public Species getSpecies(boolean showDialogIfUnknown) {
    if (species==null && showDialogIfUnknown) {
      species = IntegratorUITools.showOrganismSelectorDialog(parent);
    }
    return species;
  }
  
  /**
   * Get the data type of the stored data. If this tab
   * is based on a collection or array, this will automtically
   * return the type of the collection/array/etc.
   * <p>E.g., {@link mRNA} or {@link miRNA},...
   * @return
   */
  public Class<?> getDataContentType() {
    Object d = getExampleData();
    if (d==null) return Object.class;
    else return d.getClass();
  }
  
  /**
   * Get an example object of the data contained in this class.
   * If the data is an array or a list (or any other instance of
   * {@link Iterable}), this will automatically return an object
   * in the array or list.
   * @return
   */
  @SuppressWarnings("rawtypes")
  public Object getExampleData() {
    if (data==null) return null;
    Object ret = null;
    if (data instanceof Iterable || Iterable.class.isAssignableFrom(data.getClass())) {
      ret = (((Iterable)data).iterator().hasNext()?((Iterable)data).iterator().next():null);
    } else if (data.getClass().isArray()) {
      if (Array.getLength(data)>0) ret = Array.get(data, 0);
    } else {
      ret = data; // return the element directly.
    }
    return ret;
  }
  
  /**
   * Return a visualization for this data (e.g., a JTable).
   * @return
   */
  public abstract JComponent getVisualization();
  
  /**
   * Modify the <code>bar</code> to fit the tabs content.
   * @param bar
   */
  public abstract void createJToolBarItems(JToolBar bar);

  /**
   * If this object is visualized, e.g., by a JTable, this method should
   * return the currently selected objects (real data rows, e.g. instances of
   * mRNA, not the {@link JTable} rows or indices).
   * 
   * <p>Object must be known in
   * {@link EnrichmentActionListener#actionPerformed(java.awt.event.ActionEvent)}
   * and thus should in doubt be geneIDs, encoded by Integers.
   * 
   * @return
   */
  public List<?> getSelectedItems() {
    // Get selected rows
    int[] selRows = getSelectedIndices();
    if (selRows==null) return null;
    
    List<Object> geneList = new ArrayList<Object>(selRows.length);  
    for (int i=0; i<selRows.length; i++) {
      geneList.add(getObjectAt(selRows[i]));
    }
    
    return geneList;
  }
  
  /**
   * Return object at the given position (if data is a collection or
   * an array or anything else containing multiple other selectable objects).
   * @param i
   * @return
   */
  public abstract Object getObjectAt(int i);

  /**
   * If this object is visualized, e.g., by a JTable, this method should
   * return the currently selected indices. The indices must refer
   * to the {@link #data} of this tab.
   * @return
   */
  public abstract int[] getSelectedIndices();

  
  /**
   * Create and display a temporary loading panel with the given message and a
   * progress bar.
   * @param parent may be null. Else: all elements will be placed on this container
   * @return the ProgressBar of the container.
   */
  public static AbstractProgressBar generateLoadingPanel(Container parent, String loadingText) {
    Dimension panelSize = new Dimension(400, 75);
    
    // Create the panel
    JPanel panel = new JPanel(new VerticalLayout());
    panel.setPreferredSize(panelSize);
    panel.setOpaque(false);
    
    // Create the label and progressBar
    loadingText = (loadingText!=null && loadingText.length()>0)?loadingText:"Please wait...";
    JLabel jl = new JLabel(loadingText);
    log.info(loadingText);
    //Font font = new java.awt.Font("Tahoma", Font.PLAIN, 12);
    //jl.setFont(font);
    
    JProgressBar prog = new JProgressBar();
    prog.setPreferredSize(new Dimension(panelSize.width - 20,
      panelSize.height / 4));
    panel.add(jl);//, BorderLayout.NORTH);
    panel.add(prog);//, BorderLayout.CENTER);
    
    if (panel instanceof JComponent) {
      GUITools.setOpaqueForAllElements((JComponent) panel, false);
    }
    
    if (parent!=null) {
      if (parent instanceof JScrollPane) {
        ((JScrollPane)parent).setViewportView(panel);
      } else {
        parent.add(panel);
      }
    } else {
      // Display the panel in an jFrame
      JDialog f = new JDialog();
      f.setTitle(IntegratorUI.appName);
      f.setSize(panel.getPreferredSize());
      f.setContentPane(panel);
      f.setPreferredSize(panel.getPreferredSize());
      f.setLocationRelativeTo(null);
      f.setVisible(true);
      f.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    }
    
    // Make progressBar
    ProgressBarSwing pb = new ProgressBarSwing(prog);
    
    // Inform others of this action
    /*ActionEvent newBar = new ActionEvent(pb, JOptionPane.DEFAULT_OPTION, "NEW_PROGRESSBAR");
    if (parent instanceof IntegratorTab) {
      ((TranslatorPanel)parent).fireActionEvent(newBar);
    } else if (parent instanceof TranslatorUI) {
      ((TranslatorUI)parent).actionPerformed(newBar);
    }*/
    
    return  pb;
  }
  
  /**
   * Returns the actual underlying UI.
   */
  public IntegratorUI getIntegratorUI() {
    return this.parent!=null?this.parent:IntegratorUI.getInstance();
  }
  
  /**
   * @return the internal data.
   */
  public T getData() {
    return data;
  }
  
  /**
   * @return true if data is associated with this tab.
   */
  public boolean isReady() {
    return data!=null;
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(IntegratorTab<?> o) {
    // Just to make this class usable with ValuePair and ValueTriplet.
    return toString().compareTo(o.toString());
  }
  
  /**
   * @return the name of the tab in the parent {@link #getIntegratorUI()}.
   */
  public String getTabName() {
    return getName();
//    int i = getIntegratorUI().getTabIndex(this);
//    if (i>=0 && i<getIntegratorUI().getTabbedPane().getComponentCount()) {
//      return getIntegratorUI().getTabbedPane().getTitleAt(i);
//    } else {
//      return "Unknown";
//    }
  }
  
}

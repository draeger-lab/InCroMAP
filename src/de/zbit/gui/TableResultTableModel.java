/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.Dimension;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.TableResult;
import de.zbit.util.JTableTools;
import de.zbit.util.ScientificNumberRenderer;

/**
 * A {@link TableModel} that can be used to visualize a {@link TableResult} class as {@link JTable}.
 * Especially useful for {@link NameAndSignals}
 * 
 * @author Clemens Wrzodek
 */
public class TableResultTableModel<T extends TableResult> extends AbstractTableModel {
  private static final long serialVersionUID = -6542792109889449114L;
  
  /**
   * The {@link NameAndSignals} that should be represented by this {@link TableModel}.
   */
  private List<T> ns;
  
  /**
   * Should the first col be a row index?
   */
  private boolean includeRowIndex;
  
  public TableResultTableModel(List<T> ns) {
    this(ns, true);
  }
  
  public TableResultTableModel(List<T> ns, boolean includeRowIndex) {
    this.ns = ns;
    this.includeRowIndex = includeRowIndex;
  }
  
  /**
   * @return the underlying {@link NameAndSignals} {@link List}.
   */
  public List<T> getNameAndSignalsList() {
    return ns;
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  public int getColumnCount() {
    // 1 is the row index.
    int offset = includeRowIndex?1:0;
    if (ns.size()<1) return offset;
    return ns.get(0).getColumnCount()+offset;
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getRowCount()
   */
  public int getRowCount() {
    return ns.size();
  }

  /**
   * @return
   */
  public boolean isRowIndexIncluded() {
    return includeRowIndex;
  }
  
  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (includeRowIndex) {
      if (columnIndex==0) return (rowIndex);
      columnIndex = columnIndex-1;
    }
    
    Object o = ns.get(rowIndex).getObjectAtColumn(columnIndex);
    if (o instanceof Signal) {
      // Experiment name and signal type is already in header!
      return ((Signal)o).getSignal();
    } else {
      return o;
    }
  }
  
  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
   */
  @Override
  public Class<?> getColumnClass(int columnIndex) {
    if (includeRowIndex) {
      if (columnIndex==0) return Integer.class;
      columnIndex = columnIndex-1;
    }
    if (ns.size()<1) return super.getColumnClass(columnIndex);
    Object o = ns.get(0).getObjectAtColumn(columnIndex);
    Class<?> c = o!=null?o.getClass():null;
    if (c==null) return super.getColumnClass(columnIndex);
    return c;
  }
  
  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    if (includeRowIndex) {
      if (column==0) return "#";
      column = column-1;
    }
    if (ns.size()<1) return super.getColumnName(column);
    String s = ns.get(0).getColumnName(column);
    if (s==null || s.length()<1) {
      return super.getColumnName(column);
    }
    return s;
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static <T extends TableResult> JTable buildJTable(NameAndSignalsTab tab) {
    // Build table on scroll pane
    JTable jc = buildJTable(new TableResultTableModel(tab.getData()));
    
    // Add enrichment capabilities
    EnrichmentActionListener al = new EnrichmentActionListener(tab);
    JPopupMenu popUp = IntegratorGUITools.createEnrichmentPopup(al);
    IntegratorGUITools.addRightMousePopup(jc, popUp);
    
    // Other data type dependent capabilities
    if (tab.getDataContentType().equals(EnrichmentObject.class)) {
      if (((EnrichmentObject)tab.getExampleData()).getIdentifier().toString().startsWith("path:")) {
        // It's a KEGG Pathway enrichment.
        KEGGPathwayActionListener al2 = new KEGGPathwayActionListener(tab);
        IntegratorGUITools.addRightMousePopup(jc, IntegratorGUITools.createKeggPathwayPopup(al2, popUp));
      }
    }
    
    return jc;
  }
  
  public static <T extends TableResult> JTable buildJTable(TableResultTableModel<T> model) {
    // TODO: Implement a filtered table (a model that automatically adds
    // a JTextField row (similar to online marcar db filter) below headers
    final JTable table = new JTable(model); // new JComponentTableModel()
    
    // Set an appropriate editor for the expected column and type selectors
    /*int maxHeadRow = (isATypeSelectorRequired()?2:1);
    for (int row=0; row<maxHeadRow; row++) {
      for (int col=0; col<dataNew[row].length; col++) {
        Object cur = dataNew[row][col];
        
        TableCellEditor cellEditor;
        if (cur instanceof JCheckBox) {
          cellEditor = new DefaultCellEditor((JCheckBox)cur);
        }else if (cur instanceof JComboBox) {
          cellEditor = new DefaultCellEditor((JComboBox)cur);
        }else if (cur instanceof JTextField) {
          cellEditor = new DefaultCellEditor((JTextField)cur);
        } else {
          cellEditor = table.getCellEditor(0,col);
        }
        
        table.setCellEditor(row, col, cellEditor);      
      }
    }
    
    // Draw JComponents inside the JTable
    JComponentTableRenderer rend = new JComponentTableRenderer();
    for (int i=0; i<table.getColumnCount(); i++) {
      table.getColumnModel().getColumn(i).setCellRenderer(rend);
    }*/
    
    // Set additional attributes
    table.setPreferredScrollableViewportSize(new Dimension(500, 100));
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    //for (int i=0; i<maxHeadRow+1;i++)
      //table.setRowHeight(i, (int) (table.getRowHeight(i)*1.3));
    
    // Disallow dragging columns
    table.getTableHeader().setReorderingAllowed(true);
    
    // Resize columns to a reasonable, unique width
    if (table.getColumnModel().getColumnCount()>0)  {
      int width = table.getColumnModel().getColumn(0).getWidth();
      width = Math.max(width, 125);
      for (int i=0; i<table.getColumnModel().getColumnCount(); i++)
        table.getColumnModel().getColumn(i).setPreferredWidth(width);
      if (model.isRowIndexIncluded()) {
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
      }
    }
    
    // Add sorting capabilities
    TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(model);
    table.setRowSorter(sorter);
    
    // Make doubles scientific
    TableCellRenderer rend = new ScientificNumberRenderer();
    table.setDefaultRenderer(Double.class, rend);
    table.setDefaultRenderer(Float.class, rend);
    //table.setDefaultRenderer(RowIndex.class, RowIndex.getRowHeaderRenderer(table));
    
    // Allow searching
    JTableTools.setQuickSearch(table);
    
    return table;
  }

}

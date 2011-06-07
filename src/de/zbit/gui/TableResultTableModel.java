/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.util.List;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import de.zbit.data.NameAndSignals;
import de.zbit.data.TableResult;

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
  
  public TableResultTableModel(List<T> ns) {
    this.ns = ns;
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  public int getColumnCount() {
    if (ns.size()<1) return 0;
    return ns.get(0).getColumnCount();
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getRowCount()
   */
  public int getRowCount() {
    return ns.size();
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  public Object getValueAt(int rowIndex, int columnIndex) {
    return ns.get(rowIndex).toArray(columnIndex);
  }
  
  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
   */
  @Override
  public Class<?> getColumnClass(int columnIndex) {
    if (ns.size()<1) return super.getColumnClass(columnIndex);
    Class<?> c = ns.get(0).getColumnClass(columnIndex);
    if (c==null) return super.getColumnClass(columnIndex);
    return c;
  }
  
  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    if (ns.size()<1) return super.getColumnName(column);
    String s = ns.get(0).getColumnName(column);
    if (s==null || s.length()<1) {
      return super.getColumnName(column);
    }
    return s;
  }
  
}

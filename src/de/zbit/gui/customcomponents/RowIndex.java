/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui.customcomponents;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * Holds a simple integer called "index".
 * @author Clemens Wrzodek
 */
public class RowIndex {
  
  private final int index;
  
  public RowIndex(int index) {
    this.index = index;
  }

  /**
   * @return the index
   */
  public int getIndex() {
    return index;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return Integer.toString(index);
  }
  
  /**
   * @param table
   * @return a renderer that lets cells look like headers.
   */
  public static RowHeaderRenderer getRowHeaderRenderer(JTable table) {
    return new RowHeaderRenderer(table);
  }
  
  /**
   * A renderer that lets cells look like headers.
   * @author Clemens Wrzodek
   */
  static class RowHeaderRenderer extends JLabel implements TableCellRenderer {
    private static final long serialVersionUID = 4997446866270681200L;

    RowHeaderRenderer(JTable table) {
      JTableHeader header = table.getTableHeader();
      setForeground(header.getForeground());
      setBackground(header.getBackground());
      setFont(header.getFont());
      
      setBorder(UIManager.getBorder("TableHeader.cellBorder"));
      setHorizontalAlignment(SwingConstants.CENTER);
      setOpaque(true);
    }
    
    /* (non-Javadoc)
     * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
      boolean isSelected, boolean hasFocus, int row, int column) {
      setText((value == null) ? "" : value.toString());
      return this;
    }
  }
  
}

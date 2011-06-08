/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.data;

import javax.swing.table.TableModel;

/**
 * All classes that implement this interface should
 * be easily visualizable and writable as Table.
 * 
 * <p>It seeks compatibility to {@link TableModel}, while
 * not being a TableModel. Thus, classes implementing this
 * interface can easily get compatibility to {@link TableModel}.
 * 
 * @author Clemens Wrzodek
 */
public interface TableResult {
  
  /**
   * @return the number of columns
   */
  public int getColumnCount();
  
  /**
   * The value at the given columnIndex. This is and can also be used
   * to get the {@link Class} of each column!
   * @param colIndex
   * @return
   */
  public Object getObjectAtColumn(int colIndex);
  
  /**
   * A name for the given columnIndex.
   * @param column
   * @return
   */
  public String getColumnName(int column);
  
  
}

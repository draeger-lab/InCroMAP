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
   * @return the column contents
   */
  public Object[] toArray();
  
  /**
   * The value at the given columnIndex
   * @param colIndex
   * @return
   */
  public Object toArray(int colIndex);
  
  /**
   * The class of the {@link Object} at the given index.
   * Must be descriptive for {@link #toArray()}.
   * @param columnIndex
   * @return
   */
  public Class<?> getColumnClass(int columnIndex);
  
  /**
   * A name for the given columnIndex.
   * @param column
   * @return
   */
  public String getColumnName(int column);
  
  
}

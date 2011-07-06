/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.util.logging.Logger;

import javax.swing.table.DefaultTableCellRenderer;

import de.zbit.data.mRNA.mRNA;

/**
 * @author Clemens Wrzodek
 */
public class IterableRenderer extends DefaultTableCellRenderer {
  private static final long serialVersionUID = -3236639799789752757L;
  public static final transient Logger log = Logger.getLogger(IterableRenderer.class.getName());
  
  public IterableRenderer() {
    super();
  }
  
  /* (non-Javadoc)
   * @see javax.swing.table.DefaultTableCellRenderer#setValue(java.lang.Object)
   */
  @Override
  protected void setValue(Object value) {
    if (value==null) setText("");
    else {
      if (value instanceof Iterable || Iterable.class.isAssignableFrom(value.getClass())) {
        StringBuffer buff = new StringBuffer();
        for (Object v:((Iterable<?>)value)) {
          if (buff.length()>0) buff.append(", ");
          if (v instanceof mRNA) {
            buff.append(((mRNA)v).getName());
          } else {
            log.severe("Plese implement renderer for " + v.getClass());
            buff.append(v.toString());
          }
        }
        setText(buff.toString());
      } else {
        setText(value.toString());
      }
    }
  }
  
}

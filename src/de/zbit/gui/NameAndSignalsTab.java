/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JTable;

import de.zbit.data.NameAndSignals;

/**
 * @author Clemens Wrzodek
 */
public class NameAndSignalsTab extends IntegratorTab<List<? extends NameAndSignals>> {
  public static final transient Logger log = Logger.getLogger(NameAndSignalsTab.class.getName());

  /**
   * Ensures that data is a list, instead of a collection.
   * @param data a {@link List} or any other instance of {@link Iterable} or an array
   * containing the <code>? extends NameAndSignals</code> data.
   */
  @SuppressWarnings("unchecked")
  public NameAndSignalsTab(Object data) {
    super(null);
    
    // Convert Iterables and arrays to list and store the list as internal data structure.
    if (data instanceof List) {
      this.data = (List<? extends NameAndSignals>) data;
    } else if (data instanceof Iterable) { // e.g. collections or sets
      List dataNew = new ArrayList();
      for (Object object : ((Iterable)data)) {
        dataNew.add(object);
      }
      this.data = (List<? extends NameAndSignals>) dataNew;
    } else if (data.getClass().isArray()) {
      List dataNew = new ArrayList();
      for (int i=0; i<Array.getLength(data); i++) {
        dataNew.add(Array.get(data, i));
      }
      this.data = (List<? extends NameAndSignals>) dataNew;
    } else {
      log.log(Level.SEVERE, "Implement representation for " + data.getClass().getName());
    }
  }
  
  
  public JComponent getVisualization() {
      TableResultTableModel<? extends NameAndSignals> table = new TableResultTableModel(data);
      visualization = TableResultTableModel.buildJTable(table);
    } else {
      System.err.println("*********IMPLEMENT VISUALIZATION FOR " + example + " (" + example.getClass() + ")");
      visualization=new JTable();
    }
    synchronized (this) {
      tabbedPane.addTab(name, visualization);
      tabbedPane.setSelectedComponent(visualization);
      
      // 3. Add to list
      tabContent.add(list);
    }
  }

  
  
  
}

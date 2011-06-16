/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.lang.reflect.Array;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import de.zbit.io.CSVwriteableIO;
import de.zbit.parser.Species;

/**
 * An abstract class for all tabs, that are displayed
 * within the {@link IntegratorUI} application.
 * @author Clemens Wrzodek
 * @param <T> type of the data that is visualized in this tab.
 */
public abstract class IntegratorTab <T>{
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
  T data;
  
  /**
   * Simply stores the data.
   * @param data
   */
  public IntegratorTab(T data) {
    super();
    this.data = data;
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
      JLabeledComponent organismSelector = IntegratorGUITools.getOrganismSelector();
      int ret = GUITools.showAsDialog(parent, organismSelector, "Please select your species", true);
      if (ret == JOptionPane.OK_OPTION) {
        species = (Species) organismSelector.getSelectedItem();
      }
    }
    return species;
  }
  
  /**
   * Get the data type of the stored data. If this tab
   * is based on a collection or array, this will automtically
   * return the type of the collection/array/etc.
   * @return
   */
  public Class<?> getDataContentType() {
    return getExampleData().getClass();
  }
  
  /**
   * Get an example object of the data contained in this class.
   * If the data is an array or a list (or any other instance of
   * {@link Iterable}), this will automatically return an object
   * in the array or list.
   * @return
   */
  @SuppressWarnings("unchecked")
  public Object getExampleData() {
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
  
  
}

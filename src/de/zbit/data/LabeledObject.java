/**
 * @author Clemens Wrzodek
 */
package de.zbit.data;

import java.io.Serializable;

/**
 * This class is a very generic override of the {@link #toString()} method.
 * With it, you can return any string and associate this string with an object.
 * @author Clemens Wrzodek
 */
public class LabeledObject<T> implements Serializable, Comparable<LabeledObject<?>> {
  private static final long serialVersionUID = 2091985992659785789L;

  /**
   * The label to display in the {@link #toString()} method.
   */
  private String label;
  
  /**
   * The object represented by the label.
   */
  private T object;
  
  public LabeledObject (String label, T object) {
    super();
    this.label = label;
    this.object = object;
  }
  
  
  
  /**
   * @return the label
   */
  public String getLabel() {
    return label;
  }



  /**
   * @param label the label to set
   */
  public void setLabel(String label) {
    this.label = label;
  }



  /**
   * @return the object
   */
  public T getObject() {
    return object;
  }



  /**
   * @param object the object to set
   */
  public void setObject(T object) {
    this.object = object;
  }



  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return label;
  }



  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(LabeledObject<?> o) {
    return label.compareTo(o.getLabel());
  }
  
}

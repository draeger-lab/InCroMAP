/**
 * @author Clemens Wrzodek
 */
package de.zbit.data;

/**
 * This class is a very generic override of the {@link #toString()} method.
 * With it, you can return any string and associate this string with an object.
 * @author Clemens Wrzodek
 */
public class LabeledObject {
  
  /**
   * The label to display in the {@link #toString()} method.
   */
  private String label;
  
  /**
   * The object represented by the label.
   */
  private Object object;
  
  public LabeledObject (String label, Object object) {
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
  public Object getObject() {
    return object;
  }



  /**
   * @param object the object to set
   */
  public void setObject(Object object) {
    this.object = object;
  }



  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return label;
  }
  
}

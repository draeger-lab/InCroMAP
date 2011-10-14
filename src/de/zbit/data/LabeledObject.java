/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/Integrator> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.data;

import java.io.Serializable;

import javax.swing.ListModel;

import de.zbit.gui.tabs.NameAndSignalsTab;

/**
 * This class is a very generic override of the {@link #toString()} method.
 * With it, you can return any string and associate this string with an object.
 * @author Clemens Wrzodek
 * @version $Rev$
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



  /**
   * Returns the index of <code>object</code> inside
   * {@link #getObject()} in <code>arr</code>.
   * @param arr array of {@link LabeledObject}s
   * @param object to search for (ignoring the label)
   * @return index of <code>object</code> in <code>arr</code>.
   */
  public static <T> int getIndexOfObject(LabeledObject<T>[] arr,
      T object) {
    for (int i=0; i<arr.length; i++) {
      if (arr[i].getObject().equals(object)) return i;
    }
    return -1;
  }
  
  /**
   * Does the same as {@link #getIndexOfObject(LabeledObject[], NameAndSignalsTab)}
   * but casts each element of <code>arr</code> to
   * {@link LabeledObject}.
   * @param arr
   * @param object
   * @return
   */
  @SuppressWarnings("unchecked")
  public static <T> int getIndexOfObject(Object[] arr,
      T object) {
    for (int i=0; i<arr.length; i++) {
      if ((arr[i] instanceof LabeledObject) &&
         (((LabeledObject<T>)arr[i]).getObject().equals(object))) {
        return i;
      }
    }
    return -1;
  }



  /**
   * The same as {@link #getIndexOfObject(Object[], Object)}, but with
   * a list model.
   * @param model
   * @param object
   * @return
   */
  @SuppressWarnings("unchecked")
  public static <T> int getIndexOfObject(ListModel model,
    T object) {
    for (int i=0; i<model.getSize(); i++) {
      Object o = model.getElementAt(i);
      if ((o instanceof LabeledObject) &&
         (((LabeledObject<T>)o).getObject().equals(object))) {
        return i;
      }
    }
    return -1;
  }


}

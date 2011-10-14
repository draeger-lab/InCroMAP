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
 * @version $Rev$
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
  
  /**
   * @return the object that creates the current row. Usually, this returns <pre>this</pre>.
   */
  public Object getRowObject();
  
  
}

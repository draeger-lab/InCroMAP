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

/**
 * Holds an arbitrary object and a number (e.g., score).
 * The method {@link #compareTo(Object)} compares the score only.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
@SuppressWarnings("rawtypes")
public class ObjectAndScore<T> implements Serializable, Comparable {
  private static final long serialVersionUID = -3949304611622723593L;
  
  T object;
  double score;
  
  public ObjectAndScore (T object, double score) {
    this.object = object;
    this.score = score;
  }
  

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object o) {
    if (o instanceof Number) {
      return Double.compare(score, ((Number)o).doubleValue());
    } else if (o instanceof ObjectAndScore) {
      return Double.compare(score, ((ObjectAndScore)o).score);
    }
    return -1;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ObjectAndScore) {
      ObjectAndScore o = (ObjectAndScore)obj;
      if (o.object.equals(object) && Double.compare(score, o.score)==0) {
        return true;
      }
    }
    return false;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return (int) (object.hashCode()+score);
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "[Object: " + object.toString() + " Score: "+ score+"]";
  }
  
}

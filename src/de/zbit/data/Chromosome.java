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

import java.util.regex.Pattern;

/**
 * Interface for all objects that have an associated chromosome.
 * Please use {@link ChromosomeTools} to implement the given
 * methods.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public interface Chromosome {

  /**
   * The key to use in the {@link #addData(String, Object)} map to add
   * the corresponding Chromosome.
   */
  public final static String chromosome_key = "Chromosome";
  
  /**
   * Means Chromosome has not been set or element has no associated Chromosome.
   */
  public final static Byte default_Chromosome_byte = 0;
  
  /**
   * String representation of {@link #default_Chromosome_byte}
   */
  public final static String default_Chromosome_string = "UNKNOWN";
  
  /**
   * A regular expression that identifies a chromosome string
   * in various notations.
   * <p>Note: group 2 is the actual chromosome number or X,Y,M.
   */
  public final static Pattern chromosome_regex = Pattern.compile("(chromosome|CHROMOSOME|chr|CHR)?:?([XYM]|[1-9][0-9]?)");
  
  /**
   * Set the corresponding Chromosome.
   * @param chromosome
   */
  public void setChromosome(String chromosome);
  
//  /**
//   * Set the corresponding Chromosome.
//   * @param chromosome
//   */
//  public void setChromosome(Byte chromosome);
  
  /**
   * @return associated Chromosome.
   */
  public String getChromosome();
  
//  /**
//   * @return associated Chromosome.
//   */
//  public Byte getChromosomeAsByteRepresentation();

}

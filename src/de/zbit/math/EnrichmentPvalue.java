/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
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
package de.zbit.math;

/**
 * This is a generic interface for pValue calculations, based on four values:
 * <ol><li>Genome Size
 * <li>Gene List size
 * <li>Total Genes in the current enrichment class (e.g., pathway)
 * <li>Number of Genes from the input gene list, that are in the current
 * enrichment class (e.g., pathway)</ol>
 * <b>The first two are fixed for each instance and thus, should be declared in the
 * constructor</b>, whereas the later two are given to the method {@link #getPvalue(long, long)}.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public interface EnrichmentPvalue {
  
  /**
   * @return size of the background genome
   */
  public int getGenomeSize();
  
  /**
   * @return size of the input gene list
   */
  public int getGeneListSize();
  
  /**
   * Calculates a pValue for an enrichment significance (e.g., gene set enrichments
   * in pathways).
   * @param t Total number of genes in the current pathway
   * @param r Number of genes from the input set that are in the current pathway.
   * @return
   */
  public double getPvalue(int t, int r);
  
}
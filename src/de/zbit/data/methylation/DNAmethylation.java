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
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.data.methylation;

import java.util.logging.Logger;

import de.zbit.data.NSwithProbesAndRegion;
import de.zbit.data.id.GeneID;

/**
 * A generic class to hold DNA methylation data with annotated genes
 * (gene-based, with geneID), current probe position and Signals. 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class DNAmethylation extends NSwithProbesAndRegion {
  private static final long serialVersionUID = -6002300790004775432L;
  public static final transient Logger log = Logger.getLogger(DNAmethylation.class.getName());
  
  public DNAmethylation(String geneName) {
    this (geneName, GeneID.default_geneID);
  }
  
  /**
   * @param geneName
   * @param geneID
   */
  public DNAmethylation(String geneName, Integer geneID) {
    // Yes, we could have included the probe name.
    // But since it is not used anywhere, we removed it.
    super(null, geneName, geneID);
    unsetProbeName();
  }
  
  /**
   * @param geneName
   * @param geneID
   * @param start
   * @param end
   * @param chromosome
   */
  public DNAmethylation(String geneName, Integer geneID, Integer start,
    Integer end, String chromosome) {
    this(geneName,geneID);
    if (start!=null) setStart(start>-1?start:end);
    if (end!=null) {
      try {
        setEnd(end);
      } catch (Exception e) {
        // Only if we set end prior to start. Since we
        // Don't do that, we can ignore this error.
      }
    }
    if (chromosome!=null) setChromosome(chromosome);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    DNAmethylation nm = new DNAmethylation(name, getID());
    return super.clone(nm);
  }
    
}

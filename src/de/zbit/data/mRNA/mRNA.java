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
package de.zbit.data.mRNA;

import de.zbit.data.NSwithProbes;
import de.zbit.data.Signal;
import de.zbit.data.id.GeneID;


/**
 * A generic class to hold {@link mRNA} probes with {@link Signal}s and {@link GeneID}s.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class mRNA extends NSwithProbes {
  private static final long serialVersionUID = -5897878964584853247L;
  
  /**
   * Initialize a new mRNA with the given name.
   * @param name
   */
  public mRNA(String name) {
    this(name, default_geneID);
  }

  /**
   * @param probeName name of the probe
   * @param geneName the gene name
   * @param geneID Corresponding NCBI Gene ID (Entrez).
   */
  public mRNA(String probeName, String geneName, int geneID) {
    super (probeName, geneName, geneID);
  }
  
  /**
   * @param name the gene name
   * @param geneID Corresponding NCBI Gene ID (Entrez).
   */
  public mRNA(String name, int geneID) {
    this(null, name, geneID);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    mRNA nm = new mRNA(name, getGeneID());
    return super.clone(nm);
  }


}

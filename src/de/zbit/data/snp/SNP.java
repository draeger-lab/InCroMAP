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
package de.zbit.data.snp;

import de.zbit.data.GeneID;
import de.zbit.data.NSwithProbes;
import de.zbit.data.Signal;

/**
 * A generic class to hold a {@link SNP} with {@link Signal}s and containing {@link GeneID}.
 * @author Finja Büchel
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class SNP extends NSwithProbes {
  private static final long serialVersionUID = 7939630941567479573L;

  public SNP(String dbSNPid) {
    this (dbSNPid, GeneID.default_geneID);
  }
  
  public SNP(String dbSNPid, Integer geneID) {
    super(null, dbSNPid, geneID);
    unsetProbeName();
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    SNP nm = new SNP(name, getGeneID());
    return super.clone(nm);
  }
  
}

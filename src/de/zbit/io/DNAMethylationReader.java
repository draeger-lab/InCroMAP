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
package de.zbit.io;

import de.zbit.data.methylation.DNAmethylation;


/**
 * Basic reader to read {@link DNAmethylation} data.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class DNAMethylationReader extends AbstractGeneAndRegionBasedNSreader<DNAmethylation> {

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneAndRegionBasedNSreader#createObject(java.lang.String, java.lang.Integer, java.lang.Integer, java.lang.Integer, java.lang.String, java.lang.String[])
   */
  @Override
  protected DNAmethylation createObject(String name, Integer geneID,
    Integer start, Integer end, String chromosome, String[] line) {
    
    // Set name and geneID
    DNAmethylation m = new DNAmethylation(name, geneID, start, end, chromosome);
    
    return m;
  }

}

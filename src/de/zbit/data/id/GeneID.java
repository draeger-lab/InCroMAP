/*
 * $Id: GeneID.java 132 2012-03-26 13:40:15Z wrzodek $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/Integrator/trunk/src/de/zbit/data/GeneID.java $
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
package de.zbit.data.id;

import java.util.Collection;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;

/**
 * Interface for object that can be assigned a NCBI Entrez Gene ID.
 * 
 * <p>If implementing this interface, please also override
 * the {@link NameAndSignals#merge(Collection, NameAndSignals, MergeType)}
 * method and avoid taking mean or other stupid things of geneIDs.
 * Instead, if data is not gene-centric anymore, return {@value #default_geneID}
 * 
 * @author Clemens Wrzodek
 * @version $Rev: 132 $
 */
public interface GeneID {
  
  /**
   * The key to use in the {@link #addData(String, Object)} map to add
   * the corresponding NCBI Gene ID (Entrez).
   */
  public final static String gene_id_key = "Gene_ID";
  
  /**
   * Means gene id has not been set or mRNA has no associated gene id.
   */
  public final static Integer default_geneID = -1;
  
  /**
   * Set the corresponding NCBI Gene ID.
   * @param geneID
   */
  public void setGeneID(int geneID);
  
  /**
   * @return associated NCBI Gene ID.
   */
  public int getGeneID();
  
}
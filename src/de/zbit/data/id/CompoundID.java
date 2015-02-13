/*
  * $Id:  CompoundID.java 14:21:17 rosenbaum $
  * $URL: CompoundID.java $
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

package de.zbit.data.id;

import java.util.Collection;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;
import de.zbit.mapper.MappingUtils.IdentifierClass;
import de.zbit.mapper.MappingUtils.IdentifierType;

/**
 * Interface for object that can be assigned an InChIKey (a specific compound/metabolite)
 * 
 * <p>If implementing this interface, please also override
 * the {@link NameAndSignals#merge(Collection, NameAndSignals, MergeType)}
 * method and avoid taking mean or other stupid things of InChIKey s.
 * Instead, if data is not gene-centric anymore, return {@link GenericID#getDefaultID()}
 * 
 * @author Lars Rosenbaum
 * @version $Rev$
 */

public interface CompoundID extends GenericID<String> {
  
  /**
   * The key to use in the {@link #addData(String, Object)} map to add
   * the InChIKey.
   */
  public final static IdentifierType compound_id_key = IdentifierType.InChIKey;
  
  /**
   * The class of entitity the ID represents, here: Compound
   */
  public final static IdentifierClass compound_id_class = IdentifierClass.Compound;
  
  /**
   * Means compound id has not been set
   */
  //Lars: Very problematic to always do checks >0 in Code... should be: equals(default_geneID)
  public final static String default_compoundID = "Unknown";
  
}

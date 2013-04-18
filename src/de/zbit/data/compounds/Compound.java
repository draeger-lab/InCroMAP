/*
  * $Id:  Compound.java 11:45:11 rosenbaum $
  * $URL: Compound.java $
  * ---------------------------------------------------------------------
  * This file is part of Integrator, a program integratively analyze
  * heterogeneous microarray datasets. This includes enrichment-analysis,
  * pathway-based visualization as well as creating special tabular
  * views and many other features. Please visit the project homepage at
  * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
  * obtain the latest version of Integrator.
  *
  * Copyright (C) 2011-2013 by the University of Tuebingen, Germany.
  *
  * Integrator is free software; you can redistribute it and/or
  * modify it under the terms of the GNU Lesser General Public License
  * as published by the Free Software Foundation. A copy of the license
  * agreement is provided in the file named "LICENSE.txt" included with
  * this software distribution and also available online as
  * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
  * ---------------------------------------------------------------------
  */

package de.zbit.data.compounds;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;
import de.zbit.mapper.MappingUtils.IdentifierType;

/**
 * A class to hold metabolomics data with {@link Signal}s and {@link CompoundID}s.
 * @author Lars Rosenbaum
 * @version $Rev$
 */

public class Compound extends NameAndSignals  implements CompoundID {

  private static final long serialVersionUID = 4433984082554945790L;
	public static final transient Logger log = Logger.getLogger(Compound.class.getName());
  
  /**
   * @param compoundName
   */
  private Compound(String compoundName, Integer compoundID) {
    super(compoundName);

    if (compoundID==null) compoundID = default_compoundID;
    setCompoundID(compoundID);
  }
  
	
	@Override
	protected <T extends NameAndSignals> void merge(Collection<T> source,
			T target, MergeType m) {
		
		// Merge private variables to target (=> CompoundID)
    // probeName is in superMethod merged.
    Set<Integer> compoundIDs = new HashSet<Integer>();
    
    for (T o : source) {
     Compound mi = (Compound)o;
      compoundIDs.add(mi.getCompoundID());
    }
    
    // Set compound id, if same or unset if they differ
    if (compoundIDs.size()==1) {
      ((CompoundID)target).setCompoundID(compoundIDs.iterator().next());
    } else {
      // Reset gene-id
      ((CompoundID)target).setCompoundID(default_compoundID);
    }
	}

	@Override
	public String getUniqueLabel() {
		return this.getCommonName();
	}
	
	/* (non-Javadoc)
   * @see de.zbit.data.compounds.CompoundID#setCompoundID(int)
   */
	@Override
  public void setCompoundID(int compoundID) {
	    super.addData(compound_id_key, new Integer(compoundID));
  }

	@Override
  public int getCompoundID() {
		Integer i = (Integer) super.getData(compound_id_key);
    return i==null?default_compoundID:i;
  }
	
	/**
   * @return associated common compound name. Only uses stored name,
   * does not call any mapper or such.
   */
  public String getCommonName() {
    Object s = super.getData(IdentifierType.CompoundSynonym.toString());
    return s==null?getName():s.toString();
  }
  
  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(Object o) {
    // Compare gene symbol
    int r = super.compareTo(o);
    
    // Compare compound ID
    if (o instanceof Compound) {
      Compound ot = (Compound) o;
      if (r==0) r = getCompoundID()-ot.getCompoundID();
    }
    
    return r;
  }
  
  /**
   * Clones all attributes of this instance to the template.
   * @param <T>
   * @param template
  * @return 
   */
  protected <T extends Compound> T clone(T template) {
    super.cloneAbstractFields(template, this);
    return template;
  }
}

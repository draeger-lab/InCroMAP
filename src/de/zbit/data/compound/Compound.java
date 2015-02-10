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
package de.zbit.data.compound;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.id.CompoundID;
import de.zbit.gui.IntegratorUITools;
import de.zbit.mapper.MappingUtils.IdentifierClass;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.mapper.compounds.InChIKey2CompoundNameMapper;

/**
 * An implementation for {@link NameAndSignals} that correspond to compounds.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class Compound extends NameAndSignals implements CompoundID {
  private static final long serialVersionUID = 3951361162465167486L;

  /**
   * 
   * @param name
   */
  public Compound(String name) {
    this (name, null);
  }

  /**
   * @param name
   * @param compoundID is an InChIKey
   */
  public Compound(String name, String compoundID) {
    super(name);
    if (compoundID==null) compoundID = getDefaultID();
    setID(compoundID);
  }
  
  /**
   * @return associated compound symbol. Only uses stored symbol,
   * does not call any mapper or such.
   */
  public String getSymbol() {
    Object s = super.getData(IdentifierType.CompoundName.toString());
    return s==null?getName():s.toString();
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(Object o) {
    // Compare name
    int r = super.compareTo(o);
    
    // Compare ID
    if (o instanceof Compound) {
      Compound ot = (Compound) o;
      if (r==0) r = getID().compareTo(ot.getID());
      //---
    }
    
    return r;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#hashCode()
   */
  @Override
  public int hashCode() {
    return super.hashCode() + getID().hashCode();
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#additionalDataToString(java.lang.String, java.lang.Object)
   */
  @Override
  protected Object additionalDataToString(String key, Object value) {
    Object o = super.additionalDataToString(key, value);
    if (key.equals(getIDType())) {
      if (value.equals(getDefaultID())) {
        return "Unknown";
      } else {
        return value;
      }
    }
    return o;
  }

  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#merge(java.util.Collection, de.zbit.data.NameAndSignals, de.zbit.data.Signal.MergeType)
   */
  @Override
  protected <T extends NameAndSignals> void merge(Collection<T> source,
    T target, MergeType m) {
    /* Note: By default, if multiple NS are merged to a single instance,
     * the mean/max/min, etc. of multiple numeric values is assinged to
     * the merged one. However, this is nonse if we merge the compound ID!
     * 
     * Thus, we implement this method to change the beehaviour.
    */

    // Merge private variables to target (=> CompoundID)
    Set<String> compoundIDs = new HashSet<String>();
//    Set<Boolean> wasGeneCentered = new HashSet<Boolean>();
    for (T o : source) {
      Compound mi = (Compound)o;
      compoundIDs.add(mi.getID());
//      wasGeneCentered.add(mi.wasGeneCentered);
    }
    
    // Set the "wasGeneCentered" variable
//    ((Compound)target).wasGeneCentered = ((Compound)target).isGeneCentered() || 
//      (wasGeneCentered.size()==1 && wasGeneCentered.contains(Boolean.TRUE));
    
    // Set compound id, if same or unset if they differ
    if (compoundIDs.size()==1) {
      ((Compound)target).setID(compoundIDs.iterator().next());
    } else {
      // Reset compound-id
      ((Compound)target).setID(getDefaultID());
//      if (((Compound)target).isGeneCentered()) {
//        ((Compound)target).wasGeneCentered = true;
//        // Cannot ensure that gene is centered. Better unset flag!
//        ((Compound)target).setGeneCentered(false);
//      }
    }
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
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getUniqueLabel()
   */
  @Override
  public String getUniqueLabel() {
    // TODO Return any name that is unique and descriptive for this instance.
    return getSymbol();
  }
  

  /**
   * Convert {@link #getCompoundID()}s to nice symbols.
   * @param data
   * @throws Exception
   */
  public static void convertInstancesToSymbols(Iterable<? extends NameAndSignals> data) throws Exception {
    log.config("Loading 2CompoundSymbol mapping...");
    InChIKey2CompoundNameMapper mapper = IntegratorUITools.get2CompoundNameMapping();
    for (NameAndSignals m: data) {
      if (m instanceof CompoundID) {
        
        // Reviewer request of AppNote: Preserve originall identifier...
        if (m.getName()!=null && m.getData("Original name")==null) {
           m.addData("Original name", m.getName());
        }
        
        // Map compound id to symbol
        if (!((CompoundID) m).getID().equals(CompoundID.default_compoundID)) {
          String symbol = mapper.map(((CompoundID) m).getID());
          if (symbol!=null && symbol.length()>0) {
            m.setDisplayName(symbol);
          }
        }
      } else {
        log.warning("Can not annotate compound symbols for " + m.getClass());
        return;
      }
    }
    log.config("Converted InChIKeys to compound symbols.");
  }

	/* (non-Javadoc)
	 * @see de.zbit.data.id.GenericID#getIDClass()
	 */
  @Override
  public IdentifierClass getIDClass() {
	  return CompoundID.compound_id_class;
  }

	/* (non-Javadoc)
	 * @see de.zbit.data.id.GenericID#getIDType()
	 */
  @Override
  public IdentifierType getIDType() {
	  return CompoundID.compound_id_key;
  }

	/* (non-Javadoc)
	 * @see de.zbit.data.id.GenericID#getDefaultID()
	 */
  @Override
  public String getDefaultID() {
  	return CompoundID.default_compoundID;
  }

	/* (non-Javadoc)
	 * @see de.zbit.data.id.GenericID#setID(java.lang.Object)
	 */
  @Override
  public void setID(String id) {
  	super.addData(compound_id_key.toString(), id);
  }

	/* (non-Javadoc)
	 * @see de.zbit.data.id.GenericID#getID()
	 */
  @Override
  public String getID() {
  	String s = (String) super.getData(getIDType().toString());
    return s==null?getDefaultID():s;
  }
}

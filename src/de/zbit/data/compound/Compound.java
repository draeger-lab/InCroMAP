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
import java.util.logging.Level;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.id.CompoundID;
import de.zbit.gui.IntegratorUITools;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.mapper.compounds.CompoundID2CommonNameMapper;
import de.zbit.util.StringUtil;

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
   * @param compoundID as hmdbID
   */
  public Compound(String name, Integer compoundID) {
    super(name);
    if (compoundID==null) compoundID = default_CompoundID;
    setCompoundID(compoundID);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.compound.CompoundID#setCompoundID(int)
   */
  @Override
  public void setCompoundID(int hmdbID) {
    super.addData(compound_id_key, new Integer(hmdbID));
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.compound.CompoundID#setCompoundID(java.lang.String)
   */
  @Override
  public void setCompoundID(String hmdbID) {
    // Parse from string such as "HMDB00001"
    try {
      setCompoundID(Integer.parseInt(hmdbID.substring(4)));
    } catch (Exception e) {
      log.log(Level.WARNING, "Could not parse HMDB identifier number.", e);
    }
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.compound.CompoundID#getCompoundID()
   */
  @Override
  public int getCompoundID() {
    Integer i = (Integer) super.getData(compound_id_key);
    return i==null?default_CompoundID:i;
  }
  
  /**
   * @return associated compound symbol. Only uses stored symbol,
   * does not call any mapper or such.
   */
  public String getSymbol() {
    Object s = super.getData(IdentifierType.CommonName.toString());
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
      if (r==0) r = getCompoundID()-ot.getCompoundID();
      //---
    }
    
    return r;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#hashCode()
   */
  @Override
  public int hashCode() {
    return super.hashCode() + getCompoundID();
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#additionalDataToString(java.lang.String, java.lang.Object)
   */
  @Override
  protected Object additionalDataToString(String key, Object value) {
    Object o = super.additionalDataToString(key, value);
    if (key.equals(compound_id_key)) {
      // Format: "1" => "HMDB00001"
      if (value.equals(default_CompoundID)) {
        return "Unknown";
      } else {
        return toHMDBString(value);
      }
    }
    return o;
  }

  /**
   * @param value a {@link CompoundID} as integer or numeric string.
   * @return "HMDB000010" or similar things.
   */
  public static String toHMDBString(Object value) {
    String valueString = value.toString(); // 1 => "1"
    return "HMDB" + StringUtil.replicateCharacter('0', 5-valueString.length()) + valueString;
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
    Set<Integer> compoundIDs = new HashSet<Integer>();
//    Set<Boolean> wasGeneCentered = new HashSet<Boolean>();
    for (T o : source) {
      Compound mi = (Compound)o;
      compoundIDs.add(mi.getCompoundID());
//      wasGeneCentered.add(mi.wasGeneCentered);
    }
    
    // Set the "wasGeneCentered" variable
//    ((Compound)target).wasGeneCentered = ((Compound)target).isGeneCentered() || 
//      (wasGeneCentered.size()==1 && wasGeneCentered.contains(Boolean.TRUE));
    
    // Set compound id, if same or unset if they differ
    if (compoundIDs.size()==1) {
      ((Compound)target).setCompoundID(compoundIDs.iterator().next());
    } else {
      // Reset compound-id
      ((Compound)target).setCompoundID(default_CompoundID);
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
    log.config("Loading CompoundSymbol mapping...");
    CompoundID2CommonNameMapper mapper = IntegratorUITools.get2CommonNameMapping();
    for (NameAndSignals m: data) {
      if (m instanceof CompoundID) {
        
        // Reviewer request of AppNote: Preserve originall identifier...
        if (m.getName()!=null && m.getData("Original name")==null) {
           m.addData("Original name", m.getName());
        }
        
        // Map compound id to symbol
        if (((CompoundID) m).getCompoundID()>0) {
          String symbol = mapper.map(((CompoundID) m).getCompoundID());
          if (symbol!=null && symbol.length()>0) {
            m.setDisplayName(symbol);
          }
        }
      } else {
        log.warning("Can not annotate compound symbols for " + m.getClass());
        return;
      }
    }
    log.config("Converted CompoundIDs to compound symbols.");
  }
  
}

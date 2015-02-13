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
package de.zbit.data.protein;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.id.GeneID;
import de.zbit.mapper.MappingUtils.IdentifierClass;
import de.zbit.mapper.MappingUtils.IdentifierType;

/**
 * A generic class to hold protein modification
 * expression data with {@link Signal}s and {@link GeneID}s.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class ProteinModificationExpression extends NameAndSignals implements GeneID {
  private static final long serialVersionUID = -574351682526309872L;
  public static final transient Logger log = Logger.getLogger(ProteinModificationExpression.class.getName());
  
  /**
   * Key to store the analyte short name as {@link #addData(String, Object)}
   */
  public final static String analyte_short_key = "ANALYTE_SHORT";
  
  /**
   * Key to store the modification name as {@link #addData(String, Object)}
   */
  public final static String modification_key = "MODIFICATION";
  
  /**
   * @param geneName
   */
  private ProteinModificationExpression(String geneName) {
    super(geneName);
  }
  
  /**
   * @param geneName the gene name
   * @param analyteID
   * @param modification
   * @param geneID Corresponding NCBI Gene ID (Entrez).
   */
  public ProteinModificationExpression(String geneName, String analyteID, String modification, int geneID) {
    this(geneName);
    setAnalyteID(analyteID);
    setModification(modification);
    setID(geneID);
  }
  
  
  public String getAnalyteID() {
    Object s = super.getData(analyte_short_key);
    return s==null?null:s.toString();
  }
  
  public void setAnalyteID(String shortName) {
    super.addData(analyte_short_key, shortName);
  }
  
  public String getModification() {
    Object s = super.getData(modification_key);
    return s==null?null:s.toString();
  }
  
  public void setModification(String modName) {
    super.addData(modification_key, modName);
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.data.GenericID#getID()
   */
  @Override
  public Integer getID() {
    Integer i = (Integer) super.getData(getIDType().toString());
    return i==null?getDefaultID():i;
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.data.GenericID#setID(int)
   */
  @Override
  public void setID(Integer geneID) {
    super.addData(getIDType().toString(), new Integer(geneID));
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.GenericID#getDefaultID()
   */
  @Override
  public Integer getDefaultID(){
  	return GeneID.default_geneID;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.GenericID#getIDClass()
   */
  @Override
  public IdentifierClass getIDClass(){
  	return GeneID.gene_id_class;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.GenericID#getIDType()
   */
  @Override
  public IdentifierType getIDType(){
  	return GeneID.gene_id_key;
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignal#merge(java.util.Collection, de.zbit.data.NameAndSignal, de.zbit.data.Signal.MergeType)
   */
  @Override
  protected <T extends NameAndSignals> void merge(Collection<T> source, T target, MergeType m) {
    // Merge private variables to target (=> GeneID)
    Set<Integer> geneIDs = new HashSet<Integer>();
    for (T o :source) {
      GeneID mi = (GeneID)o;
      geneIDs.add(mi.getID());
    }
    
    // Set gene id, if same or unset if they differ
    ((GeneID)target).setID(geneIDs.size()==1?geneIDs.iterator().next():default_geneID);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getUniqueLabel()
   */
  @Override
  public String getUniqueLabel() {
//    String a = getAnalyteID();
    // If data is gene-centered, symbol should be unique.
    // do NOT use getShortProbeName() here. Others merge signals
    // by this return value!
//    if (a!=null) return a;
    
    // Maybe the modification is set. A unique label would then be
    // Protein name + modification name
    if (getModification()!=null) {
      return getAnalyteShortName();
    } else if (getAnalyteID()!=null) {
      return getAnalyteID();
    }
    
    return getName();
  }
  
  /**
   * @return {@link #getName()}_{@link #getModification()}
   */
  public String getAnalyteShortName() {
    String a = getName();
    if (getModification()!=null) {
      a = String.format("%s_%s", a, getModification());
    }
    return a;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#hashCode()
   */
  @Override
  public int hashCode() {
    return super.hashCode() + getUniqueLabel().hashCode();
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(Object o) {
    int r = super.compareTo(o);
    if (r==0 && o instanceof ProteinModificationExpression) {
      r = getUniqueLabel().compareTo(((ProteinModificationExpression)o).getUniqueLabel());
      if (r==0) {
        r = getAnalyteShortName().compareTo(((ProteinModificationExpression)o).getAnalyteShortName());
      }
    }
    return r;
  }
  
}

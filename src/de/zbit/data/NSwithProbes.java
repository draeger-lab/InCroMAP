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
package de.zbit.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import de.zbit.data.Signal.MergeType;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.gui.IntegratorUITools;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.util.Species;
import de.zbit.util.StringUtil;

/**
 * An abstract implementation for probe-based {@link NameAndSignals}.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public abstract class NSwithProbes extends NameAndSignals implements GeneID {
  private static final long serialVersionUID = 7380203443487769632L;
  public static final transient Logger log = Logger.getLogger(NSwithProbes.class.getName());
  
  
  /**
   * Probe names are added to {@link #addData(String, Object)} with this key.
   */
  public final static String probeNameKey = "Probe_name";
  
  /**
   * The key to use in the {@link #addData(String, Object)} map to add
   * the information, if exactly one gene matches one mRNA.
   */
  public final static String gene_centered_key = "Gene_centered";
  
  /**
   * A key to determine if at any stage this {@link NameAndSignals} was gene-centered.
   * If this was the case, it is irreversible and we never need to display probe
   * names!
   */
  private boolean wasGeneCentered = false;
  
  /**
   * Always marks the #gene_centered_key as invisible.
   */
  static {
    NameAndSignals.additional_data_is_invisible.add(gene_centered_key);
  }
  
  /**
   * @param probeName
   * @param geneName
   * @param geneID
   */
  public NSwithProbes(String probeName, String geneName, Integer geneID) {
    super(geneName);
    // Always or never set attributes. Else, super class might get confused
    // with column indices.
    //if (probeName!=null)
    setProbeName(probeName);
    //if (geneID!=null && geneID!=default_geneID && geneID>0)
    if (geneID==null) geneID = default_geneID;
    setGeneID(geneID);
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.data.GeneID#setGeneID(int)
   */
  @Override
  public void setGeneID(int geneID) {
    super.addData(gene_id_key, new Integer(geneID));
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.GeneID#getGeneID()
   */
  @Override
  public int getGeneID() {
    Integer i = (Integer) super.getData(gene_id_key);
    return i==null?default_geneID:i;
  }
  
  /**
   * A boolean flag to determine weather exactly one
   * instance corresponds to one gene.
   * @param bool
   */
  public void setGeneCentered(boolean bool) {
    super.addData(gene_centered_key, bool);
    wasGeneCentered|=bool;
  }
  
  /**
   * @return true if the data is gene centered. False if
   * it is unknown or not the case.
   */
  public Boolean isGeneCentered() {
    Object o = super.getData(gene_centered_key);
    return getBooleanValue(o);
  }


  /**
   * Get the boolean value of an object
   * @param o
   * @return
   */
  public static Boolean getBooleanValue(Object o) {
    if (o==null) return false;
    if (o instanceof Boolean || Boolean.class.isAssignableFrom(o.getClass()))
      return (Boolean)o;
    else return Boolean.valueOf(o.toString());
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getObjectAtColumn(int)
   */
  @Override
  public Object getObjectAtColumn(int columnIndex) {
    Object su = super.getObjectAtColumn(columnIndex);
    
    // Change geneID "-1" to "not found"
    if (su instanceof Number && su.equals(new Integer(-1))) {
      String GeneIDHeader = StringUtil.formatOptionName(GeneID.gene_id_key);
      if (getColumnName(columnIndex).equals(GeneIDHeader)) {
        return "Not found";
      }
    }
    
    return su;
  }
  
  
  
  /**
   * @return the probe name (or a list of probes,
   * separated usually by a ";").
   */
  public String getProbeName() {
    Object probeName = getData(probeNameKey);
    return probeName==null?null:probeName.toString();
  }
  
  /**
   * Set the corresponding probe name.
   */
  public void setProbeName(String probeName) {
    super.addData(probeNameKey, probeName);
  }
  
  /**
   * Remove the probe name
   */
  public void unsetProbeName() {
    super.removeData(probeNameKey);
  }
   
   /**
    * @return associated gene symbol. Only uses stored symbol,
    * does not call any mapper or such.
    */
   public String getGeneSymbol() {
     Object s = super.getData(IdentifierType.GeneSymbol.toString());
     return s==null?getName():s.toString();
   }
   
   

   /* (non-Javadoc)
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   public int compareTo(Object o) {
     // Compare gene symbol
     int r = super.compareTo(o);
     
     // Compare geneID and probe name
     if (o instanceof NSwithProbes) {
       NSwithProbes ot = (NSwithProbes) o;
       if (r==0) r = getGeneID()-ot.getGeneID();
       
       // Compare probe name
       if (r==0) {
         if (isSetProbeName() && ot.isSetProbeName()) {
           r = getProbeName().compareTo(ot.getProbeName());
         } else {
           r=-1;
         }
       }
       //---
     }
     
     return r;
   }
   
   /**
   * @return
   */
  public boolean isSetProbeName() {
    return getProbeName()!=null;
  }


  /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
     String probeName = getProbeName();
     return super.hashCode() + getGeneID() + (probeName!=null?probeName.hashCode():7) ;
   }
   
   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
     String add = null;
     //if (geneID>=0) add="gene_id:"+geneID;
     return super.toString(add);
   }
   
   /* (non-Javadoc)
    * @see de.zbit.data.NameAndSignal#merge(java.util.Collection, de.zbit.data.NameAndSignal, de.zbit.data.Signal.MergeType)
    */
   @Override
   protected <T extends NameAndSignals> void merge(Collection<T> source, T target, MergeType m) {
     
     // Merge private variables to target (=> GeneID)
     // probeName is in superMethod merged.
     Set<Integer> geneIDs = new HashSet<Integer>();
     Set<Boolean> wasGeneCentered = new HashSet<Boolean>();
     for (T o :source) {
       NSwithProbes mi = (NSwithProbes)o;
       geneIDs.add(mi.getGeneID());
       wasGeneCentered.add(mi.wasGeneCentered);
     }
     
     // Set the "wasGeneCentered" variable
     ((NSwithProbes)target).wasGeneCentered = ((NSwithProbes)target).isGeneCentered() || 
       (wasGeneCentered.size()==1 && wasGeneCentered.contains(Boolean.TRUE));
     
     // Set gene id, if same or unset if they differ
     if (geneIDs.size()==1) {
       ((GeneID)target).setGeneID(geneIDs.iterator().next());
     } else {
       // Reset gene-id
       ((GeneID)target).setGeneID(default_geneID);
       if (((NSwithProbes)target).isGeneCentered()) {
         ((NSwithProbes)target).wasGeneCentered = true;
         // Cannot ensure that gene is centered. Better unset flag!
         ((NSwithProbes)target).setGeneCentered(false);
       }
     }
   }
   
   /**
    * Clones all attributes of this instance to the template.
    * @param <T>
    * @param template
   * @return 
    */
   protected <T extends NSwithProbes> T clone(T template) {
     super.cloneAbstractFields(template, this);
     return template;
   }
   
   
   
   /* (non-Javadoc)
    * @see de.zbit.data.NameAndSignals#getUniqueLabel()
    */
   @Override
   public String getUniqueLabel() {
     // If data is gene-centered, symbol should be unique.
     if (isGeneCentered()) return getGeneSymbol();
     else if (wasGeneCentered) {
       // we do not need to fallback on probes.
       String s = getGeneSymbol();
       if (s==null || s.length()<1) s = getName();
       return getShortProbeName(s);
     }
     
     // Get probe name(s)
     String probe = getProbeName();
     // Shorten mutliple concatenated probe names
     probe = getShortProbeName(probe);
       
     return (probe==null||probe.length()<1)?getName():probe;
   }

   /**
    * if string is longer than 10 character, the <code>implodeString</code>
    * (usually ", ") is counted and a string like "firstProbe (+3 more)"
    * is generated. If the <code>implodeString</code> is not contained in the
    * given string, it is simply cut after 7 chars and "..." is appended.
    * @param probe actually, any string.
    * @return short probe string
    */
   public static String getShortProbeName(String probe) {
     // Merged (e.g., gene-centered) probeIds are usually
     // very long. Trim them to human readable length.
     if (probe!=null && probe.length()>10) {
       int probeCount = StringUtil.countString(probe, implodeString);
       // the actual probeCount is number of implodeStrings +1 !
       if (probeCount>0) {
         probe = probe.substring(0, probe.indexOf(implodeString));
         if (probe.length()>15) probe = probe.substring(0,12)+"...";
         probe = String.format("%s (+%s more)", probe, (probeCount));
       } else {
         probe = probe.substring(0,7)+"...";
       }
     }
     return probe;
   }
   

   /**
    * Convert {@link #getGeneID()}s to gene symbols and set names
    * to the symbol.
    * @param data
    * @param species
    * @throws Exception
    */
   public static void convertNamesToGeneSymbols(Iterable<? extends NameAndSignals> data, Species species) throws Exception {
     log.config("Loading GeneSymbol mapping...");
     GeneID2GeneSymbolMapper mapper = IntegratorUITools.get2GeneSymbolMapping(species);
     for (NameAndSignals m: data) {
       if (m instanceof miRNA) {
         if (((miRNA)m).hasTargets()) {
           for (miRNAtarget t: ((miRNA)m).getTargets()) {
             t.setTargetSymbol(mapper.map(t.getTarget()));
           }
         }
       } else if (m instanceof GeneID) {
         if (((GeneID) m).getGeneID()>0) {
           String symbol = mapper.map(((GeneID) m).getGeneID());
           if (symbol!=null && symbol.length()>0) {
             m.name = symbol;
           }
         }
       } else {
         log.warning("Can not annotate gene symbols for " + m.getClass());
         return;
       }
     }
     log.config("Converted GeneIDs to Gene symbols.");
   }
   
}

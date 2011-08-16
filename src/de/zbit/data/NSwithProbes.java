/**
 * @author Clemens Wrzodek
 */
package de.zbit.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import de.zbit.data.Signal.MergeType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.gui.IntegratorGUITools;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.parser.Species;
import de.zbit.util.StringUtil;

/**
 * An abstract implementation for probe-based {@link NameAndSignals}.
 * @author Clemens Wrzodek
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
  }
  
  /**
   * @return true if the data is gene centered. False if
   * it is unknown or not the case.
   */
  public Boolean isGeneCentered() {
    Object o = super.getData(gene_centered_key);
    if (o==null) return false;
    if (o instanceof Boolean) return (Boolean)o;
    else return Boolean.valueOf(o.toString());
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
           r = getProbeName().compareTo(getProbeName());
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
     for (T o :source) {
       GeneID mi = (GeneID)o;
       geneIDs.add(mi.getGeneID());
     }
     
     // Set gene id, if same or unset if they differ
     ((GeneID)target).setGeneID(geneIDs.size()==1?geneIDs.iterator().next():default_geneID);
     if (((NSwithProbes)target).isGeneCentered()) {
       // Cannot ensure that gene is centered. Better unset flag!
       ((NSwithProbes)target).setGeneCentered(false);
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
   public static void convertNamesToGeneSymbols(List<? extends NameAndSignals> data, Species species) throws Exception {
     log.info("Loading GeneSymbol mapping...");
     GeneID2GeneSymbolMapper mapper = IntegratorGUITools.get2GeneSymbolMapping(species);
     for (NameAndSignals m: data) {
       if (m instanceof mRNA) {
         if (((mRNA) m).getGeneID()>0) {
           String symbol = mapper.map(((mRNA) m).getGeneID());
           if (symbol!=null && symbol.length()>0) {
             ((mRNA) m).name = symbol;
           }
         }
       } else if (m instanceof miRNA) {
         if (((miRNA)m).hasTargets()) {
           for (miRNAtarget t: ((miRNA)m).getTargets()) {
             t.setTargetSymbol(mapper.map(t.getTarget()));
           }
         }
       } else {
         log.warning("Can not annotate gene symbols for " + m.getClass());
         return;
       }
     }
     log.info("Converted GeneIDs to Gene symbols.");
   }

}

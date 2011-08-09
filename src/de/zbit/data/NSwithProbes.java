/**
 * @author Clemens Wrzodek
 */
package de.zbit.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import de.zbit.data.Signal.MergeType;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.util.StringUtil;

/**
 * An interface for probe-based {@link NameAndSignals}.
 * @author Clemens Wrzodek
 */
public abstract class NSwithProbes extends NameAndSignals {
  private static final long serialVersionUID = 7380203443487769632L;
  public static final transient Logger log = Logger.getLogger(NSwithProbes.class.getName());
  
  
  /**
   * Probe names are added to {@link #addData(String, Object)} with this key.
   */
  public final static String probeNameKey = "Probe_name";

  /**
   * Means gene id has not been set or mRNA has no associated gene id.
   */
  public final static Integer default_geneID = -1;
  
  /**
   * The key to use in the {@link #addData(String, Object)} map to add
   * the corresponding NCBI Gene ID (Entrez).
   */
  public final static String gene_id_key = "Gene_ID";
  
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
    // Always set attributes.
    //if (probeName!=null)
    setProbeName(probeName);
    //if (geneID!=null && geneID!=default_geneID && geneID>0)
    setGeneID(geneID);
  }
  
  
  /**
   * Set the corresponding NCBI Gene ID.
   * @param geneID
   */
  public void setGeneID(int geneID) {
    super.addData(gene_id_key, new Integer(geneID));
  }
  
  /**
   * @return associated NCBI Gene ID.
   */
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
    super.addData(gene_centered_key, new Boolean(bool));
  }
  
  /**
   * @return true if the data is gene centered. False if
   * it is unknown or not the case.
   */
  public Boolean isGeneCentered() {
    Boolean i = (Boolean) super.getData(gene_centered_key);
    return i==null?false:i;
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
       NSwithProbes mi = (NSwithProbes)o;
       geneIDs.add(mi.getGeneID());
     }
     
     // Set gene id, if same or unset if they differ
     ((NSwithProbes)target).setGeneID(geneIDs.size()==1?geneIDs.iterator().next():default_geneID);
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

}

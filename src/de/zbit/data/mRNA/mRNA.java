/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.data.mRNA;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.gui.IntegratorGUITools;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.parser.Species;
import de.zbit.util.StringUtil;


/**
 * A generic class to hold mRNA probes with Signals and geneIDs.
 * @author Clemens Wrzodek
 */
public class mRNA extends NameAndSignals {
  private static final long serialVersionUID = -5897878964584853247L;
  
  /**
   * Means gene id has not been set or mRNA has no associated gene id.
   */
  public final static int default_geneID = -1;
  
  /**
   * The key to use in the {@link #addData(String, Object)} map to add
   * the corresponding NCBI Gene ID (Entrez).
   */
  public final static String gene_id_key = "Gene_ID";
  
  /**
   * Initialize a new mRNA with the given name.
   * @param name
   */
  public mRNA(String name) {
    super(name);
    setGeneID(default_geneID);
  }
  
  /**
   * @param name - the probe name.
   * @param geneID - Corresponding NCBI Gene ID (Entrez).
   */
  public mRNA(String name, int geneID) {
    this(name);
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
    int r = super.compareTo(o); // Compare mRNA name
    if (o instanceof mRNA) {
      mRNA ot = (mRNA) o;
      if (r==0) r = getGeneID()-ot.getGeneID();
    }
    
    return r;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return super.hashCode() + getGeneID();
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
    Set<Integer> geneIDs = new HashSet<Integer>();
    for (T o :source) {
      mRNA mi = (mRNA)o;
      geneIDs.add(mi.getGeneID());
    }
    
    // Set gene id, if same or unset if they differ
    ((mRNA)target).setGeneID(geneIDs.size()==1?geneIDs.iterator().next():default_geneID);
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    mRNA nm = new mRNA(name, getGeneID());
    super.cloneAbstractFields(nm, this);
    return nm;
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

  /**
   * @return
   */
  public String getProbeName() {
    Object probeName = getData(miRNA.probeNameKey);
    return probeName==null?null:probeName.toString();
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#getUniqueLabel()
   */
  @Override
  public String getUniqueLabel() {
    String probe = getProbeName();
    
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

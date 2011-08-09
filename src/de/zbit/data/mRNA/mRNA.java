/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.data.mRNA;

import java.util.List;

import de.zbit.data.NSwithProbes;
import de.zbit.data.NameAndSignals;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.gui.IntegratorGUITools;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.parser.Species;


/**
 * A generic class to hold mRNA probes with Signals and geneIDs.
 * @author Clemens Wrzodek
 */
public class mRNA extends NSwithProbes {
  private static final long serialVersionUID = -5897878964584853247L;
  
  /**
   * Initialize a new mRNA with the given name.
   * @param name
   */
  public mRNA(String name) {
    this(name, default_geneID);
  }

  /**
   * @param probeName name of the probe
   * @param geneName the gene name
   * @param geneID Corresponding NCBI Gene ID (Entrez).
   */
  public mRNA(String probeName, String geneName, int geneID) {
    super (probeName, geneName, geneID);
  }
  
  /**
   * @param name the gene name
   * @param geneID Corresponding NCBI Gene ID (Entrez).
   */
  public mRNA(String name, int geneID) {
    this(null, name, geneID);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    mRNA nm = new mRNA(name, getGeneID());
    return super.clone(nm);
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

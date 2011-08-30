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
import de.zbit.gui.IntegratorUITools;
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


}

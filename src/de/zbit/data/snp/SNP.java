package de.zbit.data.snp;

import de.zbit.data.GeneID;
import de.zbit.data.NSwithProbes;

/**
 * A generic class to hold a SNP with Signals and containing geneID.
 * @author Finja Büchel
 * @author Clemens Wrzodek
 */
public class SNP extends NSwithProbes {
  private static final long serialVersionUID = 7939630941567479573L;

  public SNP(String dbSNPid) {
    this (dbSNPid, GeneID.default_geneID);
  }
  
  public SNP(String dbSNPid, Integer geneID) {
    super(null, dbSNPid, geneID);
    unsetProbeName();
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    SNP nm = new SNP(name, getGeneID());
    return super.clone(nm);
  }
  
}

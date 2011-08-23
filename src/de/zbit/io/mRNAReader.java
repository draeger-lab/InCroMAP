/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.io;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.miRNA.miRNA;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.parser.Species;
import de.zbit.util.ProgressBar;

/**
 * A generic reader to read {@link mRNA} data.
 * @author Clemens Wrzodek
 */
public class mRNAReader extends AbstractGeneBasedNSreader<mRNA> {
  public static final transient Logger log = Logger.getLogger(mRNAReader.class.getName());
  

  /**
   * This is ONLY for use in combination with {@link #importWithGUI(String)} afterwards.
   */
  public mRNAReader() {
    super();
  }
  
  public mRNAReader(int identifierCol, IdentifierType idType, Species species) {
    super(identifierCol, idType, species);
  }

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#getAdditionalExpectedColumns()
   */
  @Override
  protected List<ExpectedColumn> getAdditionalExpectedColumns() {
    return null;
  }


  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#processAdditionalExpectedColumns(java.util.List)
   */
  @Override
  protected void processAdditionalExpectedColumns(List<ExpectedColumn> additional) {}


  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#createObject(java.lang.String, java.lang.Integer, java.lang.String[])
   */
  @Override
  protected mRNA createObject(String name, Integer geneID, String[] line) {
    // Create mRNA
    mRNA m;
    if (geneID!=null) {
      m = new mRNA(name, geneID);
    } else {
      m = new mRNA(name);
    }
    
    return m;
  }


  /**
   * @param args
   * @throws Exception 
   * @throws IOException 
   */
  @SuppressWarnings({ "unused" })
  public static void main(String[] args) throws IOException, Exception {
    mRNAReader r2 = new mRNAReader();
    r2.importWithGUI(null, "mRNA_data_new.txt");
    if (true) return;
    
    // Jaworski Dataset:
//    mRNAReader r = new mRNAReader("Ctnnb1",16, IdentifierType.Numeric,species);
//    r.addSecondIdentifier(13, IdentifierType.Symbol);
//    r.addAdditionalData(0, "probe_name");
//    r.addAdditionalData(12, "description");
//    r.addSignalColumn(7, SignalType.FoldChange); // 7-9 = Cat/Ras/Ras_vs_Cat; 10=Cat_vs_Ras
//    r.addSignalColumn(4, SignalType.pValue); // 4-6 = Cat/Ras/Ras_vs_Cat
//    
//    r.progress = new ProgressBar(0);
//    Collection<mRNA> c = r.read("mRNA_data.txt");
    
    mRNAReader r = getExampleReader();
    r.progress = new ProgressBar(0);
    Collection<mRNA> c = r.read("mRNA_data_new.txt");
    
    int noGI=0;
    for (mRNA mRNA : c) {
      if (mRNA.getGeneID()<0) noGI++;
      System.out.println(mRNA);
    }
    System.out.println(noGI + " mRNAs without Gene ID.");
  }

  public static mRNAReader getExampleReader() throws IOException {
    Species species = new Species("Mus musculus", "_MOUSE", "mouse", "mmu", 10090);
    // New dataset
    mRNAReader r = new mRNAReader(3, IdentifierType.NCBI_GeneID, species);
    r.addSecondIdentifier(1, IdentifierType.GeneSymbol);
    r.addAdditionalData(0, miRNA.probeNameKey);
    r.addAdditionalData(2, "description");
    r.addSignalColumn(27, SignalType.FoldChange, "Ctnnb1"); // 27-30 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    r.addSignalColumn(31, SignalType.pValue, "Ctnnb1"); // 31-34 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    
    return r;
  }

  
}

package de.zbit.io;

import java.util.Collection;
import java.util.logging.Logger;

import de.zbit.data.Signal.SignalType;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtargets;
import de.zbit.util.Utils;
import de.zbit.util.ValueTriplet;

/**
 * A generic reader to read miRNA data.
 * @author Clemens Wrzodek
 */
public class miRNAReader extends NameAndSignalReader<miRNA> {
  public static final transient Logger log = Logger.getLogger(miRNAReader.class.getName());
  
  /**
   * Optional: Column with probe name
   */
  private int probeNameCol;
  
  
  public miRNAReader(int miRNAnameCol) {
    super(miRNAnameCol);
  }
  public miRNAReader(int miRNAnameCol, int probeNameCol) {
    super(miRNAnameCol);
    this.probeNameCol = probeNameCol;
  }
  public miRNAReader(int miRNAnameCol, int probeNameCol, Collection<ValueTriplet<Integer, SignalType, String>> signalColumns) {
    super(miRNAnameCol, signalColumns);
    this.probeNameCol = probeNameCol;
  }

  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#createObject(java.lang.String, java.lang.String[])
   */
  @Override
  protected miRNA createObject(String name, String[] line) {
    String probeName = probeNameCol>=0?line[probeNameCol]:null;
    miRNA m = new miRNA(name, probeName);
    return m;
  }
  
  
  /**
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    // Read miRNA
    miRNAReader r = new miRNAReader(1,0);
    r.addSignalColumn(25, SignalType.FoldChange, "Ctnnb1"); // 25-28 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    r.addSignalColumn(29, SignalType.pValue, "Ctnnb1"); // 29-32 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    
    Collection<miRNA> c = r.read("miRNA_data.txt");
    
    
    // Read targets
    miRNAtargets t_all = (miRNAtargets) Utils.loadGZippedObject("miRNAtargets_HC.dat");
    //miRNAtargets t_all = (miRNAtargets) CSVwriteableIO.read(new miRNAtargets(), "miRNAtargets_HC.txt");
    int matched = miRNA.link_miRNA_and_targets(t_all, c);
    
    // Output
    for (miRNA miRNA : c) {
      System.out.println(miRNA);
    }
    
    // Print stats
    System.out.println("Total miRNAs: " + c.size() + " miRNAs_with_targets: " + matched + " = " +(matched/(double)c.size()*100.0)+ "%");
  }
  
}

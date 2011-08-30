/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.data.GeneID;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.data.miRNA.miRNAtargets;
import de.zbit.io.CSVWriter;
import de.zbit.io.mRNAReader;
import de.zbit.io.miRNAReader;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.mapper.enrichment.GeneID2ListOfKEGGpathways;
import de.zbit.parser.Species;
import de.zbit.util.ArrayUtils;
import de.zbit.util.Timer;
import de.zbit.util.Utils;
import de.zbit.util.ValueTriplet;

/**
 * @author Clemens Wrzodek
 *
 */
public class miRNA2mRNA_pair {
  public static final transient Logger log = Logger.getLogger(miRNA2mRNA_pair.class.getName());
  
  /**
   * The linkage from {@link miRNAtarget#getTarget()} to the actual {@link mRNA}.
   */
  private Map<Integer, Collection<mRNA>> link;
  
  /**
   * Minimum and maximum fold changes of mRNAs and miRNAs in {@link #link}.
   * These are used later, to calculate a combined FC.
   */
  private double mRNA_minFC=0, mRNA_maxFC, miRNA_minFC=0, miRNA_maxFC;
  
  /**
   * Initializes the {@link #link} map.
   */
  private miRNA2mRNA_pair() {
    super();
    link = new HashMap<Integer, Collection<mRNA>>();
  }
  
  /**
   * Links the given miRNAs to the given mRNAs. The
   * miRNAs must already be annotated with targets.
   * @param mirna
   * @param mrna
   * @param experimentName
   */
  public miRNA2mRNA_pair(Collection<miRNA> mirna, Collection<mRNA> mrna, String experimentName) {
    this();
    link (mirna, mrna, experimentName);
  }
  
  /**
   * Annotates the miRNAs with the given targets and
   * links them to the given mRNAs.
   * @param mirna
   * @param targets
   * @param mrna
   * @param experimentName
   */
  public miRNA2mRNA_pair(Collection<miRNA> mirna, miRNAtargets targets, Collection<mRNA> mrna, String experimentName) {
    this();
    miRNA.link_miRNA_and_targets(targets, mirna);
    link (mirna, mrna, experimentName);
  }
  
  

  /**
   * Links {@link miRNA}s to {@link mRNA}s.
   * @param mirna miRNAs
   * @param mrna mRNAs
   * @param experimentName required to get min and max fold changes.
   */
  private void link(Collection<miRNA> mirna, Collection<mRNA> mrna, String experimentName) {
    // Reset local min/max holders
    miRNA_maxFC = Float.MIN_VALUE;
    miRNA_minFC = Float.MAX_VALUE;
    link.clear();
    
    // Process mRNA to a more efficient data structure
    Map<Integer, Collection<mRNA>> geneIDmap = getGeneID2mRNAMappingAndTrackMinMax(mrna, experimentName);
    
    // Process miRNA's targets
    Set<Integer> processedTargets = new HashSet<Integer>();
    for (miRNA mir: mirna) {
      // Remember min and max fold change
      Number fc = mir.getSignalValue(SignalType.FoldChange, experimentName);
      if (fc!=null && !Double.isNaN(fc.doubleValue())) {
        miRNA_maxFC = Math.max(miRNA_maxFC, fc.doubleValue());
        miRNA_minFC = Math.min(miRNA_minFC, fc.doubleValue());
      }
      
      // Iterate through all targets
      if (!mir.hasTargets()) continue;
      for (miRNAtarget target: mir.getTargets()) {
        // Avoid duplicate targets
        if (!processedTargets.add(target.getTarget())) continue;
        
        // Look if we have a mapping and get or create mRNA list.
        Collection<mRNA> mr_targets = geneIDmap.get(target.getTarget());
        if (mr_targets!=null) {
          link.put(target.getTarget(), mr_targets);
        }
      }
    }
  }

  /**
   * A static variant of {@link #getGeneID2mRNAMappingAndTrackMinMax(Collection, String)} that creates
   * a new instance if {@link miRNA2mRNA_pair} and returns the resulting map. Min and max FC is not
   * tracked.
   * @param mrna list of all mRNAs
   * @return a map from geneID to a collection of corresponding {@link mRNA}s.
   */
  public static <T extends NameAndSignals> Map<Integer, Collection<T>> getGeneID2mRNAMapping(Collection<T> mrna) {
    return new miRNA2mRNA_pair().getGeneID2mRNAMappingAndTrackMinMax(mrna, null);
  }
  
  /**
   * @param mrna list of all mRNAs
   * @param experimentName if you want to track minimum and maximum FC, give the experiment name to track
   * those values for. Else, simply set this to <code>null</code>.
   * @return a map from geneID to a collection of corresponding {@link mRNA}s.
   */
  public <T extends NameAndSignals> Map<Integer, Collection<T>> getGeneID2mRNAMappingAndTrackMinMax(Collection<T> mrna, String experimentName) {
    // Reset local min/max holders
    mRNA_maxFC = Float.MIN_VALUE;
    mRNA_minFC = Float.MAX_VALUE;
    
    Map<Integer, Collection<T>> geneIDmap = new HashMap<Integer, Collection<T>>();
    for (T mr: mrna) {
      // Remember min and max fold change
      if (experimentName!=null) {
        Number fc = mr.getSignalValue(SignalType.FoldChange, experimentName);
        if (fc!=null && !Double.isNaN(fc.doubleValue())) {
          mRNA_maxFC = Math.max(mRNA_maxFC, fc.doubleValue());
          mRNA_minFC = Math.min(mRNA_minFC, fc.doubleValue());
        }
      }
      int geneID = (mr instanceof GeneID)?((GeneID)mr).getGeneID():-1;
      if (geneID<=0) continue;
      
      Collection<T> mr_targets = geneIDmap.get(geneID);
      if (mr_targets==null) {
        mr_targets = initializeTargetCollection();
        geneIDmap.put(geneID, mr_targets);
      }
      
      mr_targets.add(mr);
    }
    return geneIDmap;
  }

  @SuppressWarnings("unchecked")
  private static <T extends NameAndSignals> Collection<T> initializeTargetCollection() {
    return (Collection<T>) new LinkedList<mRNA>();
  }

  
  /**
   * Get all mRNA targets of this miRNA.
   * @param miRNA - MUST BE from the collection, given as input for this class.
   * I.e. miRNA must be annotated with targets.
   * @return
   */
  public Collection<mRNA> get_mRNAs(miRNA miRNA) {
    Collection<miRNAtarget> targets = miRNA.getTargets();
    if (targets==null) return null;
    
    Collection<mRNA> ret = initializeTargetCollection();
     for (miRNAtarget miRNAtarget : targets) {
       Collection<mRNA> r = get_mRNAs(miRNAtarget);
       if (r!=null && r.size()>0) ret.addAll(r);
    }
      
    return ret;
  }
  
  /**
   * Get all mRNA targets.
   * @param target
   * @return might return null if no mRNAs for the target are available.
   */
  public Collection<mRNA> get_mRNAs(miRNAtarget target) {
    Collection<mRNA> r = link.get(target.getTarget());
    if (r==null || r.size()<1)
      log.fine("No mRNAs found in expression data for " + target);
    return r;
  }
  
  /**
   * Get all mRNA targets.
   * @param target
   * @return might return null if no mRNAs for the target are available.
   */
  public Collection<mRNA> get_mRNAs(Integer geneID) {
    Collection<mRNA> r = link.get(geneID);
    if (r==null || r.size()<1)
      log.fine("No mRNAs found in expression data for GI:" + geneID);
    return r;
  }
  
  /**
   * Generate a linked list of {@link miRNA}s, {@link miRNAtarget}s. and {@link mRNA}s.
   * @param miRNA collection of miRNAs (must be a (sub-)set that has been used to initialize this class
   * @return ValueTriplet<miRNA, miRNAtarget, mRNA>
   */
  public List<ValueTriplet<miRNA, miRNAtarget, mRNA>> getExpressionPairedTable(Collection <miRNA> miRNA) {
    return getExpressionPairedTable(miRNA,false,null);
  }
  
  /**
   * Returns the {@link miRNA} - {@link miRNAtarget} - {@link mRNA} table.
   * @see #getExpressionPairedTable(Collection)
   * @param miRNA
   * @param geneCentered if true, builds the table on a gene centered basis. Else, it should be probe centered.
   * @param m defines how to deal with the signals (e.g., taking the mean). Only required when geneCentered is true.
   * Else, this parameter is ignored.
   * @return ValueTriplet<miRNA, miRNAtarget, mRNA>
   */
  public List<ValueTriplet<miRNA, miRNAtarget, mRNA>> getExpressionPairedTable(Collection <miRNA> miRNA, boolean geneCentered, MergeType m) {
    return getExpressionPairedTable(miRNA, geneCentered, m, link);
  }
  
  
  /**
   * Returns the {@link miRNA} - {@link miRNAtarget} - {@link mRNA} table.
   * @param <T> any {@link NameAndSignals} that should be matched to the {@link miRNAtargets}. Typically this is a {@link mRNA}.
   * @param miRNA
   * @param geneCentered  if true, builds the table on a gene centered basis. Else, it should be probe centered.
   * @param m defines how to deal with the signals (e.g., taking the mean). Only required when geneCentered is true.
   * Else, this parameter is ignored.
   * @param mRNA dataset of type T to pair the miRNAs with.
   * @return List<ValueTriplet<miRNA, miRNAtarget, T>>
   */
  public static <T extends NameAndSignals> List<ValueTriplet<miRNA, miRNAtarget, T>> getExpressionPairedTable(Collection <miRNA> miRNA, boolean geneCentered, MergeType m, Map<Integer, ? extends Collection<T>> mRNA) {
    
    
    // Eventually get gene centered miRNAs
    if (geneCentered) {
      miRNA = NameAndSignals.geneCentered(miRNA, m);
    }
    
    // Build result list
    List<ValueTriplet<miRNA, miRNAtarget, T>> ret = new ArrayList<ValueTriplet<miRNA, miRNAtarget, T>>();
    
    // For all miRNAs, output targets -- A
    miRNA mi = null;
    Iterator<miRNA> it = miRNA.iterator();
    while (it.hasNext()) {
      
      // Get current miRNA
      mi = it.next();
      if (!mi.hasTargets()) {
        log.fine("No targets for " + mi);
        continue;
      }
      
      
      // Get all targets -- B
      Collection<miRNAtarget> targets = mi.getUniqueTargets();
      for (miRNAtarget miRNAtarget : targets) {
        
        // Get all target mRNAs -- C
        Collection<T> target_mRNAs = mRNA.get(miRNAtarget.getTarget());
        if (target_mRNAs==null || target_mRNAs.size()<1) continue;
        
        // Eventually get gene centered mRNAs
        if (geneCentered) {
          target_mRNAs = NameAndSignals.geneCentered(target_mRNAs, m);
        }
        
        // Output each pair 2 table
        Iterator<T> it2 = target_mRNAs.iterator();
        while (it2.hasNext()) {
          T mr = it2.next();
          
          ret.add(new ValueTriplet<miRNA, miRNAtarget, T>(mi, miRNAtarget, mr));
        }
      }
    }
    
    return ret;
  }
  
  protected List<String[]> writeExpressionPairedTable(Collection <miRNA> miRNA, String experimentName) {
    return writeExpressionPairedTable(getExpressionPairedTable(miRNA),experimentName);
  }
  protected List<String[]> writeExpressionPairedTable(List<ValueTriplet<miRNA, miRNAtarget, mRNA>> table, String experimentName) {
    LinkedList<String[]> ret = new LinkedList<String[]>();
    // TODO: Species (mmu here) should be a variable (add GeneID2ListOfKEGGpathways to submitted variables)!
    GeneID2ListOfKEGGpathways pws=null;
    try {
      pws= new GeneID2ListOfKEGGpathways("mmu");
    } catch (IOException e) {
      log.log(Level.WARNING, "Could not read gene_id 2 kegg pathway mapping.", e);
    }
    
    // Add header
    String[] header = new String[]{"miRNA_probe", "miRNA", "p-value", "FC",
        "Source", "Relationship", "Combined_FC",
        "mRNA_probe", "mRNA", "p-value", "FC", "Description", "GeneID", "Pathways"};
    ret.add(header);
    
    // TODO: Make more dynamic (Iterate over all objects and scores, defined in the first of each elements).
    
    // Add content
    for (ValueTriplet<miRNA, miRNAtarget, mRNA> vt : table) {
      double mirna_fc = vt.getA().getSignalValue(SignalType.FoldChange, experimentName).doubleValue();
      double mrna_fc = vt.getC().getSignalValue(SignalType.FoldChange, experimentName).doubleValue();
      
      // Init row and set miRNA data
      String[] curRow = new String[14];
      curRow[0] = vt.getA().getProbeName();
      curRow[1] = vt.getA().getName();
      curRow[2] = vt.getA().getSignalValue(SignalType.pValue, experimentName)+"";
      curRow[3] = mirna_fc+"";
      
      // Target relationship
      curRow[4] = vt.getB().getSource();
      curRow[5] = (mirna_fc>0?"Up_":"Down_") + (mrna_fc>0?"Up":"Down");
      curRow[6] = getPairingScore(mirna_fc, mrna_fc)+"";
      
      // mRNA target
      curRow[7] = vt.getC().getData("probe_name").toString();
      curRow[8] = vt.getC().getName();
      curRow[9] = vt.getC().getSignalValue(SignalType.pValue, experimentName)+"";
      curRow[10]= mrna_fc+"";
      curRow[11]= vt.getC().getData("description").toString();
      curRow[12]= Integer.toString(vt.getC().getGeneID());
      
      // Pathways of mRNA target
      curRow[13]= "";
      if (vt.getC().getGeneID()>0 && pws!=null) {
        try {
          Collection<String> c = pws.map(vt.getC().getGeneID());
          if (c!=null && c.size()>0) {
            curRow[13] = ArrayUtils.implode(c.toArray(new String[0]), ", ");
          }
        } catch (Exception e) {
          log.log(Level.WARNING, "Could not annotate mRNA with pathways.", e);
        }
      }
      
      
      
      
      ret.add(curRow);
    }
    
    return ret;
  }
  
  /**
   * <p>Note:<br/>The given miRNA and mRNAs must have been in the list, while initializing
   * this class! Else, the min and max values for rescaling might be corrupt.</p>
   * @see #getPairingScore(float, float)
   * @param mi
   * @param mr
   * @param experimentName
   * @return
   */
  public double getPairingScore(miRNA mi, mRNA mr, String experimentName) {
    return getPairingScore(mi.getSignalValue(SignalType.FoldChange, experimentName),
      mr.getSignalValue(SignalType.FoldChange, experimentName));
  }
  /**
   * Calculates and returns a fold change for the pairing:
   * Both fold change distributions are rescaled from -0.5 to 0.5 and
   * substracted for this score.
   * @param mirna_fc
   * @param mrna_fc
   * @return
   */
  public double getPairingScore(Number mirna_fc, Number mrna_fc) {
    double n_mirna_fc = Utils.normalize(mirna_fc.doubleValue(), miRNA_minFC, miRNA_maxFC, -.5, .5);
    double n_mrna_fc = Utils.normalize(mrna_fc.doubleValue(), mRNA_minFC, mRNA_maxFC, -.5, .5);
    return n_mirna_fc-n_mrna_fc;
  }
  
  
  @SuppressWarnings("unused")
  public static void main(String[] args) throws Exception {
    String experimentName = "Ctnnb1";
    int offset = 0; // Offset for experiment: 0=Cat/1=Ras/2=Cat_vs_Ras/3=Cat_vs_Ras_KONTROLLEN
    pairData(experimentName, offset);
    if (true) return;
    
    // Batch process all.
    offset=0;
    for (String exName: new String[]{"Ctnnb1", "Ras", "Cat_vs_Ras", "Cat_vs_Ras_KONTROLLEN"}) {
      pairData(exName, offset);
      offset++;
    }
    
    /*
    // Select a random miRNA
    miRNA mi = null;
    Iterator<miRNA> it = miRNA.iterator();
    while (mi==null || !mi.hasTargets()) {
      mi = it.next();
    }
    System.out.println("Shwoing targets for " + mi);
    
    // Get and show targets
    Collection<mRNA> p = pair.get_mRNAs(mi);
    for (mRNA m : p) {
      System.out.println(m);
    }
     */ 
  }
  
  /**
   * Customized for Michael Schwarz Ctnnb1/Ras dataset.
   * @param experimentName
   * @param offset
   * @throws Exception
   */
  private static void pairData(String experimentName, int offset) throws Exception {
    //Species species = Species.search((List<Species>)Species.loadFromCSV("species.txt"), "mouse", -1);
    Species species = new Species("Mus musculus", "_MOUSE", "Mouse", "mmu", 10090);
    
    // Read mRNA
    System.out.println("Reading mRNA"); Timer t = new Timer();
    // Jaworski
    //    mRNAReader r = new mRNAReader(experimentName,16, IdentifierType.Numeric,species);
//    r.addSecondIdentifier(13, IdentifierType.Symbol);
//    r.addAdditionalData(0, "probe_name");
//    r.addAdditionalData(12, "description");
//    r.addSignalColumn(7, SignalType.FoldChange); // 7-9 = Cat/Ras/Ras_vs_Cat; 10=Cat_vs_Ras
//    r.addSignalColumn(4, SignalType.pValue); // 4-6 = Cat/Ras/Ras_vs_Cat
//    Collection<mRNA> mRNA = r.read("mRNA_data.txt");
    // New dataset
    mRNAReader r = new mRNAReader(3, IdentifierType.NCBI_GeneID, species);
    r.addSecondIdentifier(1, IdentifierType.GeneSymbol);
    r.addAdditionalData(0, "probe_name");
    r.addAdditionalData(2, "description");
    r.addSignalColumn(27+offset, SignalType.FoldChange, experimentName); // 27-30 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    r.addSignalColumn(31+offset, SignalType.pValue, experimentName); // 31-34 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    Collection<mRNA> mRNA = r.read("mRNA_data_new.txt");
    System.out.println(t.getNiceAndReset());
    
    // Read miRNA
    System.out.println("Reading miRNA");
    miRNAReader r2 = new miRNAReader(1,0);
    r2.addSignalColumn(25+offset, SignalType.FoldChange, experimentName); // 25-28 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    r2.addSignalColumn(29+offset, SignalType.pValue, experimentName); // 29-32 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    Collection<miRNA> miRNA = r2.read("miRNA_data.txt");
    System.out.println(t.getNiceAndReset());
    
    // Read targets
    System.out.println("Reading targets");
    miRNAtargets t_all = (miRNAtargets) Utils.loadGZippedObject("miRNAtargets_HC.dat");
    System.out.println(t.getNiceAndReset());
    
    
    
    // miRNA 2 mRNA pairing
    System.out.println("Pairing data");
    miRNA2mRNA_pair pair = new miRNA2mRNA_pair(miRNA, t_all, mRNA, experimentName);
    System.out.println(t.getNiceAndReset());
    
    System.out.println("Listing all realtions");
    List<ValueTriplet<miRNA, miRNAtarget, mRNA>> relations = pair.getExpressionPairedTable(miRNA,true,MergeType.Mean);
    System.out.println(t.getNiceAndReset());
    
    System.out.println("Generating string table for " + relations.size() + " relations");
    List<String[]> table = pair.writeExpressionPairedTable(relations, experimentName);
    System.out.println(t.getNiceAndReset());
    
    System.out.println("Writing table to file");
    CSVWriter writer = new CSVWriter();
    writer.write(table.toArray(new String[0][]), experimentName + "_paired_table_gene_centered.txt");
    System.out.println(t.getNiceAndReset());
    
    
    // Probe centered
    System.out.println("Listing all probe realtions");
    relations = pair.getExpressionPairedTable(miRNA);
    System.out.println(t.getNiceAndReset());
    
    System.out.println("Generating string table for " + relations.size() + " relations");
    table = pair.writeExpressionPairedTable(relations, experimentName);
    System.out.println(t.getNiceAndReset());
    
    System.out.println("Writing table to file");
    writer = new CSVWriter();
    writer.write(table.toArray(new String[0][]), experimentName + "_paired_table_probe_centered.txt");
    System.out.println(t.getNiceAndReset());
  }


}

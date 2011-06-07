/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.io.mRNAReader;
import de.zbit.mapper.AbstractMapper;
import de.zbit.mapper.GeneID2KeggIDMapper;
import de.zbit.mapper.KeggPathwayID2PathwayName;
import de.zbit.mapper.MappingUtils;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.mapper.enrichment.KeggID2PathwayMapper;
import de.zbit.math.BenjaminiHochberg;
import de.zbit.math.Correction;
import de.zbit.math.HypergeometricTest;
import de.zbit.parser.Species;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.ProgressBar;
import de.zbit.util.ValuePair;

/**
 * Identifies enriched KEGG Pathways in a list of genes.
 * @author Clemens Wrzodek
 */
@Deprecated
public class PathwayEnrichment {
  public static final transient Logger log = Logger.getLogger(PathwayEnrichment.class.getName());
  
  /**
   * Mapping from GeneID 2 Kegg ID
   */
  private GeneID2KeggIDMapper gene2kegg=null;
  
  /**
   * Mapping from KeggID 2 Kegg Pathways
   */
  private KeggID2PathwayMapper kegg2pw=null;
  
  /**
   * pValue 2 qValue FDR correction method to correct pValues
   * for multiple testing.
   */
  private Correction qVal = new BenjaminiHochberg();
  
  /**
   * Progress Bar (mainly for downloading and reading mapping flat files).
   */
  private AbstractProgressBar prog;

  public PathwayEnrichment(GeneID2KeggIDMapper gene2kegg, KeggID2PathwayMapper kegg2pw, String species_kegg_abbr, AbstractProgressBar prog) throws IOException {
    super();
    this.gene2kegg = gene2kegg;
    this.kegg2pw = kegg2pw;
    this.prog = prog;
    
    // Check if all same species and set species string
    Set<String> species = new HashSet<String>();
    if (species_kegg_abbr!=null && species_kegg_abbr.length()>1) species.add(species_kegg_abbr);
    if (gene2kegg!=null) species.add(gene2kegg.getSpeciesKEGGabbreviation());
    if (kegg2pw!=null) species.add(kegg2pw.getSpeciesKEGGabbreviation());
    if (species.size()!=1) {
      throw new IOException("Cannot init species specific pathway enrichment with " + species.size() + " species.");
    } else {
      species_kegg_abbr = species.iterator().next();
    }
    
    // Eventually initialize null variables
    initializePathwayMapping(species_kegg_abbr);
  }
  
  public PathwayEnrichment(GeneID2KeggIDMapper gene2kegg, KeggID2PathwayMapper kegg2pw, AbstractProgressBar prog) throws IOException {
    this(gene2kegg, kegg2pw, null, prog);
  }
  
  public PathwayEnrichment(String species_kegg_abbr, AbstractProgressBar prog) throws IOException {
    this(null,null, species_kegg_abbr, prog);
  }
  
  public PathwayEnrichment(GeneID2KeggIDMapper gene2kegg, KeggID2PathwayMapper kegg2pw) throws IOException {
    this (gene2kegg,kegg2pw, null);
  }
  
  public PathwayEnrichment(String species_kegg_abbr) throws IOException {
    this (species_kegg_abbr, null);
  }
  
  /**
   * Init the GeneID 2 Kegg Pathways mappings
   * @param kegg_species
   * @throws IOException
   */
  private void initializePathwayMapping(String kegg_species) throws IOException {
    //pw_mapping = new GeneID2ListOfKEGGpathways(kegg_species, prog);
    gene2kegg = new GeneID2KeggIDMapper(kegg_species, prog);
    kegg2pw = new KeggID2PathwayMapper(kegg_species, prog);
  }
  
  /**
   * Maps all given genes to a pathway centered view.
   * 
   * 
   * <p>The Type of the returned {@link Map#values()} depends on the type of the input geneList.
   * If your input list consists of {@link mRNA}, the {@link Map#values()} will also contain
   * {@link mRNA}s, else it will always contain {@link Integer}s, representing the Gene ID.
   * 
   * @param <T>
   * @param geneList
   * @param idType
   * @return a mapping from pathways to [preferable mRNAs from geneList, else: GeneIDs from the geneList].
   */
  @SuppressWarnings("unchecked")
  private <T> Map<String, Set<?>> getContainedPathways(Collection<T> geneList, IdentifierType idType) {

    // Initialize mapper from InputID to GeneID
    AbstractMapper<String, Integer> mapper=null;
    if (idType!=null && !idType.equals(IdentifierType.GeneID)) {
      try {
        mapper = MappingUtils.initialize2GeneIDMapper(idType, prog, species);
      } catch (IOException e) {
        log.log(Level.WARNING, "Could not read mapping file to map your gene identifiers to Entrez GeneIDs.", e);
        return null;
      }
    }
    
    // Mapping from Pathway 2 Genes from geneList contained in this pathway.
    Map<String, Set<?>> pw_genesFromListInPW = new HashMap<String, Set<?>>();
    for(T gene: geneList) {
      
      // Get Entrez gene ID of gene
      Integer geneID;
      mRNA mr = null;
      if (gene instanceof mRNA) {
        geneID = ((mRNA)gene).getGeneID();
        mr = ((mRNA)gene);
      } else if (mapper != null){
        try {
          geneID = mapper.map(gene.toString());
        } catch (Exception e) {
          log.log(Level.WARNING, "Could not map " + gene, e);
          continue;
        }
      } else if (Integer.class.isAssignableFrom(gene.getClass())) {
        geneID = (Integer) gene;
      } else if (idType.equals(IdentifierType.GeneID)) {
        geneID = Integer.parseInt(gene.toString());
      } else {
        log.log(Level.WARNING, "Could not get Entrez Gene ID for " + gene);
        geneID = -1;
      }
      if (geneID<=0) continue;
      
      // Get pathways, in which this gene is contained
      Collection pws=null;
      try {
        String keggID = gene2kegg.map(geneID);
        pws = kegg2pw.map(keggID);
      } catch (Exception e) {
        log.log(Level.WARNING, "Could not get Pathways for " + geneID, e);
      }
      
      // Add to list
      if (pws!=null && pws.size()>0) {
        for (Object pw : pws) {
          // Ensure that PW is in our map
          String pw_string = pw.toString();
          Set pwGenes = pw_genesFromListInPW.get(pw_string);
          if (pwGenes==null) {
            if (mr!=null) {
              pwGenes = new HashSet<mRNA>();
            } else {
              pwGenes = new HashSet<Integer>();
            }
            pw_genesFromListInPW.put(pw_string, pwGenes);
          }
          
          // Add current gene to pw list
          pwGenes.add(mr!=null?mr:geneID);
        }
      }
      
    }
    
    return pw_genesFromListInPW;
  }
  
  /**
   * Returns enriched pathways. If you have an array of genes, please use
   * {@link Arrays#asList(Object...)} 
   * <p>Note: {@link mRNA}s without {@link mRNA#getGeneID()} are NOT being
   * removed and NOT ignored. Thus, they are counted to the totalGeneList
   * size and have an influence on the pValue. If you remove all genes / probes
   * that have no assigned geneID, you might get better pValues !
   * @param <T>
   * @param geneList
   * @return
   */
  public <T> List<EnrichmentObject> getEnrichedPathways(Collection<mRNA> geneList) {
    return getEnrichedPathways(geneList, null);
  }
  
  /**
   * Returns enriched pathways. If you have an array of genes, please use
   * {@link Arrays#asList(Object...)}
   * @param <T>
   * @param geneList
   * @param idType
   */
  public <T> List<EnrichmentObject> getEnrichedPathways(Collection<T> geneList, IdentifierType idType) {
    
    // Get Pathway list from gene list
    Map<String, Set<?>> pwList = getContainedPathways(geneList, idType);
    
    // Init Kegg Pathway ID 2 Kegg Pathway Name mapping
    KeggPathwayID2PathwayName pwID2name_mapper=null;
    try {
      pwID2name_mapper = new KeggPathwayID2PathwayName(prog);
    } catch (IOException e) { // not severe, will leave the id field blank.
      log.log(Level.WARNING, "Could not read KEGG pathway mapping file.", e);
    }
    
    // Initialize pValue calculations and list for further qValue
    HypergeometricTest pval = new HypergeometricTest(kegg2pw.getSumOfGenesInAllPathways(), geneList.size());
    List<ValuePair<String, Double>> pw_qValue = new ArrayList<ValuePair<String, Double>>();
    List<EnrichmentObject> ret = new LinkedList<EnrichmentObject>();
    for (Map.Entry<String, Set<?>> entry : pwList.entrySet()) {
      
      // KEGG Pathway id
      String pw_name=entry.getKey();
      try {
        pw_name = pwID2name_mapper.map(entry.getKey());
      } catch (Exception e) {
        if (pwID2name_mapper!=null) {
          log.log(Level.WARNING, String.format("Could not map KEGG pathway id 2 name: %s", entry.getKey()), e);
        }
      }
      
      // Total # genes in pw
      int pwSize=kegg2pw.getPathwaySize(entry.getKey());
      
      // pValue
      double pv = pval.getPvalue(pwSize, entry.getValue().size());
      ValuePair<String, Double> vp = new ValuePair<String, Double>(pw_name, pv);
      pw_qValue.add(vp);
      
      // Create result object
      EnrichmentObject o = new EnrichmentObject(pw_name,entry.getKey(),entry.getValue().size(), geneList.size(),
        pwSize, kegg2pw.getSumOfGenesInAllPathways(),pv, vp.getB(), entry.getValue());
      ret.add(o);
    }
    
    // Correct pValues
    if (ret.size()>0) {
      qVal.pVal_adjust(pw_qValue);
      Comparator<ValuePair<String, Double>> pw_name_compare = pw_qValue.iterator().next().getComparator_OnlyCompareA();
      Collections.sort(pw_qValue, pw_name_compare);
      for (EnrichmentObject eo : ret) {
        int idx = Collections.binarySearch(pw_qValue, new ValuePair<String, Double>(eo.getName(), null), pw_name_compare);
        eo.setQValue(pw_qValue.get(idx).getB());
       }
    }
    
    // Initially sort returned list by pValue
    Collections.sort(ret, Signal.getComparator(NameAndSignals.defaultExperimentName, SignalType.pValue));
    
    return ret;
  }
  
  
  /**
   * @param args
   * @throws Exception 
   */
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    PathwayEnrichment e = new PathwayEnrichment("mmu",new ProgressBar(0));
    
    // Random Enrichment
//    Collection<Integer> geneList = Arrays.asList(76884, 226781, 216233, 449000, 20848, 17281, 214897, 18117, 67016, 11990, 27418, 17846, 170728, 243272, 93681, 269774, 17129, 215653, 107239, 12419, 19730, 327959, 16502, 51897, 16011, 228357, 104348, 12616, 66868, 109689, 234267, 18789, 216760, 71508, 320184, 56324, 66687, 104174, 170439, 12387, 239447, 23792, 68010, 268860, 13082, 218442, 216456, 239447, 239447, 239447, 11941, 234267, 20617, 17064, 71703, 20855, 239447, 104174, 11846, 14560, 217082, 94040, 11639, 223881, 239447, 14007, 54610, 228071, 16658, 12014, 239447, 18595, 67475, 21912, 320165, 239447, 239447, 19017, 13082, 18595, 22221, 14057, 74206, 73251, 20893, 18027, 16911, 74148, 14634, 330409, 18542, 11826, 56363, 239447, 67468, 433938, 70611, 56468, 215789, 327826, 15191, 243548, 69632, 272027, 18751, 104174, 11855, 80892, 12753, 79235, 93690, 320311, 228491, 230700, 229759, 217371, 64075, 68817, 68465, 17132, 104174, 12032, 245572, 12638, 22415, 14377, 12226, 320924, 213988, 114615, 320538, 226442, 225631, 109594, 77018, 14660, 207212, 230233, 52679, 231769, 353187, 433693, 328949, 241568, 217082, 213491, 231999, 55994, 99375, 70571, 15245, 18488, 109205, 56392, 100017, 12226, 65962, 22762, 18193, 55980, 12145, 67886, 18186, 13593, 26422, 14451, 75901, 18072, 104099, 239447, 239555, 13831, 71777, 217039, 22589, 12156, 236511, 68107, 56809, 19211, 381695, 229759, 11906, 20269, 14348, 70097, 20822, 52348, 230379, 13982, 140486, 226255, 225283, 53614, 227325, 17536, 70900, 54610, 60611, 106143, 76366, 320541, 16443, 21780, 216965, 73379, 27386, 14823, 245622, 16001, 13846, 17933, 494504, 100710, 69257, 211255, 269275, 60532, 12934, 71834, 72033, 53860, 19267, 230753, 16878);
//    List<EnrichmentObject> en = e.getEnrichedPathways(geneList, IdentifierType.GeneID);
    
    // Read mRNA
    Species species = Species.search((List<Species>)Species.loadFromCSV("species.txt"), "mouse", -1);
    mRNAReader r = new mRNAReader(3, IdentifierType.GeneID, species);
    r.addSecondIdentifier(1, IdentifierType.Symbol);
    r.addAdditionalData(0, "probe_name");
    r.addAdditionalData(2, "description");
    r.addSignalColumn(27, SignalType.FoldChange, "Ctnnb1"); // 27-30 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    r.addSignalColumn(31, SignalType.pValue, "Ctnnb1"); // 31-34 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    r.setProgressBar(e.prog);
    
    // Sort by fold change
    List<mRNA> c = new ArrayList<mRNA>(r.read("mRNA_data_new.txt"));
    Collections.sort(c, Signal.getComparator("Ctnnb1", SignalType.FoldChange));
    
    // Get all fold changes below -1.7
    List<mRNA> geneList = new ArrayList<mRNA>();
    for (int i=0; i<c.size(); i++) {
      if (c.get(i).getSignal(SignalType.FoldChange, "Ctnnb1").getSignal().floatValue() > -1.7) break;
      geneList.add(c.get(i));
    }
    
    System.out.println("PW Enrichment on " + geneList.size() + " genes.");
    List<EnrichmentObject> en = e.getEnrichedPathways(geneList);
    
    System.out.println(en.toString().replace("]], [", "]]\n["));
  }
  
}

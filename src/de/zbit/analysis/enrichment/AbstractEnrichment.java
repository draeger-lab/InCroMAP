/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.analysis.enrichment;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.gui.IntegratorGUITools;
import de.zbit.gui.miRNAandTarget;
import de.zbit.mapper.AbstractMapper;
import de.zbit.mapper.MappingUtils;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.mapper.enrichment.EnrichmentMapper;
import de.zbit.math.BenjaminiHochberg;
import de.zbit.math.Correction;
import de.zbit.math.EnrichmentPvalue;
import de.zbit.math.HypergeometricTest;
import de.zbit.parser.Species;
import de.zbit.util.AbstractProgressBar;

/**
 * Abstract enrichment class to test a list of genes for enrichments
 * (e.g., PathwayEnrichment, GO-Term enrichments, ...).
 * 
 * @author Clemens Wrzodek
 *
 * @param <EnrichIDType> The identifier type of your enrichment terms.
 * This is mostly a simply string.
 */
public abstract class AbstractEnrichment <EnrichIDType> {
  public static final transient Logger log = Logger.getLogger(AbstractEnrichment.class.getName());
  
  /**
   * Mapping from GeneID 2 Enrichment class ids (e.g., KEGG Pathway ID) 
   */
  protected EnrichmentMapper<Integer, EnrichIDType> geneID2enrich_ID=null;
    
  /**
   * Mapping from Enrichment class ID to Name (e.g. KEGG Pathway Name)
   */
  protected AbstractMapper<EnrichIDType, String> enrich_ID2Name=null;
  
  /**
   * pValue 2 qValue FDR correction method to correct pValues
   * for multiple testing.
   */
  private Correction qVal = new BenjaminiHochberg();
  
  /**
   * Progress Bar (mainly for downloading and reading mapping flat files).
   */
  protected AbstractProgressBar prog;
  
  /**
   * The {@link Species}
   */
  protected Species species;

  /**
   * Create a new enrichment analysis.
   * @param geneID2enrich_ID see {@link #geneID2enrich_ID}
   * @param enrich_IDID2Name see {@link #enrich_ID2Name}
   * @param spec see {@link #species}
   * @param prog see {@link #prog}
   * @throws IOException thrown, if the species of your mappers don't match,
   * or if one of the mappers is null and the application could not initialize
   * the mapper, because of an {@link IOException}.
   */
  public AbstractEnrichment(EnrichmentMapper<Integer, EnrichIDType> geneID2enrich_ID, AbstractMapper<EnrichIDType, String> enrich_IDID2Name, Species spec, AbstractProgressBar prog) throws IOException {
    super();
    this.geneID2enrich_ID = geneID2enrich_ID;
    this.enrich_ID2Name = enrich_IDID2Name;
    this.prog = prog;
    this.species = spec;
    
    // Eventually initialize null variables
    initializeEnrichmentMappings();
  }
  /** @see #AbstractEnrichment(EnrichmentMapper, AbstractMapper, Species, AbstractProgressBar)
   */
  public AbstractEnrichment(EnrichmentMapper<Integer, EnrichIDType> geneID2enrich_ID, AbstractMapper<EnrichIDType, String> enrich_IDID2Name, AbstractProgressBar prog) throws IOException {
    this(geneID2enrich_ID, enrich_IDID2Name, null, prog);
  }
  /** @see #AbstractEnrichment(EnrichmentMapper, AbstractMapper, Species, AbstractProgressBar)
   */
  public AbstractEnrichment(Species spec, AbstractProgressBar prog) throws IOException {
    this(null,null, spec, prog);
  }
  /** @see #AbstractEnrichment(EnrichmentMapper, AbstractMapper, Species, AbstractProgressBar)
   */
  public AbstractEnrichment(EnrichmentMapper<Integer, EnrichIDType> geneID2enrich_ID, AbstractMapper<EnrichIDType, String> enrich_IDID2Name) throws IOException {
    this (geneID2enrich_ID,enrich_IDID2Name, null);
  }
  /** @see #AbstractEnrichment(EnrichmentMapper, AbstractMapper, Species, AbstractProgressBar)
   */
  public AbstractEnrichment(Species spec) throws IOException {
    this (spec, null);
  }
  /** @see #AbstractEnrichment(EnrichmentMapper, AbstractMapper, Species, AbstractProgressBar)
   */
  public AbstractEnrichment(EnrichmentMapper<Integer, EnrichIDType> geneID2enrich_ID, AbstractProgressBar prog) throws IOException {
    this(geneID2enrich_ID,null,prog);
  }
  /** @see #AbstractEnrichment(EnrichmentMapper, AbstractMapper, Species, AbstractProgressBar)
   */
  public AbstractEnrichment(EnrichmentMapper<Integer, EnrichIDType> geneID2enrich_ID) throws IOException {
    this(geneID2enrich_ID,null,null);
  }
  
  /**
   * Initialize the {@link #geneID2enrich_ID} and {@link #enrich_sourceID2enrichment_class_id} mappings.
   * <p>This method should
   * <ul><li>Check if one of the two mappings is <code>null</code> and if so, initialize the mapping
   * <li>If initializing a new mapping, the ProgressBar {@link #prog} should be used
   * <li>If initializing a new mapping, the {@link #species} should be used
   * <li>Eventually check if all mappings are compatible (e.g., mappings for
   * the same species) and throw an {@link IOException} if not
   * </ul></p>
   * @throws IOException
   */
  protected abstract void initializeEnrichmentMappings() throws IOException;
  
  /**
   * @return a human readable name for this enrichment type.
   */
  public abstract String getName();
  
  /**
   * Maps all given genes to a enrichment object (e.g., pathway) centered view.
   * 
   * 
   * <p>The Type of the returned {@link Map#values()} depends on the type of the input geneList.
   * If your input list consists of {@link mRNA}, the {@link Map#values()} will also contain
   * {@link mRNA}s, else it will always contain {@link Integer}s, representing the Gene ID!
   * 
   * @param <T> A type that is mappable to GeneID (speciefied by idType).
   * @param geneList
   * @param idType
   * @return a mapping from EnrichedObjects (e.g., Pathways) to [NameAndSignals-List (if available) or Integer (GeneIDs)].
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <T> Map<EnrichIDType, Set<?>> getContainedEnrichments(Collection<T> geneList, IdentifierType idType) {

    // Initialize mapper from InputID to GeneID
    AbstractMapper<String, Integer> mapper=null;
    if (idType!=null && !idType.equals(IdentifierType.NCBI_GeneID)) {
      try {
        mapper = MappingUtils.initialize2GeneIDMapper(idType, prog, species);
      } catch (IOException e) {
        log.log(Level.WARNING, "Could not read mapping file to map your gene identifiers to Entrez GeneIDs.", e);
        return null;
      }
    }
    
    // Mapping from (e.g., Pathway) 2 Genes from geneList contained in this pathway.
    Map<EnrichIDType, Set<?>> enrichClass2Genes = new HashMap<EnrichIDType, Set<?>>();
    for(T gene: geneList) {
      
      // Get Entrez gene ID of gene
      Collection<Integer> geneIDs = new LinkedList<Integer>();
      Collection<NameAndSignals> mr = new LinkedList<NameAndSignals>();
      if (mapper != null){
        try {
          geneIDs.add(mapper.map(gene.toString()));
        } catch (Exception e) {
          log.log(Level.WARNING, "Could not map " + gene, e);
          continue;
        }
      } else if (idType!=null && idType.equals(IdentifierType.NCBI_GeneID)) {
        geneIDs.add(Integer.parseInt(gene.toString()));
      } else { //mRNA miRNA EnrichmentObject and such...
        geneIDs.addAll(NameAndSignals.getGeneIds(gene));
        mr.addAll(NameAndSignals.getGenes(gene));
      }
      
      if (!checkGeneIDs(geneIDs)) {
        log.log(Level.WARNING, "Could not get Entrez Gene ID for " + gene);
        continue;
      }
      
      // Add each geneID to pathway
      for (Integer geneID: geneIDs) {
        if (geneID==null || geneID.intValue()<1) continue;
        
        // Get pathways, in which this gene is contained
        Collection<EnrichIDType> pws=null;
        try {
          // Map Gene_id id 2 pathways in which this gene is contained
          pws = geneID2enrich_ID.map(geneID);
        } catch (Exception e) {
          log.log(Level.WARNING, "Could not get Enrichment objects for " + geneID, e);
        }
        
        // Add to list
        if (pws!=null && pws.size()>0) {
          for (EnrichIDType pw : pws) {
            // Ensure that PW is in our map
            Set pwGenes = enrichClass2Genes.get(pw);
            if (pwGenes==null) {
              if (mr!=null && mr.size()>0) {
                pwGenes = new HashSet<NameAndSignals>();
              } else {
                pwGenes = new HashSet<Integer>();
              }
              enrichClass2Genes.put(pw, pwGenes);
            }
            
            // Add current gene to pw list
            if (mr!=null && mr.size()>0) {
              mr = getSingleTarget(mr, geneID);
              pwGenes.addAll(mr);
            } else {
              pwGenes.add(geneID);
            }
          }
        }
      }
    }
    
    return enrichClass2Genes;
  }
  
  /**
   * Convert lists of {@link mRNA}s to single {@link miRNAandTarget} relations.
   * This allows to display the actual target that cased the pathway to popup
   * in the enrichment.
   * <p>For any other datatype, this method simply returns the input list.
   * @param mr
   * @return
   */
  private Collection<NameAndSignals> getSingleTarget(Collection<NameAndSignals> mr, int geneID) {
    boolean inkonsistency = false;
    if (mr!=null && mr.size()>0) {
      Iterator<NameAndSignals> it = mr.iterator();
      Collection<NameAndSignals> mrNew = new LinkedList<NameAndSignals>();
      while (it.hasNext()) {
        Object o = it.next();
        if (o instanceof miRNA && !(o instanceof miRNAandTarget)) {
          miRNAtarget t = ((miRNA)o).getTarget(geneID);
          if (t!=null) {
            mrNew.add(new miRNAandTarget(((miRNA)o).getName(), t));
          } else {
            // Should actually never happen...
            inkonsistency=true;
          }
        } else {
          // our list is of mixed type. dont't change it!
          inkonsistency=true;
        }
        if (inkonsistency) break;
      }
      if (!inkonsistency && mrNew.size()>0) {
        // We could convert all miRNAs to single miRNA and target relations.
        mr = mrNew;
      }
    }
    return mr;
  }
  /**
   * @param geneID
   * @return true if and only if the list contains at least one valid
   * geneID.
   */
  public static boolean checkGeneIDs(Collection<Integer> geneID) {
    if (geneID==null || geneID.size()<1) return false;
    for (Integer i: geneID) {
      if (i!=null && i.intValue()>0) return true;
    }
    return false;
  }
  
  /**
   * Returns enriched classes (e.g., pathways). If you have an array of genes, please use
   * {@link Arrays#asList(Object...)} 
   * <p>Note: {@link mRNA}s without {@link mRNA#getGeneID()} are NOT being
   * removed and NOT ignored. Thus, they are counted to the totalGeneList
   * size and have an influence on the pValue. If you remove all genes / probes
   * that have no assigned geneID, you might get better pValues !
   * @param <T>
   * @param geneList
   * @return
   */
  public <T> List<EnrichmentObject<EnrichIDType>> getEnrichments(Collection<mRNA> geneList) {
    return getEnrichments(geneList, null);
  }
  
  /**
   * Returns enriched classes (e.g., pathways). If you have an array of genes, please use
   * {@link Arrays#asList(Object...)}
   * @param <T>
   * @param geneList
   * @param idType
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <T> List<EnrichmentObject<EnrichIDType>> getEnrichments(Collection<T> geneList, IdentifierType idType) {
    
    // We have to take the gene lists in EnrichmentObjects, thus
    // the geneList is no more of type T.
    Collection geneList2 = geneList;
    if (EnrichmentObject.isEnrichmentObjectList(geneList)) {
      // Merge gene lists to collection of unique objects.
      geneList2 = EnrichmentObject.mergeGeneLists((Iterable<EnrichmentObject>) geneList);
    }
    
    // Gene center input list (e.g., list of probes).
    // Else, it may occur that you have more genes in a pathway that this pathway contains and
    // Hypergeometric test then returns 0 which is an obvious fault.
    if (NameAndSignals.isNameAndSignals(geneList2)) {
      log.info("Gene centering input list...");
      // Remark: "geneCentered()" Converts miRNAandTargets to miRNAs!
      // MergeType does NOT make any difference, because signals of input data are not processed
      Collection newList = NameAndSignals.geneCentered((Collection<? extends NameAndSignals>)geneList2, IntegratorGUITools.getMergeTypeSilent());
      if (newList!=null && newList.size()>0) {
        geneList2 = newList;
      }
    }
    
    // Map enriched objects on gene list
    Map<EnrichIDType, Set<?>> pwList = getContainedEnrichments(geneList2, idType);
    
    // Init the enriched id 2 readable name mapping (e.g. Kegg Pathway ID 2 Kegg Pathway Name mapping)
    if (enrich_ID2Name==null) {
      enrich_ID2Name = getDefaultEnrichmentID2NameMapping();
    }
    
    // Initialize pValue calculations and ProgressBar
    int geneListSize = geneList2.size();
    EnrichmentPvalue pval = new HypergeometricTest(geneID2enrich_ID.getGenomeSize(), geneListSize);
    if (prog!=null) {
      prog.reset();
      prog.setNumberOfTotalCalls(pwList.size());
    }
    
    // Create EnrichmentObjects
    List<EnrichmentObject<EnrichIDType>> ret = new LinkedList<EnrichmentObject<EnrichIDType>>();
    for (Map.Entry<EnrichIDType, Set<?>> entry : pwList.entrySet()) {
      if (prog!=null) prog.DisplayBar();
      
      // KEGG Pathway id 2 Pathway Name
      String pw_name=entry.getKey().toString();
      if (enrich_ID2Name!=null && enrich_ID2Name.isReady()) {
        try {
          pw_name = enrich_ID2Name.map(entry.getKey());
          if (pw_name==null||pw_name.length()<1) {
            pw_name = "Unknown";//entry.getKey().toString();
          }
        } catch (Exception e) {
          if (enrich_ID2Name!=null) {
            log.log(Level.WARNING, String.format("Could not map Enrichment id 2 name: %s", entry.getKey()), e);
          }
        }
      }
      
      // Total # genes in pw
      int pwSize=geneID2enrich_ID.getEnrichmentClassSize(entry.getKey());
      
      // Create result object
      EnrichmentObject<EnrichIDType> o = new EnrichmentObject<EnrichIDType>(pw_name,entry.getKey(),
          entry.getValue().size(), geneListSize, pwSize, geneID2enrich_ID.getGenomeSize(),
          pval, entry.getValue());
      ret.add(o);
    }
    
    // Correct pValues
    if (ret.size()>0 && qVal!=null) {
      qVal.setQvalue(ret);
    }
    
    // Initially sort returned list by pValue
    Collections.sort(ret, Signal.getComparator(NameAndSignals.defaultExperimentName, SignalType.pValue));
    
    return ret;
  }
  
  /**
   * Couns the unqie genes in a gene list, based on the NCBI Gene id.
   * @param <T> either {@link mRNA} or {@link Integer} (gene ids)
   * @param geneList
   * @return number of unique genes in the list.
   */
  @SuppressWarnings("rawtypes")
  public static Set<Integer> getUniqueGeneIDs(Iterable geneList) {
    /*Set<Integer> unique = new HashSet<Integer>();
    for (Object o : geneList) {
      if (o instanceof Iterable) {
        unique.addAll(getUniqueGeneIDs((Iterable) o));
      } else if (o instanceof mRNA) {
        unique.add(((mRNA)o).getGeneID());
      } else if (o instanceof Integer) {
        unique.add((Integer)o);
      } else {
        log.info("Please implement 2GeneId enrichment counter for " + o.getClass());
      }
    }
    return unique;
    */
    return (Set<Integer>) NameAndSignals.getGeneIds(geneList);
  }
  
  
  /**
   * Create a new mapper to map Enrichment IDs (e.g., KEGG Pathway IDs) to
   * Names (e.g., actual human readable name of the pathway).
   * @see #setEnrichmentID2NameMapping(AbstractMapper)
   * @see #enrich_ID2Name
   * @return a mapper from Enrichment identifier (e.g., "GO:01234" or "path:hsa00214")
   * to human reabable names (e.g., "Glycolysis").
   */
  protected abstract AbstractMapper<EnrichIDType, String> getDefaultEnrichmentID2NameMapping();
  
  /**
   * Set the mapper to map from enrichment object id (e.g., kegg pathway id)
   * to a human reable description (e.g., "Glycolysis").
   * <p>This is an alternate method to not having to create this mapping with
   * {@link #getDefaultEnrichmentID2NameMapping()}, if it is already available.
   * @param map
   */
  public void setEnrichmentID2NameMapping(AbstractMapper<EnrichIDType, String> map) {
    enrich_ID2Name = map;
  }
  
  /**
   * Set a new FDR {@link Correction} method.
   * @param c
   */
  public void setFDRCorrectionMethod(Correction c) {
    this.qVal = c;
  }
  
}

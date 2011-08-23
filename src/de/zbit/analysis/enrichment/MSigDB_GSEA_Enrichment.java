/**
 * @author Clemens Wrzodek
 */
package de.zbit.analysis.enrichment;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.zbit.data.EnrichmentObject;
import de.zbit.gui.GUITools;
import de.zbit.mapper.AbstractMapper;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.mapper.enrichment.GeneID2MSigDB_Mapper;
import de.zbit.math.Bonferroni;
import de.zbit.parser.Species;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.ProgressBar;

/**
 * Allows to perform enrichment analyzes of any data set from
 * the MSigDB (Molecular Signatures Database - see 
 * <a href=http://www.broadinstitute.org/gsea/>
 * http://www.broadinstitute.org/gsea/</a>). This has been
 * tested with version 3.0.
 * 
 * <p>This class is interactive. It will let the user select a file
 * OR enter a URL of any GMT file with either gene
 * symbols or entrez gene IDs as input file. The class will
 * automatically redirect to an url that requires no login, or
 * will take the file directly, if it is on disk or directly
 * downloadable.
 * 
 * <p>See http://www.broadinstitute.org/gsea/msigdb/collections.jsp#C1
 * for a list and download links for gene sets AND
 * http://www.broadinstitute.org/cancer/software/gsea/wiki/index.php/License_info
 * for license information.
 *  
 * @author Clemens Wrzodek
 */
public class MSigDB_GSEA_Enrichment extends AbstractEnrichment<String>  {

  /**
   * @param spec
   * @param prog
   * @throws IOException
   */
  public MSigDB_GSEA_Enrichment(Species spec, AbstractProgressBar prog) throws IOException {
    super(spec, prog);
  }
  
  /**
   * @param spec
   * @throws IOException
   */
  public MSigDB_GSEA_Enrichment(Species spec) throws IOException {
    super(spec);
  }
  

  /* (non-Javadoc)
   * @see de.zbit.analysis.enrichment.AbstractEnrichment#getDefaultEnrichmentID2NameMapping()
   */
  @Override
  protected AbstractMapper<String, String> getDefaultEnrichmentID2NameMapping() {
    return null;
  }

  /* (non-Javadoc)
   * @see de.zbit.analysis.enrichment.AbstractEnrichment#initializeEnrichmentMappings()
   */
  @Override
  protected void initializeEnrichmentMappings() throws IOException {
    String GSEA_Symbol_File = GUITools.openFileDialog(null,"Please select any " + species.getCommonName().toLowerCase() + " GMT file from the MSigDB.",true);
    if (GSEA_Symbol_File==null) throw new IOException("No valid file has been selected.");
    geneID2enrich_ID = new GeneID2MSigDB_Mapper(GSEA_Symbol_File, species, prog);
  }

  /**
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    Species species = new Species("Mus musculus", "_MOUSE", "mouse", "mmu", 10090);
    // Use, e.g., "http://www.broadinstitute.org/gsea/resources/msigdb/3.0/c3.tft.v3.0.symbols.gmt"
    MSigDB_GSEA_Enrichment e = new MSigDB_GSEA_Enrichment(species,new ProgressBar(0));
    System.out.println("Reading file completed.");
    
    // Random Enrichment
    Collection<Integer> geneList = Arrays.asList(76884, 226781, 216233, 449000, 20848, 17281, 214897, 18117, 67016, 11990, 27418, 17846, 170728, 243272, 93681, 269774, 17129, 215653, 107239, 12419, 19730, 327959, 16502, 51897, 16011, 228357, 104348, 12616, 66868, 109689, 234267, 18789, 216760, 71508, 320184, 56324, 66687, 104174, 170439, 12387, 239447, 23792, 68010, 268860, 13082, 218442, 216456, 239447, 239447, 239447, 11941, 234267, 20617, 17064, 71703, 20855, 239447, 104174, 11846, 14560, 217082, 94040, 11639, 223881, 239447, 14007, 54610, 228071, 16658, 12014, 239447, 18595, 67475, 21912, 320165, 239447, 239447, 19017, 13082, 18595, 22221, 14057, 74206, 73251, 20893, 18027, 16911, 74148, 14634, 330409, 18542, 11826, 56363, 239447, 67468, 433938, 70611, 56468, 215789, 327826, 15191, 243548, 69632, 272027, 18751, 104174, 11855, 80892, 12753, 79235, 93690, 320311, 228491, 230700, 229759, 217371, 64075, 68817, 68465, 17132, 104174, 12032, 245572, 12638, 22415, 14377, 12226, 320924, 213988, 114615, 320538, 226442, 225631, 109594, 77018, 14660, 207212, 230233, 52679, 231769, 353187, 433693, 328949, 241568, 217082, 213491, 231999, 55994, 99375, 70571, 15245, 18488, 109205, 56392, 100017, 12226, 65962, 22762, 18193, 55980, 12145, 67886, 18186, 13593, 26422, 14451, 75901, 18072, 104099, 239447, 239555, 13831, 71777, 217039, 22589, 12156, 236511, 68107, 56809, 19211, 381695, 229759, 11906, 20269, 14348, 70097, 20822, 52348, 230379, 13982, 140486, 226255, 225283, 53614, 227325, 17536, 70900, 54610, 60611, 106143, 76366, 320541, 16443, 21780, 216965, 73379, 27386, 14823, 245622, 16001, 13846, 17933, 494504, 100710, 69257, 211255, 269275, 60532, 12934, 71834, 72033, 53860, 19267, 230753, 16878);
    e.setFDRCorrectionMethod(new Bonferroni(geneList.size()));
    List<EnrichmentObject<String>> en = e.getEnrichments(geneList, IdentifierType.NCBI_GeneID);
    
    System.out.println(en.toString().replace("]], [", "]]\n["));
  }

  /* (non-Javadoc)
   * @see de.zbit.analysis.enrichment.AbstractEnrichment#getName()
   */
  @Override
  public String getName() {
    return "MSigDB Enrichment";
  }

  
}

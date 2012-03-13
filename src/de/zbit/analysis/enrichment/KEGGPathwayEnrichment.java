/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/Integrator> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.analysis.enrichment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.io.mRNAReader;
import de.zbit.mapper.AbstractMapper;
import de.zbit.mapper.KeggPathwayID2PathwayName;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.mapper.enrichment.GeneID2ListOfKEGGpathways;
import de.zbit.util.Species;
import de.zbit.util.progressbar.AbstractProgressBar;
import de.zbit.util.progressbar.ProgressBar;

/**
 * Identifies enriched KEGG Pathways in a list of genes.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class KEGGPathwayEnrichment extends AbstractEnrichment<String> { 

  /**
   * @param spec
   * @param prog
   * @throws IOException
   */
  public KEGGPathwayEnrichment(Species spec, AbstractProgressBar prog) throws IOException {
    super(spec, prog);
  }
  
  /**
   * @param spec
   * @throws IOException
   */
  public KEGGPathwayEnrichment(Species spec) throws IOException {
    super(spec);
  }
  
  /**
   * @param mapper
   * @param prog
   * @throws IOException
   */
  public KEGGPathwayEnrichment(GeneID2ListOfKEGGpathways mapper, AbstractProgressBar prog) throws IOException {
    super(mapper, prog);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.analysis.enrichment.AbstractEnrichment#getDefaultEnrichmentID2NameMapping()
   */
  @Override
  protected AbstractMapper<String, String> getDefaultEnrichmentID2NameMapping() {
    KeggPathwayID2PathwayName pwID2name_mapper=null;
    try {
      pwID2name_mapper = new KeggPathwayID2PathwayName(prog);
    } catch (IOException e) { // not severe, will leave the id field blank.
      log.log(Level.WARNING, "Could not read KEGG pathway mapping file.", e);
    }
    return pwID2name_mapper;
  }

  /* (non-Javadoc)
   * @see de.zbit.analysis.enrichment.AbstractEnrichment#initializeEnrichmentMappings()
   */
  @Override
  protected void initializeEnrichmentMappings() throws IOException {
    if (geneID2enrich_ID==null) {
      geneID2enrich_ID = new GeneID2ListOfKEGGpathways(species, prog);
    }
    else if (species!=null && geneID2enrich_ID!=null) {
      String keggAbbr = ((GeneID2ListOfKEGGpathways)geneID2enrich_ID).getSpeciesKEGGabbreviation();
      if (!keggAbbr.equals(species.getKeggAbbr())) {
        log.log(Level.WARNING, String.format("Incompatible Species in pathway enrichment: %s and %s %s", 
          keggAbbr, species.getKeggAbbr(), species));
      }
    }
  }

  /**
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    //Species species = Species.search((List<Species>)Species.loadFromCSV("species.txt"), "mouse", -1);
    Species species = new Species("Mus musculus", "_MOUSE", "Mouse", "mmu", 10090);
    
    KEGGPathwayEnrichment e = new KEGGPathwayEnrichment(species,new ProgressBar(0));
    
    // Random Enrichment
//    Collection<Integer> geneList = Arrays.asList(76884, 226781, 216233, 449000, 20848, 17281, 214897, 18117, 67016, 11990, 27418, 17846, 170728, 243272, 93681, 269774, 17129, 215653, 107239, 12419, 19730, 327959, 16502, 51897, 16011, 228357, 104348, 12616, 66868, 109689, 234267, 18789, 216760, 71508, 320184, 56324, 66687, 104174, 170439, 12387, 239447, 23792, 68010, 268860, 13082, 218442, 216456, 239447, 239447, 239447, 11941, 234267, 20617, 17064, 71703, 20855, 239447, 104174, 11846, 14560, 217082, 94040, 11639, 223881, 239447, 14007, 54610, 228071, 16658, 12014, 239447, 18595, 67475, 21912, 320165, 239447, 239447, 19017, 13082, 18595, 22221, 14057, 74206, 73251, 20893, 18027, 16911, 74148, 14634, 330409, 18542, 11826, 56363, 239447, 67468, 433938, 70611, 56468, 215789, 327826, 15191, 243548, 69632, 272027, 18751, 104174, 11855, 80892, 12753, 79235, 93690, 320311, 228491, 230700, 229759, 217371, 64075, 68817, 68465, 17132, 104174, 12032, 245572, 12638, 22415, 14377, 12226, 320924, 213988, 114615, 320538, 226442, 225631, 109594, 77018, 14660, 207212, 230233, 52679, 231769, 353187, 433693, 328949, 241568, 217082, 213491, 231999, 55994, 99375, 70571, 15245, 18488, 109205, 56392, 100017, 12226, 65962, 22762, 18193, 55980, 12145, 67886, 18186, 13593, 26422, 14451, 75901, 18072, 104099, 239447, 239555, 13831, 71777, 217039, 22589, 12156, 236511, 68107, 56809, 19211, 381695, 229759, 11906, 20269, 14348, 70097, 20822, 52348, 230379, 13982, 140486, 226255, 225283, 53614, 227325, 17536, 70900, 54610, 60611, 106143, 76366, 320541, 16443, 21780, 216965, 73379, 27386, 14823, 245622, 16001, 13846, 17933, 494504, 100710, 69257, 211255, 269275, 60532, 12934, 71834, 72033, 53860, 19267, 230753, 16878);
//    e.setFDRCorrectionMethod(new Bonferroni(geneList.size()));
//    List<EnrichmentObject<String>> en = e.getEnrichments(geneList, IdentifierType.GeneID);
    
    // Read mRNA
    mRNAReader r = new mRNAReader(3, IdentifierType.NCBI_GeneID, species);
    r.addSecondIdentifier(1, IdentifierType.GeneSymbol);
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
    List<EnrichmentObject<String>> en = e.getEnrichments(geneList);
    
    System.out.println(en.toString().replace("]], [", "]]\n["));
  }

  /* (non-Javadoc)
   * @see de.zbit.analysis.enrichment.AbstractEnrichment#getName()
   */
  @Override
  public String getName() {
    return "KEGG Pathway Enrichment";
  }

}

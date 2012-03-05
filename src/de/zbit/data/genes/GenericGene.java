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
package de.zbit.data.genes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.zbit.data.NSwithProbesAndRegion;
import de.zbit.io.CSVReader;
import de.zbit.io.GenericGeneReader;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.parser.Species;
import de.zbit.sequence.region.Region;
import de.zbit.sequence.region.SimpleRegion;
import de.zbit.sequence.region.Strand;

/**
 * Generic gene representation, holding name, description, geneID
 * as wenn as {@link Region} information and the {@link Strand}.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class GenericGene extends NSwithProbesAndRegion implements Strand {
  private static final long serialVersionUID = -2455142276393033042L;
  
  /**
   * Key to use for NameAndSignal
   */
  public final static String DescriptionKey = "DESCRIPTION";
  
  /**
   * This is an estimated maximum gene length for any organism (7Mbp).
   * The real values are (currently) as follows:
   * [Species: Homo sapiens] => 5,379,013
   * [Species: Mus musculus] => 4,434,881
   * [Species: Rattus norvegicus] => 1,865,485
   */
  public final static int MAXIMUM_GENE_LENGTH = 7000000;

  /**
   * @param probeName
   * @param geneName
   * @param geneID
   * @param chromosome
   * @param start
   * @param end
   */
  public GenericGene(String geneName, Integer geneID, String chromosome, int start, int end, byte strand) {
    super(null, geneName, geneID, chromosome, start, end);
    unsetProbeName();
    setIsOnReverseStrand(strand<0);
  }

  /* (non-Javadoc)
   * @see de.zbit.sequence.region.Strand#isOnForwardStrand()
   */
  @Override
  public boolean isOnForwardStrand() {
    return !isOnReverseStrand();
  }

  /* (non-Javadoc)
   * @see de.zbit.sequence.region.Strand#isOnReverseStrand()
   */
  @Override
  public boolean isOnReverseStrand() {
    Boolean strand = (Boolean) getData(Strand.strandKey);
    return strand;
  }

  /* (non-Javadoc)
   * @see de.zbit.sequence.region.Strand#isStrandKnown()
   */
  @Override
  public boolean isStrandKnown() {
    Boolean strand = (Boolean) getData(Strand.strandKey);
    return strand!=null;
  }

  /* (non-Javadoc)
   * @see de.zbit.sequence.region.Strand#setIsOnReverseStrand(boolean)
   */
  @Override
  public void setIsOnReverseStrand(boolean onReverse) {
    super.addData(Strand.strandKey, onReverse);
  }
  
  /**
   * 
   * @param desc
   */
  public void setDescription(String desc) {
    super.addData(DescriptionKey , desc);
  }
  
  /**
   * 
   * @return
   */
  public String getDescription() {
    Object o = getData(DescriptionKey);
    return o==null?null:o.toString();
  }
  
  /**
   * Get all genes lying in a certain region. The returned list is sorted
   * according to {@link SimpleRegion#getComparator()}!
   * @param r filter for region (null to get all)
   * @param species
   * @return
   * @throws IOException
   * @throws Exception
   */
  public static List<GenericGene> getAllGenesForRegion(Region r, Species species) throws IOException, Exception {
    // Prepare reader
    GenericGeneReader reader = new GenericGeneReader(2, IdentifierType.NCBI_GeneID, species, 5, 6, 4, 7);
    reader.addSecondIdentifier(1, IdentifierType.GeneSymbol);
    reader.setDescriptionColumn(3);
    reader.setFilterForRegion(r);
    reader.setDontInitializeToGeneIDmapper(true);
    reader.setSupressWarnings(true);
    
    // Read data
    Collection<GenericGene> content = reader.read(getStaticFileForSpecies(species));
    if (content==null || content.size()<1) return new LinkedList<GenericGene>();
    
    // Get as list and sort by region
    List<GenericGene> ret;
    if (content instanceof List) {
      ret = (List<GenericGene>) content;
    } else {
      ret = new ArrayList<GenericGene>(content);
    }
    Collections.sort(ret, SimpleRegion.getComparator());
    
    return ret;
  }

  /**
   * Hard-coded references to flat files contining 
   * genome coordinates for genes.
   * @param species
   * @return
   */
  private static CSVReader getStaticFileForSpecies(Species species) {
    if (species==null || species.getKeggAbbr()==null) {
      log.warning(String.format("Could not get gene TSS positions for unknown species \"%s\".", species==null?"NULL":species));
      return null;
    }
    
    if (species.getKeggAbbr().equals("mmu")) {
      return new CSVReader("mmu_NCBIM37.gz");
    } else if (species.getKeggAbbr().equals("rno")) {
      return new CSVReader("rno_rgsc3.4.gz");
    } else if (species.getKeggAbbr().equals("hsa")) {
      return new CSVReader("hsa_GRCh37.p5.gz");
    } else {
      return null;
    }
  }
  
}

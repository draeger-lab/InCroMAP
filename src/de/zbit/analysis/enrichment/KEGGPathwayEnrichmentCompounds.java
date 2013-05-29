/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2011-2013 by the University of Tuebingen, Germany.
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
import java.util.logging.Level;

import de.zbit.mapper.AbstractMapper;
import de.zbit.mapper.KEGGspeciesAbbreviation;
import de.zbit.mapper.KeggPathwayID2PathwayName;
import de.zbit.mapper.enrichment.CompoundID2ListOfKEGGpathways;
import de.zbit.util.Species;
import de.zbit.util.progressbar.AbstractProgressBar;

/**
 * Identifies enriched KEGG Pathways in a list of compounds.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class KEGGPathwayEnrichmentCompounds extends AbstractEnrichment<String> {
  
  /**
   * This is a small helper that cancels the first call to
   * {@link #initializeEnrichmentMappings()} from the super constructor, because
   * the {@link #additionallyReadGenes} flag has not been set at this time.
   * 
   * <p>
   * The variable is set to <code>true</code> and
   * {@link #initializeEnrichmentMappings()} is called again manually in the
   * constuctor of this class.
   */
  boolean executeInitialization = false;
  
  /**
   * If true, this mapping will read genes AND compounds. If false, only the
   * compounds will be read.
   */
  boolean additionallyReadGenes = false;
  
  /**
   * @param spec
   * @param prog
   * @throws IOException
   */
  public KEGGPathwayEnrichmentCompounds(Species spec, boolean readGenesAndCompounds, AbstractProgressBar prog) throws IOException {
    super(spec, prog);
    additionallyReadGenes = readGenesAndCompounds;
    executeInitialization = true;
    initializeEnrichmentMappings();
  }
  
  /**
   * @param spec
   * @throws IOException
   */
  public KEGGPathwayEnrichmentCompounds(Species spec, boolean readGenesAndCompounds) throws IOException {
    this(spec, readGenesAndCompounds, null);
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
    if (!executeInitialization) {
      return;
    }
    
    if (geneID2enrich_ID==null) {
      /* If we change the following mapping to "new CompoundID2ListOfKEGGpathways(TRUE, species, prog);"
       * we get an enrichment for Compounds AND Genes.
       * Please note that compound IDs must be NEGATIVE (in order to distinguish from geneIDs). 
       */
      geneID2enrich_ID = new CompoundID2ListOfKEGGpathways(additionallyReadGenes, species, prog);
    }
    else if (species!=null && geneID2enrich_ID!=null) {
      String keggAbbr = ((KEGGspeciesAbbreviation)geneID2enrich_ID).getSpeciesKEGGabbreviation();
      if (!keggAbbr.equals(species.getKeggAbbr())) {
        log.log(Level.WARNING, String.format("Incompatible Species in pathway enrichment: %s and %s %s", 
          keggAbbr, species.getKeggAbbr(), species));
      }
    }
  }

  /* (non-Javadoc)
   * @see de.zbit.analysis.enrichment.AbstractEnrichment#getName()
   */
  @Override
  public String getName() {
    return "KEGG Pathway Enrichment for Compounds";
  }

}
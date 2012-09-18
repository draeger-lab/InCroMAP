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
 * Copyright (C) 2011-2012 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.data;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.biopax.paxtools.model.Model;

import de.zbit.biopax.BioPAX2KGML;
import de.zbit.gui.IntegratorUITools;
import de.zbit.kegg.parser.pathway.Entry;
import de.zbit.kegg.parser.pathway.Graphics;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.kegg.parser.pathway.ext.EntryExtended;
import de.zbit.mapper.AbstractMapper;
import de.zbit.mapper.MappingUtils;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.util.DatabaseIdentifiers.IdentifierDatabases;
import de.zbit.util.Species;
import de.zbit.util.progressbar.AbstractProgressBar;

/**
 * A simple wrapper and holder class for BioPAX pathway models.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class BioPAXpathway {
  public static final transient Logger log = Logger.getLogger(BioPAXpathway.class.getName());
  
  /**
   * The BioPAX model
   */
  Model model;
  
  /**
   * The original input file.
   */
  File biopaxFile;
  
  /**
   * The parsed and converted KGML pathway.
   */
  Pathway p = null;
  
  /**
   * 
   * @param biopaxFile
   */
  public BioPAXpathway (File biopaxFile) {
    model = BioPAX2KGML.getModel(biopaxFile.getPath());
    this.biopaxFile = biopaxFile;
  }
  
  /**
   * Creates a list of all pathways that are contained in the {@link #model}.
   * @return
   */
  public List<String> getListOfPathways() {
    return BioPAX2KGML.getListOfPathways(model);
  }

  /**
   * @return the current BioPAX {@link #model}.
   */
  public Model getModel() {
    return model;
  }
  

  /**
   * Get the species for a pathway.
   * 
   * @param p KGML formatted BioPAX pathway (see {@link #getKGMLpathway(String)}).
   * @return the species for the pathway or <code>NULL</code> if it could
   * not be determined (or is not in our list).
   */
  public static Species getSpecies(Pathway p) {
    // 1. try to get directly from attribute
    if (p.isSetOrg()) {
      String keggAbbr = p.getOrg().toLowerCase().trim();
      for (Species s : IntegratorUITools.organisms) {
        if (s.getKeggAbbr().equals(keggAbbr)) {
          return s;
        }
      }
    }
    
    // 2. Check for annotated taxonomy
    Map<IdentifierDatabases, Collection<String>> dbIDs = p.getDatabaseIdentifiers();
    Collection<String> taxon = dbIDs.get(IdentifierDatabases.NCBI_Taxonomy);
    if (taxon!=null && taxon.size()>0 && taxon.iterator().next().trim().length()>0) {
      try {
        int tax = Integer.parseInt(taxon.iterator().next().trim());
        for (Species s : IntegratorUITools.organisms) {
          if (s.getNCBITaxonID().equals(tax)) {
            return s;
          }
        }
      } catch (Exception e) {
        log.log(Level.WARNING, "Could not match Taxon ids.", e);
      }
    }
    
    // Couldn't figure it out...
    return null;
  }


  /**
   * @param pwName a file may contain multiple pathways. Specify the pathway name here.
   * @return
   */
  public Pathway getKGMLpathway(String pwName) {
    Pathway p = null;
    if (pwName == null){
      Collection<Pathway> pathways = BioPAX2KGML.createPathwaysFromModel(model, biopaxFile.getPath(), false, null);
      if (pathways!=null && pathways.size()>0){
          // That's possible because there is no pathway defined to be selected, following we return 
          // just one pathway object!
          p = pathways.iterator().next();
      }
    } else {
      p = BioPAX2KGML.parsePathwayToKEGG(biopaxFile!=null?biopaxFile.getPath():null, pwName, model);
    }
    
    // The converter for some reason replaces spaces with '_'
    // Revert this behaviour
    if (p.isSetEntries()) {
      for (Entry e : p.getEntries()) {
        if (e.hasGraphics()) {
          Graphics g = e.getGraphics();
          if (g.getName()!=null && g.getName().length()>0) {
            g.setName(g.getName().trim().replace('_', ' '));
          }
        }
      }
    }
    
    return p;
  }

  /**
   * Checks if the current pathway contains entrez gene identifiers. If not, it
   * automatically tries to map Ensembl or RefSeq identifiers to entrez gene.
   * 
   * @param p KGML pathway, see {@link #getKGMLpathway(String)}.
   * @param species
   *        required: the species of the pathway.
   * @param progress
   *        optional: used to display the progress of reading and downloading
   *        mapping files.
   * @return <code>TRUE</code> if the file now contains entrez gene identifiers.
   *         <code>FALSE</code> if it contained no entrez ids and no other ids
   *         could be mapped to entrez.
   */
  public static boolean checkForEntrezGeneIDs(Pathway p, Species species, AbstractProgressBar progress) {
    
    // Check availability of identifiers
    boolean containsEntrez = false;
    boolean containsUniProt = false;
    boolean containsEnsembl = false;
    boolean containsRefSeq = false;
    for (Entry e : p.getEntries()) {
      if (e instanceof EntryExtended) {
        EntryExtended ee = (EntryExtended) e;
        containsEntrez |= ee.isSetIdentifierForDatabase(IdentifierDatabases.EntrezGene);
        containsUniProt |= ee.isSetIdentifierForDatabase(IdentifierDatabases.UniProt_AC);
        containsEnsembl |= ee.isSetIdentifierForDatabase(IdentifierDatabases.Ensembl);
        containsRefSeq |= ee.isSetIdentifierForDatabase(IdentifierDatabases.RefSeq);
      }
    }
    
    // Try to map to entrez
    try {
      if (containsUniProt && !containsEntrez) {
        log.info("Could not find entrez gene ids. Trying to map UniProt2entrez.");
        containsEntrez=mapEntryIdentifiersToEntrez(p, IdentifierDatabases.UniProt_AC, species, progress);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, "Could not map UniProt 2 entrez.", e);
    }
    try {
      if (containsEnsembl && !containsEntrez) {
        log.info("Could not find entrez gene ids. Trying to map ensembl2entrez.");
        containsEntrez=mapEntryIdentifiersToEntrez(p, IdentifierDatabases.Ensembl, species, progress);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, "Could not map ensembl 2 entrez.", e);
    }
    try {
      if (containsRefSeq && !containsEntrez) {
        log.info("Could not find entrez gene ids. Trying to map RefSeq2entrez.");
        containsEntrez=mapEntryIdentifiersToEntrez(p, IdentifierDatabases.RefSeq, species, progress);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, "Could not map RefSeq 2 entrez.", e);
    }
    
    // Try to revert original KEGG identifiers
    if (containsEntrez) {
      mapEntryNamesFromEntrezToKEGG();
    }
    
    return containsEntrez;
  }
  

  /**
   * Tries to enrich the annotated identifiers of pathway <code>p</code>
   * {@link Entry}s with EntrezGene idenfiers by mapping contained identifiers
   * from <code>sourceDB</code> to Entrez gene.
   * 
   * @param p
   *        The pathway to enrich
   * @param sourceDB
   *        Source database (either RefSeq or Ensembl).
   * @param species
   *        Required: the species
   * @param progress
   *        Optional: a progress bar, used only for loading the mapping file
   * @return <code>TRUE</code> if and only if at least one entrez gene
   *         identifier could be added.
   * @throws Exception
   */
  private static boolean mapEntryIdentifiersToEntrez(Pathway p, IdentifierDatabases sourceDB, Species species, AbstractProgressBar progress) throws Exception {
    boolean addedEntrez=false;
    
    IdentifierType idType;
    if (sourceDB.toString().startsWith("UniProt")) {
      idType = MappingUtils.IdentifierType.UniProt;
    } else {
      idType = MappingUtils.IdentifierType.valueOf(sourceDB.toString());
    }
    
    AbstractMapper<String, Integer> map = MappingUtils.initialize2GeneIDMapper(idType, progress, species);
    if (map!=null && p.isSetEntries()) {
      for (Entry e : p.getEntries()) {
        if (e instanceof EntryExtended) {
          EntryExtended ee = (EntryExtended) e;
          if (ee.isSetIdentifierForDatabase(sourceDB)) {
            Collection<String> ids = ee.getDatabaseIdentifiers().get(sourceDB);
            if (ids!=null) {
              for (String id: ids) {
                Integer entre = map.map(id);
                if (entre!=null && entre>0) {
                  ee.addDatabaseIdentifier(IdentifierDatabases.EntrezGene, entre.toString());
                  addedEntrez=true;
                }
              }
            }
          }
        }
      }
    }
    return addedEntrez;
  }
  
}

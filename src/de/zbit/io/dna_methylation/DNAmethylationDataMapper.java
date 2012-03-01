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
package de.zbit.io.dna_methylation;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.data.GeneID;
import de.zbit.data.genes.GenericGene;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.gui.prefs.PreferencesDialog;
import de.zbit.parser.Species;
import de.zbit.sequence.region.Region;
import de.zbit.util.prefs.SBPreferences;

/**
 * A mapper that allows mapping of region based data
 * to gene bodies or promoter regions.
 * 
 * <p>Can also be used for other, {@link Region}-based data!
 * 
 * <p>Please use the static creatInstance() methods.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class DNAmethylationDataMapper {
  public static final transient Logger log = Logger.getLogger(DNAmethylationDataMapper.class.getName());
  
  /*
   * OPTIONS
   */
  
  /**
   * If we map to promoters, this is the upstream-of-tss promoter definition.
   */
  private int upstream = 2000;
  /**
   * If we map to promoters, this is the downstream-of-tss promoter definition.
   */
  private int downstream = 500;
  /**
   * If we have multiple overlapping regions (i.e. promoters or gene bodies),
   * the user may want to discard this probe.
   */
  private boolean takeOnlyUniqueHits = false;
  /**
   * Probes, that can not be assigned to any gene just blow-up our dataset but
   * they are essentially uselesse.
   */
  private boolean discardNonAssignableProbes = true;
  /**
   * If true, maps to promoters. Else, maps to gene bodies.
   */
  private boolean mapToPromoters = true;
  
  /*
   * Real internal variables
   */
  
  /**
   * All data will be mapped on these {@link Region}s.
   */
  private List<GenericGene> templateRegions = null;
  /**
   * The maximum length of any region in {@link #templateRegions}.
   */
  private int maximumRegionLength = -1;
  
  
  public void initialize(Species species) {
    // Read all genes and TSS'es
    log.fine("Reading genome coordinates...");
    try {
      templateRegions = GenericGene.getAllGenesForRegion(null, species);
    } catch (Exception e) {
      log.log(Level.SEVERE, "Could not read genome coordinates.", e);
      return;
    }
    
    // Eventually map to promoters; Store maximum size
    if (mapToPromoters) {
      log.fine("Calculating promoter regions...");
      DNAmethIOtools.convertToPromoterRegions(upstream, downstream, templateRegions);
      maximumRegionLength = upstream + downstream;
    } else {
      // Get maximum region size
      for (GenericGene r : templateRegions) {
        maximumRegionLength = Math.max(maximumRegionLength, r.getLength());
      }
    }
    
  }
  
  
  private void readOptionsFromConfiguration() {
    SBPreferences prefs = SBPreferences.getPreferencesFor(DNAmethMapDataToPromoterOptions.class);
    upstream = DNAmethMapDataToPromoterOptions.UPSTREAM.getValue(prefs);
    downstream = DNAmethMapDataToPromoterOptions.DOWNSTREAM.getValue(prefs);;
    takeOnlyUniqueHits = DNAmethMapDataToPromoterOptions.DISCARD_PROBES_WITH_AMBIGUOUS_MAPPING.getValue(prefs);;
    discardNonAssignableProbes = DNAmethMapDataToPromoterOptions.DISCARD_NON_ASSIGNABLE_PROBES.getValue(prefs);
    mapToPromoters = DNAmethMapDataToPromoterOptions.MAP_DATA_TO_GENE_PROMOTERS.getValue(prefs);
  }
  
  /**
   * Shows the {@link DNAmethMapDataToPromoterOptions} in a dialog.
   * @return <code>TRUE</code> if the user approved the dialog
   */
  @SuppressWarnings("unchecked")
  public static boolean showMappingOptionsDialog() {
    //boolean approved = PreferencesDialog.showPreferencesDialog(DNAmethMapDataToPromoterOptions.class);
    PreferencesDialog dialog = new PreferencesDialog("Gene mapping options");
    return dialog.showPrefsDialog(DNAmethMapDataToPromoterOptions.class);    
  }
  
  /**
   * Shows the option dialog {@link #showMappingOptionsDialog()} and
   * sets all internal variables
   * @return <code>TRUE</code> if the user approved the dialog
   */
  public boolean showOptionDialogAndReadMappingConfiguration() {
    boolean approved = showMappingOptionsDialog();
    if (approved) {
      readOptionsFromConfiguration();
    }
    return approved;
  }


  /**
   * Assigns a {@link GeneID} to <code>toMap</code>. 
   * @param toMap
   * @return <code>NULL</code> if this region should be
   * discarded. Else, returnes <code>toMap</code>.
   */
  public DNAmethylation map(DNAmethylation toMap) {
    /*
     * If you have a NullPointerException here, you probably
     * forgot to call "initialize(Species)"!
     */
    
    GenericGene mappedOn = DNAmethIOtools.map(maximumRegionLength, 
      takeOnlyUniqueHits, templateRegions, toMap, mapToPromoters?upstream:0);
    
    // Discard it or set GeneID
    if (mappedOn == null || mappedOn.getGeneID()==GeneID.default_geneID) {
      //toMap.setGeneID(GeneID.default_geneID);
      if (discardNonAssignableProbes) {
        return null;
      }
    } else {
      toMap.setGeneID(mappedOn.getGeneID());
    }
    
    return toMap;
  }


  /**
   * Let's the user choose the options to initialize this mapper
   * and returns a new and correctly configured instance of it. If
   * the user cancels, null is returned.
   * <p><b>NOTE: This returns an UNINITIALIZED mapper. You still
   * have to call the {@link #initialize(Species)} method on it.</b></p>
   * @return
   */
  public static DNAmethylationDataMapper createInstanceWithGUI() {
    DNAmethylationDataMapper mapper = new DNAmethylationDataMapper();
    boolean ret = mapper.showOptionDialogAndReadMappingConfiguration();
    return ret?mapper:null;
  }
  
}

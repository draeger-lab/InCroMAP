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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.data.genes.GenericGene;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.parser.Species;
import de.zbit.sequence.region.AbstractRegion;
import de.zbit.sequence.region.Region;
import de.zbit.sequence.region.SimpleRegion;

/**
 * This class should contain tools that help importing/ reading
 * or preparing DNA methylation data.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class DNAmethIOtools {
  public static final transient Logger log = Logger.getLogger(DNAmethIOtools.class.getName());
  
  /**
   * This method allows to map {@link DNAmethylation} regions (e.g. probes) for which
   * the genomic position is known to promoter regions of genes!
   * 
   * <p><i>May cause a huge memory load! You may want to embed this into a try/catch
   * block, catching {@link java.lang.OutOfMemoryError}.</i></p>
   * 
   * <p>Uses resonable default values for the promoter definition and other input variables.</p> 
   * @param data 
   * @param species
   */
  public static void mapDataToClosestTSS(Collection<DNAmethylation> data, Species species) {
    int upstream = 2000;
    int downstream = 500;
    boolean takeOnlyUniqueHits = false;
    boolean discardNonAssignableProbes = true;
    mapDataToClosestTSS(data, species, upstream, downstream, takeOnlyUniqueHits, discardNonAssignableProbes);
  }
  
  /**
   * This method allows to map {@link DNAmethylation} regions (e.g. probes) for which
   * the genomic position is known to promoter regions of genes!
   * 
   * <p><i>May cause a huge memory load! You may want to embed this into a try/catch
   * block, catching {@link java.lang.OutOfMemoryError}.</i></p> 
   * 
   * @param data
   * @param species
   * @param upstream promoter definition: how many bps upstream of the TSS is the promoter defined
   * @param downstream promoter definition: how many bps downstream of the TSS is the promoter defined
   * @param takeOnlyUniqueHits if a probe overlaps with multiple promoters, should this be discarded?
   * @param discardNonAssignableProbes remove probes, that can not be assigned to any gene from <code>data</code>.
   */
  public static void mapDataToClosestTSS(Collection<DNAmethylation> data, Species species, int upstream, int downstream,
    boolean takeOnlyUniqueHits, boolean discardNonAssignableProbes) {
    
    // Read all genes and TSS'es
    log.info("Reading genome coordinates...");
    List<GenericGene> genesInRegion;
    try {
      genesInRegion = GenericGene.getAllGenesForRegion(null, species);
    } catch (Exception e) {
      log.log(Level.SEVERE, "Could not read genome coordinates.", e);
      return;
    }
    
    convertToPromoterRegions(upstream, downstream, genesInRegion);
    
    log.info("Mapping DNA methylation data to promoter regions...");
    Iterator<DNAmethylation> it = data.iterator();
    while (it.hasNext()) {
      DNAmethylation dnam = it.next();
      //dnam.setGeneID(GeneID.default_geneID);
      
      GenericGene mappedOn = map((upstream+downstream), takeOnlyUniqueHits,
        genesInRegion, dnam, upstream);
      
      if (mappedOn == null ) {
        if (discardNonAssignableProbes) {
          it.remove();
        }
      } else {
        dnam.setGeneID(mappedOn.getGeneID());
      }
    }
    
  }

  /**
   * Map a {@link Region} to any {@link GenericGene} in <code>allRegions</code>. 
   * @param maximumRegionLength maximum length of all regions in <code>allRegions</code>.
   * @param takeOnlyUniqueHits
   * @param allRegions your templates to map the probes on
   * @param toMap probe to map
   * @param upstreamExtension if you have promoters in <code>allRegions</code>,
   * please specify here the upstream (of tss) extension. Else, set this to 0.
   * @return gene, on which <code>toMap</code> has been mapped. Or null
   * if it could not be mapped to any gene.
   */
  public static GenericGene map(int maximumRegionLength,
    boolean takeOnlyUniqueHits,
    List<GenericGene> allRegions,
    Region toMap, int upstreamExtension) {
    // Get intersecting promoter regions
    List<GenericGene> intersects = AbstractRegion.getAllIntersections(allRegions, toMap, false, maximumRegionLength);
    if (intersects!=null & intersects.size()>0) {
      GenericGene mapToThisGene = intersects.get(0);
      if (intersects.size()>1) {
        // Get closest or skip
        if (takeOnlyUniqueHits) {
          return null;
        } else {
          // Assign to gene with closest TSS
          int minDistance = distanceToTSS(mapToThisGene, toMap, upstreamExtension);
          for (int i=1; i<intersects.size(); i++) {
            int distanceToPromoterI = distanceToTSS(intersects.get(i), toMap, upstreamExtension);
            if (distanceToPromoterI<minDistance) {
              mapToThisGene = intersects.get(i);
              minDistance = distanceToPromoterI;
            }
          }
        }
      }
      
      // Assign to this gene
      return mapToThisGene;
    }
    return null;
  }

  /**
   * @param upstream bps upstream of tss
   * @param downstream bps downstream of tss
   * @param genesInRegion all genes to convert to promoter regions
   */
  public static void convertToPromoterRegions(int upstream, int downstream,
    List<GenericGene> genesInRegion) {
    // Convert all gene-body regions to promoter regions
    for (GenericGene g: genesInRegion) {
      int newStart = g.isOnForwardStrand() ? (g.getStart()-upstream) : (g.getEnd() - downstream );
      int newEnd = g.isOnForwardStrand() ? (g.getStart()+downstream) : (g.getEnd() + upstream );
      g.setStart(newStart);
      try {
        g.setEnd(newEnd);
      } catch (Exception e) {
        // We can ignore this here! It's just if we hadn't set the Start position first.
        e.printStackTrace();
      }
    }
    Collections.sort(genesInRegion, SimpleRegion.getComparator());
  }


  /**
   * 
   * @param promoter template
   * @param probe probe for which we want to know the distance to template's TSS
   * @param upstreamExtension upstream extension, relative to TSS, of the promoter. If
   * the represents a gene body, rather than a promoter, set this to 0.
   * @return distance of <code>probe</code> to <code>promoter</code>'s TSS.
   */
  private static int distanceToTSS(GenericGene promoter, Region probe, int upstreamExtension) {
    int TSS = (promoter.isOnForwardStrand() ? (promoter.getStart()+upstreamExtension) : promoter.getEnd() - upstreamExtension );
    
    return Math.abs(probe.getMiddle() - TSS);
  }
  
}

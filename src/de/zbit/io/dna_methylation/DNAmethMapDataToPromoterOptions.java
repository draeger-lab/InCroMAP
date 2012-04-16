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
package de.zbit.io.dna_methylation;

import javax.swing.ButtonGroup;

import de.zbit.data.methylation.DNAmethylation;
import de.zbit.kegg.ext.KEGGTranslatorPanelOptions;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * This class contains options, that might be specified when mapping {@link DNAmethylation}
 * probes to promoter regions . Especially when using 
 * {@link DNAmethIOtools#mapDataToClosestTSS(java.util.Collection, de.zbit.parser.Species, int, int, boolean, boolean)}.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public interface DNAmethMapDataToPromoterOptions extends KeyProvider {
  
  final ButtonGroup geneBodyOrPromoterGroup = new ButtonGroup();
  
  
  public static final Option<Boolean> MAP_DATA_TO_GENE_BODIES = new Option<Boolean>(
      "MAP_DATA_TO_GENE_BODIES", Boolean.class, "Map data to gene bodies.",
      Boolean.FALSE, null, geneBodyOrPromoterGroup);
  
  public static final Option<Boolean> MAP_DATA_TO_GENE_PROMOTERS = new Option<Boolean>(
      "MAP_DATA_TO_GENE_PROMOTERS", Boolean.class, "Map data to gene promoters.",
      Boolean.TRUE, null, geneBodyOrPromoterGroup);  

  public static final Option<Integer> UPSTREAM = new Option<Integer>(
    "UPSTREAM", Integer.class, "Define the promoter region size, upstream of a TSS (in base pairs).",
    new Range<Integer>(Integer.class, "{[1,20000]}"), Integer.valueOf(2000), "Upstream of TSS", 
    MAP_DATA_TO_GENE_PROMOTERS, KEGGTranslatorPanelOptions.TRUE_RANGE);
  
  public static final Option<Integer> DOWNSTREAM = new Option<Integer>(
      "DOWNSTREAM", Integer.class, "Define the promoter region size, downstream of a TSS (in base pairs).",
      new Range<Integer>(Integer.class, "{[1,5000]}"), Integer.valueOf(500), "Downstream of TSS",
      MAP_DATA_TO_GENE_PROMOTERS, KEGGTranslatorPanelOptions.TRUE_RANGE);
  
  public static final Option<Boolean> DISCARD_PROBES_WITH_AMBIGUOUS_MAPPING = new Option<Boolean>(
      "DISCARD_PROBES_WITH_AMBIGUOUS_MAPPING", Boolean.class, "Discard probes that overlap with mutliple promoters.",
      Boolean.FALSE);
  
  public static final Option<Boolean> DISCARD_NON_ASSIGNABLE_PROBES = new Option<Boolean>(
      "DISCARD_NON_ASSIGNABLE_PROBES", Boolean.class, "Remove probes that can not be mapped to any promoter.",
      Boolean.TRUE, "Remove non-assignable probes");
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static final OptionGroup MAP_DATA_TO_PROMOTER_OPTIONS = new OptionGroup(
    "Map data to genes", (String)null, MAP_DATA_TO_GENE_BODIES, MAP_DATA_TO_GENE_PROMOTERS,
    UPSTREAM, DOWNSTREAM, DISCARD_PROBES_WITH_AMBIGUOUS_MAPPING, DISCARD_NON_ASSIGNABLE_PROBES);
  
}

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
package de.zbit.gui.prefs;

import de.zbit.graph.gui.options.TranslatorPanelOptions;
import de.zbit.kegg.KEGGtranslatorOptions;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * Various options required for enrichments.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public interface EnrichmentOptions extends KeyProvider {
  
  public static Option<Boolean> COUNT_MIRNA_TARGETS_FOR_LIST_RATIO = new Option<Boolean>("COUNT_MIRNA_TARGETS_FOR_LIST_RATIO", Boolean.class,
      "If true, counts the number of miRNA targets to calculate the list ratio in enrichments. If false, the number of microRNAs, regardless " +
      "of the number of targets is used to calculate the list ratio (and p-value).", Boolean.FALSE, "Count miRNA targets for list ratio");
  
  public static Option<Boolean> REMOVE_UNINFORMATIVE_TERMS = new Option<Boolean>("REMOVE_UNINFORMATIVE_TERMS", Boolean.class,
      "If true, removes very large GO terms/Pathways.", Boolean.TRUE);
  
  /**
   * Select percentage for removing GO terms.
   */
  public static final Option<Integer> MINIMUM_SIZE_OF_TERMS_TO_REMOVE = new Option<Integer>("MINIMUM_SIZE_OF_TERMS_TO_REMOVE", Integer.class,
      "Removes all enrichment terms (GO, Pathways, etc.) whose size is greater than or equal to this value..",
      new Range<Integer>(Integer.class, "{[0,10000]}"), 400, REMOVE_UNINFORMATIVE_TERMS, TranslatorPanelOptions.TRUE_RANGE);
 
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static final OptionGroup ENRICHMENT_OPTIONS = new OptionGroup(
      "Enrichment options","Select options for gene set enrichments.",
      COUNT_MIRNA_TARGETS_FOR_LIST_RATIO, REMOVE_UNINFORMATIVE_TERMS, MINIMUM_SIZE_OF_TERMS_TO_REMOVE);
  
}

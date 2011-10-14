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
package de.zbit.gui.prefs;

import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;

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
 
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static final OptionGroup ENRICHMENT_OPTIONS = new OptionGroup(
      "Enrichment options","Select options for gene set enrichments.",
      COUNT_MIRNA_TARGETS_FOR_LIST_RATIO);
  
}

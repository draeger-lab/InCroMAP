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
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
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

import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;

/**
 * Options for {@link Signal}s. Currently mainly for merging multiple
 * Signals.
 * 
 * <p>This should be kept simple and only ask the user for the {@link MergeType}.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public interface MergeTypeOptions extends KeyProvider {
  
  /**
   * Select how to merge multiple probes to one.
   */
  public static Option<MergeType> GENE_CENTER_SIGNALS_BY = new Option<MergeType>("GENE_CENTER_SIGNALS_BY", MergeType.class,
      "Select how to merge multiple probes to a gene centric dataset.", MergeType.Mean);

  public static Option<Boolean> REMEMBER_GENE_CENTER_DECISION = new Option<Boolean>("REMEMBER_GENE_CENTER_DECISION", Boolean.class,
      "Remember my decision of how to merge multiple probes and don't ask again.", Boolean.FALSE);
  
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static final OptionGroup GENE_CENTER_OPTIONS = new OptionGroup(
      "Gene center options",(String)null,
      GENE_CENTER_SIGNALS_BY, REMEMBER_GENE_CENTER_DECISION);
  
  
}

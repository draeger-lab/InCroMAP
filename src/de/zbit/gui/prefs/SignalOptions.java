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

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import de.zbit.data.Signal.MergeType;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;

/**
 * Extension for {@link MergeTypeOptions} that includes options to let the
 * user decide when to merge multiple signals or at which depth.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public interface SignalOptions extends KeyProvider {
  

// public static enum DepthChoices implements ActionCommand {
//   PATHWAY, GENE, PROBE;
//   /* (non-Javadoc) 
//    * @see de.zbit.gui.ActionCommand#getName()
//    */
//   public String getName() {
//     switch (this) {
//     case PATHWAY:
//       return "One value per pathway node (pathway centered)";
//     case GENE:
//       return "One value per gene (gene centered)";
//     case PROBE:
//       return "One node per value (probe centered - not recommended)";
//     }
//    return this.toString();
//   }
//
//   /*
//    * (non-Javadoc)
//    * 
//    * @see de.zbit.gui.ActionCommand#getToolTip()
//    */
//   public String getToolTip() {
//     switch (this) {
//       case PATHWAY:
//         return "Merge all probes that belong to one node in a pathway to one value.";
//       case GENE:
//         return "Merge all probes that belong to one gene to one value.";
//       case PROBE:
//         return "Do not merge any probes. This can result in a very confusing graph and is thus not recommended.";
//       default:
//         return this.toString();
//     }
//   }
//   
// }
// 
// 
// public static Option<DepthChoices> MERGE_DEPTH = new Option<DepthChoices>("MERGE_DEPTH", DepthChoices.class,
//   "Select the behaviour if multiple probes should be joined to one.", DepthChoices.GENE,
//   "Select visualization depth");

  
  /**
   * All Boolean options belonging to this group will be converted
   * into {@link JRadioButton}s.
   */
  public static ButtonGroup signalMergeDepth = new ButtonGroup();
  
  public static Option<Boolean> PATHWAY_CENTERED = new Option<Boolean>("PATHWAY_CENTERED", Boolean.class,
      "Merge all probes that belong to one node in a pathway to one value.", Boolean.TRUE,
      "One value per pathway node (pathway centered)", signalMergeDepth, false);
  
  public static Option<Boolean> GENE_CENTERED = new Option<Boolean>("GENE_CENTERED", Boolean.class,
      "Merge all probes that belong to one gene to one value. This can result in a very confusing graph and is thus not recommended.", Boolean.FALSE,
      "One value per gene (gene centered - not recommended)", signalMergeDepth, false);
  
  public static Option<Boolean> PROBE_CENTERED = new Option<Boolean>("PROBE_CENTERED", Boolean.class,
      "Do not merge any probes. This can result in a very confusing graph and is thus not recommended.", Boolean.FALSE,
      "One node per value (probe centered - not recommended)", signalMergeDepth, false);
  
  
 /**
  * Select how to merge multiple probes to one.
  */
 public static Option<MergeType> GENE_CENTER_SIGNALS_BY = new Option<MergeType>("GENE_CENTER_SIGNALS_BY", MergeType.class,
     "Select how to merge multiple probes to a gene centric dataset.", MergeType.Automatic,
     PROBE_CENTERED, Option.buildRange(Boolean.FALSE));
//     MERGE_DEPTH, Option.buildRange(DepthChoices.PATHWAY, DepthChoices.GENE));

 
 public static Option<Boolean> REMEMBER_GENE_CENTER_DECISION = new Option<Boolean>("REMEMBER_GENE_CENTER_DECISION", Boolean.class,
     "Remember my decision of how to merge multiple probes and don't ask again.", Boolean.TRUE,
     GENE_CENTER_SIGNALS_BY.getDependencies());
 
 

 
 @SuppressWarnings({ "unchecked", "rawtypes" })
 public static final OptionGroup GENE_CENTER_OPTIONS = new OptionGroup(
     "Signal merge options","Select the behaviour if multiple probes should be joined to one.",
     false, false,
//     MERGE_DEPTH,
     PATHWAY_CENTERED,GENE_CENTERED,PROBE_CENTERED,
     GENE_CENTER_SIGNALS_BY, REMEMBER_GENE_CENTER_DECISION);
 
}

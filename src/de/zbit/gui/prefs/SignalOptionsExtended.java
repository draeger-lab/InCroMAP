/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui.prefs;

import javax.swing.ButtonGroup;

import de.zbit.data.Signal.MergeType;
import de.zbit.gui.ActionCommand;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;
import de.zbit.util.prefs.Range;

/**
 * Extension for {@link SignalOptions} that includes options to let the
 * user decide when to merge multiple signals or at which depth.
 * 
 * @author Clemens Wrzodek
 */
public interface SignalOptionsExtended extends KeyProvider {
  

 public static enum DepthChoices implements ActionCommand {
   PATHWAY, GENE, PROBE;
   /* (non-Javadoc) 
    * @see de.zbit.gui.ActionCommand#getName()
    */
   public String getName() {
     switch (this) {
     case PATHWAY:
       return "One value per pathway node (pathway centered)";
     case GENE:
       return "One value per gene (gene centered)";
     case PROBE:
       return "One node per value (probe centered - not recommended)";
     }
    return this.toString();
   }

   /*
    * (non-Javadoc)
    * 
    * @see de.zbit.gui.ActionCommand#getToolTip()
    */
   public String getToolTip() {
     switch (this) {
       case PATHWAY:
         return "Merge all probes that belong to one node in a pathway to one value.";
       case GENE:
         return "Merge all probes that belong to one gene to one value.";
       case PROBE:
         return "Do not merge any probes. This can result in a very confusing graph and is thus not recommended.";
       default:
         return this.toString();
     }
   }
   
 }
 
 
 public static Option<DepthChoices> MERGE_DEPTH = new Option<DepthChoices>("MERGE_DEPTH", DepthChoices.class,
   "Select the behaviour if multiple probes should be joined to one..", DepthChoices.GENE,
   "Select visualization depth", new ButtonGroup());

  
  
 /**
  * Select how to merge multiple probes to one.
  */
 public static Option<MergeType> GENE_CENTER_SIGNALS_BY = new Option<MergeType>("GENE_CENTER_SIGNALS_BY", MergeType.class,
     "Select how to merge multiple probes to a gene centric dataset.", MergeType.Mean,
     MERGE_DEPTH, new Range<DepthChoices>(DepthChoices.class, DepthChoices.PATHWAY, DepthChoices.GENE));
 
 
 @SuppressWarnings({ "unchecked", "rawtypes" })
 public static final OptionGroup GENE_CENTER_OPTIONS = new OptionGroup(
     "Gene center options",null,
     MERGE_DEPTH, GENE_CENTER_SIGNALS_BY);//, REMEMBER_GENE_CENTER_DECISION);
 
 
 /*
  * THE FOLLOWING IS TO TEST AND IMPLEMENT THE BUTTONGROUP FUNCTIONALITY
  * TO THE OPTIONS.
  */
 
  /**
   * All Boolean options belonging to this group will be converted
   * into {@link JRadioButton}s.
   */
//  public static ButtonGroup signalMergeDepth = new ButtonGroup();
//  
//  public static Option<Boolean> PATHWAY_CENTERED = new Option<Boolean>("PATHWAY_CENTERED", Boolean.class,
//      "Merge all probes that belong to one node in a pathway to one value.", Boolean.FALSE,
//      "One value per pathway node (pathway centered)", signalMergeDepth);
//  
//  public static Option<Boolean> GENE_CENTERED = new Option<Boolean>("GENE_CENTERED", Boolean.class,
//      "Merge all probes that belong to one gene to one value.", Boolean.TRUE,
//      "One value per gene (gene centered)", signalMergeDepth);
//  
//  public static Option<Boolean> PROBE_CENTERED = new Option<Boolean>("PROBE_CENTERED", Boolean.class,
//      "Do not merge any probes. This can result in a very confusing graph and is thus not recommended.", Boolean.FALSE,
//      "One node per value (probe centered - not recommended)", signalMergeDepth);
  
}

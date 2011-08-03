/**
 * @author Clemens Wrzodek
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
      "Gene center options",null,
      GENE_CENTER_SIGNALS_BY, REMEMBER_GENE_CENTER_DECISION);
  
  
}

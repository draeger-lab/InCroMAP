/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui.prefs;

import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.OptionGroup;

/**
 * Various options required for enrichments.
 * 
 * @author Clemens Wrzodek
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

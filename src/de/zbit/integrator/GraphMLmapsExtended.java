/**
 * @author Clemens Wrzodek
 */
package de.zbit.integrator;

import de.zbit.data.NameAndSignals;
import de.zbit.kegg.ext.GraphMLmaps;

/**
 * Extended maps for GraphML graphs. See {@link GraphMLmaps}.
 * @author Clemens Wrzodek
 */
public interface GraphMLmapsExtended extends GraphMLmaps {
  
  /**
   * True for all nodes, that are cloned to have multiple
   * variants for the same node (e.g., to color nodes
   * according to probes, NOT gene-centered).
   * <p>False for all original nodes and
   * <p><code>NULL</code> for all nodes, that have not
   * been cloned.
   */
  public final static String NODE_IS_COPY = "_isVariant";
  
  /**
   * True for all nodes, that have been added to visualize
   * microRNA target interactions in the graph.
   */
  public static final String NODE_IS_MIRNA = "_isMiRNA";
  
  /**
   * If this node has been created for a {@link NameAndSignals},
   * this map stores the instance that corresponds to this node.
   */
  public static final String NODE_NAME_AND_SIGNALS = "_NameAndSignals";
  
}

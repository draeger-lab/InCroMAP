/**
 * @author Clemens Wrzodek
 */
package de.zbit.integrator;

import de.zbit.data.NameAndSignals;
import de.zbit.kegg.ext.GraphMLmaps;
import de.zbit.util.ValueTriplet;

/**
 * Extended maps for GraphML graphs. See {@link GraphMLmaps}.
 * @author Clemens Wrzodek
 */
public interface GraphMLmapsExtended extends GraphMLmaps {
  /*
   * Remark: a String starting with "_" is hidden in the
   * Observation table when clicked on a node.
   */
  
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

  /**
   * If this node has been colored for a specific signal/observation,
   * this KEY points to a {@link ValueTriplet}, describing this signal
   * with a)tab/filename b)experimentName c)SignalType.
   */
  public static final String NODE_BELONGS_TO = "_VisualizedSignal";

  /**
   * Stores the number of children (ONLY FOR GROUP/FOLDER NODES)
   * this node has. The stored value is an {@link Integer}.
   */
  public static final String NODE_NUMBER_OF_CHILDREN = "ChildNodes";
  
}

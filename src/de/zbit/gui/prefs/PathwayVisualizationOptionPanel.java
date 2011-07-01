/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui.prefs;

import java.io.IOException;

/**
 * Enable an option tab for the {@link PathwayVisualizationOptions}.
 * @author Clemens Wrzodek
 */
public class PathwayVisualizationOptionPanel extends PreferencesPanelForKeyProvider {
  private static final long serialVersionUID = 4574493372860651243L;

  /**
   * @param provider
   * @throws IOException
   */
  public PathwayVisualizationOptionPanel()
    throws IOException {
    super(PathwayVisualizationOptions.class);
  }
  
}

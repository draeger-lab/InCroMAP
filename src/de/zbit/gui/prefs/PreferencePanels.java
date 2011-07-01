/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui.prefs;

/**
 * This class contains a collection of all preferences (classes
 * extending {@link PreferencesPanelForKeyProvider}) that are used
 * in this application.
 * 
 * @author Clemens Wrzodek
 */
public class PreferencePanels {
  public static Class<?>[] getPreferencesClasses() {
    return new Class<?>[]{
        de.zbit.gui.prefs.GeneralOptionPanel.class,
        de.zbit.gui.prefs.MultiplePreferencesPanel.class, 
        de.zbit.gui.prefs.PreferencesPanelForKeyProvider.class, 
        de.zbit.gui.prefs.PathwayVisualizationOptionPanel.class
    };
  }
  
}
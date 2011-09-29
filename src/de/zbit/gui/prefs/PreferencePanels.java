/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui.prefs;

/**
 * This class contains a collection of all preferences (classes
 * extending {@link PreferencesPanelForKeyProvider}) that are used
 * in this application (and should be visible in the GUI).
 * 
 * @author Clemens Wrzodek
 */
public class PreferencePanels {
  public static Class<?>[] getPreferencesClasses() {
    return new Class<?>[]{
        de.zbit.gui.prefs.PathwayVisualizationOptionPanel.class,
        de.zbit.gui.prefs.GeneralOptionPanel.class, // KEGGtranslator options like remove orphans
        de.zbit.gui.prefs.TranslatorPanelOptionPanel.class, // TranslatorPanel Options like show navigation
        
//        de.zbit.gui.prefs.SignalOptionPanel.class // Included in PathwayVisualizationOptionPanel
    };
  }
  
}
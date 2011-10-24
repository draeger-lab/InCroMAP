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

/**
 * This class contains a collection of all preferences (classes
 * extending {@link PreferencesPanelForKeyProvider}) that are used
 * in this application (and should be visible in the GUI).
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
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
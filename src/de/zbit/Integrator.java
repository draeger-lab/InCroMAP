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
package de.zbit;

import java.awt.Window;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.gui.GUIOptions;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.prefs.EnrichmentOptions;
import de.zbit.gui.prefs.IntegratorIOOptions;
import de.zbit.gui.prefs.PathwayVisualizationOptions;
import de.zbit.gui.prefs.SignalOptions;
import de.zbit.kegg.KEGGtranslatorOptions;
import de.zbit.kegg.ext.TranslatorPanelOptions;
import de.zbit.util.prefs.KeyProvider;

/**
 * This class is the main class for the Integrator project.
 * 
 * <p>
 * Recommended VM-Arguments:
 * <pre>-Xms128m -Xmx1024m -splash:bin/de/zbit/gui/img/splash.gif -Duser.language=en -Duser.country=US</pre>
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class Integrator extends Launcher {
  private static final long serialVersionUID = 1217536687044388477L;
  
  /**
   * The logger for this class.
   */
  private static final transient Logger log = Logger.getLogger(Integrator.class.getName());
  
  /**
   * 
   * @param args
   */
  public static void main(String args[]) {
    // "Merge" other applications with this one
    // Must be done first, because option defaults are changed
    IntegratorUI.integrateIntoKEGGtranslator();
    GUIOptions.GUI.setDefaultValue(Boolean.TRUE);
    // Make an instance of this application
    new Integrator(args);
  }
  
  /**
   * @param args
   */
  public Integrator(String[] args) {
    super(args);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getAppName()
   */
  @Override
  public String getAppName() {
    return IntegratorUI.appName;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#addCopyrightToSplashScreen()
   */
  @Override
  protected boolean addCopyrightToSplashScreen() {
    return false;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#commandLineMode(de.zbit.AppConf)
   */
  @Override
  public void commandLineMode(AppConf appConf) {}
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getCmdLineOptions()
   */
  @Override
  public List<Class<? extends KeyProvider>> getCmdLineOptions() {
    return IntegratorUI.getStaticCommandLineOptions();
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getInteractiveOptions()
   */
  @Override
  public List<Class<? extends KeyProvider>> getInteractiveOptions() {
    // Return NULL here to only show options as dialog, that
    // are defined in de.zbit.gui.prefs.PreferencePanels
    
    // All options here are made persistent, in contrast to getCmdLineOptions()
    List<Class<? extends KeyProvider>> configList = new LinkedList<Class<? extends KeyProvider>>();
    configList.add(IntegratorIOOptions.class);
    configList.add(PathwayVisualizationOptions.class);
    configList.add(SignalOptions.class);
    configList.add(EnrichmentOptions.class);
    
    configList.add(KEGGtranslatorOptions.class);
    configList.add(TranslatorPanelOptions.class);
    //configList.add(GUIOptions.class);
    return configList;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getLogLevel()
   */
  @Override
  public Level getLogLevel() {
    return Level.INFO;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getURLlicenseFile()
   */
  @Override
  public URL getURLlicenseFile() {
    URL url = null;
    try {
      url = new URL("http://www.gnu.org/licenses/lgpl-3.0-standalone.html");
    } catch (MalformedURLException exc) {
      log.log(Level.FINER, exc.getLocalizedMessage(), exc);
    }
    return url;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getURLOnlineUpdate()
   */
  @Override
  public URL getURLOnlineUpdate() {
    try {
      return new URL("http://www.cogsys.cs.uni-tuebingen.de/software/Integrator/downloads/");
    } catch (MalformedURLException e) {
      log.log(Level.FINE, e.getLocalizedMessage(), e);
    }
    return null;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getVersionNumber()
   */
  @Override
  public String getVersionNumber() {
    return IntegratorUI.appVersion;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getYearOfProgramRelease()
   */
  @Override
  public short getYearOfProgramRelease() {
    return 2012;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#getYearWhenProjectWasStarted()
   */
  @Override
  public short getYearWhenProjectWasStarted() {
    return 2011;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.Launcher#initGUI(de.zbit.AppConf)
   */
  @Override
  public Window initGUI(AppConf appConf) {
    return new IntegratorUI(appConf);
  }
  
}

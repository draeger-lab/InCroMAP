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

import java.util.Arrays;

import de.zbit.io.NameAndSignalReader;
import de.zbit.util.Reflect;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.Range;

/**
 * IO options for Integrator.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
@SuppressWarnings("rawtypes")
public interface IntegratorIOOptions extends de.zbit.util.prefs.KeyProvider {
  static Class[] available_formats = Reflect.getAllClassesInPackage("de.zbit.io", true, true, NameAndSignalReader.class,null,true);
  
  /**
   * Readable file formats
   */
  public static final Option<Class> READER = new Option<Class>("Data type",
      Class.class, "Readable input data types",
      new Range<Class>(Class.class, Arrays.asList(available_formats)),
      (short) 2, "-t", available_formats[0]);
  
}

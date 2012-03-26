/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
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
package de.zbit.gui.customcomponents;

import javax.swing.SwingWorker;

import de.zbit.util.progressbar.AbstractProgressBar;

/**
 * A simple {@link SwingWorker} extension with a {@link AbstractProgressBar}.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public abstract class ProgressWorker<T, V> extends SwingWorker<T, V>{
  AbstractProgressBar progress=null;
  
  public void setProgressBar(AbstractProgressBar progress) {
    this.progress = progress;
  }
  
  public AbstractProgressBar getProgressBar() {
    return progress;
  }
  
}

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
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.io;

import java.io.File;
import java.util.List;

import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUITools;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.sbvc.io.BioPAXImporter;
import de.zbit.util.Species;

/**
 * General input handler for BioPAX files.
 * 
 * <p>Extension of {@link BioPAXImporter} that essentially filters the
 * available species to the ones, available in InCroMAP and
 * issues a warning if no entrez genes are in a pathway.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class BioPAXimporterInCroMAP extends BioPAXImporter {

  /**
   * @param biopaxFile
   */
  public BioPAXimporterInCroMAP(File biopaxFile) {
    super(biopaxFile, Format.JPG);
  }
  

  /* (non-Javadoc)
   * @see de.zbit.sbvc.io.BioPAXImporter#showWarningThatNoEntrezIDsAreInThePathway()
   */
  @Override
  protected void showWarningThatNoEntrezIDsAreInThePathway() {
    // Issue a warning that the file did not contain entrez gene identifiers.
    GUITools.showWarningMessage(null, "The BioPAX file did not contain any XRefs for proteins that could be mapped to Entrez Gene identifiers.\nYou will not be able to visualize data within this pathway.");
  }

  /* (non-Javadoc)
   * @see de.zbit.sbvc.io.BioPAXImporter#letUserSelectSpecies()
   */
  @Override
  protected Species letUserSelectSpecies() {
    return IntegratorUITools.showOrganismSelectorDialog(null);
  }

  /* (non-Javadoc)
   * @see de.zbit.sbvc.io.BioPAXImporter#availableOrganisms()
   */
  @Override
  protected List<Species> availableOrganisms() {
    return IntegratorUITools.organisms;
  }
  
}

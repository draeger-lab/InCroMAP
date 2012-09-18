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
 * Copyright (C) 2011-2012 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.data;

import java.io.File;
import java.util.List;

import org.biopax.paxtools.model.Model;

import de.zbit.biopax.BioPAX2KGML;
import de.zbit.gui.IntegratorUITools;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.util.Species;

/**
 * A simple wrapper and holder class for BioPAX pathway models.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class BioPAXpathway {
  
  /**
   * The BioPAX model
   */
  Model model;
  
  /**
   * The original input file.
   */
  File biopaxFile;
  
  /**
   * 
   * @param biopaxFile
   */
  public BioPAXpathway (File biopaxFile) {
    model = BioPAX2KGML.getModel(biopaxFile.getName());
    this.biopaxFile = biopaxFile;
  }
  
  /**
   * Creates a list of all pathways that are contained in the {@link #model}.
   * @return
   */
  public List<String> getListOfPathways() {
    return BioPAX2KGML.getListOfPathways(model);
  }

  /**
   * @return the current BioPAX {@link #model}.
   */
  public Model getModel() {
    return model;
  }
  
  /**
   * 
   * @return
   */
  public Species getSpecies() {
    /*
     * TODO: Implement this. It would also be sufficient (and maybe even better)
     * to return the NCBI taxonomy id here instead of an species object.
     */
    return null;
  }

  /**
   * @param pwName a file may contain multiple pathways. Specify the pathway name here.
   * @return
   */
  public Pathway getKGMLpathway(String pwName) {
    // TODO: pwName may be null.
    return BioPAX2KGML.parsePathwayToKEGG(biopaxFile!=null?biopaxFile.getName():null, pwName, model);
  }
  
}

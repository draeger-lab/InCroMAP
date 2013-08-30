/*
  * $Id:  CompoundReader.java 16:42:22 rosenbaum $
  * $URL: CompoundReader.java $
  * ---------------------------------------------------------------------
  * This file is part of Integrator, a program integratively analyze
  * heterogeneous microarray datasets. This includes enrichment-analysis,
  * pathway-based visualization as well as creating special tabular
  * views and many other features. Please visit the project homepage at
  * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
  * obtain the latest version of Integrator.
  *
  * Copyright (C) 2011-2013 by the University of Tuebingen, Germany.
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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import de.zbit.data.compound.Compound;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.mapper.MappingUtils;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.util.Species;

/**
 * @author Lars Rosenbaum
 * @version $Rev$
 */

public class CompoundReader extends AbstractCompoundReader<Compound> {
  public static final transient Logger log = Logger.getLogger(CompoundReader.class.getName());
  

  /**
   * This is ONLY for use in combination with {@link #importWithGUI(String)} afterwards.
   */
  public CompoundReader() {
    super();
  }
  

  public CompoundReader(int identifierCol, IdentifierType idType) {
    super(identifierCol, idType);
  }
  
  public CompoundReader(int identifierCol, IdentifierType idType, Species species) {
    super(identifierCol, idType,species);
  }

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#getAdditionalExpectedColumns()
   */
  @Override
  protected List<ExpectedColumn> getAdditionalExpectedColumns(){return null;}


  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#processAdditionalExpectedColumns(java.util.List)
   */
  @Override
  protected void processAdditionalExpectedColumns(List<ExpectedColumn> additional) {}

  /**
   * Create Compound from the CSV file.
   * The name and compoundID is already parsed and
   * also the signals is being taken care of.
   * Additional values from the CSV file can be parsed here.
   * Else, the return is simply
   * @param name most human readable common compound name
   * @param compoundID <b>might be <code>NULL</code></b>, if unknown!
   * @param line current line of CSV file
   * @return instance of Compound
   */
  protected Compound createObject(String name, Integer compoundID, String[] line) {
    Compound m;
    if (compoundID!=null) {
      m = new Compound(name, compoundID);
    } else {
      m = new Compound(name);
    }
    
    return m;
  }
}

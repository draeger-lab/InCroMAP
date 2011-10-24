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
package de.zbit.io;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import de.zbit.data.GeneID;
import de.zbit.data.protein.ProteinModificationExpression;
import de.zbit.gui.csv.ExpectedColumn;

/**
 * A class to read {@link ProteinModificationExpression} data.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class ProteinModificationReader extends AbstractGeneBasedNSreader<ProteinModificationExpression> {
  public static final transient Logger log = Logger.getLogger(ProteinModificationReader.class.getName());
  
  int analyteShortNameCol=-1;
  int modificationCol=-1;

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#getAdditionalExpectedColumns()
   */
  @Override
  protected List<ExpectedColumn> getAdditionalExpectedColumns() {
    List<ExpectedColumn> list = new ArrayList<ExpectedColumn>();
    
    list.add(new ExpectedColumn("Analyte short name",false));
    list.add(new ExpectedColumn("Modification name",false));
    
    return list;
  }

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#processAdditionalExpectedColumns(java.util.List)
   */
  @Override
  protected void processAdditionalExpectedColumns(List<ExpectedColumn> additional) {
    if (additional.get(0).hasAssignedColumns()) {
      analyteShortNameCol = additional.get(0).getAssignedColumn();
    }
    if (additional.get(1).hasAssignedColumns()) {
      modificationCol = additional.get(1).getAssignedColumn();
    }
  }

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#createObject(java.lang.String, java.lang.Integer, java.lang.String[])
   */
  @Override
  protected ProteinModificationExpression createObject(String name, Integer geneID, String[] line) {
    
    String analyteShortName = null;
    if (analyteShortNameCol>=0 && analyteShortNameCol<line.length) analyteShortName = line[analyteShortNameCol];
    
    String modification = null;
    if (modificationCol>=0 && modificationCol<line.length) modification = line[modificationCol];
    
    if (geneID==null) geneID = GeneID.default_geneID;
    return new ProteinModificationExpression(name, analyteShortName, modification, geneID);
  }

  
}

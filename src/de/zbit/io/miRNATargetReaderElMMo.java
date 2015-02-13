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

import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.mapper.AbstractMapper;
import de.zbit.util.Species;
import de.zbit.util.objectwrapper.ValuePair;

/**
 * miRNA Target data reader for ElMMo target predictions
 * ("mm_targets_FullList_flat.tab.gz").
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class miRNATargetReaderElMMo extends miRNATargetReader {
  public static final transient Logger log = Logger.getLogger(miRNATargetReaderElMMo.class.getName());
  
  public miRNATargetReaderElMMo() {    
    super();
  }
  public miRNATargetReaderElMMo(String species) {    
    super(species);
  }
  public miRNATargetReaderElMMo(Species species) {  
    super(species);
  }
  
  @Override
  protected Collection<ValuePair<String, miRNAtarget>> customParseInputFile(String[] line, AbstractMapper<String, Integer> mapper) {
    Collection<ValuePair<String, miRNAtarget>> ret = new LinkedList<ValuePair<String, miRNAtarget>>();
    
    // ElMMo uses RefSeq identifiers
    String target = line[col_Target];
    String[] targetANDmiRNA = target.split(Pattern.quote(":"));
    Integer targetInt = mapTargetToGeneID(mapper, targetANDmiRNA[0]);
    if (targetInt==null) return null; // Not mappable. Skip it.
    
    // Get further information
    String source = targetANDmiRNA[1];
    source = postProcess_miRNAName(source, species);
    
    float pVal = Float.NaN;
    if (col_pValueOrScore>=0) {
      if (line[col_pValueOrScore].trim().length()<1 || line[col_pValueOrScore].equals("NULL")) {
        pVal = Float.NaN;
      } else {
        pVal = Float.parseFloat(line[col_pValueOrScore]);
      }
    }
    
    String ref = this.predictionAlgorithm;
    
    // Add and return single miRNA
    miRNAtarget t = new miRNAtarget(targetInt, this.isExperimental, ref, pVal);
    ret.add(new ValuePair<String, miRNAtarget>(source, t));
    
    return ret;    
  }
  
}

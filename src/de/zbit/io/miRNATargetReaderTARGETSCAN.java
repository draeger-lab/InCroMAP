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
 * miRNA Target data reader, specially for TargetScan
 * target predictions.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class miRNATargetReaderTARGETSCAN extends miRNATargetReader {
  public static final transient Logger log = Logger.getLogger(miRNATargetReaderTARGETSCAN.class.getName());
  
  public miRNATargetReaderTARGETSCAN() {    
    super();
  }
  public miRNATargetReaderTARGETSCAN(String species) {    
    super(species);
  }
  public miRNATargetReaderTARGETSCAN(Species species) {  
    super(species);
  }
  
  @Override
  protected Collection<ValuePair<String, miRNAtarget>> customParseInputFile(String[] line, AbstractMapper<String, Integer> mapper) {
    Collection<ValuePair<String, miRNAtarget>> ret = new LinkedList<ValuePair<String, miRNAtarget>>();
    
    // TargetScan uses NCBI Gene ids for target labeling
    String target = line[col_Target];
    Integer targetInt = Integer.parseInt(target);
    
    // Get further information
    String source = line[col_miRNA];
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
    
    
    /*
     * miRNAs wie
     * - "miR-485/485-5p", "miR-214/761", "miR-96/1271",... gefunden in TargetScan v5.1
     * - "miR-128" in mmu equals: "mmu-miR-128".
     * - Web Splits "miR-26ab/1297" => "mmu-miR-26a" and "mmu-miR-26b" and  "mmu-miR-1297"
     * 
     * ==> Split miR-26ab to *a and *b and split at "/", replacing everything from "-".
     */
    int pos = source.indexOf('/');
    if (pos>0) {
      // Get common prefix
      // Targetscan NEVER puts prefixes before "let-" or "miR-".
      int pos2 = source.lastIndexOf("miR-", pos);
      if (pos2>0) pos = pos2+"miR-".length();
      else pos = source.indexOf("-", pos)+1;
      
      String prefix = source.substring(0, pos);
      String[] splitt = source.substring(pos, source.length()).split(Pattern.quote("/"));
      for (int i=0; i<splitt.length; i++) {
        String miRNA = prefix+splitt[i];
        add_miRNA(ret, targetInt, pVal, ref, miRNA);          
      }
      source = prefix+splitt[splitt.length-1];
    } else {
      add_miRNA(ret, targetInt, pVal, ref, source);
    }
    
    
    return ret;    
  }
  
  /**
   * 
   * @param ret - Collection to add the miRNA and target to
   * @param targetInt - target
   * @param pVal - pVal
   * @param ref - Reference string
   * @param miRNA - miRNA name
   */
  private void add_miRNA(Collection<ValuePair<String, miRNAtarget>> ret,
    Integer targetInt, float pVal, String ref, String miRNA) {
    miRNA = postProcess_miRNAName(miRNA, species);
    
    // Create target
    miRNAtarget t = new miRNAtarget(targetInt, this.isExperimental, ref, pVal);
    
    // Split families to single miRNAs e.g. mmu-miR-200bc or mmu-miR-320abcd
    // but skip something like "mmu-miR-17-93.mr"
    if (Character.isLetter(miRNA.charAt(miRNA.length()-1)) && 
        Character.isLetter(miRNA.charAt(miRNA.length()-2))) {
      
      String suffix="";
      for (int i=miRNA.length()-1; i>=0; i--) {
        char c = miRNA.charAt(i);
        if (Character.isLetter(c)) {
          suffix+=Character.toString(c);
        } else if (Character.isDigit(c)) {
          // Add all
          String prefix=miRNA.substring(0, i+1);
          for (char c1: suffix.toCharArray()) {
            ret.add(new ValuePair<String, miRNAtarget>(prefix+c1, t));
          }
          return; // do not add combined family
        } else {
          // e.g. "mmu-miR-17-93.mr" => Add single miRNA string
          break;
        }
      }
    }
    
    // Add single miRNA
    ret.add(new ValuePair<String, miRNAtarget>(miRNA, t));
  }
}

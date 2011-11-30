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
package de.zbit.data;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Tools to convert a chromosome from and to different condings.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public final class ChromosomeTools {
  public static final transient Logger log = Logger.getLogger(ChromosomeTools.class.getName());
  
  /**
   * Converts the chromosome to a {@link Byte} representation.
   * @param chromosome
   * @return Chromsome number or -1 = X; -2 = Y; -3 = M; Or
   * {@link Chromosome#default_Chromosome_byte} if anything went wrong.
   */
  public static byte getChromosomeByteRepresentation(String chromosome) {
    if (chromosome==null || chromosome.equals(Chromosome.default_Chromosome_string)) {
      return Chromosome.default_Chromosome_byte;
    }
    
    Matcher m = Chromosome.chromosome_regex.matcher(chromosome.trim());
    if (m.find()) {
      chromosome = m.group(2);
      
      if (chromosome.equalsIgnoreCase("X")) return -1;
      else if (chromosome.equalsIgnoreCase("Y")) return -2;
      else if (chromosome.equalsIgnoreCase("M")) return -3;
      else  {
        try {
          return (byte) Integer.parseInt(chromosome);
        } catch (NumberFormatException e) {
          log.warning(String.format("Unknown Chromosome \"%s\".", chromosome));
          return Chromosome.default_Chromosome_byte;
        }
      }
    } else {
      log.warning(String.format("Unknown Chromosome \"%s\".", chromosome));
    }
    
    return Chromosome.default_Chromosome_byte;
  }
  
  /**
   * Converts a byte chromosome representation (created with
   * {@link #getChromosomeByteRepresentation(String)}) to a
   * String representation
   * @param chromosome
   * @return e.g. "chr5"
   */
  public static String getChromosomeStringRepresentation(byte chromosome) {
    if (chromosome==-1) return "chrX";
    else if (chromosome==-2) return "chrY";
    else if (chromosome==-3) return "chrM";
    else  {
      if (chromosome==Chromosome.default_Chromosome_byte) {
        return Chromosome.default_Chromosome_string;
      } else {
        return "chr"+Byte.toString(chromosome);
      }
    }
  }
  
}

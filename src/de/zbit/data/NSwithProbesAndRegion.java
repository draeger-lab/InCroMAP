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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import de.zbit.sequence.region.Chromosome;
import de.zbit.sequence.region.ChromosomeTools;
import de.zbit.sequence.region.Region;
import de.zbit.util.Utils;

/**
 * An abstract implementation for probe-based {@link NSwithProbes}, that also contain
 * {@link Region}-based information.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public abstract class NSwithProbesAndRegion extends NSwithProbes implements Chromosome, Region {
  private static final long serialVersionUID = -1546569639548754283L;
  public static final transient Logger log = Logger.getLogger(NSwithProbesAndRegion.class.getName());

  /**
   * @param probeName
   * @param geneName
   * @param geneID
   */
  public NSwithProbesAndRegion(String probeName, String geneName, Integer geneID) {
    super(probeName, geneName, geneID);
  }
  
  public NSwithProbesAndRegion(String probeName, String geneName, Integer geneID, String chromosome, int start, int end) {
    super(probeName, geneName, geneID);
    setStart(start);
    setEnd(end);
    setChromosome(chromosome);
  }
  
  

  /**
   * @return the probe start (or {@link Region#DEFAULT_START}).
   */
  public int getStart() {
    Object probeStart = getData(Region.startKey);
    return probeStart==null?Region.DEFAULT_START:(Integer)probeStart;
  }
  
  /**
   * Set the corresponding probe start.
   */
  public void setStart(int probeStart) {
    super.addData(Region.startKey, probeStart);
  }
  
  /**
   * Remove the probe Start
   */
  public void unsetProbeStart() {
    super.removeData(Region.startKey);
  }
  
  /**
   * @return the probe end (or {@link Region#DEFAULT_START}).
   */
  public int getEnd() {
    Object probeEnd = getData(Region.endKey);
    return probeEnd==null?Region.DEFAULT_START:(Integer)probeEnd;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.sequence.region.Region#getMiddle()
   */
  @Override
  public int getMiddle() {
    int start = getStart();
    int end = getEnd();
    
    if (start!=Region.DEFAULT_START && end!=Region.DEFAULT_START) {
      return start+((end-start)/2); // get middle
    } else if (start!=Region.DEFAULT_START) {
      return start;
    } else if (end!=Region.DEFAULT_START) {
      return end;
    } else {
      return Region.DEFAULT_START;
    }
  }
  
  /**
   * Set the corresponding probe end.
   */
  public void setEnd(int probeEnd) {
    super.addData(Region.endKey, probeEnd);
  }
  
  /**
   * Remove the probe end
   */
  public void unsetProbeEnd() {
    super.removeData(Region.endKey);
  }
    
  /* (non-Javadoc)
   * @see de.zbit.data.NSwithProbes#hashCode()
   */
  @Override
  public int hashCode() {
    return super.hashCode() + getStart()+getEnd() + getChromosomeAsByteRepresentation();
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NSwithProbes#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(Object o) {
    int r = super.compareTo(o); // compares name, GeneID, (and the currently unused probe id)
    if (o instanceof NSwithProbesAndRegion) {
      NSwithProbesAndRegion ot = (NSwithProbesAndRegion) o;
      if (r==0) {
        r = Utils.compareIntegers((int)getChromosomeAsByteRepresentation(), (int)ot.getChromosomeAsByteRepresentation());
        if (r==0) {
          r = Utils.compareIntegers(getStart(), ot.getStart());
          if (r==0) {
            r = Utils.compareIntegers(getEnd(), ot.getEnd());
          }
        }
      }
    } else {
      return -2;
    }
    
    return r;
  }
  
  
  protected <T extends NameAndSignals> void merge(Collection<T> source, T target, Signal.MergeType m) {
    super.merge(source, target, m);
    
    // This is required to ensure having (min of) integers as start and end positions.
    List<Integer> positions = new ArrayList<Integer>(source.size());
    for (T o : source) {
      int s = ((Region) o).getStart();
      if (s!=Region.DEFAULT_START) positions.add(s);
    }
    if (positions.size()>0) {
      double averagePosition = Utils.min(positions);
      ((Region) target).setStart((int)(averagePosition));
    }
    
    // End pos (max and integer)
    positions = new ArrayList<Integer>(source.size());
    for (T o : source) {
      Integer s = ((Region) o).getEnd();
      if (s!=Region.DEFAULT_START) positions.add(s);
    }
    if (positions.size()>0) {
      double averagePosition = Utils.max(positions);
      ((Region) target).setEnd((int)(averagePosition));
    }
    
    
    // Chromosome (only if equal)
    byte chr = Chromosome.default_Chromosome_byte;
    if (source.iterator().hasNext()) {
      chr = ((Chromosome)source.iterator().next()).getChromosomeAsByteRepresentation();
      boolean allMatch = true;
      for (T o : source) {
        if (((Chromosome)o).getChromosomeAsByteRepresentation()!=chr) {
          allMatch = false;
          break;
        }
      }
      if (!allMatch) {
        chr = Chromosome.default_Chromosome_byte;
      }
    }
    ((Chromosome) target).setChromosome(chr);
    
  }

  /* (non-Javadoc)
   * @see de.zbit.data.Chromosome#setChromosome(java.lang.String)
   */
  @Override
  public void setChromosome(String chromosome) {
    setChromosome(ChromosomeTools.getChromosomeByteRepresentation(chromosome));
  }

  /* (non-Javadoc)
   * @see de.zbit.data.Chromosome#getChromosome()
   */
  @Override
  public String getChromosome() {
    return ChromosomeTools.getChromosomeStringRepresentation(getChromosomeAsByteRepresentation());
  }
  
  /**
   * Remove the chromosome
   */
  public void unsetChromosome() {
    super.removeData(chromosome_key);
  }

  /* (non-Javadoc)
   * @see de.zbit.data.Chromosome#setChromosome(java.lang.Byte)
   */
  @Override
  public void setChromosome(byte chromosome) {
    super.addData(chromosome_key, chromosome);
  }

  /* (non-Javadoc)
   * @see de.zbit.data.Chromosome#getChromosomeAsByteRepresentation()
   */
  @Override
  public byte getChromosomeAsByteRepresentation() {
    Byte b = (Byte) super.getData(chromosome_key);
    if (b==null) return Chromosome.default_Chromosome_byte;
    else return b;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#additionalDataToString(java.lang.String, java.lang.Object)
   */
  @Override
  protected Object additionalDataToString(String key, Object value) {
    if (value==null) return super.additionalDataToString(key, value);
    if (key.equals(chromosome_key) && value instanceof Byte) {
      return ChromosomeTools.getChromosomeStringRepresentation((Byte)value);
    } else {
      return super.additionalDataToString(key, value);
    }
  }

  /* (non-Javadoc)
   * @see de.zbit.data.Region#intersects(de.zbit.data.Region)
   */
  @Override
  public boolean intersects(Region other) {
    int start = getStart(); int end = getEnd();
    int start2 = other.getStart(); int end2 = other.getEnd();
    return  (getChromosomeAsByteRepresentation()==other.getChromosomeAsByteRepresentation()) &&
    ((start2 >= start && start2 <= end) || (start >= start2 && start <= end2));
  }


  
}

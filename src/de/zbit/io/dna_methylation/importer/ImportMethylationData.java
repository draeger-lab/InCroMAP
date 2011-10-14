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
package de.zbit.io.dna_methylation.importer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.zbit.data.NameAndSignals;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.io.AbstractGeneBasedNSreader;
import de.zbit.util.ValuePairUncomparable;

/**
 * IDEA: Provide a class that performs our DNA methylation data processing method manually.
 * 
 * <p>Unused.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class ImportMethylationData extends AbstractGeneBasedNSreader<DNAmethylation> {
  
  // TODO: Check if it is possible to let DNAmethRawProbe extend NameAndSignals???? Oder besser nicht???
  
  /* PAIR FILE EXAMPLE:
   * # software=NimbleScan  version=2.6.0.0 (158 20100317 1656) imagefile=/nfs/bioprod/production/analysis/active/30616y/55018602_532.tif designfile=/nfs/bioprod/production/designs/100205_MM9_Promoter_MeDIP_HX1.ndf  designname=100205_MM9_Promoter_MeDIP_HX1  designid=11336  date=Thu Aug 19 10:47:37 CDT 2010 border=0  ul_x=4.500  ul_y=3.500  ur_x=1050.500 ur_y=3.500  lr_x=1050.500 lr_y=1399.500 ll_x=4.500  ll_y=1399.500 qcscore=0.207 locallyaligned=no correctAstig=no Knots=  auto=no
   * IMAGE_ID  GENE_EXPR_OPTION  SEQ_ID  PROBE_ID  POSITION  X Y MATCH_INDEX SEQ_URL PM  MM
   * 55018602_532  BLOCK1  chr10:100046819-100060093 CHR10FS100050354  100050354 873 679 175172956   1105.00 0.00
   * 55018602_532  BLOCK1  chr10:100046819-100060093 CHR10FS100050464  100050464 789 1173  175217523   2145.00 0.00
   * 55018602_532  BLOCK1  chr10:100046819-100060093 CHR10FS100050969  100050969 998 778 175223156   8087.00 0.00
   * 55018602_532  BLOCK1  chr10:100046819-100060093 CHR10FS100054458  100054458 534 1244  175224494   1365.00 0.00
   * 
   * Manuel Result File EXAMPLE:
   * seqIDs probeIDs  pos 55018602_635  55018902_635  55019202_635  55022002_635  55022302_635  55018602_532  55018902_532  55019202_532  55022002_532  55022302_532  CGcontent 55018602_635  55018902_635  55019202_635  55022002_635  55022302_635  55018602_532  55018902_532  55019202_532  55022002_532  55022302_532
   * chr10:100046819-100060093     CHR10FS100050354              100050354                     7.57613258869071              7.87009005604306              7.99830907786311              7.68426049745324              7.68800572725229              7.53320580156718              7.69533021761371              7.81986981540015              7.66377600958212              7.67954648181264              6.522                         4                             4                             4                             4                             4                             4                             4                             4                             4                             4                            
   * chr10:100046819-100060093     CHR10FS100050464              100050464                     7.76006948589683              8.04816397954404              8.10947040505421              7.82417509259046              7.87440470997097              7.70581142728508              7.81746652373312              7.97846812017123              7.83769272072163              7.85436975935459              7.36                          3.88221580435195              4                             4                             4                             4                             3.34000393862637              4                             4                             3.97705443707637              4                            
   * chr10:100046819-100060093     CHR10FS100050969              100050969                     8.46342909566662              8.62594778013421              8.54735594174963              8.46513502849572              8.6145699682663               8.20915841252633              8.45306742225268              8.35907827618401              8.29183097338685              8.45302937943418              6.874                         2.97200953580413              3.28292959183692              3.28063818704703              3.29465607210396              3.60311669594199              2.32074252979358              2.60065032565545              2.6142736893683               2.61010812235857              2.61701743855518             
   * chr10:100046819-100060093     CHR10FS100054458              100054458                     7.01180834673298              6.99884716698093              6.91450855002409              6.87122491556352              6.95552986199971              6.96200222382357              7.00542414593497              6.94613231918621              6.77737810870654              6.92214621573844              3.378                         0.305540467137425             0.305540467137425             0.305540467137425             0.305540467137425             0.305540467137425             0.305540467137425             0.305540467137425             0.305540467137425             0.305540467137425             0.305540467137425            
   * 
   * 
   */
  
  /*
   * TODO: Create CSVImporterV2 mit 
   * 1. Observation1_treatment
   * Observation1_control
   * 
   * 2. Position
   * 
   * 3. GeneID (wie üblich...)
   * 
   * as input.
   * 
   * _________________________
   * TODO Verfahren:
   * 1. Alles einlesen, werte als float(?)
   * 2. Comparator: GeneID primär und position secundär
   * 3. anhand position fenster berechnen (<=OPTION für FENSTERBREITE)
   * 4. für alle gepaarten werte in fenster t-test und fc berechnen
   * 5. für result aufheben
   *  
   */
  
  public static void main(String[] args) {
    
  }
  
  /**
   * A list of valuePairs of matched columns for treatment and controls.
   */
  Iterable<ValuePairUncomparable<Iterable<Integer>, Iterable<Integer>>> raw_obs_cols;
  
  //int gene_identifier_col;
  
  int position_col;

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#getAdditionalExpectedColumns()
   */
  @Override
  protected List<ExpectedColumn> getAdditionalExpectedColumns() {
    List<ExpectedColumn> add = new LinkedList<ExpectedColumn>();
    add.add(new ExpectedColumn("Position", true));
    return add;
  }

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#processAdditionalExpectedColumns(java.util.List)
   */
  @Override
  protected void processAdditionalExpectedColumns(List<ExpectedColumn> additional) {
    position_col = additional.get(0).getAssignedColumn();
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#getExpectedSignalColumnsOverridable(int)
   */
  @Override
  protected Collection<ExpectedColumn> getExpectedSignalColumnsOverridable(
    int maxNumberOfObservations) {
    // TODO Return 10 treatment/Control pairs
    return super.getExpectedSignalColumnsOverridable(maxNumberOfObservations);
  }

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#createObject(java.lang.String, java.lang.Integer, java.lang.String[])
   */
  @Override
  protected DNAmethylation createObject(String name, Integer geneID, String[] line) {
    // TODO: create temp DNA-m object and 
    // TODO: create overridable methods that allow to generate the DNAmethRawProbe array.
    // BEtter let DNAmethRawProbe override nameAndSignals and ovveride there the
    // addSignals methoden? geht das? Wahrscheinlich speiocher zu hoch.
    return null;
  }
  
}

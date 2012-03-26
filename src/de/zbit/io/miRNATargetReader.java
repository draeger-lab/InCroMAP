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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.data.miRNA.miRNAtargets;
import de.zbit.io.csv.CSVReader;
import de.zbit.io.csv.CSVwriteableIO;
import de.zbit.mapper.AbstractMapper;
import de.zbit.mapper.MappingUtils;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.util.Species;
import de.zbit.util.StringUtil;
import de.zbit.util.logging.LogUtil;
import de.zbit.util.objectwrapper.ValuePair;
import de.zbit.util.progressbar.AbstractProgressBar;
import de.zbit.util.progressbar.ProgressBar;

/**
 * miRNA Target data reader.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class miRNATargetReader {
  public static final transient Logger log = Logger.getLogger(miRNATargetReader.class.getName());
  
  protected int col_miRNA=-1;
  protected int col_Target=-1;
  
  /**
   * Contains a reference (mainly for experimental predictions).
   * E.g. the PubMed ID or the source of this target prediction.
   * Can be inferred with "pubmed" or "pmid" or "Reference"
   */
  protected int col_Reference=-1;
  
  /**
   * If {@link #col_Reference} is < 0, the reader will
   * take this content as reference instead of using the
   * {@link #col_Reference}. In case of reading miRNA target predictions,
   * this string should be set to the name of the prediction algorithm.
   */
  protected String predictionAlgorithm=null;
  
  /**
   * If input file has data for multiple species, this allows
   * filtering for the correct species.
   */
  protected int col_Species=-1;
  
  /**
   * if !{@link #isExperimental}, column with
   * pValue (detected by all values 0-1) or score
   * (values >1).
   */
  protected int col_pValueOrScore=-1;
  
  /**
   * "human", "mouse" or "rat".
   */
  protected Species species=null;
  protected boolean isExperimental=false;
  
  /**
   * Regular expression that can be used with
   * {@link CSVReader#getColumnByMatchingContent(String)} to identify
   * column with miRNA names.
   */
  public final static String miRNAidentifierRegEx = "(.*-)?miR-\\d\\S*";
  
  /**
   * Target identifier.
   */
  protected IdentifierType targetIDtype=IdentifierType.Unknown;
  
  /**
   * A ProgressBar that allows displaying various progresses
   */
  protected AbstractProgressBar progress;
  
  /**
   * @param progress the progress to set
   */
  public void setProgress(AbstractProgressBar progress) {
    this.progress = progress;
  }

  public miRNATargetReader() {    
    super();
  }
  
  public miRNATargetReader(String species) {    
    this();
    
    try {
      this.species = Species.search(species);
    } catch (IOException e) {
      log.log(Level.WARNING, "Could not read list of species.", e);
    }
    
    if (this.species==null) {
      log.log(Level.WARNING, "Species is unknown: " + species);
    } else {
      log.config("Matched species '" + species + "' to " + this.species);
    }
  }
  
  public miRNATargetReader(Species species) {    
    this();
    this.species=species;
  }
  
  /**
   * Checks if given column of the given CSV file contains one
   * of the identifiers listed in {@link IdentifierType}.
   * @param in - input file
   * @param colNumber - column number of input file
   * @return {@link IdentifierType} of the column.
   * @throws IOException
   */
  public static IdentifierType checkColumnContent(CSVReader in, int colNumber) throws IOException {
    int c;
    String[] content = in.getColumn(colNumber, 150);
    
    // Recognize numeric columns
    c = StringUtil.matches("\\d+", content);
    if (c>=(double)content.length*0.9) return IdentifierType.NCBI_GeneID;

    // Recognize RefSeq mRNA transcript ID
    c = StringUtil.matches("(.*\\s)?[NX][MR]_\\d+.*", content);
    if (c>=(double)content.length*0.9) // if 90% matches
      return IdentifierType.RefSeq;
    
    // Recognize (any) ensembl ID
    c = StringUtil.matches("ENS.*\\d+", content);
    if (c>=(double)content.length*0.1) return IdentifierType.Ensembl;
    
    //return IdentifierType.Unknown;
    // In doubt, try sticking with gene symbols.
    return IdentifierType.GeneSymbol;
  }
  
  
  public miRNAtargets readCSV(String filename) throws IOException {
    // Open input file
    CSVReader in = new CSVReader(filename);
    in.setProgressBar(progress);
    in.setDisplayProgress(progress!=null);
    
    // Infere columns
    //int col_miRNA = this.col_miRNA;
    if (col_miRNA<0) {
      col_miRNA = in.getColumnByMatchingContent(miRNAidentifierRegEx);
    }
    
    //int col_Target = this.col_Target;
    if (col_Target<0) {
      col_Target = in.getColumnContaining("target", "GeneID");
      if (col_Target<0) col_Target = in.getColumnContaining("target", "Gene ID");
      if (col_Target<0) col_Target = in.getColumnContaining("target", "Gene_ID");
      if (col_Target<0) col_Target = in.getColumnContaining("target", "RefSeq");
      if (col_Target<0) col_Target = in.getColumnContaining("target", "ensembl");
      if (col_Target<0) col_Target = in.getColumnContaining("target");
      if (col_Target<0) col_Target = in.getColumnContaining("gene id");
    }
    //int col_Species = this.col_Species;
    // Should not be inferred automatically because maybe user wants to read all species.
    // Could be guessed by "species" or "organism"
    
    // Check columns
    if ((col_miRNA<0)||(col_Target<0)) {
      log.warning("Could not read miRNA file, due to missing columns - " +
        "miRNA_col: " + col_miRNA + ", target_col: " + col_Target);
      return null;
    }
    //IdentifierType targetIDtype = this.targetIDtype;
    if (targetIDtype==null || targetIDtype.equals(IdentifierType.Unknown)) {
      targetIDtype = checkColumnContent(in, col_Target);
    }
    // Check identifier
    if (targetIDtype.equals(IdentifierType.Unknown)) {
      log.warning("Could not infere type of target identifiers!");
      return null;
    }
    
    // Print summary
    log.info("Reading miRNA target '" + filename + "'.");
    log.config("miRNA_col: " + col_miRNA + ", target_col: " + col_Target + ", target_type: " + targetIDtype.toString());
    log.config("colRef: " + (col_Reference>=0?col_Reference:predictionAlgorithm) + ", col_species: " + col_Species);
    if (col_pValueOrScore>=0) log.config("colpValueOrScore: " + col_pValueOrScore);
    
    AbstractMapper<String, Integer> mapper = MappingUtils.initialize2GeneIDMapper(targetIDtype, progress, species);
    if (mapper!=null) {
      mapper.initialize();
      if (!mapper.isReady()) {
        throw new IOException("Could not read mapping data to map " + targetIDtype + " on entrez gene ids.");
      }
    }
    
    // Read data
    miRNAtargets ret = new miRNAtargets();
    int mapped=0, skipped=0;
    String[] line;
    while((line=in.getNextLine())!=null) {
      
      // Filter by species (eventually)
      if (col_Species>=0 && this.species!=null) {
        String spec = line[col_Species];
        if (!species.matchesIdentifier(spec)) {
          skipped++;
          continue;
        }
      }
      
      // Get miRNA => target pair.
      Collection<ValuePair<String, miRNAtarget>> vp = customParseInputFile(line, mapper);
      
      if (vp==null || vp.size()<1) {
        vp = new LinkedList<ValuePair<String, miRNAtarget>>();
        
        // Translate between different identifiers.
        String target = line[col_Target];
        Integer targetInt = mapTargetToGeneID(mapper, target);
        // Skip if not mappable to target
        if (targetInt==null) {
          log.finest("Could not map " +target==null?"null":target);
          skipped++;
          continue;
        }
        
        // Get further information
        String source = line[col_miRNA];
        source = postProcess_miRNAName(source, species);
        String ref = null;
        if (col_Reference>=0) {
          ref = line[col_Reference];
          if (this.predictionAlgorithm!=null && this.predictionAlgorithm.length()>0)
            ref = this.predictionAlgorithm + " - " + ref;
        } else {
          ref = this.predictionAlgorithm;
        }
        float pVal = Float.NaN;
        if (col_pValueOrScore>=0) {
          if (line[col_pValueOrScore].trim().length()<1 || line[col_pValueOrScore].equals("NULL")) {
            pVal = Float.NaN;
          } else {
            pVal = Float.parseFloat(line[col_pValueOrScore]);
          }
        }
        
        
        //int target, boolean experimental, String source, float pValue
        miRNAtarget t = new miRNAtarget(targetInt, this.isExperimental, ref, pVal);
        vp.add( new ValuePair<String, miRNAtarget>(source, t) );
      }
      
      // Check result
      for (ValuePair<String, miRNAtarget> v: vp) {
        if (v.getA()==null || v.getB()==null) {
          skipped++;
        } else {
          ret.addTarget(v.getA(), v.getB());
          mapped++;
        }
      }
      
    }
    log.info("miRNA target file read. Skipped " + skipped + " targets and used " + mapped + " targets.");
    
    return ret;
  }

  
  protected Integer mapTargetToGeneID(AbstractMapper<String, Integer> mapper,
    String target) {
    // Translate between different identifiers.
    Integer targetInt=null;
    if (targetIDtype.equals(IdentifierType.NCBI_GeneID)) {
      targetInt = Integer.parseInt(target);
    } else if (mapper!=null) {
      try {
        targetInt = mapper.map(target);
      } catch (Exception e) {
        log.log(Level.WARNING, "Exception while mapping " +target==null?"null":target, e);
      }
    }
    return targetInt;
  }
  
  
  /**
   * Overwrite me for more custom file parsing.
   * Return null for the generic miRNA target file parsing, return a
   * ValuePair<String, miRNAtarget>(miRNA_id, miRNA_target) for
   * a custom parsing.
   * @param line
   * @return
   */
  protected Collection<ValuePair<String, miRNAtarget>> customParseInputFile(String[] line, AbstractMapper<String, Integer> mapper) {
    return null;
  }
  
  /**
   * Try to ensure a common nomenclature for miRNA names.
   * organism-miR-*
   * 
   * E.g. if spec = mouse and miRNA = "miR-18" this method returns "mmu-miR-18".
   * 
   * @param miRNA
   * @param spec
   * @return
   */
  protected String postProcess_miRNAName(String miRNA, Species spec) {
    
    // Trim brackets (e.g., miRecords still has brackets)
    if (miRNA.startsWith("[") && miRNA.endsWith("]")) {
      miRNA = miRNA.substring(1, miRNA.length()-1);
    }
    
    // Prepend species prefix
    if (spec!=null && spec.getKeggAbbr()!=null && (miRNA.startsWith("miR") || miRNA.startsWith("let"))) {
      // If you some day remove the miRNA startswith, check
      // that the species prefix dore not already exists!
      miRNA = spec.getKeggAbbr().toLowerCase()+'-'+miRNA;
    }
    
    return miRNA;
  }

  /**
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    LogUtil.initializeLogging(Level.FINE);
    String prefix = "C:/Users/wrzodek/Desktop/OLDPC/EigeneDateien/Downloads/miRNA_targets/";
    String file;
    miRNATargetReader r;
    miRNAtargets t;
    miRNAtargets t_all = new miRNAtargets();
    
    batchCreateFiles(prefix+"ChosenSets/");
    if (true) return;
    /*t_all = (miRNAtargets) Utils.loadGZippedObject("miRNAtargets_HC.dat");
    //t_all = (miRNAtargets) CSVwriteableIO.read("miRNAtargets_HC.txt");
    t_all.printSummary();
    
    String miRNAsymbol = "98";
    ObjectAndScore<String>[] hits = t_all.findTargets(miRNAsymbol);
    System.out.println(Arrays.deepToString(hits));;
     */
    /*Collection c = t_all.getTargets(miRNAsymbol);
    if (c==null) System.out.println("0 targets for '" + miRNAsymbol + "'." );
    else {
      System.out.println(c.size() + " targets for '" + miRNAsymbol + "':");
      for (Object o:c) {
        System.out.println(o);
      }
    }*/
    //if (true) return;
    
    
    // XXX: One could extend automatization (reduce Block per file), move colInference to separate static functions, add all miRNA target files.
    
    /*
     * XXX: miRNAs wie
     * - "miR-485/485-5p", "miR-214/761", "miR-96/1271",... gefunden in TargetScan v5.1
     * - "miR-128" in mmu equals: "mmu-miR-128".
     * - Web Splits "miR-26ab/1297" => "mmu-miR-26a" and "mmu-miR-26b"
     * 
     * ==> Split miR-26ab to a) and b) and split at "/", replacing everything from "-".
     * 
     */
    
    // Predictions ("ElMMo (v4)")
    /*
     * COLUMNS: 6 = "Target:miRNA", score: 8 (0.0 - 1.0) -- posterior probability (HIGHER is better). 
     * NOTE   : (column8) "Use a cutoff of P>0.5 for medium confidence and P>0.8 for high confidence miRNA target sites"
     */
    file = prefix+"2010-01 mm_targets_FullList_flat.tab.gz";
    r = new miRNATargetReaderElMMo("mouse");
    r.col_pValueOrScore=8;
    r.col_Target=6; // Column 6 = "Target:miRNA"
    r.isExperimental=false;
    r.predictionAlgorithm="ElMMo v4";
    t = r.readCSV(file);
    t_all.addAll(t);
    System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
    
    // Predictions ("TargetScan v5.1")
    // Seem all to be "high confidence"
    file = prefix+"2009-04 TargetScanMouse_targets.txt.zip";
    r = new miRNATargetReaderTARGETSCAN("mouse");
    r.col_pValueOrScore=9;
    r.col_Species=3; // Contains NCBI Taxonomy Identifiers (ints).
    r.isExperimental=false;
    r.predictionAlgorithm="TargetScan v5.1";
    t = r.readCSV(file);
    t_all.addAll(t);
    System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
    
    // Predictions ("Diana v3")
    // Higher is better, Webservice uses 7.3 as cutoff. This removes about 84% of the data.
    file = prefix+"2009-07-10-microT_v3.0.txt.gz";
    r = new miRNATargetReader("mouse");
    r.col_pValueOrScore=3;
    r.col_Target=2;
    r.isExperimental=false;
    r.predictionAlgorithm="DIANA - microT v3.0";
    t = r.readCSV(file);
    t_all.addAll(t);
    System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
    
    // Experimental "TarBase V5.0"
    file = prefix+"2008-11-19 TarBase_V5.txt";
    r = new miRNATargetReader("mouse");
    r.col_Reference=20;
    r.col_Target=9;
    r.isExperimental=true;
    r.col_Species=4; // OPTIONAL.
    r.predictionAlgorithm = "TarBase V5.0";
    t = r.readCSV(file);
    t_all.addAll(t);
    System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
    
    
    // Experimental "miRTarBase" (Strong experimental)
    file = prefix+"2011-04 miRTarBase_SE_WR.txt";
    r = new miRNATargetReader("mouse");
    r.col_Reference=8;
    r.isExperimental=true;
    r.col_Species=2; // OPTIONAL.
    r.predictionAlgorithm = "miRTarBase (SE)";
    t = r.readCSV(file);
    t_all.addAll(t);
    System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
    
    
    // Weak Experimental "miRTarBase" (Weak experimental)
    file = prefix+"2011-04 miRTarBase_SE_WR.txt";
    t = r.readCSV(file);
    r.predictionAlgorithm = "miRTarBase (WE)";
    t_all.addAll(t);
    System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
    
    
    // Experimental "miRecords"
    file = prefix+"2010-11-25 EXPERIMENTAL miRecords_version3.txt";
    r = new miRNATargetReader("mouse");
    r.col_Reference=0;
    r.isExperimental=true;
    r.col_Species=6; // OPTIONAL. // 1=TargetGene_specied, 6=miRNA species
    r.predictionAlgorithm = "miRecords_v3";
    t = r.readCSV(file);
    t_all.addAll(t);
    System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
    
    // SAVE
    SerializableTools.saveGZippedObject("miRNAtargets.dat", t_all);
    CSVwriteableIO.write(t_all, "miRNAtargets.txt");
    
    // Filter for HC and save again
    t_all.filterTargets("DIANA - microT v3.0", 7.3, true);
    t_all.filterTargets("DIANA - microT v4.0", 0.3, true);
    t_all.filterTargets("ElMMo v4", 0.8, true);
    t_all.printSummary();
    
    SerializableTools.saveGZippedObject("miRNAtargets_HC.dat", t_all);
    CSVwriteableIO.write(t_all, "miRNAtargets_HC.txt");
    
  }
  
  /**
   * Creates miRNA target lists, required by the Integrator application
   * for human, mouse and rat.
   * @param prefix path to folder with required input files
   * @throws IOException
   */
  static void batchCreateFiles(String prefix) throws IOException {
    Species[] allSpecies = new Species[]{
        new Species("Homo sapiens", "_HUMAN", "Human", "hsa", 9606),
        new Species("Mus musculus", "_MOUSE", "Mouse", "mmu", 10090),
        new Species("Rattus norvegicus", "_RAT", "Rat", "rno", 10116)        
    };
    
    for (Species spec: allSpecies) {
      System.out.println("\nProcessing predictions for " + spec.getScientificName());
      String file = null;
      miRNATargetReader r;
      miRNAtargets t;
      miRNAtargets t_all = new miRNAtargets();
      
      System.out.println("ElMMO3 V5 Junary2011");
      //NOTE "Use a cutoff of P>0.5 for medium confidence and P>0.8 for high confidence miRNA target sites"
      if (spec.getKeggAbbr().equals("hsa")) file = prefix+"hg_targets_FullList_flat.tab.gz";
      else if (spec.getKeggAbbr().equals("mmu")) file = prefix+"mm_targets_FullList_flat.tab.gz";
      else if (spec.getKeggAbbr().equals("rno")) file = prefix+"rn_targets_FullList_flat.tab.gz";
      else System.err.println("Unkown species:" + spec.getKeggAbbr());
      r = new miRNATargetReaderElMMo(spec);
      r.setProgress(new ProgressBar(0));
      r.col_pValueOrScore=8;
      r.col_Target=6; // Column 6 = "Target:miRNA"
      r.isExperimental=false;
      r.predictionAlgorithm="ElMMo v5";
      t = r.readCSV(file);
      t.filterTargets("ElMMo v5", 0.8, true); // Filter for HC
      t_all.addAll(t);
      System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
      
      // No TargetScan available for rat!
      file = prefix+String.format("Predicted_Targets_Info_%s.txt.zip", spec.getCommonName().toUpperCase().trim());
      if (new File(file).exists()) {
        System.out.println("TargetScan 5.2 June2011 -- NOT FOR RAT");
        r = new miRNATargetReaderTARGETSCAN(spec);
        r.setProgress(new ProgressBar(0));
        r.col_pValueOrScore=9;
        r.col_Species=3; // Contains NCBI Taxonomy Identifiers (ints).
        r.isExperimental=false;
        r.predictionAlgorithm="TargetScan v5.2";
        t = r.readCSV(file);
        t_all.addAll(t);
        System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
      } else {
        System.out.println("No TargetScan available for " + spec.getCommonName());
      }
      
      System.out.println("Diana microT_v4.0 27.12.2010");
      // Higher is better, Webservice uses 7.3 as cutoff for v3 and 0.3 for v4. This removes about 84% of the data.
      file = prefix+"microT_v4.0.txt.gz";
      r = new miRNATargetReader(spec);
      r.setProgress(new ProgressBar(0));
      r.col_pValueOrScore=3;
      r.col_Target=2;
      r.col_miRNA=1;
      /* REMARK: Species filtering is performed by Ensembl mapping 
       * => IDs of other species not mappable => target won't be included. */
      r.targetIDtype = IdentifierType.Ensembl;
      r.isExperimental=false;
      r.predictionAlgorithm="DIANA - microT v4.0";
      t = r.readCSV(file);
      t.filterTargets("DIANA - microT v4.0", 0.52, true); // 0.3 is default in webservice, but 0.52 cuts ~90% of tha data. => Filter for HC
      //t.filterTargets("DIANA - microT v3.0", 7.3, true);
      t_all.addAll(t);
      System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
      
      // Filter for HC and SAVE
      t_all.printSummary();
      String outFileName = spec.getKeggAbbr() + "_HC_miRNApredTargets";
      System.out.println("Writing to " + outFileName +"\n");
      SerializableTools.saveGZippedObject(outFileName +".dat", t_all);
      CSVwriteableIO.write(t_all, outFileName+".txt");
      t_all = new miRNAtargets(); // Reset
      
      
      
      System.out.println("Experimental \"TarBase V5.0\"");
      file = prefix+"TarBase_V5.0.txt";
      r = new miRNATargetReader(spec);
      r.setProgress(new ProgressBar(0));
      r.col_Reference=20;
      r.col_Target=9;
      r.isExperimental=true;
      r.col_Species=4; // OPTIONAL.
      r.predictionAlgorithm = "TarBase V5.0";
      t = r.readCSV(file);
      t_all.addAll(t);
      System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
      
       
      System.out.println("Experimental \"miRTarBase\" (Strong experimental)");
      file = prefix+"miRTarBase_SE_WR.txt";
      r = new miRNATargetReader(spec);
      r.setProgress(new ProgressBar(0));
      r.col_Reference=8;
      r.isExperimental=true;
      r.col_Species=2; // OPTIONAL.
      r.predictionAlgorithm = "miRTarBase (SE)";
      t = r.readCSV(file);
      t_all.addAll(t);
      System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
      
      
      System.out.println("Weak Experimental \"miRTarBase\" (Weak experimental)");
      file = prefix+"miRTarBase_WE_MP.txt";
      t = r.readCSV(file);
      r.predictionAlgorithm = "miRTarBase (WE)";
      t_all.addAll(t);
      System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
      
      
      System.out.println("Experimental \"miRecords\"");
      file = prefix+"miRecords_version3.txt";
      r = new miRNATargetReader(spec);
      r.setProgress(new ProgressBar(0));
      r.col_Reference=0;
      r.isExperimental=true;
      r.col_Species=6; // OPTIONAL. // 1=TargetGene_specied, 6=miRNA species
      r.predictionAlgorithm = "miRecords_v3";
      t = r.readCSV(file);
      t_all.addAll(t);
      System.out.println("New:" + t.sizeOfTargets() + " All:"+ t_all.sizeOfTargets() + " Unique:"+ t_all.sizeOfUniqueTargets() + " miRNAs:" + t_all.size());
      
      
      // SAVE
      t_all.printSummary();      
      outFileName = spec.getKeggAbbr() + "_miRNAexpTargets";
      System.out.println("Writing to " + outFileName +"\n");
      SerializableTools.saveGZippedObject(outFileName +".dat", t_all);
      CSVwriteableIO.write(t_all, outFileName+".txt");
    }
  }
  
}

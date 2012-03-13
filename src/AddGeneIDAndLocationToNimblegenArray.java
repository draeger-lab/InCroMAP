import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.zbit.io.FileTools;
import de.zbit.io.csv.CSVReader;
import de.zbit.util.ArrayUtils;
import de.zbit.util.StringUtil;
import de.zbit.util.objectwrapper.ValuePair;

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

/**
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class AddGeneIDAndLocationToNimblegenArray {
  
  static Map<String, ValuePair<Integer, Integer>> probePositionMap = new HashMap<String, ValuePair<Integer, Integer>>();
  static Map<String, String> probeGeneMap = new HashMap<String, String>();
  
  public static void main(String[] args) throws IOException {
    //filterCSV();
    //if (true) return;
    
    String probePositionMapFile = "C:\\Users\\wrzodek\\Desktop\\EKUT-A\\gene_centered_data\\Mappings\\mm9-probes-pos.txt";
    //String probePositionMapFile = "C:\\Users\\wrzodek\\Desktop\\MARCAR\\bsp_rat_study\\annotation\\rn34-probe-pos.txt";
    int f1ProbeCol=0;
    int f1StartCol=2;
    int f1EndCol=3;
    
    String probeGeneMapFile = "C:\\Users\\wrzodek\\Desktop\\EKUT-A\\gene_centered_data\\Mappings\\mm9-probes-near-tss.txt";
    //String probeGeneMapFile = "C:\\Users\\wrzodek\\Desktop\\MARCAR\\bsp_rat_study\\annotation\\rn34-probes-near-tss.txt";
    int f2ProbeCol=0;
    int f2GeneCol=1;
    
    //String fileToAnnotate = "C:\\Users\\wrzodek\\Desktop\\gene_centered_data\\ekuta_DNAm_probelevel_data.txt";
    //String fileToAnnotate = "C:\\Users\\wrzodek\\Desktop\\MARCAR\\bsp_rat_study\\gene_centered_data\\bsp_DNAm_probelevel_data.txt";
    String fileToAnnotate = "C:\\Users\\wrzodek\\Desktop\\MARCAR\\SimonsData\\probe_level_methylation_data.txt";
    int f3ProbeCol=0;
    
    boolean removAllDataWithoutGeneIdentifier = true;
    
    // Read positions
    System.out.println("Reading position map file...");
    CSVReader r = new CSVReader(probePositionMapFile);
    String[] line;
    while ((line=r.getNextLine())!=null) {
      int start, end;
      try {
        start = Integer.parseInt(line[f1StartCol]);
        end = Integer.parseInt(line[f1EndCol]);
      } catch(Exception e) {
        e.printStackTrace();
        continue;
      }
      
      probePositionMap.put(line[f1ProbeCol].toUpperCase().trim(), new ValuePair<Integer, Integer>(start, end));
    }
    
    
    // Read gene identifier
    System.out.println("Reading gene-id map file...");
    r = new CSVReader(probeGeneMapFile);
    while ((line=r.getNextLine())!=null) {
      probeGeneMap.put(line[f2ProbeCol].toUpperCase().trim(), line[f2GeneCol]);
    }
    
    
    // Annotate file
    System.out.println("Writing new file...");
    BufferedWriter out = new BufferedWriter(new FileWriter(FileTools.removeFileExtension(fileToAnnotate)+"-annot" + (!removAllDataWithoutGeneIdentifier?"-all":"") + ".txt"));
    r = new CSVReader(fileToAnnotate);
    out.write("Gene\tprobe_start\tprobe_end\t");
    if (r.getHeader()!=null) {
      out.write(StringUtil.implode(r.getHeader(), "\t"));
    } else {
      for (int i=1; i<=r.getNumberOfColumns(); i++) {
        out.write("OriginalColumn " + i + "\t");
      }
      // Harcoded headers for bsp-rat-study
//      out.write("Probeset_ID\tAA_1D_Hd14_foldchange\tCFX_1O_d14_foldchange\tCIDB_1C_Hd14_foldchange\tCPA_1A_Hd14_foldchange\tDES_1H_Hd14_foldchange\tDHEA_1D_Hd14_foldchange\tDMN_1C_Hd14_foldchange\tETH_1H_Hd14_foldchange\tMcarb_1D_Hd14_foldchange\tMPy_1D_Hd14_foldchange\tNif_1O_d14_foldchange\tPB_1B_Hd14_foldchange\tPBO_1H_Hd14_foldchange\tTAA_1D_Hd14_foldchange\tWy_1D_Hd14_foldchange");
    }
    out.write('\n');
    while ((line=r.getNextLine())!=null) {
      String id = line[f3ProbeCol].toUpperCase().trim();
      String geneID = probeGeneMap.get(id);
      boolean isValidGeneID = geneID!=null && geneID.length()>0;
      if (removAllDataWithoutGeneIdentifier && !isValidGeneID) continue;
      ValuePair<Integer, Integer> pos = probePositionMap.get(id);
      
      out.write(isValidGeneID?geneID:"n/a");
      out.write('\t');
      out.write(pos!=null && pos.getA()!=null?Integer.toString(pos.getA()):"0");
      out.write('\t');
      out.write(pos!=null && pos.getB()!=null?Integer.toString(pos.getB()):"0");
      out.write('\t');
      out.write(StringUtil.implode(line, "\t"));
      out.write('\n');
    }
    out.close();
    
  }
  
  
  static void filterCSV() throws IOException {
    String[] miRs = new String[]{"1188", "1193", "1197", "1247", "127", "134", "136", "154", "1906-1", "300", "323", "329", "337", "341", "369", "370", "376b", "377", "379", "381", "382", "410", "411", "412", "431", "432", "433", "434", "453", "485", "487b", "493", "494", "495", "496", "539", "540", "541", "543", "544", "654", "665", "666", "667", "673", "679", "758", "770", "882"};
    String fileToAnnotate = "C:\\Users\\wrzodek\\Desktop\\gene_centered_data\\ekuta_study_miRNA.txt";
    int f1ProbeCol=0;
    
    boolean removAllDataWithoutMatch=true;
    
    // Annotate file
    System.out.println("Writing new file...");
    BufferedWriter out = new BufferedWriter(new FileWriter(FileTools.removeFileExtension(fileToAnnotate)+"-DLK1" + (!removAllDataWithoutMatch?"-all":"") + ".txt"));
    CSVReader r = new CSVReader(fileToAnnotate);
    out.write(StringUtil.implode(r.getHeader(), "\t"));
    out.write("\tisInDLK1");
    out.write('\n');
    String[] line;
    while ((line=r.getNextLine())!=null) {
      String append = "";
      String miRNAname = line[f1ProbeCol].toUpperCase().trim();
      int pos = ArrayUtils.isContainedIn(miRs, miRNAname, true);
      if (pos>=0) {
        append = (StringUtil.isWord(miRNAname, miRs[pos]))?"X":"U";
      } else if (removAllDataWithoutMatch){
        continue;
      }
      
      out.write(StringUtil.implode(line, "\t"));
      out.write('\t');
      out.write(append);
      out.write('\n');
    }
    out.close();


    
  }
  
}

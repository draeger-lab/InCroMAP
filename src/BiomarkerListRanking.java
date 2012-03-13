import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.zbit.data.GeneID;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.protein.ProteinModificationExpression;
import de.zbit.gui.IntegratorUITools;
import de.zbit.io.DirectoryParser;
import de.zbit.io.FileTools;
import de.zbit.io.NameAndSignalReader;
import de.zbit.io.SerializableTools;
import de.zbit.io.mRNAReader;
import de.zbit.io.csv.CSVReader;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.mapper.GeneSymbol2GeneIDMapper;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.mapper.MicroRNAsn2GeneIDMapper;
import de.zbit.util.Species;
import de.zbit.util.StringUtil;
import de.zbit.util.Utils;
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
public class BiomarkerListRanking {
  static String inFile = "C:\\Users\\wrzodek\\Desktop\\workspace\\Integrator\\Putative Biomarker List v2_reloaded.txt";
  static int f1HGNC_Col=2;
  
  private static Species lastSpecies;
  
  /**
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    
    
    //annotateCandidateList();
    
    specialNSRead();
    
  }
  
  static String[] organisms = new String[]{"mouse", "human", "rat"};
  static String[] organism_prefixes = new String[]{"mmu", "hsa", "rno"};
  
  static GeneSymbol2GeneIDMapper[] gs2gid = new GeneSymbol2GeneIDMapper[3];
  static MicroRNAsn2GeneIDMapper mi2gid = null;
  
  /**
   * Organism Independent!
   */
  static GeneID2GeneSymbolMapper gi2gs = null;
  
  /**
   * @throws IOException
   * @throws Exception
   */
  protected static void annotateCandidateList() throws IOException, Exception {
    // Prepare HGNC mapper
    System.out.println("Preparing entrez-mapper...");
    for (int i=0; i<organisms.length; i++) {
      if (gs2gid[i]==null) gs2gid[i] = new GeneSymbol2GeneIDMapper(organisms[i]);
    }    
    if (mi2gid==null) mi2gid = new MicroRNAsn2GeneIDMapper();
    
    
    
    // Read input file
    System.out.println("Reading input file...");
    CSVReader r = new CSVReader(inFile, true);
    
    String[] line;
    Set<Integer> processedIDs = new HashSet<Integer>();
    List<String[]> lines = new ArrayList<String[]>();
    Map<Integer, String[]> entrez2line = new HashMap<Integer, String[]>();
    while ((line=r.getNextLine())!=null) {
      
      // Read gene symbol from line and continue if valid
      String id = (f1HGNC_Col>=line.length)?null:line[f1HGNC_Col].toUpperCase().trim();
      if (id!=null && id.length()>0) {
        int offset = line.length;
        String[] newLine = new String[offset + organisms.length];
        System.arraycopy(line, 0, newLine, 0, offset);
        line = newLine;
        
        // Correct formatting erros / split to single identifiers
        for (String realId: id.split("///|/|\\|")) {
          line = newLine.clone();
          realId = realId.trim();
          if (realId.endsWith("_PREDICTED")) realId = realId.substring(0, realId.length()-10).trim();
          
          int mirPos = Math.max(realId.indexOf("MIR-"), realId.indexOf("LET-"));
          boolean isMiRNA = false;
          if (mirPos>=0) {
            realId = realId.substring(mirPos);
            isMiRNA = true;
            realId = realId.toLowerCase().replace("mir", "miR");
          }
          line[2] = realId;
          
          if (realId.contains(" ")) System.out.println(String.format("Please fix '%s': %s", realId, Arrays.toString(line)));
          
          // map to entrez
          Integer entrez;
          boolean noEntrezFound=true;
          boolean duplicatedInAnyOrganism=false;
          for (int i=0; i<organisms.length; i++) {
            entrez = getEntrezID(realId, i);
            if (entrez!=null && entrez>0) noEntrezFound = false;
            boolean isDuplicate = addEntrezId(offset+i, line, processedIDs, realId, entrez);
            if (isDuplicate) {
              duplicatedInAnyOrganism=true;
              String[] line2 = entrez2line.get(entrez);
              if (!line2[0].contains(line[0])) line2[0]+=","+line[0];
              if (line2[3].length()<1) line2[3]=line[3];
            } else {
              entrez2line.put(entrez, line);
            }
          }
          
          if (!duplicatedInAnyOrganism) {
            lines.add(line);
          }
          
          if (noEntrezFound) {
            System.out.println(String.format("Could not map '%s': %s", realId, Arrays.toString(line)));
          }
          
        }
      }
      
      
      
    }
    
    
    // Write new file
    System.out.println("Writing new file");
    BufferedWriter out = new BufferedWriter(new FileWriter(getOutFileName()));
    
    // Write headers
    if (r.getHeader()!=null) {
      out.write(StringUtil.implode(r.getHeader(), "\t"));
    } else {
      for (int i=1; i<=r.getNumberOfColumns(); i++) {
        if (i>1) out.write('\t');
        out.write("OriginalColumn " + i);
      }
    }
    // new headers
    for (int i=0; i<organisms.length; i++) {
      out.write("\tEntrez " + organisms[i]);
    }
    out.write('\n');
    // new content
    for (String[] cline : lines) {
      out.write(StringUtil.implode(cline, "\t"));
      out.write('\n');
    }
    out.close();
  }


  /**
   * @param realId HGNC symbol
   * @param i
   * @return entrez gene id for {@link #organisms} [<code>i</code>].
   * @throws Exception
   */
  protected static Integer getEntrezID(String realId, int i) throws Exception {
    // Init mappers
    for (int j=0; j<organisms.length; j++) {
      if (gs2gid[j]==null) gs2gid[j] = new GeneSymbol2GeneIDMapper(organisms[j]);
    }    
    if (mi2gid==null) mi2gid = new MicroRNAsn2GeneIDMapper();
    
    // Prepare symbol
    Integer entrez;
    String checkID = realId.toUpperCase();
    int mirPos = Math.max(checkID.indexOf("MIR-"), checkID.indexOf("LET-"));
    boolean isMiRNA = false;
    if (mirPos>=0) {
      realId = realId.substring(mirPos);
      isMiRNA = true;
      realId = realId.toLowerCase().replace("mir", "miR");
    }

    // Map to gene id
    if (!isMiRNA) {
      entrez = gs2gid[i].map(realId);
    } else {
      entrez = mi2gid.map(new miRNA(organism_prefixes[i]+"-"+realId).getPrecursorName());
      if (entrez==null) {
        // Retry with "-1". E.g. "let-7f" is "let-7f-1" in our index. 
        entrez = mi2gid.map(new miRNA(organism_prefixes[i]+"-"+realId).getPrecursorName()+"-1");
      }
    }
    return entrez;
  }
  
  
  /**
   * @return
   */
  protected static String getOutFileName() {
    return FileTools.removeFileExtension(inFile)+"-annot" + ".txt";
  }
  
  
  public static void specialNSRead() throws IOException, Exception {
    boolean unbiased = true;
    
    if (unbiased && gi2gs==null) {
      // Build one big geneID 2 GeneSymbolMapper
      gi2gs = new GeneID2GeneSymbolMapper(organisms[0]);
      for (int i=1; i<organisms.length; i++) {
        gi2gs.getMapping().putAll(new GeneID2GeneSymbolMapper(organisms[i]).getMapping());
      }
      // MicroRNAs
      if (mi2gid==null) {
        mi2gid = new MicroRNAsn2GeneIDMapper();
      }
      Map<String, Integer> miMapper =  mi2gid.getMapping();
      for (Entry<String, Integer>  e: miMapper.entrySet()) {
        gi2gs.getMapping().put(e.getValue(), e.getKey());
      }
    }
    
    
    // Read annotated candidate list
    mRNAReader mr = new mRNAReader(4, IdentifierType.NCBI_GeneID, Species.search(IntegratorUITools.organisms, "mmu", Species.KEGG_ABBR));
    String[] adKeys = new String[]{"WP", "Candidate Biomarker", "HGNC Symbol", "Comments"};
    String[] eKeys = new String[] {"Entrez mouse", "Entrez human", "Entrez rat"};
    int adi = 0; // Must result in same order as appear in columns of input file
    for (int i=0; i<adKeys.length; i++) {
      mr.addAdditionalData(adi++, adKeys[i]);
    }
    for (int i=0; i<eKeys.length; i++) {
      mr.addAdditionalData(adi++, eKeys[i]);
    }
    List<NameAndSignals> bmList = new ArrayList<NameAndSignals>( mr.read(getOutFileName()) );
    
    // Create an organism-independent list of entrez2ns 
    Map<Integer, NameAndSignals> entrez2ns = new HashMap<Integer, NameAndSignals>();
    Map<String, NameAndSignals> hgnc2ns = new HashMap<String, NameAndSignals>();
    for (NameAndSignals m: bmList) {
      hgnc2ns.put(m.getName().toUpperCase().trim(), m);
      for (String key : eKeys) {
        String entrez = m.getData(key).toString();
        if (entrez!=null && Utils.isNumber(entrez, true)) {
          entrez2ns.put(Integer.parseInt(entrez), m);
        }
      }
    }
    
    // Read every processed file available with an appropriate reader
    // Search in entrez2ns list (name AND geneID for miRNAs!)
    // Add signals to it
    String[] data_dirs = new String[]{
        "C:\\Users\\wrzodek\\Desktop\\EKUT-A\\gene_centered_data",
        "C:\\Users\\wrzodek\\Desktop\\MARCAR\\bsp_rat_study\\gene_centered_data"
    };
    SortedSet<ValuePair<String, SignalType>> signalList = new TreeSet<ValuePair<String, SignalType>>();
    for (String dir: data_dirs) {
      DirectoryParser dp = new DirectoryParser(dir, "-reader.dat");
      while (dp.hasNext()) {
        // Read dataset 
        String serializedReader = dp.next();
        Collection<NameAndSignals> dataset = readWithSerializedReader(dp.getPath() + serializedReader);
        String partnerName = getPartnerName(serializedReader);
        
        // gene-center MaximumDistanceToZero for DNAm and ProtMod; Mean for all others 
        MergeType mt = DNAmethylation.class.isAssignableFrom(NameAndSignals.getType(dataset)) ||
        ProteinModificationExpression.class.isAssignableFrom(NameAndSignals.getType(dataset))?
            MergeType.MaximumDistanceToZero:MergeType.Mean;
        dataset = NameAndSignals.geneCentered(dataset, mt);
        if (ProteinModificationExpression.class.isAssignableFrom(NameAndSignals.getType(dataset))) {
          dataset = NameAndSignals.geneCenteredByGeneID(dataset, mt);
        }
        
        // Calculate rank/n*100 for every signal
        List<NameAndSignals> datasetList = new ArrayList<NameAndSignals>(dataset);
        Collection<ValuePair<String, SignalType>> signalIDs = dataset.iterator().next().getSignalNames();
        for (ValuePair<String, SignalType> sid: signalIDs ) {
          convertToAbsoluteValues(datasetList, sid);
          Collections.sort(datasetList, Signal.getComparator(sid.getA(), sid.getB()));
          for (int i=0; i<datasetList.size(); i++) {
            float rankValue = (float)((double)(i+1)/(double)datasetList.size()*100d);
//            if (((GeneID)datasetList.get(i)).getGeneID()==13088) {
//              // 13088 == CYP2B10
//              System.out.println(datasetList.get(i).getSignalValue(sid.getB(), sid.getA()));
//              System.out.println(rankValue);
//            }
            datasetList.get(i).getSignal(sid.getB(), sid.getA()).setSignal(rankValue);
          }
        }
        
        // Add signals to current ds
        for (NameAndSignals ns : dataset) {
          // Loook for a candidate biomarker that matches this instance
          NameAndSignals gene = null;
          if (GeneID.class.isAssignableFrom(ns.getClass())) {
            int geneId =((GeneID)ns).getGeneID();
            if (geneId>0 && geneId<Integer.MAX_VALUE) {
              gene = entrez2ns.get(geneId);
              /*
               * UNBIASED SOLUTION!
               */
              if (unbiased && gene==null) {
                gene = ns;
                String symbol = gi2gs.map(geneId);
                if (symbol==null) symbol = "ENTREZ:"+geneId;
                
                entrez2ns.put(geneId, ns);
                addToBiomarkerList(bmList, ns, symbol);
                if (symbol.startsWith("ENTREZ")) {
                  ns.addData("Entrez " + lastSpecies.getCommonName().toLowerCase().trim(), geneId);
                }
                for (int i=0; i<organisms.length; i++) {
                  Integer entrez = getEntrezID(symbol, i);
                  if (entrez!=null && entrez>0 && entrez<Integer.MAX_VALUE) {
                    entrez2ns.put(entrez, ns);
                  }
                }
              }
              // ***********
            }
          } if (gene==null && miRNA.class.isAssignableFrom(ns.getClass())) {
            String orgName = ns.getName().toUpperCase().trim();
            int mirPos = Math.max(orgName.indexOf("MIR-"), orgName.indexOf("LET-"));
            if (mirPos>=0) {
              orgName = orgName.substring(mirPos).trim();
            }
            gene = hgnc2ns.get(orgName);
            
            /*
             * UNBIASED SOLUTION!
             */
            if (unbiased && gene==null) {
              gene = ns;
              hgnc2ns.put(orgName, gene);
              addToBiomarkerList(bmList, ns, orgName);
            }
            // ***********
            
          }
          
          if (gene==null) continue; // No matching bm candidate
          
          // Add signals to candidate biomarker
          for (Signal sig: new ArrayList<Signal>(ns.getSignals())) { // Clone list for unbiased solution to avoid concurrency modifications
            String newName = String.format("%s %s %s [%s]", StringUtil.firstLetterUpperCase(lastSpecies.getCommonName()), partnerName, sig.getName(), NameAndSignals.getType(ns).getSimpleName() );
            Signal sigNew = sig.clone();
            sigNew.setName(newName);
            gene.addSignal(sigNew);
            // TODO: Track a nice signal description as header
            // Organism, Partner (e.g. EKUT-A), DatasetName, DataType (e.g. DNAm), ObservationName
            // Also ensure that name is UNIQUE!!!!!
            signalList.add(sigNew.getSignalAndName());
          }
        }
      }
    }
    
    // Write new file
    System.out.println("Writing new file");
    BufferedWriter out = new BufferedWriter(new FileWriter(FileTools.removeFileExtension(getOutFileName())+"-withData" + (unbiased?"-unbiased":"") + ".txt"));
    
    // Write headers
    boolean isFirst = true;
    for (String key: adKeys) {
      out.write((!isFirst?"\t":"")+key);
      isFirst = false;
    }
    for (String key: eKeys) {
      out.write("\t"+key);
    }
    // signal headers
    Iterator<ValuePair<String, SignalType>> it = signalList.iterator();
    while (it.hasNext()) {
      out.write("\t"+it.next().getA());
    }
    out.write('\n');
    
    // new content
    ValuePair<String, SignalType> s;
    for (NameAndSignals ns: bmList) {
      isFirst = true;
      for (String key: adKeys) {
        out.write((!isFirst?"\t":"")+ns.getData(key));
        isFirst = false;
      }
      for (String key: eKeys) {
        out.write("\t"+ns.getData(key));
      }
      it = signalList.iterator();
      while (it.hasNext()) {
        s = it.next();
        float value = ns.getSignalValue(s.getB(), s.getA()).floatValue();
        if (Float.isNaN(value) || Float.isInfinite(value)) {
          out.write("\tn/a");
        } else {
          out.write("\t"+value);
        }
      }
      out.write('\n');
    }
    out.close();
    
  }


  /**
   * @param bmList
   * @param ns
   * @throws Exception 
   */
  protected static void addToBiomarkerList(List<NameAndSignals> bmList,
    NameAndSignals ns, String symbol) throws Exception {
    bmList.add(ns);
    
    String[] adKeys = new String[]{"WP", "Candidate Biomarker", "HGNC Symbol", "Comments"};
    String[] eKeys = new String[] {"Entrez mouse", "Entrez human", "Entrez rat"};
    ns.addData(adKeys[0], "4");
    ns.addData(adKeys[1], symbol);
    ns.addData(adKeys[2], symbol);
    ns.addData(adKeys[3], "");
    for (int i=0; i<organisms.length; i++) {
      Integer entrez = getEntrezID(symbol, i);
      if (entrez!=null && entrez>0 && entrez<Integer.MAX_VALUE) {
        ns.addData(eKeys[i], entrez);
      } else {
        ns.addData(eKeys[i], "n/a");
      }
    }
  }
  
  
  /**
   * @param datasetList
   * @param sid
   */
  private static void convertToAbsoluteValues(List<NameAndSignals> datasetList,
    ValuePair<String, SignalType> sid) {
    for (NameAndSignals ns : datasetList) {
      Signal sig = ns.getSignal(sid.getB(), sid.getA());
      sig.setSignal(Math.abs(sig.getSignal().doubleValue()));
    }
  }


  /**
   * @param serializedReader
   * @return
   */
  private static String getPartnerName(String serializedReader) {
    serializedReader = serializedReader.toLowerCase().replace("_", "").replace("-", "").replace(" ", "");
    if (serializedReader.contains("ekuta")) return "EKUT-A";
    if (serializedReader.contains("bsp")) return "BSP";
    if (serializedReader.contains("muw")) return "MUW";
    if (serializedReader.contains("nov")) return "NOV";
    if (serializedReader.contains("nmi")) return "NMI";
    if (serializedReader.contains("cxr")) return "CXR";
    
    return "Unknown";
  }


  public static Collection<NameAndSignals> readWithSerializedReader(String readerFile) throws IOException, Exception {
    System.out.println("Reading "  + readerFile);
    NameAndSignalReader<?> r = (NameAndSignalReader<?>) SerializableTools.loadObject(readerFile);
    String inFile = readerFile.replace("-reader.dat", ".txt");
    lastSpecies = r.getSpecies();
    return (Collection<NameAndSignals>) r.read(inFile);
  }
  
  /**
   * @param offset slot in line array to write in
   * @param line
   * @param processedIDs
   * @param id
   * @param entrez
   * @return isDuplicate always false if could not map to a gene-id!
   * @throws IOException
   */
  protected static boolean addEntrezId(int offset, String[] line,
    Set<Integer> processedIDs, String id, Integer entrez) throws IOException {
    boolean ret = false;
    
    if (entrez==null || entrez<=0) {
      line[offset] = ("n/a");
    } else {
      if (!processedIDs.add(entrez)) {
        //System.out.println(String.format("Duplicate item '%s': %s", id, Arrays.toString(line)));
        ret = true;
      }
      line[offset]= (entrez+"");
    }
    
    return ret;
  }
  
}

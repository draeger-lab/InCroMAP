/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.io;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorGUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.CSVImporterV2.CSVImporterV2;
import de.zbit.gui.CSVImporterV2.ExpectedColumn;
import de.zbit.mapper.AbstractMapper;
import de.zbit.mapper.MappingUtils;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.parser.Species;
import de.zbit.util.ProgressBar;
import de.zbit.util.ValuePair;

/**
 * A generic reader to read mRNA data.
 * @author Clemens Wrzodek
 */
public class mRNAReader extends NameAndSignalReader<mRNA> {
  public static final transient Logger log = Logger.getLogger(mRNAReader.class.getName());
  
  /**
   * Type of Identifier. Anything else than
   * Numberic (GeneID) will be mapped to this.
   */
  private IdentifierType idType;
  
  /**
   * Required for Ensembl or GeneSymbol 2 GeneID Mappers.
   * Else, this is not required and may be null.
   */
  private Species species;
  
  /**
   * Map the ID to an NCBI gene id. If this is null, the input is 
   * expected to be an NCBI gene id.
   */
  private AbstractMapper<String, Integer> mapper = null;
  
  /**
   * A second identifier that is used as Backup, <B>only if the
   * first identifier is the GeneID</B>.
   * Assigns this identifier as name, instead of the GeneID.
   * And if the GeneID is not defined, tries to map this Identifier
   * to a GeneID. It is recommended using GeneSymbols as secondary
   * and GeneID as primary identifiers, though, other combinations
   * work too.
   */
  private ValuePair<Integer, IdentifierType> secondID = null;
  
  /**
   * @return  This method returns all {@link ExpectedColumn}s required
   * to read a new file with the {@link CSVImporterV2}. This is
   * [0] an identifier and [1-10] signal columns.
   */
  public static ExpectedColumn[] getExpectedColumns() {
    List<ExpectedColumn> list = new ArrayList<ExpectedColumn>();
    List<IdentifierType> idTypes = new ArrayList<IdentifierType>(Arrays.asList(IdentifierType.values()));
    idTypes.remove(IdentifierType.Unknown);
    
    // The user may choose multiple identifier columns
    ExpectedColumn e = new ExpectedColumn("Identifier", idTypes.toArray(),true,true,true,false,null);
    list.add(e);
    list.addAll(NameAndSignalReader.getExpectedSignalColumns(10));
    return list.toArray(new ExpectedColumn[0]);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#importWithGUI(java.awt.Component, java.lang.String)
   */
  @Override
  public Collection<mRNA> importWithGUI(Component parent, String file) {
    
    // Create a new panel that allows selection of species
    JLabeledComponent spec = IntegratorGUITools.getOrganismSelector();
    
    // Create and show the import dialog
    try {
      
      // Show the CSV Import dialog
      ExpectedColumn[] exCol = getExpectedColumns();
      final CSVImporterV2 c = new CSVImporterV2(file, exCol);
      boolean dialogConfirmed = IntegratorGUITools.showCSVImportDialog(parent, c, spec);
      
      // Process user input and read data
      if (dialogConfirmed) {
        // Read all columns and types
        nameCol = exCol[0].getAssignedColumn();
        this.idType = (IdentifierType) exCol[0].getAssignedType();
        this.species = (Species) spec.getSelectedItem();
        for (int i=1; i<exCol.length; i++) {
          if (exCol[i].hasAssignedColumns()) {
            for (int j=0; j<exCol[i].getAssignedColumns().size(); i++) {
              addSignalColumn(exCol[i].getAssignedColumns().get(j), 
                (SignalType) exCol[i].getAssignedType(j), exCol[i].getName().toString());
            }
          }
        }
        
        try {
          return read(c.getApprovedCSVReader());
        } catch (Exception e) {
          GUITools.showErrorMessage(parent, e, "Could not read input file.");
        }
      }
      
    } catch (IOException e) {
      GUITools.showErrorMessage(parent, e);
    }

    
    // Only errors or canceled
    return null;
  }
  
  /**
   * This is ONLY for use in combination with {@link #importWithGUI(String)} afterwards.
   */
  public mRNAReader() {
    super(-1);
  }
  
  public mRNAReader(int identifierCol, IdentifierType idType, Species species) {
    super(identifierCol);
    this.idType = idType;
    this.species = species;
  }
  
  /**
   * It is strongly recommended setting the primary identifier to a
   * GeneID column and this (secondary) identifier to a gene symbol
   * column. See {@link #secondID} for more information!
   * @see #secondID
   * @param col
   * @param type
   */
  public void addSecondIdentifier(int col, IdentifierType type) {
    secondID = new ValuePair<Integer, IdentifierType>(col, type);
  }

  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#read(de.zbit.io.CSVReader)
   */
  @Override
  public Collection<mRNA> read(CSVReader inputCSV) throws IOException, Exception {
    // Init Mapper (primary for idType)
    if (!idType.equals(IdentifierType.NCBI_GeneID)) {
      mapper = MappingUtils.initialize2GeneIDMapper(idType, progress, species);
    } else {
      // Only if primary identifier does not require a mapper,
      // init one for the secondary identifier
      mapper = MappingUtils.initialize2GeneIDMapper(secondID.getB(), progress, species);
    }
    if (mapper!=null) mapper.readMappingData();
    
    // Read file
    Collection<mRNA> ret =  super.read(inputCSV);
    
    // Free resources
    mapper = null;
    return ret;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#createObject(java.lang.String, java.lang.String[])
   */
  @Override
  protected mRNA createObject(String name, String[] line) throws Exception {
    // Map to GeneID
    Integer geneID = null;
    if (!idType.equals(IdentifierType.NCBI_GeneID)) {
      geneID = mapper.map(name);
    } else {
      try {
        geneID = Integer.parseInt(name);
        if (geneID<=0) geneID=null;
      } catch (NumberFormatException e) {
        log.warning("Could not parse GeneID from String'" + name + "'.");
        geneID=null;
      }
      
      // Use the second identifier as Backup
      // and to store a better name
      if (secondID!=null) {
        String secondIdentifier = line[secondID.getA()];
        if (geneID==null && mapper!=null) {
          geneID = mapper.map(secondIdentifier);
        }
        name = secondIdentifier;
      }
    }
    
    // Create mRNA
    mRNA m;
    if (geneID!=null) {
      m = new mRNA(name, geneID);
    } else {
      m = new mRNA(name);
    }
    
    return m;
  }
  
  
  /**
   * @param args
   * @throws Exception 
   * @throws IOException 
   */
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws IOException, Exception {
    mRNAReader r2 = new mRNAReader();
    r2.importWithGUI(null, "mRNA_data_new.txt");
    if (true) return;
    
    Species species = Species.search((List<Species>)Species.loadFromCSV("species.txt"), "mouse", -1);
    
    // Jaworski Dataset:
//    mRNAReader r = new mRNAReader("Ctnnb1",16, IdentifierType.Numeric,species);
//    r.addSecondIdentifier(13, IdentifierType.Symbol);
//    r.addAdditionalData(0, "probe_name");
//    r.addAdditionalData(12, "description");
//    r.addSignalColumn(7, SignalType.FoldChange); // 7-9 = Cat/Ras/Ras_vs_Cat; 10=Cat_vs_Ras
//    r.addSignalColumn(4, SignalType.pValue); // 4-6 = Cat/Ras/Ras_vs_Cat
//    
//    r.progress = new ProgressBar(0);
//    Collection<mRNA> c = r.read("mRNA_data.txt");
    
    // New dataset
    mRNAReader r = new mRNAReader(3, IdentifierType.NCBI_GeneID, species);
    r.addSecondIdentifier(1, IdentifierType.GeneSymbol);
    r.addAdditionalData(0, "probe_name");
    r.addAdditionalData(2, "description");
    r.addSignalColumn(27, SignalType.FoldChange, "Ctnnb1"); // 27-30 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    r.addSignalColumn(31, SignalType.pValue, "Ctnnb1"); // 31-34 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    
    r.progress = new ProgressBar(0);
    Collection<mRNA> c = r.read("mRNA_data_new.txt");
    int noGI=0;
    for (mRNA mRNA : c) {
      if (mRNA.getGeneID()<0) noGI++;
      System.out.println(mRNA);
    }
    System.out.println(noGI + " mRNAs without Gene ID.");
  }

  
}

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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.miRNA.miRNA;
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
import de.zbit.util.ValueTriplet;

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
   * This column is the most readable name, that will define the
   * Name of the {@link NameAndSignals} object. It is not equal
   * to the primary identifier (which is the most machine readable id).
   */
  private int preferredNameColumn=-1;
  
  /**
   * Remember already issued warnings to not issue it multiple times.
   */
  private Set<String> issuedWarnings = new HashSet<String>();
  
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
        setNameAndIdentifierTypes(exCol[0]);        
        this.species = (Species) spec.getSelectedItem();
        for (int i=1; i<exCol.length; i++) {
          if (exCol[i].hasAssignedColumns()) {
            for (int j=0; j<exCol[i].getAssignedColumns().size(); j++) {
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
   * Evaluates the selected identifiers and builds the reader according
   * to the priority of selected identifiers.
   * @param expectedColumn
   */
  private void setNameAndIdentifierTypes(ExpectedColumn idCol) {
    int selCols = idCol.getAssignedColumns().size();
    
    // Create a list of columns, types and priorities
    List<ValueTriplet<Integer, IdentifierType, Integer>> l = 
      new LinkedList<ValueTriplet<Integer, IdentifierType, Integer>>();
    ValueTriplet<Integer, IdentifierType, Integer> vp = null;
    for (int i=0; i<selCols; i++) {
      // Get type and infere priority
      IdentifierType type = (IdentifierType) idCol.getAssignedType(i);
      
      vp = new ValueTriplet<Integer, IdentifierType, Integer>(
          idCol.getAssignedColumns().get(i), type, IntegratorGUITools.getPriority(type)); 
      l.add(vp);
    }
    // Sort by priority
   Collections.sort(l, vp.getComparator_OnlyCompareC());
    
    // add ids to reader
   this.nameCol = l.get(0).getA();
   this.idType = l.get(0).getB();
   if (l.size()>1) {
     addSecondIdentifier(l.get(1).getA(), l.get(1).getB());
     for (int i=2; i<l.size(); i++) {
       addAdditionalData(l.get(i).getA(), l.get(i).getB().toString());
     }
   }
   
   // Most human readable name is the name, last in the list
   preferredNameColumn = l.get(l.size()-1).getA();
   // But still, Gene symbols should be preferred to descriptions
   for (int i=l.size()-1; i>=0; i--) {
     if (l.get(i).getB().equals(IdentifierType.GeneSymbol)) {
       preferredNameColumn = l.get(i).getA();
       if (i>=2) { // If it is no primary or secondary identifier,  do not add it twice...
         removeAdditionalData(l.get(i).getA(), l.get(i).getB().toString());
       }
       break;
     }
   }
   
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
    this.preferredNameColumn = identifierCol;
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
    if (type.equals(IdentifierType.GeneSymbol)) {
      preferredNameColumn = col;
    }
  }

  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#read(de.zbit.io.CSVReader)
   */
  @Override
  public Collection<mRNA> read(CSVReader inputCSV) throws IOException, Exception {
    // Init Mapper (primary for idType)
    if (!idType.equals(IdentifierType.NCBI_GeneID)) {
      mapper = MappingUtils.initialize2GeneIDMapper(idType, progress, species);
    } else if (secondID!=null) {
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
   * @see de.zbit.io.NameAndSignalReader#read(java.lang.String[])
   */
  @Override
  public Collection<mRNA> read(String[] identifiers) throws IOException, Exception {
    // Init Mapper (primary for idType)
    if (!idType.equals(IdentifierType.NCBI_GeneID)) {
      mapper = MappingUtils.initialize2GeneIDMapper(idType, progress, species);
    }
    if (mapper!=null) mapper.readMappingData();
    
    // Read file
    Collection<mRNA> ret =  super.read(identifiers);
    
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
      // Primary identifier is a gene id.
      try {
        geneID = Integer.parseInt(name);
        if (geneID<=0) geneID=null;
      } catch (NumberFormatException e) {
        String warning = String.format("Could not parse GeneID from String '%s'.", name);
        if (!issuedWarnings.contains(warning)) {
          log.warning(warning);
          issuedWarnings.add(warning);
        }
        
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
    
    // Change name to a more human readable one.
    boolean addSecondIDasAdditionalInfo=false;
    if (preferredNameColumn>=0) {
      name = line[preferredNameColumn];
      if (secondID!=null && (!secondID.getA().equals(preferredNameColumn))) {
        addSecondIDasAdditionalInfo = true;
      }
    }
    
    // Create mRNA
    mRNA m;
    if (geneID!=null) {
      m = new mRNA(name, geneID);
    } else {
      m = new mRNA(name);
    }
    
    // SecondID is normally the name. If not, still keep this
    // information as additional information.
    if (addSecondIDasAdditionalInfo) {
      m.addData(secondID.getB().toString(), line[secondID.getA()]);
    }
    
    return m;
  }
  
  
  /**
   * @param args
   * @throws Exception 
   * @throws IOException 
   */
  @SuppressWarnings({ "unused" })
  public static void main(String[] args) throws IOException, Exception {
    mRNAReader r2 = new mRNAReader();
    r2.importWithGUI(null, "mRNA_data_new.txt");
    if (true) return;
    
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
    
    mRNAReader r = getExampleReader();
    r.progress = new ProgressBar(0);
    Collection<mRNA> c = r.read("mRNA_data_new.txt");
    
    int noGI=0;
    for (mRNA mRNA : c) {
      if (mRNA.getGeneID()<0) noGI++;
      System.out.println(mRNA);
    }
    System.out.println(noGI + " mRNAs without Gene ID.");
  }

  public static mRNAReader getExampleReader() throws IOException {
    @SuppressWarnings("unchecked")
    Species species = Species.search((List<Species>)Species.loadFromCSV("species.txt"), "mouse", -1);
    // New dataset
    mRNAReader r = new mRNAReader(3, IdentifierType.NCBI_GeneID, species);
    r.addSecondIdentifier(1, IdentifierType.GeneSymbol);
    r.addAdditionalData(0, miRNA.probeNameKey);
    r.addAdditionalData(2, "description");
    r.addSignalColumn(27, SignalType.FoldChange, "Ctnnb1"); // 27-30 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    r.addSignalColumn(31, SignalType.pValue, "Ctnnb1"); // 31-34 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
    
    return r;
  }

  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#getSpecies()
   */
  @Override
  public Species getSpecies() {
    return species;
  }
  
}

/**
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
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.integrator.ReaderCache;
import de.zbit.integrator.ReaderCacheElement;
import de.zbit.mapper.AbstractMapper;
import de.zbit.mapper.MappingUtils;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.parser.Species;
import de.zbit.util.ArrayUtils;
import de.zbit.util.ValuePair;
import de.zbit.util.ValueTriplet;

/**
 * An Abstract extension of {@link NameAndSignalReader} to read
 * gene-based data, that might contain various gene identifiers
 * (such as Ensembl, KEGG, entrez gene ids), signals and further
 * additional columns.
 * 
 * @author Clemens Wrzodek
 * @param <T>
 */
public abstract class  AbstractGeneBasedNSreader<T extends NameAndSignals> extends NameAndSignalReader<T> {
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
  public ExpectedColumn[] getExpectedColumns() {
    List<ExpectedColumn> list = new ArrayList<ExpectedColumn>();
    List<IdentifierType> idTypes = new ArrayList<IdentifierType>(Arrays.asList(IdentifierType.values()));
    idTypes.remove(IdentifierType.Unknown);
    
    // The user may choose multiple identifier columns
    ExpectedColumn e = new ExpectedColumn("Identifier", idTypes.toArray(),true,true,true,false,null);
    list.add(e);
    
    List<ExpectedColumn> additional = getAdditionalExpectedColumns();
    if (additional!=null) list.addAll(additional);
    
    Collection<ExpectedColumn> ec = getExpectedSignalColumnsOverridable(10);
    if (ec!=null) list.addAll(ec);
    return list.toArray(new ExpectedColumn[0]);
  }
  
  /**
   * Creates a collection of maxNumberOfObservations optional {@link ExpectedColumn}s
   * that accepts FoldChanges and pValues of renameable signals. This is optimal for 
   * importing {@link Signal} using the {@link CSVImporterV2}.
   * <p>Note: Extending classes can override this method!
   * @param maxNumberOfObservations number of expected observation columns to create
   * @return 
   */
  protected Collection<ExpectedColumn> getExpectedSignalColumnsOverridable(int maxNumberOfObservations) {
    // Note: Extending classes can override this method!
    return NameAndSignalReader.getExpectedSignalColumns(10);
  }

  /**
   * By default, a "Identifier" column and some Signal columns are added
   * as expected columns. If you required further ones, please implement
   * this method and return further required columns. Else, return null.
   * @return Additional columns required in addition to gene identifier
   * and signals.
   */
  protected abstract List<ExpectedColumn> getAdditionalExpectedColumns();


  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#importWithGUI(java.awt.Component, java.lang.String, de.zbit.integrator.ReaderCache)
   */
  @Override
  public Collection<T> importWithGUI(Component parent, String file, ReaderCache cache) {

    // Create a new panel that allows selection of species
    JLabeledComponent spec = IntegratorUITools.getOrganismSelector();
    
    // Create and show the import dialog
    try {
      // Definitions of required and optional columns
      ExpectedColumn[] exCol = getExpectedColumns();
      CSVReader inputReader = loadConfigurationFromCache(cache, file, exCol, spec);
      
      // Show the CSV Import dialog
      final CSVImporterV2 c = new CSVImporterV2(inputReader, exCol);
      boolean dialogConfirmed = IntegratorUITools.showCSVImportDialog(parent, c, spec);
      
      // Process user input and read data
      if (dialogConfirmed) {
        // Store in cache
        this.species = (Species) spec.getSelectedItem();
        if (cache!=null) cache.add(ReaderCacheElement.createInstance(c, species));
        
        // Read all columns and types
        setNameAndIdentifierTypes(exCol[0]);   
        
        int offset = 1;
        List<ExpectedColumn> additional = getAdditionalExpectedColumns();
        if (additional!=null && additional.size()>0) {
          processAdditionalExpectedColumns(ArrayUtils.asList(exCol, offset, (offset+additional.size()) ));
          offset+=additional.size();
        }
        
        // Signal columns
        for (int i=offset; i<exCol.length; i++) {
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
   * Process user selected assignments for {@link #getAdditionalExpectedColumns()}.
   * List should be the same as given by {@link #getAdditionalExpectedColumns()} (signal
   * and id cols have been removed).
   * @param additional
   */
  protected abstract void processAdditionalExpectedColumns(List<ExpectedColumn> additional);


  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#getSpecies()
   */
  @Override
  public Species getSpecies() {
    return species;
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
          idCol.getAssignedColumns().get(i), type, IntegratorUITools.getPriority(type)); 
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
  public AbstractGeneBasedNSreader() {
    super(-1);
  }
  

  public AbstractGeneBasedNSreader(int identifierCol, IdentifierType idType, Species species) {
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
  public Collection<T> read(CSVReader inputCSV) throws IOException, Exception {
    // Init Mapper (primary for idType)
    if (!idType.equals(IdentifierType.NCBI_GeneID)) {
      mapper = MappingUtils.initialize2GeneIDMapper(idType, getSecondaryProgressBar(), species);
    } else if (secondID!=null) {
      // Only if primary identifier does not require a mapper,
      // init one for the secondary identifier
      mapper = MappingUtils.initialize2GeneIDMapper(secondID.getB(), getSecondaryProgressBar(), species);
    }
    if (mapper!=null) mapper.readMappingData();
    
    // Read file
    Collection<T> ret =  super.read(inputCSV);
    
    // Free resources
    mapper = null;
    return ret;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#read(java.lang.String[])
   */
  @Override
  public Collection<T> read(String[] identifiers) throws IOException, Exception {
    // Init Mapper (primary for idType)
    if (!idType.equals(IdentifierType.NCBI_GeneID)) {
      mapper = MappingUtils.initialize2GeneIDMapper(idType, getSecondaryProgressBar(), species);
    }
    if (mapper!=null) mapper.readMappingData();
    
    // Read file
    Collection<T> ret =  super.read(identifiers);
    
    // Free resources
    mapper = null;
    return ret;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#createObject(java.lang.String, java.lang.String[])
   */
  @Override
  protected T createObject(String name, String[] line) throws Exception {
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
    T m = createObject(name, geneID, line);
    
    // SecondID is normally the name. If not, still keep this
    // information as additional information.
    if (addSecondIDasAdditionalInfo) {
      m.addData(secondID.getB().toString(), line[secondID.getA()]);
    }
    
    return m;
  }
  
  /**
   * Create your instance of T from the CSV file.
   * The name and geneID is already parsed and
   * also the signals is being taken care of.
   * Additional values from the CSV file can be parsed here.
   * Else, the return is simply
   * <pre>
   * return new MyClass(name, geneID);
   * </pre> 
   * @param name most human readable gene name
   * @param geneID <b>might be <code>NULL</code></b>, if unknown!
   * @param line current line of CSV file
   * @return instance of T
   */
  protected abstract T createObject(String name, Integer geneID, String[] line);
  
}

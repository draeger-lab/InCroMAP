/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.io;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.gui.CSVImporterV2.CSVImporterV2;
import de.zbit.gui.CSVImporterV2.ExpectedColumn;
import de.zbit.util.AbstractProgressBar;
import de.zbit.util.ValuePair;
import de.zbit.util.ValueTriplet;

/**
 * A generic class to read data, consisting of name and signals.
 * E.g., mRNA or miRNA data.
 * <p>Note:<br/>Your extending class should have an empty
 * constructor, such that an instance of the reader can be created
 * and {@link #importWithGUI(String)} can be called.
 * @author Clemens Wrzodek
 */
public abstract class NameAndSignalReader<T extends NameAndSignals> {
  public static final transient Logger log = Logger.getLogger(NameAndSignalReader.class.getName());
  
  /**
   * Required: Column that contains the name
   */
  protected int nameCol;
  
  /**
   * Required: List of different signal columns
   * Integer=ColumnNumber, SignalType={@link SignalType}, String=ExperimentName/ObservationName
   */
  private Collection<ValueTriplet<Integer, SignalType, String>> signalColumns;
  
  
  /**
   * This reads any column, identifies by the integer and stores it with
   * a key, identified by the String.
   */
  private Collection<ValuePair<Integer, String>> additionalDataToRead=null;
  
  /**
   * Import a file with a GUI. There are many helper methods that allow to quickly
   * implement this method:<ol><li>Create {@link ExpectedColumn}s by using
   * {@link #getExpectedSignalColumns(int)} and adding you own columns (especially
   * for {@link #nameCol}!
   * <li>Use {@link CSVImporterV2} to get the {@link CSVReader} and columns assignments
   * and build the reader. 
   * <li>Afterwards, return {@link #read(CSVReader)} using the
   * {@link CSVImporterV2#getApprovedCSVReader()}.
   * </ol>
   * @param parent the parent {@link Frame} or {@link Dialog}
   * @param file
   * @return
   */
  public abstract Collection<T> importWithGUI(Component parent, String file);
  
  /**
   * This can also be used by extending classes.
   */
  protected AbstractProgressBar progress;
  
  public NameAndSignalReader(int nameCol) {
    super();
    signalColumns=null;
    this.nameCol = nameCol;
  }
  
  public NameAndSignalReader(int miRNAnameCol, Collection<ValueTriplet<Integer, SignalType, String>> signalColumns) {
    this(miRNAnameCol);
    this.signalColumns=signalColumns;
  }
  
  
  /*public void addSignalColumn(int col, String experimentName) {
    addSignalColumn(col, SignalType.Unknown, experimentName);
  }*/
  
  public void addSignalColumn(int col, SignalType type, String experimentName) {
    if (signalColumns==null) initializeSignalColumns();
    signalColumns.add(new ValueTriplet<Integer, SignalType, String>(col, type, experimentName));
  }
  
  private void initializeSignalColumns() {
    signalColumns = new ArrayList<ValueTriplet<Integer, SignalType, String>>();
  }
  
  public void addAdditionalData(int col, String key) {
    if (additionalDataToRead==null) additionalDataToRead = new ArrayList<ValuePair<Integer, String>>();
    additionalDataToRead.add(new ValuePair<Integer, String>(col,key));
  }
  
  /**
   * @see #read(String)
   * @param file
   * @return
   * @throws Exception 
   * @throws IOException 
   */
  public Collection<T> read(File file) throws IOException, Exception {
    return read(file.getPath());
  }

  /**
   * @see #read(CSVReader)
   * @param inputCSV
   * @return
   * @throws IOException
   * @throws Exception
   */
  public Collection<T> read(String inputCSV) throws IOException, Exception {
    CSVReader r = new CSVReader(inputCSV);
    return read(r);
  }
  
  /**
   * Read {@link NameAndSignals} data from given CSV file.
   * 
   * <p>Remark:<br/>
   * Your <T extends NameAndSignal> class should define its hashcode
   * and compareTo classes only by identifying the probe or "thing
   * represented by this object". It must *NOT* include the signal
   * list in hashcode calculations. This remark is only for hashCode
   * and compareTo. NOT for the equals method.</p>
   * @param inputCSV
   * @return
   * @throws IOException
   * @throws Exception - critical exception (not IO related) that make
   * reading the input data to the desired format impossible.
   */
  public Collection<T> read(CSVReader r) throws IOException, Exception {
    Map<T, T> ret = new HashMap<T, T>();
    
    String[] line;
    while ((line=r.getNextLine())!=null) {
      
      T m = createObject(line[nameCol], line);
      
      // Assign additional data
      if (additionalDataToRead!=null) {
        for (ValuePair<Integer, String> vp : additionalDataToRead) {
          m.addData(vp.getB(), line[vp.getA()]);
        }
      }
      
      // Get unique object to assign signals
      T mi = ret.get(m);
      if (mi==null) {
        mi = m;
        ret.put(mi, mi);
      }
      
      // Add signals
      if (signalColumns!=null) {
        for (ValueTriplet<Integer, SignalType, String> vp: signalColumns) {
          Float signal = Float.NaN;
          try {
            signal = Float.parseFloat(line[vp.getA()]);
          } catch (NumberFormatException e) {
            log.log(Level.WARNING, "Error while parsing signal number.", r);
          }
          mi.addSignal(signal, vp.getC(), vp.getB());
        }
      }
    }
    
    return ret.values();
  }
  
  /*
   *       String probeName = probeNameCol>=0?line[probeNameCol]:null;
      miRNA m = new miRNA(line[miRNAnameCol], probeName);
   */
  /**
   * Create your object from the CSV file, the name is already parsed and
   * also the signals is being taken care of. Additional values from the
   * CSV file can be parsed here. Else, the return is simply
   * <pre>
   * return new MyClass(name);
   * </pre> 
   * @throws Exception - only critical exceptions should be thrown that really
   * make reading the data impossible.
   */
  protected abstract T createObject(String name, String[] line) throws Exception;
  
  /**
   * Creates a collection of maxNumberOfObservations optional {@link ExpectedColumn}s
   * that accepts FoldChanges and pValues of renameable signals. This is optimal for 
   * importing {@link Signal} using the {@link CSVImporterV2}.
   * @param maxNumberOfObservations number of expected observation columns to create
   * @return
   */
  protected static Collection<ExpectedColumn> getExpectedSignalColumns(int maxNumberOfObservations) {
    // Application can (as of today) only process pValues and fold changes.
    SignalType[] types = new SignalType[]{SignalType.FoldChange, SignalType.pValue};
    
    Collection<ExpectedColumn> r = new ArrayList<ExpectedColumn>(maxNumberOfObservations); 
    for (int i=1; i<=maxNumberOfObservations; i++) {
      ExpectedColumn e = new ExpectedColumn("Observation " + i, types, false, true,true,true,null);
      r.add(e);
    }
    return r;
  }
  
  /**
   * Creates a collection of maxNumberOfColumns optional {@link ExpectedColumn}s
   * that accepts any additional data. This is optimal for 
   * importing {@link #addAdditionalData(int, String)} using the {@link CSVImporterV2}.
   * @param maxNumberOfColumns number of {@link ExpectedColumn}s to create
   * @return
   */
  protected static Collection<ExpectedColumn> getAdditionalDataColumns(int maxNumberOfColumns) {
    // Application can (as of today) only process pValues and fold changes.
    Collection<ExpectedColumn> r = new ArrayList<ExpectedColumn>(maxNumberOfColumns); 
    for (int i=1; i<=maxNumberOfColumns; i++) {
      ExpectedColumn e = new ExpectedColumn("Additional data " + i, null, false, false,false,true,null);
      r.add(e);
    }
    return r;
  }

  /**
   * @param progress
   */
  public void setProgressBar(AbstractProgressBar progress) {
    this.progress = progress;
  }
}

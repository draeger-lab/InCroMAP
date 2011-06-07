/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.SignalType;
import de.zbit.util.ValuePair;
import de.zbit.util.ValueTriplet;

/**
 * A generic class to read data, consisting of name and signals.
 * E.g., mRNA or miRNA data.
 * @author Clemens Wrzodek
 */
public abstract class NameAndSignalReader<T extends NameAndSignals> {
  public static final transient Logger log = Logger.getLogger(NameAndSignalReader.class.getName());
  
  /**
   * Required: Column that contains the name
   */
  private int nameCol;
  
  /**
   * Required: List of different signal columns
   * Integer=ColumnNamer, SignalType={@link SignalType}, String=ExperimentName/ObservationName
   */
  private Collection<ValueTriplet<Integer, SignalType, String>> signalColumns;
  
  
  /**
   * This reads any column, identifies by the integer and stores it with
   * a key, identified by the String.
   */
  private Collection<ValuePair<Integer, String>> additionalDataToRead=null;
  
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
   * Read miRNA data from given CSV file.
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
  public Collection<T> read(String inputCSV) throws IOException, Exception {
    CSVReader r = new CSVReader(inputCSV);
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
      for (ValueTriplet<Integer, SignalType, String> vp: signalColumns) {
        mi.addSignal(Float.parseFloat(line[vp.getA()]), vp.getC(), vp.getB());
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
  
}

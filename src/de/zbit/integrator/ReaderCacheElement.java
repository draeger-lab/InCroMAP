/**
 * @author Clemens Wrzodek
 */
package de.zbit.integrator;

import java.io.File;
import java.io.Reader;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;

import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.io.NameAndSignalReader;
import de.zbit.parser.Species;

/**
 * On element, describing all properties required by a file.
 * This is used in {@link Reader}.
 * 
 * @see ReaderCache
 * @author Clemens Wrzodek
 */
public class ReaderCacheElement implements Serializable, Comparable<ReaderCacheElement> {
  private static final long serialVersionUID = 8354927528072966373L;
  
  /**
   * Used to re-identify the file.
   */
  //String absoluteFileName;
  //int fileLength;
  //Files oldFile
  
  /**
   * Timestamp for this item
   */
  private long timestamp = System.currentTimeMillis();
  
  private File describingFile; 
  
  /**
   * @return the describingFile
   */
  public File getDescribingFile() {
    return describingFile;
  }

  /**
   * @param describingFile the describingFile to set
   */
  protected void setDescribingFile(File describingFile) {
    this.describingFile = describingFile;
  }

  private Class<? extends NameAndSignalReader> usedReader;
  
  private Species organism;
  
  
  private Collection<? extends ExpectedColumn> expectedColumns = null;
    
  /*
   * TODO:
   * - CSV Reader options
   * Implement a storeSettings() and loadSettings() method in CSVReaderOptionPanel
   */

  
  /**
   * @return a comparator that compares the elemts by their {@link #timestamp}
   */
  public static Comparator<ReaderCacheElement> getAgeComparator() {
    return new Comparator<ReaderCacheElement>() {
      @Override
      public int compare(ReaderCacheElement o1, ReaderCacheElement o2) {
        return (int) (o1.timestamp-o2.timestamp);
      }
    };
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(ReaderCacheElement o) {
    // TODO Auto-generated method stub
    return 0;
  }
  
  /**
   * Resets the timestamp of this element.
   */
  public void resetTime() {
    timestamp = System.currentTimeMillis();
  }
  
}

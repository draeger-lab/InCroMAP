package de.zbit.data.miRNA;

import java.io.Serializable;

import de.zbit.data.TableResult;
import de.zbit.exception.CorruptInputStreamException;
import de.zbit.io.CSVwriteable;


/**
 * Holder class to hold targets for an miRNA.
 * @author Clemens Wrzodek
 */
public class miRNAtarget implements Comparable<miRNAtarget>, Serializable, CSVwriteable, TableResult {
  private static final long serialVersionUID = -9197560384266247185L;

  /**
   * Target gene id
   */
  private int target;
  
  /**
   * Experimental verified or predicted target
   */
  private boolean experimental;
  
  /**
   * Source of evidence (e.g. PubMed id for experimental
   * or prediction algorithm for predictions).
   */
  private String source=null;
  
  /**
   * For predictions, a pValue or Score for this target.
   */
  private float pValue=Float.NaN;
  
  /**
   * Adds a new EXPERIMENTAL VERIFIED target.
   * @param target - GeneID of the target.
   */
  public miRNAtarget(int target) {
    super();
    this.target = target;
    experimental=true;
  }
  
  /**
   * Adds a new EXPERIMENTAL VERIFIED target.
   * @param target - GeneID of the target.
   * @param source - e.g. PubMed ID as evidence.
   */
  public miRNAtarget(int target, String source) {
    this(target);
    this.source=source;
  }
  
  /**
   * Adds a new target.
   * @param target - GeneID of the target.
   * @param experimental - false if it is a predicted target. Else: true.
   * @param source - e.g. PubMed ID or Name of prediction algorithm.
   * @param pValue - a pValue or score for this target. Please set to Float.NaN if not available.
   */
  public miRNAtarget(int target, boolean experimental, String source, float pValue) {
    this(target,source);
    this.experimental=experimental;
    this.pValue = pValue;
  }

  /**
   * @return the target
   * @see #target
   */
  public int getTarget() {
    return target;
  }

  /**
   * @return false if it is a predicted target. Else: true.
   * @see #experimental
   */
  public boolean isExperimental() {
    return experimental;
  }

  /**
   * @return the source
   * @see #source
   */
  public String getSource() {
    return source;
  }

  /**
   * @return the pValue
   * @see #pValue
   */
  public float getPValue() {
    return pValue;
  }

  /**
   * @param true if and only if this target is experimentally verified.
   */
  public void setExperimental(boolean experimental) {
    this.experimental = experimental;
  }

  /**
   * @param source prediction algorithm or experimental pmid
   */
  public void setSource(String source) {
    this.source = source;
  }

  /**
   * @param value the pValue to set
   */
  public void setPValue(float value) {
    pValue = value;
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(miRNAtarget o) {
    // Compare by target, isExperimental, Source and pValue.
    if (o instanceof miRNAtarget) {
      miRNAtarget t = (miRNAtarget)o;
      int r = target-t.getTarget();
      if (r==0) {
        if (isExperimental()&&!t.isExperimental()) return -1;
        if (!isExperimental()&&t.isExperimental()) return 1;
        
        r = source.compareTo(t.getSource());
        if (r==0 && !isExperimental()) {
          r = Float.compare(this.pValue, t.getPValue());
        }
      }
      
      return r;
    }
    return -1;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return (int) (isExperimental()?1:0+source.hashCode()+(pValue*100));
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof miRNAtarget) {
      return compareTo((miRNAtarget) obj)==0;
    } else return false;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "[miRNAtarget " + target + (experimental?" experimental":" predicted") + 
    (source!=null?" source:\"" + source+"\"":"") + (Float.isNaN(pValue)?"":" pValueOrScore:" + pValue) + "]";
  }

  /* (non-Javadoc)
   * @see de.zbit.io.CSVwriteable#fromCSV(java.lang.String[], int, int)
   */
  public void fromCSV(String[] elements, int elementNumber, int CSVversionNumber)
    throws CorruptInputStreamException {
    // For Version 0:
    target = Integer.parseInt(elements[0]);
    experimental = Boolean.parseBoolean(elements[1]);
    source = elements[2];
    pValue = Float.parseFloat(elements[3]);
  }

  /* (non-Javadoc)
   * @see de.zbit.io.CSVwriteable#getCSVOutputVersionNumber()
   */
  public int getCSVOutputVersionNumber() {
    return 0;
  }

  /* (non-Javadoc)
   * @see de.zbit.io.CSVwriteable#toCSV(int)
   */
  public String toCSV(int elementNumber) {
    return target+"\t"+experimental+"\t"+source.replace("\t", " ")+"\t"+pValue;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  public miRNAtarget clone() {
    miRNAtarget t = new miRNAtarget(target, experimental, new String(source), pValue);
    return t;
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return 4;
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getObjectAtColumn(int)
   */
  @Override
  public Object getObjectAtColumn(int colIndex) {
    if (colIndex==0) return target;
    else if (colIndex==1) return experimental;
    else if (colIndex==2) return source;
    else if (colIndex==3) return pValue;
    
    return null;
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getColumnName(int)
   */
  @Override
  public String getColumnName(int colIndex) {
    if (colIndex==0) return "Target GeneID";
    else if (colIndex==1) return "Experimentally validated";
    else if (colIndex==2) return "Source";
    else if (colIndex==3) return "Score or p-value";
    
    return null;
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getRowObject()
   */
  @Override
  public Object getRowObject() {
    return this;
  }
  
}

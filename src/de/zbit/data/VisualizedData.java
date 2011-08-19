/**
 * @author Clemens Wrzodek
 */
package de.zbit.data;

import java.io.Serializable;

import de.zbit.data.Signal.SignalType;
import de.zbit.util.ValueTriplet;

/**
 * A class to remember required information to re-identify Signals,
 * that have been visualized in a pathway.
 * <p>More generic, this class holds information to re-identify any
 * {@link NameAndSignals} list and one specific set out of this list.
 * 
 * @author Clemens Wrzodek
 */
public class VisualizedData implements Serializable, Comparable<VisualizedData>{
  private static final long serialVersionUID = 9208006835682485913L;

  /**
   * Usually a string with the name of the tab, where the
   * {@link NameAndSignals} are located, that have been
   * visualized.
   * But actually, may be any unique identifier.
   */
  Object tabName;
  
  /**
   * ExperimentName to re-identify the visualized {@link Signal}
   */
  String experimentName;

  /**
   * {@link SignalType} to re-identify the visualized {@link Signal}
   */
  SignalType sigType;
  
  /**
   * Actual class of any visualized {@link NameAndSignals} instance.
   * E.g., mRNA, miRNA or ProteinModificationExpression.
   */
  Class<?> nsType;

  
  /**
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName to describe the signal
   * @param sigType to describe the signal
   * @param nsType actual class of visualized {@link NameAndSignals} instance.
   * E.g., mRNA, miRNA or ProteinModificationExpression. This is just for
   * information. It is not used to re-identify signals, or in {@link #equals(Object)},
   * {@link #hashCode()} or {@link #compareTo(VisualizedData)} methods!
   */
  public VisualizedData(Object tabName, String experimentName,
    SignalType sigType, Class<?> nsType) {
    super();
    this.tabName = tabName;
    this.experimentName = experimentName;
    this.sigType = sigType;
    this.nsType = nsType;
  }

  /**
   * @param tabName any unique identifier for the input dataset. E.g., 
   * the tab or the filename or anything else.
   * @param experimentName to describe the signal
   * @param type to describe the signal
   */
  public VisualizedData(String tabName, String experimentName, SignalType type) {
    this(tabName, experimentName, type, null);
  }

  /**
   * @param key
   * @see #VisualizedData(String, String, SignalType)
   */
  public VisualizedData(ValueTriplet<String, String, SignalType> key) {
    this(key.getA(), key.getB(), key.getC());
  }

  /**
   * @return the tabName
   */
  public Object getTabName() {
    return tabName;
  }

  /**
   * @param tabName the tabName to set
   */
  public void setTabName(Object tabName) {
    this.tabName = tabName;
  }

  /**
   * @return the experimentName
   */
  public String getExperimentName() {
    return experimentName;
  }

  /**
   * @param experimentName the experimentName to set
   */
  public void setExperimentName(String experimentName) {
    this.experimentName = experimentName;
  }

  /**
   * @return the sigType
   */
  public SignalType getSigType() {
    return sigType;
  }

  /**
   * @param sigType the sigType to set
   */
  public void setSigType(SignalType sigType) {
    this.sigType = sigType;
  }

  /**
   * @return the nsType
   */
  public Class<?> getNsType() {
    return nsType;
  }

  /**
   * @param nsType the nsType to set
   */
  public void setNsType(Class<?> nsType) {
    this.nsType = nsType;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((experimentName == null) ? 0 : experimentName.hashCode());
    //result = prime * result + ((nsType == null) ? 0 : nsType.hashCode());
    result = prime * result + ((sigType == null) ? 0 : sigType.hashCode());
    result = prime * result + ((tabName == null) ? 0 : tabName.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    VisualizedData other = (VisualizedData) obj;
    if (experimentName == null) {
      if (other.experimentName != null) return false;
    } else if (!experimentName.equals(other.experimentName)) return false;
//    if (nsType == null) {
//      if (other.nsType != null) return false;
//    } else if (!nsType.equals(other.nsType)) return false;
    if (sigType != other.sigType) return false;
    if (tabName == null) {
      if (other.tabName != null) return false;
    } else if (!tabName.equals(other.tabName)) return false;
    return true;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "VisualizedData [tabName=" + tabName + ", experimentName="
        + experimentName + ", sigType=" + sigType + ", nsType=" + nsType + "]";
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(VisualizedData o) {
    if (this == o) return 0;
    if (o == null) return -1;
    VisualizedData other = (VisualizedData) o;
    int ret = 0;
    
    if (experimentName!=null && other.experimentName!=null) {
      ret+=experimentName.compareTo(other.experimentName);
    } else if ((experimentName==null) != (other.experimentName==null)) {
      return -1;
    }
    
//    if (nsType!=null && other.nsType!=null) {
//      ret+=nsType.getName().compareTo(other.nsType.getName());
//    } else if ((nsType==null) != (other.nsType==null)) {
//      return -2;
//    }
    
    if (sigType!=null && other.sigType!=null) {
      ret+=sigType.compareTo(other.sigType);
    } else if ((sigType==null) != (other.sigType==null)) {
      return -3;
    }
    
    if (tabName!=null && other.tabName!=null) {
      ret+=tabName.toString().compareTo(other.tabName.toString());
    } else if ((tabName==null) != (other.tabName==null)) {
      return -4;
    }
    
    return ret;
  }
  
  
}

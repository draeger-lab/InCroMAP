/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import de.zbit.data.TableResult;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.data.miRNA.miRNAtargets;

/**
 * A {@link TableModel} implementing the {@link TableResult} interface
 * to create tables for {@link miRNAtargets}.
 * <p>One instance is required for every miRNA, miRNAtarget pair.
 * @author Clemens Wrzodek
 */
public class miRNAandTarget implements TableResult, Serializable {
  private static final long serialVersionUID = 2025556134114661117L;

  /**
   * miRNA identifier.
   */
  String mir;
  
  /**
   * One miRNA target.
   */
  private miRNAtarget target;
  
  /**
   * @param miRNAsystematicName
   * @param target
   */
  public miRNAandTarget(String miRNAsystematicName, miRNAtarget target) {
    super();
    this.mir = miRNAsystematicName;
    this.target = target;
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return target.getColumnCount()+1;
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getObjectAtColumn(int)
   */
  @Override
  public Object getObjectAtColumn(int colIndex) {
    if (colIndex==0) return mir;
    else return target.getObjectAtColumn(colIndex-1);
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getColumnName(int)
   */
  @Override
  public String getColumnName(int colIndex) {
    if (colIndex==0) return "microRNA";
    else return target.getColumnName(colIndex-1);
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getRowObject()
   */
  @Override
  public Object getRowObject() {
    return this;
  }
  
  /**
   * Converts the given list of targets to a list of {@link miRNAandTarget}s
   * that can be used in a {@link JTable}.
   * @param targets
   * @return {@link TableResult} compatible {@link List} of {@link miRNAandTarget}s.
   */
  public static List<miRNAandTarget> getList(miRNAtargets targets) {
    List<miRNAandTarget> ret = new ArrayList<miRNAandTarget>(100);
    
    Map<String, Collection<miRNAtarget>> rel = targets.getTargetList();
    for (String miRNA: rel.keySet()) {
      for (miRNAtarget target: rel.get(miRNA)) {
        ret.add(new miRNAandTarget(miRNA, target));
      }
    }
    
    return ret;
  }

  /**
   * @return associated {@link miRNAtarget}
   */
  public miRNAtarget getTarget() {
    return target;
  }
  
  /**
   * @return this microRNA systematic name.
   */
  public String getMicroRNA() {
    return mir;
  }
  
}

/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.TableResult;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.data.miRNA.miRNAtargets;

/**
 * A {@link TableModel} implementing the {@link TableResult} interface
 * to create tables for {@link miRNAtargets}.
 * 
 * <p>This is a special version of {@link miRNA} with exactly one {@link miRNAtarget}
 * for each {@link miRNA}. It is required to build {@link JTable}s.
 * <p>One instance is required for every miRNA, miRNAtarget pair.
 * 
 * @author Clemens Wrzodek
 */
public class miRNAandTarget extends miRNA implements TableResult, Serializable {
  private static final long serialVersionUID = 2025556134114661117L;
  
  /**
   * @param miRNAsystematicName
   * @param target
   */
  public miRNAandTarget(String miRNAsystematicName, miRNAtarget target) {
    super(miRNAsystematicName);
    setTarget(target);
  }
  public miRNAandTarget(String miRNAsystematicName, String probeName, miRNAtarget target) {
    super(miRNAsystematicName, probeName);
    setTarget(target);
  }
  
  private void setTarget (miRNAtarget target) {
    Collection<miRNAtarget> targets = new LinkedList<miRNAtarget>();
    targets.add(target);
    setTargets(targets);
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return getTarget().getColumnCount()+1; // +1 for miRNA name
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getObjectAtColumn(int)
   */
  @Override
  public Object getObjectAtColumn(int colIndex) {
    if (colIndex==0) return getName();
    else return getTarget().getObjectAtColumn(colIndex-1);
  }

  /* (non-Javadoc)
   * @see de.zbit.data.TableResult#getColumnName(int)
   */
  @Override
  public String getColumnName(int colIndex) {
    if (colIndex==0) return "microRNA";
    else return getTarget().getColumnName(colIndex-1);
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
    return getTargets().iterator().next();
  }
  
  /**
   * @return this microRNA systematic name.
   */
  public String getMicroRNA() {
    return getName();
  }

  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignals#merge(java.util.Collection, de.zbit.data.NameAndSignals, de.zbit.data.Signal.MergeType)
   */
  @Override
  protected <T extends NameAndSignals> void merge(Collection<T> source,
    T target, MergeType m) {
    super.merge(source, target, m);
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return super.toString("target:"+getTarget().toString()+" ");
  }
  
}

/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.tree.TreeNode;
import javax.swing.treetable.AbstractTreeTableModel;
import javax.swing.treetable.TreeTableModel;

import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.id.GeneID;
import de.zbit.data.miRNA.miRNA;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.customcomponents.TableResultTableModel;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.util.Species;
import de.zbit.util.objectwrapper.ValuePair;

/**
 * Builds a tree of various, Gene-Based {@link NameAndSignals}.
 * The tree has one root (the genome), which has one child for each gene.
 * Each gene has one child for each data type (e.g., mRNA, miRNA, etc.)
 * and each data type has all of its {@link NameAndSignals} as
 * child (possibly probes).
 * <p>Every {@link NameAndSignals} is re-created as {@link HeterogeneousNS},
 * containing only the given {@link Signal}(s), the name and childs.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class HeterogeneousData extends AbstractTreeTableModel<HeterogeneousNS> implements TreeTableModel {
  public static final transient Logger log = Logger.getLogger(HeterogeneousData.class.getName());
  
  /**
   * GeneID for nodes, that represent a specific data type
   * (e.g., mRNA node, etc.)
   */
  private final static int geneIDofTypeNode=-2;
  
  /**
   * Data to build a tree of.
   * (List of data types(mRNA), that contain all {@link NameAndSignals} of this type).
   * <p>Will be erased and replaced by {@link #maps} on runtime.
   */
  List<List<? extends NameAndSignals>> data;
  
  /**
   * Signals to take for each data type
   * (Matching indices with {@link #data}).
   */
  List<ValuePair<String, SignalType>> signalFromData;
  
  /**
   * {@link MergeType} to specify how to merge each data type individually.
   * (Matching indices with {@link #data}).
   */
  List<MergeType> mergeTypeForData;
  
  /**
   * Total number of nodes.
   */
  int size=0;

  /**
   * Maps from GeneID (as String) to a Collection of all probes,
   * matching to this gene ID.
   * For all lists in {@link #data}
   */
  private List<Map<String, ?>> maps;

  /**
   * Human readable data type names for all {@link #data}
   */
  private List<String> dataTypeName;

  /**
   * Extension that allows to hide some rows. Caution, this list
   * contains all VISIBLE (!=hidden) rows.
   * This holds indices of genes, which are childs of the root only!
   * E.g., 0 is the first child (and first gene) below the root.
   */
  List<Integer> visibleRows = null;
  
  /**
   */
  public HeterogeneousData() {
    super(null);
    data = new ArrayList<List<? extends NameAndSignals>>();
    signalFromData = new ArrayList<ValuePair<String, SignalType>>();
    mergeTypeForData = new ArrayList<Signal.MergeType>();
  }
  
  /**
   * Add a data type that should be considered when creating the tree with {@link #initTree(Species)}.
   * @param nsList
   * @param signal
   * @param mergeType
   */
  public void addDataType(List<? extends NameAndSignals> nsList, ValuePair<String, SignalType> signal, MergeType mergeType) {
    data.add(nsList);
    signalFromData.add(signal);
    mergeTypeForData.add(mergeType);
  }
  
  /**
   * Initializes the tree structure by building nodes for the root
   * and all genes.
   * @param species required for the GeneSymbol mapping
   * @return TreeModel
   */
  @SuppressWarnings("unchecked")
  public TreeTableModel initTree(Species species) {
    GeneID2GeneSymbolMapper gsMap = IntegratorUITools.get2GeneSymbolMapping(species);
    
    // Every tree needs exactly one root. This will not be visible later on.
    HeterogeneousNS root = new HeterogeneousNS("Genome", HeterogeneousNS.geneIDofRootNode);
    size=1;
    
    // Group all lists by geneID
    maps = new ArrayList<Map<String,?>>(data.size());
    dataTypeName = new ArrayList<String>(data.size());
    for (List<? extends NameAndSignals> d: data) {
      Class<? extends NameAndSignals> type = NameAndSignals.getType(d);
      if (miRNA.class.isAssignableFrom(type)) {
        maps.add((Map<String,?>)miRNA.groupByTargetAndReturnKeysAsString((Iterable<? extends miRNA>) d));
        dataTypeName.add(PairedNS.getTypeNameFull(type));
      } else if (GeneID.class.isAssignableFrom(type)) {
        maps.add(NameAndSignals.group_by_name(d, true, false));
        dataTypeName.add(PairedNS.getTypeNameFull(type));
      } else {
        log.warning("Can not group by gene_id: " + type.getSimpleName());
        // we need to have matching indices => add dummy map
        maps.add(new HashMap<String, NameAndSignals>());
      }
    }
    data=null; // Free unused memory... All we need is in maps!
    
    // Add all genes to the root node
    String defaultGeneID = Integer.toString(GeneID.default_geneID);
    Set<Integer> createdGeneIds = new HashSet<Integer>(maps.iterator().next().size());
    for (int i=0; i<maps.size(); i++) {
      Iterator<String> mapIgeneIDs = maps.get(i).keySet().iterator();
      while (mapIgeneIDs.hasNext()) {
        // Get geneID and symbol
        String geneID = mapIgeneIDs.next();
        Integer geneIDint = Integer.parseInt(geneID);
        if (!createdGeneIds.add(geneIDint)) continue; // One row per gene
        String geneName=null;
        try {
          geneName = gsMap.map(geneIDint);
        } catch (Exception e) {}
        if (geneName==null) geneName = geneID;
        if (geneName.equals(defaultGeneID)) geneName="Unknown";
          
        // Create gene as child of root
        HeterogeneousNS gene = new HeterogeneousNS(geneName, geneIDint);
        root.addChild(gene);
        size++;
        
        // Theoretically one node per data type (mRNA, miRNA,...) and then one per probe
        // will be created on-the-fly, but the signal still needs to be calculated.
        for (int l=0; l<maps.size(); l++) {
          addSignal(geneID, gene, l);
        }
        size+=maps.size(); // TYPE-nodes
        
      }
    }
    
    // Initial alphabetical sorting.
    root.sortChilds();
    
    setRoot(root);
    return this;
  }


  /**
   * Add a signal to a node
   * @param geneID geneID as integer
   * @param gene NS to add the signal to
   * @param l current index of {@link #data} (and all other lists).
   */
  @SuppressWarnings("unchecked")
  private void addSignal(String geneID, HeterogeneousNS gene, int l) {
    // Add merged-type-Signal to gene
    // (We need to have a consistent number of Signals (add dummy items if i>0))
    List<NameAndSignals> list = (List<NameAndSignals>) maps.get(l).get(geneID);
    ValuePair<String, SignalType> signalToGeneCenter = signalFromData.get(l);
    Double signal = Double.NaN;
    if (list!=null && list.size()>0) {
      // Gene-center signal
      signal = Signal.merge(list, mergeTypeForData.get(l), signalToGeneCenter.getA(), signalToGeneCenter.getB()).getSignal().doubleValue();
      size+=list.size(); // PROBE-nodes
    }
    gene.addSignal(signal, dataTypeName.get(l), signalToGeneCenter.getB());
  }
  

  /* (non-Javadoc)
   * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
   */
  @SuppressWarnings("unchecked")
  @Override
  public Object getChild(Object parent, int index) {
    if (parent==root) {
      // Adjusts the row index with respect to {@link #visibleRows}.
      if (visibleRows!=null) {
        index = visibleRows.get(index);
      }
    }
    
    int geneId = ((GeneID)parent).getID();
    if (geneId==HeterogeneousNS.geneIDofRootNode) {
      // All gene-nodes are pre-created
      return ((TreeNode)parent).getChildAt(index);
    } else if (geneId==geneIDofTypeNode) {
      // Is a TYPE-Node => Return probe
      if (((HeterogeneousNS)parent).getChildCount()<=0) {
        geneId = ((GeneID) ((HeterogeneousNS)parent).getParent() ).getID();
        int l = (Integer) ((HeterogeneousNS)parent).getData("_PARENT_LIST_NUMBER");
        List<NameAndSignals> probes = (List<NameAndSignals>) maps.get(l).get(Integer.toString(geneId));
        if (probes==null || probes.size()<=0) return null;
        for (int i=0; i<probes.size(); i++) {
          NameAndSignals ns = probes.get(i);
          HeterogeneousNS probe = new HeterogeneousNS(String.format("%s (%s)", ns.getName(), ns.getUniqueLabel()), geneId);
          ((HeterogeneousNS)parent).addChild(probe);
          Signal original = ns.getSignal(signalFromData.get(l).getB(), signalFromData.get(l).getA());
          probe.addSignal(original.getSignal().doubleValue(), dataTypeName.get(l), original.getType());
        }
        ((HeterogeneousNS)parent).sortChilds();
      }
      return ((HeterogeneousNS)parent).getChildAt(index);
      
    } else if (((TreeNode)parent).getParent().equals(getRoot())) {
      // Is a GENE-node (2nd level) => Create a TYPE node
      if (((HeterogeneousNS)parent).getChildCount()<=0) {
        for (int l=0; l<maps.size(); l++) {
          List<NameAndSignals> probes = (List<NameAndSignals>) maps.get(l).get(Integer.toString(geneId));
          if (probes==null || probes.size()<=0) continue;
          HeterogeneousNS type = new HeterogeneousNS(dataTypeName.get(l), geneIDofTypeNode);
          ((HeterogeneousNS)parent).addChild(type);
          type.addData("_PARENT_LIST_NUMBER", l);
          addSignal(Integer.toString(geneId), type, l);
        }
      }
      return ((HeterogeneousNS)parent).getChildAt(index);

    } else {
      // Probe node (has no childs).
    }
    
    return null;
  }

  
  /**
   * Filter this TreeTable, such that only the given row indices are visible.
   * @param vr indices of visible childs below the root. See
   * {@link #visibleRows} for more explanation.
   */
  public void setVisibleRows(Collection<Integer> vr) {
    if (vr==null) {
      visibleRows = null;
    } else if (vr instanceof List) {
      visibleRows = (List<Integer>) vr;
    } else {
      visibleRows = new ArrayList<Integer>(vr);
    }
    
    if (visibleRows!=null) {
      Collections.sort(visibleRows);
    }
  }
  
  /**
   * 
   * @return True if {@link #visibleRows} is set and thus, the content
   * is currently fileterd. False, if the content is currently unfiltered.
   */
  public boolean isSetVisibleRows() {
    return visibleRows!=null;
  }
  
  /* (non-Javadoc)
   * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  @Override
  public int getChildCount(Object parent) {
    if (parent.equals(root)) {
      if (visibleRows!=null) {
        return visibleRows.size();
      }
    }
    
    int geneId = ((GeneID)parent).getID();
    if (geneId==HeterogeneousNS.geneIDofRootNode) {
      // All gene-nodes are pre-created
      return ((TreeNode)parent).getChildCount();
    } else if (geneId==geneIDofTypeNode) {
      // Is a TYPE-Node => Return probe
      geneId = ((GeneID) ((HeterogeneousNS)parent).getParent() ).getID();
      int l = (Integer) ((HeterogeneousNS)parent).getData("_PARENT_LIST_NUMBER");
      List<NameAndSignals> probes = (List<NameAndSignals>) maps.get(l).get(Integer.toString(geneId));
      return probes==null?0:probes.size();

    } else if (((TreeNode)parent).getParent().equals(getRoot())) {
      // Is a GENE-node (2nd level) => Create a TYPE node
      int nonNullCounter=0;
      for (int l=0; l<maps.size(); l++) {
        List<NameAndSignals> probes = (List<NameAndSignals>) maps.get(l).get(Integer.toString(geneId));
        if (probes!=null && probes.size()>0) {
          nonNullCounter++;
        }
      }
      return nonNullCounter;
      
    } else {
      // Probe node (has no childs).
    }
    return 0;
  }
  
  /* (non-Javadoc)
   * @see javax.swing.treetable.AbstractTreeTableModel#isCellEditable(java.lang.Object, int)
   */
  @Override
  public boolean isCellEditable(Object node, int column) {
    return false;
  }
  
  /* (non-Javadoc)
   * @see javax.swing.treetable.AbstractTreeTableModel#getColumnClass(int)
   */
  @Override
  public Class<?> getColumnClass(int column) {
    if (column==0) return TreeTableModel.class; // Required to display the tree renderer
    else return Double.class; // Signals
  }

  /* (non-Javadoc)
   * @see javax.swing.treetable.TreeTableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return 1+maps.size(); //+1 for GeneName
  }

  /* (non-Javadoc)
   * @see javax.swing.treetable.TreeTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    if (column==0) return "Gene";
    else if ((column-1)<dataTypeName.size()) {
      return dataTypeName.get(column-1);
    } else {
      return "";
    }
  }

  /* (non-Javadoc)
   * @see javax.swing.treetable.TreeTableModel#getValueAt(java.lang.Object, int)
   */
  @Override
  public Object getValueAt(Object node, int column) {
    //return TableResultTableModel.getValueAt((TableResult)node, column);
    if (column==0) return ((HeterogeneousNS)node); // toString() returns .getName()
    else {
      // Node type-dependent
//    int geneId = ((GeneID)node).getGeneID();
//    if (geneId==HeterogeneousNS.geneIDofRootNode) {
//      // Only name should show up on root
//      return "";
//    } else if (geneId==geneIDofTypeNode) {
//      // Is a TYPE-Node => Only own signal type should show up.
//      int l = (Integer) ((HeterogeneousNS)node).getData("_PARENT_LIST_NUMBER");
//      if (column-1!=l) return "";
//      else { // Return merged signal
//        ((HeterogeneousNS)node).getSignalValue(signalFromData.get(l).getB(), signalFromData.get(l).getA());
//      }
//      
//    } else if (((TreeNode)node).getParent().equals(root)) {
//      // Is a GENE-node (2nd level) => Create a TYPE node
//      ValuePair<String, SignalType> sig = signalFromData.get(column-1);
//      ((HeterogeneousNS)node).getSignalValue(sig.getB(), sig.getA());
//      
//    } else {
//      // Probe node (has no childs).
//    }
      if (column-1>=signalFromData.size()) return Double.NaN;
      ValuePair<String, SignalType> sig = signalFromData.get(column-1);
      Number n = ((HeterogeneousNS)node).getSignalValue(sig.getB(), dataTypeName.get(column-1));
      return TableResultTableModel.returnNumberOrNA(n);
    }
  }

  /**
   */
  public int getTotalSize() {
    return size;
  }

  public Object getFirstGeneNode() {
    if (root==null || getChildCount(root)<=0) return null;
    return ((TreeNode)root).getChildAt(0);
  }

  
  
}

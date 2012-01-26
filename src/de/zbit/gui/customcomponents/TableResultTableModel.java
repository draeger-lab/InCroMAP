/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/Integrator> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.gui.customcomponents;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.border.MatteBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.PairedNS;
import de.zbit.data.Signal;
import de.zbit.data.TableResult;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.actions.NameAndSignalTabActions;
import de.zbit.gui.actions.listeners.EnrichmentActionListener;
import de.zbit.gui.actions.listeners.KEGGPathwayActionListener;
import de.zbit.gui.table.DefaultTableCellTwoRowHeaderRenderer;
import de.zbit.gui.table.JTableTools;
import de.zbit.gui.table.TableRowSorterMixed;
import de.zbit.gui.tabs.IntegratorTab;
import de.zbit.gui.tabs.IntegratorTabWithTable;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.parser.Species;
import de.zbit.sequence.region.Region;
import de.zbit.util.BooleanRendererYesNo;
import de.zbit.util.ScientificNumberRenderer;
import de.zbit.util.SortedArrayList;

/**
 * A {@link TableModel} that can be used to visualize a {@link TableResult} class as {@link JTable}.
 * Especially useful for {@link NameAndSignals}
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TableResultTableModel<T extends TableResult> extends AbstractTableModel {
  private static final long serialVersionUID = -6542792109889449114L;
  
  public static final transient Logger log = Logger.getLogger(NameAndSignalTabActions.class.getName());
  
  /**
   * The {@link NameAndSignals} that should be represented by this {@link TableModel}.
   */
  private List<T> ns;
  
  /**
   * Should the first col be a row index?
   */
  private boolean includeRowIndex;
  
  public TableResultTableModel(List<T> ns) {
    this(ns, true);
  }
  
  public TableResultTableModel(List<T> ns, boolean includeRowIndex) {
    this.ns = ns;
    this.includeRowIndex = includeRowIndex;
  }
  
  /**
   * @return the underlying {@link NameAndSignals} {@link List}.
   */
  public List<T> getNameAndSignalsList() {
    return ns;
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  public int getColumnCount() {
    // 1 is the row index.
    int offset = includeRowIndex?1:0;
    if (ns.size()<1) return offset;
    return ns.get(0).getColumnCount()+offset;
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getRowCount()
   */
  public int getRowCount() {
    return ns.size();
  }

  /**
   * @return
   */
  public boolean isRowIndexIncluded() {
    return includeRowIndex;
  }
  
  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (includeRowIndex) {
      if (columnIndex==0) return (rowIndex+1);
      columnIndex = columnIndex-1;
    }
    
    return getValueAt(ns.get(rowIndex), columnIndex);
  }

  /**
   * 
   * @param <A>
   * @param ns
   * @param columnIndex
   * @return
   */
  public static<A extends TableResult> Object getValueAt(A ns, int columnIndex) {
    Object o = ns.getObjectAtColumn(columnIndex);
    if (o instanceof Signal) {
      // Experiment name and signal type is already in header!
      Number n = ((Signal)o).getSignal();
      return returnNumberOrNA(n);
    } else {
      return returnNumberOrNA(o);
    }
  }
  
  /**
   * Returns "N/A" for {@link Double#NaN} numbers.
   * @param n
   * @return the number or "N/A".
   */
  public static Object returnNumberOrNA(Object n) {
    if (n==null) return n;
    if (n.equals(Double.NaN)) {
      return "N/A";
    } else {
      return n;
    }
  }
  
  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
   */
  @Override
  public Class<?> getColumnClass(int columnIndex) {
    if (includeRowIndex) {
      if (columnIndex==0) return Integer.class;
      columnIndex = columnIndex-1;
    }
    if (ns.size()<1) return super.getColumnClass(columnIndex);
    
    // Get first non-null object in column
    Object o = null; int i=0;
    while (o==null && i<ns.size()) {
      o = ns.get(i++).getObjectAtColumn(columnIndex);
    }
    
    Class<?> c = getColumnClass(o);
    if (c==null) return super.getColumnClass(columnIndex);
    return c;
  }
  
  /**
   * @param o ns.getObjectAtColumn(columnIndex);
   * @return Column Class OR NULL.
   */
  public static Class<?> getColumnClass(Object o) {
    Class<?> c = o!=null?o.getClass():null;
    return c;
  }
  
  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    if (includeRowIndex) {
      if (column==0) return "#";
      column = column-1;
    }
    if (ns.size()<1) return super.getColumnName(column);
    String s = ns.get(0).getColumnName(column);
    if (s==null || s.length()<1) {
      return super.getColumnName(column);
    }
    return s;
  }
  
  /**
   * Builds a nice, search- and sortable table from any integrator tab with
   * a list of {@link TableResult}
   * @param tab should actually contain a {@link List}, else, the Collection
   * is converted to a list.
   * @return
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static JTable buildJTable(IntegratorTab<Collection<? extends TableResult>> tab) {
    
    // Build table on scroll pane
    JTable jc;
    List<? extends TableResult> list;
    if (tab.getData() instanceof List) {
      list = (List<? extends TableResult>) tab.getData();
    } else {
      list = new ArrayList<TableResult>(tab.getData());
    }
    
    if (tab.getDataContentType().equals(PairedNS.class)) {
      jc = buildJTableWithBoldBorders(new TableResultTableModel(list), tab.getSpecies(), ((PairedNS)tab.getExampleData()).getBoldBorders() );
    } else {
      jc = buildJTable(new TableResultTableModel(list), tab.getSpecies());
    }
    
    
    // Add enrichment capabilities
    if (tab instanceof IntegratorTabWithTable) {
      EnrichmentActionListener al = new EnrichmentActionListener((IntegratorTabWithTable)tab);
      JPopupMenu popUp = IntegratorUITools.createEnrichmentPopup(al);
      IntegratorUITools.addRightMousePopup(jc, popUp);


      // Other data type dependent capabilities
      if (tab.getDataContentType().equals(EnrichmentObject.class)) {
        if (((EnrichmentObject)tab.getExampleData()).isKEGGenrichment()) {
          // It's a KEGG Pathway enrichment.
          popUp.addSeparator();
          KEGGPathwayActionListener al2 = new KEGGPathwayActionListener(tab);
          IntegratorUITools.addRightMousePopup(jc, IntegratorUITools.createKeggPathwayPopup(al2, popUp));
        }
      } else {
        if (Region.class.isAssignableFrom(tab.getDataContentType())) {
          NameAndSignalTabActions alNS;
          if (tab instanceof NameAndSignalsTab) {
            if (tab.getExampleData()!=null && ((NameAndSignals)tab.getExampleData()).hasSignals()) {
              alNS = ((NameAndSignalsTab) tab).getActions();
              IntegratorUITools.addRightMousePopup(jc, IntegratorUITools.createVisualizeGenomicRegionPopup(alNS, popUp));
            }
          } else {
            log.warning(String.format("Can not create 'Plot genome region' popup on %s.", tab.getClass().getSimpleName()));
          }
        }
        // Add "Visualize (only) selected data in pathway"
        // DISABLED, because would be much work:
        /* Limit vis. on selected data must be implemented in dialogs and
         * to really make this usefull, one must actually be able to also
         * select an existing pathway tab.*/
        // KEGGPathwayActionListener al2 = new KEGGPathwayActionListener(tab);
        // popUp.add(GUITools.createJMenuItem(al2,
        //   NSAction.VISUALIZE_SELETED_DATA_IN_PATHWAY, UIManager.getIcon("ICON_PATHWAY_16")));
      }
      
    }
    
    return jc;
  }
  
  public static <T extends TableResult> JTable buildJTable(TableResultTableModel<T> model) {
    return buildJTable(model, null);
  }
  
  public static <T extends TableResult> JTable buildJTable(TableResultTableModel<T> model, Species spec) {
    final JTable table = new JTable(model); // new JComponentTableModel()
    
    buildJTable(model, spec, table);
    
    return table;
  }

  /**
   * Pretty much the same as {@link #buildJTable(TableResultTableModel, Species)}, but
   * has a special treatment to consider
   * some bold borders within paired data (see, e.g., {@link PairedNS#getBoldBorders()}).
   * @param <T>
   * @param model
   * @param spec
   * @param boldBorders Column indices, whose left border should be painted bold.
   * @return
   */
  public static <T extends TableResult> JTable buildJTableWithBoldBorders(final TableResultTableModel<T> model, Species spec, final Set<Integer> boldBorders) {
    
    final JTable table = new JTable(model) {
      private static final long serialVersionUID = -868488272311106520L;

      /* (non-Javadoc)
       * @see javax.swing.JTable#prepareRenderer(javax.swing.table.TableCellRenderer, int, int)
       */
      @Override
      public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        
        // Make border bold, if boldBorders contains the index.
        short left=0, right=0;
        if (boldBorders.contains(column-(model.isRowIndexIncluded()?1:0))) { // 1 is offset for "#" column
          left=1;
        }
        if (boldBorders.contains((column+1)-(model.isRowIndexIncluded()?1:0))) {
          right=1;
        }
        if (left>0 || right>0) {
          ((JComponent)c).setBorder(new MatteBorder(0, left, 0, right, gridColor));
        }
        
        return c;
      }
    };
    
    // Let the header have two rows
    table.getTableHeader().setDefaultRenderer(new DefaultTableCellTwoRowHeaderRenderer(boldBorders));
    
    // Set/ add some parameters to the table
    buildJTable(model, spec, table);
    
    // Disallow reordering with paired data
    table.getTableHeader().setReorderingAllowed(false);
    table.getTableHeader().setPreferredSize(new Dimension(table.getTableHeader().getPreferredSize().width,  (int)(table.getRowHeight(0)*2.3)));
    
    
    return table;
  }
  
  
  /**
   * Set some nice attributes on created table.
   * @param <T>
   * @param model
   * @param spec
   * @param table
   */
  public static void buildJTable(TableModel model, Species spec, final JTable table) {
    // Set additional attributes
    table.setPreferredScrollableViewportSize(new Dimension(500, 100));
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    
    // Enable dragging columns
    table.getTableHeader().setReorderingAllowed(true);
    
    // Resize columns to a reasonable, unique width
    if (table.getColumnModel().getColumnCount()>0)  {
      int width = table.getColumnModel().getColumn(0).getWidth();
      width = Math.max(width, 125);
      for (int i=0; i<table.getColumnModel().getColumnCount(); i++)
        table.getColumnModel().getColumn(i).setPreferredWidth(width);
      if ((model instanceof  TableResultTableModel<?>) &&  
          ((TableResultTableModel<?>)model).isRowIndexIncluded()) {
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
      }
    }
    
    // Add sorting capabilities
    TableRowSorter<TableModel> sorter = new TableRowSorterMixed<TableModel>(model);
    table.setRowSorter(sorter);
    
    // Make doubles scientific
    TableCellRenderer rend = new ScientificNumberRenderer(100);
    table.setDefaultRenderer(Signal.class, rend);
    table.setDefaultRenderer(Double.class, rend);
    table.setDefaultRenderer(Float.class, rend);
    table.setDefaultRenderer(Boolean.class, new BooleanRendererYesNo());
    
    table.setDefaultRenderer(HashSet.class, new IterableRenderer(spec));
    table.setDefaultRenderer(SortedArrayList.class, new IterableRenderer(spec));
    table.setDefaultRenderer(ArrayList.class, new IterableRenderer(spec));
    
    // Cannot refer directly to "java.util.HashMap$Values".
    HashMap<String,String> map = new HashMap<String,String>();
    map.put("dummy", "dummy");
    table.setDefaultRenderer(map.values().getClass(), new IterableRenderer(spec));
    //table.setDefaultRenderer(RowIndex.class, RowIndex.getRowHeaderRenderer(table));
    
    // Allow searching
    JTableTools.setQuickSearch(table);
  }

}

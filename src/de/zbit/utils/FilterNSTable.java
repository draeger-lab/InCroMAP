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
 * Copyright (C) 2011-2012 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import javax.swing.treetable.JTreeTable;

import de.zbit.data.HeterogeneousData;
import de.zbit.gui.actions.listeners.EnrichmentActionListener;
import de.zbit.gui.table.JTableFilter;
import de.zbit.gui.tabs.IntegratorTabWithTable;

/**
 * Proivides means to filter a table. Especially for {@link IntegratorTabWithTable}
 * tabs and more especially for {@link JTreeTable}s ;-)
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class FilterNSTable {
  public static final transient Logger log = Logger.getLogger(FilterNSTable.class.getName());
  
  /**
   * Toggles the filtering of a table. Either disables it or shows a dialog
   * and then performs the filtering.
   * @param parent {@link IntegratorTabWithTable} containing the table to be filtered.
   * @param filterToglleButton OPTIONAL button that reflects the currently filtered state. May be null!
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void filterToggleTable(IntegratorTabWithTable parent, AbstractButton filterToglleButton) {
    JTable tb = (JTable) parent.getVisualization();
    
    boolean isCurrentlyFiltered = isTableFiltered(tb);
    
    
    // If filtered then unfilter
    if (isCurrentlyFiltered) {
      if (tb instanceof JTreeTable) {
        if (((JTreeTable)tb).getTree().getModel() instanceof HeterogeneousData) {
          ((HeterogeneousData)((JTreeTable)tb).getTree().getModel()).setVisibleRows(null);
        } else {
          log.warning("Cannot unfilter table with model " + ((JTreeTable)tb).getTree().getModel().getClass());
        }
      } else {
        if ((tb.getRowSorter()!=null) && (tb.getRowSorter() instanceof TableRowSorter)) {
          ((TableRowSorter)tb.getRowSorter()).setRowFilter(null);
        } else {
          log.warning("Cannot unfilter JTable.");
        }
      }
      
      // Deselect button
      if (filterToglleButton!=null) {
        filterToglleButton.setSelected(false); // Table is currently NOT filtered
      }

      
    } else {
      
      // We need to collapse all nodes before filtering.
      if (tb instanceof JTreeTable) {
        ((JTreeTable) tb).collapseAll();
      }
      
      // Show and evalutes the filter dialog
      JTableFilter f = EnrichmentActionListener.showJTableFilter(parent);
      List<Integer> genesToShow = f==null? null: f.getSelectedRows();
      if (genesToShow==null) {
        if (filterToglleButton!=null) {
          filterToglleButton.setSelected(false); // Table is currently NOT filtered
        }
        return;
      }
      
      // The first row is always the root node, simply ignore it!
      // Remove 0 and all else -1;
      if (tb instanceof JTreeTable && ((JTreeTable)tb).getTree().isRootVisible() ) {
        if (genesToShow.size()>0 && genesToShow.get(0).equals(0))  {
          genesToShow.remove(0);
        }
        for (int i=0; i<genesToShow.size();i++) {
          genesToShow.set(i, genesToShow.get(i)-1);
        }
      }
      
      // Apply the filter
      if (tb instanceof JTreeTable) {
        if (((JTreeTable)tb).getTree().getModel() instanceof HeterogeneousData) {
          ((HeterogeneousData)((JTreeTable)tb).getTree().getModel()).setVisibleRows(genesToShow);
        } else {
          log.warning("Cannot test if table is filtered with model " + ((JTreeTable)tb).getTree().getModel().getClass());
        }
      } else {
        // The following is correct, but the table does not propagate to the
        // JTree in JTreeTable!
        final Set<Integer> rowsToKeep = new HashSet<Integer>(f.getSelectedRows());
        
        // Perform the filtering
        TableRowSorter sorter = new TableRowSorter(tb.getModel());
        for (int i=0; i<tb.getColumnCount(); i++) {
          sorter.setSortable(i, false);
        }
        tb.setRowSorter(sorter);
        sorter.setRowFilter(new RowFilter() {
          @Override
          public boolean include(Entry entry) {
            return (rowsToKeep.contains(entry.getIdentifier()));
          }
        });
      }
      
      // Select button
      if (filterToglleButton!=null) {
        filterToglleButton.setSelected(true); // Table is currently filtered
      }
    }
    
    parent.rebuildTable();
  }

  /**
   * @param tb
   * @return <code>TRUE</code> if the given table is currently filtered.
   * Only works with respect to the other methods in this class.
   */
  @SuppressWarnings("rawtypes")
  public static boolean isTableFiltered(JTable tb) {
    boolean isCurrentlyFiltered = false;
    if (tb instanceof JTreeTable) {
      if (((JTreeTable)tb).getTree().getModel() instanceof HeterogeneousData) {
        isCurrentlyFiltered = ((HeterogeneousData)((JTreeTable)tb).getTree().getModel()).isSetVisibleRows();
      } else {
        log.warning("Cannot test if table is filtered with model " + ((JTreeTable)tb).getTree().getModel().getClass());
      }
    } else {
      //log.warning("Cannot test if table is filtered with table " + tb.getClass());
      if ((tb.getRowSorter()!=null) && (tb.getRowSorter() instanceof TableRowSorter)) {
        if (((TableRowSorter)tb.getRowSorter()).getRowFilter()!=null) {
          return true;
        } else {
          log.warning("Cannot detect if JTable is filtered or not.");
        }
      }
      return false;
    }
    
    return isCurrentlyFiltered;
  }
}

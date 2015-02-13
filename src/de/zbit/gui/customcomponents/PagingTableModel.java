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
package de.zbit.gui.customcomponents;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

/**
 * Simulate a JTable with a million rows, but only MAX_PAGE_SIZE rows are paged
 * in at a time and it takes LATENCY_MILLIS to load them.
 * 
 * This simulation is pretty simple. It doesn't do common-sense things like
 * canceling scheduled loads when they aren't needed anymore.
 * 
 * <P>XXX:THIS CLASS IS CURRENTLY UNUSED!
 * 
 * @author Brian Cole
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class PagingTableModel extends AbstractTableModel {
  private static final long serialVersionUID = -448471257818926368L;
  
  private static final int MAX_PAGE_SIZE = 50;
  private static final int LATENCY_MILLIS = 1500;
  
  private int dataOffset = 0;
  private ArrayList<Integer> data = new ArrayList<Integer>();
  private SortedSet<Segment> pending = new TreeSet<Segment>();
  
  public int getColumnCount() {
    return 2;
  }
  
  public String getColumnName(int col) {
    if (col == 0) return "#";
    return "fetched data";
  }
  
  public int getRowCount() {
    return 1000000; // one million
  }
  
  public Object getValueAt(int row, int col) {
    if (col == 0) {
      // first column is just the row number
      return Integer.toString(row);
    }
    // check if row is in current page, schedule if not
    ArrayList<Integer> page = data;
    int pageIndex = row - dataOffset;
    if (pageIndex < 0 || pageIndex >= page.size()) {
      // not loaded
      System.out.println("object at " + row + " isn't loaded yet");
      schedule(row);
      return "..";
    }
    Object rowObject = page.get(pageIndex);
    // for this simulation just return the whole rowObject
    return rowObject;
  }
  
  private void schedule(int offset) {
    // schedule the loading of the neighborhood around offset (if not already scheduled)
    if (isPending(offset)) {
      // already scheduled -- do nothing
      return;
    }
    int startOffset = Math.max(0, offset - MAX_PAGE_SIZE / 2);
    int length = offset + MAX_PAGE_SIZE / 2 - startOffset;
    load(startOffset, length);
  }
  
  private boolean isPending(int offset) {
    int sz = pending.size();
    if (sz == 0) return false;
    if (sz == 1) {
      // special case (for speed)
      Segment seg = pending.first();
      return seg.contains(offset);
    }
    Segment lo = new Segment(offset - MAX_PAGE_SIZE, 0);
    Segment hi = new Segment(offset + 1, 0);
    // search pending segments that may contain offset
    for (Segment seg : pending.subSet(lo, hi)) {
      if (seg.contains(offset)) return true;
    }
    return false;
  }
  
  private void load(final int startOffset, final int length) {
    // simulate something slow like loading from a database
    final Segment seg = new Segment(startOffset, length);
    pending.add(seg);
    // set up code to run in another thread
    Runnable fetch = new Runnable() {
      public void run() {
        try {
          // simulate network
          Thread.sleep(LATENCY_MILLIS);
        } catch (InterruptedException ex) {
          System.out.println("error retrieving page at " + startOffset
              + ": aborting");
          pending.remove(seg);
          return;
        }
        final ArrayList<Integer> page = new ArrayList<Integer>();
        for (int j = 0; j < length; j += 1) {
          page.add(new Integer(j + startOffset));
        }
        // done loading -- make available on the event dispatch thread
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            System.out.println("** loaded " + startOffset + " through "
                + (startOffset + length - 1));
            setData(startOffset, page);
            pending.remove(seg);
          }
        });
      }
    };
    // run on another thread
    new Thread(fetch).start();
  }
  
  private void setData(int offset, ArrayList<Integer> newData) {
    // This method must be called from the event dispatch thread.
    int lastRow = offset + newData.size() - 1;
    dataOffset = offset;
    data = newData;
    fireTableRowsUpdated(offset, lastRow);
  }
  
  public static void main(String[] argv) {
    JTable tab = new JTable(new PagingTableModel());
    JScrollPane sp = new JScrollPane(tab);
    //JScrollPane sp = LazyViewport.createLazyScrollPaneFor(tab);
    
    JFrame f = new JFrame("PagingTableModel");
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.setContentPane(sp);
    f.setSize(200, 148);
    f.setVisible(true);
  }
  
  // ---------------- begin static nested class ----------------
  
  /**
   * This class is used to keep track of which rows have been scheduled for
   * loading, so that rows don't get scheduled twice concurrently. The idea is
   * to store Segments in a sorted data structure for fast searching.
   * 
   * The compareTo() method sorts first by base position, then by length.
   */
  static final class Segment implements Comparable<Segment> {
    private int base = 0, length = 1;
    
    public Segment(int base, int length) {
      this.base = base;
      this.length = length;
    }
    
    public boolean contains(int pos) {
      return (base <= pos && pos < base + length);
    }
    
    public boolean equals(Object o) {
      return o instanceof Segment && base == ((Segment) o).base
          && length == ((Segment) o).length;
    }
    
    public int compareTo(Segment other) {
      //return negative/zero/positive as this object is less-than/equal-to/greater-than other
      int d = base - other.base;
      if (d != 0) return d;
      return length - other.length;
    }
  }
  
}

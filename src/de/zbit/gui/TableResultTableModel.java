/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import de.zbit.analysis.enrichment.AbstractEnrichment;
import de.zbit.analysis.enrichment.GOEnrichment;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.TableResult;
import de.zbit.data.mRNA.mRNA;
import de.zbit.parser.Species;

/**
 * A {@link TableModel} that can be used to visualize a {@link TableResult} class as {@link JTable}.
 * Especially useful for {@link NameAndSignals}
 * 
 * @author Clemens Wrzodek
 */
public class TableResultTableModel<T extends TableResult> extends AbstractTableModel {
  private static final long serialVersionUID = -6542792109889449114L;
  
  /**
   * The {@link NameAndSignals} that should be represented by this {@link TableModel}.
   */
  private List<T> ns;
  
  public TableResultTableModel(List<T> ns) {
    this.ns = ns;
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
    if (ns.size()<1) return 0;
    return ns.get(0).getColumnCount();
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getRowCount()
   */
  public int getRowCount() {
    return ns.size();
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  public Object getValueAt(int rowIndex, int columnIndex) {
    Object o = ns.get(rowIndex).getObjectAtColumn(columnIndex);
    if (o instanceof Signal) {
      // Experiment name and signal type is already in header!
      return ((Signal)o).getSignal();
    } else {
      return o;
    }
  }
  
  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
   */
  @Override
  public Class<?> getColumnClass(int columnIndex) {
    if (ns.size()<1) return super.getColumnClass(columnIndex);
    Object o = ns.get(0).getObjectAtColumn(columnIndex);
    Class<?> c = o!=null?o.getClass():null;
    if (c==null) return super.getColumnClass(columnIndex);
    return c;
  }
  
  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    if (ns.size()<1) return super.getColumnName(column);
    String s = ns.get(0).getColumnName(column);
    if (s==null || s.length()<1) {
      return super.getColumnName(column);
    }
    return s;
  }
  
  public static <T extends TableResult> JComponent buildJTable(TableResultTableModel<T> model) {
    // TODO: Implement a filtered table (a model that automatically adds
    // a JTextField row (similar to online marcar db filter) below headers
    final JTable table = new JTable(model); // new JComponentTableModel()
    
    // Set an appropriate editor for the expected column and type selectors
    /*int maxHeadRow = (isATypeSelectorRequired()?2:1);
    for (int row=0; row<maxHeadRow; row++) {
      for (int col=0; col<dataNew[row].length; col++) {
        Object cur = dataNew[row][col];
        
        TableCellEditor cellEditor;
        if (cur instanceof JCheckBox) {
          cellEditor = new DefaultCellEditor((JCheckBox)cur);
        }else if (cur instanceof JComboBox) {
          cellEditor = new DefaultCellEditor((JComboBox)cur);
        }else if (cur instanceof JTextField) {
          cellEditor = new DefaultCellEditor((JTextField)cur);
        } else {
          cellEditor = table.getCellEditor(0,col);
        }
        
        table.setCellEditor(row, col, cellEditor);      
      }
    }
    
    // Draw JComponents inside the JTable
    JComponentTableRenderer rend = new JComponentTableRenderer();
    for (int i=0; i<table.getColumnCount(); i++) {
      table.getColumnModel().getColumn(i).setCellRenderer(rend);
    }*/
    
    // Set additional attributes
    table.setPreferredScrollableViewportSize(new Dimension(500, 100));
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    //for (int i=0; i<maxHeadRow+1;i++)
      //table.setRowHeight(i, (int) (table.getRowHeight(i)*1.3));
    
    // Disallow dragging columns
    table.getTableHeader().setReorderingAllowed(true);
    
    // Resize columns to a reasonable width
    if (table.getColumnModel().getColumnCount()>0)  {
      int width = table.getColumnModel().getColumn(0).getWidth();
      width = Math.max(width, 120);
      for (int i=0; i<table.getColumnModel().getColumnCount(); i++)
        table.getColumnModel().getColumn(i).setPreferredWidth(width); 
    }
    
    // Add sorting capabilities
    TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(model);
    table.setRowSorter(sorter);
    
    // Add enrichment capabilities
    addRightMousePopup(table, createEnrichmentPopup());
    
    // Put all on a scroll  pane
    final JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    
    // When resizing, try to optimize table size.
    final int defaultWidth;
    if (table.getColumnModel().getColumnCount()>0) 
      defaultWidth = table.getColumnModel().getColumn(0).getWidth();
    else
      defaultWidth = 75;
    
    scrollPane.addComponentListener(new ComponentListener() {
      public void componentHidden(ComponentEvent e) {}
      public void componentMoved(ComponentEvent e) {}
      public void componentShown(ComponentEvent e) {}
      
      public void componentResized(ComponentEvent e) {
        if (table.getColumnCount()<5 && scrollPane.getWidth()>table.getColumnCount()*defaultWidth) {
          table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        } else {
          table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        }
      }
    });
    
    return scrollPane;
  }
  
  private static JPopupMenu createEnrichmentPopup(ActionListener l) {
    JPopupMenu enrichment = new JPopupMenu("Enrichments");
    
    JMenuItem jm = new JMenuItem("Pathway enrichment");
    jm.setActionCommand("KEGG_enrich");
    enrichment.add(jm);
    jm.addActionListener(l);

    jm = new JMenuItem("Gene ontology enrichment");
    jm.setActionCommand("GO_enrich");
    enrichment.add(jm);
    jm.addActionListener(l);
    
    return enrichment;
  }
  
  // TODO: IntegratorGui.addEnrichmentTab(Collection<mRNA>);
  // TODO: Noch besser: irgendwelche listeners feuern und machen!
  public static <T extends TableResult> ActionListener enrichmentAction(final JTable source, final ) {
    return new ActionListener() {
      
      public void actionPerformed(ActionEvent e) {
        // Get selected items
        int[] selRows = source.getSelectedRows();
        if (selRows.length<2) {
          // TODO: Message
        } else {
          List<mRNA> geneList = new ArrayList<mRNA>(selRows.length);
          // TODO: other instanceof's
          TableModel m = source.getModel();
          if (m instanceof TableResultTableModel) {
            List<T> underlyingDataList = ((TableResultTableModel)m).getNameAndSignalsList();
            for (int i=0; i<selRows.length; i++) {
              Object o = underlyingDataList.get(selRows[i]).getRowObject();
              System.out.println(o);
              if (o instanceof mRNA) {
                geneList.add((mRNA)o);
              }
            }
          }
          
          // Make enrichment
          if (geneList.size()<2) {
            // TODO: Message
          } else {
            AbstractEnrichment<String> enrich;
            if (e.getActionCommand().equals("GO_enrich")) {
              //enrich = new GOEnrichment() // TODO: Species and ProgressBar
              try {
                enrich = new GOEnrichment(new Species("","","",null,10090)); // mouse
              } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
              } 
              
            }
            
            // Get enrichment
            List<EnrichmentObject<String>> l = enrich.getEnrichments(geneList);
            new JTable(new TableResultTableModel<EnrichmentObject<String>>(l));
          }
          
        }
      }
    };
  }
  
  private static void addRightMousePopup(JComponent component, final JPopupMenu popup) {
    class PopClickListener extends MouseAdapter {
      public void mousePressed(MouseEvent e){
        if (e.isPopupTrigger()) doPop(e);
      }
      public void mouseReleased(MouseEvent e){
        if (e.isPopupTrigger()) doPop(e);
      }
      private void doPop(MouseEvent e){
        popup.show(e.getComponent(), e.getX(), e.getY());
      }
    }
    component.addMouseListener(new PopClickListener());
  }
  
}

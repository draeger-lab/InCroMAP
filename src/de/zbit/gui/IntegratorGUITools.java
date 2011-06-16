/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;

import de.zbit.analysis.enrichment.AbstractEnrichment;
import de.zbit.gui.CSVImporterV2.CSVImporterV2;
import de.zbit.io.OpenFile;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.parser.Species;
import de.zbit.util.Utils;

/**
 * @author Clemens Wrzodek
 */

@SuppressWarnings("unchecked")
public class IntegratorGUITools {
  public static final transient Logger log = Logger.getLogger(IntegratorGUITools.class.getName());
  
  static {
    // Load list of acceptable species
    List<Species> l=null;
    try {
      l =(List<Species>) Utils.loadGZippedObject(OpenFile.searchFileAndGetInputStream("species/hmr_species_list.dat"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    organisms = l;
  }
  
  /**
   * Supported organisms.
   */
  public static final List<Species> organisms;
  //new String[]{"Homo sapiens (human)", "Mus musculus (mouse)", "Rattus norvegicus (rat)"};
  
  
  
  /**
   * Centralized methods to create unified JLabels.
   * @param s
   * @return {@link JLabel}
   */
  public JLabel createJLabel(String s) {
    return new JLabel(s);    
  }
  
  /**
   * Show an organism selector panel to the user.
   * @return
   */
  public static JLabeledComponent getOrganismSelector() {
    JLabeledComponent l = new JLabeledComponent("Please select your organism",true,organisms);
    // Make a flexible layout
    l.setLayout(new FlowLayout());
    l.setPreferredSize(null);
    GUITools.createTitledPanel(l, "Organism selection");
    return l;
  }
  
  /**
   * Show the {@link CSVImporterV2} dialog.
   * @param parent parent {@link Frame} or {@link Dialog}
   * @param c {@link CSVImporterV2}
   * @param additionalComponent e.g., speciesSelector from {@link #getOrganismSelector()}
   * @return true if ok has been pressed.
   * @throws IOException 
   */
  public static boolean showCSVImportDialog(Component parent, CSVImporterV2 c, JComponent additionalComponent) throws IOException {
    c.setRenameButtonCaption("Edit observation names");
    c.setPreferredSize(new java.awt.Dimension(800, 450));
    
    // Customize the north-dialog.
    if (additionalComponent!=null) {
      JPanel jp = new JPanel(new BorderLayout());
      jp.add(additionalComponent, BorderLayout.NORTH);
      jp.add(c.getOptionalPanel(), BorderLayout.CENTER);
      c.add(jp, BorderLayout.NORTH);
    }
    
    return CSVImporterV2.showDialog(parent, c);
  }
  
  
  /**
   * Create a popup menu that allows a selection of available
   * {@link AbstractEnrichment}s.
   * @param l
   * @return
   */
  public static JPopupMenu createEnrichmentPopup(EnrichmentActionListener l) {
    JPopupMenu enrichment = new JPopupMenu("Enrichments");
    
    JMenuItem jm = new JMenuItem("Pathway enrichment");
    jm.setActionCommand(EnrichmentActionListener.KEGG_ENRICHMENT);
    enrichment.add(jm);
    jm.addActionListener(l);
    
    jm = new JMenuItem("Gene ontology enrichment");
    jm.setActionCommand(EnrichmentActionListener.GO_ENRICHMENT);
    enrichment.add(jm);
    jm.addActionListener(l);
    
    return enrichment;
  }
  
  /**
   * Add a right mouse popup menu to a JComponent.
   * @param component
   * @param popup
   */
  public static void addRightMousePopup(JComponent component, final JPopupMenu popup) {
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
  
  /**
   * Put the {@link JComponent} on a {@link JScrollPane}.
   * @param jc
   * @return
   */
  public static JScrollPane putOnScrollPane(JComponent jc) {
    // Put all on a scroll  pane
    final JScrollPane scrollPane = new JScrollPane(jc, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    
    // When resizing, try to optimize table size.
    if (jc instanceof JTable) {
      IntegratorTab.applyTableConstraints((JTable)jc, scrollPane);
    }
    
    return scrollPane;
  }
  
  /**
   * Execute the {@link Runnable} in an external thread.
   * @param r
   */
  public static void runInSwingWorker(final Runnable r) {
    final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
      public Void doInBackground() {
        try {
          r.run();
        } catch (Throwable t) {
          GUITools.showErrorMessage(null, t);
        }
        return null;
      }
      protected void done() {
        //hideTemporaryPanel();
        //getParentWindow().setEnabled(true);
      }
    };
    
    worker.execute();
  }

  /**
   * Return a priority for an {@link IdentifierType}.
   * NCBI_GeneIDs have the lowest priority, followed by
   * Unique identifiers (ensembl, refseq, etc.) and
   * non-unique identifiers (gene symobls, etc.) have
   * the highest priority.
   * @param type
   * @return
   */
  public static Integer getPriority(IdentifierType type) {
    if (type.equals(IdentifierType.NCBI_GeneID)) return 0;
    else if (type.equals(IdentifierType.Ensembl)) return 1;
    else if (type.equals(IdentifierType.KeggGenes)) return 1;
    else if (type.equals(IdentifierType.RefSeq)) return 1;
    else if (type.equals(IdentifierType.GeneSymbol)) return 2;
    else if (type.equals(IdentifierType.Unknown)) return 3;
    else {
      log.log(Level.SEVERE, "Please implement priority for " + type);
      return 3;
    }
  }
    
}

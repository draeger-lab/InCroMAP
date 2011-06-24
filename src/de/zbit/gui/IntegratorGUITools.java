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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import de.zbit.analysis.enrichment.AbstractEnrichment;
import de.zbit.data.LabeledObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.SignalType;
import de.zbit.gui.CSVImporterV2.CSVImporterV2;
import de.zbit.io.OpenFile;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.parser.Species;
import de.zbit.util.Utils;
import de.zbit.util.ValuePair;
import de.zbit.util.ValueTriplet;

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
   * Centralized method to create unified {@link JLabel}s.
   * @param s
   * @return {@link JLabel}
   */
  public static JLabel createJLabel(String s) {
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
    createEnrichmentPopup(l, enrichment);
    return enrichment;
  }
  
  /**
   * Append enrichment analysis {@link JMenuItem}s to the given
   * {@link JPopupMenu},
   * @param l
   * @param append
   * @return append
   */
  public static JPopupMenu createEnrichmentPopup(EnrichmentActionListener l, JPopupMenu append) {
    
    JMenuItem jm = new JMenuItem("Pathway enrichment");
    jm.setActionCommand(EnrichmentActionListener.KEGG_ENRICHMENT);
    append.add(jm);
    jm.addActionListener(l);
    
    jm = new JMenuItem("Gene ontology enrichment");
    jm.setActionCommand(EnrichmentActionListener.GO_ENRICHMENT);
    append.add(jm);
    jm.addActionListener(l);
    
    return append;
  }
  
  /**
   * Add a "Visualize pathway" {@link JMenuItem} to the given
   * {@link JPopupMenu}.
   * @param l
   * @param append
   * @return
   */
  public static JPopupMenu createKeggPathwayPopup(KEGGPathwayActionListener l, JPopupMenu append) {
    
    JMenuItem jm = new JMenuItem("Visualize pathway");
    jm.setActionCommand(KEGGPathwayActionListener.VISUALIZE_PATHWAY);
    append.add(jm);
    jm.addActionListener(l);
    
    return append;
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

  /**
   * Create a new {@link JLabeledComponent} that lets the user choose a signal from
   * the contained signals in the given <code>ns</code>.
   * @param <T>
   * @param ns
   * @return
   */
  public static <T extends NameAndSignals> JLabeledComponent createSelectExperimentBox(T ns) {
    JLabeledComponent jc = new JLabeledComponent("Select an observation",true,new String[]{"temp"});
    return createSelectExperimentBox(jc, ns);
  }
  
  /**
   * Replace the signals in the existing experiment box by the given one.
   * @param <T>
   * @param jc
   * @param ns
   * @return
   */
  public static <T extends NameAndSignals> JLabeledComponent createSelectExperimentBox(JLabeledComponent jc, T ns) {
    Collection<ValuePair<String, SignalType>> c = ns.getSignalNames();
    jc.setHeaders(c);
    return jc;
  }
  
  /**
   * Shows a signal selection box.
   * @param <T>
   * @param ui
   * @return ValueTriplet of (TabIndex In {@link IntegratorUI#getTabbedPane()}, ExperimentName, {@link SignalType}) or null.  
   */
  public static <T extends NameAndSignals> ValueTriplet<NameAndSignalsTab, String, SignalType> showSelectExperimentBox(IntegratorUI ui) {
    return showSelectExperimentBox(ui, null);
  }
  /**
   * Shows a signal selection box.
   * @param <T>
   * @param ui
   * @param initialSelection
   * @return ValueTriplet of (TabIndex In {@link IntegratorUI#getTabbedPane()}, ExperimentName, {@link SignalType}) or null.
   */
  @SuppressWarnings("rawtypes")
  public static <T extends NameAndSignals> ValueTriplet<NameAndSignalsTab, String, SignalType> showSelectExperimentBox(IntegratorUI ui, IntegratorTab initialSelection) {
    final JPanel jp = new JPanel(new BorderLayout());
    int initialSelIdx=0;
    
    // Create a list of available datasets
    List<LabeledObject> datasets = new LinkedList<LabeledObject>();
    for (int i=0; i<ui.getTabbedPane().getTabCount(); i++) {
      Component c = ui.getTabbedPane().getComponentAt(i);
      if (c instanceof NameAndSignalsTab) {
        //Class<?> cl = ((NameAndSignalsTab)c).getDataContentType(); 
        //if (cl.equals(mRNA.class) || cl.equals(miRNA.class)) {
        if (((NameAndSignalsTab)c).getSourceTab()==null && // Data has not been derived, but read from disk!
            ((NameAndSignals)((NameAndSignalsTab)c).getExampleData()).hasSignals()) {
          if (initialSelection!=null && c.equals(initialSelection)) initialSelIdx=datasets.size();
          datasets.add(new LabeledObject(ui.getTabbedPane().getTitleAt(i), c));
        }
      }
    }
    if (datasets.size()<1) {
      GUITools.showMessage("Could not find any input datasets.", ui.getApplicationName());
      return null;
    } else {
      final JLabeledComponent dataSelect = new JLabeledComponent("Select a dataset",true,datasets);
      dataSelect.setSelectedItem(datasets.get(initialSelIdx));
      
      // Add the dataset selector to a panel
      jp.add (dataSelect, BorderLayout.CENTER);
      
      // Add action listener to let user choose experiment from dataset
      final JLabeledComponent selExpBox = createSelectExperimentBox((NameAndSignals)((NameAndSignalsTab)datasets.get(initialSelIdx).getObject()).getExampleData());
      jp.add(selExpBox, BorderLayout.SOUTH);
      dataSelect.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          NameAndSignalsTab tab = (NameAndSignalsTab) ((LabeledObject)dataSelect.getSelectedItem()).getObject();
          createSelectExperimentBox(selExpBox, (NameAndSignals)tab.getExampleData());
        }
      });
      
      // Show and evaluate dialog
      int ret = JOptionPane.showConfirmDialog(ui, jp, UIManager.getString("OptionPane.titleText"), JOptionPane.OK_CANCEL_OPTION);
      if (ret==JOptionPane.OK_OPTION) {
        ValuePair<String, SignalType> expSignal = (ValuePair<String, SignalType>) selExpBox.getSelectedItem();
        return new ValueTriplet<NameAndSignalsTab, String, SignalType>(
            (NameAndSignalsTab) ((LabeledObject)dataSelect.getSelectedItem()).getObject(),
            expSignal.getA(), expSignal.getB());
      } else {
        return null;
      }
      
    }
  }
    
}

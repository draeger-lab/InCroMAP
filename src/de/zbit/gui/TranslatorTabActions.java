package de.zbit.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import y.base.Node;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.Signal.MergeType;
import de.zbit.gui.prefs.IntegratorIOOptions;
import de.zbit.kegg.TranslatorTools;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.util.StringUtil;

/**
 * Actions for a {@link JToolBar} that can be created
 * for {@link TranslatorPanel}s.
 * @author Clemens Wrzodek
 */
public class TranslatorTabActions implements ActionListener{

  /**
   * The actual tab to perform actions on.
   */
  TranslatorPanel parent;
  
  /**
   * True if and only if microRNA nodes have been inserted
   * into the {@link #parent}s graph.
   */
  boolean parentContainsMiRNANodes=false;
  
  /**
   * 
   */
  private JDropDownButton removeButton = null;
  
  public TranslatorTabActions(TranslatorPanel parent) {
    super();
    this.parent = parent;
  }
  
  
  /**
   * All actions for {@link TranslatorPanel}s are defined
   * here.
   * @author Clemens Wrzodek
   */
  public static enum TPAction implements ActionCommand {
    VISUALIZE_DATA,
    ADD_MIRNAS,
    HIGHLIGHT_ENRICHED_GENES,
    SEARCH_GRAPH,
    REMOVE_MIRNA_NODES,
    REMOVE_PROTEIN_VARIANT_NODES;
    
    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getName()
     */
    public String getName() {
      switch (this) {
      case SEARCH_GRAPH:
        return "Search";
      case REMOVE_MIRNA_NODES:
        return "Remove miRNA nodes";
      case REMOVE_PROTEIN_VARIANT_NODES:
        return "Remove protein variant nodes";
      case ADD_MIRNAS:
        return "Add miRNAs";
        
      default:
        return StringUtil.firstLetterUpperCase(toString().toLowerCase().replace('_', ' '));
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getToolTip()
     */
    public String getToolTip() {
      switch (this) {
        case VISUALIZE_DATA:
          return "Color nodes accoring to fold changes.";
        case HIGHLIGHT_ENRICHED_GENES:
          return "Highlight genes from source enrichment.";
        case SEARCH_GRAPH:
          return "Search for a string in gene names of all nodes";
        case REMOVE_MIRNA_NODES:
          return "Removes all nodes and edges that have been inserted for microRNAs.";
        case REMOVE_PROTEIN_VARIANT_NODES:
          return "Removes all nodes that have been inserted for protein or gene variants.";
        case ADD_MIRNAS:
          return "Add microRNAs with targets in the pathway to the graph.";
          
        default:
          return null; // Deactivate
      }
    }
  }
  
  
  
  
  public void createJToolBarItems(JToolBar bar) {
    // Not here just name, because button actions are linked to this instance!
    String uniqueName = parent.getClass().getSimpleName() + parent.hashCode();
    if (bar.getName().equals(uniqueName)) return;
    bar.removeAll();
    bar.setName(uniqueName);
    
    // Search
    JButton search = GUITools.createJButton(this,
        TPAction.SEARCH_GRAPH, UIManager.getIcon("ICON_SEARCH_16"));
    search.setPreferredSize(new Dimension(16,16));
    bar.add(search);
    
    // Visualize in Pathway
    JPopupMenu visualize = new JPopupMenu("Visualize data");
    KEGGPathwayActionListener al2 = new KEGGPathwayActionListener(parent);
    visualize.add(GUITools.createJMenuItem(al2,
        TPAction.VISUALIZE_DATA, UIManager.getIcon("ICON_GEAR_16")));
    visualize.add(GUITools.createJMenuItem(this,
      TPAction.ADD_MIRNAS, UIManager.getIcon("ICON_GEAR_16")));
    bar.add(new JDropDownButton(UIManager.getIcon("ICON_GEAR_16"), visualize));
    
    
    
    JButton highlight = GUITools.createJButton(al2,
        TPAction.HIGHLIGHT_ENRICHED_GENES, UIManager.getIcon("ICON_GEAR_16"));
    bar.add(highlight);
    
    
    // Remove nodes
    JPopupMenu remove = new JPopupMenu("Remove");
    remove.add(GUITools.createJMenuItem(this, TPAction.REMOVE_MIRNA_NODES, UIManager.getIcon("ICON_GEAR_16")));
    remove.add(GUITools.createJMenuItem(this, TPAction.REMOVE_PROTEIN_VARIANT_NODES, UIManager.getIcon("ICON_GEAR_16")));
    GUITools.setEnabled(false, remove, TPAction.REMOVE_MIRNA_NODES, TPAction.REMOVE_PROTEIN_VARIANT_NODES);
    removeButton = new JDropDownButton(remove.getLabel(), UIManager.getIcon("ICON_GEAR_16"), remove);
    bar.add(removeButton);
    
    
    GUITools.setOpaqueForAllElements(bar, false);    
  }


  /**
   * Enable and disable buttons, based on the graphs content in another thread.
   * @param removeButton
   */
  private void enableRemoveButtonsAsRequired(final JDropDownButton removeButton) {
    SwingWorker<Void, Void> enabler = new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() throws Exception {
        if (removeButton==null || parent==null || parent.getDocument()==null) return null;
        
        TranslatorTools tools = new TranslatorTools(parent);
        boolean containsMiRNA = tools.containsRNAnodes();
        GUITools.setEnabled(containsMiRNA, removeButton.getPopUpMenu(), TPAction.REMOVE_MIRNA_NODES);
        // TODO: Enabler for phosphoprotein nodes.
        
        // Eventually disable the whole DropDownButton.
        removeButton.setEnabled(containsMiRNA); // || containsPhospho
        return null;
      }
    };
    enabler.execute();
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    String command = e.getActionCommand();
    
    if (command.equals(TPAction.SEARCH_GRAPH.toString())) {
      String s = JOptionPane.showInputDialog("Please enter a string to search for");
      if (s!=null) {
        TranslatorTools tools = new TranslatorTools((TranslatorPanel) parent);
        tools.searchGenes(s);
        parent.repaint();
      }
      
    } else if (command.equals(TPAction.REMOVE_MIRNA_NODES.toString())) {
      removeMicroRNAnodes();
    
    } else if (command.equals(TPAction.ADD_MIRNAS.toString())) {
      // MergeType does NOT make any difference, because input data has NO SIGNALS!
      // TODO:
      // 1. Remember organism in TranslatorPanel
      // 2. LoadTargets
      KEGGPathwayActionListener.addMicroRNAs(parent, dataSource);
      
    } else if (command.equals(TPAction.REMOVE_PROTEIN_VARIANT_NODES.toString())) {
      // TODO: Remove protein variants
      GUITools.showErrorMessage(null, "Not yet implemented!");
      
    }
  }


  /**
   * Removes all nodes with type "RNA" from the parent graph.
   */
  public void removeMicroRNAnodes() {
    TranslatorTools tools = new TranslatorTools(parent);
    Map<String, Node> mi2node = tools.getRNA2NodeMap();
    for (Node n: mi2node.values()) {
      tools.getGraph().removeNode(n);
    }
    
    // Disable the "remove" button now
    enableRemoveButtonsAsRequired(removeButton);
    parent.repaint();
  }


  public void updateToolbarButtons(JToolBar toolBar) {
    boolean state = parent.getDocument()!=null;
    for (Component c: toolBar.getComponents()) {
      c.setEnabled(state);
    }
    
    // Enable or disable enrichment highlighting
    if (state) {
      state = (parent.getData()!=null && parent.getData() instanceof EnrichmentObject);
      GUITools.setEnabled(state, toolBar, TPAction.HIGHLIGHT_ENRICHED_GENES);
    }
    
    // Toggle the removeButton
    enableRemoveButtonsAsRequired(removeButton);
  }
  

  

}

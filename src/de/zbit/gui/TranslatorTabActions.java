package de.zbit.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import de.zbit.data.EnrichmentObject;
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
    HIGHLIGHT_ENRICHED_GENES,
    SEARCH_GRAPH;
    
    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getName()
     */
    public String getName() {
      switch (this) {
      case SEARCH_GRAPH:
        return "Search";
        
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
    KEGGPathwayActionListener al2 = new KEGGPathwayActionListener(parent);
    JButton showPathway = GUITools.createJButton(al2,
        TPAction.VISUALIZE_DATA, UIManager.getIcon("ICON_GEAR_16"));
    bar.add(showPathway);
    
    JButton highlight = GUITools.createJButton(al2,
        TPAction.HIGHLIGHT_ENRICHED_GENES, UIManager.getIcon("ICON_GEAR_16"));
    bar.add(highlight);
    
    GUITools.setOpaqueForAllElements(bar, false);    
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
    }
  }


  public void updateToolbarButtons(JToolBar toolBar) {
    boolean state = parent.getDocument()!=null;
    for (Component c: toolBar.getComponents()) {
      c.setEnabled(state);
    }
    if (state) {
      if (parent.getData()!=null && parent.getData() instanceof EnrichmentObject) {
        state = true;
      } else {
        state = false;
      }
      GUITools.setEnabled(state, toolBar, TPAction.HIGHLIGHT_ENRICHED_GENES);
    }
    
  }
  

  

}

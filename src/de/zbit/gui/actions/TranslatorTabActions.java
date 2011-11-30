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
package de.zbit.gui.actions;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import y.base.Node;
import y.view.Graph2D;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAandTarget;
import de.zbit.data.miRNA.miRNAtargets;
import de.zbit.data.protein.ProteinModificationExpression;
import de.zbit.gui.ActionCommand;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.JDropDownButton;
import de.zbit.gui.actions.listeners.KEGGPathwayActionListener;
import de.zbit.integrator.NameAndSignal2PWTools;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.parser.Species;
import de.zbit.util.StringUtil;
import de.zbit.util.TranslatorTools;
import de.zbit.util.ValuePair;
import de.zbit.visualization.VisualizeDataInPathway;

/**
 * Actions for a {@link JToolBar} that can be created
 * for {@link TranslatorPanel}s.
 * 
 * <p><i>Note:<br/>
 * Due to yFiles license requirements, we have to obfuscate this class
 * in the JAR release of this application. Thus, this class
 * can not be found by using the class name.<br/> If you can provide us
 * with a proof of possessing a yFiles license yourself, we can send you
 * an unobfuscated release of Integrator.</i></p>
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class TranslatorTabActions implements ActionListener{

  /**
   * The actual tab to perform actions on.
   */
  TranslatorPanel<Graph2D> parent;
  
  /**
   * True if and only if microRNA nodes have been inserted
   * into the {@link #parent}s graph.
   */
  boolean parentContainsMiRNANodes=false;
  
  /**
   * The DropDown button containing the "remove XY" visualization operations
   */
  private JDropDownButton removeButton = null;
  
  public TranslatorTabActions(TranslatorPanel<Graph2D> parent) {
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
    VISUALIZE_ENRICHMENT_PVALUES,
    ADD_MIRNAS,
    HIGHLIGHT_ENRICHED_GENES,
    SEARCH_GRAPH,
    
    REMOVE_MIRNA_NODES,
    REMOVE_NOT_DIFFERENTIALLY_EXPRESSED_MIRNA_NODES,
    REMOVE_PROTEIN_MODIFICATION_BOXES,
    REMOVE_MRNA_VISUALIZATION,
    REMOVE_DNA_METHYLATION_BOXES,
    REMOVE_ENRICHMENT_PVALUES;
    
    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getName()
     */
    public String getName() {
      switch (this) {
      case SEARCH_GRAPH:
        return "Search";
      case ADD_MIRNAS:
        return "Add miRNAs";
      case VISUALIZE_ENRICHMENT_PVALUES:
        return "Color pathway-references according enrichment"; //"Visualize enrichment p-values";
      case REMOVE_ENRICHMENT_PVALUES:
        return "Reset colored pathway-reference nodes";
        
      case REMOVE_MIRNA_NODES:
        return "Remove miRNA nodes";
      case REMOVE_NOT_DIFFERENTIALLY_EXPRESSED_MIRNA_NODES:
        return "Remove not differentially expressed miRNA nodes";
      case REMOVE_PROTEIN_MODIFICATION_BOXES:
        return "Remove protein modification boxes";
      case REMOVE_MRNA_VISUALIZATION:
        return "Remove mRNA visualization";
      case REMOVE_DNA_METHYLATION_BOXES:
        return "Remove DNA methylation boxes";
        
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
          return "Color nodes accoring to fold changes or p-values.";
        case VISUALIZE_ENRICHMENT_PVALUES:
          return "Color pathway-reference nodes accoring to p-values or signals of enriched genes.";
        case REMOVE_ENRICHMENT_PVALUES:
          return "Removes all changes made while coloring pathway-reference-nodes.";
        case HIGHLIGHT_ENRICHED_GENES:
          return "Highlight genes from source enrichment.";
        case SEARCH_GRAPH:
          return "Search for a string in gene names of all nodes";
        case ADD_MIRNAS:
          return "Add microRNAs with targets in the pathway to the graph.";
          
        case REMOVE_MIRNA_NODES:
          return "Removes all nodes and edges that have been inserted for microRNAs.";
        case REMOVE_PROTEIN_MODIFICATION_BOXES:
          return "Removes all boxes that have been inserted for protein or gene modifications.";
        case REMOVE_MRNA_VISUALIZATION:
          return "Resets the background color of all nodes.";
        case REMOVE_DNA_METHYLATION_BOXES:
          return "Remove all boxes from nodes that represent DNA methylation values.";
          
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
        TPAction.VISUALIZE_DATA, UIManager.getIcon("ICON_PENCIL_16")));
    visualize.add(GUITools.createJMenuItem(al2,
      TPAction.VISUALIZE_ENRICHMENT_PVALUES, UIManager.getIcon("ICON_PENCIL_16")));
    visualize.add(GUITools.createJMenuItem(this,
      TPAction.ADD_MIRNAS, UIManager.getIcon("ICON_PENCIL_16")));
    bar.add(new JDropDownButton(UIManager.getIcon("ICON_PENCIL_16"), visualize));
    
    
    
    JButton highlight = GUITools.createJButton(al2,
        TPAction.HIGHLIGHT_ENRICHED_GENES, UIManager.getIcon("ICON_GEAR_16"));
    bar.add(highlight);
    
    
    // Remove nodes
    JPopupMenu remove = new JPopupMenu("Remove");
    remove.add(GUITools.createJMenuItem(this, TPAction.REMOVE_MRNA_VISUALIZATION, UIManager.getIcon("ICON_TRASH_16")));
    remove.add(GUITools.createJMenuItem(this, TPAction.REMOVE_MIRNA_NODES, UIManager.getIcon("ICON_TRASH_16")));
    remove.add(GUITools.createJMenuItem(this, TPAction.REMOVE_NOT_DIFFERENTIALLY_EXPRESSED_MIRNA_NODES, UIManager.getIcon("ICON_TRASH_16")));
    remove.add(GUITools.createJMenuItem(this, TPAction.REMOVE_PROTEIN_MODIFICATION_BOXES, UIManager.getIcon("ICON_TRASH_16")));
    remove.add(GUITools.createJMenuItem(this, TPAction.REMOVE_DNA_METHYLATION_BOXES, UIManager.getIcon("ICON_TRASH_16")));
    remove.add(GUITools.createJMenuItem(this, TPAction.REMOVE_ENRICHMENT_PVALUES, UIManager.getIcon("ICON_TRASH_16")));
    // Set by default all to disabled. enableRemoveButtonsAsRequired() does the enabling job.
    GUITools.setEnabled(false, remove, TPAction.REMOVE_MIRNA_NODES, TPAction.REMOVE_NOT_DIFFERENTIALLY_EXPRESSED_MIRNA_NODES, TPAction.REMOVE_PROTEIN_MODIFICATION_BOXES,
      TPAction.REMOVE_MRNA_VISUALIZATION,TPAction.REMOVE_DNA_METHYLATION_BOXES, TPAction.REMOVE_ENRICHMENT_PVALUES);
    
    removeButton = new JDropDownButton(remove.getLabel(), UIManager.getIcon("ICON_TRASH_16"), remove);
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
        
        // Test for various visualizations
        boolean[] visData = new VisualizeDataInPathway(parent).getVisualizedDataTypes();
        GUITools.setEnabled(visData[0], removeButton.getPopUpMenu(), TPAction.REMOVE_MRNA_VISUALIZATION);
        GUITools.setEnabled(visData[2], removeButton.getPopUpMenu(), TPAction.REMOVE_PROTEIN_MODIFICATION_BOXES);
        GUITools.setEnabled(visData[3], removeButton.getPopUpMenu(), TPAction.REMOVE_DNA_METHYLATION_BOXES);
        GUITools.setEnabled(visData[4], removeButton.getPopUpMenu(), TPAction.REMOVE_ENRICHMENT_PVALUES);
        
        
        // Test for RNA nodes
        TranslatorTools tools = new TranslatorTools(parent);
        boolean containsMiRNA = tools.containsRNAnodes();
        GUITools.setEnabled(containsMiRNA, removeButton.getPopUpMenu(), TPAction.REMOVE_MIRNA_NODES);
        
        // Test for expression
        GUITools.setEnabled((containsMiRNA && NameAndSignal2PWTools.containsNotDifferentiallyExpressedMiRNA(tools)),
          removeButton.getPopUpMenu(), TPAction.REMOVE_NOT_DIFFERENTIALLY_EXPRESSED_MIRNA_NODES);
        
        // Eventually disable the whole DropDownButton.
        removeButton.setEnabled(containsMiRNA || visData[0] || visData[2] || visData[3] || visData[4]);
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
        TranslatorTools tools = new TranslatorTools((TranslatorPanel<Graph2D>) parent);
        tools.searchGenes(s);
        parent.repaint();
      }
      
    } else if (command.equals(TPAction.REMOVE_MIRNA_NODES.toString())) {
      removeMicroRNAnodes();
      
    } else if (command.equals(TPAction.REMOVE_NOT_DIFFERENTIALLY_EXPRESSED_MIRNA_NODES.toString())) {  
      removeNotDifferentiallyExpressedMicroRNAnodes();
    
    } else if (command.equals(TPAction.ADD_MIRNAS.toString())) {
      addMicroRNAnodes();
      
    } else if (command.equals(TPAction.REMOVE_PROTEIN_MODIFICATION_BOXES.toString())) {
      new VisualizeDataInPathway(parent).removeVisualization(ProteinModificationExpression.class);
      enableRemoveButtonsAsRequired(removeButton);
      parent.repaint();
      
    } else if (command.equals(TPAction.REMOVE_MRNA_VISUALIZATION.toString())) {
      new VisualizeDataInPathway(parent).removeVisualization(mRNA.class);
      enableRemoveButtonsAsRequired(removeButton);
      parent.repaint();

    } else if (command.equals(TPAction.REMOVE_DNA_METHYLATION_BOXES.toString())) {
      new VisualizeDataInPathway(parent).removeVisualization(DNAmethylation.class);
      enableRemoveButtonsAsRequired(removeButton);
      parent.repaint();

    } else if (command.equals(TPAction.REMOVE_ENRICHMENT_PVALUES.toString())) {
      new VisualizeDataInPathway(parent).removeVisualization(EnrichmentObject.class);
      enableRemoveButtonsAsRequired(removeButton);
      parent.repaint();
      
    }
  }


  /**
   * Add microRNA nodes to the graph.
   * <b>Displayes and hides a temporary progress bar!</b>
   * @return true if and only if at least one node has been added to the graph.
   */
  public boolean addMicroRNAnodes() {
    if (!parent.isReady()) return false;
    parent.showTemporaryLoadingBar("Adding microRNAs to pathway...");
    
    // 1. Get organism from TranslatorPanel
    Species spec = getSpeciesOfPathway(parent, IntegratorUITools.organisms);
    
    // 2. LoadTargets
    ValuePair<miRNAtargets, Species> vp = IntegratorUITools.loadMicroRNAtargets(spec);
    
    // 3. Visualize targets.
    int nodesAdded = 0;
    if (vp!=null && vp.getA()!=null && vp.getA().size()>0) {
      nodesAdded = KEGGPathwayActionListener.addMicroRNAs((Graph2D)parent.getDocument(), miRNAandTarget.getList(vp.getA()));
    }
    
    parent.hideTemporaryLoadingBar();
    return (nodesAdded>0);
  }


  /**
   * Get the {@link Species} of a pathway.
   * @param tp
   * @param spec a list of species to choose from.
   * @return {@link Species} of the pathway, currently shown in the {@link TranslatorPanel}.
   * Or <code>NULL</code> if it is unknown, the panel is not ready or it is a reference pw.
   */
  public static Species getSpeciesOfPathway(TranslatorPanel<?> tp, List<Species> spec) {
    File f = tp.getInputFile();
    if (f==null) return null;
    if (spec==null) try {
      spec = Species.generateSpeciesDataStructure();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    
    // Return prefix of input file (e.g., "mmu00124.xml" => "mmu").
    String prefix = getPrefix(f.getName().trim());
    if (prefix==null || prefix.length()<1 || prefix.equalsIgnoreCase("ko") || 
        prefix.equalsIgnoreCase("map") || prefix.equalsIgnoreCase("ec") || 
        prefix.equalsIgnoreCase("rn")) {
      // Reference, or non-organism-specific pathways.
      return null;
    }
    
    // Search species of pathway
    return Species.search(spec, prefix, Species.KEGG_ABBR);
  }


  /**
   * @param name
   * @return first letters of the string, until NOT {@link Character#isLetter(char)}.
   */
  private static String getPrefix(String name) {
    StringBuffer prefix = new StringBuffer();
    if (name!=null) {
      char[] nameA = name.toCharArray();
      for (char c: nameA) {
        if (!Character.isLetter(c)) break;
        prefix.append(c);
      }
    }
    return prefix.toString();
  }


  /**
   * Removes all nodes with type "RNA" from the parent graph.
   */
  public void removeMicroRNAnodes() {
    // Ensure to notify the graph first, that we can not have any
    // visualized miRNA now.
    new VisualizeDataInPathway(parent).removeVisualization(miRNA.class);
    
    // Remove miRNA nodes
    TranslatorTools tools = new TranslatorTools(parent);
    removeMicroRNAnodes(tools);
    
    // Disable the "remove" button now
    enableRemoveButtonsAsRequired(removeButton);
    parent.repaint();
  }
  
  /**
   * Removes all nodes with type "RNA", that have signals
   * marked as not differentially expressed (in properties)
   *  from the parent graph.
   */
  public void removeNotDifferentiallyExpressedMicroRNAnodes() {
    
    // Remove miRNA nodes
    TranslatorTools tools = new TranslatorTools(parent);
    NameAndSignal2PWTools.containsNotDifferentiallyExpressedMiRNA(tools, true);
    
    // Check if we removed all nodes and reflect this change
    if (!tools.containsRNAnodes()) {
      new VisualizeDataInPathway(parent).removeVisualization(miRNA.class);
    }
    
    // Disable the "remove" button now
    enableRemoveButtonsAsRequired(removeButton);
    parent.repaint();
  }


  /**
   * Retrieves the {@link TranslatorTools#getRNA2NodeMap()} and
   * removed all nodes in this maps values.
   * @param tools
   */
  public static void removeMicroRNAnodes(TranslatorTools tools) {
    synchronized (tools.getGraph()) {
      Map<String, List<Node>> mi2node = tools.getRNA2NodeMap();
      for (List<Node> nl: mi2node.values()) {
        for (Node n: nl) {
          tools.getGraph().removeNode(n);
        }
      }
    }
  }


  public void updateToolbarButtons(JToolBar toolBar) {
    boolean state = parent.getDocument()!=null;
    for (Component c: toolBar.getComponents()) {
      c.setEnabled(state);
    }
    
    // Enable or disable enrichment highlighting
    if (state) {
      state = (parent.getData(TPAction.HIGHLIGHT_ENRICHED_GENES.toString())!=null);
      GUITools.setEnabled(state, toolBar, TPAction.HIGHLIGHT_ENRICHED_GENES);
    }
    
    // Toggle the removeButton
    enableRemoveButtonsAsRequired(removeButton);
  }
  

  

}

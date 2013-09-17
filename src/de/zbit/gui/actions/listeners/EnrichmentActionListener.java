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
package de.zbit.gui.actions.listeners;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import de.zbit.analysis.enrichment.AbstractEnrichment;
import de.zbit.analysis.enrichment.GOEnrichment;
import de.zbit.analysis.enrichment.KEGGPathwayEnrichment;
import de.zbit.analysis.enrichment.MSigDB_GSEA_Enrichment;
import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.id.CompoundID;
import de.zbit.data.id.GeneID;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.actioncommand.ActionCommandWithIcon;
import de.zbit.gui.customcomponents.ProgressWorker;
import de.zbit.gui.table.JTableFilter;
import de.zbit.gui.tabs.IntegratorTab;
import de.zbit.gui.tabs.IntegratorTabWithTable;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.util.Species;
import de.zbit.util.StringUtil;

/**
 * Can handle enrichment actions for {@link IntegratorTab}s.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class EnrichmentActionListener implements ActionListener {
  public static final transient Logger log = Logger.getLogger(EnrichmentActionListener.class.getName());
  
  /**
   * The source tab
   */
  IntegratorTabWithTable source;
  
  /**
   * Will be infered from {@link #source}
   */
  private Species species;
  
  /**
   * Determins wether the {@link JTable} selection should
   * be taken (false) or a dialog should prompt the user
   * for genes (true).
   */
  boolean withDialog = false;
  
  /**
   * All available Enrichments are listed here.
   * @author Clemens Wrzodek
   */
  public static enum Enrichments implements ActionCommandWithIcon {
    KEGG_ENRICHMENT,
    GO_ENRICHMENT,
    /**
     * Enrichments from the MSigDB (Molecular Signatures Database)
     * http://www.broadinstitute.org/gsea/
     */
    MSIGDB_ENRICHMENT;
    
    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getName()
     */
    public String getName() {
      switch (this) {
      case KEGG_ENRICHMENT:
        return "KEGG Pathway Enrichment";
      case GO_ENRICHMENT:
        return "GO Enrichment";
      case MSIGDB_ENRICHMENT:
        return "MSigDB Enrichment";
        
      default: // "Enrichment";
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
        case KEGG_ENRICHMENT:
          return "KEGG Pathway Enrichment";
        case GO_ENRICHMENT:
          return "Gene Ontology Enrichment";
        case MSIGDB_ENRICHMENT:
          return "Perform an enrichment, based on a gene set from "+
          "<a href=http://www.broadinstitute.org/gsea/>http://www.broadinstitute.org/gsea/</a>";
          
        default:
          return null; // Deactivate
      }
    }
    
    /* (non-Javadoc)
     * @see de.zbit.gui.ActionCommandWithIcon#getIcon()
     */
    @Override
    public Icon getIcon() {
      switch (this) {
        case KEGG_ENRICHMENT:
          return UIManager.getIcon("ICON_KEGG_16");
        case GO_ENRICHMENT:
          return UIManager.getIcon("ICON_GO_16");
        case MSIGDB_ENRICHMENT:
          return UIManager.getIcon("ICON_MSIGDB_16");
          
        default:
          return null; // No icon
      }
    }
    
  }
  
  public EnrichmentActionListener(IntegratorTabWithTable source) {
    this.source = source;
    if (source!=null) {
      this.species = source.getSpecies(false);
    }
  }
  
  public EnrichmentActionListener(IntegratorTabWithTable source, boolean withDialog) {
    this(source);
    this.withDialog = withDialog;
  }
  
  
  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @SuppressWarnings("rawtypes")
  public synchronized void actionPerformed(final ActionEvent e) {
    // Get selected items
    final List<?> geneList;
    if (e.getSource() instanceof List) {
      // Only for integrated enrichments (see IntegratedEnrichmentDialog).
      geneList = getGeneList(e.getModifiers()==1?true:false, ((List)e.getSource()).toArray());
    } else if (e.getSource() instanceof IntegratorTabWithTable) {
      geneList = getGeneList((IntegratorTabWithTable)e.getSource());
    } else {
      geneList = getGeneList();
    }
    if (geneList==null || geneList.size()<1) {
      GUITools.showErrorMessage(source, "No elements selected for enrichment analysis.");
      return;
    } else if (!checkGeneList(geneList, true)) return;
    final boolean listContainsCompoundIDs = listContainsCompoundIDs(geneList);
    final boolean listContainsGeneIDs = listContainsGeneIDs(geneList);
    final String loadingString = "Performing enrichment analysis...";
    
    // Log this action.
    if (log.isLoggable(Level.FINER)) {
      log.finer(loadingString + e.getActionCommand() + " on " + Arrays.deepToString(geneList.toArray()));
    }
    
    // Create a worker to execute everything in a new thread.
    if (species==null && source!=null) this.species = source.getSpecies();
    SwingWorker<Collection<? extends NameAndSignals>, Void> worker = new ProgressWorker<Collection<? extends NameAndSignals>, Void>() {
      @Override
      protected Collection<? extends NameAndSignals> doInBackground() throws Exception {
        // Get Enrichment class
        final AbstractEnrichment<String> enrich;
        try {
          log.info("Downloading and reading enrichment file.");
          if (e.getActionCommand().equals(Enrichments.GO_ENRICHMENT.toString())) {
            enrich = new GOEnrichment(species, getProgressBar());
          } else if (e.getActionCommand().equals(Enrichments.KEGG_ENRICHMENT.toString())) {
              enrich = new KEGGPathwayEnrichment(species,listContainsGeneIDs,listContainsCompoundIDs,getProgressBar());
          } else if (e.getActionCommand().equals(Enrichments.MSIGDB_ENRICHMENT.toString())) {
            enrich = new MSigDB_GSEA_Enrichment(species, getProgressBar());
          } else {
            GUITools.showErrorMessage(source, String.format("Unknown enrichment command: %s", e.getActionCommand()));
            return null;
          }
        } catch (IOException e1) {
          GUITools.showErrorMessage(source, e1, "Could not read enrichment mapping.");
          return null;
        }
        
        // Perform analysis
        log.info(loadingString);
        List<EnrichmentObject<String>> l=null;
        /*if (geneList.get(0) instanceof mRNA) {
          l = enrich.getEnrichments((List<mRNA>)geneList);
        } else if (geneList.get(0) instanceof Integer) {
          // Assume geneIDs
          log.log(Level.INFO, "Received an Integer List for enrichment analysis. Assuming they are GeneIDs!");
          l = enrich.getEnrichments((List<Integer>)geneList, IdentifierType.NCBI_GeneID);
        } else if (geneList.get(0) instanceof EnrichmentObject) {
          l = enrich.getEnrichments(EnrichmentObject.mergeGeneLists((Iterable<EnrichmentObject>) geneList));
        } else {*/
          //GUITools.showErrorMessage(source, String.format("Enrichment for %s is not yet implemented.", geneList.get(0).getClass()));
        try {
          l = enrich.getEnrichments(geneList,null,null);
        } catch (Throwable e) {
          e.printStackTrace();
          GUITools.showErrorMessage(null, e);
        }
        //}
        
        // Inform user about results
        if (l!=null && l.size()<1) {
          GUITools.showMessage("Could not find any enriched objects.", enrich.getName());
          return null; // Will close the tab on null-result.
        } /*else {
          System.out.println(l.toString().replace("]], [", "]]\n["));
        }*/
        
        return l;
      }
    };
    
    // Create tab
    String eName = Enrichments.valueOf(e.getActionCommand()).getName();
    eName = StringUtil.makeUnique(IntegratorUI.getInstance().getTabNames(), eName);
    NameAndSignalsTab tab = new NameAndSignalsTab(IntegratorUI.getInstance(), worker, loadingString, species);
    String tip = eName + " for " + geneList.size() + " objects";
    if (source!=null && source.getName()!=null) tip+= " from \"" + source.getName() + "\".";
    IntegratorUI.getInstance().addTab(tab, eName, tip, IntegratorUITools.inferIconForTab(tab));
    
    // An enrichment tab should always have a source.
    tab.setSourceTab(this.source);
    if (this.source==null) {
      if (e.getSource()!=null) {
        if (e.getSource() instanceof List) {
          tab.setSourceTab((IntegratorTab<?>) ((List)e.getSource()).get(0));
        } else if (e.getSource() instanceof IntegratorTabWithTable) {
          tab.setSourceTab((IntegratorTabWithTable)e.getSource());
        }
      }
    }
  }
  
  
  /**
   * Check if a list contains compounds.
   * @param geneList
   * @return {@code TRUE} if {@code geneList} contains at least one
   * item that is an instance of {@link CompoundID}.
   */
  public static boolean listContainsCompoundIDs(List<?> geneList) {
    for (Object o : geneList) {
      if (o instanceof CompoundID) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Check if a list contains genes.
   * @param geneList
   * @return {@code TRUE} if {@code geneList} contains at least one
   * item that is an instance of {@link GeneID}.
   */
  public static boolean listContainsGeneIDs(List<?> geneList) {
    for (Object o : geneList) {
      if (o instanceof GeneID) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks the gene list e.g. if it contains miRNAs and
   * if these miRNAs contain targets.
   * <p>Errors are displayed by this method itself.
   * @param geneList
   * @return
   */
  private boolean checkGeneList(List<?> geneList, boolean showMessage) {
    boolean noTargetsFound = true;
    for (Object o: geneList) {
      if (o instanceof miRNA) {
        if (((miRNA)o).hasTargets()) {
          noTargetsFound = false;
          break;
        }
      } else {
        noTargetsFound = false;
        break;
      }
    }
    
    if (noTargetsFound && showMessage) {
      GUITools.showMessage("None of the selected miRNAs has annotated targets. Please use the " +
      		"\"annotate targets\" button to annotate your miRNAs with targets.", "Can not continue");
    }
    
    return !noTargetsFound;
  }

  /**
   * Note: also works for compounds.
   * @return list of selected genes for the enrichment.
   */
  private List<?> getGeneList() {
    List<?> geneList = source.getSelectedItems();
    
    // Get content string
    String content = "genes";
    if (CompoundID.class.isAssignableFrom(source.getDataContentType())) {
      content = "compounds";
    }
    
    // Eventually ask user
    if (withDialog) {
      boolean showFilterDialog=true;
      if (geneList!=null && geneList.size()>1) {
        int ret = GUITools.showQuestionMessage(source, "Do you want to take the selected " + content + " for the enrichment analysis?", "Enrichment analysis", "Yes", "No");
        if (ret==0) {
          showFilterDialog=false;
        }
      }
      if (showFilterDialog) {
        JTableFilter filt = showJTableFilter(source);
        
        if (filt==null) return null;
        geneList = source.getSelectedItems(filt.getSelectedRows());
      }

    }
    return geneList;
  }
  
  /**
   * @param tabs
   * @return list of selected genes for the enrichment.
   * This MUST BE {@link IntegratorTabWithTable} and is Object for compatibility reasons.
   * WARNING: List may contain different types!
   */
  private List<?> getGeneList(Object... tabs) {
    return getGeneList(false, tabs);
  }
  /**
   * @param and if true, connects everything with "And". If false (recommended default),
   * an "Or"-connection is performed. NOTE: "And" does only work for instances of
   * {@link GeneID}.
   * @param tabs
   * @return list of selected genes for the enrichment.
   * This MUST BE {@link IntegratorTabWithTable} and is Object for compatibility reasons.
   * WARNING: List may contain different types!
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  private List<?> getGeneList(boolean and, Object... tabs) {
    List geneList = new ArrayList();
    for (Object ta : tabs) {
      IntegratorTabWithTable tab = (IntegratorTabWithTable) ta;
      geneList.addAll(tab.getSelectedItems());
    } 
    
    // Eventually ask user
    if (withDialog) {
      geneList.clear();
      boolean showFilterDialog=true;
//      if (geneList!=null && geneList.size()>1) {
//        int ret = GUITools.showQuestionMessage(source, "Do you want to take the selected genes for the enrichment analysis?", "Enrichment analysis", "Yes", "No");
//        if (ret==0) {
//          showFilterDialog=false;
//        }
//      }
      if (showFilterDialog) {
        for (Object ta : tabs) {
          IntegratorTabWithTable tab = (IntegratorTabWithTable) ta;
          
          JTableFilter filt = showJTableFilter(tab);
          
          if (filt==null) return null;
          List<?> newItems = tab.getSelectedItems(filt.getSelectedRows());
          // Check for missing miRNA targets and annotate
          if (!checkGeneList(newItems, false)) {
            if (tab instanceof NameAndSignalsTab) {
              ((NameAndSignalsTab)tab).getActions().annotateMiRNAtargets();
            }
          }
          
          
          // Add items to list
          if (!and || geneList.size()<=0) {
            // default = "or"-connection, or build initial "and" list.
            geneList.addAll(newItems);
          } else {
            // perform AND
            //geneList.retainAll(newItems); // performs exact object checks
            RetainAllGeneIDs(geneList, newItems);
            if (geneList.size()<1) break; // and-connection=> will never get >0 again.
          }
        }
      }
    }
    
    return geneList;
  }

  /**
   * Show and return a {@link JTableFilter} dialog for the given tab. 
   * @param tab
   * @return {@link JTableFilter} dialog.
   */
  public static JTableFilter showJTableFilter(IntegratorTabWithTable tab) {
    String typeName = IntegratorUI.getShortTypeNameForNS(tab.getDataContentType());
    if (typeName==null) typeName = ""; else typeName += "-";
    String fileName = tab.getName();
    if (fileName==null) fileName = ""; else fileName = " (\"" + fileName + "\")";
    String moreTableInfoString = String.format("%stable%s", typeName, fileName);
    String title = String.format("Apply filter to %s to select genes for enrichment", moreTableInfoString);
    
    JTableFilter filt = new JTableFilter((JTable) tab.getVisualization());
    tab.setDefaultInitialSelectionOfJTableFilter(filt);
    filt.setDescribingLabel(title);
    filt = JTableFilter.showDialog(tab, filt,title);
    return filt;
  }

  /**
   * Performs {@link Collection#retainAll(Collection)} with a special
   * implementation for {@link GeneID}s. Compares and retains elements
   * if they have the same GeneID. Objects that don't implement the
   * {@link GeneID} interface are being removed!
   * @param original
   * @param newItems
   * @return true if <code>original</code> has been modified
   */
  @SuppressWarnings("rawtypes")
  public static boolean RetainAllGeneIDs(Iterable original, Iterable newItems) {
    boolean modified = false;
    Iterator<?> e = original.iterator();
    Set<Integer> newItemsGeneIDs = NameAndSignals.getGeneIds(newItems);
    while (e.hasNext()) {
      Object item = e.next();
      if (item instanceof miRNA) {
        boolean oneTargetIsInList = false;
        for (miRNAtarget target : ((miRNA) item).getTargets()) {
          if (newItemsGeneIDs.contains(target.getTarget())) {
            oneTargetIsInList = true;
            break;
          }
        }
        if (!oneTargetIsInList) {
          e.remove();
          modified = true;
        }
      } else if (item instanceof GeneID) {
        if (!newItemsGeneIDs.contains(((GeneID) item).getID())) {
          e.remove();
          modified = true;
        }
      } else {
        e.remove();
        modified = true;
      }
    }
    return modified;
  }

  /**
   * @param species
   */
  public void setSpecies(Species species) {
    this.species = species;
  }
  
}

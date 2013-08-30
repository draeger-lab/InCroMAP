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
 * Copyright (C) 2011-2013 by the University of Tuebingen, Germany.
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
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.id.GeneID;
import de.zbit.gui.BaseFrameTab;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.actioncommand.ActionCommand;
import de.zbit.gui.tabs.IntegratorTab;
import de.zbit.gui.tabs.IntegratorTabWithTable;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.io.csv.CSVWriter;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.mapper.KeggGenesID2GeneID;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.mapper.enrichment.KeggPathway2KEGGGeneIDs;
import de.zbit.util.ArrayUtils;
import de.zbit.util.Species;
import de.zbit.util.Utils;
import de.zbit.util.objectwrapper.ValuePair;

/**
 * For a given KEGG Pathway, this class exports all genes in this pathway
 * and associated experimental data from a given set of {@link NameAndSignals}.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class ExportPathwayData implements ActionListener {
  public static final transient Logger log = Logger.getLogger(ExportPathwayData.class.getName());
  
  /**
   * The source tab (should be a pathway enrichment!)
   */
  BaseFrameTab source;
  
  /**
   * Action command for PATHWAY_EXPORT actions.
   */
  public final static ActionCommand PATHWAY_EXPORT = new ActionCommand() {
    @Override
    public String getToolTip() {
      return "Exports a table that contains all genes in this pathway and associated expression values.";
    }
    @Override
    public String getName() {
      return "Export details";
    }
  };
  
  
  /**
   * @param source The source tab from which this one originates. This
   * is the tab that contains a pathway enrichment.
   */
  public ExportPathwayData(BaseFrameTab source) {
    this.source = source;
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals(PATHWAY_EXPORT.toString())) {
      
      if (this.source==null || !(this.source instanceof IntegratorTab)) {
        GUITools.showErrorMessage(null, "Could not detect data source for pathway export.");
        return;
      }
      
      // Try to get the original input dataset (the one we have performed an enrichment on)
      final Collection<? extends NameAndSignals> data;
      {
        IntegratorTabWithTable source = (IntegratorTabWithTable) this.source;
        if (source!=null && source instanceof IntegratorTabWithTable) {
          while (((IntegratorTabWithTable) source).getDataContentType().equals(EnrichmentObject.class) ||
              !(source instanceof NameAndSignalsTab) ) {
            source = (IntegratorTabWithTable) ((IntegratorTabWithTable) source).getSourceTab();
          }
          if (source instanceof NameAndSignalsTab) {
            data = ((NameAndSignalsTab) source).getData();
          } else {
            data = null;
          }
        } else {
          data = null;
        }
      }
      
      // Get the selected pathway(s)
      final List<?> geneList = ((IntegratorTab<?>) this.source).getSelectedItems();
      if (geneList==null || geneList.size()<1) {
        GUITools.showErrorMessage(null, "No elements selected for pathway export.");
        return;
      }
      
      // For all KEGG Pathway ids export a file
      for (final Object pw : geneList) {
        
        // Get output file
        String fileNameProposal = ((pw instanceof NameAndSignals)? (((NameAndSignals)pw).getName() + ".txt") : null);
        final File f = GUITools.showSaveFileChooser(IntegratorUI.instance, IntegratorUI.saveDir, fileNameProposal, SBFileFilter.createTSVFileFilter());
        if (f!=null) {
          IntegratorUI.saveDir = f.getParent();
          
          // Export all pathway details in a separate thread.
          Runnable r = new Runnable() {
            @Override
            public void run() {
              try {
                synchronized (geneList) {
                  String pwId; EnrichmentObject<?> pwo=null;
                  if (pw instanceof EnrichmentObject<?>) {
                    pwo = (EnrichmentObject<?>) pw;
                    pwId = pwo.getIdentifier().toString();
                  } else {
                    pwId = pw.toString();
                  }
                  if (pwId.startsWith("path:")) pwId = pwId.substring(5);
                  
                  // Export all data
                  boolean ret = exportPathway(f, pwId, pwo, data);
                  if (ret) {
                    GUITools.showMessage("Saved table successfully to \"" + f.getPath() + "\".", IntegratorUI.appName);
                  }
                }
              } catch (Throwable e) {
                GUITools.showErrorMessage(IntegratorUI.getInstance(), e);
              }
            }
          };
          IntegratorUITools.runInSwingWorker(r);
          // -----
          
        }
        
      }
      
    }
  }
  
  
  

  /**
   * Export the given pathway as tabular file containing a list with all genes.
   * 
   * @param outFile output file to be written
   * @param pwId pathway id to export (e.g., "hsa00130").
   * @param pwo optional parent {@link EnrichmentObject} (if applicable, may be null).
   * @return {@code TRUE} if all went fine.
   */
  public boolean exportPathway(File outFile, String pwId, EnrichmentObject<?> pwo) {
    return exportPathway(outFile, pwId, pwo, null);
  }
  
  /**
   * Export the given pathway as tabular file containing a list with all genes.
   * 
   * @param outFile output file to be written
   * @param pwId pathway id to export (e.g., "hsa00130").
   * @param pwo optional parent {@link EnrichmentObject} (if applicable, may be null).
   * @param signals also write all signals from these {@link NameAndSignals}.
   * @return {@code TRUE} if all went fine.
   */
  public boolean exportPathway(File outFile, String pwId, EnrichmentObject<?> pwo, Collection<? extends NameAndSignals> signals) {
    
    // XXX: This code is quite ineffective. It was enough for my purposes though...
    try {
      Species spec = getSpecies();
      KeggPathway2KEGGGeneIDs pw2g = new KeggPathway2KEGGGeneIDs(spec);
      
      // Required are IDs like "path:hsa00010". Ensure the prefix (which is usually omitted)
      if (!pwId.contains(":")) pwId = "path:" + pwId.trim();
      
      // Get all genes in the pathway (as KEGG Genes ID)
      Collection<String> geneInPW = pw2g.map(pwId);
      if (geneInPW==null) {
        GUITools.showErrorMessage(null, "Could not detect data source for pathway export.");
        return false;
      }
      pw2g = null; // Free resources
      
      // Prepare data
      Map<Integer, Collection<NameAndSignals>> dataMap = null;
      Collection<ValuePair<String, SignalType>> signalDescriptors = null;
      if (signals!=null && signals.size()>0) {
        dataMap = new HashMap<Integer, Collection<NameAndSignals>>(signals.size());
        for (NameAndSignals ns : signals) {
          if (ns instanceof GeneID) {
            Utils.addToMapOfSets(dataMap, new Integer(((GeneID) ns).getGeneID()), ns);
          }
        }
        
        NameAndSignals item = signals.iterator().next();
        signalDescriptors = item.getSignalNames();
        if (signalDescriptors==null) {
          signalDescriptors = Collections.emptyList();
        } else {
          // Sort
          signalDescriptors = Utils.collectionToList(signalDescriptors);
          Collections.sort((List<ValuePair<String, SignalType>>)signalDescriptors);
        }
      }
      
      // Create Comment
      StringBuffer buf = new StringBuffer();
      buf.append("Pathway '");
      buf.append(pwo.getName());
      buf.append("', " + pwId);
      buf.append(".\n");
      buf.append("Enrichment p-value: " + pwo.getSignalValue(SignalType.pValue, EnrichmentObject.signalNameForPvalues));
      buf.append('\n');
      buf.append("Pathway size: " + pwo.getTotalGenesInClass() + "; ");
      buf.append("Enriched genes: " + pwo.getNumberOfEnrichedGenesInClass());
      
      // Load additional mappers
      KeggGenesID2GeneID kgGenes2entrez = new KeggGenesID2GeneID(spec);
      GeneID2GeneSymbolMapper entrez2symbol = new GeneID2GeneSymbolMapper(spec.getCommonName());
      
      // Create Header
      List<String> header = new ArrayList<String>();
      header.add("KEGG_id");
      header.add("Entrez");
      header.add("Symbol");
      if (dataMap!=null) {
        header.add("Probe_id");
        for (ValuePair<String, SignalType> vp : signalDescriptors) {
          header.add(vp.getA()+"_"+vp.getB().toString());
        }
      }
      
      // Export each gene in pw with additional infos
      Collection<List<String>> table = new LinkedList<List<String>>();
      for (String kgID : geneInPW) {
        boolean rowAdded = false;
        List<String> row = new ArrayList<String>();
        row.add(kgID);
        
        // Add enrez ID and gene symbol
        Integer entrez = kgGenes2entrez.map(kgID);
        if (entrez==null || entrez<=0) {
          row.add("n/a"); //Entrez
          row.add("n/a"); //Symbol
          
          if (dataMap!=null) {
            row.add("n/a"); // Affy
            for (int i=0; i<signalDescriptors.size(); i++) {
              row.add("n/a"); // Signals
            }
          }
          
        } else {
          // we HAVE an entrez id and can write additional information
          row.add(entrez.toString());
          String symbol = entrez2symbol.map(entrez);
          if (symbol==null) {
            row.add("n/a");
          } else {
            row.add(symbol);
          }
          
          
          // Maybe export data
          if (dataMap!=null) {
            Collection<NameAndSignals> nses = dataMap.get(entrez);
            if (nses != null && nses.size()>0) {
              for(NameAndSignals ns : nses) {
                ArrayList<String> newRow = new ArrayList<String>(row);
                if (ns.getData(IdentifierType.Affymetrix.toString())!=null) {
                  newRow.add(ns.getData(IdentifierType.Affymetrix.toString()).toString());
                } else {
                  newRow.add("n/a");
                }
                
                // Add all signal values
                for (ValuePair<String, SignalType> desc : signalDescriptors) {
                  Number val = ns.getSignalValue(desc.getB(), desc.getA());
                  if (val == null || Double.isNaN(val.doubleValue())) {
                    newRow.add("n/a");
                  } else {
                    newRow.add(Double.toString(val.doubleValue()));
                  }
                }
                
                // Add a separate row for each probe (NameAndSignals)
                table.add(newRow);
                rowAdded = true;
              }
            } else {
              row.add("n/a"); // Affy
              for (int i=0; i<signalDescriptors.size(); i++) {
                row.add("n/a"); // Signals
              }
            }
          }
        }
        
        // If we DONT have signals, we still need to add the current row to our table
        if (!rowAdded) {
          table.add(row);
        }
      }
      
      // Write the table
      CSVWriter w = new CSVWriter();
      w.write(ArrayUtils.toArray(table), header.toArray(), buf.toString(), outFile);
      return true;
      
    } catch (Exception e) {
      e.printStackTrace();
      GUITools.showErrorMessage(null, e, "Could not export pathway details.");
    }
    return false;
  }

  /**
   * @return
   */
  public Species getSpecies() {
    return KEGGPathwayActionListener.getSpecies(this.source);
  }
  
}

/*
 * $Id: BioPAXImporter.java 169 2012-10-31 14:19:47Z wrzodek $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/SBVC/trunk/src/de/zbit/sbvc/io/BioPAXImporter.java $
 * ---------------------------------------------------------------------
 * This file is part of SBVC, the systems biology visualizer and
 * converter. This tools is able to read a plethora of systems biology
 * file formats and convert them to an internal data structure.
 * These files can then be visualized, either using a simple graph
 * (KEGG-style) or using the SBGN-PD layout and rendering constraints.
 * Some currently supported IO formats are SBML (+qual, +layout), KGML,
 * BioPAX, SBGN, etc. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/SBVC> to obtain the
 * latest version of SBVC.
 *
 * Copyright (C) 2012-2012 by the University of Tuebingen, Germany.
 *
 * SBVC is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.sbvc.io;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import de.zbit.biopax.BioPAXpathway;
import de.zbit.gui.GUITools;
import de.zbit.gui.JLabeledComponent;
import de.zbit.kegg.Translator;
import de.zbit.kegg.io.AbstractKEGGtranslator;
import de.zbit.kegg.io.BatchKEGGtranslator;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.kegg.parser.pathway.Pathway;
import de.zbit.util.NotifyingWorker;
import de.zbit.util.Species;

/**
 * A {@link SwingWorker} that handles imports of BioPAX files
 * and translations to destination formats.
 * 
 * @author Clemens Wrzodek
 * @version $Rev: 169 $
 */
public class BioPAXImporter extends NotifyingWorker<Object> {
  public static final transient Logger log = Logger.getLogger(BioPAXImporter.class.getName());

  
  /**
   * The original input file.
   */
  private File biopaxFile;
  
  /**
   * 
   */
  private Format outputFormat;
  
  
  /**
   * Imports the given <code>biopaxFile</code>.
   * @param biopaxFile
   */
  public BioPAXImporter(File biopaxFile, Format outputFormat) {
    super();
    this.biopaxFile = biopaxFile;
    this.outputFormat = outputFormat;
  }
  

  /* (non-Javadoc)
   * @see javax.swing.SwingWorker#doInBackground()
   */
  @Override
  protected Object doInBackground() throws Exception {
    try {
      // 1. Extract the model
      publish(new ActionEvent(this, 3, null));
      log.info(String.format("Reading BioPAX file '%s'...", biopaxFile.getName()));
      BioPAXpathway bp = new BioPAXpathway(biopaxFile);
      if (!bp.isSetModel()) {
        GUITools.showErrorMessage(null, "Could not read the model. Is it a valid BioPAX file?");
        return null;
      }
      List<String> pathwayList = bp.getListOfPathways();
      
      // 2. Eventually (if n>1) let the user pick a pathway.
      String pwName = null;
      if (pathwayList!=null && pathwayList.size()>1){
        // BioPAX files allow packing multiple pathways into one file => let the user choose one.
        JLabeledComponent pwSel = new JLabeledComponent(
          "Please select a pathway:", true, pathwayList);
        
        // Let user choose
        String title = biopaxFile!=null ? String.format("Select pathway from '%s'", biopaxFile.getName()) : System.getProperty("app.name");
        int button = JOptionPane.showOptionDialog(null, pwSel, title,
          JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
        
        // Return chosen class
        if (button!=JOptionPane.OK_OPTION) {
          publish(new ActionEvent(this, 2, null)); // Remove this tab
          publish(new ActionEvent(this, 5, null)); // Remove this from list of listeners
          return null;
        }
        pwName = pwSel.getSelectedItem().toString();
        
      } else if (pathwayList.size()==1) {
        pwName = pathwayList.iterator().next();
      }
      
      
      // 3. Convert to KGML
      Pathway keggPathway = bp.getKGMLpathway(pwName);
      
      // 4. Get species
      Species spec = BioPAXpathway.getSpecies(keggPathway, availableOrganisms());
      if (spec==null) {
        // Ask user
        spec = letUserSelectSpecies();
      }
      
      // 5. Ensure that we have entrez ids (try to map others, augments the KGML ids)
      if (spec!=null) {
        boolean containsEntrez = BioPAXpathway.checkForEntrezGeneIDs(keggPathway, spec, getProgressBar());
        if (!containsEntrez) {
          showWarningThatNoEntrezIDsAreInThePathway();
        }
      }
      
      // DEBUG output of KGML
      // TODO: Uncomment the next line
//      KGMLWriter.writeKGML(keggPathway, false); // Can be used for debugging
      
      // Reading done. Send some infos to the underlying listeners
      // Recommended name for this tab
      String name = (keggPathway.getTitle()!=null && keggPathway.getTitle().trim().length()>0) ? keggPathway.getTitle() : biopaxFile.getName();
      publish(new ActionEvent(this, 11, name));
      // Recommended ToolTip for this tab
      publish(new ActionEvent(this, 12, String.format("BioPAX pathway '%s' from file '%s'.", pwName, biopaxFile.getName())));
      // The species
      if (spec!=null) {
        publish(new ActionEvent(spec, 13, null));
      }
      
      
      // The order in which the following events happen is important
      AbstractKEGGtranslator<?> translator = (AbstractKEGGtranslator<?>) BatchKEGGtranslator.getTranslator(outputFormat, Translator.getManager());
      
      translator.setProgressBar(getProgressBar());
      if (translator instanceof KEGG2yGraph) {
        // BioPAX pathway visualization requires the inclusion of reactions.
        ((KEGG2yGraph)translator).setDrawArrowsForReactions(true);
      }
      Object result = translator.translate(keggPathway);
      
      publish(new ActionEvent(result, 4, null));
      return result;
      
    } catch (Exception e) {
      GUITools.showErrorMessage(null, e, "Could not import BioPAX pathway.");
      publish(new ActionEvent(this, 2, null)); // Remove this tab
      publish(new ActionEvent(this, 5, null)); // Remove this from list of listeners
      return null;
    }
  }
  
  /*
   * ALL FOLLOWING METHODS CAN BE OVERWRITTEN BY EXTENDING CLASSES.
   */
  
  /**
   * This is called if the pathway does not contain any entrez gene
   * ids and no entrez ids could be mapped from other ids.
   */
  protected void showWarningThatNoEntrezIDsAreInThePathway() {
    // We may show a warning here...
    return;
  }

  /**
   * Overwrite or implement this method to ask the
   * user for a species if none could be infered from
   * the selected file.
   * @return
   */
  protected Species letUserSelectSpecies() {
    //IntegratorUITools.showOrganismSelectorDialog(parent);
    return null;
  }

  /**
   * @return a list of all organisms that are available within your application.
   */
  protected List<Species> availableOrganisms() {
    try {
      return Species.generateSpeciesDataStructure();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }
}

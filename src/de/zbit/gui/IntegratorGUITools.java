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
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.zbit.gui.CSVImporterV2.CSVImporterV2;
import de.zbit.io.OpenFile;
import de.zbit.parser.Species;
import de.zbit.util.Utils;

/**
 * @author Clemens Wrzodek
 */

@SuppressWarnings("unchecked")
public class IntegratorGUITools {
  
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
  
}

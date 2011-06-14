/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.io.IOException;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.zbit.io.OpenFile;
import de.zbit.parser.Species;
import de.zbit.util.Utils;

/**
 * @author Clemens Wrzodek
 */

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
    return new JLabeledComponent("Please select your organism",true,organisms);
  }
  
}

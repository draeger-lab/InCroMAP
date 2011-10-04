/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui.prefs;

import java.awt.Component;
import java.io.IOException;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import de.zbit.data.Signal.MergeType;
import de.zbit.gui.ActionCommand;
import de.zbit.gui.ActionCommandFactory;
import de.zbit.gui.GUITools;

/**
 * Enables an option tab for the {@link MergeTypeOptions}.
 * @author Clemens Wrzodek
 */
public class SignalOptionPanel extends PreferencesPanelForKeyProvider  {
  private static final long serialVersionUID = 19866295658231672L;

  /**
   * @param provider
   * @throws IOException
   */
  public SignalOptionPanel() throws IOException {
    super(SignalOptions.class);
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.gui.prefs.PreferencesPanelForKeyProvider#init()
   */
  @Override
  public void init() {
    super.init();
    performCommonSignalOptionPanelModifications(this);
  }


  /**
   * Removes the {@link MergeType#AskUser} and adds a few tooltips.
   */
  public static void performCommonSignalOptionPanelModifications(JComponent SigOptionalPanel) {
    removeItemFromJComboBox(SigOptionalPanel, MergeType.AskUser);
    addToolTipToJComboBox(SigOptionalPanel, MergeType.Automatic, "Automatically selectes MaxDistanceToZero for fold changes, minimum for p-values and mean for all others.");
    addToolTipToJComboBox(SigOptionalPanel, MergeType.NormalizedSumOfLog2Values, "This calculation is especially suitable for p-values from DNA methylation data.");
  }


  /**
   * Adds a <code>tooltip</code> for a specific Element <code>comboBoxElement</code>
   * to all {@link JComboBox}es on <code>sigOptionalPanel</code> that
   * contain this element.
   * @param sigOptionalPanel
   * @param comboBoxElement
   * @param tooltip
   * @return true if at least one item has been modified.
   */
  public static boolean addToolTipToJComboBox(JComponent sigOptionalPanel,
    final Object comboBoxElement, final String tooltip) {
    
    if (sigOptionalPanel instanceof JComboBox) {
      ComboBoxModel model = ((JComboBox)sigOptionalPanel).getModel();
      boolean ret = false;
      
      // Create an actionCommand that contains the selected tooltip
      ActionCommand newObject = ActionCommandFactory.create(comboBoxElement.toString(), tooltip);
      
      // Look for the old element and replace it.
      int sel = ((JComboBox)sigOptionalPanel).getSelectedIndex(); //backup
      for (int i=0; i<model.getSize(); i++) {
        if (model.getElementAt(i).equals(comboBoxElement)) {
          ret = true;
          ((JComboBox)sigOptionalPanel).removeItemAt(i);
          ((JComboBox)sigOptionalPanel).insertItemAt(newObject, i);
        }
      }
      if (ret) ((JComboBox)sigOptionalPanel).setSelectedIndex(sel); // restore selection
      
      return ret;
    } else if (sigOptionalPanel.getComponentCount()>0) {
      boolean ret = false;
      for (Component c2: sigOptionalPanel.getComponents()) {
        if (c2 instanceof JComponent) {
          ret |= addToolTipToJComboBox((JComponent)c2, comboBoxElement, tooltip);
        }
      }
      return ret;
    } else {
      return false;
    }
  }


  /**
   * Remove any item from any {@link JComboBox} that is on the given
   * component or any sub-component of this component.
   * @param c
   * @param toRemove
   * @return true if and only if at least one item has been removed
   */
  public static boolean removeItemFromJComboBox(JComponent c, Object toRemove) {
    
    if (c instanceof JComboBox) {
      int oldSize = ((JComboBox)c).getItemCount();
      ((JComboBox)c).removeItem(toRemove);
      //System.out.println("Removing " + (((JComboBox)c).getItemCount()<oldSize));
      return (((JComboBox)c).getItemCount()<oldSize);
    } else if (c.getComponentCount()>0) {
      boolean ret = false;
      for (Component c2: c.getComponents()) {
        if (c2 instanceof JComponent) {
          ret |= removeItemFromJComboBox((JComponent)c2, toRemove);
        }
      }
      return ret;
    } else {
      return false;
    }
    
  }
  
  /**
   * Removes the {@link JComponent}, corresponding to
   * {@link MergeTypeOptions#REMEMBER_GENE_CENTER_DECISION}
   * from this panel.
   */
  public void removeRememberSelectionCheckBox() {
    GUITools.removeAllComponentsWithName(this, MergeTypeOptions.REMEMBER_GENE_CENTER_DECISION.getOptionName());
  }
  
  
  
  
  
//  
//
//  /*
//   * (non-Javadoc)
//   * @see de.zbit.gui.prefs.PreferencesPanel#accepts(java.lang.Object)
//   */
//  @Override
//  public boolean accepts(Object key) {
//    try {
//      System.out.println(provider.getField(key.toString()));
//    } catch (SecurityException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    } catch (NoSuchFieldException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
//    return preferences.keySetFull().contains(key);
//  }
//  
//  /*
//   * (non-Javadoc)
//   * @see de.zbit.gui.prefs.PreferencesPanel#loadPreferences()
//   */
//  @Override
//  protected SBPreferences loadPreferences() throws IOException {
//    System.out.println(provider == null ? "null" : SBPreferences.getPreferencesFor(provider));
//    return provider == null ? null : SBPreferences.getPreferencesFor(provider);
//  }
  
  
}

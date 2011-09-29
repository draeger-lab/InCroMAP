/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui.prefs;

import java.io.IOException;

import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import de.zbit.gui.JLabeledComponent;
import de.zbit.gui.customcomponents.NodeShapeSelector;

/**
 * Enable an option tab for the {@link PathwayVisualizationOptions}.
 * @author Clemens Wrzodek
 */
public class PathwayVisualizationOptionPanel extends PreferencesPanelForKeyProviders {
  private static final long serialVersionUID = 4574493372860651243L;

  /**
   * @param provider
   * @throws IOException
   */
  public PathwayVisualizationOptionPanel()
    throws IOException {
    super("Observation and visualization options", SignalOptions.class, PathwayVisualizationOptions.class);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.prefs.PreferencesPanelForKeyProvider#init()
   */
  @Override
  public void init() {
    super.init();
    if (getComponentCount()>0) {
      changeFCSpinnerStepsize(this);
      changeNodeShapeSelector();
    }
  }

  /**
   * 
   */
  private void changeNodeShapeSelector() {
    Object c = option2component.get(PathwayVisualizationOptions.CHANGE_NODE_SHAPE);
    if (c!=null && c instanceof JLabeledComponent) {
      Object c2 = ((JLabeledComponent)c).getColumnChooser();
      if (c2 instanceof JComboBox) {
        ((JComboBox)c2).setRenderer(new NodeShapeSelector());
      } else {
        // Huh? should be a ComboBox!
        System.err.println("Could not change renderer of node shape selector!");
      }
    }
  }

  /**
   * Tries to change the step size of the fold change spinner to 0.1
   */
  public static void changeFCSpinnerStepsize(PreferencesPanel panel) {
    Object c = panel.getComponentForOption(PathwayVisualizationOptions.FOLD_CHANGE_FOR_MAXIMUM_COLOR);
    if (c!=null && c instanceof JLabeledComponent) {
      Object c2 = ((JLabeledComponent)c).getColumnChooser();
      if (c2 instanceof JSpinner) {
        SpinnerModel model = ((JSpinner)c2).getModel();
        if (model instanceof SpinnerNumberModel) {
          ((SpinnerNumberModel)model).setStepSize(.1f);
        }
      }
    }
  }
  
}

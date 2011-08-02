/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import de.zbit.gui.prefs.PathwayVisualizationOptionPanel;
import de.zbit.gui.prefs.SignalOptionPanel;


/**
 * @author Clemens Wrzodek
 */
public class VisualizeDataInPathwayDialog extends JPanel {
  
  /*
   * TODO: Create a dialog, asking for
   * 1. if user wants to merge data gene-centered (default: true)
   *   1.1 MergeType
   * 2. Colors and maxFC
   * 3. NodeShape
   * 
   * See IntegratorGUITools.getMergeType() for how to start writing this class ;-) 
   */
  
  protected int buttonPressed;


  public VisualizeDataInPathwayDialog() {
    super ();
    try {
      init(new LayoutHelper(this));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  


  
  
  public void init(LayoutHelper lh) throws IOException {
    /* 1 one value per node (pathway centered)
     * 2 one value per gene (gene centered)
     * 3 one node per value (probe centered - not recommended)
     * - MergeType bei 1 und 2
     * 
     * o Colors und threshold
     * 
     * o Change Shape? ONLY IF YES, Shape
     * 
     */
    
    JRadioButton[] depth = new JRadioButton[3];
    depth[0] = new JRadioButton("One value per pathway node (pathway centered)");
    depth[1] = new JRadioButton("One value per gene (gene centered)", true);
    depth[2] = new JRadioButton("One node per value (probe centered - not recommended)");
    
    final SignalOptionPanel sop = new SignalOptionPanel();
    sop.removeRememberSelectionCheckBox();
    
    ButtonGroup mergeDepth = new ButtonGroup();
    for (int i=0; i<depth.length; i++) {
      mergeDepth.add(depth[i]);
      lh.add(depth[i]);
      
      final int final_i = i;
      depth[i].addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
          sop.setEnabled(final_i>=1);
        }
      });
    }
    lh.add(sop);
    
    PathwayVisualizationOptionPanel colors = new PathwayVisualizationOptionPanel();
    lh.add(colors);
    
    
    
    JComboBox box = new NodeShapeSelector();
    lh.add(box);
    
    

  }
  
  public static boolean showDialog(final VisualizeDataInPathwayDialog c) {

    // Initialize the dialog
    final JDialog jd;
//    if (parent!=null && parent instanceof Frame) {
//      jd = new JDialog((Frame)parent, title, true);
//    } else if (parent!=null && parent instanceof Dialog) {
//      jd = new JDialog((Dialog)parent, title, true);
//    } else {
      jd = new JDialog();
//      jd.setTitle(title);
      jd.setModal(true);
//    }
    
    // Initialize the panel
//    c.setName(title);
    jd.add(c);
    // Close dialog with ESC button.
    jd.getRootPane().registerKeyboardAction(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        c.buttonPressed = JOptionPane.CANCEL_OPTION;
        jd.dispose();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    // Close dialog with ENTER button.
    jd.getRootPane().registerKeyboardAction(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
          c.buttonPressed = JOptionPane.OK_OPTION;
          jd.dispose();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    
    // Set close operations
    jd.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        jd.setVisible(false);
      }
    });
    c.addComponentListener(new ComponentListener() {
      public void componentHidden(ComponentEvent e) {
        jd.setVisible(false);
      }
      public void componentMoved(ComponentEvent e) {}
      public void componentResized(ComponentEvent e) {}
      public void componentShown(ComponentEvent e) {}
    });
    
    // Set size
    jd.setPreferredSize(c.getPreferredSize());
    jd.setSize(c.getPreferredSize());
    //jd.pack();
//    jd.setLocationRelativeTo(parent);
    
    // Set visible and wait until invisible
    jd.pack();
    jd.setVisible(true);
    
    
    // Dispose and return if dialog has been confirmed.
    jd.dispose();
    
    return (c.getButtonPressed()==JOptionPane.OK_OPTION);
  }
  
  /**
   * @return
   */
  private int getButtonPressed() {
    return buttonPressed;
  }

  /**
   * Just for DEMO and testing purposes.
   */
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      JFrame parent = new JFrame();
      parent.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      VisualizeDataInPathwayDialog dialog = new VisualizeDataInPathwayDialog();
      showDialog(dialog);

      System.exit(0);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
    
}

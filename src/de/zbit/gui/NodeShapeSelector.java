/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;

import y.view.GenericNodeRealizer;
import y.view.ShapeNodePainter;

/**
 * A special {@link JComboBox} for the selection of yFiles
 * node shapes.
 * @author Clemens Wrzodek
 */
public class NodeShapeSelector extends JComboBox implements ListCellRenderer {
  private static final long serialVersionUID = -6888199259419237517L;
  
  
  public NodeShapeSelector() {
    this (new Byte[]{1,2,3,4,5,6,7,8,9,10});
  }
  
  private NodeShapeSelector(ComboBoxModel aModel) {
    super (aModel);
    setRenderer(this);
  }
  
  public NodeShapeSelector(Byte[] allowedShapes) {
    this (new DefaultComboBoxModel(allowedShapes));
  }
  
  
  
  /* (non-Javadoc)
   * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
   */
  @Override
  public Component getListCellRendererComponent(JList list, Object value,
    int index, final boolean isSelected, final boolean cellHasFocus) {
    
    final int HEIGHT_PREVIEW = Math.max(getHeight()-8, 10);
    final int WIDTH_PREVIEW = Math.min(getWidth(), HEIGHT_PREVIEW*2);
    
    // Renderer for Byte values, corresponding to node shapes.
    Byte b = (Byte) value;
    
    final ShapeNodePainter painter = new ShapeNodePainter(b);
    final GenericNodeRealizer preview = new GenericNodeRealizer();
    preview.setSize(WIDTH_PREVIEW-4, HEIGHT_PREVIEW-4);
    preview.setCenter(WIDTH_PREVIEW/2, HEIGHT_PREVIEW/2);
    
    Color color2 = (isSelected || cellHasFocus)?UIManager.getColor("ComboBox.selectionForeground"):
      UIManager.getColor("ComboBox.foreground");
    Color fallBack = new Color(212, 228, 254);
    if (color2==null) color2 = fallBack;
    
    preview.setFillColor(color2.equals(Color.WHITE)?fallBack:Color.WHITE);
    preview.setFillColor2(color2);
    
    
    //preview.setSelected(isSelected);
    
    JComponent ret = new JComponent() {
      private static final long serialVersionUID = -2225874537253669176L;
      @Override
      protected void paintComponent(Graphics g) {
        BufferedImage image = new BufferedImage(WIDTH_PREVIEW, HEIGHT_PREVIEW, BufferedImage.TYPE_INT_ARGB);
        
        // Get graphics and fill with default BGcolor
        Graphics2D g2d = (Graphics2D) image.getGraphics();
        Color bgColor = (isSelected || cellHasFocus)?UIManager.getColor("ComboBox.selectionBackground"):
          UIManager.getColor("ComboBox.background");
        if (bgColor==null) bgColor = Color.WHITE;
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
        
        painter.paint(preview, (Graphics2D) image.getGraphics());
        g.drawImage(image, 0, 0, null);
      }
    };
    
    ret.setSize(WIDTH_PREVIEW, HEIGHT_PREVIEW);
    ret.setPreferredSize(ret.getSize());
    
    return ret;
  }
  

  
}

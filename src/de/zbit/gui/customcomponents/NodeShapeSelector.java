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
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.gui.customcomponents;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
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
public class NodeShapeSelector extends JComboBox implements ListCellRenderer {
  private static final long serialVersionUID = -6888199259419237517L;
  
  public final static Byte[] validChoices = new Byte[]{0, 1,2,3,4,5,6,7,8,9,10};
  
  /**
   * Any default renderer for {@link String}s.
   */
  private ListCellRenderer defaultListRenderer = null;
  
  /**
   * Text that is displayed for all values <=0.
   */
  public static String NO_SELECTION_TEXT = "Keep default shape";
  
  public NodeShapeSelector() {
    this (validChoices);
  }
  
  private NodeShapeSelector(ComboBoxModel aModel) {
    super (aModel);
    setRenderer(this);
  }
  
  public NodeShapeSelector(Byte[] allowedShapes) {
    this (new DefaultComboBoxModel(allowedShapes));
  }
  
  private ListCellRenderer getDefaultRenderer() {
    /*
     * Get the systems default renderer.
     */
    if (defaultListRenderer==null) {
      // new DefaultListCellRenderer(); Is not necessarily the default!
      // even UIManager.get("List.cellRenderer"); returns a different value!
      try {
        defaultListRenderer = new JComboBox().getRenderer();
        if (defaultListRenderer==null) {
          defaultListRenderer = (ListCellRenderer) UIManager.get("List.cellRenderer");
        }
      } catch (Throwable t){t.printStackTrace();}
      if (defaultListRenderer==null) {
        defaultListRenderer = new DefaultListCellRenderer();;
      }
    }
    //-------------------
    return defaultListRenderer;
  }
  
  
  /* (non-Javadoc)
   * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
   */
  @Override
  public Component getListCellRendererComponent(JList list, Object value,
    int index, final boolean isSelected, final boolean cellHasFocus) {
    int minimumSize = 15;
    final int HEIGHT_PREVIEW = Math.max(getHeight()-8, minimumSize);
    final int WIDTH_PREVIEW = Math.min(Math.max(getWidth(), minimumSize*2), HEIGHT_PREVIEW*2);
    
    // Renderer for Byte values, corresponding to node shapes.
    Byte b = (Byte) value;
    if (b<1) {
      return getDefaultRenderer().getListCellRendererComponent(list, NO_SELECTION_TEXT, index, isSelected, cellHasFocus);
    }
    
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

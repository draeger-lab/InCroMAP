/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/Integrator> to
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
package de.zbit.graph;

import java.awt.Paint;
import java.util.List;

import org.jfree.chart.labels.IntervalXYItemLabelGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.XYDataset;

import de.zbit.data.NameAndSignals;
import de.zbit.data.genes.GenericGene;

/**
 * A special extention from {@link XYBarRenderer} to render
 * Genes.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class XYGenesBarRenderer extends XYBarRenderer {
  private static final long serialVersionUID = -1334050070174676592L;

  /**
   * Key to use for storing color information in
   * {@link NameAndSignals#addData(String, Object)}.
   */
  public final static String COLOR_KEY = "COLOR";
  
  /**
   * 
   */
  List<GenericGene> genesInRegion;
  
  /**
   * @param genesInRegion in the SAME ORDER as they appear
   * in the current series!
   */
  public XYGenesBarRenderer(List<GenericGene> genesInRegion) {
    super();
    init(genesInRegion);
  }
  
  /**
   * 
   * @param margin
   * @param genesInRegion in the SAME ORDER as they appear
   * in the current series!
   */
  public XYGenesBarRenderer(double margin, List<GenericGene> genesInRegion) {
    super(margin);
    init(genesInRegion);
  }
  
  /**
   * Set default values.
   * @param genesInRegion
   */
  protected void init(List<GenericGene> genesInRegion) {
    setGenesInRegion(genesInRegion);
    setUseYInterval(true);
    createItemLabelsAndToolTips();
  }

  /**
   * @return the genesInRegion
   */
  public List<GenericGene> getGenesInRegion() {
    return genesInRegion;
  }

  /**
   * @param genesInRegion the genesInRegion to set
   */
  public void setGenesInRegion(List<GenericGene> genesInRegion) {
    this.genesInRegion = genesInRegion;
  }
  
  
  
  /* (non-Javadoc)
   * @see org.jfree.chart.renderer.AbstractRenderer#getItemPaint(int, int)
   */
  @Override
  public Paint getItemPaint(int row, int column) {
    // Check if there is set a default color on this gene.
    Object color = genesInRegion.get(column).getData(COLOR_KEY);
    if (color instanceof java.awt.Color) {
      return (java.awt.Color)color;
    } else {
      return super.getItemPaint(row, column);
    }
  }
  
  /**
   * Set the gene name as label and description as tooltip
   */
  private void createItemLabelsAndToolTips() {
    setBaseItemLabelGenerator(new IntervalXYItemLabelGenerator(){
      private static final long serialVersionUID = 1L;
      /* (non-Javadoc)
       * @see org.jfree.chart.labels.AbstractXYItemLabelGenerator#generateLabelString(org.jfree.data.xy.XYDataset, int, int)
       */
      @Override
      public String generateLabelString(XYDataset dataset, int series, int item) {
        return genesInRegion.get(item).getName();
      }
    });
    setBaseToolTipGenerator(new XYToolTipGenerator() {
      @Override
      public String generateToolTip(XYDataset dataset, int series, int item) {
        return genesInRegion.get(item).getDescription();
      }
    });
    setBaseItemLabelsVisible(true);
    
    //localXYBarRenderer.setBaseItemLabelFont(new Font("Dialog", Font.BOLD, 14));
    /*
     * Wenn start sichtbar
     *   Wenn Ende sichtbar, dann center
     *   sonst links
     * Else Wenn Ende sichtbar
     *   dann rechts
     * else
     *   CENTER //Hopefully... 
     */
    
    /* Try to place labels on genes as visible as possible.
     * Default is centered, change if only one ranges into visible area */
//    ItemLabelPosition pos = new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.CENTER);
//    if (genesInRegion.size()==1) {
//      Region r = genesInRegion.get(0);
//      if (r.getStart() >= target.getStart() && r.getEnd()<=target.getEnd()) {
//        // Kepp default centering
//      } else {
//        // is going out of range somewhere
//        
//        //Unfortunately this is relative to the middle, so
//        // right means "right of middle", but not "on right side" :-(
////        int middle = r.getMiddle();
////        if (middle<target.getMiddle()) {
////          pos =  new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.CENTER_RIGHT);
////        } else {
////          pos =  new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.CENTER_LEFT);
////        }
//      }
//    }
//    localXYBarRenderer.setBaseNegativeItemLabelPosition(pos);
//    localXYBarRenderer.setBasePositiveItemLabelPosition(pos);
    
  }
  
  
  
}

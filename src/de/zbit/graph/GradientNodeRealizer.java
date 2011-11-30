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

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import y.view.NodeRealizer;

/**
 * <b<This is not yet implemented.</b> Idea is to have multiple colors,
 * sort them (e.g., from red tones, to white tones to blue tones)
 * and use this gradient as background for a simple rectangle node.
 * 
 * <P>XXX:THIS CLASS IS CURRENTLY UNUSED!
 * 
 * <p><i>Note:<br/>
 * Due to yFiles license requirements, we have to obfuscate this class
 * in the JAR release of this application. Thus, this class
 * can not be found by using the class name.<br/> If you can provide us
 * with a proof of possessing a yFiles license yourself, we can send you
 * an unobfuscated release of Integrator.</i></p>
 * 
 * XXX: Isn't this supported by default?
 * Also, consider using {@link GradientPaint}.
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class GradientNodeRealizer extends NodeRealizer {
  
  List<Color> colors = new ArrayList<Color>();
  
  public void addFillColor(Color arg0) {
    colors.add(arg0);
  }
  
  
  /* (non-Javadoc)
   * @see y.view.NodeRealizer#setFillColor(java.awt.Color)
   */
  @Override
  public void setFillColor(Color arg0) {
    colors.add(arg0);
  }
  
  /* (non-Javadoc)
   * @see y.view.NodeRealizer#setFillColor2(java.awt.Color)
   */
  @Override
  public void setFillColor2(Color arg0) {
    colors.add(arg0);
  }

  /* (non-Javadoc)
   * @see y.view.NodeRealizer#createCopy(y.view.NodeRealizer)
   */
  @Override
  public NodeRealizer createCopy(NodeRealizer arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see y.view.NodeRealizer#paintNode(java.awt.Graphics2D)
   */
  @Override
  protected void paintNode(Graphics2D arg0) {
    // TODO Auto-generated method stub
    
  }


  
}

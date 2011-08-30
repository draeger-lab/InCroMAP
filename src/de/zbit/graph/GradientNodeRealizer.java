/**
 * @author Clemens Wrzodek
 */
package de.zbit.graph;

import java.awt.Color;
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
 * @author Clemens Wrzodek
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

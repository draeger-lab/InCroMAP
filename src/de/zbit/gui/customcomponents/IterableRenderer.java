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
package de.zbit.gui.customcomponents;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.table.DefaultTableCellRenderer;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.miRNA.miRNAandTarget;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUITools;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.util.Species;
import de.zbit.util.StringUtil;

/**
 * A renderer that shows instances of {@link Iterable} as comma
 * separated string.
 * <p>It is used especialy for the GeneList in an {@link EnrichmentObject}. 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class IterableRenderer extends DefaultTableCellRenderer {
  private static final long serialVersionUID = -3236639799789752757L;
  public static final transient Logger log = Logger.getLogger(IterableRenderer.class.getName());
  
  /**
   * To convert geneIDs to gene symbols.
   */
  Species species = null;
  
  public IterableRenderer() {
    super();
  }
  
  /**
   * @param spec
   */
  public IterableRenderer(Species spec) {
    this();
    species = spec;
  }

  /* (non-Javadoc)
   * @see javax.swing.table.DefaultTableCellRenderer#setValue(java.lang.Object)
   */
  @Override
  protected void setValue(Object value) {
    GeneID2GeneSymbolMapper mapper = IntegratorUITools.get2GeneSymbolMapping(species);
    String initialString = "<html><body><nobr>";
    String space = "&nbsp;";
    if (value==null) setText("");
    else {
      if (value instanceof Iterable || Iterable.class.isAssignableFrom(value.getClass())) {
        StringBuffer buff = getCommaStringFromIterable(value, mapper, initialString, space);
        buff.append("</nobr></body></html>");
        setText(buff.toString());
      } else {
        setText(value.toString());
      }
    }
  }

  /**
   * Converts a list to a single comma separated string of elements.
   * @param value the actual list to convert
   * @param mapper a GeneID2GeneSymbolMapper, may be null
   * @param initialString start string to append at the beginning of StringBuffer
   * @param space the string to use to mimic a space between to elements (a comma
   * will automatically be prepended)
   * @return
   */
  public static StringBuffer getCommaStringFromIterable(Object value,
      GeneID2GeneSymbolMapper mapper, String initialString, String space) {
    StringBuffer buff = new StringBuffer(initialString);
    Map<String, List<miRNAtarget>> miRNAandTarget = new HashMap<String, List<miRNAtarget>>();
    
    // Create comma separated list of values.
    for (Object v:((Iterable<?>)value)) {
      
      if (v instanceof miRNAandTarget) {
        // Will be set later...
        
        // Create a map of miRNA and targets in the pathway.
        List<miRNAtarget> l = miRNAandTarget.get(((miRNAandTarget)v).getName());
        if (l==null) {
          l = new LinkedList<miRNAtarget>();
          miRNAandTarget.put(((miRNAandTarget)v).getName(), l);
        }
        l.add(((miRNAandTarget)v).getTarget());
      } else {
        // Will be set now
        if (buff.length()>initialString.length()) buff.append(","+ space);
        
        if (v instanceof NameAndSignals || NameAndSignals.class.isAssignableFrom(v.getClass())) {
          buff.append(((NameAndSignals)v).getName());
          
        } else if (v instanceof miRNAtarget) {
          buff.append(((miRNAtarget)v).getNiceTargetString());
          
        } else if (v instanceof Integer) {
          // Gene IDs
          String symbol = toSymbol((Integer) v,mapper);
          buff.append(symbol);
          
        } else if (v.getClass().equals(String.class)) {
          buff.append((String)v);
          
        } else {
          log.severe("Plese implement renderer for " + v.getClass());
          buff.append(v.toString());
        }
      }
    }
    
    // Process miRNA and targets.
    for (String miR: miRNAandTarget.keySet()) {
      if (buff.length()>initialString.length()) buff.append(";" + space);
      buff.append("<b>" + miR + ":</b>"+space);
      List<miRNAtarget> l = miRNAandTarget.get(miR);
      for (int i=0; i<l.size(); i++) {
        if (i>0) buff.append(","+space);
        String symbol = toSymbol((Integer) l.get(i).getTarget(),mapper);
        buff.append(symbol);
      }
    }
    return buff;
  }
  
  
  /**
   * Try to convert the given GeneID to a Gene Symbol.
   * @param GeneID
   * @param mapper
   * @return
   */
  public static String toSymbol(int GeneID, GeneID2GeneSymbolMapper mapper) {
    if (mapper==null) return Integer.toString(GeneID);
    String s=null;
    try {
      s = mapper.map(GeneID);
    } catch (Exception e) {}
    return (s!=null && s.length()>0)?s:Integer.toString(GeneID);
  }
  
  /* (non-Javadoc)
   * @see javax.swing.JLabel#setText(java.lang.String)
   */
  @Override
  public void setText(String text) {
    super.setText(text);
    setToolTipText(StringUtil.insertLineBreaksAndCount(text.replace("&nbsp;", " "), 
      StringUtil.TOOLTIP_LINE_LENGTH+20, text.startsWith("<html>")?"<br/>":"\n").getA());
  }
}

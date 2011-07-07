/**
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.table.DefaultTableCellRenderer;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.parser.Species;
import de.zbit.util.StringUtil;

/**
 * A renderer that shows instances of {@link Iterable} as comma
 * separated string.
 * <p>It is used especialy for the GeneList in an {@link EnrichmentObject}. 
 * @author Clemens Wrzodek
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
    GeneID2GeneSymbolMapper mapper = IntegratorGUITools.get2GeneSymbolMapping(species);
    String initialString = "<html><body><nobr>";
    String space = "&nbsp;";
    if (value==null) setText("");
    else {
      if (value instanceof Iterable || Iterable.class.isAssignableFrom(value.getClass())) {
        StringBuffer buff = new StringBuffer(initialString);
        Map<String, List<miRNAtarget>> miRNAandTarget = new HashMap<String, List<miRNAtarget>>();
        
        // Create comma separated list of values.
        for (Object v:((Iterable<?>)value)) {
          if (buff.length()>initialString.length()) buff.append(","+ space);
          
          if (v instanceof miRNAandTarget) {
            // Create a map of miRNA and targets in the pathway.
            List<miRNAtarget> l = miRNAandTarget.get(((miRNAandTarget)v).getName());
            if (l==null) {
              l = new LinkedList<miRNAtarget>();
              miRNAandTarget.put(((miRNAandTarget)v).getName(), l);
            }
            l.add(((miRNAandTarget)v).getTarget());
            
          } else if (v instanceof NameAndSignals || NameAndSignals.class.isAssignableFrom(v.getClass())) {
            buff.append(((NameAndSignals)v).getName());
            
          } else if (v instanceof Integer) {
            // Gene IDs
            String symbol = toSymbol((Integer) v,mapper);
            buff.append(symbol);
            
          } else {
            log.severe("Plese implement renderer for " + v.getClass());
            buff.append(v.toString());
            
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
        
        buff.append("</nobr></body></html>");
        setText(buff.toString());
      } else {
        setText(value.toString());
      }
    }
  }
  
  
  /**
   * Try to convert the given GeneID to a Gene Symbol.
   * @param GeneID
   * @param mapper
   * @return
   */
  public String toSymbol(int GeneID, GeneID2GeneSymbolMapper mapper) {
    if (species==null || mapper==null) return Integer.toString(GeneID);
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
      GUITools.TOOLTIP_LINE_LENGTH+20, text.startsWith("<html>")?"<br/>":"\n").getA());
  }
}

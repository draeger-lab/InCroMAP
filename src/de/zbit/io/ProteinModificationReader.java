/**
 * @author Clemens Wrzodek
 */
package de.zbit.io;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import de.zbit.data.GeneID;
import de.zbit.data.protein.ProteinModificationExpression;
import de.zbit.gui.csv.ExpectedColumn;

/**
 * A class to read {@link ProteinModificationExpression} data.
 * 
 * @author Clemens Wrzodek
 */
public class ProteinModificationReader extends AbstractGeneBasedNSreader<ProteinModificationExpression> {
  public static final transient Logger log = Logger.getLogger(ProteinModificationReader.class.getName());
  
  int analyteShortNameCol=-1;
  int modificationCol=-1;

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#getAdditionalExpectedColumns()
   */
  @Override
  protected List<ExpectedColumn> getAdditionalExpectedColumns() {
    List<ExpectedColumn> list = new ArrayList<ExpectedColumn>();
    
    list.add(new ExpectedColumn("Analyte short name",false));
    list.add(new ExpectedColumn("Modification name",false));
    
    return list;
  }

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#processAdditionalExpectedColumns(java.util.List)
   */
  @Override
  protected void processAdditionalExpectedColumns(List<ExpectedColumn> additional) {
    if (additional.get(0).hasAssignedColumns()) {
      analyteShortNameCol = additional.get(0).getAssignedColumn();
    }
    if (additional.get(1).hasAssignedColumns()) {
      modificationCol = additional.get(1).getAssignedColumn();
    }
  }

  /* (non-Javadoc)
   * @see de.zbit.io.AbstractGeneBasedNSreader#createObject(java.lang.String, java.lang.Integer, java.lang.String[])
   */
  @Override
  protected ProteinModificationExpression createObject(String name, Integer geneID, String[] line) {
    
    String analyteShortName = null;
    if (analyteShortNameCol>=0 && analyteShortNameCol<line.length) analyteShortName = line[analyteShortNameCol];
    
    String modification = null;
    if (modificationCol>=0 && modificationCol<line.length) modification = line[modificationCol];
    
    if (geneID==null) geneID = GeneID.default_geneID;
    return new ProteinModificationExpression(name, analyteShortName, modification, geneID);
  }

  
}

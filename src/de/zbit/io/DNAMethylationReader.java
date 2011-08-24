/**
 * @author Clemens Wrzodek
 */
package de.zbit.io;

import java.awt.Component;
import java.util.Collection;

import de.zbit.data.methylation.DNAmethylation;
import de.zbit.integrator.ReaderCache;
import de.zbit.parser.Species;


/**
 * TODO: Implement me.
 * @author Clemens Wrzodek
 */
public class DNAMethylationReader extends NameAndSignalReader<DNAmethylation> {

  /**
   * @param nameCol
   */
  public DNAMethylationReader(int nameCol) {
    super(nameCol);
    // TODO Auto-generated constructor stub
  }

  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#importWithGUI(java.awt.Component, java.lang.String, de.zbit.integrator.ReaderCache)
   */
  @Override
  public Collection<DNAmethylation> importWithGUI(Component parent,
    String file, ReaderCache cache) {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#createObject(java.lang.String, java.lang.String[])
   */
  @Override
  protected DNAmethylation createObject(String name, String[] line)
    throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see de.zbit.io.NameAndSignalReader#getSpecies()
   */
  @Override
  public Species getSpecies() {
    // TODO Auto-generated method stub
    return null;
  }
}

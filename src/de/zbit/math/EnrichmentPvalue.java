/**
 * 
 * @author Clemens Wrzodek
 */
package de.zbit.math;

/**
 * This is a generic interface for pValue calculations, based on four values:
 * <ol><li>Genome Size
 * <li>Gene List size
 * <li>Total Genes in the current enrichment class (e.g., pathway)
 * <li>Number of Genes from the input gene list, that are in the current
 * enrichment class (e.g., pathway)</ol>
 * <b>The first two are fixed for each instance and thus, should be declared in the
 * constructor</b>, whereas the later two are given to the method {@link #getPvalue(long, long)}.
 * 
 * @author Clemens Wrzodek
 */
public interface EnrichmentPvalue {
  
  /**
   * @return size of the background genome
   */
  public int getGenomeSize();
  
  /**
   * @return size of the input gene list
   */
  public int getGeneListSize();
  
  /**
   * Calculates a pValue for an enrichment significance (e.g., gene set enrichments
   * in pathways).
   * @param t Total number of genes in the current pathway
   * @param r Number of genes from the input set that are in the current pathway.
   * @return
   */
  public double getPvalue(int t, int r);
  
}
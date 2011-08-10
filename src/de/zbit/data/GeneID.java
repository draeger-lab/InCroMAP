/**
 * @author Clemens Wrzodek
 */
package de.zbit.data;

/**
 * Interface for object that can be assigned a NCBI Entrez Gene ID.
 * @author Clemens Wrzodek
 */
public interface GeneID {
  
  /**
   * The key to use in the {@link #addData(String, Object)} map to add
   * the corresponding NCBI Gene ID (Entrez).
   */
  public final static String gene_id_key = "Gene_ID";
  
  /**
   * Means gene id has not been set or mRNA has no associated gene id.
   */
  public final static Integer default_geneID = -1;
  
  /**
   * Set the corresponding NCBI Gene ID.
   * @param geneID
   */
  public void setGeneID(int geneID);
  
  /**
   * @return associated NCBI Gene ID.
   */
  public int getGeneID();
  
}
package de.zbit.data.id;

import de.zbit.mapper.MappingUtils.IdentifierClass;
import de.zbit.mapper.MappingUtils.IdentifierType;

/**
 * An abstract mapper that can download a csv file or read an
 * supplied / already downloaded file and build an internal map
 * from one column to another.
 * Afterwards, one sourceIdentifier can be mapped to the
 * corresponding targetIdentifier.
 * @author Clemens Wrzodek
 * @version $Rev: 1282 $
 */

public interface GenericID<ObjectType> {
	
	/**
	 * Returns the class of the identifier, e.g., Compound or Gene
	 * @return {@link IdentifierClass}
	 */
	public IdentifierClass getIDClass();
	
	/**
	 * The type of the ID, e.g., NCBI_GeneID or HMDB
	 * @return {@link IdentifierType}
	 */
	public IdentifierType getIDType();
	
	/**
   * Returns the default ID if not ID is set
   * @return Default ID
   */
  public ObjectType getDefaultID();
  
  /**
   * Set the ID.
   * @param ID to set
   */
  public void setID(ObjectType id);
  
  /**
   * @return ID of the object
   */
  public ObjectType getID();

}

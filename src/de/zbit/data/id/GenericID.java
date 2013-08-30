package de.zbit.data.id;

import java.util.Collection;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.MergeType;
import de.zbit.mapper.MappingUtils.IdentifierClass;
import de.zbit.mapper.MappingUtils.IdentifierType;

/**
 * Interface for object that can be assigned some ID.
 * 
 * <p>If implementing this interfaceor a subclass, please also override
 * the {@link NameAndSignals#merge(Collection, NameAndSignals, MergeType)}
 * method and avoid taking mean or other stupid things of IDs.
 * Instead, if data is not gene-centric anymore, return {@link GenericID#getDefaultID()}
 * 
 * @author Lars Rosenbaum
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

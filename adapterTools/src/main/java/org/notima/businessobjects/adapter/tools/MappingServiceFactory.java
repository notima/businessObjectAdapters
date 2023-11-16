package org.notima.businessobjects.adapter.tools;

import org.notima.generic.ifacebusinessobjects.MappingServiceInstanceFactory;

/**
 * Returns a mapping service for given arguments.
 * 
 * @author Daniel Tamm
 *
 */
public interface MappingServiceFactory {

	/**
	 * 
	 * @param sourceSystem
	 * @param targetSystem
	 * @param typeOfObject
	 * @return	A Mapping Service Instance Factory that support the parameters.
	 */
	public MappingServiceInstanceFactory getMappingServiceFor(String sourceSystem, String targetSystem, String typeOfObject);
	
	/**
	 * 
	 * @param sourceSystem
	 * @return	A Mapping Service Instance Factory that support the parameters.
	 */
	public MappingServiceInstanceFactory getMappingServiceFor(String sourceSystem);
	
}

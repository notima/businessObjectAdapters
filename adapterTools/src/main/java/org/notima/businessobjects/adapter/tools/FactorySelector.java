package org.notima.businessobjects.adapter.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * Helper class to choose appropriate factory for a tenant.
 * 
 * @author Daniel Tamm
 *
 */
@SuppressWarnings("rawtypes")
public class FactorySelector {

	private Map<String, List<BusinessObjectFactory>> factoryMapList;
	
	/**
	 * Constructor.
	 * 
	 * @param factories	Available factories. 
	 */
	public FactorySelector(List<BusinessObjectFactory> factories) {
		
		factoryMapList = new TreeMap<String, List<BusinessObjectFactory>>();
	
		if (factories==null) return;
		
		List<BusinessObjectFactory> fs;
		for (BusinessObjectFactory bf : factories) {
			fs = factoryMapList.get(bf.getSystemName());
			if (fs==null) {
				fs = new ArrayList<BusinessObjectFactory>();
				factoryMapList.put(bf.getSystemName(), fs);
			}
			fs.add(bf);
		}
		
	}
	
	
	/**
	 * Returns first available factory for given system name.
	 * 
	 * @param systemName	The system name.
	 * @return	A factory if found. Null if not found.
	 */
	public BusinessObjectFactory getFirstFactoryFor(String systemName) {
		
		if (systemName==null)
			return null;
		
		List<BusinessObjectFactory> fs = factoryMapList.get(systemName);
		if (fs==null || fs.size()<1)
			return null;
		
		return fs.get(0);
		
	}

	/**
	 * Compares tenant details.
	 * 
	 * @param taxId			The tax id.
	 * @param countryCode	The country code
	 * @param bp			The business partner to compare.
	 * @return		True if this is the tenant
	 */
	private boolean isThisTenant(String taxId, String countryCode, BusinessPartner bp) {
		
		if (taxId==null)
			return false;
		
		if (bp==null)
			return false;
		
		if (taxId.equals(bp.getTaxId())) {
			
			// Compare country code if supplied
			if (countryCode==null)
				return true;
			
			if (bp.getCountryCode()==null)
				return true;
			
			return (countryCode.equals(bp.getCountryCode()));
			
		}

		return false;
	}
	
	/**
	 * Returns a business object factory that handles the given tenant.
	 * 
	 * @param systemName		The system to look for. Required.
	 * @param taxId				The tax id. Required.
	 * @param countryCode		Country code. Optional.
	 * @return	The factory. Null if none is found.
	 */
	@SuppressWarnings("unchecked")
	public BusinessObjectFactory getFactoryWithTenant(String systemName, String taxId, String countryCode) throws NoSuchTenantException {
		
		if (systemName==null)
			return null;
		
		if (taxId==null)
			return null;
		
		List<BusinessObjectFactory> fs = factoryMapList.get(systemName);
		if (fs==null || fs.size()<1)
			return null;

		// Iterate through the list to find the bof with given tenant
		List<BusinessPartner> bps;
		BusinessPartner bp;
		for (BusinessObjectFactory bf : fs) {
			
			// Check for current tenant first
			bp = bf.getCurrentTenant();
			if (isThisTenant(taxId, countryCode, bp)) {
				return bf;
			}
			
			bps = bf.listTenants().getBusinessPartner();
			for (BusinessPartner b : bps) {
				if (isThisTenant(taxId, countryCode, b)) {
					bf.setTenant(taxId, countryCode);
					return bf;
				}
			}
			
		}
		
		return null;
		
	}
	
}

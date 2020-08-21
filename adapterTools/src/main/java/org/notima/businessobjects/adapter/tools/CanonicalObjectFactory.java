package org.notima.businessobjects.adapter.tools;

import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * Used to lookup canonical objects from active adapters.
 * 
 * @author Daniel Tamm
 *
 */
public interface CanonicalObjectFactory {

	@SuppressWarnings("rawtypes")
	public BusinessObjectFactory lookupAdapter(String adapterName);	
	
	public Invoice<?> lookupCustomerInvoice(String adapterName, String orgNo, String countryCode, String invoiceNo) throws NoSuchTenantException;
	
}

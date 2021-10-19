package org.notima.businessobjects.adapter.tools;

import java.util.Properties;

import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.ifacebusinessobjects.BusinessObjectConverter;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchProcessor;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;

/**
 * Used to lookup canonical objects from active adapters.
 * 
 * @author Daniel Tamm
 *
 */
public interface CanonicalObjectFactory {

	@SuppressWarnings("rawtypes")
	public BusinessObjectFactory lookupAdapter(String adapterName);	
	
	@SuppressWarnings("rawtypes")
	public BusinessObjectConverter lookupConverter(String adapterName);
	
	public PaymentFactory lookupPaymentFactory(String systemName);
	
	public PaymentBatchProcessor lookupPaymentBatchProcessor(String systemName);
	
	public Invoice<?> lookupCustomerInvoice(String adapterName, String orgNo, String countryCode, String invoiceNo) throws NoSuchTenantException;
	
	/**
	 * Converts a canonical invoice to a given adapter format.
	 * 
	 * @param adapterName		The adapter name
	 * @param source			The source invoice
	 * @param props				Properties sent to the converter.
	 * @return		A string representation of the adapter format.
	 */
	public Object convertToNativeInvoice(String adapterName, Invoice<?> source, Properties props) throws Exception;
	
}

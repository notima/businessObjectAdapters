package org.notima.businessobjects.adapter.tools;

import java.util.Collection;

import org.notima.generic.ifacebusinessobjects.InvoiceDeliveryMethod;

public interface InvoiceDeliveryMethodFactory {

	/**
	 * @param type	The type of delivery method, ie email or ekopost.
	 * @return	The delivery method. Null if none of that type is registered.
	 */
	public InvoiceDeliveryMethod getInvoiceDeliveryMethod(String type);

	/**
	 * @return	The types of the registered delivery methods.
	 */
	public Collection<String> getInvoiceDeliveryTypes();

}

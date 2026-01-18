package org.notima.businessobjects.adapter.tools;

import java.util.Properties;

import org.notima.generic.businessobjects.InvoiceList;

public interface InvoiceListFormatter {

	/**
	 * Formats an invoice
	 * 
	 * @param invoices			The invoice to be formatted.
	 * @param format			Requested format
	 * @param props				Properties sent to the formatter. These properties depends on the specific formatter.
	 * @return					A reference to the created invoice list. Normally a file path.
	 * @throws					Exception if something goes wrong.
	 */
	public String formatInvoice(InvoiceList invoices, String format, Properties props) throws Exception;
	
	/**
	 * What formats the formatter provides.
	 * 
	 * @return		Return an array of strings representing the format this formatter provides.
	 */
	public String[] getFormats();
	
	
}

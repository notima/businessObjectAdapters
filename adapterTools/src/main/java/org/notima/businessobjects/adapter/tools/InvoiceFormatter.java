package org.notima.businessobjects.adapter.tools;

import java.util.Properties;

import org.notima.generic.businessobjects.Invoice;

/**
 * Interface that formats an order list.
 * 
 * @author Daniel Tamm
 *
 */
public interface InvoiceFormatter {

	/** Property for the directory the formatted invoice is written to. */
	public static final String OUTPUT_DIR = "OutputDir";
	
	/** Property for the file name (without extension) of the formatted invoice. */
	public static final String OUTPUT_FILENAME = "OutputFilename";

	/**
	 * Formats an invoice
	 * 
	 * @param invoice			The invoice to be formatted.
	 * @param props				Properties sent to the formatter. These properties depends on the specific formatter.
	 * @return					A reference to the created order list. Normally a file path.
	 * @throws					Exception if something goes wrong.
	 */
	public String formatInvoice(Invoice<?> invoice, String format, Properties props) throws Exception;
	
	/**
	 * What formats the formatter provides.
	 * 
	 * @return		Return an array of strings representing the format this formatter provides.
	 */
	public String[] getFormats();
	
}

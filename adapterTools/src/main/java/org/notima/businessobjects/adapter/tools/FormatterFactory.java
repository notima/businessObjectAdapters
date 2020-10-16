package org.notima.businessobjects.adapter.tools;

public interface FormatterFactory {

	/**
	 * Returns an orderlist formatter for given format.
	 * 
	 * @param format		The format.
	 * @return	An order list formatter (if any).
	 */
	public OrderListFormatter getFormatter(String format);
	
	/**
	 * Returns an invoice formatter for given format.
	 * 
	 * @param format		The format.
	 * @return	An invoice reminder formatter (if any).
	 */
	public InvoiceReminderFormatter getInvoiceReminderFormatter(String format);
	
}

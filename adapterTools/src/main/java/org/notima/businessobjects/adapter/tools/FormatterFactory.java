package org.notima.businessobjects.adapter.tools;

public interface FormatterFactory {

	/**
	 * 
	 * Returns an invoice formatter for given format.
	 * 
	 * @param format
	 * @return	An invoice formatter
	 */
	public InvoiceFormatter getInvoiceFormatter(String format);	
	
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
	

	/**
	 * Returns a formatter for given class and format.
	 * 
	 * @param format		The target format.
	 * @return	A formatter for given class. Null if none is found.
	 */
	public ReportFormatter<?> getReportFormatter(Class<?> clazz, String format);
	
}

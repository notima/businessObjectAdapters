package org.notima.businessobjects.adapter.tools;

public interface FormatterFactory {

	/**
	 * Returns an orderlist formatter for given format.
	 * 
	 * @param format		The format.
	 * @return	An order list formatter (if any).
	 */
	public OrderListFormatter getFormatter(String format);
	
}

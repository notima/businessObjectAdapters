package org.notima.businessobjects.adapter.tools;

import java.util.Properties;

public interface ReportFormatter<T> {
	
	/**
	 * Formats a report from a data object
	 * 
	 * @param data	 			The data to be formatted as a report.
	 * @param props				Properties sent to the formatter. These properties depends on the specific formatter.
	 * @return					A reference to the created report. Normally a file path.
	 * @throws					Exception if something goes wrong.
	 */
	public String formatReport(T data, String format, Properties props) throws Exception;

	/**
	 * Returns the class that this formatter formats. 
	 * @return		The class.
	 * 
	 */
	public T getClazz();
	
	/**
	 * What formats the formatter provides.
	 * 
	 * @return		Return an array of strings representing the format this formatter provides.
	 */
	public String[] getFormats();
	
	
}

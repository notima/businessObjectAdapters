package org.notima.businessobjects.adapter.csv;

import java.util.Properties;

import org.notima.businessobjects.adapter.tools.InvoiceListFormatter;
import org.notima.generic.businessobjects.InvoiceList;

public class CsvInvoiceListFormatter implements InvoiceListFormatter {

	public final String CSV_FORMAT = "csv";
	public final String[] formats = {CSV_FORMAT};
	
	private InvoiceListToCsv formatter;
	
	@Override
	public String formatInvoice(InvoiceList invoices, String format, Properties props) throws Exception {

		formatter = new InvoiceListToCsv(invoices, props);
		return formatter.writeToFile();
		
	}

	@Override
	public String[] getFormats() {
		return formats;
	}

}

package org.notima.businessobjects.adapter.jasperreports;

import java.net.URL;
import java.util.Properties;

import org.notima.businessobjects.adapter.tools.InvoiceReminderFormatter;
import org.notima.generic.businessobjects.DunningEntry;

public class JasperInvoiceCollectionFormatter extends JasperBasePdfFormatter implements InvoiceReminderFormatter {
	
	public final static String JASPER_COMPANY_NAME = "JasperCompanyName";
	public final static String JASPER_TAX_ID = "JasperTaxId";

	@Override
	public String formatReminder(DunningEntry<?, ?> dunningEntry, String format, Properties props) throws Exception {

		if (dunningEntry==null) {
			throw new Exception("Dunning entry can't be null");
		}
		
		String jasperFile = props.getProperty(JASPER_FILE);
		// Lookup default jasper file as a resource
		URL url = ClassLoader.getSystemResource("reports/CollectionNotice.jasper");
		if (url!=null) {
			jasperFile = url.getFile();
		} else {
			throw new Exception("The property " + JASPER_FILE + " must be set.");
		}
		
		Object[] data = new Object[1];
		data[0] = dunningEntry;

		JasperParameterCallback jpc = null;

		if ("pdf".equalsIgnoreCase(format) || format==null) {
			return formatReportAsPdf(
					data, 
					jasperFile, 
					jpc, 
					props);
		} else {
			return null;
		}

	}

	
}

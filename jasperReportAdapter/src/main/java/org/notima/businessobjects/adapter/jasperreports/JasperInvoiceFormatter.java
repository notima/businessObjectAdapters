package org.notima.businessobjects.adapter.jasperreports;

import java.net.URL;
import java.util.Properties;

import org.notima.businessobjects.adapter.tools.InvoiceFormatter;
import org.notima.generic.businessobjects.Invoice;

public class JasperInvoiceFormatter extends JasperBasePdfFormatter implements InvoiceFormatter {
	
	public final static String JASPER_COMPANY_NAME = "JasperCompanyName";
	public final static String JASPER_TAX_ID = "JasperTaxId";

	@Override
	public String formatInvoice(Invoice<?> invoice, String format, Properties props) throws Exception {

		if (invoice==null) {
			throw new Exception("Invoice entry can't be null");
		}
		
		String jasperFile = props.getProperty(JASPER_FILE);
		if (jasperFile==null) {
			jasperFile = "reports/InvoiceBasic.jasper";
			// Lookup default jasper file as a resource
			URL url = this.getClass().getClassLoader().getResource(jasperFile);
			if (url!=null) {
				jasperFile = url.getFile();
			} else {
				throw new Exception("The property " + JASPER_FILE + " must be set.");
			}
		}
		
		Object[] data = new Object[1];
		data[0] = invoice;

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

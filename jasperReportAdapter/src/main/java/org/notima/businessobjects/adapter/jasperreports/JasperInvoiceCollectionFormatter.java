package org.notima.businessobjects.adapter.jasperreports;

import java.net.URL;
import java.util.Properties;

import org.notima.businessobjects.adapter.tools.InvoiceReminderFormatter;
import org.notima.generic.businessobjects.DunningEntry;

public class JasperInvoiceCollectionFormatter extends JasperBasePdfFormatter implements InvoiceReminderFormatter {
	
	public final static String JASPER_COMPANY_NAME = "JasperCompanyName";
	public final static String JASPER_TAX_ID = "JasperTaxId";
	
	private boolean usePlusgirot = false;

	@Override
	public String formatReminder(DunningEntry<?, ?> dunningEntry, String format, Properties props) throws Exception {

		if (dunningEntry==null) {
			throw new Exception("Dunning entry can't be null");
		}
		
		String jasperFile = props.getProperty(JASPER_FILE);

		Object[] data = new Object[1];
		data[0] = dunningEntry;

		JasperParameterCallback jpc = null;

		if ("pdf".equalsIgnoreCase(format) || format==null) {
			if (jasperFile == null) {
				// Load from bundle resources — use URL to avoid OSGI bundle-path issues
				String resourceName = "reports/CollectionNotice" + (usePlusgirot ? "PG" : "") + ".jasper";
				URL url = this.getClass().getClassLoader().getResource(resourceName);
				if (url == null) {
					throw new Exception(resourceName + " not found in bundle; set the " + JASPER_FILE + " property to override.");
				}
				return formatReportAsPdf(data, url, jpc, props);
			}
			return formatReportAsPdf(data, jasperFile, jpc, props);
		} else {
			return null;
		}

	}

	public boolean isUsePlusgirot() {
		return usePlusgirot;
	}

	public void setUsePlusgirot(boolean usePlusgirot) {
		this.usePlusgirot = usePlusgirot;
	}

	
}

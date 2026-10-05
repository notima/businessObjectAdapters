package org.notima.businessobjects.adapter.jasperreports;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.notima.businessobjects.adapter.jasperreports.ds.InvoiceListXmlDataSource;
import org.notima.businessobjects.adapter.tools.InvoiceFormatter;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;

/**
 * Formats an invoice as PDF using a Jasper report.
 * <p>
 * Uses the report given by {@link #JASPER_FILE} (or the generic
 * {@link org.notima.generic.ifacebusinessobjects.InvoiceFormatter#TEMPLATE_FILE}), or the bundled
 * {@value #DEFAULT_INVOICE_REPORT} when not set. The report's creditor is the invoice's
 * sender unless an invoice list file is configured (see {@link InvoiceListXmlDataSource}).
 * Registered both as an OSGi service
 * (see {@link Activator}) and through {@link java.util.ServiceLoader}.
 */
@SuppressWarnings("deprecation")
public class JasperInvoiceFormatter extends JasperBasePdfFormatter implements InvoiceFormatter {

	public final static String JASPER_COMPANY_NAME = "JasperCompanyName";
	public final static String JASPER_TAX_ID = "JasperTaxId";

	/** Classpath resource of the invoice report used when {@link #JASPER_FILE} isn't set. */
	public final static String DEFAULT_INVOICE_REPORT = "reports/InvoiceBasic.jasper";

	@Override
	public String formatInvoice(Invoice<?> invoice, String format, Properties props) throws Exception {

		if (invoice==null) {
			throw new Exception("Invoice entry can't be null");
		}
		if (props==null) {
			props = new Properties();
		}
		if (format!=null && !"pdf".equalsIgnoreCase(format)) {
			return null;
		}

		Object[] data = new Object[1];
		data[0] = invoice;

		JasperParameterCallback jpc = null;

		InvoiceListXmlDataSource.setCurrentCreditor(invoice.getSender());
		List<Runnable> restoreLines = fillLineFallbacks(invoice);
		try {
			String jasperFile = props.getProperty(JASPER_FILE, props.getProperty(TEMPLATE_FILE));
			if (jasperFile!=null) {
				return formatReportAsPdf(data, jasperFile, jpc, props);
			}

			// Default report bundled in this jar
			URL url = this.getClass().getClassLoader().getResource(DEFAULT_INVOICE_REPORT);
			if (url==null) {
				throw new Exception("The property " + JASPER_FILE + " must be set.");
			}
			File reportFile = toReportFile(url);
			if (reportFile!=null) {
				return formatReportAsPdf(data, reportFile.getAbsolutePath(), jpc, props);
			}
			return formatReportAsPdf(data, url, jpc, props);
		} finally {
			InvoiceListXmlDataSource.setCurrentCreditor(null);
			for (Runnable r : restoreLines) r.run();
		}

	}

	/**
	 * The bundled reports print a line's {@code key} as article number and its
	 * {@code name} as line text. Fills them from {@code productKey} and
	 * {@code description} where empty, for the duration of the formatting.
	 *
	 * @return	Actions that restore the lines afterwards.
	 */
	private List<Runnable> fillLineFallbacks(Invoice<?> invoice) {
		List<Runnable> restore = new ArrayList<Runnable>();
		if (invoice.getLines()==null) return restore;
		for (final InvoiceLine line : invoice.getLines()) {
			if (isEmpty(line.getName()) && !isEmpty(line.getDescription())) {
				final String original = line.getName();
				line.setName(line.getDescription());
				restore.add(() -> line.setName(original));
			}
			if (isEmpty(line.getKey()) && !isEmpty(line.getProductKey())) {
				final String original = line.getKey();
				line.setKey(line.getProductKey());
				restore.add(() -> line.setKey(original));
			}
		}
		return restore;
	}

	private static boolean isEmpty(String s) {
		return s==null || s.trim().length()==0;
	}


}

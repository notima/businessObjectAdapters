package org.notima.businessobjects.adapter.jasperreports;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.notima.generic.ifacebusinessobjects.CollectionNoticeFormatter;
import org.notima.businessobjects.adapter.tools.InvoiceReminderFormatter;
import org.notima.generic.businessobjects.DunningEntry;
import org.notima.generic.ifacebusinessobjects.InvoiceFormatter;

/**
 * Formats a dunning entry as a collection notice (inkassokrav) using the bundled
 * CollectionNotice reports.
 * <p>
 * The debt is the open amount of the entry's invoices, plus late payment interest from each
 * invoice's due date until today, the entry's reminder fee (60 if not set) and the collection
 * fee ({@link CollectionNoticeFormatter#COLLECTION_FEE}, 180 if not set). The template is
 * {@link CollectionNoticeFormatter#TEMPLATE}, falling back to {@link #setTemplate(Template)}.
 * <p>
 * Registered as a {@link CollectionNoticeFormatter} only: as an {@link InvoiceReminderFormatter}
 * it would compete with the reminder formatter for the pdf format.
 */
public class JasperInvoiceCollectionFormatter extends JasperBasePdfFormatter implements CollectionNoticeFormatter, InvoiceReminderFormatter {

	public final static String JASPER_COMPANY_NAME = "JasperCompanyName";
	public final static String JASPER_TAX_ID = "JasperTaxId";

	/** Report parameters, as named in the CollectionNotice reports. */
	public final static String PARAM_COLLECTION_FEE = "CollectionFee";
	public final static String PARAM_INTEREST_RATE = "InterestRate";

	/** The bundled templates. */
	public enum Template {
		/** Payment slip for bankgiro. */
		BANKGIRO(TEMPLATE_BANKGIRO, "CollectionNotice"),
		/** Payment slip for plusgiro. Also states that a samfällighet's fee is directly enforceable. */
		PLUSGIRO(TEMPLATE_PLUSGIRO, "CollectionNoticePG"),
		/** Bankgiro, stating that a samfällighet's fee is directly enforceable. */
		BANKGIRO_SAMF(TEMPLATE_BANKGIRO_SAMF, "CollectionNotice_Samf");

		private final String key;
		private final String reportName;

		Template(String key, String reportName) {
			this.key = key;
			this.reportName = reportName;
		}

		public String getKey() {
			return key;
		}

		public String getResourceName() {
			return "reports/" + reportName + ".jasper";
		}

		/**
		 * @param key		One of the CollectionNoticeFormatter.TEMPLATE_* values.
		 * @return			The template, null if key is null.
		 */
		public static Template fromKey(String key) throws Exception {
			if (key == null || key.trim().isEmpty()) return null;
			for (Template t : values()) {
				if (t.key.equalsIgnoreCase(key.trim())) return t;
			}
			throw new Exception("Unknown collection notice template: " + key);
		}
	}

	private Template template = Template.BANKGIRO;

	@Override
	public String formatCollectionNotice(DunningEntry<?, ?> dunningEntry, String format, Properties props) throws Exception {

		if (dunningEntry==null) {
			throw new Exception("Dunning entry can't be null");
		}
		if (props==null) {
			props = new Properties();
		}
		if (format!=null && !"pdf".equalsIgnoreCase(format)) {
			return null;
		}

		Object[] data = new Object[1];
		data[0] = dunningEntry;

		JasperParameterCallback jpc = createParameterCallback(props);

		String jasperFile = props.getProperty(JASPER_FILE, props.getProperty(InvoiceFormatter.TEMPLATE_FILE));
		if (jasperFile != null) {
			return formatReportAsPdf(data, jasperFile, jpc, props);
		}

		Template t = Template.fromKey(props.getProperty(TEMPLATE));
		if (t == null) t = template;

		// Load from bundle resources — use URL to avoid OSGI bundle-path issues
		String resourceName = t.getResourceName();
		URL url = this.getClass().getClassLoader().getResource(resourceName);
		if (url == null) {
			throw new Exception(resourceName + " not found in bundle; set the " + JASPER_FILE + " property to override.");
		}
		// As a file when possible, so that the subreport and images resolve
		File reportFile = toReportFile(url);
		if (reportFile != null) {
			return formatReportAsPdf(data, reportFile.getAbsolutePath(), jpc, props);
		}
		return formatReportAsPdf(data, url, jpc, props);

	}

	/**
	 * Same as {@link #formatCollectionNotice(DunningEntry, String, Properties)}.
	 */
	@Override
	public String formatReminder(DunningEntry<?, ?> dunningEntry, String format, Properties props) throws Exception {
		return formatCollectionNotice(dunningEntry, format, props);
	}

	private JasperParameterCallback createParameterCallback(Properties props) throws Exception {

		final Properties extra = new Properties();
		Double collectionFee = getDouble(props, COLLECTION_FEE);
		if (collectionFee != null) {
			extra.put(PARAM_COLLECTION_FEE, collectionFee);
		}
		Double interestRate = getDouble(props, INTEREST_RATE);
		if (interestRate != null) {
			extra.put(PARAM_INTEREST_RATE, interestRate);
		}
		return new JasperParameterCallback() {
			@Override
			public Properties getExtraProperties() {
				return extra;
			}
		};

	}

	private static Double getDouble(Properties props, String key) throws Exception {
		Object value = props.get(key);
		if (value == null) return null;
		if (value instanceof Number) return ((Number) value).doubleValue();
		String s = value.toString().trim().replace(',', '.');
		if (s.isEmpty()) return null;
		try {
			return Double.valueOf(s);
		} catch (NumberFormatException e) {
			throw new Exception("Invalid " + key + ": " + value);
		}
	}

	/**
	 * @return	The template used when the properties don't name one.
	 */
	public Template getTemplate() {
		return template;
	}

	public void setTemplate(Template template) {
		this.template = template != null ? template : Template.BANKGIRO;
	}

	public boolean isUsePlusgirot() {
		return template == Template.PLUSGIRO;
	}

	public void setUsePlusgirot(boolean usePlusgirot) {
		this.template = usePlusgirot ? Template.PLUSGIRO : Template.BANKGIRO;
	}


}

package org.notima.businessobjects.adapter.tools.task;

import java.io.File;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.notima.businessobjects.adapter.paymentbatch.BasicPaymentBatchChannelFactory;
import org.notima.businessobjects.adapter.tools.AdapterToolsSettings;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.FormatterFactory;
import org.notima.businessobjects.adapter.tools.PaymentChannelReportWriter;
import org.notima.businessobjects.adapter.tools.table.PaymentBatchTable;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PaymentBatchChannelThresholds;
import org.notima.generic.businessobjects.PaymentBatchProcessOptions;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.ThresholdCheckResult;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchProcessor;

/**
 * Matches the pending report files of a tenant's active payment channels against their destination
 * systems (like process-payment-channel --match-only) and writes one report per channel to the
 * tenant's report directory.
 * 
 * The report directory is the tenant information's reportDirectory (or defaultOutputDirectory),
 * set with set-tenant-info. It can be overridden with {@link #setReportDirectory(String)}.
 *
 * Nothing is booked, no files are moved and the channels aren't changed.
 *
 * The task can be triggered by a Camel route (bean:...?method=run) or by the Karaf scheduler
 * (registered as a Runnable service with a scheduler.expression property). See the adapterTools README.
 */
public class PaymentChannelMatchReportTask extends Task implements Runnable {

	public static final String TASK_ID = "payment-channel-match-report";
	public static final String DEFAULT_FORMAT = "xls";

	private final String	orgNo;
	private String			countryCode;
	private String			reportDirectory;
	private String			format = DEFAULT_FORMAT;
	private CanonicalObjectFactory	cof;
	private FormatterFactory		formatterFactory;
	private AdapterToolsSettings	settings;

	/**
	 * @param orgNo		The org number (tax id) of the tenant whose channels are matched.
	 */
	public PaymentChannelMatchReportTask(String orgNo) {
		this(orgNo, null);
	}
	
	/**
	 * @param orgNo		The org number (tax id) of the tenant whose channels are matched.
	 * @param out		Where to print progress (ie a shell console), in addition to the log. Can be null.
	 */
	public PaymentChannelMatchReportTask(String orgNo, PrintStream out) {
		super(out);
		this.orgNo = orgNo!=null ? orgNo.trim() : null;
	}
	
	/**
	 * Logs a message and prints it to the output stream, if there is one.
	 */
	private void progress(String msg) {
		log.info("Task {}: {}", getTaskId(), msg);
		if (outStream!=null) {
			outStream.println(msg);
		}
	}

	/**
	 * @return	The task id, one per tenant so different tenants can run at the same time.
	 */
	@Override
	public String getTaskId() {
		return TASK_ID + "-" + orgNo;
	}

	/**
	 * Runs the task. Errors are logged, not thrown, so a scheduler or route keeps running.
	 */
	@Override
	public void run() {
		try {
			execute();
		} catch (Exception e) {
			log.error("Task " + getTaskId() + " failed", e);
		}
	}

	/**
	 * @return	The paths of the written reports.
	 */
	@Override
	protected Object onExecute() throws Exception {

		if (orgNo==null || orgNo.length()==0) {
			throw new IllegalStateException("No tenant (orgNo) set for task " + TASK_ID);
		}
		
		// Services are injected (ie Blueprint references) or looked up in the OSGi registry
		CanonicalObjectFactory cof = this.cof!=null ? this.cof : (CanonicalObjectFactory)getServiceReference(CanonicalObjectFactory.class);
		FormatterFactory formatterFactory = this.formatterFactory!=null ? this.formatterFactory : (FormatterFactory)getServiceReference(FormatterFactory.class);
		AdapterToolsSettings settings = this.settings!=null ? this.settings : (AdapterToolsSettings)getServiceReference(AdapterToolsSettings.class);
		if (cof==null || formatterFactory==null) {
			throw new IllegalStateException("CanonicalObjectFactory or FormatterFactory service not available");
		}
		
		String effectiveCountryCode = countryCode!=null && countryCode.trim().length()>0 ? countryCode.trim() 
				: (settings!=null ? settings.getDefaultCountryCode() : null);
		TaxSubjectIdentifier tenant = new TaxSubjectIdentifier(orgNo, effectiveCountryCode);
		
		String dirName = reportDirectory!=null && reportDirectory.trim().length()>0 ? reportDirectory.trim()
				: PaymentChannelReportWriter.resolveReportDirectory(cof, tenant, effectiveCountryCode);
		if (dirName==null) {
			throw new IllegalStateException("No report directory for tenant " + tenant 
					+ ". Set it with: set-tenant-info " + orgNo + " reportDirectory <directory>");
		}
		File dir = new File(dirName);
		if (!dir.isDirectory() && !dir.mkdirs()) {
			throw new IllegalStateException("Report directory " + dirName + " can't be created");
		}
		
		PaymentBatchChannelFactory channelFactory = cof.lookupFirstPaymentBatchChannelFactory();
		if (channelFactory==null) {
			throw new IllegalStateException("No payment batch channel factory available");
		}
		if (channelFactory instanceof BasicPaymentBatchChannelFactory) {
			((BasicPaymentBatchChannelFactory)channelFactory).setCanonicalObjectFactory(cof);
		}

		List<String> reports = new ArrayList<String>();
		int active = 0;
		for (PaymentBatchChannel channel : listTenantChannels(channelFactory, tenant)) {
			if (!isActive(channel)) continue;
			active++;
			updateLockMetaData("Matching channel " + channelName(channel));
			try {
				String report = matchAndReport(cof, formatterFactory, channel, dirName);
				if (report!=null) {
					reports.add(report);
				}
			} catch (Exception e) {
				log.error("Task " + getTaskId() + ": channel " + channelName(channel) + " failed", e);
				if (outStream!=null) {
					outStream.println(channelName(channel) + ": failed: " + e.getMessage());
				}
			}
		}

		progress(active + " active channels, " + reports.size() + " reports written to " + dirName);
		return reports;
	}
	
	/**
	 * Lists the tenant's channels. Channels are often stored without country code, so channels
	 * with the tenant's tax id and either no or the same country code are included.
	 */
	private List<PaymentBatchChannel> listTenantChannels(PaymentBatchChannelFactory channelFactory, TaxSubjectIdentifier tenant) {
		List<PaymentBatchChannel> result = new ArrayList<PaymentBatchChannel>();
		List<PaymentBatchChannel> candidates = new ArrayList<PaymentBatchChannel>();
		candidates.addAll(channelFactory.listChannelsForTenant(new TaxSubjectIdentifier(tenant.getTaxId())));
		if (tenant.hasCountryCode()) {
			candidates.addAll(channelFactory.listChannelsForTenant(tenant));
		}
		for (PaymentBatchChannel ch : candidates) {
			if (!result.contains(ch) && PaymentChannelReportWriter.isSameTenant(ch.getTenant(), tenant)) {
				result.add(ch);
			}
		}
		return result;
	}

	/**
	 * Matches one channel's pending files and writes its report.
	 *
	 * @return	The path of the written report, or null if there was nothing to report.
	 */
	private String matchAndReport(CanonicalObjectFactory cof, FormatterFactory formatterFactory, PaymentBatchChannel channel, String dirName) throws Exception {

		String sourceDirectory = channel.getOptions()!=null ? channel.getOptions().getSourceDirectory() : null;
		if (sourceDirectory==null) {
			progress(channelName(channel) + ": no source directory, skipped");
			return null;
		}
		PaymentBatchFactory sourceFactory = cof.lookupPaymentBatchFactory(channel.getSourceSystem());
		PaymentBatchProcessor processor = cof.lookupPaymentBatchProcessor(channel.getDestinationSystem());
		if (sourceFactory==null || processor==null) {
			progress(channelName(channel) + ": no adapter for " + channel.getSourceSystem() + " or "
					+ channel.getDestinationSystem() + ", skipped");
			return null;
		}

		sourceFactory.setSource(sourceDirectory);
		List<PaymentBatch> batches = sourceFactory.readPaymentBatches();
		PaymentBatchProcessOptions options = new PaymentBatchProcessOptions();
		for (PaymentBatch pb : batches) {
			processor.lookupInvoiceReferences(pb, options);
		}

		// Before merging, since the thresholds are checked per report file
		reportMatchResult(channel, batches);
		
		PaymentBatch merged = PaymentChannelReportWriter.mergeBatches(batches);
		if (merged==null || merged.isEmpty()) {
			progress(channelName(channel) + ": no pending payments");
			return null;
		}

		PaymentBatchTable table = new PaymentBatchTable(merged, true);
		String fileName = PaymentChannelReportWriter.buildFileName(channel, merged, format);
		String path = PaymentChannelReportWriter.writeReport(formatterFactory, table, format, dirName, fileName);
		if (path==null) {
			throw new IllegalStateException("No report formatter for format " + format);
		}
		progress(channelName(channel) + ": report written to " + path);
		return path;

	}

	/**
	 * Reports the matching result of the channel's pending files and checks them against the
	 * channel's thresholds, the same way process-payment-channel does (per report file).
	 */
	private void reportMatchResult(PaymentBatchChannel channel, List<PaymentBatch> batches) {
		
		String name = channelName(channel);
		
		// Totals over all pending files, counted with the same rules as the thresholds
		ThresholdCheckResult total = new PaymentBatchChannelThresholds().evaluate(batches);
		if (total.getPaymentCount()==0) return;
		StringBuilder result = new StringBuilder(name + ": " + total.getPaymentCount() + (total.getPaymentCount()==1 ? " payment, " : " payments, ") 
				+ (total.getPaymentCount() - total.getUnmatchedCount()) + " matched, " 
				+ total.getUnmatchedCount() + " unmatched (" + formatNumber(total.getUnmatchedPercent()) + " %)");
		if (!total.getUnmatchedAmountPerCurrency().isEmpty()) {
			result.append(", unmatched amount");
			String separator = " ";
			for (Map.Entry<String, Double> e : total.getUnmatchedAmountPerCurrency().entrySet()) {
				result.append(separator).append(e.getKey()).append(" ").append(formatAmount(e.getValue()));
				separator = ", ";
			}
		}
		progress(result.toString());
		
		PaymentBatchChannelThresholds thresholds = channel.getOptions()!=null ? channel.getOptions().getThresholds() : null;
		if (thresholds==null || !thresholds.hasLimits()) {
			progress(name + ": thresholds: none");
			return;
		}
		progress(name + ": thresholds: " + describe(thresholds));
		for (Map.Entry<String, List<PaymentBatch>> file : PaymentChannelReportWriter.groupBySource(batches).entrySet()) {
			ThresholdCheckResult check = thresholds.evaluate(file.getValue());
			if (check.isBreached()) {
				progress(name + ": processing would stop at " + file.getKey() + ": " + String.join("; ", check.getBreaches()));
				return;
			}
		}
		progress(name + ": all files within thresholds");
		
	}
	
	private static String describe(PaymentBatchChannelThresholds t) {
		List<String> parts = new ArrayList<String>();
		if (t.getMaxUnmatchedCount()!=null) {
			parts.add("max unmatched " + t.getMaxUnmatchedCount());
		}
		if (t.getMaxUnmatchedPercent()!=null) {
			parts.add("max unmatched " + formatNumber(t.getMaxUnmatchedPercent()) + " %");
		}
		if (t.getMaxUnmatchedAmount()!=null) {
			parts.add("max unmatched amount " + t.getMaxUnmatchedAmount());
		}
		return String.join(", ", parts);
	}
	
	private static String formatNumber(double d) {
		return new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(d);
	}
	
	private static String formatAmount(double d) {
		return new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(d);
	}
	
	private static boolean isActive(PaymentBatchChannel channel) {
		return channel.getStatus()==null || channel.getStatus().isActive();
	}

	private static String channelName(PaymentBatchChannel channel) {
		return channel.getChannelDescription()!=null && channel.getChannelDescription().trim().length()>0
				? channel.getChannelDescription() : channel.getChannelId();
	}

	public String getOrgNo() {
		return orgNo;
	}
	
	public String getCountryCode() {
		return countryCode;
	}

	/**
	 * @param countryCode	The tenant's country code. If not set, the default country code (AdapterTools settings) is used.
	 */
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	
	public String getReportDirectory() {
		return reportDirectory;
	}

	/**
	 * @param reportDirectory	Overrides the tenant's report directory. Created if it doesn't exist.
	 */
	public void setReportDirectory(String reportDirectory) {
		this.reportDirectory = reportDirectory;
	}

	public String getFormat() {
		return format;
	}

	/**
	 * @param format	The report format, ie xls (default).
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * @param cof	The canonical object factory. If not set, it's looked up in the OSGi service registry.
	 */
	public void setCanonicalObjectFactory(CanonicalObjectFactory cof) {
		this.cof = cof;
	}

	/**
	 * @param formatterFactory	The formatter factory. If not set, it's looked up in the OSGi service registry.
	 */
	public void setFormatterFactory(FormatterFactory formatterFactory) {
		this.formatterFactory = formatterFactory;
	}

	/**
	 * @param settings	The AdapterTools settings (default country code). If not set, it's looked up in the OSGi service registry.
	 */
	public void setSettings(AdapterToolsSettings settings) {
		this.settings = settings;
	}
	
}

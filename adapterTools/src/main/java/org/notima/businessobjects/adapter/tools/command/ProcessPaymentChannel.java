package org.notima.businessobjects.adapter.tools.command;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.AdapterToolsSettings;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.PaymentChannelReportWriter;
import org.notima.businessobjects.adapter.tools.FormatterFactory;
import org.notima.businessobjects.adapter.tools.table.PaymentBatchTable;
import org.notima.businessobjects.adapter.tools.table.PaymentProcessResultTable;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PaymentBatchChannelOptions;
import org.notima.generic.businessobjects.PaymentBatchProcessOptions;
import org.notima.generic.businessobjects.PaymentBatchProcessResult;
import org.notima.generic.businessobjects.ThresholdCheckResult;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchProcessor;
import org.notima.util.FileUtils;
import org.notima.util.LocalDateUtils;

@Command(scope = "notima", name = "process-payment-channel", description = "Processes a payment channel")
@Service
public class ProcessPaymentChannel implements Action {
	
	@Reference
	private FormatterFactory	formatterFactory;
	
	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference
	private AdapterToolsSettings settings;
	
	private static final String		DONE_DIR = "done";
	
	@Reference 
	Session sess;

    @Option(name = "--match-only", description = "Run only a match session.", required = false, multiValued = false)
    private boolean matchOnly;
    
    @Option(name = "--draft-payments", description = "Only creates drafts of the payments, if supported by the destination adapter", required = false, multiValued = false)
    private boolean	draftPayments;
    
    @Option(name = "--non-matched-as-prepayments", description = "Account non matched as prepayments.", required = false, multiValued = false)
    private boolean nonMatchedAsPrepayments;
    
    @Option(name = "-d", aliases = { "--dry-run" }, description = "Shows the payments and vouchers that would be created, without creating them. Invoice balances are not reduced between payments in a dry run.", required = false, multiValued = false)
    private boolean dryRun;
    
    @Option(name = _NotimaCmdOptions.UNTIL_DATE, description = "Only process files dated until this date yyyy-MM-dd. Files after it, or without a known date, are left in the directory.", required = false)
    private String untilDateStr;

    @Option(name = _NotimaCmdOptions.MANUAL_MAP, description = "Manual mapping. Example \"Ref=InvoiceNo,Ref=InvoiceNo\"", required = false)
    private String	manualMapStr;
    
    @Option(name = "--force", description = "Process even if a report file exceeds the channel's thresholds (unmatched payments).", required = false, multiValued = false)
    private boolean force;

    @Option(name = "--fees-per-payment", description = "Creates fees for each payment (instead of a lump sum).", required = false, multiValued = false)
    private boolean feesPerPayment;
	
    @Option(name = "-p", aliases = { "--account-payout-only" }, description = "Only account payout", required = false, multiValued = false)
    private boolean accountPayoutOnly;
    
    @Option(name="-of", description="Output match result to file name", required = false, multiValued = false)
    private String	outFile;
    
    @Option(name="-format", description="The format of match result file to be output. Without -of, the file is written to the tenant's report directory (set-tenant-info)", required = false, multiValued = false)
    private String format;

	@Argument(index = 0, name = "channelId", description ="The payment channel to run. Could also be description (if unique)", required = true, multiValued = false)
	private String channelId = "";
    
	private String paymentSource;
	
	private PaymentBatchTable paymentBatchTable;
	
	private LocalDate	untilDate;
	
	private PaymentBatchChannelFactory channelFactory;
	private PaymentBatchChannel channel;
	private PaymentBatchFactory		sourcePaymentFactory;
	private PaymentBatchProcessor destinationPaymentProcessor;
	private PaymentBatchProcessOptions processOptions;
	
	private List<PaymentBatch>	listOfBatches = null;
	private List<PaymentBatchProcessResult>	processResults = new ArrayList<PaymentBatchProcessResult>();
	private boolean				allBatchesProcessed = false;
	private boolean				stopped = false;
	private List<String>		thresholdMessages = new ArrayList<String>();
	

	
	@Override
	public Object execute() throws Exception {
		
		initChannelFactory();
		
		findChannel();

		initProcessOptions();

		processChannel();
		
		printAllBatches();
		
		PaymentProcessResultTable.printResults(processResults, destinationPaymentProcessor.getSystemName(), sess.getConsole());
		
		printThresholdMessages();
		
		return null;
	}
	
	private void initChannelFactory() throws Exception {

		channelFactory = cof.lookupFirstPaymentBatchChannelFactory();
		if (channelFactory==null) throw new Exception("No channel factories defined.");
		
	}
	
	private void initProcessOptions() throws Exception {
		
		sourcePaymentFactory = cof.lookupPaymentBatchFactory(channel.getSourceSystem());
		destinationPaymentProcessor = cof.lookupPaymentBatchProcessor(channel.getDestinationSystem());

		if (channel.getOptions()==null || channel.getOptions().getSourceDirectory()==null) throw new Exception("Source directory not defined");
		
		paymentSource = channel.getOptions().getSourceDirectory();
		
		processOptions = new PaymentBatchProcessOptions();
		processOptions.setNonMatchedAsPrepayments(nonMatchedAsPrepayments);
		processOptions.setDraftPaymentsIfPossible(draftPayments);
		processOptions.setFeesPerPayment(feesPerPayment);
		processOptions.setAccountPayoutOnly(accountPayoutOnly);
		processOptions.addManualReferenceMapFromCommaList(manualMapStr);
		if (dryRun) {
			processOptions.setDryRun(true);
		}
		
		if (untilDateStr!=null) {
			untilDate = LocalDate.parse(untilDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
		}
		
	}
	
	/**
	 * Finds channel by ID first and then description
	 * 
	 * @throws Exception
	 */
	private void findChannel() throws Exception {

		channel = channelFactory.findChannelWithIdOrDescription(channelId);
		if (channel==null)
			throw new Exception("No channel with ID or description [" + channelId + "] found.");
		
	}
	
	
	/**
	 * Process the batches in the currently selected channel
	 * 
	 * @throws Exception
	 */
	private void processChannel() throws Exception {
		
		if (channel.getChannelDescription()!=null && channel.getChannelDescription().trim().length()>0) {
			sess.getConsole().println("Processing channel " + channel.getChannelDescription());
		}
		
		sourcePaymentFactory.setSource(paymentSource);
		List<PaymentBatch> batches = sourcePaymentFactory.readPaymentBatches(); 
		
		listOfBatches = new ArrayList<PaymentBatch>();
		
		for (List<PaymentBatch> fileBatches : groupBySource(batches).values()) {
			processFile(fileBatches);
			if (stopped) break;
		}
		allBatchesProcessed = true;
		
	}
	
	/**
	 * Groups batches by source (file). A file can result in more than one batch (ie one per currency).
	 */
	private Map<String, List<PaymentBatch>> groupBySource(List<PaymentBatch> batches) {
		Map<String, List<PaymentBatch>> result = new LinkedHashMap<String, List<PaymentBatch>>();
		for (PaymentBatch pb : batches) {
			List<PaymentBatch> fileBatches = result.get(pb.getSource());
			if (fileBatches==null) {
				fileBatches = new ArrayList<PaymentBatch>();
				result.put(pb.getSource(), fileBatches);
			}
			fileBatches.add(pb);
		}
		return result;
	}
	
	/**
	 * Processes all batches from one source file. The file is moved to the done directory
	 * when all its batches are processed.
	 * 
	 * @param fileBatches	Batches with the same source.
	 * @throws Exception
	 */
	private void processFile(List<PaymentBatch> fileBatches) throws Exception {

		if (!shouldProcess(getFileStartDate(fileBatches))) {
			return;
		}
		
		if (!checkThresholds(fileBatches)) {
			return;
		}
		
		for (PaymentBatch pb : fileBatches) {
			if (matchOnly) {
				destinationPaymentProcessor.lookupInvoiceReferences(pb, processOptions);
			} else {
				processResults.add(destinationPaymentProcessor.processPaymentBatch(pb, processOptions));
			}
			formatReport(pb);
		}
		
		if (!matchOnly && !dryRun) {
			String source = fileBatches.get(0).getSource();
			updateReconciledUntil(getReconciledDate(fileBatches));
			channel.setLastProcessedBatch(source);
			channelFactory.persistChannel(channel);
			FileUtils.moveFileToNewDirectory(
					channel.getOptions().getSourceProperties().get("directory") + File.separator + source,
					DONE_DIR);
		}
		
	}
	
	/**
	 * Matches the file's payments against the destination (read only) and checks the channel's
	 * thresholds. If a threshold is exceeded, the channel is stopped at this file, except in a
	 * dry run where it's only reported.
	 * 
	 * @param fileBatches	Batches with the same source.
	 * @return	True if the file should be processed.
	 * @throws Exception
	 */
	private boolean checkThresholds(List<PaymentBatch> fileBatches) throws Exception {
		
		if (matchOnly || force) return true;
		PaymentBatchChannelOptions opts = channel.getOptions();
		if (opts==null || !opts.hasThresholds()) return true;
		
		// Remember the match fields so that processing works exactly as without the check.
		Map<Payment<?>, Object[]> savedMatches = new IdentityHashMap<Payment<?>, Object[]>();
		for (PaymentBatch pb : fileBatches) {
			if (!pb.hasPayments()) continue;
			for (Payment<?> p : pb.getPayments()) {
				savedMatches.put(p, new Object[] { p.getMatchedInvoiceNo(), p.getMatchedInvoiceOpenAmount() });
			}
		}
		
		for (PaymentBatch pb : fileBatches) {
			destinationPaymentProcessor.lookupInvoiceReferences(pb, processOptions);
		}
		ThresholdCheckResult check = opts.getThresholds().evaluate(fileBatches);
		String source = fileBatches.get(0).getSource();
		String breaches = String.join("; ", check.getBreaches());
		
		if (check.isBreached() && !dryRun) {
			// Keep the matches so the report shows which payments were matched.
			for (PaymentBatch pb : fileBatches) {
				formatReport(pb);
			}
			thresholdMessages.add("Channel stopped at " + source + ": " + breaches);
			thresholdMessages.add("The file and any later files were not processed. Use --force to process anyway.");
			stopped = true;
			return false;
		}
		
		for (Map.Entry<Payment<?>, Object[]> e : savedMatches.entrySet()) {
			e.getKey().setMatchedInvoiceNo((String)e.getValue()[0]);
			e.getKey().setMatchedInvoiceOpenAmount((Double)e.getValue()[1]);
		}
		if (check.isBreached()) {
			thresholdMessages.add("DRY RUN - the channel would stop at " + source + ": " + breaches);
		}
		return true;
		
	}
	
	private void printThresholdMessages() {
		if (thresholdMessages.isEmpty()) return;
		sess.getConsole().println();
		for (String msg : thresholdMessages) {
			sess.getConsole().println(msg);
		}
	}
	
	/**
	 * @return	The earliest first payment date of the batches, or null if none have payments.
	 */
	private LocalDate getFirstPaymentDate(List<PaymentBatch> batches) {
		LocalDate result = null;
		for (PaymentBatch pb : batches) {
			result = earliest(result, LocalDateUtils.asLocalDate(pb.getFirstPaymentDate()));
		}
		return result;
	}
	
	/**
	 * @return	The first date of the file: the start of the period the report covers, or the first
	 * 			payment date, whichever is earlier. Null if neither is known.
	 */
	private LocalDate getFileStartDate(List<PaymentBatch> batches) {
		LocalDate result = getFirstPaymentDate(batches);
		for (PaymentBatch pb : batches) {
			result = earliest(result, pb.getPeriodFrom());
		}
		return result;
	}
	
	/**
	 * @return	The date the channel is reconciled until when the file is processed: the end of the
	 * 			period the report covers if known, otherwise the first payment date.
	 * 			Null if neither is known (ie an empty report without period).
	 */
	private LocalDate getReconciledDate(List<PaymentBatch> batches) {
		LocalDate periodTo = null;
		for (PaymentBatch pb : batches) {
			if (pb.getPeriodTo()!=null && (periodTo==null || pb.getPeriodTo().isAfter(periodTo))) {
				periodTo = pb.getPeriodTo();
			}
		}
		return periodTo!=null ? periodTo : getFirstPaymentDate(batches);
	}
	
	/**
	 * Moves the channel's reconciled until date forward. It's never cleared or moved backwards.
	 */
	private void updateReconciledUntil(LocalDate date) {
		if (date==null) return;
		LocalDate current = channel.getStatus()!=null ? channel.getStatus().getReconciledUntil() : null;
		if (current==null || date.isAfter(current)) {
			channel.setReconciledUntil(date);
		}
	}
	
	private static LocalDate earliest(LocalDate a, LocalDate b) {
		if (a==null) return b;
		if (b==null) return a;
		return b.isBefore(a) ? b : a;
	}
	
	/**
	 * Checks the until date to see if this file should be processed. With an until date, a file
	 * is only processed (and moved) if its date is known and not after the until date.
	 * 
	 * @param fileStartDate		The first date of the file. Null if unknown.
	 * @return
	 */
	private boolean shouldProcess(LocalDate fileStartDate) {
		if (untilDate==null) return true;
		return fileStartDate!=null && !fileStartDate.isAfter(untilDate);
	}

	
	
	private void formatReport(PaymentBatch pb) throws Exception {

		paymentBatchTable = new PaymentBatchTable(pb, true);

		if (!allBatchesProcessed) {
			listOfBatches.add(pb);
			return;
		}
		paymentBatchTable.getShellTable().print(sess.getConsole());
		

	}
	
	private void writeToFormat(PaymentBatch pb) throws Exception {

		if (format!=null && !pb.isEmpty()) {
			String fileName = outFile!=null ? outFile : PaymentChannelReportWriter.buildFileName(channel, pb, format);
			// Without an explicit out file, the report goes to the tenant's report directory (if set)
			String outputDir = null;
			if (outFile==null) {
				outputDir = PaymentChannelReportWriter.resolveReportDirectory(cof, channel.getTenant(), 
						settings!=null ? settings.getDefaultCountryCode() : null);
				if (outputDir!=null) {
					new File(outputDir).mkdirs();
				}
			}
			String of = PaymentChannelReportWriter.writeReport(formatterFactory, paymentBatchTable, format, outputDir, fileName);
			if (of!=null) {
				sess.getConsole().println("Output file to: " + of);
			} else {
				sess.getConsole().println("Can't find formatter for " + format);
			}
		}
		
	}
	
	private void printAllBatches() throws Exception {

		PaymentBatch pb = PaymentChannelReportWriter.mergeBatches(listOfBatches);
		if (pb==null) return;

		paymentBatchTable = new PaymentBatchTable(pb, true);
		paymentBatchTable.getShellTable().print(sess.getConsole());
		writeToFormat(pb);
		
	}
	

}

package org.notima.businessobjects.adapter.tools.command;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.BasicReportFormatter;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.FormatterFactory;
import org.notima.businessobjects.adapter.tools.ReportFormatter;
import org.notima.businessobjects.adapter.tools.table.GenericTable;
import org.notima.businessobjects.adapter.tools.table.PaymentBatchTable;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PaymentBatchProcessOptions;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchProcessor;
import org.notima.util.LocalDateUtils;

@Command(scope = "notima", name = "process-payment-channel", description = "Processes a payment channel")
@Service
public class ProcessPaymentChannel implements Action {
	
	@Reference
	private FormatterFactory	formatterFactory;
	
	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference 
	Session sess;

    @Option(name = "--match-only", description = "Run only a match session.", required = false, multiValued = false)
    private boolean matchOnly;
    
    @Option(name = "--draft-payments", description = "Only creates drafts of the payments, if supported by the destination adapter", required = false, multiValued = false)
    private boolean	draftPayments;
    
    @Option(name = "--non-matched-as-prepayments", description = "Account non matched as prepayments.", required = false, multiValued = false)
    private boolean nonMatchedAsPrepayments;
    
    @Option(name = "-d", aliases = { "--dry-run" }, description = "Let's you know what would be done, but doesn't do it", required = false, multiValued = false)
    private boolean dryRun;
    
    @Option(name = _NotimaCmdOptions.UNTIL_DATE, description = "Only process until this date yyyy-MM-dd", required = false)
    private String untilDateStr;
    
    @Option(name = "--fees-per-payment", description = "Creates fees for each payment (instead of a lump sum).", required = false, multiValued = false)
    private boolean feesPerPayment;
	
    @Option(name = "-p", aliases = { "--account-payout-only" }, description = "Only account payout", required = false, multiValued = false)
    private boolean accountPayoutOnly;
    
    @Option(name="-of", description="Output match result to file name", required = false, multiValued = false)
    private String	outFile;
    
    @Option(name="-format", description="The format of match result file to be output", required = false, multiValued = false)
    private String format;

	@Argument(index = 0, name = "channelId", description ="The payment channel to run", required = true, multiValued = false)
	private String channelId = "";
    
	private String paymentSource;
	
	private PaymentBatchTable paymentBatchTable;
	private ReportFormatter<GenericTable> rf;
	
	private LocalDate	untilDate;
	
	private PaymentBatchChannelFactory channelFactory;
	private PaymentBatchChannel channel;
	private PaymentBatchFactory		sourcePaymentFactory;
	private PaymentBatchProcessor destinationPaymentProcessor;
	private PaymentBatchProcessOptions processOptions;

	private void initParameters() throws Exception {

		channelFactory = cof.lookupFirstPaymentBatchChannelFactory();
		if (channelFactory==null) throw new Exception("No channel factories defined.");
		
		channel = channelFactory.findChannelWithId(channelId);
		if (channel==null) throw new Exception("No channel with ID [" + channelId + "] found.");

		sourcePaymentFactory = cof.lookupPaymentBatchFactory(channel.getSourceSystem());
		destinationPaymentProcessor = cof.lookupPaymentBatchProcessor(channel.getDestinationSystem());

		if (channel.getOptions()==null || channel.getOptions().getSourceDirectory()==null) throw new Exception("Source directory not defined");
		
		paymentSource = channel.getOptions().getSourceDirectory();
		
		processOptions = new PaymentBatchProcessOptions();
		processOptions.setNonMatchedAsPrepayments(nonMatchedAsPrepayments);
		processOptions.setDraftPaymentsIfPossible(draftPayments);
		processOptions.setFeesPerPayment(feesPerPayment);
		processOptions.setAccountPayoutOnly(accountPayoutOnly);
		if (dryRun) {
			processOptions.setDryRun(true);
		}
		
		if (untilDateStr!=null) {
			untilDate = LocalDate.parse(untilDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
		}
		
	}
	
	@Override
	public Object execute() throws Exception {
		
		initParameters();
		sourcePaymentFactory.setSource(paymentSource);
		List<PaymentBatch> batches = sourcePaymentFactory.readPaymentBatches(); 
		
		for (PaymentBatch pb : batches) {
			processAndPrint(pb);
		}
		
		return null;
	}
	
	private void processAndPrint(PaymentBatch pb) throws Exception {

		if (!shouldProcess(pb)) {
			return;
		}
		
		if (matchOnly) {
			destinationPaymentProcessor.lookupInvoiceReferences(pb);
		} else {
			destinationPaymentProcessor.processPaymentBatch(pb, processOptions);
			if (!dryRun) {
				// Update processed date
				channel.setReconciledUntil(LocalDateUtils.asLocalDate(pb.getFirstPaymentDate()));
				channel.setLastProcessedBatch(pb.getSource());
				channelFactory.persistChannel(channel);
			}
		}
		
		formatReport(pb);
		
	}
	
	/**
	 * Checks until date to see if this should be processed.
	 * 
	 * @param pb
	 * @return
	 */
	private boolean shouldProcess(PaymentBatch pb) {
		if (untilDate==null) return true;
		
		LocalDate firstPaymentDate = LocalDateUtils.asLocalDate(pb.getFirstPaymentDate());
		
		if (firstPaymentDate.isAfter(untilDate)) {
			return false;
		}
		return true;
		
	}
	
	
	private void constructOutFile(PaymentBatch pb) {
		if (format!=null && outFile==null && rf!=null) {
			// We need to construct an outfile.
			outFile = pb.getSource() + "." + format;
		}
	}
	
	@SuppressWarnings("unchecked")
	private void formatReport(PaymentBatch pb) throws Exception {
		
		paymentBatchTable = new PaymentBatchTable(pb, true);
		paymentBatchTable.getShellTable().print(sess.getConsole());
		
		if (format!=null) {
			
			// Try to find a report formatter
			rf = (ReportFormatter<GenericTable>) formatterFactory.getReportFormatter(GenericTable.class, format);
			
			if (rf!=null) {
				Properties props = new Properties();
				constructOutFile(pb);
				props.setProperty(BasicReportFormatter.OUTPUT_FILENAME, outFile);
				
				String of = rf.formatReport((GenericTable)paymentBatchTable, format, props);
				sess.getConsole().println("Output file to: " + of);
				// Reset outfile for another run
				outFile = null;
			} else {
				sess.getConsole().println("Can't find formatter for " + format);
			}
		}

	}
	

}

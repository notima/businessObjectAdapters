package org.notima.businessobjects.adapter.fortnox;

import java.util.Map;
import java.util.TreeMap;

import org.notima.api.fortnox.FortnoxException;
import org.notima.api.fortnox.entities3.Invoice;
import org.notima.generic.businessobjects.BasicPaymentBatchProcessor;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PaymentBatchProcessOptions;
import org.notima.generic.businessobjects.PaymentBatchProcessResult;
import org.notima.generic.businessobjects.PaymentBatchProcessResult.ResultCode;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes a payment batch
 * 
 * @author Daniel Tamm
 *
 */
public class FortnoxPaymentBatchProcessor extends BasicPaymentBatchProcessor {

	private Logger log = LoggerFactory.getLogger(FortnoxPaymentBatchProcessor.class);	
	
	private FortnoxAdapter fortnoxAdapter;
	
	private Map<TaxSubjectIdentifier, FortnoxPaymentBatchRunner> fortnoxRunnerMap = new TreeMap<TaxSubjectIdentifier, FortnoxPaymentBatchRunner>();
	
	public FortnoxAdapter getFortnoxAdapter() {
		return fortnoxAdapter;
	}

	public void setFortnoxAdapter(FortnoxAdapter fortnoxAdapter) {
		this.fortnoxAdapter = fortnoxAdapter;
	}
	
	@Override
	public PaymentBatch lookupInvoiceReferences(PaymentBatch report) throws Exception {

		if (!report.hasPayments()) return report;

		FortnoxPaymentBatchRunner cl = createFortnoxPaymentBatchRunner(report);
		
		boolean fortnoxClientFailed = false;
		
		String existingReference = null;
		
		Invoice inv;
		int matchCount = 0;
		int totalCount = 0;
		
		// Iterate through the batch to find the references.
		for (Payment<?> payment : report.getPayments()) {
			
			existingReference = payment.getMatchedInvoiceNo();
			
			if (existingReference==null) {
				// Try to lookup Fortnox invoice id
				try {
					if (!fortnoxClientFailed) {
						inv = cl.getInvoiceToPayAndUpdatePayment(payment);
					}
				} catch (FortnoxException fe) {
					fortnoxClientFailed = true;
					System.err.println(fe.getErrorInformation().getMessage());
				}
			} else {
				if (!fortnoxClientFailed) {
					inv = cl.getFortnoxInvoice(existingReference);
					if (inv!=null) {
						matchCount++;
					}
				}

			}
			
		}
		
		return report;
	}

	@Override
	public synchronized PaymentBatchProcessResult processPaymentBatch(PaymentBatch report, PaymentBatchProcessOptions options) throws Exception {
		
		 PaymentBatchProcessResult processResult = new PaymentBatchProcessResult();
		
		if (report==null) {
			return processResult;
		}
		
		if (report.isEmpty()) {
			// Successful
			processResult.setResultCode(ResultCode.OK);
			return processResult;
		}

		// Get Fortnox client for this report
		FortnoxPaymentBatchRunner batchRunner = createFortnoxPaymentBatchRunner(report);	
		batchRunner.setOptions(options);
		batchRunner.setPaymentBatch(report);

		batchRunner.processPayments();
		batchRunner.processFees();
		batchRunner.processPayout();
		
		return batchRunner.getPaymentBatchProcessResult();
	}
	
	@Override
	public String getSystemName() {
		return FortnoxAdapter.SYSTEMNAME;
	}
	
	
	private FortnoxPaymentBatchRunner createFortnoxPaymentBatchRunner(PaymentBatch report) throws Exception {

		FortnoxPaymentBatchRunner runner = fortnoxRunnerMap.get(report.getBatchOwner());
		if (runner==null) {
			// Make sure we have the correct Fortnox instance
			fortnoxAdapter.setTenant(report.getBatchOwner().getTaxId(), report.getBatchOwner().getCountryCode());
			FortnoxExtendedClient client = new FortnoxExtendedClient(fortnoxAdapter);
			runner = new FortnoxPaymentBatchRunner(client);
			fortnoxRunnerMap.put(report.getBatchOwner(), runner);
		}
		return runner;
		
	}

}

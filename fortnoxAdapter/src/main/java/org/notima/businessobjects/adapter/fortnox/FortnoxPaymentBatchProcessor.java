package org.notima.businessobjects.adapter.fortnox;

import java.util.Properties;

import org.notima.api.fortnox.FortnoxException;
import org.notima.api.fortnox.entities3.Invoice;
import org.notima.businessobjects.adapter.fortnox.FortnoxExtendedClient.ReferenceField;
import org.notima.generic.businessobjects.BasicPaymentBatchProcessor;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PaymentBatchProcessResult;

/**
 * Processes a payment batch
 * 
 * @author Daniel Tamm
 *
 */
public class FortnoxPaymentBatchProcessor extends BasicPaymentBatchProcessor {

	private FortnoxAdapter fortnoxAdapter;
	private FortnoxExtendedClient	cl;
	
	public FortnoxAdapter getFortnoxAdapter() {
		return fortnoxAdapter;
	}

	public void setFortnoxAdapter(FortnoxAdapter fortnoxAdapter) {
		this.fortnoxAdapter = fortnoxAdapter;
	}

	@Override
	public PaymentBatch lookupInvoiceReferences(PaymentBatch report) throws Exception {

		if (report.hasPayments()) return report;
		
		// Make sure we have the correct Fortnox instance
		fortnoxAdapter.setTenant(report.getBatchOwner().getTaxId(), report.getBatchOwner().getCountryCode());
		
		boolean fortnoxClientFailed = false;
		
		cl = new FortnoxExtendedClient(fortnoxAdapter);
		
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
						inv = cl.getInvoiceToPay(
								payment.getDestinationSystemReference(), 
								ReferenceField.valueOf(payment.getDestinationSystemReferenceField()), 
								payment.getPaymentDate(),
								null);
						if (inv!=null) {
							matchCount++;
							if (inv.getOCR()!=null && inv.getOCR().trim().length()>0)
								payment.setMatchedInvoiceNo(inv.getOCR());
							else 
								payment.setMatchedInvoiceNo(inv.getDocumentNumber());
						}
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
		
		return null;
	}

	@Override
	public PaymentBatchProcessResult processPaymentBatch(PaymentBatch report, Properties props) throws Exception {

		setOptionsFromProperties(props);
		
		return null;
	}

	@Override
	public String getSystemName() {
		return FortnoxAdapter.SYSTEMNAME;
	}


}

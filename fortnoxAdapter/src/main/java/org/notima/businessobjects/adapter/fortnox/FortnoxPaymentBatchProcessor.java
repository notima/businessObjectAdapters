package org.notima.businessobjects.adapter.fortnox;

import java.io.PrintStream;
import java.util.Properties;

import org.notima.generic.businessobjects.BasicPaymentBatchProcessor;
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
	
	public FortnoxAdapter getFortnoxAdapter() {
		return fortnoxAdapter;
	}

	public void setFortnoxAdapter(FortnoxAdapter fortnoxAdapter) {
		this.fortnoxAdapter = fortnoxAdapter;
	}

	@Override
	public PaymentBatch lookupInvoiceReferences(PaymentBatch report) throws Exception {
		// TODO Auto-generated method stub
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

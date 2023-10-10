package org.notima.businessobjects.adapter.tools;

import org.notima.generic.businessobjects.BasicPaymentBatchProcessor;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PaymentBatchProcessOptions;
import org.notima.generic.businessobjects.PaymentBatchProcessResult;

public class FilePaymentBatchProcessor extends BasicPaymentBatchProcessor {

	public static final String SystemName = "File";
	
	@Override
	public PaymentBatch lookupInvoiceReferences(PaymentBatch report) throws Exception {
		return report;
	}

	@Override
	public PaymentBatchProcessResult processPaymentBatch(PaymentBatch report, PaymentBatchProcessOptions options)
			throws Exception {
		
		FilePaymentBatchProcessorRunner runner = new FilePaymentBatchProcessorRunner(report);
		if (options!=null)
			runner.setOptions(options);
		
		return runner.getResult();
	}

	@Override
	public String getSystemName() {
		return SystemName;
	}

}

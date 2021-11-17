package org.notima.businessobjects.adapter.ratepay;

import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;
import org.notima.ratepay.RatepayReport;
import org.notima.ratepay.RatepayReportParser;

public class RatepayAdapter implements PaymentFactory {

	public static String SYSTEMNAME = "Ratepay";
	
	@Override
	public PaymentBatch readPaymentBatchFromSource(String source) throws Exception {

        RatepayReport report = RatepayReportParser.createFromFile(source);
        RatepayToPaymentBatch converter = RatepayToPaymentBatch.buildFromReport(report);
		
		return converter.getPaymentBatch();
	}

	@Override
	public String getSystemName() {
		return SYSTEMNAME;
	}
	
	
}

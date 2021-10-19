package org.notima.businessobjects.adapter.ratepay;

import java.io.FileInputStream;
import java.util.List;

import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;
import org.notima.ratepay.RatepayReportParser;
import org.notima.ratepay.RatepayReportRow;

public class RatepayAdapter implements PaymentFactory {

	public static String SYSTEMNAME = "Ratepay";
	
	@Override
	public PaymentBatch readPaymentBatchFromSource(String source) throws Exception {

        RatepayReportParser parser = new RatepayReportParser();
        List<RatepayReportRow> report = parser.parseFile(new FileInputStream(source));
        RatepayToCanonicalPayment converter = RatepayToCanonicalPayment.buildFromList(report);
		
		return converter.getPaymentBatch();
	}

	@Override
	public String getSystemName() {
		return SYSTEMNAME;
	}
	
	
}

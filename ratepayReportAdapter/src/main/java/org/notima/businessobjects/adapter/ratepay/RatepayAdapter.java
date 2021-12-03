package org.notima.businessobjects.adapter.ratepay;

import java.io.File;

import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;
import org.notima.ratepay.RatepayReport;
import org.notima.ratepay.RatepayReportParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RatepayAdapter implements PaymentFactory {

	public static String SYSTEMNAME = "Ratepay";
	
	public static Logger log = LoggerFactory.getLogger(RatepayAdapter.class);	
	
	@Override
	public PaymentBatch readPaymentBatchFromSource(String source) throws Exception {

		// Check to see if source is a file
		File sourceFile = new File(source);
		File sourceDir = null;
		if (sourceFile.exists() && sourceFile.getParentFile().isDirectory()) {
			sourceDir = sourceFile.getParentFile();
			RatepayDirectoryToPaymentBatch directoryReader = new RatepayDirectoryToPaymentBatch(sourceDir.getCanonicalPath());
			PaymentBatch result = directoryReader.createPaymentBatchFromFile(sourceFile.getName());
			return result;
		} else {
			// TODO: Probably obsolete code
	        RatepayReport report = RatepayReportParser.createFromFile(source);
	        RatepayToPaymentBatch converter = RatepayToPaymentBatch.buildFromReport(report);
			return converter.getPaymentBatch();
		}
	}

	@Override
	public String getSystemName() {
		return SYSTEMNAME;
	}
	
	
}

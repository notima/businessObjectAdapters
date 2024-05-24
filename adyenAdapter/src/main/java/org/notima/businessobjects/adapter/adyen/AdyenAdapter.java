package org.notima.businessobjects.adapter.adyen;

import java.io.File;

import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;
import org.notima.adyen.AdyenReport;
import org.notima.adyen.AdyenReportParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdyenAdapter implements PaymentFactory {

	public static String SYSTEMNAME = "Adyen";
	
	public static Logger log = LoggerFactory.getLogger(AdyenAdapter.class);	
	
	@Override
	public PaymentBatch readPaymentBatchFromSource(String source) throws Exception {

		// Check to see if source is a file
		File sourceFile = new File(source);
		File sourceDir = null;
		if (sourceFile.exists() && sourceFile.getParentFile().isDirectory()) {
			sourceDir = sourceFile.getParentFile();
			String filenameonly = sourceFile.getName();
			AdyenDirectoryToPaymentBatch directoryReader = new AdyenDirectoryToPaymentBatch(sourceDir.getCanonicalPath());
			PaymentBatch result = directoryReader.createPaymentBatchFromFile(filenameonly);
			return result;
		} else {
			// TODO: Probably obsolete code
	        AdyenReport report = AdyenReportParser.createFromFile(source);
	        AdyenToPaymentBatch converter = AdyenToPaymentBatch.buildFromReport(report);
			return converter.getPaymentBatch();
		}
	}

	@Override
	public String getSystemName() {
		return SYSTEMNAME;
	}
	
	
}

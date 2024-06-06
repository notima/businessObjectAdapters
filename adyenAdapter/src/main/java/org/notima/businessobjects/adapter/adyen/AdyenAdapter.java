package org.notima.businessobjects.adapter.adyen;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
	public List<PaymentBatch> readPaymentBatchesFromSource(String source) throws Exception {

		List<PaymentBatch> batchList = new ArrayList<PaymentBatch>();
		
		// Check to see if source is a file
		File sourceFile = new File(source);
		File sourceDir = null;
		if (sourceFile.exists() && sourceFile.getParentFile().isDirectory()) {
			sourceDir = sourceFile.getParentFile();
			String filenameonly = sourceFile.getName();
			AdyenDirectoryToPaymentBatch directoryReader = new AdyenDirectoryToPaymentBatch(sourceDir.getCanonicalPath());
			if (!sourceFile.isDirectory()) {
				PaymentBatch result = directoryReader.createPaymentBatchFromFile(filenameonly);
				batchList.add(result);
			} else {
				batchList = directoryReader.readFilesInDirectory();
			}
			return batchList;
		} else {
			// TODO: Probably obsolete code
	        AdyenReport report = AdyenReportParser.createFromFile(source);
	        AdyenReportToPaymentBatch converter = AdyenReportToPaymentBatch.buildFromReport(report);
	        PaymentBatch pb = converter.getPaymentBatch(); 
	        batchList.add(pb);
			return batchList;
		}
	}

	@Override
	public String getSystemName() {
		return SYSTEMNAME;
	}
	
	
}

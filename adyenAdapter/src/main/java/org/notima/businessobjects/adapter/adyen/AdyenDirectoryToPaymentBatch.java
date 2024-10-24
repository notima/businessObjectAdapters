package org.notima.businessobjects.adapter.adyen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jline.utils.Log;
import org.notima.generic.businessobjects.BankAccountDetail;
import org.notima.generic.businessobjects.Payment.PaymentType;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;
import org.notima.adyen.AdyenReport;
import org.notima.adyen.AdyenReportParser;
import org.notima.businessobjects.adapter.paymentbatch.DirectoryPaymentBatchFactory;

/**
 * Class that takes a directory as argument, scans it for files and converts to payment batches.
 * The class looks for a property file to determine who the files belong to.
 * 
 * @author Daniel Tamm
 *
 */
public class AdyenDirectoryToPaymentBatch extends DirectoryPaymentBatchFactory {

	public static String			ADYEN_PROPERTY_FILE = "adyen.properties";
	
	public AdyenDirectoryToPaymentBatch(String directoryToRead) throws Exception {
		setSource(directoryToRead);
	}

	public AdyenDirectoryToPaymentBatch() {
		
	}
	
	public String getPropertyFile() {
		return ADYEN_PROPERTY_FILE;
	}
	
	public PaymentBatch createPaymentBatchFromFile(String file) throws IOException, Exception {
		
		AdyenReport ratepayReport = AdyenReportParser.createFromFile(directory + File.separator + file);
		ratepayReport.setCurrency(defaultCurrency);
		AdyenReportToPaymentBatch converter = AdyenReportToPaymentBatch.buildFromReport(ratepayReport);
		PaymentBatch result = converter.getPaymentBatch();
		result.setBatchOwner(taxIdentifier);
		result.setPaymentType(PaymentType.RECEIVABLE);
		BankAccountDetail bad = new BankAccountDetail();
		bad.setCurrency(defaultCurrency);
		bad.setGeneralLedgerBankAccount(generalLedgerBankAccount);
		bad.setGeneralLedgerInTransitAccount(generalLedgerInTransitAccount);
		bad.setGeneralLedgerReconciliationAccount(generalLedgerReconciliationAccount);
		bad.setGeneralLedgerFeeAccount(generalLedgerFeeAccount);
		result.setVoucherSeries(voucherSeries);
		result.setBankAccount(bad);
		result.setSource(file);
		result.setGeneralLedgerUnknownTrxAccount(generalLedgerUnknownTrxAccount);
		return result;
		
	}
	
	public String[] getFilteredFiles() {
		String[] files = directoryFile.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.toLowerCase().endsWith("xlsx"))
					return true;
				return false;
			}});
		return files;
	}
	
	@Override
	public String getSystemName() {
		return AdyenAdapter.SYSTEMNAME;
	}
	
	@Override
	public List<PaymentBatch> readPaymentBatches() {
		return readFilesInDirectory();
	}

	@Override
	public void setDestination(String dest) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PaymentBatch writePaymentBatch(PaymentBatch batch) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	
	
}

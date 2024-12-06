package org.notima.businessobjects.adapter.ratepay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.notima.businessobjects.adapter.paymentbatch.DirectoryPaymentBatchFactory;
import org.notima.generic.businessobjects.BankAccountDetail;
import org.notima.generic.businessobjects.Payment.PaymentType;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.ratepay.RatepayReport;
import org.notima.ratepay.RatepayReportParser;

/**
 * Class that takes a directory as argument, scans it for files and converts to payment batches.
 * The class looks for a property file to determine who the files belong to.
 * 
 * @author Daniel Tamm
 *
 */
public class RatepayDirectoryToPaymentBatch extends DirectoryPaymentBatchFactory {

	public static String			RATEPAY_PROPERTY_FILE = "ratepay.properties";
	
	public RatepayDirectoryToPaymentBatch(String directoryToRead) throws Exception {
		setSource(directoryToRead);
	}

	public RatepayDirectoryToPaymentBatch() {
		
	}
	
	public void setSource(String directoryToRead) throws Exception {

		channelOptions.setDirectory(directoryToRead);
		channelOptions.setTaxIdentifier(TaxSubjectIdentifier.getUndefinedIdentifier());
		checkDirectoryValid();
		checkForTaxIdentifierAndCurrency();
		
	}
	
	
	
	/**
	 * Finds all applicable files in the directory and returns them as payment batches.
	 * 
	 * @return
	 */
	public List<PaymentBatch> readFilesInDirectory() {
		List<PaymentBatch> result = new ArrayList<PaymentBatch>();

		String[] filesToRead = getFilteredFiles();
		for (String file : filesToRead) {
			try {
				result.add(createPaymentBatchFromFile(file));
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}
		
		return result;
	}
	
	public PaymentBatch createPaymentBatchFromFile(String file) throws IOException, Exception {
		
		RatepayReport ratepayReport = RatepayReportParser.createFromFile(channelOptions.getDirectory() + File.separator + file);
		ratepayReport.setCurrency(channelOptions.getDefaultCurrency());
		RatepayToPaymentBatch converter = RatepayToPaymentBatch.buildFromReport(ratepayReport);
		PaymentBatch result = converter.getPaymentBatch();
		result.setBatchOwner(channelOptions.getTaxIdentifier());
		result.setPaymentType(PaymentType.RECEIVABLE);
		BankAccountDetail bad = new BankAccountDetail();
		bad.setCurrency(channelOptions.getDefaultCurrency());
		bad.setGeneralLedgerBankAccount(channelOptions.getGeneralLedgerBankAccount());
		bad.setGeneralLedgerInTransitAccount(channelOptions.getGeneralLedgerInTransitAccount());
		bad.setGeneralLedgerReconciliationAccount(channelOptions.getGeneralLedgerReconciliationAccount());
		bad.setGeneralLedgerFeeAccount(channelOptions.getGeneralLedgerFeeAccount());
		result.setVoucherSeries(channelOptions.getVoucherSeries());
		result.setBankAccount(bad);
		result.setSource(file);
		result.setGeneralLedgerUnknownTrxAccount(channelOptions.getGeneralLedgerUnknownTrxAccount());
		return result;
		
	}
	
	public String[] getFilteredFiles() {
		String[] files = channelOptions.getDirectoryFile().list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.toLowerCase().endsWith("csv"))
					return true;
				return false;
			}});
		Arrays.sort(files);
		return files;
	}

	private void checkDirectoryValid() throws FileNotFoundException {
		File f = new File(channelOptions.getDirectory());
		if (!f.isDirectory()) {
			throw new FileNotFoundException(channelOptions.getDirectory());
		}
		channelOptions.setDirectoryFile(f);
	}
	
	/**
	 * Checks if the directory is readable and retreives directory information.
	 */
	private void checkForTaxIdentifierAndCurrency() {

		File f = new File(channelOptions.getDirectory() + File.separator + RATEPAY_PROPERTY_FILE);
		if (f.exists() && f.canRead()) {
			readPropertyFile(f);
		}
		
	}
	
	
	@Override
	public String getSystemName() {
		return RatepayAdapter.SYSTEMNAME;
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

	@Override
	public String getPropertyFile() {
		return RATEPAY_PROPERTY_FILE;
	}
	
	
}

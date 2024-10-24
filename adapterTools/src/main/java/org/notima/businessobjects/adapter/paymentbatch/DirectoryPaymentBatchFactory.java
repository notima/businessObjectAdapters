package org.notima.businessobjects.adapter.paymentbatch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for creating payment batches from a directory.
 * 
 */
public abstract class DirectoryPaymentBatchFactory implements PaymentBatchFactory {

	public Logger log = LoggerFactory.getLogger(this.getClass());		
	
	protected TaxSubjectIdentifier 		taxIdentifier;
	protected String					directory;
	protected File						directoryFile;
	protected String					defaultCurrency;
	protected String					generalLedgerBankAccount;
	protected String					generalLedgerInTransitAccount;
	protected String					generalLedgerReconciliationAccount;
	protected String					generalLedgerFeeAccount;
	protected String					generalLedgerUnknownTrxAccount;
	protected String					voucherSeries;

	/**
	 * The property file that contains the values for this reader
	 * 
	 * @return
	 */
	public abstract String getPropertyFile();
	
	public abstract PaymentBatch createPaymentBatchFromFile(String file) throws IOException, Exception;
	
	public abstract String[] getFilteredFiles();
	
	public void setSource(String directoryToRead) throws Exception {

		directory = directoryToRead;
		taxIdentifier = TaxSubjectIdentifier.getUndefinedIdentifier();
		checkDirectoryValid();
		checkForTaxIdentifierAndCurrency();
		
	}

	public TaxSubjectIdentifier getTaxIdentifier() {
		return taxIdentifier;
	}

	public void setTaxIdentifier(TaxSubjectIdentifier taxIdentifier) {
		this.taxIdentifier = taxIdentifier;
	}

	public String getDefaultCurrency() {
		return defaultCurrency;
	}

	public void setDefaultCurrency(String defaultCurrency) {
		this.defaultCurrency = defaultCurrency;
	}

	private void checkDirectoryValid() throws FileNotFoundException {
		File f = new File(directory);
		if (!f.isDirectory()) {
			throw new FileNotFoundException(directory);
		}
		directoryFile = f;
	}
	
	/**
	 * Checks if the directory is readable and retreives directory information.
	 */
	private void checkForTaxIdentifierAndCurrency() {

		File f = new File(directory + File.separator + getPropertyFile());
		if (f.exists() && f.canRead()) {
			readPropertyFile(f);
		}
		
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
	
	protected void readPropertyFile(File f) {
		
		Properties props = new Properties();
		try {
			props.load(new FileReader(f));
			
			String taxId = props.getProperty("taxId");
			String countryCode = props.getProperty("countryCode");
			defaultCurrency = props.getProperty("defaultCurrency");
			taxIdentifier = new TaxSubjectIdentifier(taxId, countryCode);
			generalLedgerBankAccount = props.getProperty("generalLedgerBankAccount");
			generalLedgerInTransitAccount = props.getProperty("generalLedgerInTransitAccount");
			generalLedgerReconciliationAccount = props.getProperty("generalLedgerReconciliationAccount");
			generalLedgerFeeAccount = props.getProperty("generalLedgerFeeAccount");
			generalLedgerUnknownTrxAccount = props.getProperty("generalLedgerUnknownTrxAccount");
			voucherSeries = props.getProperty("voucherSeries");
			logRetrievedProperties();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	protected void logRetrievedProperties() {
		// TODO: Improve logging
		if (log.isDebugEnabled()) {
			if (defaultCurrency!=null) {
				log.debug("Currency defined in %s: %s", getPropertyFile(), defaultCurrency);
			}
		}
	}
	
	
}

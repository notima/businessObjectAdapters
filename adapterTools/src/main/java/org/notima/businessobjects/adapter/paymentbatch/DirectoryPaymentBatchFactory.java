package org.notima.businessobjects.adapter.paymentbatch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.notima.generic.businessobjects.BankAccountDetail;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.PaymentBatchChannelOptions;
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

	protected PaymentBatchChannelOptions channelOptions = new PaymentBatchChannelOptions();

	/**
	 * The property file that contains the values for this reader
	 * 
	 * @return
	 */
	public abstract String getPropertyFile();
	
	public abstract PaymentBatch createPaymentBatchFromFile(String file) throws IOException, Exception;
	
	/**
	 * Creates the payment batches from a file. A file can result in more than one batch,
	 * for instance one per currency. All batches have the file as source.
	 * 
	 * Default implementation returns the batch from {@link #createPaymentBatchFromFile(String)}.
	 * 
	 * @param file	The file name (in the source directory).
	 * @return	The batches.
	 */
	public List<PaymentBatch> createPaymentBatchesFromFile(String file) throws IOException, Exception {
		List<PaymentBatch> result = new ArrayList<PaymentBatch>();
		result.add(createPaymentBatchFromFile(file));
		return result;
	}
	
	public abstract String[] getFilteredFiles();
	
	public void setSource(String directoryToRead) throws Exception {

		channelOptions.setDirectory(directoryToRead);
		channelOptions.setTaxIdentifier(TaxSubjectIdentifier.getUndefinedIdentifier());
		checkDirectoryValid();
		checkForTaxIdentifierAndCurrency();
		
	}

	public PaymentBatchChannelOptions getChannelOptions() {
		return channelOptions;
	}
	
	public TaxSubjectIdentifier getTaxIdentifier() {
		return channelOptions.getTaxIdentifier();
	}

	public void setTaxIdentifier(TaxSubjectIdentifier taxIdentifier) {
		channelOptions.setTaxIdentifier(taxIdentifier);
	}

	public String getDefaultCurrency() {
		return channelOptions.getDefaultCurrency();
	}

	public void setDefaultCurrency(String defaultCurrency) {
		channelOptions.setDefaultCurrency(defaultCurrency);
	}

	/**
	 * Sets the bank account and general ledger accounts of the batch using the
	 * channel's accounts for given currency.
	 * 
	 * @param batch		The batch to update.
	 * @param currency	The currency of the batch. If null, the default currency is used.
	 * @throws org.notima.generic.businessobjects.UnknownCurrencyAccountException	If the channel has no account defined for the currency.
	 */
	protected void setAccountsForCurrency(PaymentBatch batch, String currency) {
		String c = currency!=null ? currency : channelOptions.getDefaultCurrency();
		BankAccountDetail bad = new BankAccountDetail();
		bad.setCurrency(c);
		bad.setGeneralLedgerBankAccount(channelOptions.getGeneralLedgerBankAccount(c));
		bad.setGeneralLedgerInTransitAccount(channelOptions.getGeneralLedgerInTransitAccount(c));
		bad.setGeneralLedgerReconciliationAccount(channelOptions.getGeneralLedgerReconciliationAccount(c));
		bad.setGeneralLedgerFeeAccount(channelOptions.getGeneralLedgerFeeAccount(c));
		batch.setBankAccount(bad);
		batch.setGeneralLedgerUnknownTrxAccount(channelOptions.getGeneralLedgerUnknownTrxAccount(c));
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

		File f = new File(channelOptions.getDirectory() + File.separator + getPropertyFile());
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
				result.addAll(createPaymentBatchesFromFile(file));
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
			channelOptions.setDefaultCurrency(props.getProperty("defaultCurrency"));
			channelOptions.setTaxIdentifier(new TaxSubjectIdentifier(taxId, countryCode));
			channelOptions.setGeneralLedgerBankAccount(props.getProperty("generalLedgerBankAccount"));
			channelOptions.setGeneralLedgerInTransitAccount(props.getProperty("generalLedgerInTransitAccount"));
			channelOptions.setGeneralLedgerReconciliationAccount(props.getProperty("generalLedgerReconciliationAccount"));
			channelOptions.setGeneralLedgerFeeAccount(props.getProperty("generalLedgerFeeAccount"));
			channelOptions.setGeneralLedgerUnknownTrxAccount(props.getProperty("generalLedgerUnknownTrxAccount"));
			channelOptions.setVoucherSeries(props.getProperty("voucherSeries"));
			channelOptions.setSourceReference(props.getProperty("sourceReference"));
			channelOptions.setSourceReferenceRegex(props.getProperty("sourceReferenceRegex"));
			channelOptions.setDestinationReference(props.getProperty("destinationReference"));
			channelOptions.setDestinationReferenceRegex(props.getProperty("destinationReferenceRegex"));
			logRetrievedProperties();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	protected void logRetrievedProperties() {
		// TODO: Improve logging
		if (log.isDebugEnabled()) {
			if (channelOptions.getDefaultCurrency()!=null) {
				log.debug("Currency defined in %s: %s", getPropertyFile(), channelOptions.getDefaultCurrency());
			}
		}
	}
	
	
}

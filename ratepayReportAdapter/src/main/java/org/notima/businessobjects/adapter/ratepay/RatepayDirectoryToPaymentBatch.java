package org.notima.businessobjects.adapter.ratepay;

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
import org.notima.ratepay.RatepayReport;
import org.notima.ratepay.RatepayReportParser;

/**
 * Class that takes a directory as argument, scans it for files and converts to payment batches.
 * The class looks for a property file to determine who the files belong to.
 * 
 * @author Daniel Tamm
 *
 */
public class RatepayDirectoryToPaymentBatch implements PaymentBatchFactory {

	public static String			RATEPAY_PROPERTY_FILE = "ratepay.properties";
	
	private TaxSubjectIdentifier 	taxIdentifier;
	private String					directory;
	private File					directoryFile;
	private String					defaultCurrency;
	
	public RatepayDirectoryToPaymentBatch(String directoryToRead) throws Exception {
		setSource(directoryToRead);
	}

	public RatepayDirectoryToPaymentBatch() {
		
	}
	
	public void setSource(String directoryToRead) throws Exception {

		directory = directoryToRead;
		taxIdentifier = TaxSubjectIdentifier.getUndefinedIdentifier();
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

		String[] filesToRead = getCsvFiles();
		for (String file : filesToRead) {
			try {
				result.add(createPaymentBatchFromFile(file));
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}
		
		return result;
	}
	
	private PaymentBatch createPaymentBatchFromFile(String file) throws IOException, Exception {
		
		RatepayReport ratepayReport = RatepayReportParser.createFromFile(directory + File.separator + file);
		ratepayReport.setCurrency(defaultCurrency);
		RatepayToPaymentBatch converter = RatepayToPaymentBatch.buildFromReport(ratepayReport);
		PaymentBatch result = converter.getPaymentBatch();
		result.setBatchOwner(taxIdentifier);
		result.setPaymenType(PaymentType.RECEIVABLE);
		BankAccountDetail bad = new BankAccountDetail();
		bad.setCurrency(defaultCurrency);
		result.setBankAccount(bad);
		result.setSource(file);
		return result;
		
	}
	
	private String[] getCsvFiles() {
		String[] files = directoryFile.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if (name.toLowerCase().endsWith("csv"))
					return true;
				return false;
			}});
		return files;
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

		File f = new File(directory + File.separator + RATEPAY_PROPERTY_FILE);
		if (f.exists() && f.canRead()) {
			readRatepayPropertyFile(f);
		}
		
	}
	
	private void readRatepayPropertyFile(File f) {
		
		Properties props = new Properties();
		try {
			props.load(new FileReader(f));
			
			String taxId = props.getProperty("taxId");
			String countryCode = props.getProperty("countryCode");
			defaultCurrency = props.getProperty("defaultCurrency");
			taxIdentifier = new TaxSubjectIdentifier(taxId, countryCode);
			logRetrievedProperties();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void logRetrievedProperties() {
		// TODO: Improve logging
		if (RatepayAdapter.log.isDebugEnabled()) {
			if (defaultCurrency!=null) {
				Log.debug("Currency defined in ratepay.properties: %s",  defaultCurrency);
			}
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
	
	
}

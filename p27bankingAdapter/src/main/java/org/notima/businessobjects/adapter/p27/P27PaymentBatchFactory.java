package org.notima.businessobjects.adapter.p27;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.notima.businessobjects.adapter.paymentbatch.DirectoryPaymentBatchFactory;
import org.notima.generic.businessobjects.BankAccountDetail;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.iso20022.camt.CamtDocument;
import org.notima.iso20022.camt.CamtReader;

/**
 * Reads the ISO 20022 bank files (camt.052, camt.053, camt.054) of a directory into
 * payment batches. Settings (tax id, accounts, voucher series, reference matching)
 * are read from {@link #P27_PROPERTY_FILE} in the directory.
 * 
 * A file results in one batch of incoming payments and one of outgoing payments per account.
 * 
 * @author Daniel Tamm
 *
 */
public class P27PaymentBatchFactory extends DirectoryPaymentBatchFactory {

	public static String P27_PROPERTY_FILE = "p27.properties";

	public P27PaymentBatchFactory(String directoryToRead) throws Exception {
		setSource(directoryToRead);
	}

	public P27PaymentBatchFactory() {

	}

	@Override
	public String getPropertyFile() {
		return P27_PROPERTY_FILE;
	}

	@Override
	public List<PaymentBatch> createPaymentBatchesFromFile(String file) throws IOException, Exception {

		CamtDocument doc = new CamtReader().read(new File(channelOptions.getDirectory() + File.separator + file));

		CamtToPaymentBatch converter = new CamtToPaymentBatch();
		converter.setDestinationReferenceField(channelOptions.getDestinationReference());
		converter.setDestinationReferenceRegex(channelOptions.getDestinationReferenceRegex());
		converter.setSourceReferenceRegex(channelOptions.getSourceReferenceRegex());

		List<PaymentBatch> result = converter.convert(doc);
		for (PaymentBatch batch : result) {
			// Keep the bank account identity from the file, add the channel's ledger accounts.
			BankAccountDetail fromFile = batch.getBankAccount();
			setAccountsForCurrency(batch, fromFile.getCurrency());
			BankAccountDetail bad = batch.getBankAccount();
			bad.setAccountNo(fromFile.getAccountNo());
			bad.setIBAN(fromFile.getIBAN());
			bad.setBIC(fromFile.getBIC());
			bad.setBankName(fromFile.getBankName());
			batch.setBatchOwner(channelOptions.getTaxIdentifier());
			batch.setVoucherSeries(channelOptions.getVoucherSeries());
			batch.setSource(file);
		}
		return result;
	}

	/**
	 * Returns the first batch of the file. Use {@link #createPaymentBatchesFromFile(String)}
	 * since a file normally results in more than one batch.
	 */
	@Override
	public PaymentBatch createPaymentBatchFromFile(String file) throws IOException, Exception {
		List<PaymentBatch> batches = createPaymentBatchesFromFile(file);
		return batches.isEmpty() ? null : batches.get(0);
	}

	@Override
	public String[] getFilteredFiles() {
		String[] files = channelOptions.getDirectoryFile().list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				String n = name.toLowerCase();
				return n.endsWith(".xml") || n.endsWith(".xml.txt");
			}});
		Arrays.sort(files);
		return files;
	}

	@Override
	public String getSystemName() {
		return P27Adapter.SYSTEMNAME;
	}

	@Override
	public List<PaymentBatch> readPaymentBatches() {
		return readFilesInDirectory();
	}

	@Override
	public void setDestination(String dest) throws Exception {
		// Writing payment files (pain.001) is not supported yet.
	}

	@Override
	public PaymentBatch writePaymentBatch(PaymentBatch batch) throws Exception {
		// Writing payment files (pain.001) is not supported yet.
		return null;
	}

}

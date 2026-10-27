package org.notima.businessobjects.adapter.p27;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads ISO 20022 bank files (camt.052, camt.053, camt.054) into payment batches.
 * 
 * The source can be a single file or a directory. Settings are read from
 * {@link P27PaymentBatchFactory#P27_PROPERTY_FILE} in the file's directory.
 */
public class P27Adapter implements PaymentFactory {

	public static String SYSTEMNAME = "P27";

	public static Logger log = LoggerFactory.getLogger(P27Adapter.class);

	@Override
	public List<PaymentBatch> readPaymentBatchesFromSource(String source) throws Exception {

		File sourceFile = new File(source);
		if (sourceFile.isDirectory()) {
			P27PaymentBatchFactory directoryReader = new P27PaymentBatchFactory(sourceFile.getCanonicalPath());
			return directoryReader.readFilesInDirectory();
		}
		List<PaymentBatch> batchList = new ArrayList<PaymentBatch>();
		if (sourceFile.exists()) {
			P27PaymentBatchFactory directoryReader = new P27PaymentBatchFactory(sourceFile.getCanonicalFile().getParent());
			batchList.addAll(directoryReader.createPaymentBatchesFromFile(sourceFile.getName()));
		}
		return batchList;
	}

	@Override
	public String getSystemName() {
		return SYSTEMNAME;
	}

}

package org.notima.adyen.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.notima.businessobjects.adapter.adyen.AdyenDirectoryToPaymentBatch;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.exception.CurrencyMismatchException;
import org.notima.generic.businessobjects.exception.DateMismatchException;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;

public class TestAdyenDirectoryToPaymentBatch {

	private List<PaymentBatch> batches;
	private File reportsDirectory;
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		
		try {
			getReportsDirectory();
			createBatches();
			createPayouts();
			TestUtil.createGson().toJson(batches, System.out);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	private void getReportsDirectory() {
		String cwd = System.getProperty("user.dir");
		reportsDirectory = new File(cwd + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "reports");
		if (!reportsDirectory.exists()) {
			fail("Testing directory src/test/resources/reports can't be located");
		}
	}
	
	private void createBatches() throws Exception {
		PaymentBatchFactory batchFactory = new AdyenDirectoryToPaymentBatch();
		batchFactory.setSource(reportsDirectory.getAbsolutePath());
		batches = batchFactory.readPaymentBatches();
		System.out.println(batches.size() + " batches created.");
	}

	private void createPayouts() throws DateMismatchException, CurrencyMismatchException {
		for (PaymentBatch batch : batches) {
			
			batch.retrievePayoutLines();
			
		}
	}
	

}

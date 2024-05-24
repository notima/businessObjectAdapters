package org.notima.adyen.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.notima.businessobjects.adapter.adyen.AdyenDirectoryToPaymentBatch;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;

public class TestAdyenDirectoryToPaymentBatch {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		
		String cwd = System.getProperty("user.dir");
		File reportsDirectory = new File(cwd + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "reports");
		if (!reportsDirectory.exists()) {
			fail("Testing directory src/test/resources/reports can't be located");
		}
		try {
			PaymentBatchFactory batchFactory = new AdyenDirectoryToPaymentBatch();
			batchFactory.setSource(reportsDirectory.getAbsolutePath());
			List<PaymentBatch> batches = batchFactory.readPaymentBatches();
			System.out.println(batches.size() + " batches created.");
			TestUtil.createGson().toJson(batches, System.out);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
		

}

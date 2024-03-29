package org.notima.businessobjects.adapter.ratepay;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;

public class TestRatepayDirectoryToPaymentBatch {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testRatepayPaymentBatchFactory() {
		
		String cwd = System.getProperty("user.dir");
		File reportsDirectory = new File(cwd + File.separator + "src" + File.separator + "test" + File.separator + "resources" + File.separator + "reports");
		if (!reportsDirectory.exists()) {
			fail("Testing directory src/test/resources/reports can't be located");
		}
		try {
			PaymentBatchFactory batchFactory = new RatepayDirectoryToPaymentBatch();
			batchFactory.setSource(reportsDirectory.getAbsolutePath());
			List<PaymentBatch> batches = batchFactory.readPaymentBatches();
			System.out.println(batches.size() + " batches created.");
			TestRatepayUtil.createGson().toJson(batches, System.out);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}

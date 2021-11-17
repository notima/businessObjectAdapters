package org.notima.businessobjects.adapter.ratepay;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.ratepay.RatepayReport;

public class TestRatepayToPaymentBatch {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		try {
			
			RatepayReport ratepayReport =  TestRatepayUtil.getTestReport();
			RatepayToPaymentBatch canonical = RatepayToPaymentBatch.buildFromReport(ratepayReport);
			PaymentBatch paymentBatch = canonical.getPaymentBatch();
			System.out.println(paymentBatch.toString());
			
		} catch (Exception ee) {
			fail(ee.getMessage());			
		}
	}

}

package org.notima.businessobjects.adapter.ratepay;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.ratepay.RatepayReportRow;

public class TestRatepayToPaymentBatch {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		try {
			
			List<RatepayReportRow> ratepayReport =  TestRatepayUtil.getTestReport();
			RatepayToCanonicalPayment canonical = RatepayToCanonicalPayment.buildFromList(ratepayReport);
			PaymentBatch paymentBatch = canonical.getPaymentBatch();
			System.out.println(paymentBatch.toString());
			
		} catch (Exception ee) {
			fail(ee.getMessage());			
		}
	}

}

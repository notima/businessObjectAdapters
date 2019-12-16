package com.svea.businessobjects.test;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.notima.generic.businessobjects.Order;

public class TestLookupOrder extends TestSveaAdminBusinessObjectFactory {

	@Test
	public void testLookupOrder() {
		try {
			
			String orderNo = testProperties.getProperty("test.order", "1");
			Order<com.svea.webpayadminservice.client.Order> result = bof.lookupOrder(orderNo);
			log.info("Lookup of order: {} resulted in Webpay order: {} ", orderNo, result.getDocumentKey());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	

}

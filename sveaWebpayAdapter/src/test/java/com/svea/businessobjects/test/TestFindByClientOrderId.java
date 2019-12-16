package com.svea.businessobjects.test;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.notima.generic.businessobjects.Order;

public class TestFindByClientOrderId extends TestSveaAdminBusinessObjectFactory {
	
	@Test
	public void testfindByClientOrderId() {
		try {
			String clientOrderId = testProperties.getProperty("test.client.orderId", "1");
			Order<com.svea.webpayadminservice.client.Order> result = bof.findByClientOrderId(clientOrderId);
			log.info("Lookup of client order id: {} returned Webpay Order: {}", clientOrderId, result.getNativeOrder().getSveaOrderId());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	

}

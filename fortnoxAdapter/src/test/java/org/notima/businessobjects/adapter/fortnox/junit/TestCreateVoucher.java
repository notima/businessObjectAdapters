package org.notima.businessobjects.adapter.fortnox.junit;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxException;
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.businessobjects.adapter.fortnox.FortnoxConverter;

public class TestCreateVoucher extends FortnoxAdapterTestBase {

	/**
	 * This test is excluded from the automatic test suite because it tries to create a voucher.
	 * 
	 *  To test this, run it manually as a single unit test.
	 * 
	 */

	@Test
	public void testCreateVoucher() {
		try {
			
			Voucher voucher = new FortnoxConverter().createSingleTransactionVoucher("A", null, "1580", "1799",
					1000, "fortnoxAdapter - Testar");

			
			FortnoxClient3 client = (FortnoxClient3)factory.getClient();
			voucher = client.setVoucher(voucher);
			
			log.info("Created voucher " + voucher.getVoucherNumber());
			
		} catch (FortnoxException fe) {
			
			log.info(fe.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	
}

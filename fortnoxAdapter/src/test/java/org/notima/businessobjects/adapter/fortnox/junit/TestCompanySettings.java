package org.notima.businessobjects.adapter.fortnox.junit;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.notima.generic.businessobjects.BusinessPartner;

public class TestCompanySettings extends FortnoxAdapterTestBase {

	@SuppressWarnings("unchecked")
	@Test
	public void testLookupCompanySettings() {
		
		try {
			BusinessPartner<org.notima.api.fortnox.entities3.Customer> bp = factory.lookupThisCompanyInformation();
			log.info("Running tests on database: {} : {}", bp.getIdentityNo(), bp.getName());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	
}

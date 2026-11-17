package org.notima.businessobjects.adapter.fortnox.junit;

import static org.junit.Assert.*;

import org.junit.Test;
import org.notima.api.fortnox.entities3.Customer;
import org.notima.api.fortnox.entities3.Supplier;
import org.notima.businessobjects.adapter.fortnox.FortnoxCustomerSync;
import org.notima.businessobjects.adapter.fortnox.FortnoxSupplierSync;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.sync.BusinessPartnerSyncProfile;

public class TestFortnoxPartnerSync {

	private static BusinessPartner<Object> partner() {
		BusinessPartner<Object> bp = new BusinessPartner<Object>();
		bp.setName("Acme AB");
		bp.setTaxId("5566778899");
		bp.setIsCustomer(true);
		bp.setIsVendor(true);
		bp.setPaymentTermKey("30");
		bp.setWebsite("www.acme.se");
		Location a = new Location();
		a.setAddress1("Gatan 1");
		a.setPostal("123 45");
		a.setCity("Staden");
		a.setEmail("info@acme.se");
		a.setPhone("08-123 45");
		bp.setAddressOfficial(a);
		bp.setRemitToAccountType("BG");
		bp.setRemitToAccount("123-4567");
		return bp;
	}

	@Test
	public void customerRoundTripKeepsFingerprint() throws Exception {
		BusinessPartner<Object> bp = partner();
		Customer c = FortnoxCustomerSync.toFortnox(bp, false);
		assertEquals("556677-8899", c.getOrganisationNumber());
		assertNull("Not sent when creating", c.getAddress2());
		BusinessPartner<Customer> back = FortnoxCustomerSync.toCanonical(c);
		BusinessPartnerSyncProfile p = new BusinessPartnerSyncProfile(false).withDefaults("SEK", "SE");
		assertEquals(p.fingerprint(bp), p.fingerprint(back));
		assertTrue(back.getIsCustomer());
	}

	@Test
	public void updateClearsEmptyFields() throws Exception {
		Customer c = FortnoxCustomerSync.toFortnox(partner(), true);
		assertEquals("", c.getAddress2());
		assertNull("Empty currency is Fortnox's default, not cleared", c.getCurrency());
	}

	@Test
	public void supplierRoundTripKeepsWhereToPay() throws Exception {
		BusinessPartner<Object> bp = partner();
		Supplier s = FortnoxSupplierSync.toFortnox(bp, false);
		assertEquals("123-4567", s.getBG());
		assertNull(s.getPG());
		BusinessPartner<Supplier> back = FortnoxSupplierSync.toCanonical(s);
		BusinessPartnerSyncProfile p = new BusinessPartnerSyncProfile(true).withDefaults("SEK", "SE");
		assertEquals(p.fingerprint(bp), p.fingerprint(back));
		assertTrue(back.getIsVendor());
	}

	@Test
	public void numberGoesWithNewPartner() throws Exception {
		BusinessPartner<Object> bp = partner();
		bp.setIdentityNo("1001");
		assertEquals("1001", FortnoxCustomerSync.toFortnox(bp, false).getCustomerNumber());
		assertNull("Not changed on update", FortnoxCustomerSync.toFortnox(bp, true).getCustomerNumber());
		assertNull("A customer too: the supplier register has its own series", FortnoxSupplierSync.toFortnox(bp, false).getSupplierNumber());
		bp.setIsCustomer(false);
		assertEquals("1001", FortnoxSupplierSync.toFortnox(bp, false).getSupplierNumber());
		Customer c = FortnoxCustomerSync.toFortnox(bp, false);
		assertEquals("1001", FortnoxCustomerSync.toCanonical(c).getIdentityNo());
	}
}

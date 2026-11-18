package org.notima.businessobjects.adapter.fortnox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerFileData;
import org.notima.generic.businessobjects.Location;

public class FortnoxSupplierFileImporterTest {

	private File resourceFile(String name) throws Exception {
		return new File(getClass().getResource("/" + name).toURI());
	}

	@Test
	public void testCanImport() throws Exception {
		FortnoxSupplierFileImporter importer = new FortnoxSupplierFileImporter();
		assertTrue(importer.canImport(resourceFile("fortnox_leverantorsregister_sample.csv")));
		assertFalse(importer.canImport(resourceFile("fortnox_kundregister_sample.csv")));
		assertFalse(new FortnoxCustomerFileImporter().canImport(resourceFile("fortnox_leverantorsregister_sample.csv")));
	}

	@Test
	public void testImport() throws Exception {
		BusinessPartnerFileData data = new FortnoxSupplierFileImporter()
				.importFile(resourceFile("fortnox_leverantorsregister_sample.csv"));

		assertEquals("Fortnox supplier register", data.getSourceFormat());
		assertEquals("Fortnox", data.getSourceSystem());
		List<BusinessPartner<?>> partners = data.getBusinessPartners();
		assertEquals(3, partners.size());
		for (BusinessPartner<?> bp : partners) {
			assertEquals(Boolean.TRUE, bp.getIsVendor());
			assertNull(bp.getIsCustomer());
			assertTrue(bp.isCompany());
		}

		// Comma in the name, bankgiro preferred over plusgiro
		BusinessPartner<?> s = partners.get(0);
		assertEquals("Exempel, Bygg & Co AB", s.getName());
		assertEquals("12", s.getIdentityNo());
		assertEquals("5560000002", s.getTaxId());
		assertEquals(Boolean.TRUE, s.getActive());
		assertEquals("BG", s.getRemitToAccountType());
		assertEquals("123-4567", s.getRemitToAccount());
		assertEquals("SEK", s.getCurrency());
		assertEquals("30", s.getPaymentTermKey());
		assertEquals("www.exempel.se", s.getWebsite());
		assertEquals("Ramavtal", s.getComments());
		assertEquals("SE", s.getCountryCode());
		Location a = s.getAddressOfficial();
		assertEquals("Storgatan 1", a.getAddress1());
		assertEquals("Box 12", a.getAddress2());
		assertEquals("111 22", a.getPostal());
		assertEquals("STOCKHOLM", a.getCity());
		assertEquals("faktura@exempel.se", a.getEmail());
		assertEquals("08-123456", a.getPhone());

		// Plusgiro, reverse charge, a number that isn't only digits
		BusinessPartner<?> pg = partners.get(1);
		assertEquals("5287-3312", pg.getIdentityNo());
		assertEquals("PG", pg.getRemitToAccountType());
		assertEquals("4749702-9", pg.getRemitToAccount());
		assertEquals("REVERSE", pg.getVatType());
		assertNull(pg.getAddressOfficial().getAddress1());
		assertNull(pg.getPaymentTermKey());

		// Inactive foreign supplier without org no, paid by IBAN
		BusinessPartner<?> foreign = partners.get(2);
		assertNull(foreign.getTaxId());
		assertEquals(Boolean.FALSE, foreign.getActive());
		assertEquals("FI", foreign.getCountryCode());
		assertEquals("EUR", foreign.getCurrency());
		assertEquals("FI12345678", foreign.getVatNo());
		assertEquals("FI2112345600000785", foreign.getRemitToIBAN());
		assertEquals("NDEAFIHH", foreign.getRemitToBIC());
		assertNull(foreign.getRemitToAccountType());
	}

}

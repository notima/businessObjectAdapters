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

public class FortnoxCustomerFileImporterTest {

	private File resourceFile(String name) throws Exception {
		return new File(getClass().getResource("/" + name).toURI());
	}

	@Test
	public void testCanImport() throws Exception {
		FortnoxCustomerFileImporter importer = new FortnoxCustomerFileImporter();
		assertTrue(importer.canImport(resourceFile("fortnox_kundregister_sample.csv")));
		assertFalse(importer.canImport(resourceFile("Report_AR_20241231.txt")));
	}

	@Test
	public void testImport() throws Exception {
		BusinessPartnerFileData data = new FortnoxCustomerFileImporter()
				.importFile(resourceFile("fortnox_kundregister_sample.csv"));

		assertEquals("Fortnox customer register", data.getSourceFormat());
		List<BusinessPartner<?>> partners = data.getBusinessPartners();
		assertEquals(3, partners.size());
		for (BusinessPartner<?> bp : partners) {
			assertEquals(Boolean.TRUE, bp.getIsCustomer());
		}

		// Company with a comma in the name, invoice and delivery address
		BusinessPartner<?> company = partners.get(0);
		assertEquals("Exempel, Bygg & Co AB", company.getName());
		assertEquals("1001", company.getIdentityNo());
		assertEquals("5560000001", company.getTaxId());
		assertEquals("SE556000000101", company.getVatNo());
		assertTrue(company.isCompany());
		assertEquals(Boolean.TRUE, company.getActive());
		assertTrue(company.isEmailInvoice());
		assertEquals("SE", company.getCountryCode());
		assertEquals("SEK", company.getCurrency());
		assertEquals("30", company.getPaymentTermKey());
		assertEquals("SEVAT", company.getVatType());
		assertEquals("7350000000001", company.getGln());
		assertEquals("0007:5560000001", company.getPeppolId());
		assertEquals("www.exempel.se", company.getWebsite());

		Location official = company.getAddressOfficial();
		assertEquals("Storgatan 1", official.getAddress1());
		assertEquals("Box 12", official.getAddress2());
		assertEquals("111 22", official.getPostal());
		assertEquals("Stockholm", official.getCity());
		assertEquals("faktura@exempel.se", official.getEmail());
		assertEquals("08-123456", official.getPhone());

		Location shipping = company.getAddressShipping();
		assertEquals("Lager", shipping.getName());
		assertEquals("Hamnvägen 5", shipping.getAddress1());
		assertEquals("Göteborg", shipping.getCity());

		// Private person with quotes in the name, no delivery address
		BusinessPartner<?> person = partners.get(1);
		assertEquals("Anna \"Testsson\" Andersson", person.getName());
		assertFalse(person.isCompany());
		assertFalse(person.isEmailInvoice());
		assertNull(person.getAddressShipping());
		assertNull(person.getAddressOfficial().getEmail());
		assertNull(person.getCurrency());
		assertNull(person.getPaymentTermKey());
		assertNull(person.getGln());

		// Inactive foreign company
		BusinessPartner<?> foreign = partners.get(2);
		assertEquals(Boolean.FALSE, foreign.getActive());
		assertEquals("FI", foreign.getCountryCode());
		assertEquals("EUR", foreign.getCurrency());
		assertEquals("EUREVERSEDVAT", foreign.getVatType());
	}

}

package org.notima.businessobjects.adapter.fortnox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceFileData;

public class FortnoxReportInvoiceImporterTest {

	private File resourceFile(String name) throws Exception {
		return new File(getClass().getResource("/" + name).toURI());
	}

	@Test
	public void testCanImport() throws Exception {
		File ap = resourceFile("Report_AP_20241231.txt");
		File ar = resourceFile("Report_AR_20241231.txt");

		assertTrue(new FortnoxApReportImporter().canImport(ap));
		assertFalse(new FortnoxApReportImporter().canImport(ar));
		assertTrue(new FortnoxArReportImporter().canImport(ar));
		assertFalse(new FortnoxArReportImporter().canImport(ap));
	}

	@Test
	public void testApImport() throws Exception {
		InvoiceFileData data = new FortnoxApReportImporter().importFile(resourceFile("Report_AP_20241231.txt"));

		assertEquals(LocalDate.of(2024, 12, 31), data.getAsOfDate());
		assertTrue(data.isOpenItemsOnly());
		assertFalse(data.getInvoices().isEmpty(), "AP report should contain invoices");
		for (Invoice<?> inv : data.getInvoices()) {
			assertFalse(inv.isSalesTransaction(), "AP invoices must not be sales transactions");
			assertNotNull(inv.getInvoiceKey());
			assertNotNull(inv.getBusinessPartner());
			assertEquals(Boolean.TRUE, inv.getBusinessPartner().getIsVendor());
			assertNotNull(inv.getInvoiceDate());
			assertNotNull(inv.getDueDate());
		}

		// First line: 10 Sparbank AB, löpnr 1016, blank OCR, belopp 5 479,00, saldo 53,00
		Invoice<?> first = data.getInvoices().get(0);
		assertEquals("1016", first.getInvoiceKey());
		assertNull(first.getOcr());
		assertEquals("Sparbank AB", first.getBusinessPartner().getName());
		assertEquals("10", first.getBusinessPartner().getIdentityNo());
		assertEquals("SEK", first.getCurrency());
		assertEquals(5479.0, first.getGrandTotal(), 0.001);
		assertEquals(53.0, first.getOpenAmt(), 0.001);
	}

	@Test
	public void testArImport() throws Exception {
		InvoiceFileData data = new FortnoxArReportImporter().importFile(resourceFile("Report_AR_20241231.txt"));

		assertEquals(LocalDate.of(2024, 12, 31), data.getAsOfDate());
		assertFalse(data.getInvoices().isEmpty(), "AR report should contain invoices");
		for (Invoice<?> inv : data.getInvoices()) {
			assertTrue(inv.isSalesTransaction(), "AR invoices must be sales transactions");
			assertNotNull(inv.getBusinessPartner());
			assertEquals(Boolean.TRUE, inv.getBusinessPartner().getIsCustomer());
			assertNotNull(inv.getInvoiceDate());
			assertNotNull(inv.getDueDate());
		}

		// First line: 101 Scene Concept Sweden AB, invoice 2459, 2 813,00 open
		Invoice<?> first = data.getInvoices().get(0);
		assertEquals("2459", first.getInvoiceKey());
		assertEquals("Scene Concept Sweden AB", first.getBusinessPartner().getName());
		assertEquals(2813.0, first.getGrandTotal(), 0.001);
		assertEquals(2813.0, first.getOpenAmt(), 0.001);
	}

	/** When all invoices are in SEK, Fortnox leaves out the columns "Belopp i SEK" and "Saldo i SEK". */
	@Test
	public void testApImportWithoutSekColumns() throws Exception {
		InvoiceFileData data = new FortnoxApReportImporter().importFile(resourceFile("Report_AP_sek_only.txt"));
		assertEquals(LocalDate.of(2026, 11, 18), data.getAsOfDate());
		assertEquals(2, data.getInvoices().size());
		Invoice<?> first = data.getInvoices().get(0);
		assertEquals("46", first.getInvoiceKey());
		assertNull(first.getOcr());
		assertEquals("SEK", first.getCurrency());
		assertEquals(2129.0, first.getGrandTotal(), 0.001);
		assertEquals(1000.0, first.getOpenAmt(), 0.001);
		assertEquals("4749702-9", first.getBusinessPartner().getIdentityNo());
		assertEquals("D 23", org.notima.generic.businessobjects.settlement.PostedInvoices.sourceVoucherNo(first));
		assertFalse(org.notima.generic.businessobjects.settlement.PostedInvoices.isPosted(first), "only a hint");
		assertEquals("Exempel Finans AB", first.getBusinessPartner().getName());
		Invoice<?> second = data.getInvoices().get(1);
		assertEquals("12345", second.getOcr());
		assertEquals(2145.0, second.getOpenAmt(), 0.001);
	}

	@Test
	public void testArImportWithoutSekColumns() throws Exception {
		assertTrue(new FortnoxArReportImporter().canImport(resourceFile("Report_AR_sek_only.txt")));
		InvoiceFileData data = new FortnoxArReportImporter().importFile(resourceFile("Report_AR_sek_only.txt"));
		assertEquals(1, data.getInvoices().size());
		Invoice<?> inv = data.getInvoices().get(0);
		assertEquals("1001", inv.getInvoiceKey());
		assertEquals(1250.0, inv.getGrandTotal(), 0.001);
		assertEquals(250.0, inv.getOpenAmt(), 0.001);
		assertEquals("B 5", org.notima.generic.businessobjects.settlement.PostedInvoices.sourceVoucherNo(inv));
	}

}

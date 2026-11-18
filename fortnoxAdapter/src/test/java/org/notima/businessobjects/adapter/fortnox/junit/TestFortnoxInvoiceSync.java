package org.notima.businessobjects.adapter.fortnox.junit;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.notima.api.fortnox.entities3.InvoiceRow;
import org.notima.api.fortnox.entities3.SupplierInvoice;
import org.notima.api.fortnox.entities3.SupplierInvoiceRow;
import org.notima.businessobjects.adapter.fortnox.FortnoxCustomerInvoiceSync;
import org.notima.businessobjects.adapter.fortnox.FortnoxSupplierInvoiceSync;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.businessobjects.sync.InvoiceSyncProfile;
import org.notima.generic.businessobjects.sync.SyncLink;

public class TestFortnoxInvoiceSync {

	private static BusinessPartner<Object> partner(String link, String number) {
		BusinessPartner<Object> bp = new BusinessPartner<Object>();
		bp.setName("Acme AB");
		bp.setTaxId("5566778899");
		if (link!=null) bp.putSyncLink(new SyncLink(link, number));
		return bp;
	}

	private static Invoice<Object> invoice(BusinessPartner<?> bp, boolean sales) throws Exception {
		Invoice<Object> inv = new Invoice<Object>();
		inv.setSalesTransaction(sales);
		inv.setInvoiceKey("F-1");
		inv.setInvoiceDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-03-01"));
		inv.setDueDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-03-31"));
		inv.setBusinessPartner(bp);
		InvoiceLine l = new InvoiceLine();
		l.setDescription("Konsulttjänster mars");
		l.setQtyEntered(10);
		l.setUOM("h");
		l.setPriceActual(950.0);
		l.setTaxPercent(25);
		if (!sales) l.setAccountNo("6550");
		inv.addInvoiceLine(l);
		inv.calculateGrandTotal();
		return inv;
	}

	@Test
	public void customerInvoiceRoundTripKeepsFingerprintWithWhatFortnoxAssigned() throws Exception {
		FortnoxCustomerInvoiceSync h = new FortnoxCustomerInvoiceSync(null);
		Invoice<Object> local = invoice(partner("Fortnox/customer", "10"), true);
		org.notima.api.fortnox.entities3.Invoice fx = h.toFortnox(local);
		assertEquals("10", fx.getCustomerNumber());
		assertNull("Without an account, Fortnox's default", fx.getInvoiceRows().getInvoiceRow().get(0).getAccountNumber());

		// What Fortnox returns
		fx.setDocumentNumber("1001");
		fx.setOCR("100178");
		fx.setCustomerName("Acme AB");
		fx.setOrganisationNumber("556677-8899");
		InvoiceRow r = fx.getInvoiceRows().getInvoiceRow().get(0);
		r.setAccountNumber("3041");
		FortnoxCustomerInvoiceSync.takeAssigned(local, fx);
		assertEquals("1001", local.getInvoiceKey());
		assertEquals("3041", local.getLines().get(0).getAccountNo());

		Invoice<?> back = h.toCanonical(null, "556000-0000", fx);
		InvoiceSyncProfile p = new InvoiceSyncProfile(false).withDefaults("SEK");
		assertEquals(p.fingerprint(local), p.fingerprint(back));
		assertEquals("10", back.getBusinessPartner().getSyncLink("Fortnox/customer").getRemoteKey());
	}

	@Test(expected = Exception.class)
	public void customerNotLinkedIsRefused() throws Exception {
		new FortnoxCustomerInvoiceSync(null).toFortnox(invoice(partner(null, null), true));
	}

	@Test
	public void supplierInvoiceIsSentAsRowsAndReadBackWithoutGeneratedRows() throws Exception {
		FortnoxSupplierInvoiceSync h = new FortnoxSupplierInvoiceSync(null);
		Invoice<Object> local = invoice(partner("Fortnox/supplier", "7"), false);
		InvoiceSyncProfile.setVendorInvoiceNo(local, "A-77");
		SupplierInvoice fx = h.toFortnox(local);
		assertEquals("7", fx.getSupplierNumber());
		assertEquals("A-77", fx.getInvoiceNumber());
		assertEquals(11875.0, fx.getTotal(), 0.001);
		assertEquals(2375.0, fx.getVAT(), 0.001);
		assertEquals(1, fx.getSupplierInvoiceRows().getSupplierInvoiceRow().size());
		assertEquals(9500.0, fx.getSupplierInvoiceRows().getSupplierInvoiceRow().get(0).getDebit(), 0.001);

		// Fortnox adds the debt and VAT rows
		List<SupplierInvoiceRow> rows = new ArrayList<SupplierInvoiceRow>(fx.getSupplierInvoiceRows().getSupplierInvoiceRow());
		SupplierInvoiceRow tot = new SupplierInvoiceRow();
		tot.setCode("TOT"); tot.setAccount("2440"); tot.setCredit(11875.0);
		SupplierInvoiceRow vat = new SupplierInvoiceRow();
		vat.setCode("VAT"); vat.setAccount("2641"); vat.setDebit(2375.0);
		rows.add(tot); rows.add(vat);
		fx.getSupplierInvoiceRows().setSupplierInvoiceRow(rows);
		fx.setGivenNumber("55");

		Invoice<?> back = h.toCanonical(null, "556000-0000", fx);
		assertEquals(1, back.getLines().size());
		assertEquals(25.0, back.getLines().get(0).getTaxPercent(), 0.001);
		// The partner here is found by its link; compare with the same partner
		back.setBusinessPartner(local.getBusinessPartner());
		InvoiceSyncProfile p = new InvoiceSyncProfile(true).withDefaults("SEK");
		assertEquals(p.fingerprint(local), p.fingerprint(back));
		assertEquals(p.matchKey(local), p.matchKey(back));
	}

}

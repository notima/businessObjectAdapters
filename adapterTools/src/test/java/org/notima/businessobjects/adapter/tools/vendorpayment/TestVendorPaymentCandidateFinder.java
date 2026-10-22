package org.notima.businessobjects.adapter.tools.vendorpayment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Proxy;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.OrderInvoiceOperationResult;
import org.notima.generic.businessobjects.OrderInvoiceReaderOptions;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

class TestVendorPaymentCandidateFinder {

	private static Invoice<?> invoice(String docNo, String supplierNo, String dueDate, double openAmt, String currency) {
		Invoice<Object> inv = new Invoice<Object>();
		inv.setDocumentKey(docNo);
		BusinessPartner<Object> bp = new BusinessPartner<Object>();
		bp.setIdentityNo(supplierNo);
		inv.setBusinessPartner(bp);
		inv.setDueDate(Date.valueOf(dueDate));
		inv.setOpenAmt(openAmt);
		inv.setCurrency(currency);
		return inv;
	}

	private static VendorPaymentCandidateFinder finder() {
		VendorPaymentCandidateFinder finder = new VendorPaymentCandidateFinder("SE");
		finder.setToday(java.sql.Timestamp.valueOf("2026-10-21 10:15:00"));
		return finder;
	}

	private static VendorPaymentCandidate summarize(VendorPaymentCandidateFinder finder, Set<Object> unbooked, Invoice<?>... invoices) {
		VendorPaymentCandidate c = new VendorPaymentCandidate();
		finder.summarize(c, new ArrayList<Invoice<?>>(Arrays.asList(invoices)), unbooked);
		return c;
	}

	private static VendorPaymentCandidate summarize(VendorPaymentCandidateFinder finder, Invoice<?>... invoices) {
		return summarize(finder, Collections.emptySet(), invoices);
	}

	@Test
	void invoicesDueWithinTheDefaultPeriodAreCounted() {
		VendorPaymentCandidate c = summarize(finder(),
				invoice("1", "15", "2026-10-28", 1000.0, "SEK"),	// last day of the period
				invoice("2", "15", "2026-10-29", 500.0, "SEK"),		// after the period
				invoice("3", "16", "2026-10-21", 250.0, "SEK"),		// due today
				invoice("4", "17", "2026-10-25", 0.0, "SEK"));		// paid
		assertEquals(VendorPaymentCandidate.Status.DUE_SOON, c.getStatus());
		assertEquals(2, c.getInvoices());
		assertEquals(2, c.getSuppliers());
		assertEquals(1250.0, c.getAmount(), 0.001);
		assertEquals("SEK", c.getCurrency());
		assertEquals(0, c.getOverdueInvoices());
		assertEquals(Date.valueOf("2026-10-21"), c.getEarliestDueDate());
		assertEquals(0, c.getMaxDaysOverdue());
	}

	@Test
	void overdueInvoices() {
		VendorPaymentCandidate c = summarize(finder(),
				invoice("1", "15", "2026-10-11", 400.0, "SEK"),
				invoice("2", "16", "2026-10-24", 600.0, "EUR"));
		assertEquals(VendorPaymentCandidate.Status.OVERDUE, c.getStatus());
		assertEquals(2, c.getInvoices());
		assertEquals(1, c.getOverdueInvoices());
		assertEquals(400.0, c.getOverdueAmount(), 0.001);
		assertEquals("*", c.getCurrency(), "several currencies");
		assertEquals(10, c.getMaxDaysOverdue());

		VendorPaymentCandidateFinder f = finder();
		f.setIncludeOverdue(false);
		c = summarize(f,
				invoice("1", "15", "2026-10-11", 400.0, "SEK"),
				invoice("2", "16", "2026-10-24", 600.0, "SEK"));
		assertEquals(VendorPaymentCandidate.Status.DUE_SOON, c.getStatus(), "overdue excluded");
		assertEquals(600.0, c.getAmount(), 0.001);
	}

	@Test
	void creditsAreDeductedPerSupplier() {
		VendorPaymentCandidate c = summarize(finder(),
				invoice("1", "15", "2026-10-15", 1000.0, "SEK"),
				invoice("2", "15", "2026-12-31", -300.0, "SEK"),	// credit not due: deducted anyway
				invoice("3", "16", "2026-10-25", 500.0, "SEK"),
				invoice("4", "16", "2026-10-01", -500.0, "SEK"));	// covers the invoice
		assertEquals(1, c.getSuppliers());
		assertEquals(1, c.getInvoices());
		assertEquals(700.0, c.getAmount(), 0.001);
		assertEquals(700.0, c.getOverdueAmount(), 0.001, "not more than what is owed");

		c = summarize(finder(),
				invoice("3", "16", "2026-10-25", 500.0, "SEK"),
				invoice("4", "16", "2026-10-01", -500.0, "SEK"));
		assertEquals(VendorPaymentCandidate.Status.NOTHING_DUE, c.getStatus());
	}

	@Test
	void daysAheadAndPaymentDate() {
		VendorPaymentCandidateFinder f = finder();
		f.setDaysAhead(14);
		assertEquals(Date.valueOf("2026-11-04"), new Date(f.getLastDueDate().getTime()));
		VendorPaymentCandidate c = summarize(f, invoice("1", "15", "2026-11-04", 100.0, "SEK"));
		assertEquals(1, c.getInvoices());

		f = finder();
		f.setDaysAhead(0);
		c = summarize(f, invoice("1", "15", "2026-10-22", 100.0, "SEK"));
		assertEquals(VendorPaymentCandidate.Status.NOTHING_DUE, c.getStatus(), "only due today");

		f = finder();
		f.setPaymentDate(Date.valueOf("2026-10-23"));
		c = summarize(f,
				invoice("1", "15", "2026-10-23", 100.0, "SEK"),
				invoice("2", "15", "2026-10-24", 100.0, "SEK"));
		assertEquals(1, c.getInvoices(), "payment date overrides days ahead");
	}

	@Test
	void unbookedInvoicesAreCountedAndMarked() {
		Set<Object> unbooked = new HashSet<Object>(Arrays.asList("2"));
		VendorPaymentCandidate c = summarize(finder(), unbooked,
				invoice("1", "15", "2026-10-22", 100.0, "SEK"),
				invoice("2", "16", "2026-10-23", 200.0, "SEK"));
		assertEquals(2, c.getInvoices());
		assertEquals(1, c.getUnbookedInvoices());
		assertEquals(300.0, c.getAmount(), 0.001);

		VendorPaymentCandidateFinder f = finder();
		f.setIncludeUnbooked(false);
		c = summarize(f, unbooked,
				invoice("1", "15", "2026-10-22", 100.0, "SEK"),
				invoice("2", "16", "2026-10-23", 200.0, "SEK"));
		assertEquals(1, c.getInvoices());
		assertEquals(0, c.getUnbookedInvoices());
	}

	@Test
	void nothingDue() {
		VendorPaymentCandidate c = summarize(finder());
		assertEquals(VendorPaymentCandidate.Status.NOTHING_DUE, c.getStatus());
		assertNull(c.getEarliestDueDate());
		assertNull(c.getCurrency());
	}

	/**
	 * Adapter with the given tenants. Open vendor invoices are 1 and 2, unbooked are 2
	 * (also open) and 3 (not among the open ones).
	 */
	@SuppressWarnings("rawtypes")
	private static BusinessObjectFactory adapter(List<String> checked, String... taxIds) {
		BusinessPartnerList<Object> tenants = new BusinessPartnerList<Object>();
		List<BusinessPartner<Object>> list = new ArrayList<BusinessPartner<Object>>();
		for (String t : taxIds) {
			BusinessPartner<Object> bp = new BusinessPartner<Object>();
			bp.setTaxId(t);
			bp.setName("Tenant " + t);
			list.add(bp);
		}
		tenants.setBusinessPartner(list);
		return (BusinessObjectFactory) Proxy.newProxyInstance(TestVendorPaymentCandidateFinder.class.getClassLoader(),
				new Class<?>[] { BusinessObjectFactory.class }, (proxy, method, args) -> {
			switch (method.getName()) {
				case "getSystemName":	return "Test";
				case "listTenants":		return tenants;
				case "setTenant":		checked.add(args[0] + "/" + args[1]); return null;
				case "readVendorInvoices":
					OrderInvoiceOperationResult r = new OrderInvoiceOperationResult();
					if (((OrderInvoiceReaderOptions) args[0]).isUnpostedOnly()) {
						r.addAffectedInvoice(invoice("2", "16", "2026-10-22", 200.0, "SEK"));
						r.addAffectedInvoice(invoice("3", "17", "2026-10-23", 300.0, "SEK"));
					} else {
						r.addAffectedInvoice(invoice("1", "15", "2026-10-01", 100.0, "SEK"));
						r.addAffectedInvoice(invoice("2", "16", "2026-10-22", 200.0, "SEK"));
					}
					return r;
				default:				return null;
			}
		});
	}

	@Test
	@SuppressWarnings("rawtypes")
	void tenantsAreCheckedWithOpenAndUnbookedInvoices() {
		List<String> checked = new ArrayList<String>();
		VendorPaymentCandidateFinder finder = finder();
		finder.setOnlyTaxIds(Arrays.asList("7164168515", "556000-0000"));
		List<VendorPaymentCandidate> result = finder.find(
				Collections.<BusinessObjectFactory>singletonList(adapter(checked, "716416-8515", "559110-9045")));

		assertEquals(Arrays.asList("716416-8515/SE"), checked);
		assertEquals(2, result.size());
		VendorPaymentCandidate c = result.get(0);
		assertEquals(VendorPaymentCandidate.Status.OVERDUE, c.getStatus());
		assertEquals(3, c.getInvoices(), "unbooked invoice 2 counted once");
		assertEquals(2, c.getUnbookedInvoices());
		assertEquals(600.0, c.getAmount(), 0.001);
		assertEquals(VendorPaymentCandidate.Status.ERROR, result.get(1).getStatus(), "not a tenant");
		assertEquals("Not a tenant", result.get(1).getError());
	}

}

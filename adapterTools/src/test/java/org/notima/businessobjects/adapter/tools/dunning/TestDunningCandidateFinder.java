package org.notima.businessobjects.adapter.tools.dunning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Proxy;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.OrderInvoiceOperationResult;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

class TestDunningCandidateFinder {

	private static Invoice<?> invoice(String customerNo, String dueDate, double openAmt, String currency) {
		Invoice<Object> inv = new Invoice<Object>();
		BusinessPartner<Object> bp = new BusinessPartner<Object>();
		bp.setIdentityNo(customerNo);
		inv.setBusinessPartner(bp);
		inv.setDueDate(Date.valueOf(dueDate));
		inv.setOpenAmt(openAmt);
		inv.setCurrency(currency);
		return inv;
	}

	private static DunningCandidate summarize(int minDaysOverdue, Invoice<?>... invoices) {
		DunningCandidateFinder finder = new DunningCandidateFinder("SE");
		finder.setToday(Date.valueOf("2026-10-09"));
		finder.setMinDaysOverdue(minDaysOverdue);
		DunningCandidate c = new DunningCandidate();
		List<Invoice<?>> list = new ArrayList<Invoice<?>>(Arrays.asList(invoices));
		finder.summarizeOverdue(c, list);
		return c;
	}

	@Test
	void overdueInvoicesAreSummarizedPerCustomer() {
		DunningCandidate c = summarize(1,
				invoice("15", "2026-08-31", 1250.0, "SEK"),
				invoice("16", "2026-09-30", 1250.0, "SEK"),
				invoice("16", "2026-08-31", 625.5, "SEK"),
				invoice("17", "2026-10-09", 500.0, "SEK"),		// due today: not overdue yet
				invoice("18", "2026-12-31", 900.0, "SEK"),		// not due
				invoice("19", "2026-08-31", 0.0, "SEK"),		// paid
				invoice("20", "2026-08-31", -100.0, "SEK"));	// credit
		assertEquals(DunningCandidate.Status.NEEDS_DUNNING, c.getStatus());
		assertEquals(3, c.getOverdueInvoices());
		assertEquals(2, c.getCustomers());
		assertEquals(3125.5, c.getOverdueAmount(), 0.001);
		assertEquals("SEK", c.getCurrency());
		assertEquals(Date.valueOf("2026-08-31"), c.getOldestDueDate());
		assertEquals(39, c.getMaxDaysOverdue());
	}

	@Test
	void minDaysOverdueSkipsRecentlyDueInvoices() {
		DunningCandidate c = summarize(10,
				invoice("15", "2026-09-29", 100.0, "SEK"),		// 10 days overdue
				invoice("16", "2026-09-30", 100.0, "EUR"));		// 9 days overdue
		assertEquals(1, c.getOverdueInvoices());
		assertEquals("SEK", c.getCurrency());

		c = summarize(1,
				invoice("15", "2026-09-29", 100.0, "SEK"),
				invoice("16", "2026-09-30", 100.0, "EUR"));
		assertEquals("*", c.getCurrency(), "several currencies");
	}

	@Test
	void nothingOverdue() {
		DunningCandidate c = summarize(1, invoice("15", "2026-12-31", 100.0, "SEK"));
		assertEquals(DunningCandidate.Status.NOTHING_OVERDUE, c.getStatus());
		assertEquals(0, c.getOverdueInvoices());
		assertNull(c.getOldestDueDate());

		c = summarize(1);
		assertEquals(DunningCandidate.Status.NOTHING_OVERDUE, c.getStatus());
	}


	/**
	 * Adapter with the given tenants, each with one invoice overdue; records which
	 * tenants (tax id + country) were checked.
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
		return (BusinessObjectFactory) Proxy.newProxyInstance(TestDunningCandidateFinder.class.getClassLoader(),
				new Class<?>[] { BusinessObjectFactory.class }, (proxy, method, args) -> {
			switch (method.getName()) {
				case "getSystemName":	return "Test";
				case "listTenants":		return tenants;
				case "setTenant":		checked.add(args[0] + "/" + args[1]); return null;
				case "readInvoices":
					OrderInvoiceOperationResult r = new OrderInvoiceOperationResult();
					r.addAffectedInvoice(invoice("1", "2026-08-31", 100.0, "SEK"));
					return r;
				default:				return null;
			}
		});
	}

	@Test
	@SuppressWarnings("rawtypes")
	void onlyTheGivenTenantsAreChecked() {
		List<String> checked = new ArrayList<String>();
		DunningCandidateFinder finder = new DunningCandidateFinder("SE");
		finder.setToday(Date.valueOf("2026-10-09"));
		finder.setOnlyTaxIds(Arrays.asList("7164168515", " 559110-9045 ", "556000-0000"));
		finder.setCountryCode("NO");
		List<DunningCandidate> result = finder.find(
				Collections.<BusinessObjectFactory>singletonList(adapter(checked, "716416-8515", "559110-9045", "769615-6707")));

		assertEquals(Arrays.asList("716416-8515/NO", "559110-9045/NO"), checked, "only the given tenants, with the given country");
		assertEquals(3, result.size());
		assertEquals(DunningCandidate.Status.NEEDS_DUNNING, result.get(0).getStatus());
		assertEquals("716416-8515", result.get(0).getTaxId());
		assertEquals(DunningCandidate.Status.ERROR, result.get(2).getStatus(), "not a tenant");
		assertEquals("556000-0000", result.get(2).getTaxId());
	}

	@Test
	@SuppressWarnings("rawtypes")
	void allTenantsWithoutFilter() {
		List<String> checked = new ArrayList<String>();
		DunningCandidateFinder finder = new DunningCandidateFinder("SE");
		finder.find(Collections.<BusinessObjectFactory>singletonList(adapter(checked, "716416-8515", "559110-9045")));
		assertEquals(Arrays.asList("716416-8515/SE", "559110-9045/SE"), checked);
	}

}

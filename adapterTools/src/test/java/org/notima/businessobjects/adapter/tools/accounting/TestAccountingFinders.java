package org.notima.businessobjects.adapter.tools.accounting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.notima.businessobjects.adapter.tools.table.AccountBalanceTable;
import org.notima.businessobjects.adapter.tools.table.AccountingSummaryTable;
import org.notima.businessobjects.adapter.tools.table.SuspenseBalanceTable;
import org.notima.generic.businessobjects.AccountCategory;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.businessobjects.SuspenseAccountType;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.ifacebusinessobjects.AccountingModelProvider;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;

class TestAccountingFinders {

	/** An adapter of system "Test" with a tenant with accounting and one without. */
	@SuppressWarnings("rawtypes")
	private static List<BusinessObjectFactory> adapters() {
		BusinessPartnerList<Object> tenants = new BusinessPartnerList<Object>();
		List<BusinessPartner<Object>> list = new ArrayList<BusinessPartner<Object>>();
		for (String t : new String[] { "556000-0000", "559999-9999" }) {
			BusinessPartner<Object> bp = new BusinessPartner<Object>();
			bp.setTaxId(t);
			bp.setName("Tenant " + t);
			list.add(bp);
		}
		tenants.setBusinessPartner(list);
		BusinessObjectFactory adapter = (BusinessObjectFactory) Proxy.newProxyInstance(TestAccountingFinders.class.getClassLoader(),
				new Class<?>[] { BusinessObjectFactory.class }, (proxy, method, args) -> {
			switch (method.getName()) {
				case "getSystemName":	return "Test";
				case "listTenants":		return tenants;
				default:				return null;
			}
		});
		return Collections.singletonList(adapter);
	}

	private static AccountingModelService service(TestAccountingModelService.FakeProvider provider, Path dir) {
		return new AccountingModelService(() -> Collections.<AccountingModelProvider>singletonList(provider),
				new AccountingModelCache(dir.toFile()), "SE");
	}

	@Test
	void summary(@TempDir Path dir) throws Exception {
		AccountingSummaryFinder finder = new AccountingSummaryFinder("SE", service(new TestAccountingModelService.FakeProvider(), dir));
		finder.setDate(LocalDate.of(2026, 3, 31));
		List<AccountingSummary> result = finder.find(adapters());

		assertEquals(2, result.size());
		AccountingSummary s = result.get(0);
		assertEquals(TenantAccountingStatus.OK, s.getStatus());
		assertEquals("2026", s.getFiscalYear());
		assertEquals(500.0, s.getRevenue(), 0.001, "only February's sale");
		assertEquals(500.0, s.getResult(), 0.001);
		assertEquals(1500.0, s.getCash(), 0.001);
		assertEquals(1, s.getVouchers());
		assertEquals(LocalDate.of(2026, 2, 1), s.getLastVoucherDate());
		assertEquals(TenantAccountingStatus.NO_ACCOUNTING, result.get(1).getStatus());

		AccountingSummaryTable table = new AccountingSummaryTable(result, false);
		assertEquals(1, table.getRows().size(), "tenant without accounting hidden");
		assertEquals("1 of 2 tenants read (1 without accounting)", table.getSummary());
	}

	@Test
	void balances(@TempDir Path dir) throws Exception {
		AccountBalanceFinder finder = new AccountBalanceFinder("SE", service(new TestAccountingModelService.FakeProvider(), dir));
		finder.setAccounts(Arrays.asList("1930", "19*", "2440"));
		finder.setCategories(Arrays.asList(AccountCategory.REVENUE));
		finder.setOnlyTaxIds(Arrays.asList("5560000000"));
		List<AccountBalances> result = finder.find(adapters());

		assertEquals(1, result.size());
		List<AccountBalances.Balance> b = result.get(0).getBalances();
		assertEquals(4, b.size());
		assertEquals(1750.0, b.get(0).getBalance(), 0.001, "whole year by default (today)");
		assertEquals("1 account", b.get(1).getName());
		assertFalse(b.get(2).isFound());
		assertEquals(-750.0, b.get(3).getBalance(), 0.001, "revenue as booked, credit negative");

		AccountBalanceTable table = new AccountBalanceTable(result, false, true);
		assertEquals(3, table.getRows().size(), "zero balance of the missing account hidden");
	}

	@Test
	void refresh(@TempDir Path dir) throws Exception {
		TestAccountingModelService.FakeProvider provider = new TestAccountingModelService.FakeProvider();
		AccountingModelService service = service(provider, dir);

		AccountingRefresher refresher = new AccountingRefresher("SE", service);
		refresher.setAllYears(true);
		List<AccountingRefreshResult> result = refresher.find(adapters());
		assertEquals(2, result.get(0).getRefreshed());
		assertEquals(4, result.get(0).getVouchers());
		assertEquals("2025 – 2026", result.get(0).getFiscalYear());
		assertEquals(2, provider.fetched);

		// Recently cached: up to date with a max age
		refresher.setMaxAge(Duration.ofHours(1));
		result = refresher.find(adapters());
		assertEquals(0, result.get(0).getRefreshed());
		assertEquals(2, result.get(0).getUpToDate());
		assertEquals(2, provider.fetched);

		// Queries then use the cache
		AccountingSummaryFinder summary = new AccountingSummaryFinder("SE", service);
		summary.setDate(LocalDate.of(2025, 12, 31));
		assertTrue(summary.find(adapters()).get(0).isOk());
		assertEquals(2, provider.fetched);
	}

	@Test
	void suspenseAccounts(@TempDir Path dir) throws Exception {
		TestAccountingModelService.FakeProvider provider = new TestAccountingModelService.FakeProvider();
		TenantInformation ti = new TenantInformation();
		ti.setTenant(new TaxSubjectIdentifier("556000-0000", "SE"));
		ti.setSuspenseAccounts(SuspenseAccountType.UNIDENTIFIED, Arrays.asList("1930"));
		ti.setSuspenseAccounts(SuspenseAccountType.OWNER_SETTLEMENT, Arrays.asList("2893"));
		TenantInformationFactory tif = new TenantInformationFactory() {
			public TenantInformation getTenantInformation(TaxSubjectIdentifier tenant) {
				return "556000-0000".equals(tenant.getTaxId()) ? ti : null;
			}
			public TenantInformation persistTenantInformation(TenantInformation info) {
				return info;
			}
		};

		SuspenseAccountFinder finder = new SuspenseAccountFinder("SE", service(provider, dir), tif);
		List<SuspenseBalances> result = finder.find(adapters());
		assertEquals(2, result.size());
		SuspenseBalances b = result.get(0);
		assertTrue(b.isOk());
		assertTrue(b.hasNonZero());
		assertEquals(2, b.getBalances().size());
		assertEquals(1750.0, b.getBalances().get(0).getBalance(), 0.001);
		assertFalse(b.getBalances().get(1).isFound(), "2893 isn't in the chart of accounts");
		assertEquals(TenantAccountingStatus.NOT_CONFIGURED, result.get(1).getStatus());
		assertEquals(1, provider.fetched, "accounting not read for a tenant without suspense accounts");

		SuspenseBalanceTable table = new SuspenseBalanceTable(result, false, false);
		assertEquals(1, table.getRows().size(), "only the non-zero account");
		assertEquals("1 of 1 tenant has suspense accounts that aren't zero (1 without suspense accounts)", table.getSummary());

		finder.setTypes(EnumSet.of(SuspenseAccountType.OWNER_SETTLEMENT));
		b = finder.find(adapters()).get(0);
		assertEquals(1, b.getBalances().size());
		assertFalse(b.hasNonZero());
	}

}

package org.notima.businessobjects.adapter.tools.accounting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.notima.generic.businessobjects.AccountElement;
import org.notima.generic.businessobjects.AccountElement.AccountElementType;
import org.notima.generic.businessobjects.AccountStatementLines;
import org.notima.generic.businessobjects.AccountingFileData;
import org.notima.generic.businessobjects.AccountingPeriod;
import org.notima.generic.businessobjects.AccountingPeriodData;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.AccountingVoucherLine;
import org.notima.generic.businessobjects.BalanceSheetLine;
import org.notima.generic.businessobjects.BalanceSheetReport;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.ProfitLossReport;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.AccountingModelProvider;

class TestAccountingModelService {

	private static final TaxSubjectIdentifier TENANT = new TaxSubjectIdentifier("556000-0000", "SE");

	/** A provider of one tenant with fiscal years 2025 and 2026; counts the models fetched. */
	static class FakeProvider implements AccountingModelProvider {
		int fetched;
		public String getSystemName() { return "Test"; }
		public boolean hasAccountingFor(TaxSubjectIdentifier tenant) { return "556000-0000".equals(tenant.getTaxId()); }
		public List<AccountingPeriod> listFiscalYears(TaxSubjectIdentifier tenant) {
			return Arrays.asList(year(2025), year(2026));
		}
		public AccountingFileData getAccountingModel(TaxSubjectIdentifier tenant, AccountingPeriod fy) {
			fetched++;
			AccountingFileData data = new AccountingFileData();
			data.setSourceFormat("SIE4");
			BusinessPartner<Object> company = new BusinessPartner<Object>();
			company.setName("Test AB");
			company.setTaxId("556000-0000");
			data.setCompany(company);
			AccountingPeriodData p = new AccountingPeriodData();
			p.setName(fy.getName());
			p.setPeriodStart(fy.getPeriodStart());
			p.setPeriodEnd(fy.getPeriodEnd());
			AccountElement bank = new AccountElement("1930");
			bank.setName("Bank");
			bank.setOpeningBalance(1000.0);
			AccountElement sales = new AccountElement("3010");
			sales.setName("Sales");
			p.setChartOfAccounts(new ArrayList<AccountElement>(Arrays.asList(bank, sales)));
			p.setVouchers(new ArrayList<AccountingVoucher>(Arrays.asList(
					voucher("1", fy.getPeriodStart().plusMonths(1), 500),
					voucher("2", fy.getPeriodStart().plusMonths(6), 250))));
			data.setPeriods(new ArrayList<AccountingPeriodData>(Arrays.asList(p)));
			return data;
		}
	}

	static AccountingPeriod year(int y) {
		AccountingPeriod p = new AccountingPeriod();
		p.setName(String.valueOf(y));
		p.setPeriodStart(LocalDate.of(y, 1, 1));
		p.setPeriodEnd(LocalDate.of(y, 12, 31));
		return p;
	}

	static AccountingVoucher voucher(String no, LocalDate date, double amount) {
		AccountingVoucher v = new AccountingVoucher();
		v.setVoucherSeries("A");
		v.setVoucherNo(no);
		v.setAcctDate(date);
		v.setDescription("Sale " + no);
		AccountingVoucherLine d = new AccountingVoucherLine();
		d.setAcctNo("1930");
		d.setDebitAmount(BigDecimal.valueOf(amount));
		AccountingVoucherLine c = new AccountingVoucherLine();
		c.setAcctNo("3010");
		c.setCreditAmount(BigDecimal.valueOf(amount));
		v.setLines(new ArrayList<AccountingVoucherLine>(Arrays.asList(d, c)));
		return v;
	}

	private static AccountingModelService service(FakeProvider provider, File dir) {
		return new AccountingModelService(() -> Collections.<AccountingModelProvider>singletonList(provider),
				new AccountingModelCache(dir), "SE");
	}

	@Test
	void modelsAreCachedAndClassified(@TempDir Path dir) throws Exception {
		FakeProvider provider = new FakeProvider();
		AccountingModelService service = service(provider, dir.toFile());

		assertNull(service.providerFor(null, new TaxSubjectIdentifier("559999-9999", "SE")));
		assertNull(service.providerFor("Fortnox", TENANT), "other system");
		AccountingModelProvider p = service.providerFor("test", TENANT);
		assertNotNull(p);

		AccountingPeriodData period = service.periodOf(p, TENANT, LocalDate.of(2026, 3, 1));
		assertEquals(LocalDate.of(2026, 1, 1), period.getPeriodStart());
		assertEquals(AccountElementType.BalanceAcct, period.getChartOfAccounts().get(0).getAccountElementType(), "classified by Swedish rules");
		assertEquals("ASSET_CASH", period.getChartOfAccounts().get(0).getAccountClass());
		assertEquals(1, provider.fetched);

		// Read back from the cache: same content, no new fetch
		AccountingModelService again = service(provider, dir.toFile());
		AccountingPeriodData cached = again.periodOf(p, TENANT, LocalDate.of(2026, 3, 1));
		assertEquals(1, provider.fetched);
		assertEquals(2, cached.getVouchers().size());
		assertEquals(new BigDecimal("500.0"), cached.getVouchers().get(0).getLines().get(0).getDebitAmount());
		assertEquals(LocalDate.of(2026, 2, 1), cached.getVouchers().get(0).getAcctDate());
		assertEquals("Test AB", again.model(p, TENANT, year(2026)).getCompany().getName());
		assertTrue(new File(dir.toFile(), "Test/5560000000/2026-01-01.json").isFile());
		assertTrue(new File(dir.toFile(), "Test/5560000000/fiscal-years.json").isFile());

		// Max age 0: always fetched
		again.setMaxAge(Duration.ZERO);
		again.periodOf(p, TENANT, LocalDate.of(2026, 3, 1));
		assertEquals(2, provider.fetched);

		assertNull(service.periodOf(p, TENANT, LocalDate.of(2024, 6, 1)), "no fiscal year");
	}

	@Test
	void reportsFromTheModel(@TempDir Path dir) throws Exception {
		ModelAccountingReportProvider reports = new ModelAccountingReportProvider(service(new FakeProvider(), dir.toFile()));
		BusinessPartner<Object> bp = new BusinessPartner<Object>();
		bp.setTaxId("556000-0000");
		assertTrue(reports.hasAccountingFor(bp));

		AccountingPeriod firstHalf = new AccountingPeriod();
		firstHalf.setPeriodEnd(LocalDate.of(2026, 6, 30));
		BalanceSheetReport bs = reports.getBalanceSheet(bp, firstHalf, null, (Properties) null);
		assertEquals(1, bs.getLines().size());
		BalanceSheetLine bank = bs.getLines().get(0);
		assertEquals("1930", bank.getAccountNo());
		assertEquals(0, new BigDecimal("1500.00").compareTo(bank.getColumns().get(0).getAmount()), "only February's sale");
		assertEquals(LocalDate.of(2026, 1, 1), bs.getReportPeriod().getPeriodStart(), "start of the fiscal year");

		ProfitLossReport pl = reports.getProfitLossReport(bp, new AccountingPeriod(), null, null);
		assertEquals(1, pl.getLines().size());
		assertEquals(0, new BigDecimal("-750.00").compareTo(pl.getLines().get(0).getColumns().get(0).getAmount()), "credit negative");

		AccountStatementLines gl = reports.getAccountStatementLines(bp, "1930", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 31), false);
		assertEquals(1500.0, gl.getStartBalance(), 0.001);
		assertEquals(1, gl.getAccountStatementLine().size());
		assertEquals("3010", gl.getAccountStatementLine().get(0).getOtherAccountNo());
		assertEquals(1750.0, gl.getEndBalance(), 0.001);

		assertEquals("Sale 2", reports.getAccountingVoucher(bp, null, "A", "2").getDescription());
		assertEquals(2, reports.getAccountingVoucherList(bp, null, null).size());
	}

}

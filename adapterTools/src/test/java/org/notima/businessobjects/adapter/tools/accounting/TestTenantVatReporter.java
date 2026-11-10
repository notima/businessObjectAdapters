package org.notima.businessobjects.adapter.tools.accounting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.AccountingModelProvider;

class TestTenantVatReporter {

	private static TenantVatReporter reporter(Path dir) {
		TestAccountingModelService.FakeProvider provider = new TestAccountingModelService.FakeProvider();
		return new TenantVatReporter(new AccountingModelService(
				() -> Collections.<AccountingModelProvider>singletonList(provider), new AccountingModelCache(dir.toFile()), "SE"));
	}

	@Test
	void rangeOverTwoFiscalYears(@TempDir Path dir) throws Exception {
		// The fake provider sells 500 on Feb 1 and 250 on Jul 1 each year, on 3010 without VAT
		TenantVatReporter.Result r = reporter(dir).report(null, new TaxSubjectIdentifier("556000-0000", "SE"),
				LocalDate.of(2025, 7, 1), LocalDate.of(2026, 2, 28));

		assertTrue(r.isOk(), r.getStatusText());
		assertEquals("Test", r.getSystemName());
		assertEquals(Arrays.asList("2025", "2026"), r.getFiscalYears());
		assertEquals(BigDecimal.valueOf(750), r.getReport().getReported("05"));
		assertEquals(2, r.getReport().getVoucherCount());
	}

	@Test
	void withoutAccountingOrVatReport(@TempDir Path dir) throws Exception {
		TenantVatReporter reporter = reporter(dir);
		LocalDate from = LocalDate.of(2026, 1, 1), to = LocalDate.of(2026, 1, 31);

		assertEquals(TenantAccountingStatus.NO_ACCOUNTING,
				reporter.report(null, new TaxSubjectIdentifier("559999-9999", "SE"), from, to).getStatus());
		assertEquals(TenantAccountingStatus.NO_FISCAL_YEAR,
				reporter.report(null, new TaxSubjectIdentifier("556000-0000", "SE"), LocalDate.of(2020, 1, 1), LocalDate.of(2020, 3, 31)).getStatus());
		assertEquals(TenantAccountingStatus.NOT_CONFIGURED,
				reporter.report(null, new TaxSubjectIdentifier("556000-0000", "XX"), from, to).getStatus());
	}

	@Test
	void tenantVatCodesGoFirst(@TempDir Path dir) throws Exception {
		TenantVatReporter.Result r = reporter(dir).report(null, new TaxSubjectIdentifier("556000-0000", "SE"),
				LocalDate.of(2025, 7, 1), LocalDate.of(2026, 2, 28), Collections.singletonMap("3010", "MF"));

		assertEquals(BigDecimal.ZERO, r.getReport().getReported("05"));
		assertEquals(BigDecimal.valueOf(750), r.getReport().getReported("42"));
		assertTrue(r.getReport().getUnknownRate().isEmpty());
	}

}

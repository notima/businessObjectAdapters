package org.notima.businessobjects.adapter.sie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.notima.generic.businessobjects.AccountElement;
import org.notima.generic.businessobjects.AccountingFileData;
import org.notima.generic.businessobjects.AccountingPeriodData;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.AccountingVoucherLine;
import org.notima.generic.businessobjects.BusinessPartner;

public class SieAccountingFileImporterTest {

	private static final String SAMPLE_SIE = "/20251231-sie1-260504.se";

	@Test
	public void testImportSample() throws Exception {
		File file = new File(getClass().getResource(SAMPLE_SIE).toURI());

		AccountingFileData result = new SieAccountingFileImporter().importFile(file);

		assertEquals("SIE4", result.getSourceFormat());
		assertEquals("SE", result.getCountryCode());
		assertEquals("Helukabel AB", result.getCompany().getName());
		assertEquals("556419-6383", result.getCompany().getTaxId());

		// Two fiscal years in chronological order
		List<AccountingPeriodData> periods = result.getPeriods();
		assertEquals(2, periods.size());
		assertEquals("2024", periods.get(0).getName());
		assertEquals(LocalDate.of(2025, 1, 1), periods.get(1).getPeriodStart());
		assertEquals(LocalDate.of(2025, 12, 31), periods.get(1).getPeriodEnd());

		// Chart of accounts copied to every period, opening balance on current year
		AccountingPeriodData current = periods.get(1);
		assertEquals(792, current.getChartOfAccounts().size());
		AccountElement acct1120 = findAccount(current, "1120");
		assertNotNull(acct1120);
		assertEquals(1545382.79, acct1120.getOpeningBalance(), 0.001);
		assertEquals(792, periods.get(0).getChartOfAccounts().size());
	}

	@Test
	public void testExportAndReimport() throws Exception {
		AccountingPeriodData period = new AccountingPeriodData();
		period.setName("2025");
		period.setPeriodStart(LocalDate.of(2025, 1, 1));
		period.setPeriodEnd(LocalDate.of(2025, 12, 31));

		List<AccountElement> coa = new ArrayList<>();
		AccountElement bank = new AccountElement("1930");
		bank.setName("Bank");
		bank.setOpeningBalance(1000.0);
		coa.add(bank);
		AccountElement sales = new AccountElement("3010");
		sales.setName("Sales");
		coa.add(sales);
		period.setChartOfAccounts(coa);

		AccountingVoucher voucher = new AccountingVoucher();
		voucher.setVoucherSeries("A");
		voucher.setVoucherNo("1");
		voucher.setAcctDate(LocalDate.of(2025, 3, 15));
		voucher.setDescription("Sale");
		AccountingVoucherLine debit = new AccountingVoucherLine();
		debit.setAcctNo("1930");
		debit.setDebitAmount(BigDecimal.valueOf(500));
		voucher.addVoucherLine(debit);
		AccountingVoucherLine credit = new AccountingVoucherLine();
		credit.setAcctNo("3010");
		credit.setCreditAmount(BigDecimal.valueOf(500));
		credit.setCostCenter("10");
		voucher.addVoucherLine(credit);
		List<AccountingVoucher> vouchers = new ArrayList<>();
		vouchers.add(voucher);
		period.setVouchers(vouchers);

		BusinessPartner<?> company = new BusinessPartner<>();
		company.setName("Test AB");
		company.setTaxId("556000-0000");

		File tmp = Files.createTempFile("sie-roundtrip-", ".si").toFile();
		tmp.deleteOnExit();
		new SieAccountingFileExporter().exportFile(tmp, period, company, true);

		AccountingFileData result = new SieAccountingFileImporter().importFile(tmp);
		assertEquals("Test AB", result.getCompany().getName());
		assertEquals(1, result.getPeriods().size());

		AccountingPeriodData reimported = result.getPeriods().get(0);
		assertEquals("2025", reimported.getName());
		assertEquals(1000.0, findAccount(reimported, "1930").getOpeningBalance(), 0.001);
		assertEquals(1, reimported.getVouchers().size());

		AccountingVoucher v = reimported.getVouchers().get(0);
		assertEquals("A", v.getVoucherSeries());
		assertEquals(LocalDate.of(2025, 3, 15), v.getAcctDate());
		assertEquals(2, v.getLines().size());
		AccountingVoucherLine l1 = v.getLines().get(0);
		assertEquals("1930", l1.getAcctNo());
		assertEquals("Bank", l1.getAcctName());
		assertEquals(0, BigDecimal.valueOf(500).compareTo(l1.getDebitAmount()));
		assertNull(l1.getCostCenter());
		AccountingVoucherLine l2 = v.getLines().get(1);
		assertEquals(0, BigDecimal.valueOf(500).compareTo(l2.getCreditAmount()));
		assertEquals("10", l2.getCostCenter());
	}

	private static AccountElement findAccount(AccountingPeriodData period, String accountNo) {
		for (AccountElement ae : period.getChartOfAccounts()) {
			if (accountNo.equals(ae.getAccountNo())) return ae;
		}
		return null;
	}

}

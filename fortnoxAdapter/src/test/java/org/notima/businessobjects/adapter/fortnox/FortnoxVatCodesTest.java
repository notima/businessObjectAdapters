package org.notima.businessobjects.adapter.fortnox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.notima.api.fortnox.entities3.AccountSubset;
import org.notima.api.fortnox.entities3.Accounts;
import org.notima.generic.businessobjects.AccountElement;
import org.notima.generic.businessobjects.AccountingFileData;
import org.notima.generic.businessobjects.AccountingPeriodData;

public class FortnoxVatCodesTest {

	private static AccountSubset account(int no, String vatCode) {
		AccountSubset a = new AccountSubset();
		a.setNumber(no);
		a.setVATCode(vatCode);
		return a;
	}

	@Test
	public void vatCodesBecomeTaxKeys() {
		AccountElement sales = new AccountElement("3010");
		AccountElement own = new AccountElement("3020");
		own.setTaxKey("MP2");
		AccountElement bank = new AccountElement("1930");
		AccountElement sub = new AccountElement("4610");
		AccountElement settle = new AccountElement("2650");
		AccountElement odd = new AccountElement("4999");
		AccountingPeriodData p = new AccountingPeriodData();
		p.setChartOfAccounts(new ArrayList<AccountElement>(Arrays.asList(sales, own, bank, sub, settle, odd)));
		AccountingFileData data = new AccountingFileData();
		data.setPeriods(new ArrayList<AccountingPeriodData>(Arrays.asList(p)));
		Accounts accounts = new Accounts();
		accounts.setAccountSubset(new ArrayList<AccountSubset>(Arrays.asList(
				account(3010, " MP1 "), account(3020, "MP1"), account(1930, ""), account(4610, "IT"), account(2650, "R2"),
				account(4999, "XYZ"))));

		FortnoxAccountingModelProvider.applyVatCodes(data, accounts);

		assertEquals("MP1", sales.getTaxKey());
		assertEquals("MP2", own.getTaxKey());	// a key in the model is kept
		assertNull(bank.getTaxKey());
		assertEquals("IREVD", sub.getTaxKey());	// translated to the tax rules
		assertEquals("SETTLEMENT", settle.getTaxKey());
		assertEquals("XYZ", odd.getTaxKey());	// unknown: kept, to show as unknown
	}

	@Test
	public void sruCodesBecomeReportingCodes() {
		AccountElement tax = new AccountElement("8910");
		AccountElement own = new AccountElement("3010");
		own.setReportingCode("7410");
		AccountElement none = new AccountElement("1930");
		AccountingPeriodData p = new AccountingPeriodData();
		p.setChartOfAccounts(new ArrayList<AccountElement>(Arrays.asList(tax, own, none)));
		AccountingFileData data = new AccountingFileData();
		data.setPeriods(new ArrayList<AccountingPeriodData>(Arrays.asList(p)));
		AccountSubset a8910 = account(8910, null);
		a8910.setSRU(" 7528 ");
		AccountSubset a3010 = account(3010, "MP1");
		a3010.setSRU("7411");
		AccountSubset a1930 = account(1930, null);
		a1930.setSRU("0");
		Accounts accounts = new Accounts();
		accounts.setAccountSubset(new ArrayList<AccountSubset>(Arrays.asList(a8910, a3010, a1930)));

		FortnoxAccountingModelProvider.applyVatCodes(data, accounts);

		assertEquals("7528", tax.getReportingCode());
		assertEquals("7410", own.getReportingCode());	// a code in the model is kept
		assertEquals("MP1", own.getTaxKey());
		assertNull(none.getReportingCode());			// 0 is no code
	}

}

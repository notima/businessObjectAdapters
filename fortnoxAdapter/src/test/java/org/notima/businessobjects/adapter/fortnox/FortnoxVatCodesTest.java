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
		AccountingPeriodData p = new AccountingPeriodData();
		p.setChartOfAccounts(new ArrayList<AccountElement>(Arrays.asList(sales, own, bank)));
		AccountingFileData data = new AccountingFileData();
		data.setPeriods(new ArrayList<AccountingPeriodData>(Arrays.asList(p)));
		Accounts accounts = new Accounts();
		accounts.setAccountSubset(new ArrayList<AccountSubset>(Arrays.asList(
				account(3010, " MP1 "), account(3020, "MP1"), account(1930, ""))));

		FortnoxAccountingModelProvider.applyVatCodes(data, accounts);

		assertEquals("MP1", sales.getTaxKey());
		assertEquals("MP2", own.getTaxKey());	// a key in the model is kept
		assertNull(bank.getTaxKey());
	}

}

package org.notima.businessobjects.adapter.tools.accounting;

import java.time.LocalDate;

import org.notima.generic.businessobjects.AccountCategory;
import org.notima.generic.businessobjects.AccountElement.AccountElementType;
import org.notima.generic.businessobjects.AccountingPeriodData;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.accounting.LedgerCalculator;
import org.notima.generic.businessobjects.accounting.LedgerSummary;

/**
 * Finds the key figures ({@link AccountingSummary}) of the accounting of tenants at a date:
 * revenue and result from the start of the fiscal year, and equity and cash at the date.
 *
 * <p>Cash and bank are the accounts of category {@link AccountCategory#ASSET_CASH}, which the
 * rules of the tenant's country set.</p>
 */
public class AccountingSummaryFinder extends AbstractAccountingFinder<AccountingSummary> {

	public AccountingSummaryFinder(String defaultCountryCode, AccountingModelService service) {
		super(defaultCountryCode, service);
	}

	@Override
	protected AccountingSummary newCandidate() {
		return new AccountingSummary();
	}

	@Override
	protected void checkPeriod(AccountingPeriodData period, AccountingSummary c) {

		LedgerSummary pl = LedgerCalculator.summarize(period, AccountElementType.PLAcct, null, date);
		c.setRevenue(round(pl.sum(AccountCategory.REVENUE, AccountCategory.REVENUE_OTHER).getResult()));
		c.setResult(round(pl.total().getResult()));

		LedgerSummary bs = LedgerCalculator.summarize(period, AccountElementType.BalanceAcct, null, date);
		c.setEquity(round(-bs.getCategoryTotals(AccountCategory.EQUITY).getBalanceAtTo()));
		c.setCash(round(bs.getCategoryTotals(AccountCategory.ASSET_CASH).getBalanceAtTo()));

		LocalDate last = null;
		int vouchers = 0;
		for (AccountingVoucher v : period.getVouchers()) {
			LocalDate d = v.getAcctDate();
			if (d != null && d.isAfter(date)) continue;
			vouchers++;
			if (d != null && (last == null || d.isAfter(last))) last = d;
		}
		c.setVouchers(vouchers);
		c.setLastVoucherDate(last);

	}

	static double round(double amount) {
		return Math.round(amount * 100) / 100.0;
	}

}

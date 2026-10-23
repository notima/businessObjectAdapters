package org.notima.businessobjects.adapter.tools.accounting;

import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.AccountCategory;
import org.notima.generic.businessobjects.AccountingPeriodData;
import org.notima.generic.businessobjects.accounting.AccountTotals;
import org.notima.generic.businessobjects.accounting.LedgerCalculator;
import org.notima.generic.businessobjects.accounting.LedgerSummary;

/**
 * Finds the balances of accounts or account categories of tenants at a date
 * ({@link AccountBalances}). The balance of a P&amp;L account is its result from the start of
 * the fiscal year. Balances are debit positive, as booked.
 *
 * <p>An account is given by its number ("1930"), or by a prefix ending with {@code *} ("19*"),
 * which sums all accounts starting with it.</p>
 */
public class AccountBalanceFinder extends AbstractAccountingFinder<AccountBalances> {

	private List<String> accounts = new ArrayList<String>();
	private List<AccountCategory> categories = new ArrayList<AccountCategory>();

	public AccountBalanceFinder(String defaultCountryCode, AccountingModelService service) {
		super(defaultCountryCode, service);
	}

	/** @param accounts	The account numbers or prefixes ("19*") to show. */
	public void setAccounts(List<String> accounts) {
		this.accounts = accounts != null ? accounts : new ArrayList<String>();
	}

	/** @param categories	The categories to show. */
	public void setCategories(List<AccountCategory> categories) {
		this.categories = categories != null ? categories : new ArrayList<AccountCategory>();
	}

	@Override
	protected AccountBalances newCandidate() {
		return new AccountBalances();
	}

	@Override
	protected void checkPeriod(AccountingPeriodData period, AccountBalances c) {

		LedgerSummary s = LedgerCalculator.summarize(period, null, null, date);
		for (String account : accounts) {
			if (account.endsWith("*")) {
				String prefix = account.substring(0, account.length() - 1);
				double sum = 0;
				int count = 0;
				for (AccountTotals t : s.getAccounts()) {
					if (!t.getAccountNo().startsWith(prefix)) continue;
					sum += t.getBalanceAtTo();
					count++;
				}
				c.addBalance(new AccountBalances.Balance(account, count + (count == 1 ? " account" : " accounts"),
						AccountingSummaryFinder.round(sum), count > 0));
			} else {
				AccountTotals t = s.getAccount(account);
				c.addBalance(new AccountBalances.Balance(account, t != null ? t.getName() : "",
						t != null ? AccountingSummaryFinder.round(t.getBalanceAtTo()) : 0.0, t != null));
			}
		}
		for (AccountCategory category : categories) {
			int count = s.getAccounts(category).size();
			c.addBalance(new AccountBalances.Balance(category.name(), count + (count == 1 ? " account" : " accounts"),
					AccountingSummaryFinder.round(s.getCategoryTotals(category).getBalanceAtTo()), count > 0));
		}

	}

}

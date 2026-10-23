package org.notima.businessobjects.adapter.tools.accounting;

import java.util.ArrayList;
import java.util.List;

/**
 * The balances of some accounts or account categories of one tenant at a date. Produced by
 * {@link AccountBalanceFinder}.
 */
public class AccountBalances extends AbstractAccountingCandidate {

	/** The balance of one account, account prefix or category. */
	public static class Balance {
		private final String	key;
		private final String	name;
		private final double	balance;
		private final boolean	found;

		public Balance(String key, String name, double balance, boolean found) {
			this.key = key;
			this.name = name;
			this.balance = balance;
			this.found = found;
		}
		/** @return	The account number, prefix ("19*") or category asked for. */
		public String getKey() {
			return key;
		}
		/** @return	The account name; the number of accounts for a prefix or category. */
		public String getName() {
			return name;
		}
		/** @return	The balance, debit positive. */
		public double getBalance() {
			return balance;
		}
		/** @return	False if the tenant has no such account. */
		public boolean isFound() {
			return found;
		}
	}

	private final List<Balance> balances = new ArrayList<Balance>();

	public List<Balance> getBalances() {
		return balances;
	}

	public void addBalance(Balance balance) {
		balances.add(balance);
	}

}

package org.notima.businessobjects.adapter.tools.accounting;

import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.SuspenseAccountType;

/**
 * The balances of the suspense accounts of one tenant at a date. Produced by
 * {@link SuspenseAccountFinder}.
 */
public class SuspenseBalances extends AbstractAccountingCandidate {

	/** The balance of one suspense account. */
	public static class Balance {
		private final SuspenseAccountType	type;
		private final String				accountNo;
		private final String				name;
		private final double				balance;
		private final boolean				found;

		public Balance(SuspenseAccountType type, String accountNo, String name, double balance, boolean found) {
			this.type = type;
			this.accountNo = accountNo;
			this.name = name;
			this.balance = balance;
			this.found = found;
		}
		public SuspenseAccountType getType() {
			return type;
		}
		public String getAccountNo() {
			return accountNo;
		}
		public String getName() {
			return name;
		}
		/** @return	The balance, debit positive. */
		public double getBalance() {
			return balance;
		}
		/** @return	True if the balance isn't zero (rounded to cents). */
		public boolean isNonZero() {
			return Math.abs(balance) >= 0.005;
		}
		/** @return	False if the account isn't in the tenant's chart of accounts. */
		public boolean isFound() {
			return found;
		}
	}

	/** The suspense accounts to check, from the tenant information. */
	private final List<Balance> configured = new ArrayList<Balance>();
	private final List<Balance> balances = new ArrayList<Balance>();

	public List<Balance> getBalances() {
		return balances;
	}

	public void addBalance(Balance balance) {
		balances.add(balance);
	}

	/** @return	True if any suspense account isn't zero. */
	public boolean hasNonZero() {
		for (Balance b : balances) {
			if (b.isNonZero()) return true;
		}
		return false;
	}

	List<Balance> getConfigured() {
		return configured;
	}

}

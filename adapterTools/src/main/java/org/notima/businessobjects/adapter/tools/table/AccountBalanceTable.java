package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.businessobjects.adapter.tools.accounting.AccountBalances;
import org.notima.businessobjects.adapter.tools.accounting.TenantAccountingStatus;

/**
 * Balances of accounts or account categories of tenants, one row per tenant and account.
 * Tenants whose accounting couldn't be read get one row with their status.
 */
public class AccountBalanceTable extends GenericTable {

	private int checked;
	private int read;
	private int withBalance;

	/**
	 * @param results	The tenants.
	 * @param showAll	If false, tenants without accounting (or fiscal year) aren't shown.
	 * @param nonZero	If true, zero balances aren't shown.
	 */
	public AccountBalanceTable(List<AccountBalances> results, boolean showAll, boolean nonZero) {

		addColumn("Adapter");
		addColumn("Tax id");
		addColumn("Name");
		addColumn("Year");
		addColumn("Account");
		addColumn("Account name");
		addColumn("Balance", GenericColumn.ALIGNMENT_RIGHT);

		setEmptyTableText(results == null || results.isEmpty() ? "No tenants found" : "No balances to show");
		if (results == null) return;

		for (AccountBalances r : results) {
			checked++;
			if (!r.isOk()) {
				boolean without = r.getStatus() == TenantAccountingStatus.NO_ACCOUNTING || r.getStatus() == TenantAccountingStatus.NO_FISCAL_YEAR;
				if (without && !showAll) continue;
				addRow().addContent(nvl(r.getAdapterName()), nvl(r.getTaxId()), nvl(r.getName()), "", "", r.getStatusText(), "");
				continue;
			}
			read++;
			boolean any = false;
			for (AccountBalances.Balance b : r.getBalances()) {
				if (b.getBalance() != 0.0) any = true;
				if (nonZero && b.getBalance() == 0.0) continue;
				addRow().addContent(
						nvl(r.getAdapterName()),
						nvl(r.getTaxId()),
						nvl(r.getName()),
						nvl(r.getFiscalYear()),
						b.getKey(),
						b.isFound() ? nvl(b.getName()) : "(no such account)",
						nfmt.format(b.getBalance()));
			}
			if (any) withBalance++;
		}

	}

	/** Summary, ie "3 of 12 tenants have a balance (15 checked)". */
	public String getSummary() {
		return withBalance + " of " + read + (read == 1 ? " tenant" : " tenants") + " read "
				+ (withBalance == 1 ? "has" : "have") + " a balance"
				+ (checked != read ? " (" + (checked - read) + " not read)" : "");
	}

	private static String nvl(String s) {
		return s != null ? s : "";
	}

}

package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.businessobjects.adapter.tools.accounting.SuspenseBalances;
import org.notima.businessobjects.adapter.tools.accounting.TenantAccountingStatus;

/**
 * The suspense account balances of tenants, one row per tenant and account. Tenants whose
 * accounting couldn't be read get one row with their status.
 */
public class SuspenseBalanceTable extends GenericTable {

	private int checked;
	private int read;
	private int nonZero;
	private int notConfigured;
	private int errors;

	/**
	 * @param results		The tenants.
	 * @param showAll		If false, tenants without suspense accounts or accounting aren't shown.
	 * @param includeZero	If true, also suspense accounts that are zero are shown.
	 */
	public SuspenseBalanceTable(List<SuspenseBalances> results, boolean showAll, boolean includeZero) {

		addColumn("Adapter");
		addColumn("Tax id");
		addColumn("Name");
		addColumn("Year");
		addColumn("Type");
		addColumn("Account");
		addColumn("Account name");
		addColumn("Balance", GenericColumn.ALIGNMENT_RIGHT);

		setEmptyTableText(results == null || results.isEmpty() ? "No tenants found" : "No suspense account is non-zero");
		if (results == null) return;

		for (SuspenseBalances r : results) {
			checked++;
			if (r.getStatus() == TenantAccountingStatus.NOT_CONFIGURED) notConfigured++;
			if (r.getStatus() == TenantAccountingStatus.ERROR) errors++;
			if (!r.isOk()) {
				// Errors are always shown; tenants without configuration or accounting with -a
				if (r.getStatus() != TenantAccountingStatus.ERROR && !showAll) continue;
				addRow().addContent(nvl(r.getAdapterName()), nvl(r.getTaxId()), nvl(r.getName()), "", "", "", r.getStatusText(), "");
				continue;
			}
			read++;
			if (r.hasNonZero()) nonZero++;
			for (SuspenseBalances.Balance b : r.getBalances()) {
				if (!includeZero && !b.isNonZero()) continue;
				addRow().addContent(
						nvl(r.getAdapterName()),
						nvl(r.getTaxId()),
						nvl(r.getName()),
						nvl(r.getFiscalYear()),
						b.getType().name(),
						b.getAccountNo(),
						b.isFound() ? nvl(b.getName()) : "(not in chart of accounts)",
						nfmt.format(b.getBalance()));
			}
		}

	}

	/** Summary, ie "2 of 10 tenants have suspense accounts that aren't zero (3 not configured)". */
	public String getSummary() {
		return nonZero + " of " + read + (read == 1 ? " tenant " : " tenants ")
				+ (nonZero == 1 ? "has" : "have") + " suspense accounts that aren't zero"
				+ (notConfigured > 0 ? " (" + notConfigured + " without suspense accounts)" : "")
				+ (errors > 0 ? " (" + errors + " couldn't be read)" : "")
				+ (checked - read - notConfigured - errors > 0 ? " (" + (checked - read - notConfigured - errors) + " without accounting)" : "");
	}

	private static String nvl(String s) {
		return s != null ? s : "";
	}

}

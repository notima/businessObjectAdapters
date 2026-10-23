package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.businessobjects.adapter.tools.accounting.AccountingRefreshResult;
import org.notima.businessobjects.adapter.tools.accounting.TenantAccountingStatus;

/** The result of refreshing the cached accounting of tenants, one row per tenant. */
public class AccountingRefreshTable extends GenericTable {

	private int checked;
	private int refreshedTenants;
	private int refreshedYears;
	private int errors;

	/**
	 * @param results	The tenants.
	 * @param showAll	If false, tenants without accounting aren't shown.
	 */
	public AccountingRefreshTable(List<AccountingRefreshResult> results, boolean showAll) {

		addColumn("Adapter");
		addColumn("Tax id");
		addColumn("Name");
		addColumn("Status");
		addColumn("Years");
		addColumn("Fetched", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Up to date", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Vouchers", GenericColumn.ALIGNMENT_RIGHT);

		setEmptyTableText(results == null || results.isEmpty() ? "No tenants found" : "No tenant with accounting");
		if (results == null) return;

		for (AccountingRefreshResult r : results) {
			checked++;
			if (r.getStatus() == TenantAccountingStatus.ERROR) errors++;
			if (r.getRefreshed() > 0) refreshedTenants++;
			refreshedYears += r.getRefreshed();
			if (r.getStatus() == TenantAccountingStatus.NO_ACCOUNTING && !showAll) continue;
			boolean ok = r.isOk();
			addRow().addContent(
					nvl(r.getAdapterName()),
					nvl(r.getTaxId()),
					nvl(r.getName()),
					r.getStatusText(),
					nvl(r.getFiscalYear()),
					ok ? String.valueOf(r.getRefreshed()) : "",
					ok ? String.valueOf(r.getUpToDate()) : "",
					ok ? String.valueOf(r.getVouchers()) : "");
		}

	}

	/** Summary, ie "Fetched 14 fiscal years of 12 of 15 tenants (1 couldn't be read)". */
	public String getSummary() {
		return "Fetched " + refreshedYears + (refreshedYears == 1 ? " fiscal year" : " fiscal years")
				+ " of " + refreshedTenants + " of " + checked + (checked == 1 ? " tenant" : " tenants")
				+ (errors > 0 ? " (" + errors + " couldn't be read)" : "");
	}

	private static String nvl(String s) {
		return s != null ? s : "";
	}

}

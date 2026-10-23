package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.businessobjects.adapter.tools.accounting.AccountingSummary;
import org.notima.businessobjects.adapter.tools.accounting.TenantAccountingStatus;

/**
 * Key figures of the accounting of tenants, one row per tenant. Being a {@link GenericTable},
 * it can be shown on the console or as HTML.
 */
public class AccountingSummaryTable extends GenericTable {

	private int checked;
	private int read;
	private int withoutAccounting;
	private int errors;

	/**
	 * @param summaries	The tenants.
	 * @param showAll	If false, tenants without accounting (or fiscal year) aren't shown.
	 */
	public AccountingSummaryTable(List<AccountingSummary> summaries, boolean showAll) {

		addColumn("Adapter");
		addColumn("Tax id");
		addColumn("Name");
		addColumn("Status");
		addColumn("Year");
		addColumn("Revenue", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Result", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Equity", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Cash", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Vouchers", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Last voucher");

		setEmptyTableText(summaries == null || summaries.isEmpty() ? "No tenants found" : "No tenant with accounting");
		if (summaries == null) return;

		for (AccountingSummary s : summaries) {
			checked++;
			if (s.isOk()) read++;
			if (s.getStatus() == TenantAccountingStatus.ERROR) errors++;
			boolean without = s.getStatus() == TenantAccountingStatus.NO_ACCOUNTING || s.getStatus() == TenantAccountingStatus.NO_FISCAL_YEAR;
			if (without) withoutAccounting++;
			if (without && !showAll) continue;
			boolean ok = s.isOk();
			addRow().addContent(
					nvl(s.getAdapterName()),
					nvl(s.getTaxId()),
					nvl(s.getName()),
					s.getStatusText(),
					nvl(s.getFiscalYear()),
					ok ? nfmt.format(s.getRevenue()) : "",
					ok ? nfmt.format(s.getResult()) : "",
					ok ? nfmt.format(s.getEquity()) : "",
					ok ? nfmt.format(s.getCash()) : "",
					ok ? String.valueOf(s.getVouchers()) : "",
					ok && s.getLastVoucherDate() != null ? s.getLastVoucherDate().toString() : "");
		}

	}

	/** Summary, ie "12 of 15 tenants read (2 without accounting, 1 couldn't be read)". */
	public String getSummary() {
		return read + " of " + checked + (checked == 1 ? " tenant" : " tenants") + " read"
				+ (withoutAccounting > 0 ? " (" + withoutAccounting + " without accounting)" : "")
				+ (errors > 0 ? " (" + errors + " couldn't be read)" : "");
	}

	private static String nvl(String s) {
		return s != null ? s : "";
	}

}

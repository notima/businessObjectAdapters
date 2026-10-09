package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.businessobjects.adapter.tools.dunning.DunningCandidate;

/**
 * Tenants and their dunning status, one row per tenant. Being a {@link GenericTable}, it
 * can be shown on the console ({@link #getShellTable()}) or as HTML ({@link #getHtmlTable()}),
 * ie in an e-mail.
 */
public class DunningCandidateTable extends GenericTable {

	private int	needingDunning;
	private int	errors;
	private int	checked;

	/**
	 * @param candidates	The tenants to show.
	 * @param showAll		If false, only tenants that need a dunning run or couldn't be
	 * 						checked are shown.
	 */
	public DunningCandidateTable(List<DunningCandidate> candidates, boolean showAll) {

		addColumn("Adapter");
		addColumn("Tax id");
		addColumn("Name");
		addColumn("Status");
		addColumn("Invoices", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Customers", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Overdue amt", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Cur");
		addColumn("Oldest due");
		addColumn("Days late", GenericColumn.ALIGNMENT_RIGHT);

		setEmptyTableText(candidates == null || candidates.isEmpty()
				? "No tenants found"
				: "No tenant needs a dunning run");

		if (candidates == null) return;

		for (DunningCandidate c : candidates) {
			checked++;
			if (c.needsDunning()) needingDunning++;
			if (c.getStatus() == DunningCandidate.Status.ERROR) errors++;
			if (!showAll && c.getStatus() == DunningCandidate.Status.NOTHING_OVERDUE) continue;
			addRow(c);
		}

	}

	private void addRow(DunningCandidate c) {

		boolean overdue = c.needsDunning();
		addRow().addContent(
				nvl(c.getAdapterName()),
				nvl(c.getTaxId()),
				nvl(c.getName()),
				status(c),
				overdue ? String.valueOf(c.getOverdueInvoices()) : "",
				overdue ? String.valueOf(c.getCustomers()) : "",
				overdue ? nfmt.format(c.getOverdueAmount()) : "",
				overdue ? nvl(c.getCurrency()) : "",
				overdue && c.getOldestDueDate() != null ? dfmt.format(c.getOldestDueDate()) : "",
				overdue ? String.valueOf(c.getMaxDaysOverdue()) : "");

	}

	private static String status(DunningCandidate c) {
		switch (c.getStatus()) {
			case NEEDS_DUNNING:		return "Needs dunning";
			case NOTHING_OVERDUE:	return "Nothing overdue";
			default:				return "Error: " + nvl(c.getError());
		}
	}

	/**
	 * Summary, ie "3 of 12 tenants need a dunning run (1 couldn't be checked)".
	 */
	public String getSummary() {
		return needingDunning + " of " + checked + (checked == 1 ? " tenant " : " tenants ")
				+ (needingDunning == 1 ? "needs" : "need") + " a dunning run"
				+ (errors > 0 ? " (" + errors + " couldn't be checked)" : "");
	}

	private static String nvl(String s) {
		return s != null ? s : "";
	}

}

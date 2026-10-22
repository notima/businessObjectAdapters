package org.notima.businessobjects.adapter.tools.table;

import java.util.Date;
import java.util.List;

import org.notima.businessobjects.adapter.tools.vendorpayment.VendorPaymentCandidate;

/**
 * Tenants and their vendor payment status, one row per tenant. Being a {@link GenericTable},
 * it can be shown on the console ({@link #getShellTable()}) or as HTML ({@link #getHtmlTable()}),
 * ie in an e-mail.
 */
public class VendorPaymentCandidateTable extends GenericTable {

	private int	needingPayment;
	private int	withOverdue;
	private int	errors;
	private int	checked;
	private Date lastDueDate;

	/**
	 * @param candidates	The tenants to show.
	 * @param showAll		If false, only tenants with invoices to pay or that couldn't be
	 * 						checked are shown.
	 * @param lastDueDate	The last due date counted, for the summary.
	 */
	public VendorPaymentCandidateTable(List<VendorPaymentCandidate> candidates, boolean showAll, Date lastDueDate) {

		this.lastDueDate = lastDueDate;

		addColumn("Adapter");
		addColumn("Tax id");
		addColumn("Name");
		addColumn("Status");
		addColumn("Invoices", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Suppliers", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Amount", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Cur");
		addColumn("Overdue amt", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Unbooked", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Earliest due");
		addColumn("Days late", GenericColumn.ALIGNMENT_RIGHT);

		setEmptyTableText(candidates == null || candidates.isEmpty()
				? "No tenants found"
				: "No tenant has vendor invoices to pay");

		if (candidates == null) return;

		for (VendorPaymentCandidate c : candidates) {
			checked++;
			if (c.needsPayment()) needingPayment++;
			if (c.getStatus() == VendorPaymentCandidate.Status.OVERDUE) withOverdue++;
			if (c.getStatus() == VendorPaymentCandidate.Status.ERROR) errors++;
			if (!showAll && c.getStatus() == VendorPaymentCandidate.Status.NOTHING_DUE) continue;
			addRow(c);
		}

	}

	private void addRow(VendorPaymentCandidate c) {

		boolean due = c.needsPayment();
		addRow().addContent(
				nvl(c.getAdapterName()),
				nvl(c.getTaxId()),
				nvl(c.getName()),
				status(c),
				due ? String.valueOf(c.getInvoices()) : "",
				due ? String.valueOf(c.getSuppliers()) : "",
				due ? nfmt.format(c.getAmount()) : "",
				due ? nvl(c.getCurrency()) : "",
				due && c.getOverdueInvoices() > 0 ? nfmt.format(c.getOverdueAmount()) : "",
				due && c.getUnbookedInvoices() > 0 ? String.valueOf(c.getUnbookedInvoices()) : "",
				due && c.getEarliestDueDate() != null ? dfmt.format(c.getEarliestDueDate()) : "",
				due && c.getMaxDaysOverdue() > 0 ? String.valueOf(c.getMaxDaysOverdue()) : "");

	}

	private static String status(VendorPaymentCandidate c) {
		switch (c.getStatus()) {
			case OVERDUE:		return "Overdue";
			case DUE_SOON:		return "Due";
			case NOTHING_DUE:	return "Nothing due";
			default:			return "Error: " + nvl(c.getError());
		}
	}

	/**
	 * Summary, ie "4 of 12 tenants have vendor invoices to pay by 2026-10-28 (2 with overdue invoices)".
	 */
	public String getSummary() {
		return needingPayment + " of " + checked + (checked == 1 ? " tenant " : " tenants ")
				+ (needingPayment == 1 ? "has" : "have") + " vendor invoices to pay"
				+ (lastDueDate != null ? " by " + dfmt.format(lastDueDate) : "")
				+ (withOverdue > 0 ? " (" + withOverdue + " with overdue invoices)" : "")
				+ (errors > 0 ? " (" + errors + " couldn't be checked)" : "");
	}

	private static String nvl(String s) {
		return s != null ? s : "";
	}

}

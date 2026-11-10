package org.notima.businessobjects.adapter.tools.table;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.tax.vatreport.VatReport;

/**
 * The boxes of a VAT return with their reported amounts, section by section; for the checked
 * boxes (ie the output VAT on purchases with reverse charge) also the VAT the bases give, and
 * the difference to the booked VAT (exact, not rounded down). Optionally the
 * accounts of each box, and what to check before filing: accounts that aren't reported and
 * VAT settlement vouchers that weren't counted.
 */
public class VatReportTable extends GenericTable {

	private static final NumberFormat WHOLE = new DecimalFormat("#,##0");

	/**
	 * @param report		The VAT return.
	 * @param showAll		Also show boxes that are zero.
	 * @param showAccounts	Show the accounts of each box under it.
	 */
	public VatReportTable(VatReport report, boolean showAll, boolean showAccounts) {

		addColumn("Box");
		addColumn("Description");
		addColumn("Amount", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Calculated", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Difference", GenericColumn.ALIGNMENT_RIGHT);
		setEmptyTableText("No VAT to report");
		if (report == null) return;

		String section = null;
		for (VatReport.BoxValue b : report.getBoxes()) {
			boolean derived = b.getBox().isDerived();
			boolean calculated = b.isChecked() && b.getCalculated().signum() != 0;
			if (!showAll && !derived && !calculated && b.getReported().signum() == 0 && b.getAccounts().isEmpty()) continue;
			if (b.getBox().getSection() != null && !b.getBox().getSection().equals(section)) {
				section = b.getBox().getSection();
				addRow().addContent("", section, "", "", "");
			}
			String calc = "", diff = "";
			if (b.isChecked()) {
				// Exact amounts, so that a small difference shows
				BigDecimal d = b.getAmount().subtract(b.getCalculated());
				calc = nfmt.format(b.getCalculated());
				diff = d.abs().compareTo(new BigDecimal("0.005")) < 0 ? "" : nfmt.format(d);
			}
			addRow().addContent(b.getBox().getCode(), b.getBox().getName(), WHOLE.format(b.getReported()), calc, diff);
			if (showAccounts) {
				for (VatReport.AccountAmount a : b.getAccounts()) {
					addRow().addContent("", "    " + a.getAccountNo() + " " + nvl(a.getAccountName()), nfmt.format(a.getAmount()), "", "");
				}
			}
		}

	}

	/** The lines to check, one per account not reported or settlement voucher left out; empty if none. */
	public static String checks(VatReport report) {
		StringBuilder sb = new StringBuilder();
		if (report == null) return "";
		for (VatReport.AccountAmount a : report.getNotReported()) {
			sb.append("Not reported in any box: ").append(a.getAccountNo()).append(' ').append(nvl(a.getAccountName()))
					.append("  ").append(nfmt.format(a.getAmount())).append('\n');
		}
		for (VatReport.AccountAmount a : report.getUnknownRate()) {
			sb.append("VAT rate unknown, vouchers not checked: ").append(a.getAccountNo()).append(' ').append(nvl(a.getAccountName()))
					.append("  ").append(nfmt.format(a.getAmount()))
					.append("  (set-tenant-info <orgNo> vatCode.").append(a.getAccountNo()).append(" MP1/MP2/MP3, or exclude)\n");
		}
		for (VatReport.AccountAmount a : report.getUnknownTaxKeys()) {
			sb.append("Unknown VAT code ").append(a.getTaxKey()).append(" on ").append(a.getAccountNo()).append(' ')
					.append(nvl(a.getAccountName())).append(", placed as without a code\n");
		}
		for (AccountingVoucher v : report.getSettlementVouchers()) {
			sb.append("VAT settlement, not counted: ").append(nvl(v.getVoucherSeries())).append(nvl(v.getVoucherNo()))
					.append(' ').append(v.getAcctDate()).append(' ').append(nvl(v.getDescription())).append('\n');
		}
		return sb.toString();
	}

	/** The amount to pay (positive) or get back (negative), ie box 49 of the Swedish return. */
	public static String toPay(VatReport report) {
		VatReport.BoxValue total = null;
		for (VatReport.BoxValue b : report.getBoxes()) {
			if (b.getBox().isDerived()) total = b;
		}
		if (total == null) return "";
		BigDecimal amount = total.getReported();
		return (amount.signum() >= 0 ? "VAT to pay: " : "VAT to get back: ") + WHOLE.format(amount.abs());
	}

	private static String nvl(String s) {
		return s != null ? s : "";
	}

}

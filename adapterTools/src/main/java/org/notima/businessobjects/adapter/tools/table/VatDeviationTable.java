package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.tax.vatreport.VatReport;

/**
 * The vouchers whose booked VAT differs from the VAT their base gives
 * ({@link VatReport#getDeviations()}): reverse charge not booked, wrongly calculated, or VAT
 * booked without a base.
 */
public class VatDeviationTable extends GenericTable {

	public VatDeviationTable(List<VatReport.Deviation> deviations) {

		addColumn("Voucher");
		addColumn("Date");
		addColumn("Box");
		addColumn("Base", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Calculated", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Booked", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Difference", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Problem");
		addColumn("Description");
		setEmptyTableText("No deviations");
		if (deviations == null) return;

		for (VatReport.Deviation d : deviations) {
			AccountingVoucher v = d.getVoucher();
			addRow().addContent(
					nvl(v.getVoucherSeries()) + nvl(v.getVoucherNo()),
					v.getAcctDate() != null ? v.getAcctDate().toString() : "",
					d.isInputVat() ? "input" : d.getVatBox(),
					nfmt.format(d.getBase()),
					nfmt.format(d.getExpected()),
					nfmt.format(d.getBooked()),
					nfmt.format(d.getDifference()),
					problem(d),
					fit(v.getDescription(), 40));
		}

	}

	private static String problem(VatReport.Deviation d) {
		switch (d.getKind()) {
			case MISSING_VAT:  return d.isInputVat() ? "Input VAT not booked" : "Reverse charge not booked";
			case MISSING_BASE: return d.isInputVat() ? "Input VAT without base" : "VAT without base";
			default:           return d.isInputVat() ? "Input VAT differs" : "VAT wrongly calculated";
		}
	}

	private static String fit(String s, int max) {
		if (s == null) return "";
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}

	private static String nvl(String s) {
		return s != null ? s : "";
	}

}

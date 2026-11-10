package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.generic.businessobjects.tax.vatreport.VatReport;

/**
 * The accounts whose input VAT must be deducted (mixed activity), as used in the range of a
 * VAT return ({@link VatReport#getInputVatChecks()}).
 */
public class InputVatCheckTable extends GenericTable {

	public InputVatCheckTable(List<VatReport.InputVatCheck> checks) {

		addColumn("Account");
		addColumn("Name");
		addColumn("Rate", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Base", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Input VAT", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Vouchers", GenericColumn.ALIGNMENT_RIGHT);
		setEmptyTableText("No accounts checked");
		if (checks == null) return;

		for (VatReport.InputVatCheck c : checks) {
			addRow().addContent(
					c.getAccountNo(),
					c.getAccountName() != null ? c.getAccountName() : "",
					c.getRate().stripTrailingZeros().toPlainString() + " %",
					nfmt.format(c.getBase()),
					nfmt.format(c.getExpected()),
					c.getVoucherCount());
		}
	}

}

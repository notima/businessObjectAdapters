package org.notima.fortnox.command.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.notima.api.fortnox.entities3.Voucher;
import org.notima.api.fortnox.entities3.VoucherRow;
import org.notima.businessobjects.adapter.tools.table.GenericColumn;
import org.notima.businessobjects.adapter.tools.table.GenericTable;

public class VoucherTable extends GenericTable {

	private NumberFormat nfmt = new DecimalFormat("#,##0.00");	
	
	public void initColumns() {
		
		addColumn("Date");
		addColumn("Acct No");
		addColumn("Description");
		GenericColumn col = new GenericColumn("Debet");
		col.alignRight();
		addColumn(col);
		col = new GenericColumn("Credit");
		col.alignRight();
		addColumn(col);
		addColumn("Is deleted");
		
	}
	
	public VoucherTable(Voucher vv) {
		initColumns();

		addRow(vv);
		
	}
	
	private void addRow(Voucher vv) {

		if (vv==null)
			return;
		
		if (vv.getVoucherRows()==null || vv.getVoucherRows().getVoucherRow()==null || vv.getVoucherRows().getVoucherRow().size()==0) {
			return;
		}

		// Add a first row
		addRow().addContent(
				(vv.getVoucherSeries() + vv.getVoucherNumber()),
				"",
				vv.getDescription(),
				vv.getCostCenter()!=null && vv.getCostCenter().trim().length()>0 ? ("Cst " + vv.getCostCenter()) : "",
				vv.getProject()!=null && vv.getProject().trim().length()>0 ? ("Pr " + vv.getProject()) : "",
				""
				);
		
		for (VoucherRow vr : vv.getVoucherRows().getVoucherRow()) {
			addRow().addContent(
					vv.getTransactionDate(),
					vr.getAccount(),
					vr.getDescription() + (vr.getTransactionInformation()!=null && vr.getTransactionInformation().trim().length()>0 ? (" : " + vr.getTransactionInformation()) : ""),
					nfmt.format(vr.getDebit()),
					nfmt.format(vr.getCredit()),
					vr.getRemoved()!=null && vr.getRemoved().booleanValue() ? "*" : ""
					)
					;
		}
		
		// Add comment if existing
		if (vv.getComments()!=null && vv.getComments().trim().length()>0) {
			addRow().addContent(
					"",
					"",
					vv.getComments(),
					"",
					"",
					""
					);
		}
		
	}
	
}

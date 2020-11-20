package org.notima.fortnox.command.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.api.fortnox.entities3.Voucher;
import org.notima.api.fortnox.entities3.VoucherRow;

public class VoucherTable extends ShellTable {

	private NumberFormat nfmt = new DecimalFormat("#,##0.00");	
	
	public void initColumns() {
		
		column("Date");
		column("Acct No");
		column("Description");
		column("Debet").alignRight();
		column("Credit").alignRight();
		column("Is deleted");
		
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
					vr.getDescription(),
					nfmt.format(vr.getDebit()),
					nfmt.format(vr.getCredit()),
					vr.getRemoved()!=null && vr.getRemoved().booleanValue() ? "*" : ""
					)
					;
		}
		
	}
	
}

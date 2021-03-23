package org.notima.fortnox.command.table;

import java.util.List;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.api.fortnox.entities3.VoucherSubset;
import org.notima.api.fortnox.entities3.Vouchers;

public class VouchersTable extends ShellTable {
	
	public void initColumns() {
		
		column("VoucherNo");
		column("Date");
		column("Description");
		column("Comments");
		
	}
	
	public VouchersTable(Vouchers vouchers) {
		initColumns();

		if (vouchers==null || vouchers.getVoucherSubset()==null) return;
		
		List<VoucherSubset> vss = vouchers.getVoucherSubset();
		
		for (VoucherSubset vv : vss) {
			addRow(vv);
		}
		
	}
	
	private void addRow(VoucherSubset vv) {

		if (vv==null)
			return;

		// Add a first row
		addRow().addContent(
				(vv.getVoucherSeries() + " " + vv.getVoucherNumber()),
				vv.getTransactionDate(),
				vv.getDescription(),
				vv.getComments()
				);
		
	}
	
}

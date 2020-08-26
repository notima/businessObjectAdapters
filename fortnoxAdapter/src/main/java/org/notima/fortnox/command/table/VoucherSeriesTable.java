package org.notima.fortnox.command.table;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.api.fortnox.entities3.VoucherSeriesCollection;
import org.notima.api.fortnox.entities3.VoucherSeriesSubset;

public class VoucherSeriesTable extends ShellTable {

	public void initColumns() {
		
		column("Code");
		column("Description");
		column("Manual");
		column("Year");
		
	}
	
	public VoucherSeriesTable(VoucherSeriesCollection vv) {
		initColumns();

		for (VoucherSeriesSubset vs : vv.getVoucherSeriesSubset()) {
			addRow(vs);
		}
		
	}
	
	private void addRow(VoucherSeriesSubset vs) {

		if (vs==null)
			return;

		addRow().addContent(
				vs.getCode(),
				vs.getDescription(),
				vs.getManual(),
				vs.getYear()
				)
				;
		
	}
	
}

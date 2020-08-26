package org.notima.fortnox.command.table;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.api.fortnox.entities3.ModeOfPaymentSubset;
import org.notima.api.fortnox.entities3.ModesOfPayments;

public class ModesOfPaymentsTable extends ShellTable {

	public void initColumns() {
		
		column("Code");
		column("Description");
		column("Account");
		
	}
	
	public ModesOfPaymentsTable(ModesOfPayments vv) {
		initColumns();

		for (ModeOfPaymentSubset vs : vv.getModeOfPaymentSubset()) {
			addRow(vs);
		}
		
	}
	
	private void addRow(ModeOfPaymentSubset vs) {

		if (vs==null)
			return;

		addRow().addContent(
				vs.getCode(),
				vs.getDescription(),
				vs.getAccountNumber()
				)
				;
		
	}
	
}

package org.notima.fortnox.command.table;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.api.fortnox.entities3.TermsOfPaymentSubset;
import org.notima.api.fortnox.entities3.TermsOfPayments;

public class TermsOfPaymentsTable extends ShellTable {

	public void initColumns() {
		
		column("Code");
		column("Description");
		
	}
	
	public TermsOfPaymentsTable(TermsOfPayments vv) {
		initColumns();

		for (TermsOfPaymentSubset vs : vv.getTermsOfPaymentSubset()) {
			addRow(vs);
		}
		
	}
	
	private void addRow(TermsOfPaymentSubset vs) {

		if (vs==null)
			return;

		addRow().addContent(
				vs.getCode(),
				vs.getDescription()
				)
				;
		
	}
	
}

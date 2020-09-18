package org.notima.fortnox.command.table;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.api.fortnox.entities3.FinancialYearSubset;
import org.notima.api.fortnox.entities3.FinancialYears;

public class FinancialYearTable extends ShellTable {

	public void initColumns() {
		
		column("Id");
		column("From date");
		column("To date");
		column("Description");
		column("Method");
		
	}
	
	public FinancialYearTable(FinancialYears vv) {
		initColumns();

		for (FinancialYearSubset vs : vv.getFinancialYearSubset()) {
			addRow(vs);
		}
		
	}
	
	private void addRow(FinancialYearSubset vs) {

		if (vs==null)
			return;

		addRow().addContent(
				vs.getId(),
				vs.getFromDate(),
				vs.getToDate(),
				vs.getAccountCharts(),
				vs.getAccountingMethod()
				)
				;
		
	}
	
}

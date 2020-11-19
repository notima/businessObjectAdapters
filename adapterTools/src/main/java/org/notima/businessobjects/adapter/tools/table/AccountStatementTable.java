package org.notima.businessobjects.adapter.tools.table;

import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.generic.businessobjects.AccountStatementLine;
import org.notima.generic.businessobjects.AccountStatementLines;
import org.notima.generic.businessobjects.util.NumberUtils;

public class AccountStatementTable extends ShellTable {

	public AccountStatementTable(AccountStatementLines alines) {
		
		Col col = new Col("AcctNo");
		column(col);
		col = new Col("Voucher");
		column(col);
		col = new Col("DateAcct");
		column(col);
		col = new Col("Description");
		column(col);
		col = new Col("Amount").alignRight();
		column(col);
		col = new Col("Balance").alignRight();
		column(col);
		
		if (alines==null) {
			emptyTableText("No content");
			return;
		}
		
		// Add starting line
		double runningBalance = NumberUtils.roundToPrecision(alines.getStartBalance(), 2);
		
		addRow().addContent("",
				"",
				alines.getStartDate(),
				"",
				0D,
				runningBalance
				);
		
		for (AccountStatementLine asl : alines.getAccountStatementLine()) {
			runningBalance += asl.getTrxAmount();
			runningBalance = NumberUtils.roundToPrecision(runningBalance, 2);
			addRow().addContent(
					asl.getThisAccountNo(), 
					(asl.getVoucherSeries()!=null ? asl.getVoucherSeries() + " " : "") + asl.getVoucherNo(),
					asl.getAccountDate(),
					asl.getDescription(),
					asl.getTrxAmount(),
					runningBalance
					);
		}
		
		// TODO Add ending line
		
	}
	
}

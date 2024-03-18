package org.notima.businessobjects.adapter.tools.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.notima.generic.businessobjects.AccountStatementLine;
import org.notima.generic.businessobjects.AccountStatementLines;
import org.notima.util.NumberUtils;

public class AccountStatementTable extends GenericTable {

	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_RESET = "\u001B[0m";
	
	private NumberFormat nfmt = new DecimalFormat("#,##0.00");	
	
	public AccountStatementTable(AccountStatementLines alines) {
		
		addColumn("AcctNo");
		addColumn("Voucher");
		addColumn("DateAcct");
		addColumn("Description");
		GenericColumn col = new GenericColumn("Amount").alignRight();
		addColumn(col);
		col = new GenericColumn("Balance").alignRight();
		addColumn(col);
		
		if (alines==null) {
			setEmptyTableText("No content");
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
		
		AccountStatementLine asl = null, nextLine = null;
		boolean lastLineInDate;
		int lines = alines.getAccountStatementLine().size();
		for (int i = 0; i<lines; i++) {
			lastLineInDate = false;
			asl = alines.getAccountStatementLine().get(i);
			if (i<(lines-1)) {
				nextLine = alines.getAccountStatementLine().get(i+1);
				if (nextLine.getAccountDate().isAfter(asl.getAccountDate())) {
					lastLineInDate = true;
				}
			} else {
				lastLineInDate = true;
			}
			runningBalance += asl.getTrxAmount();
			runningBalance = NumberUtils.roundToPrecision(runningBalance, 2);
			addRow().addContent(
					asl.getThisAccountNo(), 
					(asl.getVoucherSeries()!=null ? asl.getVoucherSeries() + " " : "") + asl.getVoucherNo(),
					asl.getAccountDate(),
					asl.getDescription(),
					nfmt.format(asl.getTrxAmount()),
					greenText(lastLineInDate, nfmt.format(runningBalance))
					);
		}
		
		// TODO Add ending line
		
	}
	
	private String greenText(boolean on, String text) {
		return ((on ? ANSI_GREEN : "") + text + (on ? ANSI_RESET : ""));
	}
	
	
}

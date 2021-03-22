package org.notima.fortnox.command.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.api.fortnox.entities3.Account;
import org.notima.api.fortnox.entities3.AccountSubset;

public class AccountTable extends ShellTable {

	private NumberFormat nfmt = new DecimalFormat("#,##0.00");	
	
	public void initColumns() {
		
		column("Acct No");
		column("Description");
		column("VAT code");
		column("SRU");
		column("Active");
		column("YearId");
		column("Beg Bal").alignRight();
		column("End Bal").alignRight();
		
	}
	
	public AccountTable(List<Object> accounts) {
		initColumns();
		
		Account ii;
		AccountSubset is;
		
		for (Object oo : accounts) {
			
			if (oo instanceof Account) {
				ii = (Account)oo;
				addRow(ii);
			}
			if (oo instanceof AccountSubset) {
				is = (AccountSubset)oo;
				addRow(is);
			}
			
		}
		
	}
	
	public AccountTable(Account invoice) {
		
		initColumns();
		addRow(invoice);

	}
	
	private void addRow(AccountSubset is) {

		addRow().addContent(
				is.getNumber(),
				is.getDescription(),
				is.getVATCode(),
				is.getSRU(),
				is.getActive(),
				is.getYear(),
				"-",
				"-")
				;
		
	}
	
	private void addRow(Account aa) {

		addRow().addContent(
				aa.getNumber(),
				aa.getDescription(),
				aa.getVATCode(),
				aa.getSRU(),
				aa.getActive(),
				aa.getYear(),
				nfmt.format(aa.getBalanceBroughtForward()),
				nfmt.format(aa.getBalanceCarriedForward())
				)
				;
		
	}
	
}

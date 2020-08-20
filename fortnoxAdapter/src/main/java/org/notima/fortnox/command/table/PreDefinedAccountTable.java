package org.notima.fortnox.command.table;

import java.util.List;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.api.fortnox.entities3.PreDefinedAccount;
import org.notima.api.fortnox.entities3.PreDefinedAccountSubset;

public class PreDefinedAccountTable extends ShellTable {
	
	public void initColumns() {
		
		column("Type");
		column("Acct No");
		
	}
	
	public PreDefinedAccountTable(List<Object> accounts) {
		initColumns();
		
		PreDefinedAccount ii;
		PreDefinedAccountSubset is;
		
		for (Object oo : accounts) {
			
			if (oo instanceof PreDefinedAccount) {
				ii = (PreDefinedAccount)oo;
				addRow(ii);
			}
			if (oo instanceof PreDefinedAccountSubset) {
				is = (PreDefinedAccountSubset)oo;
				addRow(is);
			}
			
		}
		
	}
	
	private void addRow(PreDefinedAccountSubset is) {

		addRow().addContent(
				is.getName(),
				is.getAccount())
				;
		
	}
	
	private void addRow(PreDefinedAccount aa) {

		addRow().addContent(
				aa.getName(),
				aa.getAccount());
		
	}
	
}

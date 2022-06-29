package org.notima.fortnox.command.table;

import java.util.Map;

import org.notima.businessobjects.adapter.tools.table.GenericTable;

public class RevenueAccountMapTable extends GenericTable {
	
	public void initColumns() {
		
		column("VAT Code");
		column("Account");
		
	}
	
	public RevenueAccountMapTable(Map<String, Integer> accountMap) {
		initColumns();

		if (accountMap==null || accountMap.isEmpty()) {
			this.setEmptyTableText("No mappings");
			return;
		}
		
		for (String vatCode : accountMap.keySet()) {
			addRow(vatCode, accountMap.get(vatCode));
		}
	}
	
	private void addRow(String vatCode, Integer acctNo) {

		addRow().addContent(
				vatCode,
				acctNo
				)
				;
		
	}
	
	
}

package org.notima.fortnox.command.table;

import java.util.Map;

import org.notima.api.fortnox.entities3.VatInfo;
import org.notima.businessobjects.adapter.tools.table.GenericTable;

public class RevenueAccountMapTable extends GenericTable {
	
	public void initColumns() {
		
		column("VAT Code");
		column("Rev. acct");
		column("Tax due");
		column("Tax claim");
		column("Rate %");
		column("Domicile");
		
	}
	
	public RevenueAccountMapTable(Map<String, VatInfo> accountMap) {
		initColumns();

		if (accountMap==null || accountMap.isEmpty()) {
			this.setEmptyTableText("No mappings");
			return;
		}
		
		for (String vatCode : accountMap.keySet()) {
			addRow(vatCode, accountMap.get(vatCode));
		}
	}
	
	private void addRow(String vatCode, VatInfo vatInfo) {

		addRow().addContent(
				vatCode,
				vatInfo.getDefaultRevenueAccount(),
				vatInfo.getDefaultVatDueAccount(),
				vatInfo.getDefaultVatClaimAccount(),
				vatInfo.getVatRate(),
				vatInfo.getTaxDomicile()
				)
				;
		
	}
	
	
}

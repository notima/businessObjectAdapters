package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.generic.businessobjects.Tax;

public class TaxRateTable extends GenericTable {

	public TaxRateTable(List<Tax> bpl) {
		
		addColumn("Tax id");
		addColumn("Country");
		addColumn("Rate");
		addColumn("Rev acct");
		addColumn("Tax due acct");
		
		if (bpl==null || bpl.size()==0) {
			setEmptyTableText("No content");
			return;
		}
		
		for (Tax p : bpl) {
			addRow().addContent(
					p.getKey(),
					p.getCountryCode(),
					p.getRate(),
					p.getDefaultRevenueAccount(),
					p.getTaxDebtAccount()
					);
		}
		
	}
	
}

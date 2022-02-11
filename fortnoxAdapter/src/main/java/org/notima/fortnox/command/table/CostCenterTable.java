package org.notima.fortnox.command.table;

import org.notima.api.fortnox.entities3.CostCenterSubset;
import org.notima.api.fortnox.entities3.CostCenters;
import org.notima.businessobjects.adapter.tools.table.GenericTable;

public class CostCenterTable extends GenericTable {
	
	public void initColumns() {
		
		column("Code");
		column("Description");
		column("Notes");
		
	}
	
	public CostCenterTable(CostCenters ccs) {
		initColumns();
		
		if (ccs==null || 
				ccs.getCostCenterSubset()==null) {
			return;
		}
		
		for (CostCenterSubset cc : ccs.getCostCenterSubset()) {
			
			addRow(cc);
			
		}
		
	}
	
	private void addRow(CostCenterSubset cc) {

		addRow().addContent(
				cc.getCode(),
				cc.getDescription(),
				cc.getNote()
				)
				;
		
	}
	
	
}

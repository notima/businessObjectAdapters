package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.generic.businessobjects.BusinessPartner;

public class BusinessPartnerTable extends GenericTable {

	private List<BusinessPartner<?>> bpList;
	
	
	public BusinessPartnerTable(List<BusinessPartner<?>> bpl) {

		addColumn("Tax id");
		addColumn("Name");
		
		if (bpl==null || bpl.size()==0) {
			setEmptyTableText("No tenants");
			return;
		}
		
		bpList = bpl;
		populateRows();
		
	}

	private void populateRows() {
		
		if (getRows()!=null)
			this.getRows().clear();

		if (bpList==null) return;
		
		for (BusinessPartner<?> p : bpList) {
			addRow().addContent(p.getTaxId(), p.getName());
		}
		
	}
	
	
}

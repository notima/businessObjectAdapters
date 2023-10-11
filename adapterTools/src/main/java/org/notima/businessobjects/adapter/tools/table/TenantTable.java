package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.generic.businessobjects.BusinessPartner;

public class TenantTable extends GenericTable {

	private String adapterName = "-";
	private List<BusinessPartner<Object>> bpList;
	
	
	public TenantTable(List<BusinessPartner<Object>> bpl) {

		addColumn("Adapter");
		addColumn("Tax id");
		addColumn("Name");
		
		if (bpl==null || bpl.size()==0) {
			setEmptyTableText("No tenants");
			return;
		}
		
		bpList = bpl;
		populateRows();
		
	}

	public void setAdapterName(String adapterName) {
		this.adapterName = adapterName;
		populateRows();
	}

	private void populateRows() {
		
		if (getRows()!=null)
			this.getRows().clear();
		
		for (BusinessPartner<Object> p : bpList) {
			addRow().addContent(adapterName, p.getTaxId(), p.getName());
		}
		
	}
	
	
}

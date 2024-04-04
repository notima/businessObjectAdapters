package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.generic.businessobjects.BusinessPartner;

public class BusinessPartnerExtendedTable extends GenericTable {

	private List<BusinessPartner<?>> bpList;
	
	
	public BusinessPartnerExtendedTable(List<BusinessPartner<?>> bpl) {

		addColumn("ID");
		addColumn("Name");
		addColumn("Tax id");
		addColumn("Address 1");
		addColumn("Address 2");
		addColumn("Postal");
		addColumn("City");
		addColumn("E-mail");
		addColumn("Comment");
		
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
			addRow().addContent(
					p.getIdentityNo(),
					p.getName(),
					p.getTaxId(),
					(p.hasLocations() ? p.getAddressOfficial().getAddress1() : ""),
					(p.hasLocations() ? p.getAddressOfficial().getAddress2() : ""),
					(p.hasLocations() ? p.getAddressOfficial().getPostal() : ""),
					(p.hasLocations() ? p.getAddressOfficial().getCity() : ""),
					(p.hasLocations() ? p.getAddressOfficial().getEmail() : ""),
					p.getComments()!=null ? p.getComments() : ""
					);
		}
		
	}
	
	
}

package org.notima.fortnox.command.table;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;

public class ClientTable extends ShellTable {

	public void initColumns() {
		
		column("OrgNo");
		column("Name");
		
	}
	
	public ClientTable(BusinessPartnerList<?> bpl) {
		initColumns();

		for (BusinessPartner<?> bp : bpl.getBusinessPartner()) {
			addRow(bp);
		}
		
	}
	
	private void addRow(BusinessPartner<?> bp) {

		if (bp==null)
			return;

		addRow().addContent(
				bp.getTaxId(),
				bp.getName()
				)
				;
		
	}
	
}

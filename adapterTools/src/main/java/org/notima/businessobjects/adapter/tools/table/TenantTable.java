package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.generic.businessobjects.BusinessPartner;

public class TenantTable extends ShellTable {

	public TenantTable(List<BusinessPartner<Object>> bpl) {
		
		Col col = new Col("Tax id");
		column(col);
		col = new Col("Name");
		column(col);
		
		if (bpl==null || bpl.size()==0) {
			emptyTableText("No content");
			return;
		}
		
		for (BusinessPartner<Object> p : bpl) {
			addRow().addContent(p.getTaxId(), p.getName());
		}
		
	}
	
}

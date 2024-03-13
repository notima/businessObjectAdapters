package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Product;

public class ProductTable extends GenericTable {

	private List<Product<?>> bpList;
	
	
	public ProductTable(List<Product<?>> bpl) {

		addColumn("Key");
		addColumn("Name");
		
		if (bpl==null || bpl.size()==0) {
			setEmptyTableText("No products");
			return;
		}
		
		bpList = bpl;
		populateRows();
		
	}

	private void populateRows() {
		
		if (getRows()!=null)
			this.getRows().clear();

		if (bpList==null) return;
		
		for (Product<?> p : bpList) {
			addRow().addContent(p.getKey(), p.getName());
		}
		
	}
	
	
}

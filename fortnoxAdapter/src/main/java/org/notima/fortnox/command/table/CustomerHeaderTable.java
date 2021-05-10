package org.notima.fortnox.command.table;

import java.util.List;

import org.notima.api.fortnox.entities3.Customer;
import org.notima.api.fortnox.entities3.CustomerSubset;
import org.notima.businessobjects.adapter.tools.table.GenericTable;

public class CustomerHeaderTable extends GenericTable {
	
	public void initColumns() {
		
		column("Cust #");
		column("Org No");
		column("Customer name");
		column("Email Invoice");
		column("ToP");
		
	}
	
	public CustomerHeaderTable(List<Object> invoices) {
		initColumns();
		
		Customer ii;
		CustomerSubset is;
		
		for (Object oo : invoices) {
			
			if (oo instanceof Customer) {
				ii = (Customer)oo;
				addRow(ii);
			}
			if (oo instanceof CustomerSubset) {
				is = (CustomerSubset)oo;
				addRow(is);
			}
			
		}
		
	}
	
	public CustomerHeaderTable(Customer invoice) {
		
		initColumns();
		addRow(invoice);

	}
	
	private void addRow(CustomerSubset is) {

		addRow().addContent(
				is.getCustomerNumber(),
				is.getOrganisationNumber(),
				is.getName(),
				"N/A",
				"N/A" 
				)
				;
		
	}
	
	private void addRow(Customer invoice) {

		addRow().addContent(
				invoice.getCustomerNumber(),
				invoice.getOrganisationNumber(),
				invoice.getName(),
				invoice.getEmailInvoice(),
				invoice.getTermsOfPayment()
				)
				;
		
	}
	
}

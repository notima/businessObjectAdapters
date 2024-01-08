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
		column("Invoice address");
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
				is.getEmail(),
				"N/A" 
				)
				;
		
	}
	
	private void addRow(Customer customer) {

		addRow().addContent(
				customer.getCustomerNumber(),
				customer.getOrganisationNumber(),
				customer.getName(),
				getInvoiceAddress(customer),
				customer.getEmailInvoice(),
				customer.getTermsOfPayment()
				)
				;
		
	}
	
	private String getInvoiceAddress(Customer customer) {
		if (customer==null) return "";
		
		StringBuffer address = new StringBuffer();
		
		if (customer.getAddress1()!=null && customer.getAddress1().trim().length()>0) {
			address.append(customer.getAddress1());
		}
		if (customer.getAddress2()!=null && customer.getAddress2().trim().length()>0) {
			commaToBuffer(address).append(customer.getAddress2());
		}

		if (customer.getZipCode()!=null && customer.getZipCode().trim().length()>0) {
			commaToBuffer(address).append(customer.getZipCode());
		}
		
		if (customer.getCity()!=null && customer.getCity().trim().length()>0) {
			commaToBuffer(address).append(customer.getCity());
		}
		
		return address.toString();
	}

	private StringBuffer commaToBuffer(StringBuffer buf) {
		if (buf.length()>0) {
			buf.append(", ");
		}
		return buf;
	}
	
}

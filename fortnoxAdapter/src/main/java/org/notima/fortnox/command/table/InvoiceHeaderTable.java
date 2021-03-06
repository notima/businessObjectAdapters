package org.notima.fortnox.command.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.notima.api.fortnox.entities3.Invoice;
import org.notima.api.fortnox.entities3.InvoiceSubset;
import org.notima.businessobjects.adapter.tools.table.GenericTable;

public class InvoiceHeaderTable extends GenericTable {

	private NumberFormat nfmt = new DecimalFormat("#,##0.00");	
	
	public void initColumns() {
		
		column("Date");
		column("Invoice No");
		column("Cust #");
		column("Customer name");
		column("Order #");
		column("Your Order #");
		column("ExtRef1");
		column("ExtRef2");
		column("Grand total").alignRight();
		column("Open amt").alignRight();
		column("Pmt term");
		column("Not completed");
		column("Warehouse Ready");
		
	}
	
	public InvoiceHeaderTable(List<Object> invoices) {
		initColumns();
		
		Invoice ii;
		InvoiceSubset is;
		
		for (Object oo : invoices) {
			
			if (oo instanceof Invoice) {
				ii = (Invoice)oo;
				addRow(ii);
			}
			if (oo instanceof InvoiceSubset) {
				is = (InvoiceSubset)oo;
				addRow(is);
			}
			
		}
		
	}
	
	public InvoiceHeaderTable(Invoice invoice) {
		
		initColumns();
		addRow(invoice);

	}
	
	private void addRow(InvoiceSubset is) {

		addRow().addContent(
				is.getInvoiceDate(),
				is.getDocumentNumber() + (is.isCancelled() ? " **" : ""),
				is.getCustomerNumber(),
				is.getCustomerName(),
				"N/A",
				"N/A",
				is.getExternalInvoiceReference1(),
				is.getExternalInvoiceReference2(),
				nfmt.format(is.getTotal()),
				nfmt.format(is.getBalance()),
				is.getTermsOfPayment(),
				"N/A", 
				"N/A")
				;
		
	}
	
	private void addRow(Invoice invoice) {

		addRow().addContent(
				invoice.getInvoiceDate(),
				invoice.getDocumentNumber() + (invoice.isCancelled() ? " **" : ""),
				invoice.getCustomerNumber(),
				invoice.getCustomerName(),
				invoice.getOrderReference(),
				invoice.getYourOrderNumber(),
				invoice.getExternalInvoiceReference1(),
				invoice.getExternalInvoiceReference2(),
				nfmt.format(invoice.getTotal()),
				nfmt.format(invoice.getBalance()),
				invoice.getTermsOfPayment(),
				invoice.isNotCompleted(),
				invoice.getWarehouseReady()
				)
				;
		
	}
	
}

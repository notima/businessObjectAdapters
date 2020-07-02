package org.notima.fortnox.command.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.api.fortnox.entities3.Invoice;

public class InvoiceHeaderTable extends ShellTable {

	private NumberFormat nfmt = new DecimalFormat("#,##0.00");	
	
	public void initColumns() {
		
		column("Date");
		column("Invoice No");
		column("Customer name");
		column("Order No");
		column("Your Order No");
		column("ExtRef1");
		column("ExtRef2");
		column("Grand total").alignRight();
		column("Pmt term");
		
	}
	
	public InvoiceHeaderTable(List<Invoice> invoices) {
		initColumns();
		
		for (Invoice ii : invoices) {
			addRow(ii);
		}
		
	}
	
	public InvoiceHeaderTable(Invoice invoice) {
		
		initColumns();
		addRow(invoice);

	}
	
	private void addRow(Invoice invoice) {

		addRow().addContent(
				invoice.getInvoiceDate(),
				invoice.getDocumentNumber(),
				invoice.getCustomerName(),
				invoice.getOrderReference(),
				invoice.getYourOrderNumber(),
				invoice.getExternalInvoiceReference1(),
				invoice.getExternalInvoiceReference2(),
				nfmt.format(invoice.getTotal()),
				invoice.getTermsOfPayment())
				;
		
		
	}
	
}

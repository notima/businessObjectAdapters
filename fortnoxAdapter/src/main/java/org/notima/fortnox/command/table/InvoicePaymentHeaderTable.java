package org.notima.fortnox.command.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.api.fortnox.entities3.InvoicePayment;
import org.notima.api.fortnox.entities3.InvoicePaymentSubset;

public class InvoicePaymentHeaderTable extends ShellTable {

	private NumberFormat nfmt = new DecimalFormat("#,##0.00");	
	
	public void initColumns() {
		
		column("PmtNo");
		column("Date");
		column("Invoice No");
		column("Cust #");
		column("Customer name");
		column("Amount").alignRight();
		column("Currency");
		column("Source");
		
	}
	
	public InvoicePaymentHeaderTable(List<Object> payments) {
		initColumns();
		
		InvoicePayment ii;
		InvoicePaymentSubset is;
		
		for (Object oo : payments) {
			
			if (oo instanceof InvoicePayment) {
				ii = (InvoicePayment)oo;
				addRow(ii);
				continue;
			}
			if (oo instanceof InvoicePaymentSubset) {
				is = (InvoicePaymentSubset)oo;
				addRow(is);
			}
			
		}
		
	}
	
	public InvoicePaymentHeaderTable(InvoicePayment invoicePayment) {
		
		initColumns();
		addRow(invoicePayment);

	}
	
	private void addRow(InvoicePaymentSubset is) {

		addRow().addContent(
				is.getNumber(),
				is.getPaymentDate(),
				is.getInvoiceNumber(),
				"N/A",
				"N/A",
				nfmt.format(is.getAmount()),
				is.getCurrency(),
				is.getSource()
				);
		
	}
	
	private void addRow(InvoicePayment is) {

		addRow().addContent(
				is.getNumber(),
				is.getPaymentDate(),
				is.getInvoiceNumber(),
				is.getInvoiceCustomerNumber(),
				is.getInvoiceCustomerName(),
				nfmt.format(is.getAmount()),
				is.getCurrency(),
				is.getSource()
				)
				;
		
	}
	
}

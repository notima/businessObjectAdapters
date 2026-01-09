package org.notima.fortnox.command.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.notima.api.fortnox.entities3.InvoiceInterface;
import org.notima.api.fortnox.entities3.SupplierInvoice;
import org.notima.api.fortnox.entities3.SupplierInvoiceSubset;
import org.notima.businessobjects.adapter.tools.table.GenericTable;

public class SupplierInvoiceHeaderTable extends GenericTable {

	private NumberFormat nfmt = new DecimalFormat("#,##0.00");
	
	class InvoiceComparator implements Comparator<InvoiceInterface> {
		
		@Override
		public int compare(InvoiceInterface o1, InvoiceInterface o2) {
			
			// First compare dates
			if (o1.getInvoiceDate().equals(o2.getInvoiceDate())) {
				// Compare document numbers
				return o1.getDocumentNumber().compareTo(o2.getDocumentNumber());
			} else {
				return o1.getInvoiceDate().compareTo(o2.getInvoiceDate());
			}
			
		}
		
	}
	
	public void initColumns() {
		
		column("Date");
		column("Invoice No");
		column("Supp #");
		column("Supplier name");
		column("ExtInv No");
		column("Grand total").alignRight();
		column("Open amt").alignRight();
		column("Curr");
		column("Due date");
		
	}
	
	public SupplierInvoiceHeaderTable(List<InvoiceInterface> invoices) {

		initColumns();
		
		Collections.sort(invoices, new InvoiceComparator());
		
		SupplierInvoice ii;
		SupplierInvoiceSubset iii;
		
		for (Object oo : invoices) {
			
			if (oo instanceof SupplierInvoice) {
				ii = (SupplierInvoice)oo;
				addRow(ii);
			}
			if (oo instanceof SupplierInvoiceSubset) {
				iii = (SupplierInvoiceSubset)oo;
				addRow(iii);
			}
			
		}
		
	}
	
	public SupplierInvoiceHeaderTable(SupplierInvoice invoice, boolean includeAddress) {

		initColumns();
		
		addRow(invoice);

	}

	private void addRow(SupplierInvoiceSubset is) {

		addRow().addContent(
				is.getInvoiceDate(),
				is.getDocumentNumber() + (is.isCancelled() ? " **" : ""),
				is.getSupplierNumber(),
				is.getSupplierName(),
				is.getExternalInvoiceNumber(),
				nfmt.format(is.getTotal()),
				nfmt.format(is.getBalance()),
				is.getCurrency(),
				is.getDueDate()
				)
				;
		
	}
	
	private void addRow(SupplierInvoice invoice) {

		addRow().addContent(
				invoice.getInvoiceDate(),
				invoice.getDocumentNumber() + (invoice.isCancelled() ? " **" : ""),
				invoice.getSupplierNumber(),
				getSupplierName(invoice),
				invoice.getExternalInvoiceNumber(),
				nfmt.format(invoice.getTotal()),
				nfmt.format(invoice.getBalance()),
				invoice.getCurrency(),
				invoice.getDueDate()
				)
				;
		
	}
	
	private String getSupplierName(SupplierInvoice invoice) {
		
		return invoice.getSupplierName();
		
	}
	
}

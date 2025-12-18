package org.notima.businessobjects.adapter.tools.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.notima.generic.businessobjects.Invoice;

@SuppressWarnings("rawtypes")
public class InvoiceHeaderTable extends GenericTable {

	private NumberFormat nfmt = new DecimalFormat("#,##0.00");
	private boolean includeAddress = false;
	
	class InvoiceComparator implements Comparator<Invoice<?>> {
		
		@Override
		public int compare(Invoice<?> o1, Invoice<?> o2) {
			
			// First compare dates
			if (o1.getInvoiceDate().equals(o2.getInvoiceDate())) {
				// Compare document numbers
				return o1.getDocumentKey().compareTo(o2.getDocumentKey());
			} else {
				return o1.getInvoiceDate().compareTo(o2.getInvoiceDate());
			}
			
		}
		
	}
	
	public void initColumns() {
		
		column("Date");
		column("Invoice No");
		column("Cust #");
		column("Customer name");
		column("Order #");
		column("Your Order #");
		column("Our reference");
		column("ExtRef1");
		column("ExtRef2");
		column("Grand total").alignRight();
		column("Open amt").alignRight();
		column("Curr");
		column("Ctry");
		column("Pmt term");
		
	}
	
	public InvoiceHeaderTable(List<Invoice<?>> invoices, boolean includeAddress) {

		this.includeAddress = includeAddress;
		initColumns();
		
		Collections.sort(invoices, new InvoiceComparator());
		
		Invoice ii;
		
		for (Object oo : invoices) {
			
			if (oo instanceof Invoice) {
				ii = (Invoice)oo;
				addRow(ii);
			}
			
		}
		
	}
	
	public InvoiceHeaderTable(Invoice<?> invoice, boolean includeAddress) {

		this.includeAddress = includeAddress;
		initColumns();
		
		addRow(invoice);

	}
	
	private void addRow(Invoice<?> invoice) {

		addRow().addContent(
				invoice.getInvoiceDate(),
				invoice.getDocumentKey(),
				invoice.getBusinessPartner().getIdentityNo(),
				getCustomerName(invoice),
				invoice.getOrderKey(),
				invoice.getPoDocumentNo(),
				invoice.getOurReference(),
				invoice.getExternalReference1(),
				invoice.getExternalReference2(),
				nfmt.format(invoice.getGrandTotal()),
				nfmt.format(invoice.getOpenAmt()),
				invoice.getCurrency(),
				invoice.getDeliveryCountry(),
				invoice.getPaymentTermKey()
				)
				;
		
	}
	
	private String getCustomerName(Invoice<?> invoice) {
		
		if (!includeAddress) {
			return invoice.getBusinessPartner().getName();
		} else {
			StringBuffer buf = new StringBuffer();
			buf.append(invoice.getBusinessPartner().getName() + " ");
			buf.append(invoice.getBillLocation().getAddress1());
			return buf.toString();
		}
		
	}
	
}

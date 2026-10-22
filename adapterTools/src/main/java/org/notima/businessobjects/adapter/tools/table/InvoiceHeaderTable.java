package org.notima.businessobjects.adapter.tools.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.notima.generic.businessobjects.Invoice;

@SuppressWarnings("rawtypes")
public class InvoiceHeaderTable extends GenericTable {

	private boolean includeAddress = false;
	private boolean showDueDate = false;
	private boolean showReferences = true;
	
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
		if (showReferences) {
			column("Order #");
			column("Your Order #");
			column("Our reference");
			column("ExtRef1");
			column("ExtRef2");
		}
		column("Grand total").alignRight();
		column("Open amt").alignRight();
		column("Curr");
		if (showDueDate) {
			column("Due date");
		}
		column("Ctry");
		column("Pmt term");
		
	}
	
	public InvoiceHeaderTable(List<Invoice<?>> invoices, boolean includeAddress) {
		this(invoices, includeAddress, false, true);
	}

	/**
	 * @param invoices			The invoices to show.
	 * @param includeAddress	Show the address after the customer name.
	 * @param showDueDate		Show a due date column.
	 * @param showReferences	Show the order and reference columns (Order #, Your Order #,
	 * 							Our reference, ExtRef1, ExtRef2).
	 */
	public InvoiceHeaderTable(List<Invoice<?>> invoices, boolean includeAddress, boolean showDueDate, boolean showReferences) {

		this.includeAddress = includeAddress;
		this.showDueDate = showDueDate;
		this.showReferences = showReferences;
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

		List<Object> content = new ArrayList<Object>();
		content.add(dfmt.format(invoice.getInvoiceDate()));
		content.add(invoice.getDocumentKey());
		content.add(invoice.getBusinessPartner().getIdentityNo());
		content.add(getCustomerName(invoice));
		if (showReferences) {
			content.add(invoice.getOrderKey());
			content.add(invoice.getPoDocumentNo());
			content.add(invoice.getOurReference());
			content.add(invoice.getExternalReference1());
			content.add(invoice.getExternalReference2());
		}
		content.add(nfmt.format(invoice.getGrandTotal()));
		content.add(nfmt.format(invoice.getOpenAmt()));
		content.add(invoice.getCurrency());
		if (showDueDate) {
			content.add(invoice.getDueDate()!=null ? dfmt.format(invoice.getDueDate()) : "");
		}
		content.add(invoice.getDeliveryCountry());
		content.add(invoice.getPaymentTermKey());

		addRow().addContent(content);
		
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

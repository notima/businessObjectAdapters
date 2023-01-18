package org.notima.fortnox.command.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.notima.api.fortnox.entities3.Invoice;
import org.notima.api.fortnox.entities3.InvoiceInterface;
import org.notima.api.fortnox.entities3.InvoiceSubset;
import org.notima.api.fortnox.entities3.Order;
import org.notima.api.fortnox.entities3.OrderInterface;
import org.notima.api.fortnox.entities3.OrderSubset;
import org.notima.businessobjects.adapter.tools.table.GenericTable;

public class OrderHeaderTable extends GenericTable {

	private NumberFormat nfmt = new DecimalFormat("#,##0.00");	
	
	class OrderComparator implements Comparator<OrderInterface> {
		
		@Override
		public int compare(OrderInterface o1, OrderInterface o2) {
			
			// First compare dates
			if (o1.getOrderDate().equals(o2.getOrderDate())) {
				// Compare document numbers
				return o1.getDocumentNumber().compareTo(o2.getDocumentNumber());
			} else {
				return o1.getOrderDate().compareTo(o2.getOrderDate());
			}
			
		}
		
	}
	
	
	public void initColumns() {
		
		column("Date");
		column("Order No");
		column("Cust #");
		column("Customer name");
		column("Your Order #");
		column("ExtRef1");
		column("ExtRef2");
		column("Grand total").alignRight();
		column("Curr");
		column("Pmt term");
		column("Not completed");
		
	}
	
	public OrderHeaderTable(List<OrderInterface> orders) {
		initColumns();

		Collections.sort(orders, new OrderComparator());
		
		Order ii;
		OrderSubset is;
		
		for (Object oo : orders) {
			
			if (oo instanceof Order) {
				ii = (Order)oo;
				addRow(ii);
			}
			if (oo instanceof OrderSubset) {
				is = (OrderSubset)oo;
				addRow(is);
			}
			
		}
		
	}
	
	public OrderHeaderTable(Order order) {
		
		initColumns();
		addRow(order);

	}
	
	private void addRow(OrderSubset is) {

		addRow().addContent(
				is.getOrderDate(),
				is.getDocumentNumber() + (is.isCancelled() ? " **" : ""),
				is.getCustomerNumber(),
				is.getCustomerName(),
				"N/A",
				is.getExternalInvoiceReference1(),
				is.getExternalInvoiceReference2(),
				nfmt.format(is.getTotal()),
				is.getCurrency(),
				"N/A",
				"N/A")
				;
		
	}
	
	private void addRow(Order invoice) {

		addRow().addContent(
				invoice.getOrderDate(),
				invoice.getDocumentNumber() + (invoice.isCancelled() ? " **" : ""),
				invoice.getCustomerNumber(),
				invoice.getCustomerName(),
				invoice.getYourOrderNumber(),
				invoice.getExternalInvoiceReference1(),
				invoice.getExternalInvoiceReference2(),
				nfmt.format(invoice.getTotal()),
				invoice.getCurrency(),
				invoice.getTermsOfPayment(),
				invoice.isNotCompleted()
				)
				;
		
	}
	
}

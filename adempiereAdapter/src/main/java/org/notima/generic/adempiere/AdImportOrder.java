package org.notima.generic.adempiere;

import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.OrderLine;
import org.notima.generic.ifacebusinessobjects.OrderInvoiceLine;

public class AdImportOrder {

	/**
	 * Splits a generic order into AdImportOrderLine
	 */
	public List<AdImportOrderLine> splitToOrderRows(Order order) {
		
		List<AdImportOrderLine> list = new ArrayList<AdImportOrderLine>();
		AdImportOrderLine l;
		
		OrderLine src;
		
		for (Object oo : order.getLines()) {

			src = (OrderLine)oo;
			
			l = new AdImportOrderLine();
			l.setBusinessPartnerNo(order.getBusinessPartner().getIdentityNo());
			l.setDocumentType(order.isSalesOrder() ? "Standard Order" : "Purchase Order");
			l.setSoTrx(order.isSalesOrder());
			l.setWarehouseId(Integer.parseInt(order.getWarehouseId()));
			l.setCurrencyIsoCode(order.getCurrency());
			l.setDateOrdered(order.getDocumentDate()!=null ? new java.sql.Timestamp(order.getDocumentDate().getTime()) : null);
			l.setProductKey(src.getProductKey());
			l.setQtyOrdered(src.getQtyEntered());
			l.setUnitPrice(src.getPriceActual());
			list.add(l);
			
		}
		
		return list;
		
	}
	
	/**
	 * Splits a generic invoice into AdImportOrderLine
	 */
	public List<AdImportOrderLine> splitToOrderRows(Invoice invoice) {
		
		List<AdImportOrderLine> list = new ArrayList<AdImportOrderLine>();
		AdImportOrderLine l;
		
		OrderInvoiceLine src;
		
		for (Object oo : invoice.getLines()) {

			src = (OrderInvoiceLine)oo;
			
			l = new AdImportOrderLine();
			if (invoice.getBusinessPartner()!=null)
				l.setBusinessPartnerNo(invoice.getBusinessPartner().getIdentityNo());
			l.setDocumentType("Standard Order");
			l.setSoTrx(true);
			if (invoice.getWarehouseId()!=null) {
				l.setWarehouseId(Integer.parseInt(invoice.getWarehouseId()));
			}
			l.setCurrencyIsoCode(invoice.getCurrency());
			l.setDateOrdered(invoice.getDocumentDate()!=null ? new java.sql.Timestamp(invoice.getDocumentDate().getTime()) : null);
			l.setProductKey("TC-" + src.getProductKey());
			l.setQtyOrdered(src.getQtyEntered());
			//l.setUnitPrice(src.getPriceActual());
			list.add(l);
			
		}
		
		return list;
		
	}
	
	
}

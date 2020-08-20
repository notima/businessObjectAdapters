package org.notima.generic.adempiere;

import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.businessobjects.Invoice;

public class AdImportInvoice {

	/**
	 * Splits a list of generic invoice into AdImportInvoiceLines
	 * 
	 * @param invoice
	 * @return A list of AdImportInvoiceLines
	 */
	public List<AdImportInvoiceLine> splitInvoiceToRows(Invoice invoice) {
		
		List<AdImportInvoiceLine> lst = new ArrayList<AdImportInvoiceLine>();
		AdImportInvoiceLine l;
		
		InvoiceLine src;
		
		for (Object oo : invoice.getLines()) {

			src = (InvoiceLine)oo;
			l = new AdImportInvoiceLine();
			l.setBusinessPartnerNo(invoice.getBusinessPartner().getIdentityNo());
			l.setBpDocumentNo(invoice.getDocumentKey());
			l.setCurrencyIsoCode(invoice.getCurrency());
			l.setDeliveryNote(invoice.getShipmentNo());
			l.setDocumentType(invoice.isCreditNote() ? "AP CreditMemo" : "AP Invoice");
			l.setDueDate(invoice.getDueDate()!=null ? new java.sql.Timestamp(invoice.getDueDate().getTime()) : null);
			l.setFreightAmt(invoice.getFreightAmount());
			l.setGrandTotal(invoice.getGrandTotal());
			l.setInvoiceDate(invoice.getDocumentDate()!=null ? new java.sql.Timestamp(invoice.getDocumentDate().getTime()) : null);
			l.setOurCustomerNo(invoice.getOurCustomerNo());
			l.setPoDocumentNo(src.getPoDocumentNo()!=null && src.getPoDocumentNo().trim().length()>0 ? src.getPoDocumentNo() : invoice.getPoDocumentNo());
			l.setPoReference(src.getPoReference()!=null && src.getPoReference().trim().length()>0 ? src.getPoReference() : invoice.getPoDocumentNo());
			l.setProductDescription(src.getDescription());
			l.setQtyInvoiced(src.getQtyEntered());
			l.setSoTrx(false);
			l.setUnitPrice(src.getPriceActual());
			l.setVendorProductNo(src.getVendorProductNo());
			lst.add(l);
			
		}
		
		return lst;
		
	}
	
}

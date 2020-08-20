package org.notima.generic.adempiere;

import java.util.ArrayList;
import java.util.List;

/**
 * Class used to map against I_Invoice table.
 * Use the toObjectList method to create a list of parameter values for insert/updates using the
 * camel sql component.
 * 
 * @author daniel.tamm
 *
 */
public class AdImportInvoiceLine {

	private String 	businessPartnerNo;
	private String	vendorProductNo;
	private Double	qtyInvoiced;
	private Double	unitPrice;
	private java.sql.Timestamp invoiceDate;
	private String	deliveryNote;
	private String	poReference;
	private String	poDocumentNo;
	private String	productDescription;
	private String	bpDocumentNo;
	private String	documentType;
	private java.sql.Timestamp dueDate;
	private Double	grandTotal;
	private Double	totalLines;
	private String	currencyIsoCode;
	private String	bpBankgiro;
	private String	bpPlusgiro;
	private Double	freightAmt;
	private String	ourCustomerNo;
	private String	filePath;
	private String	ocr;
	private boolean isSoTrx;
	private String filePath2; // not used
	private Double	lineTotalAmt;
	private Double	lineNetAmt;
	private String  UPC;
	

	/**
	 * Generates a list of objects that can be used with the camel sql component 
	 * when creating inserts/updates 
	 * 
	 * 
	 * @param src
	 * @return
	 */
	public static List<Object> toObjectList(AdImportInvoiceLine src) {
		
		List<Object> entries;
		
		entries = new ArrayList<Object>();
		entries.add(src.getBusinessPartnerNo());		// 1
		entries.add(src.getVendorProductNo());			// 2
		entries.add(src.getQtyInvoiced());				// 3
		entries.add(src.getUnitPrice());				// 4
		entries.add(src.getInvoiceDate());				// 5
		entries.add(src.getDeliveryNote());				// 6
		entries.add(src.getPoReference());				// 7
		entries.add(src.getPoDocumentNo());				// 8
		entries.add(src.getProductDescription());		// 9
		entries.add(src.getBpDocumentNo());				// 10
		entries.add(src.getDocumentType());				// 11	
		entries.add(src.isSoTrx() ? "Y" : "N");			// 12
		entries.add(src.getFilePath());				// 13 FilePath
		// entries.add(src.getFilePath2());			// FilePath2 not used 
		entries.add(src.getDueDate());				// 14 DueDate
		entries.add(src.getGrandTotal());			// 15 GrandTotal
		entries.add(src.getTotalLines());			// 16 TotalLines
		entries.add(src.getCurrencyIsoCode());		// 17 ISO_Code
		entries.add(src.getBpBankgiro());			// 18 BP_Bankgiro
		entries.add(src.getBpPlusgiro());			// 19 BP_Plusgiro
		entries.add(src.getFreightAmt());			// 20 FreightAmt
		entries.add(src.getPoReference() );		// 21 ReferenceNo
		entries.add(src.getOcr());					// 22 OCR
		entries.add(src.getLineNetAmt());			// 23 line net amt (excl tax)
		entries.add(src.getLineTotalAmt());			// 24 line total amt (incl. tax)
		entries.add(src.getUPC());					// 25
		
		return entries;
	}
	
	
	public String getBusinessPartnerNo() {
		return businessPartnerNo;
	}
	public void setBusinessPartnerNo(String businessPartnerNo) {
		this.businessPartnerNo = businessPartnerNo;
	}
	public String getVendorProductNo() {
		return vendorProductNo;
	}
	public void setVendorProductNo(String vendorProductNo) {
		this.vendorProductNo = vendorProductNo;
	}
	public Double getQtyInvoiced() {
		return qtyInvoiced;
	}
	public void setQtyInvoiced(Double qtyInvoiced) {
		this.qtyInvoiced = qtyInvoiced;
	}
	public Double getUnitPrice() {
		return unitPrice;
	}
	public void setUnitPrice(Double unitPrice) {
		this.unitPrice = unitPrice;
	}
	public java.sql.Timestamp getInvoiceDate() {
		return invoiceDate;
	}
	public void setInvoiceDate(java.sql.Timestamp invoiceDate) {
		this.invoiceDate = invoiceDate;
	}
	public String getDeliveryNote() {
		return deliveryNote;
	}
	public void setDeliveryNote(String deliveryNote) {
		this.deliveryNote = deliveryNote;
	}
	public String getPoReference() {
		return poReference;
	}
	public void setPoReference(String poReference) {
		this.poReference = poReference;
	}
	public String getProductDescription() {
		return productDescription;
	}
	public void setProductDescription(String productDescription) {
		this.productDescription = productDescription;
	}
	public String getBpDocumentNo() {
		return bpDocumentNo;
	}
	public void setBpDocumentNo(String bpDocumentNo) {
		this.bpDocumentNo = bpDocumentNo;
	}
	public String getDocumentType() {
		return documentType;
	}
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}
	public boolean isSoTrx() {
		return isSoTrx;
	}
	public void setSoTrx(boolean isSoTrx) {
		this.isSoTrx = isSoTrx;
	}

	public String getPoDocumentNo() {
		return poDocumentNo;
	}
	public void setPoDocumentNo(String poDocumentNo) {
		this.poDocumentNo = poDocumentNo;
	}
	
	public java.sql.Timestamp getDueDate() {
		return dueDate;
	}
	public void setDueDate(java.sql.Timestamp dueDate) {
		this.dueDate = dueDate;
	}
	public Double getGrandTotal() {
		return grandTotal;
	}
	public void setGrandTotal(Double grandTotal) {
		this.grandTotal = grandTotal;
	}
	public Double getTotalLines() {
		return totalLines;
	}
	public void setTotalLines(Double totalLines) {
		this.totalLines = totalLines;
	}
	public String getCurrencyIsoCode() {
		return currencyIsoCode;
	}
	public void setCurrencyIsoCode(String currencyIsoCode) {
		this.currencyIsoCode = currencyIsoCode;
	}
	public String getBpBankgiro() {
		return bpBankgiro;
	}
	public void setBpBankgiro(String bpBankgiro) {
		this.bpBankgiro = bpBankgiro;
	}
	public String getBpPlusgiro() {
		return bpPlusgiro;
	}
	public void setBpPlusgiro(String bpPlusgiro) {
		this.bpPlusgiro = bpPlusgiro;
	}
	public Double getFreightAmt() {
		return freightAmt;
	}
	public void setFreightAmt(Double freightAmt) {
		this.freightAmt = freightAmt;
	}
	public String getOurCustomerNo() {
		return ourCustomerNo;
	}
	public void setOurCustomerNo(String ourCustomerNo) {
		this.ourCustomerNo = ourCustomerNo;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getOcr() {
		return ocr;
	}
	public void setOcr(String ocr) {
		this.ocr = ocr;
	}
	public void setFilePath2(String imageFile2) {
		this.filePath2 = imageFile2;
	}
	public String getFilePath2() {
		return filePath2;
	}
	public Double getLineTotalAmt() {
		return lineTotalAmt;
	}
	public void setLineTotalAmt(Double lineTotalAmt) {
		this.lineTotalAmt = lineTotalAmt;
	}
	public Double getLineNetAmt() {
		return lineNetAmt;
	}
	public void setLineNetAmt(Double lineNetAmt) {
		this.lineNetAmt = lineNetAmt;
	}
	public void setUPC(String UPC) {
		this.UPC = UPC;
	}
	public String getUPC() {
		return this.UPC;
	}
	
}

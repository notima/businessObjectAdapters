package org.notima.generic.adempiere;

import java.util.ArrayList;
import java.util.List;

public class AdImportOrderLine {

	private String	businessPartnerNo;
	private Integer warehouseId;
	private String	vendorProductNo;
	private String	productKey;
	private Double	qtyOrdered;
	private Double	unitPrice;
	private String	currencyIsoCode;
	private java.sql.Timestamp dateOrdered;
	private String	documentType;
	private boolean	soTrx;

	/**
	 * Generates a list of objects that can be used with the camel sql component
	 * when creating inserts/updates
	 * 
	 * insert bpartnervalue, m_warehouse_id, vendorproductno, productvalue, qtyordered,
	 *        priceactual, iso_code, dateordered, doctypename
	 */
	public static List<Object> toObjectList(AdImportOrderLine src) {
		
		List<Object> entries;
		
		entries = new ArrayList<Object>();
		
		entries.add(src.getBusinessPartnerNo());
		entries.add(src.getWarehouseId());
		entries.add(src.getVendorProductNo());
		entries.add(src.getProductKey());
		entries.add(src.getQtyOrdered());
		entries.add(src.getUnitPrice());
		entries.add(src.getCurrencyIsoCode());
		entries.add(src.getDateOrdered());
		entries.add(src.getDocumentType());
		entries.add(src.isSoTrx() ? "Y" : "N");
		
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
	public String getProductKey() {
		return productKey;
	}
	public void setProductKey(String productKey) {
		this.productKey = productKey;
	}
	public Double getQtyOrdered() {
		return qtyOrdered;
	}
	public void setQtyOrdered(Double qtyOrdered) {
		this.qtyOrdered = qtyOrdered;
	}
	public Double getUnitPrice() {
		return unitPrice;
	}
	public void setUnitPrice(Double unitPrice) {
		this.unitPrice = unitPrice;
	}
	public String getCurrencyIsoCode() {
		return currencyIsoCode;
	}
	public void setCurrencyIsoCode(String currencyIsoCode) {
		this.currencyIsoCode = currencyIsoCode;
	}
	public java.sql.Timestamp getDateOrdered() {
		return dateOrdered;
	}
	public void setDateOrdered(java.sql.Timestamp dateOrdered) {
		this.dateOrdered = dateOrdered;
	}
	public String getDocumentType() {
		return documentType;
	}
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public Integer getWarehouseId() {
		return warehouseId;
	}

	public void setWarehouseId(Integer warehouseId) {
		this.warehouseId = warehouseId;
	}

	public boolean isSoTrx() {
		return soTrx;
	}

	public void setSoTrx(boolean soTrx) {
		this.soTrx = soTrx;
	}
	
	
	
}

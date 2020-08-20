package org.notima.generic.adempiere;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Order;

public class AdempiereOrder {

	private int		adClientId;
	private int		adOrgId;
	private boolean	soTrx;
	private String	documentNo;
	private String	docStatus;
	private int		docTypeId;
	private String	description;
	private int		salesrepId;
	private	Date	dateOrdered;
	private	Date	datePromised;
	private int		partnerId;
	private	int		partnerLocationId;
	private String	poReference;
	private String	currency;
	private String	paymentRule;
	private int		paymentTermId;
	private String	invoiceRule;
	private String	deliveryRule;
	private String	deliveryViaRule;
	private int		shipperId;
	private Double	totalLines;
	private	Double	grandTotal;
	private int		warehouseId;
	private int		pricelistId;
	private boolean	taxIncluded;
	private	int		userId;
	private int		billLocationId;
	private int		billUserId;
	private boolean	dropShip;
	
	/**
	 * Loads order from database
	 * 
	 * @param orderId
	 * @return
	 */
	public static Order load(int orderId, Connection conn) throws Exception {
		
		Order dst = new Order();
		
		PreparedStatement ps = conn.prepareStatement(
				"select ad_client_id, ad_org_id, issotrx, documentno, docstatus, c_doctype_id, " + 
				"description, salesrep_id, dateordered, datepromised, c_bpartner_id, c_bpartner_location_id, " + 
				"poreference, (select iso_code from c_currency c where c.c_currency_id=o.c_currency_id), paymentrule, " + 
				"c_paymentterm_id, invoicerule, deliveryrule, " + 
				"deliveryviarule, m_shipper_id, totallines, grandtotal, m_warehouse_id, m_pricelist_id, " + 
				"istaxincluded, ad_user_id, bill_location_id, bill_user_id, isdropship from c_order o where c_order_id=?"
			); 
		
		ps.setInt(1, orderId);
		ResultSet rs = ps.executeQuery();
		AdempiereOrder aOrder = null;
		if (rs.next()) {
			aOrder = new AdempiereOrder(rs);
		}
		rs.close();
		ps.close();
		
		if (aOrder==null)
			return null;
		
		BusinessPartner bpartner = AdempiereBusinessPartner.load(aOrder.getPartnerId(), conn);
		dst.setBpartner(bpartner);
		Location shipAddress = AdempiereLocation.convert(AdempiereLocation.findById(conn, aOrder.getPartnerLocationId()));
		dst.setShipLocation(shipAddress);
		if (aOrder.getBillLocationId()!=aOrder.getPartnerLocationId()) {
			Location billAddress = AdempiereLocation.convert(AdempiereLocation.findById(conn, aOrder.billLocationId));
			dst.setBillLocation(billAddress);
		}
		
		dst.setOrderKey(aOrder.getDocumentNo()); 		// DocumentNo
		dst.setCurrency(aOrder.getCurrency()); 		// Currency
		dst.setDateOrdered(aOrder.getDateOrdered());
		dst.setDatePromised(aOrder.getDatePromised());
		dst.setDeliveryRule(aOrder.getDeliveryRule());
		dst.setDeliveryViaRule(aOrder.getDeliveryViaRule());
		dst.setStatus(aOrder.getDocStatus());
		dst.setGrandTotal(aOrder.getGrandTotal());
		dst.setVatTotal(aOrder.getGrandTotal()-aOrder.getTotalLines());
		dst.setSalesOrder(aOrder.isSoTrx());
		
		// Read order lines
		List<AdempiereOrderLine> olines = AdempiereOrderLine.loadAllForOrder(orderId, conn);
		for (AdempiereOrderLine line : olines) {
			dst.addOrderLine(AdempiereOrderLine.convert(line));
		}
		
		return dst;
		
	}

	public AdempiereOrder() {}
	
	public AdempiereOrder(ResultSet rs) throws SQLException {
		int c=1;
		adClientId = rs.getInt(c++);
		adOrgId = rs.getInt(c++);
		soTrx = "Y".equalsIgnoreCase(rs.getString(c++));
		documentNo = rs.getString(c++);
		docStatus = rs.getString(c++);
		docTypeId = rs.getInt(c++);
		description = rs.getString(c++);
		salesrepId = rs.getInt(c++);
		dateOrdered = rs.getDate(c++);
		datePromised = rs.getDate(c++);
		partnerId = rs.getInt(c++);
		partnerLocationId = rs.getInt(c++);
		poReference = rs.getString(c++);
		currency = rs.getString(c++);
		paymentRule = rs.getString(c++);
		paymentTermId = rs.getInt(c++);
		invoiceRule = rs.getString(c++);
		deliveryRule = rs.getString(c++);
		deliveryViaRule = rs.getString(c++);
		shipperId = rs.getInt(c++);
		totalLines = rs.getDouble(c++);
		grandTotal = rs.getDouble(c++);
		warehouseId = rs.getInt(c++);
		pricelistId = rs.getInt(c++);
		taxIncluded = "Y".equalsIgnoreCase(rs.getString(c++));
		userId = rs.getInt(c++);
		billLocationId = rs.getInt(c++);
		billUserId = rs.getInt(c++);
		dropShip = "Y".equalsIgnoreCase(rs.getString(c++));
	}
	
	public int getAdClientId() {
		return adClientId;
	}

	public void setAdClientId(int adClientId) {
		this.adClientId = adClientId;
	}

	public int getAdOrgId() {
		return adOrgId;
	}

	public void setAdOrgId(int adOrgId) {
		this.adOrgId = adOrgId;
	}

	public boolean isSoTrx() {
		return soTrx;
	}

	public void setSoTrx(boolean soTrx) {
		this.soTrx = soTrx;
	}

	public String getDocumentNo() {
		return documentNo;
	}

	public void setDocumentNo(String documentNo) {
		this.documentNo = documentNo;
	}

	public String getDocStatus() {
		return docStatus;
	}

	public void setDocStatus(String docStatus) {
		this.docStatus = docStatus;
	}

	public int getDocTypeId() {
		return docTypeId;
	}

	public void setDocTypeId(int docTypeId) {
		this.docTypeId = docTypeId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getSalesrepId() {
		return salesrepId;
	}

	public void setSalesrepId(int salesrepId) {
		this.salesrepId = salesrepId;
	}

	public Date getDateOrdered() {
		return dateOrdered;
	}

	public void setDateOrdered(Date dateOrdered) {
		this.dateOrdered = dateOrdered;
	}

	public Date getDatePromised() {
		return datePromised;
	}

	public void setDatePromised(Date datePromised) {
		this.datePromised = datePromised;
	}

	public int getPartnerId() {
		return partnerId;
	}

	public void setPartnerId(int partnerId) {
		this.partnerId = partnerId;
	}

	public int getPartnerLocationId() {
		return partnerLocationId;
	}

	public void setPartnerLocationId(int partnerLocationId) {
		this.partnerLocationId = partnerLocationId;
	}

	public String getPoReference() {
		return poReference;
	}

	public void setPoReference(String poReference) {
		this.poReference = poReference;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getPaymentRule() {
		return paymentRule;
	}

	public void setPaymentRule(String paymentRule) {
		this.paymentRule = paymentRule;
	}

	public int getPaymentTermId() {
		return paymentTermId;
	}

	public void setPaymentTermId(int paymentTermId) {
		this.paymentTermId = paymentTermId;
	}

	public String getInvoiceRule() {
		return invoiceRule;
	}

	public void setInvoiceRule(String invoiceRule) {
		this.invoiceRule = invoiceRule;
	}

	public String getDeliveryRule() {
		return deliveryRule;
	}

	public void setDeliveryRule(String deliveryRule) {
		this.deliveryRule = deliveryRule;
	}

	public String getDeliveryViaRule() {
		return deliveryViaRule;
	}

	public void setDeliveryViaRule(String deliveryViaRule) {
		this.deliveryViaRule = deliveryViaRule;
	}

	public int getShipperId() {
		return shipperId;
	}

	public void setShipperId(int shipperId) {
		this.shipperId = shipperId;
	}

	public Double getTotalLines() {
		return totalLines;
	}

	public void setTotalLines(Double totalLines) {
		this.totalLines = totalLines;
	}

	public Double getGrandTotal() {
		return grandTotal;
	}

	public void setGrandTotal(Double grandTotal) {
		this.grandTotal = grandTotal;
	}

	public int getWarehouseId() {
		return warehouseId;
	}

	public void setWarehouseId(int warehouseId) {
		this.warehouseId = warehouseId;
	}

	public int getPricelistId() {
		return pricelistId;
	}

	public void setPricelistId(int pricelistId) {
		this.pricelistId = pricelistId;
	}

	public boolean isTaxIncluded() {
		return taxIncluded;
	}

	public void setTaxIncluded(boolean taxIncluded) {
		this.taxIncluded = taxIncluded;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getBillLocationId() {
		return billLocationId;
	}

	public void setBillLocationId(int billLocationId) {
		this.billLocationId = billLocationId;
	}

	public int getBillUserId() {
		return billUserId;
	}

	public void setBillUserId(int billUserId) {
		this.billUserId = billUserId;
	}

	public boolean isDropShip() {
		return dropShip;
	}

	public void setDropShip(boolean dropShip) {
		this.dropShip = dropShip;
	}
	
	
	
}

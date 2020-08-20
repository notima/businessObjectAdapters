package org.notima.generic.adempiere;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.OrderLine;

public class AdempiereOrderLine {

	private int		orderlineId;
	private int		orderId;
	private int		lineId;
	private Date	dateOrdered;
	private Date	datePromised;
	private Date	dateDelivered;
	private Date	dateInvoiced;
	private String	description;
	private int		productId;
	private String	productNo;
	private String	productName;
	private int		warehouseId;
	private int		uomId;
	private String	uom;
	private double	qtyOrdered;
	private double	qtyReserved;
	private double	qtyDelivered;
	private double	qtyInvoiced;
	private double	priceActual;
	private double	lineNetAmt;
	private int		taxId;
	private double	taxRate;
	private double	discount;
	private int		chargeId;
	private double	priceList;
	
	public static String selectSql =
			"select c_orderline_id, c_order_id, line, dateordered, datepromised, datedelivered, dateinvoiced, " +
			"ol.description, ol.m_product_id, p.value, p.name, m_warehouse_id, ol.c_uom_id, u.x12de355, qtyordered, " + 
			"qtyreserved, qtydelivered, qtyinvoiced, priceactual, " + 
			"pricelist, linenetamt, discount, c_charge_id, ol.c_tax_id, t.rate from c_orderline ol " + 
			"left join m_product p on (ol.m_product_id=p.m_product_id) " + 
			"left join c_uom u on (ol.c_uom_id=u.c_uom_id) " + 
			"left join c_tax t on (ol.c_tax_id=t.c_tax_id) ";

	public static List<AdempiereOrderLine> loadAllForOrder(int orderId, Connection conn) throws Exception {
		
		List<AdempiereOrderLine> result = new ArrayList<AdempiereOrderLine>();

		PreparedStatement ps = conn.prepareStatement(selectSql + " where c_order_id=?");
		ps.setInt(1, orderId);
		ResultSet rs = ps.executeQuery();
		
		while(rs.next()) {
			result.add(new AdempiereOrderLine(rs));
		}
		rs.close();
		ps.close();
		
		return result;
		
	}
	
	/**
	 * Converts Adempiere Order Line to Business objects line
	 * 
	 * @param src
	 * @return
	 */
	public static OrderLine convert(AdempiereOrderLine src) {

		OrderLine dst = new OrderLine();
		dst.setProductKey(src.getProductNo());
		dst.setName(src.getProductName());
		dst.setLineNo(src.getLineId());
		dst.setPriceActual(src.getPriceActual());
		dst.setQtyEntered(src.getQtyOrdered());
		dst.setTaxKey(Integer.toString(src.getTaxId()));
		dst.setTaxPercent(src.getTaxRate());
		dst.setUOM(src.getUom());
		dst.setDescription(src.getDescription());
		
		return dst;
		
	}
	
	public AdempiereOrderLine(ResultSet rs) throws Exception {
		int c=1;
		orderlineId = rs.getInt(c++);
		orderId = rs.getInt(c++);
		lineId = rs.getInt(c++);
		dateOrdered = rs.getDate(c++);
		datePromised = rs.getDate(c++);
		dateDelivered = rs.getDate(c++);
		dateInvoiced = rs.getDate(c++);
		description = rs.getString(c++);
		productId = rs.getInt(c++);
		productNo = rs.getString(c++);
		productName = rs.getString(c++);
		warehouseId = rs.getInt(c++);
		uomId = rs.getInt(c++);
		uom = rs.getString(c++);
		qtyOrdered = rs.getDouble(c++);
		qtyReserved = rs.getDouble(c++);
		qtyDelivered = rs.getDouble(c++);
		qtyInvoiced = rs.getDouble(c++);
		priceActual = rs.getDouble(c++);
		priceList = rs.getDouble(c++);
		lineNetAmt = rs.getDouble(c++);
		discount = rs.getDouble(c++);
		chargeId = rs.getInt(c++);
		taxId = rs.getInt(c++);
		taxRate = rs.getDouble(c++);
	}
	
	public int getOrderlineId() {
		return orderlineId;
	}
	public void setOrderlineId(int orderlineId) {
		this.orderlineId = orderlineId;
	}
	public int getLineId() {
		return lineId;
	}
	public void setLineId(int lineId) {
		this.lineId = lineId;
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
	public Date getDateDelivered() {
		return dateDelivered;
	}
	public void setDateDelivered(Date dateDelivered) {
		this.dateDelivered = dateDelivered;
	}
	public Date getDateInvoiced() {
		return dateInvoiced;
	}
	public void setDateInvoiced(Date dateInvoiced) {
		this.dateInvoiced = dateInvoiced;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public int getProductId() {
		return productId;
	}
	public void setProductId(int productId) {
		this.productId = productId;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public int getWarehouseId() {
		return warehouseId;
	}
	public void setWarehouseId(int warehouseId) {
		this.warehouseId = warehouseId;
	}
	public int getUomId() {
		return uomId;
	}
	public void setUomId(int uomId) {
		this.uomId = uomId;
	}
	public String getUom() {
		return uom;
	}
	public void setUom(String uom) {
		this.uom = uom;
	}
	public double getQtyOrdered() {
		return qtyOrdered;
	}
	public void setQtyOrdered(double qtyOrdered) {
		this.qtyOrdered = qtyOrdered;
	}
	public double getQtyReserved() {
		return qtyReserved;
	}
	public void setQtyReserved(double qtyReserved) {
		this.qtyReserved = qtyReserved;
	}
	public double getQtyDelivered() {
		return qtyDelivered;
	}
	public void setQtyDelivered(double qtyDelivered) {
		this.qtyDelivered = qtyDelivered;
	}
	public double getQtyInvoiced() {
		return qtyInvoiced;
	}
	public void setQtyInvoiced(double qtyInvoiced) {
		this.qtyInvoiced = qtyInvoiced;
	}
	public double getPriceActual() {
		return priceActual;
	}
	public void setPriceActual(double priceActual) {
		this.priceActual = priceActual;
	}
	public double getLineNetAmt() {
		return lineNetAmt;
	}
	public void setLineNetAmt(double lineNetAmt) {
		this.lineNetAmt = lineNetAmt;
	}
	public int getTaxId() {
		return taxId;
	}
	public void setTaxId(int taxId) {
		this.taxId = taxId;
	}
	public double getTaxRate() {
		return taxRate;
	}
	public void setTaxRate(double taxRate) {
		this.taxRate = taxRate;
	}
	public double getDiscount() {
		return discount;
	}
	public void setDiscount(double discount) {
		this.discount = discount;
	}
	
	public int getOrderId() {
		return orderId;
	}
	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}
	
	public int getChargeId() {
		return chargeId;
	}
	public void setChargeId(int chargeId) {
		this.chargeId = chargeId;
	}
	public double getPriceList() {
		return priceList;
	}
	public void setPriceList(double priceList) {
		this.priceList = priceList;
	}

	public String getProductNo() {
		return productNo;
	}

	public void setProductNo(String productNo) {
		this.productNo = productNo;
	}
	
	
	
}

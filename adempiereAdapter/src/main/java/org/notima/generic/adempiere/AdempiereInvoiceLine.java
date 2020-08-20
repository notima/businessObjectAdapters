package org.notima.generic.adempiere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.InvoiceLine;

public class AdempiereInvoiceLine {

	private int		invoicelineId;
	private int		invoiceId;
	private int		lineId;
	private String	description;
	private int		productId;
	private String	productNo;
	private String	productName;
	private int		uomId;
	private String	uom;
	private double	qtyInvoiced;
	private double	priceActual;
	private double	lineNetAmt;
	private double	taxAmt;
	private int		chargeId;
	private int		taxId;
	private double	taxRate;

	
	public static String selectSql =
			"select c_invoiceline_id, c_invoice_id, line, " +
			"il.description, il.m_product_id, p.value, p.name, il.c_uom_id, u.x12de355," + 
			"qtyinvoiced, priceactual, " + 
			"linenetamt, taxamt, c_charge_id, il.c_tax_id, t.rate from c_Invoiceline il " + 
			"left join m_product p on (il.m_product_id=p.m_product_id) " + 
			"left join c_uom u on (il.c_uom_id=u.c_uom_id) " + 
			"left join c_tax t on (il.c_tax_id=t.c_tax_id) ";

	public static List<AdempiereInvoiceLine> loadAllForInvoice(int invoiceId, Connection conn) throws Exception {
		
		List<AdempiereInvoiceLine> result = new ArrayList<AdempiereInvoiceLine>();

		PreparedStatement ps = conn.prepareStatement(selectSql + " where c_invoice_id=? order by line asc");
		ps.setInt(1, invoiceId);
		ResultSet rs = ps.executeQuery();
		
		while(rs.next()) {
			result.add(new AdempiereInvoiceLine(rs));
		}
		rs.close();
		ps.close();
		
		// Check for charges
		AdempiereCharge c;
		for (AdempiereInvoiceLine il : result) {
			if (il.getChargeId()>0) {
				// TODO: Make language dependent on BP-document-settings
				c = AdempiereCharge.loadCharge(il.getChargeId(), "sv_SE", conn);
				il.setProductName(c.getName());
			}
		}
		
		return result;
		
	}
	
	/**
	 * Converts Adempiere Invoice Line to Business objects line
	 * 
	 * @param src
	 * @return
	 */
	public static InvoiceLine convert(AdempiereInvoiceLine src) {

		InvoiceLine dst = new InvoiceLine();
		dst.setProductKey(src.getProductNo());
		dst.setName(src.getProductName());
		dst.setLineNo(src.getLineId());
		dst.setPriceActual(src.getPriceActual());
		dst.setQtyEntered(src.getQtyInvoiced());
		dst.setTaxKey(Integer.toString(src.getTaxId()));
		dst.setTaxPercent(src.getTaxRate());
		dst.setTaxAmount(src.getTaxAmt());
		dst.setLineNet(src.getLineNetAmt());
		dst.setUOM(src.getUom());
		dst.setDescription(src.getDescription());
		
		return dst;
		
	}
	
	public AdempiereInvoiceLine(ResultSet rs) throws Exception {
		int c=1;
		invoicelineId = rs.getInt(c++);
		invoiceId = rs.getInt(c++);
		lineId = rs.getInt(c++);
		description = rs.getString(c++);
		productId = rs.getInt(c++);
		productNo = rs.getString(c++);
		productName = rs.getString(c++);
		uomId = rs.getInt(c++);
		uom = rs.getString(c++);
		qtyInvoiced = rs.getDouble(c++);
		priceActual = rs.getDouble(c++);
		lineNetAmt = rs.getDouble(c++);
		taxAmt = rs.getDouble(c++);
		chargeId = rs.getInt(c++);
		taxId = rs.getInt(c++);
		taxRate = rs.getDouble(c++);
	}
	

	public int getLineId() {
		return lineId;
	}
	public void setLineId(int lineId) {
		this.lineId = lineId;
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
	
	public double getTaxAmt() {
		return taxAmt;
	}

	public void setTaxAmt(double taxAmt) {
		this.taxAmt = taxAmt;
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
	public int getChargeId() {
		return chargeId;
	}
	public void setChargeId(int chargeId) {
		this.chargeId = chargeId;
	}

	public String getProductNo() {
		return productNo;
	}

	public void setProductNo(String productNo) {
		this.productNo = productNo;
	}

	public int getInvoicelineId() {
		return invoicelineId;
	}

	public void setInvoicelineId(int invoicelineId) {
		this.invoicelineId = invoicelineId;
	}

	public int getInvoiceId() {
		return invoiceId;
	}

	public void setInvoiceId(int invoiceId) {
		this.invoiceId = invoiceId;
	}
	
	
	
}

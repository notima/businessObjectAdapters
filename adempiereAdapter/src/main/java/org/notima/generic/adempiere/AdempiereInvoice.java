package org.notima.generic.adempiere;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Person;
import org.notima.generic.businessobjects.PriceList;

public class AdempiereInvoice {

	private int		adClientId;
	private int		adOrgId;
	private int		invoiceId;
	private boolean	soTrx;
	private String	documentNo;
	private String	docStatus;
	private int		docTypeId;
	private String	description;
	private int		salesrepId;
	private	Date	dateInvoiced;
	private	Date	datePay;
	private int		partnerId;
	private	int		partnerLocationId;
	private String	poReference;
	private String	currency;
	private String	paymentRule;
	private String	invoiceRule;
	private Double	totalLines;
	private	Double	grandTotal;
	private int		pricelistId;
	private boolean	taxIncluded;
	private	int		userId;
	private String 	ocr;
	
	
	private static String selectSql = 
			"select ad_client_id, ad_org_id, c_invoice_id, documentno, docstatus, c_doctype_id, " + 
			"description, salesrep_id, dateinvoiced, coalesce(i.duedate, paymenttermduedate(i.c_paymentterm_id, i.dateinvoiced)), c_bpartner_id, c_bpartner_location_id, " + 
			"poreference, (select iso_code from c_currency c where c.c_currency_id=i.c_currency_id), paymentrule, " + 
			"c_paymentterm_id, " + 
			"totallines, grandtotal, m_pricelist_id, " + 
			"istaxincluded, ad_user_id, ocr FROM c_invoice i ";

	/**
	 * Loads invoice from database
	 * 
	 * @param invoiceId
	 * @return
	 */
	public static Invoice load(int invoiceId, Connection conn) throws Exception {
		
		PreparedStatement ps = conn.prepareStatement(
				selectSql + " WHERE c_invoice_id=?"
			);
		
		ps.setInt(1, invoiceId);
		ResultSet rs = ps.executeQuery();
		AdempiereInvoice aInvoice = null;
		if (rs.next()) {
			aInvoice = new AdempiereInvoice(rs);
		}
		rs.close();
		ps.close();
		
		if (aInvoice==null)
			return null;
		
		
		return enrichInvoice(aInvoice, conn);
		
	}

	/**
	 * Loads invoice using document number
	 * 
	 * @param documentNo
	 * @param adClientId
	 * @param adOrgNo
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public static Invoice load(String documentNo, int adClientId, int adOrgNo, Connection conn) throws Exception {

		PreparedStatement ps = conn.prepareStatement(
				selectSql + " WHERE documentno=? and ad_client_id=? and (ad_org_id=0 or ad_org_id=?)"
			);
		
		ps.setString(1, documentNo);
		ps.setInt(2, adClientId);
		ps.setInt(3, adOrgNo);
		ResultSet rs = ps.executeQuery();
		AdempiereInvoice aInvoice = null;
		if (rs.next()) {
			aInvoice = new AdempiereInvoice(rs);
		}
		rs.close();
		ps.close();
		
		if (aInvoice==null)
			return null;
		
		return enrichInvoice(aInvoice, conn);
		
	}
	
	public AdempiereInvoice() {}
	
	public AdempiereInvoice(ResultSet rs) throws SQLException {
		int c=1;
		adClientId = rs.getInt(c++);
		adOrgId = rs.getInt(c++);
		invoiceId = rs.getInt(c++);
		documentNo = rs.getString(c++);
		docStatus = rs.getString(c++);
		docTypeId = rs.getInt(c++);
		description = rs.getString(c++);
		salesrepId = rs.getInt(c++);
		dateInvoiced = rs.getDate(c++);
		datePay = rs.getDate(c++);
		partnerId = rs.getInt(c++);
		partnerLocationId = rs.getInt(c++);
		poReference = rs.getString(c++);
		currency = rs.getString(c++);
		paymentRule = rs.getString(c++);
		invoiceRule = rs.getString(c++);;
		totalLines = rs.getDouble(c++);
		grandTotal = rs.getDouble(c++);
		pricelistId = rs.getInt(c++);
		taxIncluded = "Y".equalsIgnoreCase(rs.getString(c++));
		userId = rs.getInt(c++);
		ocr = rs.getString(c++);
	}

	public static Invoice enrichInvoice(AdempiereInvoice aInvoice, Connection conn) throws Exception {

		Invoice dst = new Invoice();		
		
		BusinessPartner bpartner = AdempiereBusinessPartner.load(aInvoice.getPartnerId(), conn);
		PriceList pl = AdPriceList.loadPriceList(aInvoice.pricelistId, conn);
		dst.setRoundingDecimals(pl!=null ? pl.getPricePrecision() : 2);
		
		dst.setBillBpartner(bpartner);
		
		BusinessPartner sender = AdempiereBusinessPartner.loadOrgBp(aInvoice.getAdOrgId(), conn);
		dst.setSender(sender);
		
		Location billAddress = AdempiereLocation.convert(AdempiereLocation.findById(conn, aInvoice.getPartnerLocationId()));
		dst.setBillLocation(billAddress);
		
		dst.setInvoiceKey(aInvoice.getDocumentNo()); 		// DocumentNo
		dst.setCurrency(aInvoice.getCurrency()); 		// Currency
		dst.setInvoiceDate(aInvoice.getDateInvoiced());
		dst.setDueDate(aInvoice.getDatePay());
		dst.setStatus(aInvoice.getDocStatus());
		dst.setGrandTotal(aInvoice.getGrandTotal());
		dst.setVatTotal(aInvoice.getGrandTotal()-aInvoice.getTotalLines());
		dst.setNetTotal(aInvoice.getTotalLines());
		dst.setOcr(aInvoice.getOcr());
		dst.setPoDocumentNo(aInvoice.getPoReference());
		if (aInvoice.getUserId()>0) {
			Person theirRef = new Person();
			AdempiereContact c = AdempiereContact.load(conn, aInvoice.getUserId());
			theirRef.setName(c.getName());
			theirRef.setEmail(c.getEmail());
			dst.setBillPerson(theirRef);
		}
		
		// Read order lines
		List<AdempiereInvoiceLine> ilines = AdempiereInvoiceLine.loadAllForInvoice(aInvoice.invoiceId, conn);
		for (AdempiereInvoiceLine line : ilines) {
			dst.addInvoiceLine(AdempiereInvoiceLine.convert(line));
		}
		
		return dst;
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

	public Date getDateInvoiced() {
		return dateInvoiced;
	}

	public void setDateInvoiced(Date dateInvoiced) {
		this.dateInvoiced = dateInvoiced;
	}

	public Date getDatePay() {
		return datePay;
	}

	public void setDatePay(Date datePay) {
		this.datePay = datePay;
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

	public String getInvoiceRule() {
		return invoiceRule;
	}

	public void setInvoiceRule(String invoiceRule) {
		this.invoiceRule = invoiceRule;
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
	public String getOcr() {
		return ocr;
	}
	public void setOcr(String ocr) {
		this.ocr = ocr;
	}

	
	
}

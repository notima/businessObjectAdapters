package org.notima.ratepay;

import java.time.LocalDate;
import java.util.List;

import org.notima.generic.businessobjects.TaxSubjectIdentifier;

public class RatepayReport {

	private	String					fullPath;
	private	LocalDate				settlementDate;
	private String					shopId;
	private String					shopName;
	private TaxSubjectIdentifier 	taxSubject;
	private String					currency;
	
	private List<RatepayReportRow>	reportRows;
	
	public String getFullPath() {
		return fullPath;
	}
	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}
	public LocalDate getSettlementDate() {
		return settlementDate;
	}
	public void setSettlementDate(LocalDate settlementDate) {
		this.settlementDate = settlementDate;
	}
	public String getShopId() {
		return shopId;
	}
	public void setShopId(String shopId) {
		this.shopId = shopId;
	}
	public String getShopName() {
		return shopName;
	}
	public void setShopName(String shopName) {
		this.shopName = shopName;
	}
	public TaxSubjectIdentifier getTaxSubject() {
		return taxSubject;
	}
	public void setTaxSubject(TaxSubjectIdentifier taxSubject) {
		this.taxSubject = taxSubject;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public List<RatepayReportRow> getReportRows() {
		return reportRows;
	}
	public void setReportRows(List<RatepayReportRow> reportRows) {
		this.reportRows = reportRows;
	}
	
	
	
}

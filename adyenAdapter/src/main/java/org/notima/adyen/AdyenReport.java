package org.notima.adyen;

import java.time.LocalDate;
import java.util.List;

import org.notima.generic.businessobjects.PayoutFee;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.PaymentReportGeneric;
import org.notima.generic.ifacebusinessobjects.PaymentReportRow;

public class AdyenReport implements PaymentReportGeneric {

	private	String					fullPath;
	private	LocalDate				settlementDate;
	private String					shopId;
	private String					shopName;
	private TaxSubjectIdentifier 	taxSubject;
	private String					currency;
	private double					totalAmount = 0;
	
	private List<PaymentReportRow>	reportRows;
	private List<PaymentReportRow>	payoutRows;
	private List<PayoutFee>	feeRows;
	
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
	public List<PaymentReportRow> getReportRows() {
		return reportRows;
	}
	public void setReportRows(List<PaymentReportRow> reportRows) {
		this.reportRows = reportRows;
	}
	public List<PaymentReportRow> getPayoutRows() {
		return payoutRows;
	}
	public void setPayoutRows(List<PaymentReportRow> payoutRows) {
		this.payoutRows = payoutRows;
	}
	
	public List<PayoutFee> getFeeRows() {
		return feeRows;
	}
	public void setFeeRows(List<PayoutFee> feeRows) {
		this.feeRows = feeRows;
	}
	public double getTotalAmount() {
		return totalAmount;
	}
	
	/**
	 * Adds amount to total amount
	 * 
	 * @param payout
	 */
	public void addToTotalAmount(double payout) {
		totalAmount += payout;
	}
	
	
}

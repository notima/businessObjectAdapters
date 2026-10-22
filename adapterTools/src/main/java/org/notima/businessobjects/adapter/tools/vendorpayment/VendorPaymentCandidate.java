package org.notima.businessobjects.adapter.tools.vendorpayment;

import java.util.Date;

import org.notima.businessobjects.adapter.tools.tenant.TenantCandidate;

/**
 * The vendor payment status of one tenant: whether it has vendor invoices to pay, and a
 * summary of them. Produced by {@link VendorPaymentCandidateFinder}; contains only data,
 * so it can be presented in any form (console table, file, e-mail).
 */
public class VendorPaymentCandidate implements TenantCandidate {

	public enum Status {
		/** Has vendor invoices to pay, some of them already past their due date. */
		OVERDUE,
		/** Has vendor invoices that fall due within the period checked. */
		DUE_SOON,
		/** Has no vendor invoices to pay within the period checked. */
		NOTHING_DUE,
		/** Couldn't be checked, see {@link VendorPaymentCandidate#getError()}. */
		ERROR
	}

	private String	adapterName;
	private String	taxId;
	private String	countryCode;
	private String	name;

	private Status	status;
	private String	error;

	private int		invoices;
	private int		suppliers;
	private double	amount;
	/** The currency of the invoices to pay; null if none, "*" if several. */
	private String	currency;
	private int		overdueInvoices;
	private double	overdueAmount;
	private int		unbookedInvoices;
	private Date	earliestDueDate;
	private int		maxDaysOverdue;

	public String getAdapterName() {
		return adapterName;
	}
	public void setAdapterName(String adapterName) {
		this.adapterName = adapterName;
	}
	public String getTaxId() {
		return taxId;
	}
	public void setTaxId(String taxId) {
		this.taxId = taxId;
	}
	public String getCountryCode() {
		return countryCode;
	}
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	/** @return	True if the tenant has vendor invoices to pay. */
	public boolean needsPayment() {
		return status == Status.OVERDUE || status == Status.DUE_SOON;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	@Override
	public void markError(String error) {
		this.status = Status.ERROR;
		this.error = error;
	}
	/** @return	The number of vendor invoices to pay. */
	public int getInvoices() {
		return invoices;
	}
	public void setInvoices(int invoices) {
		this.invoices = invoices;
	}
	/** @return	The number of suppliers to pay. */
	public int getSuppliers() {
		return suppliers;
	}
	public void setSuppliers(int suppliers) {
		this.suppliers = suppliers;
	}
	/** @return	The amount to pay, after deducting the suppliers' credit notes. */
	public double getAmount() {
		return amount;
	}
	public void setAmount(double amount) {
		this.amount = amount;
	}
	/** @return	The currency of the invoices to pay; null if none, "*" if several. */
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	/** @return	The number of invoices to pay that are past their due date. */
	public int getOverdueInvoices() {
		return overdueInvoices;
	}
	public void setOverdueInvoices(int overdueInvoices) {
		this.overdueInvoices = overdueInvoices;
	}
	/** @return	The part of {@link #getAmount()} that is past its due date. */
	public double getOverdueAmount() {
		return overdueAmount;
	}
	public void setOverdueAmount(double overdueAmount) {
		this.overdueAmount = overdueAmount;
	}
	/** @return	The number of invoices to pay that aren't booked yet, ie need booking or approval before payment. */
	public int getUnbookedInvoices() {
		return unbookedInvoices;
	}
	public void setUnbookedInvoices(int unbookedInvoices) {
		this.unbookedInvoices = unbookedInvoices;
	}
	public Date getEarliestDueDate() {
		return earliestDueDate;
	}
	public void setEarliestDueDate(Date earliestDueDate) {
		this.earliestDueDate = earliestDueDate;
	}
	/** @return	Days since the due date of the oldest overdue invoice; 0 if none is overdue. */
	public int getMaxDaysOverdue() {
		return maxDaysOverdue;
	}
	public void setMaxDaysOverdue(int maxDaysOverdue) {
		this.maxDaysOverdue = maxDaysOverdue;
	}

}

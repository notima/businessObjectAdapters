package org.notima.businessobjects.adapter.tools.dunning;

import java.util.Date;

/**
 * The dunning status of one tenant: whether it has overdue customer invoices to remind,
 * and a summary of them. Produced by {@link DunningCandidateFinder}; contains only data,
 * so it can be presented in any form (console table, file, e-mail).
 */
public class DunningCandidate {

	public enum Status {
		/** Has overdue invoices to remind. */
		NEEDS_DUNNING,
		/** Has no overdue invoices to remind. */
		NOTHING_OVERDUE,
		/** Couldn't be checked, see {@link DunningCandidate#getError()}. */
		ERROR
	}

	private String	adapterName;
	private String	taxId;
	private String	countryCode;
	private String	name;

	private Status	status;
	private String	error;

	private int		overdueInvoices;
	private int		customers;
	private double	overdueAmount;
	/** The currency of the overdue invoices; null if none, "*" if several. */
	private String	currency;
	private Date	oldestDueDate;
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
	public boolean needsDunning() {
		return status == Status.NEEDS_DUNNING;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	/** @return	The number of overdue invoices to remind. */
	public int getOverdueInvoices() {
		return overdueInvoices;
	}
	public void setOverdueInvoices(int overdueInvoices) {
		this.overdueInvoices = overdueInvoices;
	}
	/** @return	The number of customers with overdue invoices, ie the number of reminders. */
	public int getCustomers() {
		return customers;
	}
	public void setCustomers(int customers) {
		this.customers = customers;
	}
	/** @return	The open amount of the overdue invoices. */
	public double getOverdueAmount() {
		return overdueAmount;
	}
	public void setOverdueAmount(double overdueAmount) {
		this.overdueAmount = overdueAmount;
	}
	/** @return	The currency of the overdue invoices; null if none, "*" if several. */
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public Date getOldestDueDate() {
		return oldestDueDate;
	}
	public void setOldestDueDate(Date oldestDueDate) {
		this.oldestDueDate = oldestDueDate;
	}
	/** @return	Days since the due date of the oldest overdue invoice. */
	public int getMaxDaysOverdue() {
		return maxDaysOverdue;
	}
	public void setMaxDaysOverdue(int maxDaysOverdue) {
		this.maxDaysOverdue = maxDaysOverdue;
	}

}

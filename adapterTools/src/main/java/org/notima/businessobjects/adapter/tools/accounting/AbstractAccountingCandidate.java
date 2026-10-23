package org.notima.businessobjects.adapter.tools.accounting;

import org.notima.businessobjects.adapter.tools.tenant.TenantCandidate;

/** The common part of the results of the accounting queries over tenants. */
public abstract class AbstractAccountingCandidate implements TenantCandidate {

	private String	adapterName;
	private String	taxId;
	private String	countryCode;
	private String	name;
	private TenantAccountingStatus status;
	private String	error;
	/** The fiscal year read, ie "2026". */
	private String	fiscalYear;

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
	public TenantAccountingStatus getStatus() {
		return status;
	}
	public void setStatus(TenantAccountingStatus status) {
		this.status = status;
	}
	public boolean isOk() {
		return status == TenantAccountingStatus.OK;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	@Override
	public void markError(String error) {
		this.status = TenantAccountingStatus.ERROR;
		this.error = error;
	}
	public String getFiscalYear() {
		return fiscalYear;
	}
	public void setFiscalYear(String fiscalYear) {
		this.fiscalYear = fiscalYear;
	}

	/** Status text for a table, ie "No accounting" or "Error: …". */
	public String getStatusText() {
		if (status == null) return "";
		switch (status) {
			case OK:				return "OK";
			case NO_ACCOUNTING:		return "No accounting";
			case NO_FISCAL_YEAR:	return "No fiscal year";
			default:				return "Error: " + (error != null ? error : "");
		}
	}

}

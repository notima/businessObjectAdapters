package org.notima.businessobjects.adapter.tools.accounting;

/** Whether the accounting of a tenant could be read, for the accounting queries over tenants. */
public enum TenantAccountingStatus {
	/** The accounting was read. */
	OK,
	/** No accounting model provider has the tenant's accounting. */
	NO_ACCOUNTING,
	/** The tenant has no fiscal year for the date asked for. */
	NO_FISCAL_YEAR,
	/** The accounting couldn't be read, see the error. */
	ERROR
}

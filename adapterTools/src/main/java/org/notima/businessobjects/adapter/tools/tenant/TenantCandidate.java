package org.notima.businessobjects.adapter.tools.tenant;

/**
 * The result of checking one tenant with an {@link AbstractTenantCandidateFinder}, ie
 * whether it needs a dunning run or has vendor invoices to pay.
 */
public interface TenantCandidate {

	void setAdapterName(String adapterName);

	void setTaxId(String taxId);

	void setName(String name);

	void setCountryCode(String countryCode);

	/** Marks the tenant as not checked, ie because the adapter failed. */
	void markError(String error);

}

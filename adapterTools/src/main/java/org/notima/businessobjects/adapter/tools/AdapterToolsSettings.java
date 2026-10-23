package org.notima.businessobjects.adapter.tools;

import java.util.Dictionary;

/**
 * Settings for the adapterTools bundle, read from the AdapterTools config PID
 * in $KARAF_HOME/etc/AdapterTools.cfg.
 */
public class AdapterToolsSettings {

	public static final String PID = "AdapterTools";

	private String defaultCountryCode = "SE";

	/**
	 * SystemName of the adapter whose TenantInformationFactory should be used,
	 * when more than one is registered. If null, the first one (by SystemName) is used.
	 */
	private String tenantInformationAdapter;

	/**
	 * Cached accounting models are used for this many hours before they're fetched again
	 * from the accounting system. 0 to always fetch.
	 */
	private int accountingCacheMaxAgeHours = 24;

	public void setFromDictionary(Dictionary<String, Object> properties) {
		String cc = (String) properties.get("defaultCountryCode");
		if (cc != null && !cc.trim().isEmpty()) {
			defaultCountryCode = cc.trim();
		}
		String tia = (String) properties.get("tenantInformationAdapter");
		if (tia != null && !tia.trim().isEmpty()) {
			tenantInformationAdapter = tia.trim();
		}
		Object maxAge = properties.get("accountingCacheMaxAgeHours");
		if (maxAge != null && !maxAge.toString().trim().isEmpty()) {
			try {
				accountingCacheMaxAgeHours = Integer.parseInt(maxAge.toString().trim());
			} catch (NumberFormatException e) {
				// Keep the default
			}
		}
	}

	public String getDefaultCountryCode() {
		return defaultCountryCode;
	}

	public void setDefaultCountryCode(String defaultCountryCode) {
		this.defaultCountryCode = defaultCountryCode;
	}

	public String getTenantInformationAdapter() {
		return tenantInformationAdapter;
	}

	public void setTenantInformationAdapter(String tenantInformationAdapter) {
		this.tenantInformationAdapter = tenantInformationAdapter;
	}

	public int getAccountingCacheMaxAgeHours() {
		return accountingCacheMaxAgeHours;
	}

	public void setAccountingCacheMaxAgeHours(int accountingCacheMaxAgeHours) {
		this.accountingCacheMaxAgeHours = accountingCacheMaxAgeHours;
	}

}

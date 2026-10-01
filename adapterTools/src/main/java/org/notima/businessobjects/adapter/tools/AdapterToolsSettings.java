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

	public void setFromDictionary(Dictionary<String, Object> properties) {
		String cc = (String) properties.get("defaultCountryCode");
		if (cc != null && !cc.trim().isEmpty()) {
			defaultCountryCode = cc.trim();
		}
		String tia = (String) properties.get("tenantInformationAdapter");
		if (tia != null && !tia.trim().isEmpty()) {
			tenantInformationAdapter = tia.trim();
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

}

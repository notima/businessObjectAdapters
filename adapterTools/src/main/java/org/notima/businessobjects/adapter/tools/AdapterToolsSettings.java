package org.notima.businessobjects.adapter.tools;

import java.util.Dictionary;

/**
 * Settings for the adapterTools bundle, read from the AdapterTools config PID
 * in $KARAF_HOME/etc/AdapterTools.cfg.
 */
public class AdapterToolsSettings {

	public static final String PID = "AdapterTools";

	private String defaultCountryCode = "SE";

	public void setFromDictionary(Dictionary<String, Object> properties) {
		String cc = (String) properties.get("defaultCountryCode");
		if (cc != null && !cc.trim().isEmpty()) {
			defaultCountryCode = cc.trim();
		}
	}

	public String getDefaultCountryCode() {
		return defaultCountryCode;
	}

	public void setDefaultCountryCode(String defaultCountryCode) {
		this.defaultCountryCode = defaultCountryCode;
	}

}

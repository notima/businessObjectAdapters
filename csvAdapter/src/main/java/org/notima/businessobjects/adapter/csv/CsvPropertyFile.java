package org.notima.businessobjects.adapter.csv;

import java.util.Dictionary;

public class CsvPropertyFile {

	private String	tenantFile;
	private String	customerFile;
	
	public void setFromDictionary(Dictionary<String, Object> properties) {
	    tenantFile = (String)properties.get("tenantFile");
		customerFile = (String)properties.get("customerFile");
	}
	
	public String getTenantFile() {
		return tenantFile;
	}
	public void setTenantFile(String tenantFile) {
		this.tenantFile = tenantFile;
	}
	public String getCustomerFile() {
		return customerFile;
	}
	public void setCustomerFile(String customerFile) {
		this.customerFile = customerFile;
	}
	
	
	
}

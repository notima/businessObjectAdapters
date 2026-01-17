package org.notima.businessobjects.adapter.json;

import java.util.Dictionary;

public class JsonPropertyFile {

	private String	tenantFile;
	private String	customerFile;
	private String	paymentBatchChannelFile;
	private String	tenantsDirectory;
	
	public void setFromDictionary(Dictionary<String, Object> properties) {
	    tenantFile = (String)properties.get("tenantFile");
		customerFile = (String)properties.get("customerFile");
		paymentBatchChannelFile = (String)properties.get("paymentBatchChannelFile");
		tenantsDirectory = (String)properties.get("tenantsDirectory");
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
	public String getPaymentBatchChannelFile() {
		return paymentBatchChannelFile;
	}
	public void setPaymentBatchChannelFile(String paymentBatchChannelFile) {
		this.paymentBatchChannelFile = paymentBatchChannelFile;
	}

	/**
	 * Tenants directory are the main directory of all tenants. Each tenant will have a subdirectory 
	 * in this directory.
	 * 
	 * @return
	 */
	public String getTenantsDirectory() {
		return tenantsDirectory;
	}

	public void setTenantsDirectory(String tenantsDirectory) {
		this.tenantsDirectory = tenantsDirectory;
	}
	
}

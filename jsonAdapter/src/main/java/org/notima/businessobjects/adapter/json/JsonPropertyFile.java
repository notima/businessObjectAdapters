package org.notima.businessobjects.adapter.json;

import java.util.Dictionary;

public class JsonPropertyFile {

	private String	tenantFile;
	private String	customerFile;
	private String	paymentBatchChannelFile;
	
	public void setFromDictionary(Dictionary<String, Object> properties) {
	    tenantFile = (String)properties.get("tenantFile");
		customerFile = (String)properties.get("customerFile");
		paymentBatchChannelFile = (String)properties.get("paymentBatchChannelFile");
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
	
	
}

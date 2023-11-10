package org.notima.businessobjects.adapter.infometric;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;

public class InfometricTenant {

	public static String			INFOMETRIC_PROPERTY_FILE = "infometric.properties";
	
	private TaxSubjectIdentifier 	taxIdentifier;
	
	private String orgNo;
	private String countryCode;
	private String name;
	private String	productKey;
	private String	invoiceLineText;
	private File   directoryFile;
	
	private String tenantDirectory;

	public InfometricTenant(String tenantDirectory) throws Exception {

		this.tenantDirectory = tenantDirectory;
		taxIdentifier = TaxSubjectIdentifier.getUndefinedIdentifier();
		checkDirectoryValid();
		checkForTaxIdentifierAndCurrency();
		
	}
	
	public InfometricTenant(BusinessPartner<?> bp) {
		orgNo = bp.getTaxId();
		countryCode = bp.getCountryCode();
		name = bp.getName();
	}
	
	public String getOrgNo() {
		return orgNo;
	}

	public void setOrgNo(String orgNo) {
		this.orgNo = orgNo;
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

	public String getTenantDirectory() {
		return tenantDirectory;
	}

	public void setTenantDirectory(String tenantDirectory) {
		this.tenantDirectory = tenantDirectory;
	}
	
	public TaxSubjectIdentifier getTaxIdentifier() {
		return taxIdentifier;
	}

	public void setTaxIdentifier(TaxSubjectIdentifier taxIdentifier) {
		this.taxIdentifier = taxIdentifier;
	}

	public String getProductKey() {
		return productKey;
	}

	public void setProductKey(String productKey) {
		this.productKey = productKey;
	}

	public String getInvoiceLineText() {
		return invoiceLineText;
	}

	public void setInvoiceLineText(String invoiceLineText) {
		this.invoiceLineText = invoiceLineText;
	}

	private void checkDirectoryValid() throws FileNotFoundException {
		File f = new File(tenantDirectory);
		if (!f.isDirectory()) {
			throw new FileNotFoundException(tenantDirectory);
		}
		directoryFile = f;
	}
	
	/**
	 * Checks if the directory is readable and retreives directory information.
	 */
	private void checkForTaxIdentifierAndCurrency() {

		File f = new File(tenantDirectory + File.separator + INFOMETRIC_PROPERTY_FILE);
		if (f.exists() && f.canRead()) {
			readInfometricPropertyFile(f);
		}
		
	}
	
	private void readInfometricPropertyFile(File f) {
		
		Properties props = new Properties();
		try {
			props.load(new FileReader(f));
			
			String taxId = props.getProperty("taxId");
			String countryCode = props.getProperty("countryCode");
			taxIdentifier = new TaxSubjectIdentifier(taxId, countryCode);
			productKey = props.getProperty("productKey");
			invoiceLineText = props.getProperty("invoiceLineText");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	
	
	
}

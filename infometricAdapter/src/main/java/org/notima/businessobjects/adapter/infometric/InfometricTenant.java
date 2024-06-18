package org.notima.businessobjects.adapter.infometric;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.util.json.JsonUtil;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class InfometricTenant {

	public static String			INFOMETRIC_PROPERTY_FILE = "infometric.properties";
	public static String			INFOMETRIC_JSON_FILE = "infometric.json";
	
	private TaxSubjectIdentifier 	taxIdentifier;
	
	private String orgNo;
	private String countryCode;
	private String name;
	private File   directoryFile;
	private File   propertyFile;
	private InfometricTenantSettings 	tenantSettings = new InfometricTenantSettings();
	
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
		taxIdentifier = TaxSubjectIdentifier.createBusinessTaxSubject(orgNo, countryCode, name);
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

	public void setTenantDirectory(String tenantDirectory) throws FileNotFoundException {
		this.tenantDirectory = tenantDirectory;
		checkDirectoryValid();
		checkForTaxIdentifierAndCurrency();
	}
	
	public TaxSubjectIdentifier getTaxIdentifier() {
		return taxIdentifier;
	}

	public void setTaxIdentifier(TaxSubjectIdentifier taxIdentifier) {
		this.taxIdentifier = taxIdentifier;
	}
	
	public String getDefaultProductKey() {
		return tenantSettings.getDefaultProductMapping().getDestinationProductId();
	}

	/**
	 * Default mapping of infometrics ELM article.
	 * 
	 * @param productKey
	 * @param description
	 */
	public void setDefaultMapping(String productKey, String description) {
		tenantSettings.addProductMapping(InfometricTenantSettings.DEFAULT_PRODUCT, productKey, description);
	}

	public String getDefaultInvoiceLineText() {
		return tenantSettings.getDefaultProductMapping().getDestinationName();
	}

	private void checkDirectoryValid() throws FileNotFoundException {
		File f = new File(tenantDirectory);
		if (!f.isDirectory()) {
			throw new FileNotFoundException(tenantDirectory);
		}
		directoryFile = f;
		propertyFile = new File(tenantDirectory + File.separator + INFOMETRIC_PROPERTY_FILE);
	}
	
	/**
	 * Checks if the directory is readable and retreives directory information.
	 */
	private void checkForTaxIdentifierAndCurrency() {

		
		if (propertyFile.exists() && propertyFile.canRead()) {
			readInfometricPropertyFile();
		}
		try {
			readInfometricTenantSettings();
		} catch (Exception ee) {
			
		}
		
	}
	
	public InfometricTenantSettings getTenantSettings() {
		return tenantSettings;
	}

	public void savePropertyFile() {
		saveInfometricPropertyFile();
		saveInfometricTenantSettings();
	}
	
	private void saveInfometricTenantSettings() {
		
		File f = new File(directoryFile.getAbsolutePath() + File.separator + INFOMETRIC_JSON_FILE);
		Gson gson = JsonUtil.buildGson();
		try {
			FileWriter fw = new FileWriter(f);
			gson.toJson(tenantSettings, fw);
			fw.flush();
			fw.close();
		} catch (JsonIOException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void readInfometricTenantSettings() {
		
		File f = new File(directoryFile.getAbsolutePath() + File.separator + INFOMETRIC_JSON_FILE);
		Gson gson = JsonUtil.buildGson();
		try {
			tenantSettings = gson.fromJson(new FileReader(f), InfometricTenantSettings.class);
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		} catch (JsonIOException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	private void readInfometricPropertyFile() {
		
		Properties props = new Properties();
		try {
			props.load(new FileReader(propertyFile));
			
			String taxId = props.getProperty("taxId");
			String countryCode = props.getProperty("countryCode");
			taxIdentifier = new TaxSubjectIdentifier(taxId, countryCode);
			readInfometricTenantSettings();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	// The saveInfometricPropertyFile method
    private void saveInfometricPropertyFile() {
        Properties props = new Properties();
        try {
            if (taxIdentifier != null) {
                props.setProperty("taxId", taxIdentifier.getTaxId());
                props.setProperty("countryCode", taxIdentifier.getCountryCode());
            }

            props.store(new FileWriter(propertyFile), "Infometric Properties");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	
}

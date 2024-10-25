package org.notima.businessobjects.adapter.json.impl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.util.json.JsonUtil;

import com.google.gson.Gson;

public class TenantList {
	
	@SuppressWarnings("rawtypes")
	private BusinessPartnerList tenants;

	private File	tenantFile;
	
	private static Gson gson = JsonUtil.buildGson();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static TenantList createFromFile(File file) throws IOException {
		TenantList tl = new TenantList();
		if (file.exists()) {
			FileReader fr = new FileReader(file);
			tl.tenants = gson.fromJson(fr, BusinessPartnerList.class);
			fr.close();
		} else {
			tl.tenants = new BusinessPartnerList();
		}
		if (tl.tenants.getBusinessPartner()==null) {
			tl.tenants.setBusinessPartner(new ArrayList<BusinessPartner>());
		}
		tl.tenantFile = file;
		return tl;
	}

	public void saveToFile() throws IOException {
		
		// Create parent directories if they don't exist
	    tenantFile.getParentFile().mkdirs();
	    
		FileWriter fw = new FileWriter(tenantFile);
		
		gson.toJson(tenants, fw);
		fw.close();
	}
	
	@SuppressWarnings("rawtypes")
	public BusinessPartnerList getTenants() {
		return tenants;
	}

	@SuppressWarnings("rawtypes")
	public void setTenants(BusinessPartnerList tenants) {
		this.tenants = tenants;
	}
	
	
	
}

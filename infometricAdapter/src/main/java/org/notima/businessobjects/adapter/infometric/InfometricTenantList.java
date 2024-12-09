package org.notima.businessobjects.adapter.infometric;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.util.json.JsonUtil;

import com.google.gson.Gson;

public class InfometricTenantList {

	private List<InfometricTenant> tenants;
	
	private File tenantFile;
	
	Type listType = (Type)new TypeToken<List<InfometricTenant>>() {}.getType();
	
	private static Gson gson = JsonUtil.buildGson();
	
	/**
	 * Creates this class from a gson file
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static InfometricTenantList createFromFile(File file) throws IOException {
		InfometricTenantList tenantList = new InfometricTenantList();
		tenantList.tenants = new ArrayList<InfometricTenant>();

		if (file.exists()) {
			FileReader fr = new FileReader(file);
			tenantList.tenants = gson.fromJson(fr, tenantList.listType);
			fr.close();
		} else {
			tenantList.tenants = new ArrayList<InfometricTenant>();
		}
		tenantList.tenantFile = file;
		
		return tenantList;
	}
	
	
	public void saveToFile() throws IOException {
		
		// Create parent directories if they don't exist
	    tenantFile.getParentFile().mkdirs();
	    
		FileWriter fw = new FileWriter(tenantFile);
		
		gson.toJson(tenants, fw);
		fw.close();
	}
	
	
	public void addTenant(InfometricTenant tenant) {
		tenants.add(tenant);
	}


	public List<InfometricTenant> getTenants() {
		return tenants;
	}
	
	public BusinessPartnerList<InfometricTenant> listTenants() {
		BusinessPartnerList<InfometricTenant> dlist = new BusinessPartnerList<InfometricTenant>();
		List<BusinessPartner<InfometricTenant>> list = new ArrayList<BusinessPartner<InfometricTenant>>();
		for (InfometricTenant it : tenants) {
			list.add(InfometricConverter.toBusinessPartner(it));
		}
		dlist.setBusinessPartner(list);
		return dlist;
	}


	public void setTenants(List<InfometricTenant> tenants) {
		this.tenants = tenants;
	}
	
	
	
	
}

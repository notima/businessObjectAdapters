package org.notima.businessobjects.adapter.json.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.util.json.JsonUtil;

import com.google.gson.Gson;

public class TenantList {
	
	private BusinessPartnerList tenants;

	private static Gson gson = JsonUtil.buildGson();
	
	public static TenantList createFromFile(File file) throws FileNotFoundException {
		FileReader fr = new FileReader(file);
		Gson lgson = gson.newBuilder().create();
		TenantList tl = new TenantList();
		tl.tenants = lgson.fromJson(fr, BusinessPartnerList.class);
		return tl;
	}
	
}

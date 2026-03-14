package org.notima.businessobjects.adapter.json.impl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.util.json.JsonUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class TenantInformationListImpl {

	private List<TenantInformation> entries;
	private File sourceFile;

	private static Gson gson = JsonUtil.buildGson();

	public static TenantInformationListImpl createFromFile(File file) throws IOException {
		TenantInformationListImpl impl = new TenantInformationListImpl();
		impl.sourceFile = file;
		Type listType = new TypeToken<List<TenantInformation>>() {}.getType();
		if (file.exists()) {
			try (FileReader fr = new FileReader(file)) {
				impl.entries = gson.fromJson(fr, listType);
			}
		}
		if (impl.entries == null) {
			impl.entries = new ArrayList<TenantInformation>();
		}
		return impl;
	}

	public void writeToFile() throws IOException {
		if (sourceFile.getParentFile() != null) {
			sourceFile.getParentFile().mkdirs();
		}
		try (FileWriter fw = new FileWriter(sourceFile)) {
			gson.toJson(entries, fw);
		}
	}

	public TenantInformation findByTenant(TaxSubjectIdentifier tenant) {
		if (tenant == null) return null;
		for (TenantInformation ti : entries) {
			if (ti.getTenant() != null && ti.getTenant().isEqual(tenant)) {
				return ti;
			}
		}
		return null;
	}

	public List<TenantInformation> getEntries() {
		return entries;
	}

}

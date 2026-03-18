package org.notima.businessobjects.adapter.json;

import java.io.File;
import java.io.IOException;

import org.notima.businessobjects.adapter.json.impl.TenantInformationListImpl;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;

public class JsonTenantInformationFactory implements TenantInformationFactory {

	public static final String DEFAULT_FILE = "jsonAdapter/tenantInformation.json";

	private TenantInformationListImpl tenantList;

	public JsonTenantInformationFactory(String pathToJsonFile) throws IOException {
		String path = (pathToJsonFile != null && !pathToJsonFile.trim().isEmpty())
				? pathToJsonFile.trim()
				: DEFAULT_FILE;
		tenantList = TenantInformationListImpl.createFromFile(new File(path));
	}

	@Override
	public TenantInformation getTenantInformation(TaxSubjectIdentifier tenant) {
		return tenantList.findByTenant(tenant);
	}

	@Override
	public TenantInformation persistTenantInformation(TenantInformation ti) throws IOException {
		TenantInformation existing = tenantList.findByTenant(ti.getTenant());
		if (existing != null) {
			tenantList.getEntries().remove(existing);
		}
		tenantList.getEntries().add(ti);
		tenantList.writeToFile();
		return ti;
	}

}

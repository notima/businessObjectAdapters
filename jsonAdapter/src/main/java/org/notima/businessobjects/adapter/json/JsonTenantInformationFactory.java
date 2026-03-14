package org.notima.businessobjects.adapter.json;

import java.io.File;
import java.io.IOException;

import org.notima.businessobjects.adapter.json.impl.TenantInformationListImpl;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;

public class JsonTenantInformationFactory implements TenantInformationFactory {

	private TenantInformationListImpl tenantList;

	public JsonTenantInformationFactory(String pathToJsonFile) throws IOException {
		tenantList = TenantInformationListImpl.createFromFile(new File(pathToJsonFile));
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

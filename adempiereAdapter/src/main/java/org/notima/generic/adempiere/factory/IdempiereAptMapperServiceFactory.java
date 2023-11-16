package org.notima.generic.adempiere.factory;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.ifacebusinessobjects.MappingService;
import org.notima.generic.ifacebusinessobjects.MappingServiceInstanceFactory;

public class IdempiereAptMapperServiceFactory implements MappingServiceInstanceFactory {
	
	private AdempiereJdbcFactory adapter;

	public IdempiereAptMapperServiceFactory(AdempiereJdbcFactory adapter) {
		this.adapter = adapter;
		
	}
	
	public int getAdClientId() {
		return adapter.getADClientID();
	}

	@Override
	public String getSourceSystemName() {
		return Activator.SYSTEM_NAME;
	}

	@Override
	public String getTargetSystemName() {
		return MappingServiceInstanceFactory.ANY_SOURCE_TARGET;
	}
	
	@Override
	public MappingService getMappingService(TaxSubjectIdentifier tenant) throws NoSuchTenantException {
		int adOrgId = getAdOrgIdFromTaxSubject(tenant);
		
		return new IDempiereAptMapper(adapter.getDataSource(), adOrgId);
	}

	private int getAdOrgIdFromTaxSubject(TaxSubjectIdentifier tenant) throws NoSuchTenantException {
		
		if (tenant==null) return 0;

		adapter.refreshTenantMap();
		adapter.setTenant(tenant.getTaxId(), tenant.getCountryCode());
		BusinessPartner<?> bp = adapter.getCurrentTenant();
		int adOrgNo = Integer.parseInt(bp.getIdentityNo());
		return adOrgNo;
		
	}
	
}

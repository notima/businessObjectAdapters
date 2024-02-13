package org.notima.businessobjects.adapter.json;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.BusinessPartnerManager;

public class JsonBusinessPartnerMgr<B> implements BusinessPartnerManager<B> {
	
	@Override
	public BusinessPartner<B> lookupBusinessPartner(TaxSubjectIdentifier tsi) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BusinessPartner<B> addBusinessPartner(TaxSubjectIdentifier tsi, BusinessPartner<B> bp) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeBusinessPartner(TaxSubjectIdentifier tsi) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getSystemName() {
		// TODO Auto-generated method stub
		return null;
	}

}

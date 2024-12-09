package org.notima.businessobjects.adapter.infometric;

import org.notima.generic.businessobjects.BusinessPartner;

public class InfometricConverter {

	public static BusinessPartner<InfometricTenant> toBusinessPartner(InfometricTenant src) {
		
		BusinessPartner<InfometricTenant> dst = new BusinessPartner<InfometricTenant>();
		
		dst.setTaxId(src.getOrgNo());
		dst.setCountryCode(src.getCountryCode());
		dst.setName(src.getName());
		dst.setNativeBusinessPartner(src);
		
		return dst;
		
	}
	
}

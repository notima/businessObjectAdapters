package org.notima.fortnox.command;

import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.businessobjects.adapter.tools.FactorySelector;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

public class FortnoxCommand {

	@Reference
	protected List<BusinessObjectFactory> bofs;
	
	protected BusinessObjectFactory bf;
	
	protected BusinessObjectFactory getBusinessObjectFactoryForOrgNo(String orgNo) throws Exception {
		
		FactorySelector selector = new FactorySelector(bofs);
		bf = selector.getFactoryWithTenant(FortnoxAdapter.SYSTEMNAME, orgNo, null);
		
		return bf;
	}
	
	/**
	 * Gets the fortnox client.
	 * 
	 * @param orgNo
	 * @return
	 * @throws Exception
	 */
	protected FortnoxClient3 getFortnoxClient(String orgNo) throws Exception {
		
		getBusinessObjectFactoryForOrgNo(orgNo);

		if (bf==null) {
			return null;
		}
		
		FortnoxAdapter fa = (FortnoxAdapter)bf;
		return fa.getClient();
		
	}
	
	
}

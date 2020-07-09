package org.notima.fortnox.command;

import java.util.List;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.businessobjects.adapter.tools.FactorySelector;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

public class FortnoxCommand {

	/**
	 * Gets the fortnox client.
	 * 
	 * @param orgNo
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	protected FortnoxClient3 getFortnoxClient(List<BusinessObjectFactory> bofs, String orgNo) throws Exception {
		
		FactorySelector selector = new FactorySelector(bofs);
		
		BusinessObjectFactory bf = selector.getFactoryWithTenant(FortnoxAdapter.SYSTEMNAME, orgNo, null);

		if (bf==null) {
			return null;
		}
		
		FortnoxAdapter fa = (FortnoxAdapter)bf;
		return fa.getClient();
		
	}
	
	
}

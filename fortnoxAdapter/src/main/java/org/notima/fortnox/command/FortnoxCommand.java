package org.notima.fortnox.command;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.businessobjects.adapter.fortnox.FortnoxAdapter;
import org.notima.businessobjects.adapter.tools.FactorySelector;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

public class FortnoxCommand {

	@SuppressWarnings("rawtypes")
	@Reference
	protected List<BusinessObjectFactory> bofs;
	
	protected FortnoxAdapter bf;
	
	protected Date fromDate = null, untilDate = null;
	
	protected FortnoxAdapter getBusinessObjectFactoryForOrgNo(String orgNo) throws Exception {
		
		FactorySelector selector = new FactorySelector(bofs);
		bf = (FortnoxAdapter)selector.getFactoryWithTenant(FortnoxAdapter.SYSTEMNAME, orgNo, null);
		
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
	
	protected void parseDates(String fromDateStr, String untilDateStr) throws ParseException {
		
		if (fromDateStr!=null) {
			fromDate = FortnoxClient3.s_dfmt.parse(fromDateStr);
		}
		if (untilDateStr!=null) {
			untilDate = FortnoxClient3.s_dfmt.parse(untilDateStr);
		}
		
	}
	
	protected boolean isInRange(Date compareDate) {
		
		if (compareDate==null) return true;
		
		if (fromDate==null && untilDate==null) return true;
		if (fromDate!=null && compareDate.before(fromDate)) return false;
		if (untilDate!=null && compareDate.after(untilDate)) return false;

		return true;
		
	}
	
	
}

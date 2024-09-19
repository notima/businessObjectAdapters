package org.notima.businessobjects.adapter.time4u;

import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.TimeRecordService;
import org.notima.generic.ifacebusinessobjects.TimeRecordServiceFactory;

public class Time4uAdapterFactory implements TimeRecordServiceFactory {

	@Override
	public TimeRecordService getTimeRecordServiceFor(String system, TaxSubjectIdentifier tenant) {

		
		return null;
	}

	@Override
	public String getSystemName() {
		return Activator.SYSTEMNAME;
	}

}

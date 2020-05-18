package com.svea.businessobjects;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

import com.svea.businessobjects.sveaadmin.SveaAdminBusinessObjectFactory;

@Services(
		provides = {
				@ProvideService(BusinessObjectFactory.class),
		}
)
public class Activator extends BaseActivator {

	@Override
	protected void doStart() throws Exception {
		
		Dictionary<String, String> props = new Hashtable<String,String>();
		props.put("SystemName", "WebpayAdminService");
		
		SveaAdminBusinessObjectFactory sabof = new SveaAdminBusinessObjectFactory();
		register(BusinessObjectFactory.class, sabof, props);
		
	}

	@Override
	protected void doStop() {
		super.doStop();
	}

	
}

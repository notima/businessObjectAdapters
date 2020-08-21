package org.notima.generic.ubl.factory;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.generic.ifacebusinessobjects.BusinessObjectConverter;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(BusinessObjectFactory.class),
				@ProvideService(BusinessObjectConverter.class),
		}
)
public class Activator extends BaseActivator {
	
	private Logger log = LoggerFactory.getLogger(Activator.class);	
	
	@Override
	protected void doStop() {
		super.doStop();
		
	}

	@Override
	public void doStart() {
	
		ConfigurationAdmin configurationAdmin = null;
		
		ServiceReference<ConfigurationAdmin> reference = bundleContext.getServiceReference(ConfigurationAdmin.class);
		if (reference != null) {
			
            // retrieving the ConfigurationAdmin service
            configurationAdmin = bundleContext.getService(reference);
            try {
                // retrieving the configuration using the PID
                Configuration configuration = configurationAdmin.getConfiguration("UBLProperties");
                if (configuration != null) {
                    Dictionary<String, Object> properties = configuration.getProperties();
                    if (properties!=null) {
//	                    clientId = (String)properties.get("clientId");
//	                    orgId = (String)properties.get("orgId");
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            bundleContext.ungetService(reference);
        }			
		
		Dictionary<String, String> props = new Hashtable<String,String>();
		
		// Lookup data source
		
		try {
			
			UBL21Factory adapter = new UBL21Factory();
			
			props.put("SystemName", adapter.getSystemName());
	
			log.info("Created UBL21Adapter");
			register(BusinessObjectFactory.class, adapter, props);
			
			UBL21Converter converter = new UBL21Converter();
			
			log.info("Created UBL21Converter");
			register(BusinessObjectConverter.class, converter, props);
			
				
		} catch (Exception ee) {
			log.error("Failed to create UBL21Adapter/converter", ee);
		}
		
	}

}

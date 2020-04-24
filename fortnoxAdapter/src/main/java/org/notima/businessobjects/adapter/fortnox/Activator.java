package org.notima.businessobjects.adapter.fortnox;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.api.fortnox.clients.FortnoxClientManager;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(BusinessObjectFactory.class),
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
	
		String fortnoxClientsFile = null;
		ConfigurationAdmin configurationAdmin = null;
		
		ServiceReference<ConfigurationAdmin> reference = bundleContext.getServiceReference(ConfigurationAdmin.class);
		if (reference != null) {
			
            // retrieving the ConfigurationAdmin service
            configurationAdmin = bundleContext.getService(reference);
            try {
                // retrieving the configuration using the PID
                Configuration configuration = configurationAdmin.getConfiguration("FortnoxProperties");
                if (configuration != null) {
                    Dictionary<String, Object> properties = configuration.getProperties();
                    if (properties!=null) {
	                    fortnoxClientsFile = (String)properties.get("fortnoxClientsFile");
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            bundleContext.ungetService(reference);
        }			
		
		Dictionary<String, String> props = new Hashtable<String,String>();
		props.put("SystemName", "Fortnox");
		
		// This is only necessary if the configuration admin for some reason wan't loaded (JUnitTest situation).
		if (fortnoxClientsFile==null) {
			URL url = ClassLoader.getSystemResource("fortnoxClients.xml");
			if (url!=null) {
				fortnoxClientsFile = url.getFile();
			} else {
				log.warn("No fortnoxClients.xml file found to initialize the FortnoxClientManager");
			}
		}
		
		if (fortnoxClientsFile!=null) {
			
			FortnoxClientManager mgr = new FortnoxClientManager(fortnoxClientsFile);
			
			FortnoxAdapter fapt = new FortnoxAdapter();
			fapt.setClientManager(mgr);
			
			log.info("Created FortnoxProvisioner from file " + mgr.getClientsFile());
			register(BusinessObjectFactory.class, fapt, props);

		}
		
	}


}

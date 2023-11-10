package org.notima.businessobjects.adapter.infometric;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(BusinessObjectFactory.class)
		}
)
public class Activator extends BaseActivator {
	
	private Logger log = LoggerFactory.getLogger(Activator.class);	
	
	private String infometricBaseDirectory = "infometric-files";
	
	@Override
	protected void doStop() {
		super.doStop();
		
	}

	@Override
	public void doStart() throws IOException {
	
		ConfigurationAdmin configurationAdmin = null;
		
		ServiceReference<ConfigurationAdmin> reference = bundleContext.getServiceReference(ConfigurationAdmin.class);
		if (reference != null) {
			
            // retrieving the ConfigurationAdmin service
            configurationAdmin = bundleContext.getService(reference);
            try {
                // retrieving the configuration using the PID
                Configuration configuration = configurationAdmin.getConfiguration("InfometricProperties");
                if (configuration != null) {
                    Dictionary<String, Object> properties = configuration.getProperties();
                    if (properties!=null) {
	                    infometricBaseDirectory = (String)properties.get("infometricBaseDirectory");
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            bundleContext.ungetService(reference);
        }			
		
		Dictionary<String, String> props = new Hashtable<String,String>();
		props.put("SystemName", InfometricAdapter.SYSTEM);

		InfometricAdapter fapt = new InfometricAdapter();
		fapt.setInfometricBaseDirectory(infometricBaseDirectory);
			
		log.info("Initialized Infometric Adapter using base directory: " + infometricBaseDirectory);
		register(BusinessObjectFactory.class, fapt, props);
		
	}


}

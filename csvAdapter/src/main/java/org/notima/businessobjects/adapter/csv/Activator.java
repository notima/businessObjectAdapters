package org.notima.businessobjects.adapter.csv;

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

	public static String CSV_PID="csvAdapterProperties";
	
	private Logger log = LoggerFactory.getLogger(Activator.class);	

	private ConfigurationAdmin configurationAdmin = null;	
	private ServiceReference<ConfigurationAdmin> reference;
	private Configuration configuration;
	private CsvPropertyFile csvProperties;
	
	@Override
	protected void doStop() {
		super.doStop();
	}

	@Override
	public void doStart() throws IOException {
		initConfiguration();
		createAndRegisterAdapter();
	}
	
	private void createAndRegisterAdapter() {
		
		@SuppressWarnings("rawtypes")
		CsvAdapter<?,?,?,?,?,?> adapter = new CsvAdapter(csvProperties);
		
		Dictionary<String, String> props = new Hashtable<String,String>();
		props.put("SystemName", CsvAdapter.SYSTEM_NAME);
		register(BusinessObjectFactory.class, adapter, props);
		log.info("Registered adapter for " + CsvAdapter.SYSTEM_NAME);
		
	}
	
	private void initConfiguration() throws IOException {
		csvProperties = new CsvPropertyFile();
		reference = bundleContext.getServiceReference(ConfigurationAdmin.class);
		if (reference!=null) {
            // retrieving the ConfigurationAdmin service
            configurationAdmin = bundleContext.getService(reference);
            configuration = configurationAdmin.getConfiguration(CSV_PID);
            
            if (configuration != null) {
                Dictionary<String, Object> properties = configuration.getProperties();
                if (properties!=null) {
                	csvProperties.setFromDictionary(properties);
                }
            }
		}
	}
	
}

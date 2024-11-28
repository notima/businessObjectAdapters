package org.notima.businessobjects.adapter.json;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(BusinessObjectFactory.class),
				@ProvideService(PaymentBatchChannelFactory.class)
		}
)
public class Activator extends BaseActivator {

	public static String CSV_PID="jsonAdapterProperties";
	
	private Logger log = LoggerFactory.getLogger(Activator.class);	

	private ConfigurationAdmin configurationAdmin = null;	
	private ServiceReference<ConfigurationAdmin> reference;
	private Configuration configuration;
	private JsonPropertyFile jsonProperties;

	private Dictionary<String, String> props = new Hashtable<String,String>();
	
	@Override
	protected void doStop() {
		super.doStop();
	}

	@Override
	public void doStart() throws IOException {
		initConfiguration();
		createAndRegisterAdapter();
		createAndRegisterPaymentBatchChannelFactory();
	}
	
	private void createAndRegisterAdapter() {
		
		@SuppressWarnings("rawtypes")
		JsonAdapter<?,?,?,?,?,?> adapter = new JsonAdapter(jsonProperties);
		
		register(BusinessObjectFactory.class, adapter, props);
		log.info("Registered adapter for " + JsonAdapter.SYSTEM_NAME);
		
	}
	
	private void createAndRegisterPaymentBatchChannelFactory() {

		try {
			PaymentBatchChannelFactory paymentBatchChannelFactory = new JsonPaymentBatchChannelFactory(jsonProperties.getPaymentBatchChannelFile());
			register(PaymentBatchChannelFactory.class, paymentBatchChannelFactory, props);
			log.info("Registered PaymentBatchChannelFactory adapter for " + JsonAdapter.SYSTEM_NAME);
		} catch (Exception ee) {
			log.error(ee.getMessage());
			ee.printStackTrace();
		}
		
		
	}
	
	private void initConfiguration() throws IOException {
		
		props.put("SystemName", JsonAdapter.SYSTEM_NAME);
		
		jsonProperties = new JsonPropertyFile();
		reference = bundleContext.getServiceReference(ConfigurationAdmin.class);
		if (reference!=null) {
            // retrieving the ConfigurationAdmin service
            configurationAdmin = bundleContext.getService(reference);
            configuration = configurationAdmin.getConfiguration(CSV_PID);
            
            if (configuration != null) {
                Dictionary<String, Object> properties = configuration.getProperties();
                if (properties!=null) {
                	jsonProperties.setFromDictionary(properties);
                }
            }
		}
	}
	
	
}

package org.notima.businessobjects.adapter.fortnox;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxUtil;
import org.notima.api.fortnox.clients.FortnoxClientList;
import org.notima.api.fortnox.clients.FortnoxClientManager;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchProcessor;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(BusinessObjectFactory.class),
				@ProvideService(PaymentBatchProcessor.class)
		}
)
public class Activator extends BaseActivator {
	
	private Logger log = LoggerFactory.getLogger(Activator.class);	
	
	@Override
	protected void doStop() {
		super.doStop();
		
	}

	@Override
	public void doStart() throws IOException {
	
		String fortnoxClientsFile = null;
		String fortnoxCredentialsFile = null;
		String defaultClientSecret = null;
		String defaultClientId = null;
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
						fortnoxCredentialsFile = (String)properties.get("fortnoxCredentialsFile");
	                    defaultClientSecret = (String)properties.get("defaultClientSecret");
	                    defaultClientId = (String)properties.get("defaultClientId");
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
			
			System.setProperty(FortnoxClient3.DFortnox4JFile, fortnoxClientsFile);
			System.setProperty(CredentialsFile.CREDENTIALS_FILE_PROPERTY, fortnoxCredentialsFile);
			FortnoxClientManager mgr = null;
			
			try {
				mgr = new FortnoxClientManager(fortnoxClientsFile);
				mgr.setDefaultClientSecret(defaultClientSecret);
				mgr.setDefaultClientId(defaultClientId);
			} catch (FileNotFoundException fne) {
				
				// Create the file
				try {
					FortnoxUtil.writeFortnoxClientListToFile(new FortnoxClientList(), fortnoxClientsFile);
					log.info("Creating new empty Fortnox Clients file: " + fortnoxClientsFile);
					mgr = new FortnoxClientManager(fortnoxClientsFile);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}

			register(FortnoxClientManager.class, mgr);
			
			FortnoxAdapter fapt = new FortnoxAdapter();
			fapt.setClientManager(mgr);
			
			log.info("Created FortnoxProvisioner from file " + mgr.getClientsFile());
			register(BusinessObjectFactory.class, fapt, props);
			
			FortnoxPaymentBatchProcessor fpbp = new FortnoxPaymentBatchProcessor();
			fpbp.setFortnoxAdapter(fapt);
			
			register(PaymentBatchProcessor.class, fpbp, props);
			log.info("Registered FortnoxPaymentBatchProcessor.");

		}
		
	}


}

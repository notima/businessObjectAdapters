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
import org.notima.api.fortnox.FortnoxConstants;
import org.notima.api.fortnox.FortnoxUtil;
import org.notima.api.fortnox.clients.FortnoxClientList;
import org.notima.api.fortnox.clients.FortnoxClientManager;
import org.notima.api.fortnox.clients.FortnoxPropertyFile;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchProcessor;
import org.notima.generic.ifacebusinessobjects.TaxRateProvider;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(BusinessObjectFactory.class),
				@ProvideService(PaymentBatchProcessor.class),
				@ProvideService(TaxRateProvider.class)
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
	
		FortnoxPropertyFile fortnoxProperties = new FortnoxPropertyFile();
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
                    	fortnoxProperties.setFromDictionary(properties);
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
		if (fortnoxProperties.getFortnoxClientsFile()==null) {
			URL url = ClassLoader.getSystemResource("fortnoxClients.xml");
			if (url!=null) {
				fortnoxProperties.setFortnoxClientsFile(url.getFile());
			} else {
				log.warn("No fortnoxClients.xml file found to initialize the FortnoxClientManager");
			}
		}
		
		if (fortnoxProperties.getFortnoxClientsFile()!=null) {
			
			System.setProperty(FortnoxConstants.DFortnox4JFile, fortnoxProperties.getFortnoxClientsFile());
			System.setProperty(CredentialsFile.CREDENTIALS_FILE_PROPERTY, fortnoxProperties.getFortnoxCredentialsFile());
			FortnoxClientManager mgr = null;
			
			try {
				mgr = new FortnoxClientManager(fortnoxProperties.getFortnoxClientsFile());
				mgr.setDefaultClientSecret(fortnoxProperties.getDefaultClientSecret());
				mgr.setDefaultClientId(fortnoxProperties.getDefaultClientId());
			} catch (FileNotFoundException fne) {
				
				// Create the file
				try {
					FortnoxUtil.writeFortnoxClientListToFile(new FortnoxClientList(), fortnoxProperties.getFortnoxClientsFile());
					log.info("Creating new empty Fortnox Clients file: " + fortnoxProperties.getFortnoxClientsFile());
					mgr = new FortnoxClientManager(fortnoxProperties.getFortnoxClientsFile());
					
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
			
			TaxRateProvider trp = new FortnoxTaxRateProvider();
			register(TaxRateProvider.class, trp, props);
			try {
				((FortnoxTaxRateProvider)trp).start(bundleContext);
				log.info("Registered Fortnox Tax Rate Provider");
			} catch (Exception e) {
				log.error("Not able to start Fortnox Tax Rate Provider");
			}

		}
		
	}


}

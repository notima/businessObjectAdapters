package org.notima.generic.pgp;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.businessobjects.adapter.tools.MessageSender;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(PGPEmailMessageSender.class)
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
        PGPEmailMessageSender emailSender = new PGPEmailMessageSender();
        ConfigurationAdmin configurationAdmin = null;
		ServiceReference<ConfigurationAdmin> reference = bundleContext.getServiceReference(ConfigurationAdmin.class);
        if (reference != null) {
			
            // retrieving the ConfigurationAdmin service
            configurationAdmin = bundleContext.getService(reference);
            // retrieving the configuration using the PID
            Configuration configuration = configurationAdmin.getConfiguration("EmailProperties");
            if (configuration != null) {
                Dictionary<String, Object> properties = configuration.getProperties();
                if (properties!=null) {
                    emailSender.setEmailHost((String)properties.get("emailHost"));
                    emailSender.setEmailUser((String)properties.get("emailUser"));
                    emailSender.setEmailPass((String)properties.get("emailPass"));
                    emailSender.setEmailPort((String)properties.get("emailPort"));
                    emailSender.setEmailName((String)properties.get("emailName"));
                    try{
                        emailSender.setSenderPublicKey(new File((String)properties.get("senderPublicKey")));
                        emailSender.setSenderPrivateKey(new File((String)properties.get("senderPrivateKey")));
                    } catch (NullPointerException npe){
                        log.error("Keypair not properly set up in EmailProperties.cfg", npe);
                    }
                    emailSender.setSenderPrivateKeyPassword((String)properties.get("senderPrivateKeyPassword"));
                }
            }
            bundleContext.ungetService(reference);
        }	
        
		register(MessageSender.class, emailSender);
        log.info("Email message sender registered");
    }
}

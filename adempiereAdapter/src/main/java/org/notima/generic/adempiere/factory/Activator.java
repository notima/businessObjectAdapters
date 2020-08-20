package org.notima.generic.adempiere.factory;

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.sql.DataSource;

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
	
		String clientId = null;
		String orgId = null;
		ConfigurationAdmin configurationAdmin = null;
		
		ServiceReference<ConfigurationAdmin> reference = bundleContext.getServiceReference(ConfigurationAdmin.class);
		if (reference != null) {
			
            // retrieving the ConfigurationAdmin service
            configurationAdmin = bundleContext.getService(reference);
            try {
                // retrieving the configuration using the PID
                Configuration configuration = configurationAdmin.getConfiguration("AdempiereProperties");
                if (configuration != null) {
                    Dictionary<String, Object> properties = configuration.getProperties();
                    if (properties!=null) {
	                    clientId = (String)properties.get("clientId");
	                    orgId = (String)properties.get("orgId");
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            bundleContext.ungetService(reference);
        }			

		if (clientId==null)
			clientId = "1000000";
		if (orgId == null)
			orgId = "1000000";
		
		Dictionary<String, String> props = new Hashtable<String,String>();
		props.put("SystemName", "Adempiere");
		
		// Lookup data source
		
		try {
			Collection<ServiceReference<DataSource>> dsRef = bundleContext.getServiceReferences(DataSource.class, 
					"(osgi.jndi.service.name=adempiere)");
			AdempiereJdbcFactory adapter = null;
			if (dsRef!=null && dsRef.size()>0) {
	
				DataSource ds = bundleContext.getService(dsRef.iterator().next());
				adapter = new AdempiereJdbcFactory(ds, Integer.parseInt(clientId), Integer.parseInt(orgId));
	
				log.info("Created AdempiereAdapter. ClientId {}, OrgId {}", clientId, orgId);
				register(BusinessObjectFactory.class, adapter, props);
				
			} else {
				
				log.warn("No datasource for adempiere found. No AdempiereAdapter created");
				
			}
		} catch (Exception ee) {
			log.error("Failed to create AdempiereAdapter", ee);
		}
		
	}


}

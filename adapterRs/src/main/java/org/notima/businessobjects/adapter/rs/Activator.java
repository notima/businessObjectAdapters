package org.notima.businessobjects.adapter.rs;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.ws.rs.core.Application;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    private ServiceRegistration<Application> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("osgi.jaxrs.application.base", "/bso");
        registration = context.registerService(Application.class, new AdapterApplication(), properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (registration != null) {
            registration.unregister();
        }
    }	
	
}

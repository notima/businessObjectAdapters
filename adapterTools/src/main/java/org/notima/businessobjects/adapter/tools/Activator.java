package org.notima.businessobjects.adapter.tools;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(FormatterFactory.class),
				@ProvideService(CanonicalObjectFactory.class),
				@ProvideService(MessageSenderFactory.class),
		}
)
public class Activator extends BaseActivator {


	private Logger log = LoggerFactory.getLogger(Activator.class);	
	
	@Override
	public void doStart() {

		FormatterFactory formatterFactory = new FormatterFactoryImpl();
		log.info("Created FormatterFactory");
		((FormatterFactoryImpl)formatterFactory).setBundleContext(bundleContext);
		register(FormatterFactory.class, formatterFactory);
		
		CanonicalObjectFactory cof = new CanonicalObjectFactoryImpl();
		((CanonicalObjectFactoryImpl)cof).setBundleContext(bundleContext);
		log.info("Created Canonical Object Factory");
		register(CanonicalObjectFactory.class, cof);

		MessageSenderFactory messageSenderFactory = new MessageSenderFactoryImpl();
		log.info("Created MessageSenderFactory");
		((MessageSenderFactoryImpl)messageSenderFactory).setBundleContext(bundleContext);
		register(MessageSenderFactory.class, messageSenderFactory);
		
	}
	
	
}
	

package org.notima.businessobjects.adapter.tools;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(FormatterFactory.class),
		}
)
public class Activator extends BaseActivator {


	private Logger log = LoggerFactory.getLogger(Activator.class);	
	
	@Override
	public void doStart() {

		FormatterFactory formatterFactory = new FormatterFactoryImpl();
		log.info("Created FormatterFactory");
		register(FormatterFactory.class, formatterFactory);
		
	}
	
	
}
	

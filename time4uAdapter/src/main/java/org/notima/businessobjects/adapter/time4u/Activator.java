package org.notima.businessobjects.adapter.time4u;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.generic.ifacebusinessobjects.TimeRecordServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Services(
		provides = {
				@ProvideService(TimeRecordServiceFactory.class)
		}
		)
public class Activator extends BaseActivator {

	public static String SYSTEMNAME = "Time4u";
	
	private Logger log = LoggerFactory.getLogger(Activator.class);	
	
	@Override
	public void doStart() throws Exception {
		super.doStart();
		
		Dictionary<String, String> props = new Hashtable<String,String>();
		props.put("SystemName", "Time4u");
		
		Time4uAdapterFactory adapter = new Time4uAdapterFactory();
		log.info("Created Time4U TimeRecord Service Factory Adapter");
		register(TimeRecordServiceFactory.class, adapter, props);
		
	}
	
	
}

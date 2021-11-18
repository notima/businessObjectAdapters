package org.notima.businessobjects.adapter.ratepay;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(PaymentFactory.class),
				@ProvideService(PaymentBatchFactory.class)
		}
)
public class Activator extends BaseActivator {

	private Logger log = LoggerFactory.getLogger(Activator.class);	
	
	@Override
	public void doStart() throws Exception {
		super.doStart();
		
		Dictionary<String, String> props = new Hashtable<String,String>();
		props.put("SystemName", "Ratepay");
		
		RatepayAdapter ratepayAdapter = new RatepayAdapter();
		log.info("Created Ratepay Adapter");
		register(PaymentFactory.class, ratepayAdapter, props);
		
		RatepayDirectoryToPaymentBatch paymentBatchFactory = new RatepayDirectoryToPaymentBatch();
		register(PaymentBatchFactory.class, paymentBatchFactory, props);
		log.info("Created Ratepay Batch Adapter");
		log.info("Context: " + this.bundleContext.toString());		
		
	}
	
	
}

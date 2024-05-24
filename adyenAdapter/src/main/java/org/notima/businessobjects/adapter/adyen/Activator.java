package org.notima.businessobjects.adapter.adyen;

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
		props.put("SystemName", "Adyen");
		
		AdyenAdapter adyenAdapter = new AdyenAdapter();
		log.info("Created Adyen Adapter");
		register(PaymentFactory.class, adyenAdapter, props);
		
		AdyenDirectoryToPaymentBatch paymentBatchFactory = new AdyenDirectoryToPaymentBatch();
		register(PaymentBatchFactory.class, paymentBatchFactory, props);
		log.info("Created Adyen Batch Adapter");
		log.info("Context: " + this.bundleContext.toString());		
		
	}
	
	
}

package org.notima.businessobjects.adapter.tools;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.generic.businessobjects.tax.BasicTaxRateProviderFI;
import org.notima.generic.businessobjects.tax.BasicTaxRateProviderSE;
import org.notima.generic.ifacebusinessobjects.PaymentBatchProcessor;
import org.notima.generic.ifacebusinessobjects.TaxRateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
		provides = {
				@ProvideService(FormatterFactory.class),
				@ProvideService(CanonicalObjectFactory.class),
				@ProvideService(MessageSenderFactory.class),
				@ProvideService(PaymentBatchProcessor.class),
				@ProvideService(MappingServiceFactory.class),
				@ProvideService(TaxRateProvider.class)
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
		
		Dictionary<String, String> props = new Hashtable<String,String>();
		props.put("SystemName", FilePaymentBatchProcessor.SystemName);
		
		FilePaymentBatchProcessor paymentBatchProcessor = new FilePaymentBatchProcessor();
		register(PaymentBatchProcessor.class, paymentBatchProcessor, props);
		log.info("Created File Payment Batch Processor");
		log.info("Context: " + this.bundleContext.toString());		
		
		MappingServiceFactory mappingFactory = new MappingServiceFactoryImpl();
		((MappingServiceFactoryImpl)mappingFactory).setBundleContext(bundleContext);
		log.info("Created Mapping Factory");
		register(MappingServiceFactory.class, mappingFactory);
		
		TaxRateProvider trp = new BasicTaxRateProviderSE();
		props.put("SystemName", trp.getSystemName());
		register(TaxRateProvider.class, trp, props);
		try {
			log.info("Registered Basic SE Tax Rate Provider");
		} catch (Exception e) {
			log.error("Not able to start Basic SE Tax Rate Provider");
		}
		
		trp = new BasicTaxRateProviderFI();
		props.put("SystemName", trp.getSystemName());
		register(TaxRateProvider.class, trp, props);
		try {
			log.info("Registered Basic FI Tax Rate Provider");
		} catch (Exception e) {
			log.error("Not able to start Basic FI Tax Rate Provider");
		}
		
	}
	
	
}
	

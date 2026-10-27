package org.notima.businessobjects.adapter.p27;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;

@Services(
		provides = {
				@ProvideService(PaymentFactory.class),
				@ProvideService(PaymentBatchFactory.class)
		}
)
public class Activator extends BaseActivator {

	@Override
	public void doStart() throws Exception {
		super.doStart();

		Dictionary<String, String> props = new Hashtable<String,String>();
		props.put("SystemName", P27Adapter.SYSTEMNAME);

		register(PaymentFactory.class, new P27Adapter(), props);
		register(PaymentBatchFactory.class, new P27PaymentBatchFactory(), props);
		P27Adapter.log.info("Created P27 adapter");
	}

}

package org.notima.businessobjects.adapter.p27;

import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;

@Services(
		provides = {
				@ProvideService(PaymentBatchFactory.class)
		}
)
public class Activator extends BaseActivator {
	
}

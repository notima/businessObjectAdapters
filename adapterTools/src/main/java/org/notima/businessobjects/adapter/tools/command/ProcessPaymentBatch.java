package org.notima.businessobjects.adapter.tools.command;

import java.io.File;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.ifacebusinessobjects.PaymentBatchProcessor;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;

@Command(scope = "notima", name = "process-payment-batch", description = "Processes a payment batch")
@Service
public class ProcessPaymentBatch implements Action {
	
	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference 
	Session sess;

    @Option(name = "--match-only", description = "Run only a match session.", required = false, multiValued = false)
    private boolean matchOnly;
	
	@Argument(index = 0, name = "paymentBatchProcessor", description ="The payment processor to send to", required = true, multiValued = false)
	private String paymentBatchProcessorStr = "";

	@Argument(index = 1, name = "paymentSource", description ="The payment source (normally a file)", required = true, multiValued = false)
	private String paymentSource = "";

	@Argument(index = 2, name = "paymentFactory", description ="The payment destination adapter name", required = true, multiValued = false)
	private String paymentFactoryStr = "";

	@Override
	public Object execute() throws Exception {
		
		PaymentFactory paymentFactory = cof.lookupPaymentFactory(paymentFactoryStr);
		PaymentBatchProcessor paymentProcessor = cof.lookupPaymentBatchProcessor(paymentBatchProcessorStr);
		
		PaymentBatch pb = paymentFactory.readPaymentBatchFromSource(paymentSource);
		if (matchOnly) {
			paymentProcessor.lookupInvoiceReferences(pb);
		} else {
			paymentProcessor.processPaymentBatch(pb, null);
		}
		
		return null;
	}
	

}

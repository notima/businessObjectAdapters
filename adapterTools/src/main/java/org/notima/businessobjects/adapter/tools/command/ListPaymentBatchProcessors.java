package org.notima.businessobjects.adapter.tools.command;

import java.util.Collection;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchProcessor;

@Command(scope = "notima", name = "list-payment-batch-processors", description = "Lists avaliable payment batch processors")
@Service
public class ListPaymentBatchProcessors implements Action {

	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference 
	Session sess;
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {

		Collection<PaymentBatchProcessor> paymentBatchProcessors = cof.listPaymentBatchProcessors();
		
		for (PaymentBatchProcessor pbp : paymentBatchProcessors) {
			sess.getConsole().println(pbp.getSystemName());
		}
		
		return null;
	}

}

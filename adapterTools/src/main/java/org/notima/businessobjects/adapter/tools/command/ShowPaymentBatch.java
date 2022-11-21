package org.notima.businessobjects.adapter.tools.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.table.PaymentBatchTable;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;

@Command(scope = "notima", name = "show-payment-batch", description = "Shows a payment batch")
@Service
public class ShowPaymentBatch implements Action {
	
	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference 
	Session sess;

	@Argument(index = 0, name = "paymentFactory", description ="The payment destination adapter name", required = true, multiValued = false)
	private String paymentFactoryStr = "";
	
	@Argument(index = 1, name = "paymentSource", description ="The payment source (normally a file)", required = true, multiValued = false)
	@Completion(FileCompleter.class)
	private String paymentSource = "";
	
    @Option(name = "-d", aliases = { "--detailed" }, description = "Show a more detailed view", required = false, multiValued = false)
    private boolean detailed;

	@Override
	public Object execute() throws Exception {
		
		PaymentFactory paymentFactory = cof.lookupPaymentFactory(paymentFactoryStr);
		PaymentBatch pb = paymentFactory.readPaymentBatchFromSource(paymentSource);
		
		PaymentBatchTable paymentBatchTable = new PaymentBatchTable(pb, detailed);
		paymentBatchTable.getShellTable().print(sess.getConsole());
		
		return null;
	}
	

}

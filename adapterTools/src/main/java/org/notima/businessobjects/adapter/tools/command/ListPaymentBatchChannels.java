package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.table.PaymentBatchChannelTable;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;

@Command(scope = "notima", name = "list-payment-batch-channels", description = "Lists payment batch channels the first available provider")
@Service
public class ListPaymentBatchChannels implements Action {

	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference 
	Session sess;
	
	private PaymentBatchChannelFactory factory;
	
	private TaxSubjectIdentifier		tenant;
	
    @Argument(index = 0, name = "orgNo", description = "The org number to show details for", required = true, multiValued = false)
    private String orgNo;
	
	@Override
	public Object execute() throws Exception {

		initTenant();
		doTheStuff();
		
		return null;
	}

	private void initTenant() {
		tenant = new TaxSubjectIdentifier(orgNo);
	}
	
	private void doTheStuff() throws Exception {

		factory = cof.lookupFirstPaymentBatchChannelFactory();

		List<PaymentBatchChannel> result = factory.listChannelsForTenant(tenant);
		
		PaymentBatchChannelTable table = new PaymentBatchChannelTable(result);
		table.getShellTable().print(sess.getConsole());
		
	}
	
}

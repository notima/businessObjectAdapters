package org.notima.businessobjects.adapter.tools.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.businessobjects.BasicPaymentBatchChannel;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;

@Command(scope = "notima", name = "add-payment-batch-channel", description = "Adds / modifies payment batch channels")
@Service
public class AddPaymentBatchChannel implements Action {

	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference 
	Session sess;
	
	private PaymentBatchChannelFactory factory;
	
	private TaxSubjectIdentifier		tenant;
	
    @Argument(index = 0, name = "orgNo", description = "The org number to show details for", required = true, multiValued = false)
    private String orgNo;
    
    @Argument(index = 1, name = "destinationSystem", description = "The destination system of the payments", required = true, multiValued = false)
    private String destinationSystem;
    
    @Argument(index = 2, name = "sourceSystem", description = "The adapter where the payments are read from", required = true, multiValued = false)
    private String sourceSystem;
    
    @Argument(index = 3, name = "sourceConfiguration", description = "Source configuration", required = true, multiValued = false)
    private String sourceConfiguration;
    
    @Option(name = "-id", aliases = { "--channelId" }, description = "Use this channelID (or overwrite existing with ID)", required = false, multiValued = false)
    private String channelId;
    
    private PaymentBatchChannel channel;
    
	@Override
	public Object execute() throws Exception {

		initTenant();
		initPaymentBatchChannelFromParams();
		doTheStuff();
		
		return null;
	}

	private void initTenant() {
		tenant = new TaxSubjectIdentifier(orgNo);
	}
	
	private void doTheStuff() throws Exception {

		factory = cof.lookupFirstPaymentBatchChannelFactory();
		factory.persistChannel(channel);
		
	}
	
	private void initPaymentBatchChannelFromParams() {
		
		channel = new BasicPaymentBatchChannel();
		channel.setTenant(tenant);
		channel.setDestinationSystem(destinationSystem);
		channel.setSourceSystem(sourceSystem);
		channel.parseSourceOptions(sourceConfiguration);
		if (channelId!=null) {
			channel.setChannelId(channelId);
		}
		
	}
	
}

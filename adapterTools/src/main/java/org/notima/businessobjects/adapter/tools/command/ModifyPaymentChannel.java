package org.notima.businessobjects.adapter.tools.command;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.businessobjects.PaymentBatchChannelOptions;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;

@Command(scope = "notima", name = "modify-payment-channel", description = "Modifies a payment channel")
@Service
public class ModifyPaymentChannel implements Action {
	
	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference 
	Session sess;

    @Option(name = "--set-reconciled-until", description = "Sets reconciled until", required = false, multiValued = false)
    private String reconciledUntil;
    
    @Option(name = "--source-file-filter", description = "Sets source file filter", required = false, multiValued = false)
    private String sourceFileFilter;
    
    @Option(name = "--set-description", description = "Sets description", required = false, multiValued = false)
    private String description;

	@Argument(index = 0, name = "channelId", description ="The payment channel to run", required = true, multiValued = false)
	private String channelId = "";
	
	private PaymentBatchChannelFactory channelFactory;
	private PaymentBatchChannel channel;
	private PaymentBatchChannelOptions options;
	
	private boolean updated = false;

	private void initParameters() throws Exception {

		channelFactory = cof.lookupFirstPaymentBatchChannelFactory();
		if (channelFactory==null) throw new Exception("No channel factories defined.");
		
		channel = channelFactory.findChannelWithId(channelId);
		if (channel==null) throw new Exception("No channel with ID [" + channelId + "] found.");
		
		options = channel.getOptions();
		
	}
	
	@Override
	public Object execute() throws Exception {
		
		initParameters();
		
		modify();
		
		return null;
		
	}
	
	private void modify() throws Exception {

		if (reconciledUntil!=null) {
			channel.setReconciledUntil(LocalDate.parse(reconciledUntil, DateTimeFormatter.ISO_LOCAL_DATE));
			updated = true;
		}
		
		if (description!=null) {
			channel.setChannelDescription(description);
			updated = true;
		}

		if (sourceFileFilter!=null) {
			if (options==null) {
				options = new PaymentBatchChannelOptions();
				channel.setPaymentBatchChannelOptions(options);
			}
			options.setSourceFileFilter(sourceFileFilter);
			updated = true;
		}

		if (updated) {
			channelFactory.persistChannel(channel);
		}
		
	}
	
	

}

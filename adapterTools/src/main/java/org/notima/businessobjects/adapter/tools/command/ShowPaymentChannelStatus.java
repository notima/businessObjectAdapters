package org.notima.businessobjects.adapter.tools.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.table.PaymentChannelStatusTable;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;

@Command(scope = "notima", name = "show-payment-channel-status", description = "Shows channel status")
@Service
public class ShowPaymentChannelStatus implements Action {

	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference
	private Session sess;
	
	@Argument(index = 0, name = "channelId", description ="The payment channel", required = true, multiValued = false)
	private String channelId = "";
	
	private PaymentBatchChannelFactory channelFactory;
	private PaymentBatchChannel channel;
	
	
	@Override
	public Object execute() throws Exception {

		initParameters();
		
		showChannelStatus();
		
		return null;
	}
	
	private void showChannelStatus() {
		
		PaymentChannelStatusTable pcst = new PaymentChannelStatusTable(channel);
		pcst.getShellTable().print(sess.getConsole());
		
	}
	
	
	private void initParameters() throws Exception {

		channelFactory = cof.lookupFirstPaymentBatchChannelFactory();
		if (channelFactory==null) throw new Exception("No channel factories defined.");
		
		channel = channelFactory.findChannelWithId(channelId);
		if (channel==null) throw new Exception("No channel with ID [" + channelId + "] found.");
		
	}
	

}

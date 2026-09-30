package org.notima.businessobjects.adapter.tools.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.paymentbatch.BasicPaymentBatchChannelFactory;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.table.PaymentChannelStatusTable;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchFactory;

@Command(scope = "notima", name = "show-payment-channel-status", description = "Shows channel status")
@Service
public class ShowPaymentChannelStatus implements Action {

	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference
	private Session sess;
	
	@Argument(index = 0, name = "channelId", description ="The payment channel. Could also be description (if unique)", required = true, multiValued = false)
	private String channelId = "";
	
	private PaymentBatchChannelFactory channelFactory;
	private PaymentBatchFactory	paymentBatchFactory;
	private PaymentBatchChannel channel;
	
	
	@Override
	public Object execute() throws Exception {

		initParameters();
		
		showChannelStatus();
		
		return null;
	}
	
	private void showChannelStatus() {
		
		PaymentChannelStatusTable pcst = new PaymentChannelStatusTable(paymentBatchFactory, channel);
		pcst.getShellTable().print(sess.getConsole());
		
	}
	
	
	private void initParameters() throws Exception {

		channelFactory = cof.lookupFirstPaymentBatchChannelFactory();
		if (channelFactory==null) throw new Exception("No channel factories defined.");
		
		channel = channelFactory.findChannelWithIdOrDescription(channelId);
		if (channel==null) throw new Exception("No channel with ID or description [" + channelId + "] found.");
		
		// Read the source directory now so the files and dates are current
		if (channelFactory instanceof BasicPaymentBatchChannelFactory) {
			BasicPaymentBatchChannelFactory basicFactory = (BasicPaymentBatchChannelFactory)channelFactory;
			basicFactory.setCanonicalObjectFactory(cof);
			basicFactory.refreshUnprocessedEntries(channel);
		}
		
		paymentBatchFactory = cof.lookupPaymentBatchFactory(channel.getSourceSystem());	
		paymentBatchFactory.setSource(channel.getOptions().getSourceDirectory());
		
	}
	

}

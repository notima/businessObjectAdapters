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
import org.notima.generic.businessobjects.PaymentBatchChannelStatus;
import org.notima.generic.businessobjects.PaymentBatchChannelThresholds;
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

    @Option(name = "--max-unmatched", description = "Max number of unmatched payments in a report file. \"none\" removes the limit.", required = false, multiValued = false)
    private String maxUnmatched;

    @Option(name = "--max-unmatched-percent", description = "Max share of unmatched payments in a report file, in percent. \"none\" removes the limit.", required = false, multiValued = false)
    private String maxUnmatchedPercent;

    @Option(name = "--max-unmatched-amount", description = "Max unmatched amount per currency, ie 5000 or \"5000,{500:EUR}\". \"none\" removes the limit.", required = false, multiValued = false)
    private String maxUnmatchedAmount;

    @Option(name = "--activate", description = "Sets the channel as active", required = false, multiValued = false)
    private boolean activate;

    @Option(name = "--deactivate", description = "Sets the channel as inactive", required = false, multiValued = false)
    private boolean deactivate;

	@Argument(index = 0, name = "channelId", description ="The payment channel to modify. Could also be description (if unique)", required = true, multiValued = false)
	private String channelId = "";
	
	private PaymentBatchChannelFactory channelFactory;
	private PaymentBatchChannel channel;
	private PaymentBatchChannelOptions options;
	
	private boolean updated = false;

	private void initParameters() throws Exception {

		if (activate && deactivate) throw new Exception("--activate and --deactivate can't be used together.");

		channelFactory = cof.lookupFirstPaymentBatchChannelFactory();
		if (channelFactory==null) throw new Exception("No channel factories defined.");
		
		channel = channelFactory.findChannelWithIdOrDescription(channelId);
		if (channel==null) throw new Exception("No channel with ID or description [" + channelId + "] found.");
		
		options = channel.getOptions();
		
	}
	
	@Override
	public Object execute() throws Exception {
		
		initParameters();
		
		modify();
		
		return null;
		
	}
	
	private void modify() throws Exception {

		// First, since invalid values should stop the command before anything is changed.
		if (maxUnmatched!=null || maxUnmatchedPercent!=null || maxUnmatchedAmount!=null) {
			modifyThresholds();
			updated = true;
		}

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

		if (activate || deactivate) {
			PaymentBatchChannelStatus status = channel.getStatus();
			if (status==null) {
				status = new PaymentBatchChannelStatus();
				channel.setStatus(status);
			}
			status.setActive(activate);
			updated = true;
		}

		if (updated) {
			channelFactory.persistChannel(channel);
		}
		
	}

	private void modifyThresholds() throws Exception {
		
		// Validate all values before changing anything
		Integer count = null;
		Double percent = null;
		try {
			if (maxUnmatched!=null && !isNone(maxUnmatched)) {
				count = Integer.valueOf(maxUnmatched.trim());
			}
			if (maxUnmatchedPercent!=null && !isNone(maxUnmatchedPercent)) {
				percent = Double.valueOf(maxUnmatchedPercent.trim());
			}
		} catch (NumberFormatException e) {
			throw new Exception("Not a number: " + e.getMessage());
		}
		String amount = maxUnmatchedAmount!=null && !isNone(maxUnmatchedAmount) ? maxUnmatchedAmount : null;
		if (amount!=null) {
			new PaymentBatchChannelThresholds().setMaxUnmatchedAmount(amount);
		}
		
		if (options==null) {
			options = new PaymentBatchChannelOptions();
			channel.setPaymentBatchChannelOptions(options);
		}
		PaymentBatchChannelThresholds thresholds = options.getThresholds();
		if (thresholds==null) {
			thresholds = new PaymentBatchChannelThresholds();
		}
		if (maxUnmatched!=null) {
			thresholds.setMaxUnmatchedCount(count);
		}
		if (maxUnmatchedPercent!=null) {
			thresholds.setMaxUnmatchedPercent(percent);
		}
		if (maxUnmatchedAmount!=null) {
			thresholds.setMaxUnmatchedAmount(amount);
		}
		options.setThresholds(thresholds.hasLimits() ? thresholds : null);
		
	}
	
	private static boolean isNone(String value) {
		return "none".equalsIgnoreCase(value.trim());
	}

}

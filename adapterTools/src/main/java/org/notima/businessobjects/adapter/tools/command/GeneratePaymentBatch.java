package org.notima.businessobjects.adapter.tools.command;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchGenerator;

/**
 * Generates/fetches payment batch source files for a payment channel over a date range, by
 * referencing only the channel (no separate adapter-specific config to pass on the command line).
 */
@Command(scope = "notima", name = "generate-payment-batch", description = "Generates payment batch source files for a channel over a date range")
@Service
public class GeneratePaymentBatch implements Action {

	@Reference
	private CanonicalObjectFactory cof;

	@Reference
	Session sess;

	@Argument(index = 0, name = "channelId", description = "The payment channel to generate for. Could also be description (if unique)", required = true, multiValued = false)
	private String channelId = "";

	@Option(name = _NotimaCmdOptions.FROM_DATE, description = "Generate from this date yyyy-MM-dd. Defaults to the day after the channel's last generated date.", required = false)
	private String fromDateStr;

	@Option(name = _NotimaCmdOptions.UNTIL_DATE, description = "Generate until this date yyyy-MM-dd. Defaults to yesterday.", required = false)
	private String untilDateStr;

	@Option(name = "-od", aliases = { "--outputdir" }, description = "Write files to this directory instead of the channel's own directory", required = false)
	private String outputDirStr;

	private PaymentBatchChannelFactory channelFactory;
	private PaymentBatchChannel channel;

	@Override
	public Object execute() throws Exception {

		initChannelFactory();
		findChannel();

		File configDir = resolveConfigDirectory();
		File outputDir = (outputDirStr != null) ? new File(outputDirStr) : configDir;

		PaymentBatchGenerator generator = cof.lookupPaymentBatchGenerator(channel.getSourceSystem());
		if (generator == null) {
			throw new Exception("Source system [" + channel.getSourceSystem() + "] does not support generating payment batches");
		}

		LocalDate toDate = (untilDateStr != null)
				? LocalDate.parse(untilDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
				: LocalDate.now().minusDays(1);

		LocalDate fromDate;
		if (fromDateStr != null) {
			fromDate = LocalDate.parse(fromDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
		} else {
			fromDate = generator.getLastGeneratedDate(configDir)
					.map(d -> d.plusDays(1))
					.orElseThrow(() -> new Exception(
							"No checkpoint found for channel [" + channelId + "]; specify --fromdate explicitly for the first run."));
		}

		if (fromDateStr == null && untilDateStr == null) {
			sess.getConsole().println("No date range given - generating from " + fromDate + " to " + toDate + " (channel ["
					+ channelId + "])");
			String reply = sess.readLine("Continue? (y/n) ", null);
			if (!"y".equalsIgnoreCase(reply)) {
				sess.getConsole().println("Command execution cancelled");
				return null;
			}
		}

		if (fromDate.isAfter(toDate)) {
			sess.getConsole().println("Already up to date, nothing to generate");
			return null;
		}

		List<File> files = generator.generateBatchFiles(fromDate, toDate, configDir, outputDir);
		for (File f : files) {
			sess.getConsole().println("Wrote: " + f.getPath());
		}

		return null;
	}

	private void initChannelFactory() throws Exception {
		channelFactory = cof.lookupFirstPaymentBatchChannelFactory();
		if (channelFactory == null) throw new Exception("No channel factories defined.");
	}

	/**
	 * Finds channel by ID first and then description
	 */
	private void findChannel() throws Exception {
		channel = channelFactory.findChannelWithId(channelId);
		if (channel == null) {
			channel = channelFactory.findChannelByDescription(channelId);
		}
		if (channel == null)
			throw new Exception("No channel with ID [" + channelId + "] found.");
	}

	private File resolveConfigDirectory() throws Exception {
		if (channel.getOptions() == null || channel.getOptions().getSourceDirectory() == null) {
			throw new Exception("Source directory not defined");
		}
		return new File(channel.getOptions().getSourceDirectory());
	}

}

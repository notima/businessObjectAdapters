package org.notima.businessobjects.adapter.tools.command;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.notima.businessobjects.adapter.tools.AdapterToolsSettings;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.FormatterFactory;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.businessobjects.adapter.tools.task.PaymentChannelMatchReportTask;

/**
 * Runs the payment channel match report task for a tenant, the same task that can be
 * scheduled or triggered from a route.
 */
@Command(scope = "notima", name = "run-match-report", description = "Matches the pending files of a tenant's active payment channels (like process-payment-channel --match-only) and writes one report per channel to the tenant's report directory. Nothing is booked or moved.")
@Service
public class RunMatchReport extends AbstractAction {

	@Reference
	private CanonicalObjectFactory cof;

	@Reference
	private FormatterFactory formatterFactory;

	@Reference
	private AdapterToolsSettings settings;

	@Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo (default from AdapterTools config)", required = false, multiValued = false)
	private String countryCode;

	@Option(name = "--report-dir", description = "Write the reports to this directory instead of the tenant's report directory", required = false, multiValued = false)
	private String reportDirectory;

	@Option(name = "-format", description = "The report format (default " + PaymentChannelMatchReportTask.DEFAULT_FORMAT + ")", required = false, multiValued = false)
	private String format;

	@Option(name = "--until", aliases = { _NotimaCmdOptions.UNTIL_DATE }, description = "Only match files dated until (and including) this date yyyy-MM-dd. Files after it, or without a known date, are skipped.", required = false, multiValued = false)
	private String untilDateStr;

	@Argument(index = 0, name = "orgNo", description = "The org number of the tenant", required = true, multiValued = false)
	@Completion(OrgNoCompleter.class)
	private String orgNo;

	@Override
	protected Object onExecute() throws Exception {

		PaymentChannelMatchReportTask task = new PaymentChannelMatchReportTask(orgNo, sess.getConsole());
		task.setCanonicalObjectFactory(cof);
		task.setFormatterFactory(formatterFactory);
		task.setSettings(settings);
		task.setCountryCode(countryCode);
		task.setReportDirectory(reportDirectory);
		if (untilDateStr!=null) {
			task.setUntilDate(LocalDate.parse(untilDateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE));
		}
		if (format!=null && format.trim().length()>0) {
			task.setFormat(format.trim());
		}

		try {
			task.execute();
		} catch (Exception e) {
			sess.getConsole().println("Match report failed: " + e.getMessage());
		}
		return null;

	}

}

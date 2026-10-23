package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.AdapterToolsSettings;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.businessobjects.adapter.tools.dunning.DunningCandidate;
import org.notima.businessobjects.adapter.tools.dunning.DunningCandidateFinder;
import org.notima.businessobjects.adapter.tools.table.DunningCandidateTable;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * Lists the tenants that need a dunning run, ie that have overdue customer invoices.
 * See also create-dunning-run.
 */
@Command(scope = "notima", name = "list-dunning-candidates", description = "Lists tenants that need a dunning run (have overdue customer invoices). See also create-dunning-run.")
@Service
public class ListDunningCandidates implements Action {

	@Reference
	private Session sess;

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;

	@Reference
	private AdapterToolsSettings settings;

	@Reference
	private CanonicalObjectFactory cof;

	@Option(name = "-d", aliases = { "--days-overdue" }, description = "Minimum days past the due date for an invoice to be reminded (default 1, ie any overdue invoice)", required = false, multiValued = false)
	private int minDaysOverdue = 1;

	@Option(name = "--dunning-interval", description = "Days between reminders. Tenants reminded fewer days ago (print-dunning-run with --internal-comment) get status 'Dunned at' instead of 'Needs dunning' and are only shown with -a (default "
			+ DunningCandidateFinder.DEFAULT_DUNNING_INTERVAL_DAYS + ", 0 to ignore)", required = false, multiValued = false)
	private int dunningIntervalDays = DunningCandidateFinder.DEFAULT_DUNNING_INTERVAL_DAYS;

	@Option(name = "-co", aliases = { "--country-code" }, description = "Country code for all tenants checked (default: each tenant's own, else the default country code)", required = false, multiValued = false)
	private String countryCode;

	@Option(name = "-a", aliases = { "--all" }, description = "Also list tenants with nothing overdue or recently dunned", required = false, multiValued = false)
	private boolean showAll;

	@Argument(index = 0, name = "adapter", description = "Only check tenants of this adapter", required = false, multiValued = false)
	@Completion(AdapterCompleter.class)
	private String systemName;

	@Argument(index = 1, name = "orgNo", description = "Only check these tenants: one or more org numbers, separated by spaces or commas. All tenants if omitted", required = false, multiValued = true)
	@Completion(OrgNoCompleter.class)
	private List<String> orgNos;

	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {

		TenantQuerySupport.Selection selection = TenantQuerySupport.select(bofs, systemName, orgNos, sess);
		if (selection == null) return null;
		List<BusinessObjectFactory> adapters = selection.adapters;

		DunningCandidateFinder finder = new DunningCandidateFinder(settings.getDefaultCountryCode());
		finder.setMinDaysOverdue(minDaysOverdue);
		finder.setDunningIntervalDays(dunningIntervalDays);
		finder.setTenantInformationFactory(cof.lookupTenantInformationFactory());
		TenantQuerySupport.configure(finder, selection.orgNos, countryCode, sess);

		List<DunningCandidate> candidates = finder.find(adapters);
		TenantQuerySupport.clearProgress(sess);

		DunningCandidateTable table = new DunningCandidateTable(candidates, showAll, dunningIntervalDays);
		table.getShellTable().print(sess.getConsole());
		sess.getConsole().println();
		sess.getConsole().println(table.getSummary()
				+ (minDaysOverdue > 1 ? " (invoices at least " + minDaysOverdue + " days overdue)" : ""));

		return null;
	}


}

package org.notima.businessobjects.adapter.tools.command;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
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

	@Option(name = "-d", aliases = { "--days-overdue" }, description = "Minimum days past the due date for an invoice to be reminded (default 1, ie any overdue invoice)", required = false, multiValued = false)
	private int minDaysOverdue = 1;

	@Option(name = "--orgNo", description = "Only check these tenants: an org number, or several separated by commas (ie 556000-0000,716416-8515)", required = false, multiValued = false)
	private String orgNos;

	@Option(name = "-co", aliases = { "--country-code" }, description = "Country code for all tenants checked (default: each tenant's own, else the default country code)", required = false, multiValued = false)
	private String countryCode;

	@Option(name = "-a", aliases = { "--all" }, description = "Also list tenants with nothing overdue", required = false, multiValued = false)
	private boolean showAll;

	@Argument(index = 0, name = "adapter", description = "Only check tenants of this adapter", required = false, multiValued = false)
	@Completion(AdapterCompleter.class)
	private String systemName;

	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {

		List<BusinessObjectFactory> adapters = new ArrayList<BusinessObjectFactory>();
		if (bofs != null) {
			for (BusinessObjectFactory bf : bofs) {
				if (systemName == null || systemName.equals(bf.getSystemName())) {
					adapters.add(bf);
				}
			}
		}
		if (adapters.isEmpty()) {
			sess.getConsole().println(systemName == null ? "No adapters registered" : "No adapter named " + systemName);
			return null;
		}

		DunningCandidateFinder finder = new DunningCandidateFinder(settings.getDefaultCountryCode());
		finder.setMinDaysOverdue(minDaysOverdue);
		finder.setCountryCode(countryCode);
		if (orgNos != null) {
			finder.setOnlyTaxIds(Arrays.asList(orgNos.split(",")));
		}
		finder.setProgressListener((adapterName, tenant, index, count) -> {
			// Overwritten on the same line; cleared when done
			sess.getConsole().print("\rChecking " + adapterName + " " + index + "/" + count + ": "
					+ fit(tenant.getName(), 50) + "\u001B[K");
			sess.getConsole().flush();
		});

		List<DunningCandidate> candidates = finder.find(adapters);
		sess.getConsole().print("\r\u001B[K");

		DunningCandidateTable table = new DunningCandidateTable(candidates, showAll);
		table.getShellTable().print(sess.getConsole());
		sess.getConsole().println();
		sess.getConsole().println(table.getSummary()
				+ (minDaysOverdue > 1 ? " (invoices at least " + minDaysOverdue + " days overdue)" : ""));

		return null;
	}

	private static String fit(String s, int max) {
		if (s == null) return "";
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}

}

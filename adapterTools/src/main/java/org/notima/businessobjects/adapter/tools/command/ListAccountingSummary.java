package org.notima.businessobjects.adapter.tools.command;

import java.time.Duration;
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
import org.notima.businessobjects.adapter.tools.accounting.AccountingModelService;
import org.notima.businessobjects.adapter.tools.accounting.AccountingSummary;
import org.notima.businessobjects.adapter.tools.accounting.AccountingSummaryFinder;
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.businessobjects.adapter.tools.table.AccountingSummaryTable;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * Lists key figures of the accounting of tenants: revenue and result of the fiscal year until
 * the date, equity and cash at the date. See also refresh-accounting.
 */
@Command(scope = "notima", name = "list-accounting-summary", description = "Lists revenue and result of the year, equity and cash of tenants at a date. See also refresh-accounting.")
@Service
public class ListAccountingSummary implements Action {

	@Reference
	private Session sess;

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;

	@Reference
	private AdapterToolsSettings settings;

	@Reference
	private AccountingModelService accountingModels;

	@Option(name = "--date", description = "The date (yyyy-MM-dd, default today)", required = false, multiValued = false)
	private String dateStr;

	@Option(name = "--refresh", description = "Fetch the accounting from the accounting systems even if cached", required = false, multiValued = false)
	private boolean refresh;

	@Option(name = "-co", aliases = { "--country-code" }, description = "Country code for all tenants (default: each tenant's own, else the default country code)", required = false, multiValued = false)
	private String countryCode;

	@Option(name = "-a", aliases = { "--all" }, description = "Also list tenants without accounting", required = false, multiValued = false)
	private boolean showAll;

	@Argument(index = 0, name = "adapter", description = "Only tenants of this adapter", required = false, multiValued = false)
	@Completion(AdapterCompleter.class)
	private String systemName;

	@Argument(index = 1, name = "orgNo", description = "Only these tenants: one or more org numbers, separated by spaces or commas. All tenants if omitted", required = false, multiValued = true)
	@Completion(OrgNoCompleter.class)
	private List<String> orgNos;

	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {

		TenantQuerySupport.Selection selection = TenantQuerySupport.select(bofs, systemName, orgNos, sess);
		if (selection == null) return null;
		List<BusinessObjectFactory> adapters = selection.adapters;

		AccountingSummaryFinder finder = new AccountingSummaryFinder(settings.getDefaultCountryCode(), accountingModels);
		finder.setDate(TenantQuerySupport.parseDate(dateStr));
		if (refresh) finder.setMaxAge(Duration.ZERO);
		TenantQuerySupport.configure(finder, selection.orgNos, countryCode, sess);

		List<AccountingSummary> summaries = finder.find(adapters);
		TenantQuerySupport.clearProgress(sess);

		AccountingSummaryTable table = new AccountingSummaryTable(summaries, showAll);
		table.getShellTable().print(sess.getConsole());
		sess.getConsole().println();
		sess.getConsole().println(table.getSummary() + " at " + finder.getDate());

		return null;
	}

}

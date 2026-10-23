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
import org.notima.businessobjects.adapter.tools.accounting.AccountingRefreshResult;
import org.notima.businessobjects.adapter.tools.accounting.AccountingRefresher;
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
import org.notima.businessobjects.adapter.tools.table.AccountingRefreshTable;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * Fetches the accounting of tenants from their accounting systems into the cache used by the
 * accounting queries (list-accounting-summary, list-account-balances) and reports.
 */
@Command(scope = "notima", name = "refresh-accounting", description = "Fetches the accounting of tenants from their accounting systems into the accounting cache")
@Service
public class RefreshAccounting implements Action {

	@Reference
	private Session sess;

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;

	@Reference
	private AdapterToolsSettings settings;

	@Reference
	private AccountingModelService accountingModels;

	@Option(name = "--date", description = "Refresh the fiscal year that contains this date (yyyy-MM-dd, default today)", required = false, multiValued = false)
	private String dateStr;

	@Option(name = "--all-years", description = "Refresh all fiscal years of the tenants", required = false, multiValued = false)
	private boolean allYears;

	@Option(name = "--max-age-hours", description = "Only refresh fiscal years cached more than this many hours ago (default 0, ie all)", required = false, multiValued = false)
	private int maxAgeHours = 0;

	@Option(name = "--orgNo", description = "Only these tenants: an org number, or several separated by commas", required = false, multiValued = false)
	private String orgNos;

	@Option(name = "-co", aliases = { "--country-code" }, description = "Country code for all tenants (default: each tenant's own, else the default country code)", required = false, multiValued = false)
	private String countryCode;

	@Option(name = "-a", aliases = { "--all" }, description = "Also list tenants without accounting", required = false, multiValued = false)
	private boolean showAll;

	@Argument(index = 0, name = "adapter", description = "Only tenants of this adapter", required = false, multiValued = false)
	@Completion(AdapterCompleter.class)
	private String systemName;

	@SuppressWarnings("rawtypes")
	@Override
	public Object execute() throws Exception {

		List<BusinessObjectFactory> adapters = TenantQuerySupport.adapters(bofs, systemName, sess);
		if (adapters.isEmpty()) return null;

		AccountingRefresher refresher = new AccountingRefresher(settings.getDefaultCountryCode(), accountingModels);
		refresher.setDate(TenantQuerySupport.parseDate(dateStr));
		refresher.setAllYears(allYears);
		refresher.setMaxAge(Duration.ofHours(Math.max(maxAgeHours, 0)));
		TenantQuerySupport.configure(refresher, orgNos, countryCode, sess);

		List<AccountingRefreshResult> results = refresher.find(adapters);
		TenantQuerySupport.clearProgress(sess);

		AccountingRefreshTable table = new AccountingRefreshTable(results, showAll);
		table.getShellTable().print(sess.getConsole());
		sess.getConsole().println();
		sess.getConsole().println(table.getSummary() + " into " + accountingModels.getCache().getBaseDirectory());

		return null;
	}

}

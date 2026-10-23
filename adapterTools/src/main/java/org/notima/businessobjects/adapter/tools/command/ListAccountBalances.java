package org.notima.businessobjects.adapter.tools.command;

import java.time.Duration;
import java.util.ArrayList;
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
import org.notima.businessobjects.adapter.tools.accounting.AccountBalanceFinder;
import org.notima.businessobjects.adapter.tools.accounting.AccountBalances;
import org.notima.businessobjects.adapter.tools.accounting.AccountingModelService;
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
import org.notima.businessobjects.adapter.tools.table.AccountBalanceTable;
import org.notima.generic.businessobjects.AccountCategory;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * Lists the balances of accounts or account categories of tenants at a date, ie to see which
 * tenants have money left on a tax account. See also refresh-accounting.
 */
@Command(scope = "notima", name = "list-account-balances", description = "Lists the balances of accounts or account categories of tenants at a date. See also refresh-accounting.")
@Service
public class ListAccountBalances implements Action {

	@Reference
	private Session sess;

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;

	@Reference
	private AdapterToolsSettings settings;

	@Reference
	private AccountingModelService accountingModels;

	@Option(name = "--account", description = "Accounts, separated by commas. A prefix ending with * sums the accounts starting with it (ie 1930,19*)", required = false, multiValued = false)
	private String accounts;

	@Option(name = "--category", description = "Account categories, separated by commas (ie ASSET_CASH,EQUITY). The rules of the tenant's country set the categories", required = false, multiValued = false)
	private String categories;

	@Option(name = "--date", description = "The date (yyyy-MM-dd, default today)", required = false, multiValued = false)
	private String dateStr;

	@Option(name = "--non-zero", description = "Only show balances that aren't zero", required = false, multiValued = false)
	private boolean nonZero;

	@Option(name = "--refresh", description = "Fetch the accounting from the accounting systems even if cached", required = false, multiValued = false)
	private boolean refresh;

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

		List<String> accountList = split(accounts);
		List<AccountCategory> categoryList = new ArrayList<AccountCategory>();
		for (String c : split(categories)) {
			AccountCategory category = AccountCategory.parse(c.toUpperCase());
			if (category == null) {
				sess.getConsole().println("Unknown category " + c + ". Categories: " + java.util.Arrays.toString(AccountCategory.values()));
				return null;
			}
			categoryList.add(category);
		}
		if (accountList.isEmpty() && categoryList.isEmpty()) {
			sess.getConsole().println("Give the accounts (--account) or categories (--category) to list.");
			return null;
		}

		List<BusinessObjectFactory> adapters = TenantQuerySupport.adapters(bofs, systemName, sess);
		if (adapters.isEmpty()) return null;

		AccountBalanceFinder finder = new AccountBalanceFinder(settings.getDefaultCountryCode(), accountingModels);
		finder.setAccounts(accountList);
		finder.setCategories(categoryList);
		finder.setDate(TenantQuerySupport.parseDate(dateStr));
		if (refresh) finder.setMaxAge(Duration.ZERO);
		TenantQuerySupport.configure(finder, orgNos, countryCode, sess);

		List<AccountBalances> results = finder.find(adapters);
		TenantQuerySupport.clearProgress(sess);

		AccountBalanceTable table = new AccountBalanceTable(results, showAll, nonZero);
		table.getShellTable().print(sess.getConsole());
		sess.getConsole().println();
		sess.getConsole().println(table.getSummary() + " at " + finder.getDate());

		return null;
	}

	private static List<String> split(String s) {
		List<String> result = new ArrayList<String>();
		if (s == null) return result;
		for (String part : s.split(",")) {
			if (!part.trim().isEmpty()) result.add(part.trim());
		}
		return result;
	}

}

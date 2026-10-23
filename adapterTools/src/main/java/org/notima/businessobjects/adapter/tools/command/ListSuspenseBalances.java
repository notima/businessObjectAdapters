package org.notima.businessobjects.adapter.tools.command;

import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
import org.notima.businessobjects.adapter.tools.accounting.AccountingModelService;
import org.notima.businessobjects.adapter.tools.accounting.SuspenseAccountFinder;
import org.notima.businessobjects.adapter.tools.accounting.SuspenseBalances;
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.businessobjects.adapter.tools.table.SuspenseBalanceTable;
import org.notima.generic.businessobjects.SuspenseAccountType;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;

/**
 * Lists the suspense accounts of tenants that aren't zero. Which accounts are suspense
 * accounts is set per tenant and type with set-tenant-info, ie
 * {@code set-tenant-info 556000-0000 suspenseAccounts.UNIDENTIFIED 1799,2999}.
 */
@Command(scope = "notima", name = "list-suspense-balances", description = "Lists the suspense accounts of tenants that aren't zero. Set them with set-tenant-info <orgNo> suspenseAccounts.<type> <accounts>.")
@Service
public class ListSuspenseBalances implements Action {

	@Reference
	private Session sess;

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;

	@Reference
	private AdapterToolsSettings settings;

	@Reference
	private CanonicalObjectFactory cof;

	@Reference
	private AccountingModelService accountingModels;

	@Option(name = "--type", description = "Only these types, separated by commas: UNIDENTIFIED, IDENTIFIED, OWNER_SETTLEMENT (default all)", required = false, multiValued = false)
	private String typesStr;

	@Option(name = "--date", description = "The date (yyyy-MM-dd, default today)", required = false, multiValued = false)
	private String dateStr;

	@Option(name = "--include-zero", description = "Also list suspense accounts that are zero", required = false, multiValued = false)
	private boolean includeZero;

	@Option(name = "--refresh", description = "Fetch the accounting from the accounting systems even if cached", required = false, multiValued = false)
	private boolean refresh;

	@Option(name = "-co", aliases = { "--country-code" }, description = "Country code for all tenants (default: each tenant's own, else the default country code)", required = false, multiValued = false)
	private String countryCode;

	@Option(name = "-a", aliases = { "--all" }, description = "Also list tenants without suspense accounts or accounting", required = false, multiValued = false)
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

		Set<SuspenseAccountType> types = EnumSet.noneOf(SuspenseAccountType.class);
		if (typesStr != null) {
			for (String t : typesStr.split(",")) {
				if (t.trim().isEmpty()) continue;
				SuspenseAccountType type = SuspenseAccountType.parse(t);
				if (type == null) {
					sess.getConsole().println("Unknown type " + t.trim() + ". Types: " + Arrays.toString(SuspenseAccountType.values()));
					return null;
				}
				types.add(type);
			}
		}

		TenantInformationFactory tif = cof.lookupTenantInformationFactory();
		if (tif == null) {
			sess.getConsole().println("No TenantInformationFactory registered, so no suspense accounts are set. Is the notima-json feature installed?");
			return null;
		}

		TenantQuerySupport.Selection selection = TenantQuerySupport.select(bofs, systemName, orgNos, sess);
		if (selection == null) return null;

		SuspenseAccountFinder finder = new SuspenseAccountFinder(settings.getDefaultCountryCode(), accountingModels, tif);
		finder.setTypes(types);
		finder.setDate(TenantQuerySupport.parseDate(dateStr));
		if (refresh) finder.setMaxAge(Duration.ZERO);
		TenantQuerySupport.configure(finder, selection.orgNos, countryCode, sess);

		List<SuspenseBalances> results = finder.find(selection.adapters);
		TenantQuerySupport.clearProgress(sess);

		SuspenseBalanceTable table = new SuspenseBalanceTable(results, showAll, includeZero);
		table.getShellTable().print(sess.getConsole());
		sess.getConsole().println();
		sess.getConsole().println(table.getSummary() + " at " + finder.getDate());

		return null;
	}

}

package org.notima.businessobjects.adapter.tools.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.businessobjects.adapter.tools.table.VendorPaymentCandidateTable;
import org.notima.businessobjects.adapter.tools.vendorpayment.VendorPaymentCandidate;
import org.notima.businessobjects.adapter.tools.vendorpayment.VendorPaymentCandidateFinder;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * Lists the tenants that have vendor invoices to pay, ie overdue or falling due within
 * --days-ahead days. See also list-vendor-invoices.
 */
@Command(scope = "notima", name = "list-vendor-payment-candidates", description = "Lists tenants that have vendor invoices to pay (overdue or due within --days-ahead days). See also list-vendor-invoices.")
@Service
public class ListVendorPaymentCandidates implements Action {

	@Reference
	private Session sess;

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;

	@Reference
	private AdapterToolsSettings settings;

	@Option(name = "-d", aliases = { "--days-ahead" }, description = "Count invoices that fall due within this many days from today (default "
			+ VendorPaymentCandidateFinder.DEFAULT_DAYS_AHEAD + ")", required = false, multiValued = false)
	private int daysAhead = VendorPaymentCandidateFinder.DEFAULT_DAYS_AHEAD;

	@Option(name = _NotimaCmdOptions.UNTIL_DATE, description = "Count invoices that fall due on or before this date (yyyy-MM-dd), ie the next payment date. Overrides --days-ahead", required = false, multiValued = false)
	private String untilDateStr;

	@Option(name = "--exclude-overdue", description = "Only count invoices that fall due within the period, not those already overdue", required = false, multiValued = false)
	private boolean excludeOverdue;

	@Option(name = "--exclude-unbooked", description = "Don't count unbooked invoices (by default they're counted and shown in column Unbooked)", required = false, multiValued = false)
	private boolean excludeUnbooked;

	@Option(name = "-co", aliases = { "--country-code" }, description = "Country code for all tenants checked (default: each tenant's own, else the default country code)", required = false, multiValued = false)
	private String countryCode;

	@Option(name = "-a", aliases = { "--all" }, description = "Also list tenants with nothing to pay", required = false, multiValued = false)
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

		VendorPaymentCandidateFinder finder = new VendorPaymentCandidateFinder(settings.getDefaultCountryCode());
		finder.setDaysAhead(daysAhead);
		if (untilDateStr != null) {
			DateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd");
			dfmt.setLenient(false);
			finder.setPaymentDate(dfmt.parse(untilDateStr));
		}
		finder.setIncludeOverdue(!excludeOverdue);
		finder.setIncludeUnbooked(!excludeUnbooked);
		TenantQuerySupport.configure(finder, selection.orgNos, countryCode, sess);

		List<VendorPaymentCandidate> candidates = finder.find(adapters);
		TenantQuerySupport.clearProgress(sess);

		VendorPaymentCandidateTable table = new VendorPaymentCandidateTable(candidates, showAll, finder.getLastDueDate());
		table.getShellTable().print(sess.getConsole());
		sess.getConsole().println();
		sess.getConsole().println(table.getSummary()
				+ (excludeOverdue ? " (overdue invoices not included)" : "")
				+ (excludeUnbooked ? " (unbooked invoices not included)" : ""));

		return null;
	}


}

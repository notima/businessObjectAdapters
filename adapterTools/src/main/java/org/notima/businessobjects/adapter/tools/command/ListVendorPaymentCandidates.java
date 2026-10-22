package org.notima.businessobjects.adapter.tools.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

	@Option(name = "--orgNo", description = "Only check these tenants: an org number, or several separated by commas (ie 556000-0000,716416-8515)", required = false, multiValued = false)
	private String orgNos;

	@Option(name = "-co", aliases = { "--country-code" }, description = "Country code for all tenants checked (default: each tenant's own, else the default country code)", required = false, multiValued = false)
	private String countryCode;

	@Option(name = "-a", aliases = { "--all" }, description = "Also list tenants with nothing to pay", required = false, multiValued = false)
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

		VendorPaymentCandidateFinder finder = new VendorPaymentCandidateFinder(settings.getDefaultCountryCode());
		finder.setDaysAhead(daysAhead);
		if (untilDateStr != null) {
			DateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd");
			dfmt.setLenient(false);
			finder.setPaymentDate(dfmt.parse(untilDateStr));
		}
		finder.setIncludeOverdue(!excludeOverdue);
		finder.setIncludeUnbooked(!excludeUnbooked);
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

		List<VendorPaymentCandidate> candidates = finder.find(adapters);
		sess.getConsole().print("\r\u001B[K");

		VendorPaymentCandidateTable table = new VendorPaymentCandidateTable(candidates, showAll, finder.getLastDueDate());
		table.getShellTable().print(sess.getConsole());
		sess.getConsole().println();
		sess.getConsole().println(table.getSummary()
				+ (excludeOverdue ? " (overdue invoices not included)" : "")
				+ (excludeUnbooked ? " (unbooked invoices not included)" : ""));

		return null;
	}

	private static String fit(String s, int max) {
		if (s == null) return "";
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}

}

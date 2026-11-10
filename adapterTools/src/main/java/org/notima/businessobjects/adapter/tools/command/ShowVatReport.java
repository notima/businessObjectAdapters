package org.notima.businessobjects.adapter.tools.command;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.accounting.AccountingModelService;
import org.notima.businessobjects.adapter.tools.accounting.TenantVatReporter;
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.businessobjects.adapter.tools.table.VatDeviationTable;
import org.notima.businessobjects.adapter.tools.table.VatReportTable;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;

/**
 * Shows the VAT return of a tenant for a date range, calculated from the accounting model
 * that refresh-accounting caches (fetched if it isn't cached, or is older than the cache's
 * maximum age). The range may span fiscal years.
 *
 * <p>The output VAT on purchases with reverse charge is checked voucher by voucher: the
 * Calculated column is the VAT that the bases give, and the vouchers where the booked VAT
 * differs are listed.</p>
 */
@Command(scope = "notima", name = "show-vat-report", description = "Shows the VAT return of a tenant for a date range, from its accounting. See also refresh-accounting.")
@Service
public class ShowVatReport implements Action {

	@Reference
	private Session sess;

	@Reference
	private AccountingModelService accountingModels;

	@Reference
	private CanonicalObjectFactory cof;

	@Option(name = "-co", aliases = { "--country-code" }, description = "The tenant's country, whose VAT return to make (default SE)", required = false, multiValued = false)
	private String countryCode = "SE";

	@Option(name = "--adapter", description = "Only the accounting of this adapter (default: the first that has the tenant's accounting)", required = false, multiValued = false)
	@Completion(AdapterCompleter.class)
	private String systemName;

	@Option(name = "--accounts", description = "Show the accounts of each box", required = false, multiValued = false)
	private boolean showAccounts;

	@Option(name = "-a", aliases = { "--all" }, description = "Also show boxes that are zero", required = false, multiValued = false)
	private boolean showAll;

	@Option(name = "--refresh", description = "Fetch the accounting from the accounting system even if cached", required = false, multiValued = false)
	private boolean refresh;

	@Argument(index = 0, name = "orgNo", description = "The tenant's org number", required = true, multiValued = false)
	@Completion(OrgNoCompleter.class)
	private String orgNo;

	@Argument(index = 1, name = "fromDate", description = "First day of the range (yyyy-MM-dd)", required = true, multiValued = false)
	private String fromStr;

	@Argument(index = 2, name = "toDate", description = "Last day of the range (yyyy-MM-dd)", required = true, multiValued = false)
	private String toStr;

	@Override
	public Object execute() throws Exception {

		LocalDate from, to;
		try {
			from = LocalDate.parse(fromStr.trim());
			to = LocalDate.parse(toStr.trim());
		} catch (DateTimeParseException e) {
			sess.getConsole().println("Dates are given as yyyy-MM-dd: " + e.getParsedString());
			return null;
		}
		if (to.isBefore(from)) {
			sess.getConsole().println("The range ends before it starts.");
			return null;
		}

		String cc = countryCode != null && !countryCode.trim().isEmpty() ? countryCode.trim().toUpperCase() : "SE";
		TenantVatReporter reporter = new TenantVatReporter(accountingModels);
		if (refresh) reporter.setMaxAge(Duration.ZERO);
		TaxSubjectIdentifier tenant = new TaxSubjectIdentifier(orgNo, cc);
		// The tenant's own VAT codes (set-tenant-info orgNo vatCode.3010 MP1)
		TenantInformationFactory tif = cof != null ? cof.lookupTenantInformationFactory() : null;
		TenantInformation ti = tif != null ? tif.getTenantInformation(tenant) : null;
		java.util.Map<String, String> vatCodes = ti != null ? ti.getVatCodes() : null;
		TenantVatReporter.Result result = reporter.report(systemName, tenant, from, to, vatCodes);
		if (!result.isOk()) {
			sess.getConsole().println(result.getStatusText());
			return null;
		}

		sess.getConsole().println(result.getReport().getDefinition().getName() + " " + orgNo + " " + from + " - " + to
				+ " (" + result.getSystemName() + ", " + String.join(", ", result.getFiscalYears())
				+ (vatCodes != null && !vatCodes.isEmpty() ? ", " + vatCodes.size() + " VAT codes from the tenant's settings" : "") + ")");
		new VatReportTable(result.getReport(), showAll, showAccounts).getShellTable().print(sess.getConsole());
		sess.getConsole().println();
		sess.getConsole().println(VatReportTable.toPay(result.getReport()) + "  (" + result.getReport().getVoucherCount()
				+ " vouchers; whole units, fractions left out)");
		if (!result.getReport().getDeviations().isEmpty()) {
			sess.getConsole().println();
			sess.getConsole().println("Vouchers whose VAT differs from base x rate (reverse charge in boxes 20-24 and 50; "
					+ "sales in boxes 05-08 with a known rate; VAT booked on a separate voucher shows as both missing and without base):");
			new VatDeviationTable(result.getReport().getDeviations()).getShellTable().print(sess.getConsole());
		}
		String checks = VatReportTable.checks(result.getReport());
		if (!checks.isEmpty()) {
			sess.getConsole().println();
			sess.getConsole().println("To check:");
			sess.getConsole().print(checks);
		}
		return null;
	}

}

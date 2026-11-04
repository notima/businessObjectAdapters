package org.notima.businessobjects.adapter.tools.command;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.ifacebusinessobjects.CollectionNoticeFormatter;
import org.notima.businessobjects.adapter.tools.FormatterFactory;
import org.notima.businessobjects.adapter.tools.command.completer.AdapterCompleter;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.businessobjects.adapter.tools.dunning.DunningRunFilter;
import org.notima.generic.businessobjects.DunningEntry;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.InvoiceFormatter;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;

/**
 * Creates collection notices (inkassokrav) for the given invoices.
 * <p>
 * The invoices are taken from the adapter's dunning run, ie unpaid overdue invoices, so
 * that the debtor, creditor and payment account are the same as on the reminders. One
 * notice is created per debtor. The notices are formatted by the registered
 * {@link CollectionNoticeFormatter} for the format (the jasperReportAdapter's for pdf).
 */
@Command(scope = "notima", name = "create-collection-notice", description = "Create collection notices (inkassokrav) for the given overdue invoices, one per debtor")
@Service
public class CreateCollectionNotice implements Action {

	@Reference
	private CanonicalObjectFactory cof;

	@Reference
	private FormatterFactory formatterFactory;

	@Reference
	private Session sess;

	@Argument(index = 0, name = "adapterName", description = "The adapter name", required = true, multiValued = false)
	@Completion(AdapterCompleter.class)
	private String adapterName = "";

	@Argument(index = 1, name = "orgNo", description = "The orgno of the client", required = true, multiValued = false)
	@Completion(OrgNoCompleter.class)
	private String orgNo = "";

	@Argument(index = 2, name = "invoiceNo", description = "The invoice numbers to include", required = true, multiValued = true)
	private List<String> invoiceNos;

	@Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo", required = false, multiValued = false)
	private String countryCode = "SE";

	@Option(name = "-od", aliases = { "--output-dir" }, description = "Output directory. Default is the tenant's default output directory, or the home directory", required = false, multiValued = false)
	@Completion(FileCompleter.class)
	private String outputDirectory;

	@Option(name = "-cf", aliases = { "--collection-fee" }, description = "Collection fee (inkassokostnad). Default 180", required = false, multiValued = false)
	private String collectionFeeStr;

	@Option(name = "-rf", aliases = { "--reminder-fee" }, description = "Reminder fee (påminnelseavgift), 0 if no reminder fee has been charged. Default 60", required = false, multiValued = false)
	private String reminderFeeStr;

	@Option(name = "-ir", aliases = { "--interest-rate" }, description = "Yearly late payment interest in percent (dröjsmålsränta = the reference rate + 8). Default 8", required = false, multiValued = false)
	private String interestRateStr;

	@Option(name = "--samf", description = "Use the template stating that a samfällighet's fee is directly enforceable (bankgiro). The plusgiro template always states it", required = false, multiValued = false)
	private boolean samf;

	@Option(name = "--pg", description = "Use the plusgiro payment slip. Default is plusgiro if the tenant's remit to account type is PG, else bankgiro", required = false, multiValued = false)
	private boolean plusgiro;

	@Option(name = "-t", aliases = { "--template-file" }, description = "Use this template file (for pdf a .jasper file) instead of the formatter's own templates", required = false, multiValued = false)
	@Completion(FileCompleter.class)
	private String templateFile;

	@Option(name = "-f", aliases = { "--format" }, description = "Output format. Default pdf", required = false, multiValued = false)
	private String format = "pdf";

	private BusinessObjectFactory<?,?,?,?,?,?> bof;
	private TenantInformation tenantInformation;
	private DunningRun<?,?> dunningRun;

	@Override
	public Object execute() throws Exception {

		Double collectionFee = parseAmount(collectionFeeStr, "collection fee");
		Double reminderFee = parseAmount(reminderFeeStr, "reminder fee");
		Double interestRate = parseAmount(interestRateStr, "interest rate");

		CollectionNoticeFormatter formatter = formatterFactory.getCollectionNoticeFormatter(format);
		if (formatter == null) {
			sess.getConsole().println("No collection notice formatter for " + format + " available (is the jasperReportAdapter installed?)");
			return null;
		}

		bof = cof.lookupAdapter(adapterName);
		if (bof == null) {
			sess.getConsole().println("No adapter named " + adapterName);
			return null;
		}
		bof.setTenant(orgNo, countryCode);
		tenantInformation = lookupTenantInformation();

		if (!selectInvoices()) {
			return null;
		}

		boolean usePlusgiro = applyTenantPaymentInformation() || plusgiro;

		Properties props = new Properties();
		props.setProperty(CollectionNoticeFormatter.TEMPLATE, usePlusgiro ? CollectionNoticeFormatter.TEMPLATE_PLUSGIRO
				: (samf ? CollectionNoticeFormatter.TEMPLATE_BANKGIRO_SAMF : CollectionNoticeFormatter.TEMPLATE_BANKGIRO));
		String outDir = getOutputDirectory();
		if (outDir != null) {
			props.setProperty(InvoiceFormatter.OUTPUT_DIR, outDir);
		}
		if (templateFile != null) {
			props.setProperty(InvoiceFormatter.TEMPLATE_FILE, templateFile);
		}
		if (collectionFee != null) {
			props.put(CollectionNoticeFormatter.COLLECTION_FEE, collectionFee);
		}
		if (interestRate != null) {
			props.put(CollectionNoticeFormatter.INTEREST_RATE, interestRate / 100.0);
		}

		for (DunningEntry<?,?> entry : dunningRun.getEntries()) {
			if (reminderFee != null) {
				entry.setReminderFee(reminderFee);
			}
			props.setProperty(InvoiceFormatter.OUTPUT_FILENAME, getFileName(entry));
			String path = formatter.formatCollectionNotice(entry, format, props);
			sess.getConsole().println(entry.getDebtor().getName() + ": " + invoiceKeys(entry) + " -> " + path);
		}

		return null;
	}

	/**
	 * Keeps the given invoices of the adapter's dunning run.
	 *
	 * @return	True if there is anything to create a notice for.
	 */
	private boolean selectInvoices() throws Exception {

		final Set<String> wanted = new LinkedHashSet<String>();
		for (String no : invoiceNos) {
			for (String s : no.split(",")) {
				if (s.trim().length() > 0) wanted.add(s.trim());
			}
		}

		dunningRun = bof.lookupDunningRun(null, null);
		if (dunningRun == null) {
			sess.getConsole().println("The adapter " + adapterName + " returned no overdue invoices");
			return false;
		}
		DunningRunFilter.exclude(dunningRun, inv -> !wanted.contains(inv.getDocumentKey()));

		Set<String> found = new LinkedHashSet<String>();
		for (DunningEntry<?,?> entry : dunningRun.getEntries()) {
			for (Invoice<?> inv : entry.getInvoices()) {
				found.add(inv.getDocumentKey());
			}
		}
		List<String> missing = new ArrayList<String>();
		for (String no : wanted) {
			if (!found.contains(no)) missing.add(no);
		}
		if (!missing.isEmpty()) {
			sess.getConsole().println("Not included since they aren't unpaid and overdue: " + String.join(", ", missing));
		}

		int notOwed = DunningRunFilter.excludeNotOwed(dunningRun);
		if (notOwed > 0) {
			sess.getConsole().println(notOwed + (notOwed == 1 ? " invoice" : " invoices")
					+ " not included since nothing is owed (no open amount, or the customer's overpayments and credits exceed the debt)");
		}

		if (dunningRun.getEntries().isEmpty()) {
			sess.getConsole().println("No collection notices to create");
			return false;
		}
		return true;

	}

	/**
	 * Uses the tenant information's remit to account, if any, as on the reminders.
	 *
	 * @return	True if the remit to account is a plusgiro account.
	 */
	private boolean applyTenantPaymentInformation() {

		TenantInformation ti = tenantInformation;
		if (ti == null || ti.getRemitToAccount() == null || ti.getRemitToAccount().trim().length() == 0) {
			return false;
		}
		for (DunningEntry<?,?> entry : dunningRun.getEntries()) {
			entry.setBgNo(ti.getRemitToAccount().trim());
			if (entry.getCreditor() != null) {
				ti.copyPaymentInformationTo(entry.getCreditor());
			}
		}
		boolean pg = ti.getRemitToAccountType() != null && "PG".equalsIgnoreCase(ti.getRemitToAccountType().trim());
		sess.getConsole().println("Payment to " + (pg ? "plusgiro " : "bankgiro ") + ti.getRemitToAccount().trim()
				+ " (from tenant information)");
		return pg;

	}

	private String getOutputDirectory() {
		if (outputDirectory != null) return outputDirectory;
		if (tenantInformation != null && tenantInformation.getDefaultOutputDirectory() != null
				&& tenantInformation.getDefaultOutputDirectory().trim().length() > 0) {
			File dir = new File(tenantInformation.getDefaultOutputDirectory().trim());
			dir.mkdirs();
			return dir.getPath();
		}
		return null;
	}

	private static String getFileName(DunningEntry<?,?> entry) {
		return "inkassokrav-" + String.join("-", invoiceKeys(entry)).replaceAll("[^a-zA-Z0-9_\\-]", "_");
	}

	private static List<String> invoiceKeys(DunningEntry<?,?> entry) {
		List<String> keys = new ArrayList<String>();
		for (Invoice<?> inv : entry.getInvoices()) {
			keys.add(inv.getDocumentKey());
		}
		return keys;
	}

	private static Double parseAmount(String s, String what) throws Exception {
		if (s == null) return null;
		Double d;
		try {
			d = Double.valueOf(s.trim().replace(',', '.'));
		} catch (NumberFormatException e) {
			throw new Exception("Invalid " + what + ": " + s);
		}
		if (d < 0) {
			throw new Exception("The " + what + " can't be negative: " + s);
		}
		return d;
	}

	private TenantInformation lookupTenantInformation() {
		TenantInformationFactory tif = cof.lookupTenantInformationFactory();
		return tif != null ? tif.getTenantInformation(new TaxSubjectIdentifier(orgNo.trim(), countryCode)) : null;
	}

}

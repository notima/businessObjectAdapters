package org.notima.businessobjects.adapter.tools.command;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXB;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.dunning.DunningRunFilter;
import org.notima.businessobjects.adapter.tools.table.DunningRunTable;
import org.notima.generic.businessobjects.DunningEntry;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.businessobjects.exception.NoSuchTenantException;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;
import org.notima.util.NotimaUtil;
import org.apache.karaf.shell.api.action.Completion;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;

@Command(scope = "notima", name = "create-dunning-run", description = "Create a dunning run file")
@Service
public class CreateDunningRun implements Action {

	public static DateFormat	s_dfmt = new SimpleDateFormat("yyyy-MM-dd");	
	
	/** --outfile value that writes to the tenant's default output directory. */
	public static final String DEFAULT_OUTFILE = "default";
	
	@Reference
	private CanonicalObjectFactory cof;
	
	@Reference
	Session sess;
	
	@Argument(index = 0, name = "adapterName", description ="The adapter name", required = true, multiValued = false)
	private String adapterName = "";
	
	@Argument(index = 1, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	@Completion(OrgNoCompleter.class)
	private String orgNo = "";
	
	@Option(name = "--duedateuntil", description = "Select invoices with max this due date. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String dueDateUntilStr;
	
	@Option(name = "--duedatefrom", description = "Skip invoices due before this date, ie old invoices that are hard to collect. (format yyyy-mm-dd)", required = false, multiValued = false)
	private String dueDateFromStr;
	
    @Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo", required = false, multiValued = false)
    private String countryCode = "SE";
	
    @Option(name = "-of", aliases = { "--outfile" }, description = "Write the dunning run to file. 'default' writes it to the tenant's default output directory as <tenant>-dunning-run-<yyyyMMdd-HHmmss>.xml", required = false, multiValued = false)
    private String outFile;
    
    @Option(name = "-rf", aliases = { "--reminder-fee" }, description = "Reminder fee added to each reminder, e.g. 60 or 62.50. If not set, the reminder template's default fee (60) applies.", required = false, multiValued = false)
    private String reminderFeeStr;
    
    private Date dueDateUntil;
    
    private Date dueDateFrom;
    
    /** Invoices removed by --duedatefrom. */
    private int excludedInvoices;
    
    private Double reminderFee;
    
	private BusinessObjectFactory<?,?,?,?,?,?> bof;

	private DunningRun<?,?> dunningRun;
	
	@Override
	public Object execute() throws Exception {
		
		initBusinessObjectFactory();
		
		parseDates();
		
		parseReminderFee();
		
		createDunningRun();
		
		setReminderFee();
		
		applyTenantPaymentInformation();
		
		printDunningRun();
		
		writeToFileIfApplicable();
		
		// Printed above; returning it would make the shell print the object itself
		return null;
		
	}
	
	
	private void parseDates() throws ParseException {
		
		if (dueDateUntilStr!=null) {
			dueDateUntil = s_dfmt.parse(dueDateUntilStr);
		}
		if (dueDateFromStr!=null) {
			dueDateFrom = s_dfmt.parse(dueDateFromStr);
		}
		if (dueDateFrom!=null && dueDateUntil!=null && dueDateFrom.after(dueDateUntil)) {
			throw new ParseException("--duedatefrom (" + dueDateFromStr + ") is after --duedateuntil (" + dueDateUntilStr + ")", 0);
		}
		
	}
	
	private void parseReminderFee() throws Exception {
		
		if (reminderFeeStr==null) return;
		try {
			reminderFee = Double.valueOf(reminderFeeStr.trim().replace(',', '.'));
		} catch (NumberFormatException e) {
			throw new Exception("Invalid reminder fee: " + reminderFeeStr);
		}
		if (reminderFee < 0) {
			throw new Exception("The reminder fee can't be negative: " + reminderFeeStr);
		}
		
	}
	
	/**
	 * Sets the reminder fee on all entries of the dunning run, if given.
	 */
	private void setReminderFee() {
		
		if (reminderFee==null || dunningRun==null) return;
		for (DunningEntry<?,?> entry : dunningRun.getEntries()) {
			entry.setReminderFee(reminderFee);
		}
		
	}
	
	private void initBusinessObjectFactory() throws NoSuchTenantException {
	
		bof = cof.lookupAdapter(adapterName);
		bof.setTenant(orgNo, countryCode);
		
	}
	
	private void createDunningRun() throws Exception {
		dunningRun = bof.lookupDunningRun(null, dueDateUntil);
		excludedInvoices = DunningRunFilter.excludeDueBefore(dunningRun, dueDateFrom);
	}
	
	/**
	 * If the tenant information has a remit to account, it's used on the reminders instead of
	 * the account from the adapter: as the account on the payment slip (bgNo) and as the
	 * creditor's payment information, whose account type (BG / PG) decides the slip.
	 */
	private void applyTenantPaymentInformation() {
		
		if (dunningRun==null || dunningRun.getEntries().isEmpty()) return;
		TenantInformation ti = lookupTenantInformation();
		if (ti == null || ti.getRemitToAccount() == null || ti.getRemitToAccount().trim().length() == 0) {
			return;
		}
		for (DunningEntry<?,?> entry : dunningRun.getEntries()) {
			entry.setBgNo(ti.getRemitToAccount().trim());
			if (entry.getCreditor() != null) {
				ti.copyPaymentInformationTo(entry.getCreditor());
			}
		}
		boolean plusgiro = isPlusgiro(ti.getRemitToAccountType());
		sess.getConsole().println("Payment to " + (plusgiro ? "plusgiro " : "bankgiro ") + ti.getRemitToAccount().trim()
				+ " (from tenant information)");
		if (plusgiro) {
			warnAboutOcrNumbersForPlusgiro();
		}
		sess.getConsole().println();
		
	}
	
	private static boolean isPlusgiro(String accountType) {
		return accountType != null && "PG".equalsIgnoreCase(accountType.trim());
	}
	
	/**
	 * Plusgiro needs OCR numbers with a length digit and a check digit (5-15 digits), which
	 * invoices made for bankgiro don't always have. The reminders show the invoice's own OCR.
	 */
	private void warnAboutOcrNumbersForPlusgiro() {
		
		List<String> invalid = new ArrayList<String>();
		for (DunningEntry<?,?> entry : dunningRun.getEntries()) {
			for (Invoice<?> inv : entry.getInvoices()) {
				if (!isValidPlusgiroOcr(inv.getOcr())) {
					invalid.add(inv.getDocumentKey() + " (" + (inv.getOcr() != null ? inv.getOcr() : "no OCR") + ")");
				}
			}
		}
		if (!invalid.isEmpty()) {
			sess.getConsole().println("Warning: these invoices' OCR numbers lack the length and check digits plusgiro requires, "
					+ "so plusgiro payments may be rejected: " + String.join(", ", invalid));
		}
		
	}
	
	/** OCR number with length digit and check digit, 5-15 digits, as plusgiro requires. */
	static boolean isValidPlusgiroOcr(String ocr) {
		if (ocr == null) return false;
		String digits = ocr.trim();
		if (!digits.matches("\\d{5,15}")) return false;
		int lengthDigit = Character.digit(digits.charAt(digits.length() - 2), 10);
		return lengthDigit == digits.length() % 10 && NotimaUtil.isValidOCRNumber(digits);
	}
	
	private void printDunningRun() {
		
		DunningRunTable table = new DunningRunTable(dunningRun);
		table.getShellTable().print(sess.getConsole());
		if (!table.isEmpty()) {
			sess.getConsole().println();
			sess.getConsole().println(table.getSummary());
		}
		if (excludedInvoices > 0) {
			sess.getConsole().println(excludedInvoices + (excludedInvoices==1 ? " invoice" : " invoices")
					+ " due before " + dueDateFromStr + " not included");
		}
		
	}
	
	private void writeToFileIfApplicable() throws IOException {
		
		if (outFile!=null) {
			
			if (DEFAULT_OUTFILE.equalsIgnoreCase(outFile.trim())) {
				outFile = defaultOutFile();
			}
				
			if (!outFile.endsWith(".xml")) {
				outFile += ".xml";
			}
			JAXB.marshal(dunningRun, new File(outFile));
			sess.getConsole().println("Written to " + outFile);
			
		}		
		
	}
	
	/**
	 * A new file in the tenant's default output directory:
	 * {@code <tenant>-dunning-run-<yyyyMMdd-HHmmss>.xml}, named like the files of read-invoices.
	 */
	private String defaultOutFile() throws IOException {
		
		TenantInformation ti = lookupTenantInformation();
		if (ti == null || ti.getDefaultOutputDirectory() == null || ti.getDefaultOutputDirectory().trim().length() == 0) {
			throw new IOException("No default output directory configured for tenant " + getTenantId()
					+ ". Set it with set-tenant-info or give a file name with --outfile.");
		}
		String tenantName = (ti.getTenant() != null && ti.getTenant().hasName())
				? ti.getTenant().getLegalName().replaceAll("[^a-zA-Z0-9_\\-]", "_")
				: orgNo;
		String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		File dir = new File(ti.getDefaultOutputDirectory().trim());
		dir.mkdirs();
		return new File(dir, tenantName + "-dunning-run-" + timestamp + ".xml").getPath();
		
	}
	
	private TaxSubjectIdentifier getTenantId() {
		return new TaxSubjectIdentifier(orgNo.trim(), countryCode);
	}
	
	/**
	 * @return	The stored tenant information, or null if there is none.
	 */
	private TenantInformation lookupTenantInformation() {
		TenantInformationFactory tif = cof.lookupTenantInformationFactory();
		return tif != null ? tif.getTenantInformation(getTenantId()) : null;
	}
	
}

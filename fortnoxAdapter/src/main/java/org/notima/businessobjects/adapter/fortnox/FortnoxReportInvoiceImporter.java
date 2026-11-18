package org.notima.businessobjects.adapter.fortnox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceFileData;
import org.notima.generic.ifacebusinessobjects.InvoiceFileImporter;

/**
 * Base class for reading Fortnox ledger reports (kundreskontralista /
 * leverantörsreskontralista) exported as text.
 *
 * <p>The file is ISO-8859-1 encoded and tab-separated. Business partner blocks are
 * separated by lines of dashes (-----). Each block starts with a partner line
 * (partner id, partner name) followed by one or more invoice lines. The report date
 * is taken from the "Per datum" header line.
 *
 * <p>The amounts in SEK ("Belopp i SEK", "Saldo i SEK") are left out of the report when all
 * invoices are in SEK; the column header tells which.
 *
 * <p>The report only lists invoices with a remaining balance, so the result is
 * flagged as {@link InvoiceFileData#isOpenItemsOnly() open items only}.
 */
public abstract class FortnoxReportInvoiceImporter implements InvoiceFileImporter {

	protected static final Charset CHARSET = Charset.forName("ISO-8859-1");
	protected static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final Pattern DATE_PATTERN   = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
	private static final Pattern SEPARATOR_LINE = Pattern.compile("^-{5,}\\s*$");
	private static final Pattern PER_DATUM      = Pattern.compile("Per datum\\s+(\\d{4}-\\d{2}-\\d{2})");

	/** Number of lines at the start of the file searched by {@link #canImport(File)}. */
	private static final int HEADER_LINES = 20;

	@Override public String   getSystemName()     { return FortnoxAdapter.SYSTEMNAME; }
	@Override public String[] getFileExtensions() { return new String[]{"txt"}; }
	@Override public String   getFileDescription() { return getFormatName() + " (*.txt)"; }

	/**
	 * @return True for the customer ledger (AR), false for the supplier ledger (AP).
	 */
	protected abstract boolean isSalesTransaction();

	/** @return True if {@code line}, one of the first lines of the file, identifies this report type. */
	protected abstract boolean isReportHeader(String line);

	/** @return True for column header and summary rows that should be ignored. */
	protected abstract boolean isSkippableLine(String col0);

	/** @return True if the line is an invoice line. */
	protected abstract boolean isInvoiceLine(String[] cols);

	/** @return True if the line starts a new business partner block. */
	protected abstract boolean isPartnerLine(String[] cols);

	/** Parses one invoice line, or returns null if the line is malformed. */
	protected abstract Invoice<?> parseInvoice(String[] cols, BusinessPartner<?> partner);

	@Override
	public boolean canImport(File file) {
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), CHARSET))) {
			for (int i = 0; i < HEADER_LINES; i++) {
				String line = br.readLine();
				if (line == null) break;
				if (isReportHeader(line)) return true;
			}
		} catch (IOException ignored) {}
		return false;
	}

	@Override
	public InvoiceFileData importFile(File file) throws IOException {
		List<String> lines = readLines(file);
		List<Invoice<?>> invoices = new ArrayList<>();

		BusinessPartner<?> currentPartner = null;
		int amountCol = -1;		// set when the amounts in SEK are left out (see expandSekColumns)
		for (String rawLine : lines) {
			String trimmed = rawLine.trim();
			if (trimmed.isEmpty()) continue;

			// Partner-block separator — reset current partner
			if (SEPARATOR_LINE.matcher(trimmed).matches()) {
				currentPartner = null;
				continue;
			}

			String[] cols = rawLine.split("\t", -1);
			if (cols.length < 2) continue;

			if (isColumnHeader(cols)) amountCol = withoutSekColumns(cols);

			if (isSkippableLine(cols[0].trim())) continue;

			if (isInvoiceLine(cols) && currentPartner != null) {
				Invoice<?> inv = parseInvoice(amountCol >= 0 ? expandSekColumns(cols, amountCol) : cols, currentPartner);
				if (inv != null) invoices.add(inv);
				continue;
			}

			if (isPartnerLine(cols)) {
				currentPartner = buildPartner(cols[0].trim(), cols[1].trim());
			}
		}

		InvoiceFileData result = new InvoiceFileData();
		result.setSourceFormat(getFormatName());
		result.setSourceSystem(FortnoxAdapter.SYSTEMNAME);
		result.setAsOfDate(extractReportDate(lines));
		result.setOpenItemsOnly(true);
		result.setInvoices(invoices);
		return result;
	}

	// -------------------------------------------------------------------------
	// Helpers for subclasses
	// -------------------------------------------------------------------------

	/**
	 * Creates an invoice with the fields common to both ledgers.
	 *
	 * @param partner	The customer or supplier.
	 * @param invoiceKey	The invoice number.
	 * @param cols		The line's columns.
	 * @param dateCol	Index of the invoice date column. The due date, voucher number,
	 * 					currency, amount, amount in SEK, balance and balance in SEK
	 * 					columns follow in that order.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected Invoice<?> buildInvoice(BusinessPartner<?> partner, String invoiceKey, String[] cols, int dateCol) {
		Invoice inv = new Invoice();
		inv.setSalesTransaction(isSalesTransaction());
		inv.setInvoiceKey(invoiceKey);
		inv.setInvoiceDate(toDate(LocalDate.parse(cols[dateCol].trim(), DATE_FMT)));
		inv.setDueDate(toDate(LocalDate.parse(cols[dateCol + 1].trim(), DATE_FMT)));
		// Vernr ("D 23"): to find the voucher the invoice is posted on
		org.notima.generic.businessobjects.settlement.PostedInvoices.setSourceVoucherNo(inv, clean(cols[dateCol + 2]));
		inv.setCurrency(cols[dateCol + 3].trim());
		inv.setGrandTotal(parseSwedishAmount(cols[dateCol + 5].trim()));  // belopp_sek
		inv.setOpenAmt(parseSwedishAmount(cols[dateCol + 7].trim()));     // saldo_sek
		inv.setBusinessPartner(partner);
		inv.setBillBpartner(partner);
		return inv;
	}

	protected static boolean isDate(String s) {
		return DATE_PATTERN.matcher(s.trim()).matches();
	}

	/** Trims whitespace including non-breaking spaces, returning null if nothing remains. */
	protected static String clean(String s) {
		if (s == null) return null;
		String c = s.replace(' ', ' ').trim();
		return c.isEmpty() ? null : c;
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/** The column header of the invoice lines: has a "Belopp" column. */
	private static boolean isColumnHeader(String[] cols) {
		for (String c : cols) if (c.trim().equals("Belopp")) return true;
		return false;
	}

	/**
	 * When all invoices are in SEK, Fortnox leaves out the columns "Belopp i SEK" and
	 * "Saldo i SEK".
	 *
	 * @return	The index of "Belopp" in such a header, else -1.
	 */
	private static int withoutSekColumns(String[] cols) {
		int amount = -1;
		for (int i = 0; i < cols.length; i++) {
			String c = cols[i].trim();
			if (c.equals("Belopp")) amount = i;
			if (c.endsWith("i SEK")) return -1;
		}
		return amount;
	}

	/**
	 * Gives an invoice line without the amounts in SEK the columns of the full report, with
	 * the amount and the balance (already in SEK) as the amounts in SEK.
	 */
	private static String[] expandSekColumns(String[] cols, int amountCol) {
		if (cols.length < amountCol + 2) return cols;
		List<String> result = new ArrayList<>();
		for (int i = 0; i < amountCol; i++) result.add(cols[i]);
		result.add(cols[amountCol]);
		result.add(cols[amountCol]);
		result.add(cols[amountCol + 1]);
		result.add(cols[amountCol + 1]);
		for (int i = amountCol + 2; i < cols.length; i++) result.add(cols[i]);
		return result.toArray(new String[0]);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private BusinessPartner<?> buildPartner(String id, String name) {
		BusinessPartner bp = new BusinessPartner();
		bp.setIdentityNo(id);
		bp.setName(name);
		if (isSalesTransaction()) bp.setIsCustomer(Boolean.TRUE);
		else                      bp.setIsVendor(Boolean.TRUE);
		bp.setCompany(true);
		return bp;
	}

	private List<String> readLines(File file) throws IOException {
		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(file), CHARSET))) {
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		}
		return lines;
	}

	private LocalDate extractReportDate(List<String> lines) {
		for (String line : lines) {
			Matcher m = PER_DATUM.matcher(line.trim());
			if (m.find()) {
				return LocalDate.parse(m.group(1), DATE_FMT);
			}
		}
		return null;
	}

	private static Date toDate(LocalDate d) {
		return d == null ? null : Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Converts a Swedish-formatted amount string to a double.
	 * Space is the thousands separator; comma is the decimal separator.
	 * Example: "2 813,00" → 2813.00, "-7 596,00" → -7596.00
	 */
	private static double parseSwedishAmount(String s) {
		return Double.parseDouble(s.replace(" ", "").replace(" ", "").replace(",", "."));
	}

}

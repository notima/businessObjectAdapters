package org.notima.businessobjects.adapter.jasperreports;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.InvoiceLine;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Person;
import org.notima.generic.businessobjects.tax.BasicTaxRateProviderSE;
import org.notima.util.TaxIdFormatter;
import org.notima.util.TaxIdStructure;

/**
 * Static helpers for expressions in the bundled invoice reports (e.g. InvoicePlain).
 */
public class InvoiceReportHelper {

	private InvoiceReportHelper() {}

	/**
	 * The address lines of {@code location}, one per line and without empty ones:
	 * c/o, address 1, address 2 and postal code + city.
	 *
	 * @param location	The address, may be null.
	 * @return	The lines separated by newlines, empty if there are none.
	 */
	public static String addressLines(Location location) {
		StringBuilder sb = new StringBuilder();
		if (location==null) return "";
		if (!isEmpty(location.getCo())) appendLine(sb, "c/o " + location.getCo().trim());
		appendLine(sb, location.getAddress1());
		appendLine(sb, location.getAddress2());
		String postal = location.getPostal()!=null ? location.getPostal().trim() : "";
		String city = location.getCity()!=null ? location.getCity().trim() : "";
		appendLine(sb, (postal + " " + city).trim());
		return sb.toString();
	}

	/**
	 * The name and the official address of {@code bp}, one per line and without empty ones.
	 */
	public static String nameAndAddress(BusinessPartner<?> bp) {
		if (bp==null) return "";
		StringBuilder sb = new StringBuilder();
		appendLine(sb, bp.getName());
		appendLine(sb, addressLines(bp.getAddressOfficial()));
		return sb.toString();
	}

	/**
	 * @param lines		Invoice lines.
	 * @param taxKey	A tax key, e.g. {@value BasicTaxRateProviderSE#KEY_SE_REVERSE_CHARGE}.
	 * @return	True if any of the lines has the tax key.
	 */
	public static boolean hasTaxKey(List<?> lines, String taxKey) {
		if (lines==null || taxKey==null) return false;
		for (Object o : lines) {
			if (o instanceof InvoiceLine && taxKey.equals(((InvoiceLine)o).getTaxKey())) return true;
		}
		return false;
	}

	/**
	 * @return	True if any of the lines is domestic reverse charge
	 * 			({@value BasicTaxRateProviderSE#KEY_SE_REVERSE_CHARGE}).
	 */
	public static boolean hasDomesticReverseCharge(List<?> lines) {
		return hasTaxKey(lines, BasicTaxRateProviderSE.KEY_SE_REVERSE_CHARGE);
	}

	/**
	 * The VAT number of a Swedish buyer, as printed on invoices with domestic reverse charge.
	 *
	 * @return	The VAT number of {@code bp}, or its tax id converted to a Swedish VAT number
	 * 			when the partner has no country (domestic reverse charge means a Swedish
	 * 			buyer), or null.
	 */
	public static String vatNo(BusinessPartner<?> bp) {
		if (bp==null) return null;
		String vatNo = bp.getVatNo();
		if (!isEmpty(vatNo)) return vatNo.trim();
		if (isEmpty(bp.getTaxId())) return null;
		try {
			String format = TaxIdFormatter.determineFormat("SE", bp.getTaxId());
			if (format==null || TaxIdStructure.FMT_UNKNOWN.equals(format)) return null;
			return TaxIdFormatter.printTaxId(format, bp.getTaxId(), TaxIdStructure.FMT_SE14);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * The seller's tax registration as printed in the invoice footer, in Swedish.
	 *
	 * @see #taxRegistration(BusinessPartner, ResourceBundle)
	 */
	public static String taxRegistration(BusinessPartner<?> bp) {
		return taxRegistration(bp, null);
	}

	/**
	 * The seller's tax registration as printed in the invoice footer, e.g.
	 * {@code "Momsreg.nr: SE556000000001     Godkänd för F-skatt"}: the VAT number if one
	 * is set (not derived from the tax id) and the F-tax approval if approved.
	 *
	 * @param texts	The report's texts ({@code invoice.vatRegNo}, {@code invoice.fTax});
	 * 				null or a missing key gives the Swedish text.
	 * @return	The text, or null if there is nothing to print.
	 */
	public static String taxRegistration(BusinessPartner<?> bp, ResourceBundle texts) {
		if (bp==null) return null;
		StringBuilder sb = new StringBuilder();
		if (bp.hasVatNo()) sb.append(text(texts, "invoice.vatRegNo", "Momsreg.nr:")).append(' ').append(bp.getVatNo().trim());
		if (bp.isApprovedForFTax()) {
			if (sb.length()>0) sb.append("     ");
			sb.append(text(texts, "invoice.fTax", "Godkänd för F-skatt"));
		}
		return sb.length()>0 ? sb.toString() : null;
	}

	private static String text(ResourceBundle texts, String key, String fallback) {
		if (texts==null) return fallback;
		try {
			return texts.getString(key);
		} catch (MissingResourceException e) {
			return fallback;
		}
	}

	/**
	 * Formats an amount like the report pattern {@code #,##0.00}, but with an ordinary
	 * space as grouping separator. Locales such as sv-SE group with a (narrow) no-break
	 * space, which the PDF base fonts (e.g. Helvetica, used for bold text) can't show, so
	 * the separator would disappear.
	 *
	 * @param amount	The amount, may be null.
	 * @param locale	The report locale, or null for the default locale.
	 * @return	The formatted amount, or null if there's no amount.
	 */
	public static String formatAmount(Double amount, Locale locale) {
		if (amount==null) return null;
		DecimalFormat fmt = new DecimalFormat("#,##0.00;-#,##0.00",
				DecimalFormatSymbols.getInstance(locale!=null ? locale : Locale.getDefault()));
		return fmt.format(amount).replace('\u202F', ' ').replace('\u00A0', ' ');
	}

	/** @return	The tax id of {@code bp}, or null if there's none. */
	public static String taxId(BusinessPartner<?> bp) {
		return bp!=null ? blankToNull(bp.getTaxId()) : null;
	}

	/** @return	The phone number of {@code bp}'s official address, or null if there's none. */
	public static String phone(BusinessPartner<?> bp) {
		return bp!=null && bp.getAddressOfficial()!=null ? blankToNull(bp.getAddressOfficial().getPhone()) : null;
	}

	/** @return	The e-mail address of {@code bp}'s official address, or null if there's none. */
	public static String email(BusinessPartner<?> bp) {
		return bp!=null && bp.getAddressOfficial()!=null ? blankToNull(bp.getAddressOfficial().getEmail()) : null;
	}

	private static String blankToNull(String s) {
		return s==null || s.trim().isEmpty() ? null : s.trim();
	}

	/**
	 * @return	The name of {@code person}, or null if there's no person or it has no name.
	 */
	public static String personName(Person person) {
		return person!=null && person.hasName() ? person.getName().trim() : null;
	}

	private static void appendLine(StringBuilder sb, String line) {
		if (isEmpty(line)) return;
		if (sb.length()>0) sb.append('\n');
		sb.append(line.trim());
	}

	private static boolean isEmpty(String s) {
		return s==null || s.trim().length()==0;
	}

}

package org.notima.businessobjects.adapter.fortnox;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;

/**
 * Reads the Fortnox supplier ledger report (leverantörsreskontralista) exported as text.
 *
 * <p>Invoice line columns (0-indexed): 0=löpnr, 1=OCR/Fakturanr, 2=faktdat, 3=förfdat,
 * 4=vernr, 5=valuta, 6=belopp, 7=belopp_sek, 8=saldo, 9=saldo_sek.
 *
 * <p>The löpnr (Fortnox's own number for the supplier invoice) becomes the invoice key,
 * since the OCR/Fakturanr column is often empty. A non-empty OCR/Fakturanr is kept as
 * {@link Invoice#getOcr() OCR}.
 *
 * <p>Supplier IDs may be alphanumeric (e.g. "282-4647"), unlike the purely
 * numeric customer IDs in the AR report. Invoice lines are identified by the
 * presence of a date in column 2 (faktdat).
 */
public class FortnoxApReportImporter extends FortnoxReportInvoiceImporter {

	@Override public String getFormatName() { return "Fortnox AP report"; }

	@Override protected boolean isSalesTransaction() { return false; }

	@Override
	protected boolean isReportHeader(String line) {
		return line.contains("Leverant");  // Leverantörsbetalningar / Leverantörsreskontralista
	}

	@Override
	protected boolean isSkippableLine(String col0) {
		return col0.equals("Levnr")
				|| col0.equals("Löpnr")      // Löpnr (ö = U+00F6)
				|| col0.startsWith("Antal fakturor")
				|| col0.startsWith("Summa")
				|| col0.startsWith("Totalsumma")
				|| col0.startsWith("Utskrivet");
	}

	@Override
	protected boolean isInvoiceLine(String[] cols) {
		return cols.length >= 3 && isDate(cols[2].trim());
	}

	@Override
	protected boolean isPartnerLine(String[] cols) {
		// col0 and col1 non-empty, col2 absent or not a date
		return !cols[0].trim().isEmpty() && !cols[1].trim().isEmpty()
				&& (cols.length < 3 || !isDate(cols[2].trim()));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	protected Invoice<?> parseInvoice(String[] cols, BusinessPartner<?> supplier) {
		if (cols.length < 10) return null;
		try {
			Invoice inv = buildInvoice(supplier, cols[0].trim(), cols, 2);
			inv.setOcr(clean(cols[1]));
			return inv;
		} catch (Exception e) {
			return null;  // skip malformed lines silently
		}
	}

}

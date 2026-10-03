package org.notima.businessobjects.adapter.fortnox;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Invoice;

/**
 * Reads the Fortnox customer ledger report (kundreskontralista) exported as text.
 *
 * <p>Invoice line columns (0-indexed): 0=faktnr, 1=faktdat, 2=förfdat, 3=vernr,
 * 4=valuta, 5=belopp, 6=belopp_sek, 7=saldo, 8=saldo_sek.
 */
public class FortnoxArReportImporter extends FortnoxReportInvoiceImporter {

	@Override public String getFormatName() { return "Fortnox AR report"; }

	@Override protected boolean isSalesTransaction() { return true; }

	@Override
	protected boolean isReportHeader(String line) {
		return line.contains("Kundreskontra") || line.startsWith("Kundnr\t");
	}

	@Override
	protected boolean isSkippableLine(String col0) {
		return col0.equals("Kundnr")
				|| col0.equals("Fakturanr")
				|| col0.startsWith("Antal fakturor")
				|| col0.startsWith("Summa")
				|| col0.startsWith("Totalsumma")
				|| col0.startsWith("Utskrivet");
	}

	@Override
	protected boolean isInvoiceLine(String[] cols) {
		// col1 looks like a date (faktdat)
		return isDate(cols[1].trim());
	}

	@Override
	protected boolean isPartnerLine(String[] cols) {
		// col0 is a plain integer, col1 is the customer name
		return cols[0].trim().matches("-?\\d+") && !isDate(cols[1].trim());
	}

	@Override
	protected Invoice<?> parseInvoice(String[] cols, BusinessPartner<?> customer) {
		if (cols.length < 9) return null;
		try {
			return buildInvoice(customer, cols[0].trim(), cols, 1);
		} catch (Exception e) {
			return null;  // skip malformed lines silently
		}
	}

}

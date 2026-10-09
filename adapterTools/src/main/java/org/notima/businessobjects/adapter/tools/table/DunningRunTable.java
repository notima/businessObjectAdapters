package org.notima.businessobjects.adapter.tools.table;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.DunningEntry;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.Location;

/**
 * One row per reminder (dunning entry) of a dunning run: the customer, the invoices
 * reminded, how overdue they are and what the customer is asked to pay.
 *
 * <p>The reminder templates add the reminder fee to each invoice reminded, so the amount
 * to pay is the open amount plus the fee per invoice.</p>
 */
public class DunningRunTable extends GenericTable {

	/** The fee the reminder templates use when the dunning entry has none. */
	public static final double TEMPLATE_DEFAULT_FEE = 60.0;

	private int		reminders;
	private int		invoices;
	private double	openTotal;
	private double	feeTotal;
	private boolean	defaultFeeUsed;

	public DunningRunTable(DunningRun<?,?> dunningRun) {

		addColumn("Letter no");
		addColumn("Cust no");
		addColumn("Customer");
		addColumn("Address");
		addColumn("Invoices");
		addColumn("Oldest due");
		addColumn("Days late", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Open amt", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("Fee", GenericColumn.ALIGNMENT_RIGHT);
		addColumn("To pay", GenericColumn.ALIGNMENT_RIGHT);

		if (dunningRun==null || dunningRun.getEntries()==null || dunningRun.getEntries().isEmpty()) {
			setEmptyTableText("No overdue invoices to remind");
			return;
		}

		for (DunningEntry<?,?> entry : dunningRun.getEntries()) {
			addEntry(entry);
		}

	}

	private void addEntry(DunningEntry<?,?> entry) {

		List<String> keys = new ArrayList<String>();
		double open = 0;
		Date oldestDue = null;
		for (Invoice<?> inv : entry.getInvoices()) {
			keys.add(inv.getDocumentKey()!=null ? inv.getDocumentKey() : "?");
			open += inv.getOpenAmt();
			if (inv.getDueDate()!=null && (oldestDue==null || inv.getDueDate().before(oldestDue))) {
				oldestDue = inv.getDueDate();
			}
		}
		double fee = feeOf(entry) * entry.getInvoices().size();
		if (entry.getReminderFee()==null) defaultFeeUsed = true;

		reminders++;
		invoices += entry.getInvoices().size();
		openTotal += open;
		feeTotal += fee;

		BusinessPartner<?> debtor = entry.getDebtor();

		addRow().addContent(
				nvl(entry.getLetterNo()),
				debtor!=null ? nvl(debtor.getIdentityNo()) : "",
				debtor!=null ? nvl(debtor.getName()) : "",
				debtor!=null ? address(debtor.getAddressOfficial()) : "",
				String.join(", ", keys),
				oldestDue!=null ? dfmt.format(oldestDue) : "",
				oldestDue!=null ? String.valueOf(daysSince(oldestDue)) : "",
				nfmt.format(open),
				nfmt.format(fee),
				nfmt.format(open + fee));

	}

	/** The reminder fee per invoice, as the templates apply it. */
	public static double feeOf(DunningEntry<?,?> entry) {
		return entry.getReminderFee()!=null ? entry.getReminderFee() : TEMPLATE_DEFAULT_FEE;
	}

	/**
	 * Summary of the run, e.g. "5 reminders, 7 invoices, open 12 345,67, fees 0,00, to pay 12 345,67",
	 * noting when the templates' default fee applies.
	 */
	public String getSummary() {
		return reminders + (reminders==1 ? " reminder, " : " reminders, ")
				+ invoices + (invoices==1 ? " invoice" : " invoices")
				+ ", open " + nfmt.format(openTotal)
				+ ", fees " + nfmt.format(feeTotal)
				+ ", to pay " + nfmt.format(openTotal + feeTotal)
				+ (defaultFeeUsed ? " (reminder fee " + nfmt.format(TEMPLATE_DEFAULT_FEE)
						+ " per invoice, the template default)" : "");
	}

	private static long daysSince(Date date) {
		return TimeUnit.MILLISECONDS.toDays(new Date().getTime() - date.getTime());
	}

	private static String address(Location loc) {
		if (loc==null) return "";
		StringBuilder sb = new StringBuilder(nvl(loc.getAddress1()).trim());
		String city = nvl(loc.getCity()).trim();
		if (city.length()>0) {
			if (sb.length()>0) sb.append(", ");
			sb.append(city);
		}
		return sb.toString();
	}

	private static String nvl(String s) {
		return s!=null ? s : "";
	}

}

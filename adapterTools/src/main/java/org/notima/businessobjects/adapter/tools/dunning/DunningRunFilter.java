package org.notima.businessobjects.adapter.tools.dunning;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.notima.generic.businessobjects.DunningEntry;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;

/**
 * Removes invoices from a dunning run after it has been created, ie old invoices that are
 * hard to collect, or invoices the customer doesn't owe anything on.
 */
public class DunningRunFilter {

	private DunningRunFilter() {}

	/**
	 * Removes the invoices due before {@code dueDateFrom}.
	 *
	 * @param dunningRun	The dunning run to filter.
	 * @param dueDateFrom	The earliest due date to keep; null to keep all.
	 * @return	The number of invoices removed.
	 * @see #exclude(DunningRun, Predicate)
	 */
	public static int excludeDueBefore(DunningRun<?,?> dunningRun, Date dueDateFrom) {

		if (dueDateFrom == null) return 0;
		return exclude(dunningRun, inv -> inv.getDueDate() != null && inv.getDueDate().before(dueDateFrom));

	}

	/**
	 * Removes what the customer doesn't owe anything on. Invoices without open amount are
	 * removed, and so are reminders whose open amounts in total aren't positive, ie a
	 * customer with only overpaid invoices and credit notes (a debt to the customer).
	 * Overpaid invoices and credit notes of a customer that owes more than that are kept,
	 * so that the reminder shows what the customer owes in total.
	 *
	 * @param dunningRun	The dunning run to filter.
	 * @return	The number of invoices removed.
	 * @see #exclude(DunningRun, Predicate)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int excludeNotOwed(DunningRun<?,?> dunningRun) {

		int removed = exclude(dunningRun, inv -> Math.abs(inv.getOpenAmt()) < 0.005);
		if (dunningRun == null || dunningRun.getEntries() == null) return removed;

		for (Iterator<DunningEntry<?,?>> entries = dunningRun.getEntries().iterator(); entries.hasNext(); ) {
			DunningEntry entry = entries.next();
			double owed = 0;
			for (Invoice<?> inv : (List<Invoice<?>>) entry.getInvoices()) {
				owed += inv.getOpenAmt();
			}
			if (owed < 0.005) {
				removed += entry.getInvoices().size();
				entries.remove();
			}
		}
		return removed;

	}

	/**
	 * Removes the invoices matching {@code remove}. Reminders left without invoices
	 * are removed; the totals of the others are recalculated, and a reminder whose letter /
	 * OCR number came from a removed invoice gets those of its first remaining invoice.
	 *
	 * @param dunningRun	The dunning run to filter.
	 * @param remove		Which invoices to remove.
	 * @return	The number of invoices removed.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int exclude(DunningRun<?,?> dunningRun, Predicate<Invoice<?>> remove) {

		if (dunningRun == null || dunningRun.getEntries() == null) return 0;

		int removed = 0;
		for (Iterator<DunningEntry<?,?>> entries = dunningRun.getEntries().iterator(); entries.hasNext(); ) {
			DunningEntry entry = entries.next();
			String firstKey = entry.getInvoices().isEmpty() ? null
					: ((Invoice<?>) entry.getInvoices().get(0)).getDocumentKey();
			boolean firstRemoved = false;
			int removedHere = 0;
			int index = 0;
			for (Iterator<Invoice<?>> invoices = entry.getInvoices().iterator(); invoices.hasNext(); index++) {
				Invoice<?> inv = invoices.next();
				if (remove.test(inv)) {
					if (index == 0) firstRemoved = true;
					invoices.remove();
					removedHere++;
				}
			}
			removed += removedHere;
			if (entry.getInvoices().isEmpty()) {
				entries.remove();
				continue;
			}
			if (removedHere > 0) {
				Invoice<?> first = (Invoice<?>) entry.getInvoices().get(0);
				if (firstRemoved && firstKey != null && firstKey.equals(entry.getLetterNo())) {
					entry.setLetterNo(first.getDocumentKey());
					entry.setOcrNo(first.getOcr());
				}
				entry.calculateValues();
			}
		}
		return removed;

	}

}

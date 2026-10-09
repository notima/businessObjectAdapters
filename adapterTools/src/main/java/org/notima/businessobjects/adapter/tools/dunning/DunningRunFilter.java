package org.notima.businessobjects.adapter.tools.dunning;

import java.util.Date;
import java.util.Iterator;

import org.notima.generic.businessobjects.DunningEntry;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;

/**
 * Removes invoices from a dunning run after it has been created, ie old invoices that are
 * hard to collect.
 */
public class DunningRunFilter {

	private DunningRunFilter() {}

	/**
	 * Removes the invoices due before {@code dueDateFrom}. Reminders left without invoices
	 * are removed; the totals of the others are recalculated, and a reminder whose letter /
	 * OCR number came from a removed invoice gets those of its first remaining invoice.
	 *
	 * @param dunningRun	The dunning run to filter.
	 * @param dueDateFrom	The earliest due date to keep; null to keep all.
	 * @return	The number of invoices removed.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static int excludeDueBefore(DunningRun<?,?> dunningRun, Date dueDateFrom) {

		if (dunningRun == null || dunningRun.getEntries() == null || dueDateFrom == null) return 0;

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
				if (inv.getDueDate() != null && inv.getDueDate().before(dueDateFrom)) {
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

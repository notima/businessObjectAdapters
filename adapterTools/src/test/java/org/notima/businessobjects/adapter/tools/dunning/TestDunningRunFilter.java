package org.notima.businessobjects.adapter.tools.dunning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.notima.generic.businessobjects.DunningEntry;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceLine;

class TestDunningRunFilter {

	private static Invoice<Object> invoice(String key, String dueDate, double openAmt) {
		Invoice<Object> inv = new Invoice<Object>();
		inv.setDocumentKey(key);
		inv.setOcr(key + "0");
		inv.setDueDate(Date.valueOf(dueDate));
		inv.setOpenAmt(openAmt);
		InvoiceLine line = new InvoiceLine();
		line.setQtyEntered(1);
		line.setPriceActual(openAmt);
		inv.addInvoiceLine(line);
		return inv;
	}

	/** A reminder with letter / OCR number from its first invoice, as the adapters make them. */
	private static DunningEntry<Object,Object> entry(Invoice<Object>... invoices) {
		DunningEntry<Object,Object> e = new DunningEntry<Object,Object>();
		e.setLetterNo(invoices[0].getDocumentKey());
		e.setOcrNo(invoices[0].getOcr());
		for (Invoice<Object> inv : invoices) e.addInvoice(inv);
		return e;
	}

	@Test
	@SuppressWarnings("unchecked")
	void invoicesDueBeforeTheDateAreRemoved() {
		DunningRun<Object,Object> run = new DunningRun<Object,Object>();
		DunningEntry<Object,Object> oldOnly = entry(invoice("100", "2025-03-31", 500));
		DunningEntry<Object,Object> mixed = entry(invoice("101", "2025-06-30", 400), invoice("150", "2026-08-31", 1250));
		DunningEntry<Object,Object> recent = entry(invoice("160", "2026-09-30", 300), invoice("170", "2026-01-01", 200));
		run.addDunningEntry(oldOnly);
		run.addDunningEntry(mixed);
		run.addDunningEntry(recent);

		int removed = DunningRunFilter.excludeDueBefore(run, Date.valueOf("2026-01-01"));

		assertEquals(2, removed);
		List<DunningEntry<?,?>> entries = new ArrayList<DunningEntry<?,?>>(run.getEntries());
		assertEquals(2, entries.size(), "the reminder with only old invoices is dropped");

		assertEquals(1, mixed.getInvoices().size());
		assertEquals("150", mixed.getLetterNo(), "letter no from the first remaining invoice");
		assertEquals("1500", mixed.getOcrNo());
		assertEquals(1250, mixed.getGrandTotal(), 0.001, "totals recalculated");

		assertEquals(2, recent.getInvoices().size(), "due on the from date is kept");
		assertEquals("160", recent.getLetterNo());
	}

	@Test
	@SuppressWarnings("unchecked")
	void noDateKeepsAll() {
		DunningRun<Object,Object> run = new DunningRun<Object,Object>();
		run.addDunningEntry(entry(invoice("100", "2020-01-01", 500)));
		assertEquals(0, DunningRunFilter.excludeDueBefore(run, null));
		assertEquals(1, run.getEntries().size());
	}

}

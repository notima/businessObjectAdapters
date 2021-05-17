package org.notima.fortnox.command.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.api.fortnox.entities3.Invoice;
import org.notima.api.fortnox.entities3.InvoiceRow;

public class InvoiceLineTable extends ShellTable {

	private NumberFormat nfmt = new DecimalFormat("#,##0.00");
	
	public InvoiceLineTable(Invoice invoice, List<InvoiceRow> lines) {

		column("Art No");
		column("Description");
		column("Qty").alignRight();
		column("Unit");
		column("PPU").alignRight();
		column("Line tax").alignRight();
		column("Line total").alignRight();
		column("Acct").alignRight();
		
		if (lines==null) {
			lines = invoice.getInvoiceRows().getInvoiceRow();
		}
		
		if (lines==null) {
			emptyTableText("No lines");
			return;
		}
		
		for (InvoiceRow l : lines) {
			addRow().addContent(
					l.getArticleNumber(),
					l.getDescription(),
					nfmt.format(l.getDeliveredQuantity()),
					l.getUnit(),
					nfmt.format(l.getPrice()),
					nfmt.format(l.getVAT()),
					nfmt.format(l.getTotal()),
					l.getAccountNumber()
			);
		}
		
	}
	
}

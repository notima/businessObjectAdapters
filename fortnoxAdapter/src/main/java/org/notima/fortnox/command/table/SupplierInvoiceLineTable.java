package org.notima.fortnox.command.table;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.api.fortnox.entities3.SupplierInvoice;
import org.notima.api.fortnox.entities3.SupplierInvoiceRow;

public class SupplierInvoiceLineTable extends ShellTable {

	private NumberFormat nfmt = new DecimalFormat("#,##0.00");
	
	public SupplierInvoiceLineTable(SupplierInvoice invoice, List<SupplierInvoiceRow> lines) {

		column("Art No");
		column("Description");
		column("Qty").alignRight();
		column("Unit");
		column("PPU").alignRight();
		column("Line total").alignRight();
		column("Acct").alignRight();
		column("Acct amt").alignRight();

		
		if (lines==null) {
			lines = invoice.getSupplierInvoiceRows().getSupplierInvoiceRow();
		}
		
		if (lines==null) {
			emptyTableText("No lines");
			return;
		}
		
		for (SupplierInvoiceRow l : lines) {
			addRow().addContent(
					l.getArticleNumber(),
					l.getItemDescription(),
					nfmt.format(l.getQuantity()),
					l.getUnit(),
					nfmt.format(l.getPrice()),
					nfmt.format(l.getTotal()),
					l.getAccount(),
					nfmt.format(l.getAmount())
			);
		}
		
	}
	
}

package org.notima.businessobjects.adapter.csv;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.notima.businessobjects.adapter.tools.BasicReportFormatter;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.InvoiceList;

public class InvoiceListToCsv extends BasicReportFormatter {

	public static String[] headers = new String[] {
			"businessPartnerNumber",
			"businessPartnerName",
			"uniqueDocumentNo",
			"invoiceDate",
			"dueDate",
			"amountDue",
			"currency",
			"journalCode",
			"paymentMethod",
			"entryLabel",
			"ledgerAccount"
		};	

	private InvoiceList ilist;
	
	public InvoiceListToCsv(InvoiceList list, Properties props) {
		setFromProperties(props);
		ilist = list;
	}
	
	private Object[] mapInvoice(Invoice<?> invoice) {
		
		return new Object[] {
				invoice.getBusinessPartner().getIdentityNo(),
				invoice.getBusinessPartner().getName(),
				invoice.getDocumentKey(),
				invoice.getInvoiceDate(),
				invoice.getDueDate(),
				invoice.getOpenAmt(),
				invoice.getCurrency(),
				"AP",		// Accounts payable
				"BGPG",
				invoice.getDocumentKey() + "-" + invoice.getBusinessPartner().getName(),
				"4xxx"
		};
		
	}
	
	public String writeToFile() throws IOException {
		
		String path = getPath();
		
		if (!path.toLowerCase().endsWith(".csv")) {
			path += ".csv";
		}

		CSVFormat format = CSVFormat.DEFAULT.withHeader(headers).withRecordSeparator(System.lineSeparator());
		
	    try (Writer writer = Files.newBufferedWriter(Paths.get(path));
	              CSVPrinter printer = new CSVPrinter(writer, format)) {

	             for (Invoice<?> invoice : ilist.getInvoiceList()) {
	                 printer.printRecord(mapInvoice(invoice));
	             }
	    }
	
	    return path;
		
	}
	
}

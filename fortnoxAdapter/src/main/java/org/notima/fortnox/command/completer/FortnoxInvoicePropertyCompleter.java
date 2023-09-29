package org.notima.fortnox.command.completer;

import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;


@Service
public class FortnoxInvoicePropertyCompleter implements Completer {
	
    public static final String INVOICE_PROPERTY_WAREHOUSE_READY = "warehouseReady";
    public static final String INVOICE_PROPERTY_FIX_COMMENT_LINES = "fixCommentLines";
	public static final String INVOICE_PROPERTY_DUE_DATE = "dueDate";
	public static final String INVOICE_PROPERTY_INVOICE_DATE = "invoiceDate";
	// Refresh customer name from the customer register
	public static final String INVOICE_PROPERTY_COPY_CUSTOMER_NAME_TO_INVOICE = "copyCustomerName";

    public static final String[] InvoiceProperties = new String[] {
    	INVOICE_PROPERTY_WAREHOUSE_READY,
    	INVOICE_PROPERTY_FIX_COMMENT_LINES,
		INVOICE_PROPERTY_DUE_DATE,
		INVOICE_PROPERTY_INVOICE_DATE,
		INVOICE_PROPERTY_COPY_CUSTOMER_NAME_TO_INVOICE
    };
    
	@Override
	public int complete(Session session, CommandLine commandLine, List<String> candidates) {

		StringsCompleter delegate = new StringsCompleter();
		for (int i = 0; i<InvoiceProperties.length; i++) {
			delegate.getStrings().add(InvoiceProperties[i]);
		}
		return delegate.complete(session, commandLine, candidates);
		
	}
	
	
}

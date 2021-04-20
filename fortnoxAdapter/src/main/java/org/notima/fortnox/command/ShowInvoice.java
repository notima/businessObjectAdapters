package org.notima.fortnox.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.Invoice;
import org.notima.fortnox.command.table.InvoiceHeaderTable;
import org.notima.fortnox.command.table.InvoiceLineTable;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "fortnox", name = "show-fortnox-invoice", description = "Show a specific invoice")
@Service
public class ShowInvoice extends FortnoxCommand implements Action {

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Reference 
	Session sess;
	
	@Argument(index = 0, name = "orgNo", description ="The orgno of the client", required = true, multiValued = false)
	private String orgNo = "";

	@Argument(index = 1, name = "invoiceNo", description ="The invoice no", required = true, multiValued = false)
	private String invoiceNo;
	
	@Override
	public Object execute() throws Exception {
		
		FortnoxClient3 fc = getFortnoxClient(bofs, orgNo);
		if (fc == null) {
			sess.getConsole().println("Can't get client for " + orgNo);
			return null;
		}
		
		Invoice invoice = fc.getInvoice(invoiceNo);

		if (invoice==null) {
			sess.getConsole().println("Invoice " + invoiceNo + " not found.");
			return null;
		}
		
		InvoiceHeaderTable iht = new InvoiceHeaderTable(invoice);
		InvoiceLineTable ilt = new InvoiceLineTable(invoice, null);
		
		iht.getShellTable().print(sess.getConsole());
		ilt.print(sess.getConsole());
		
		return null;
	}
	
	
}
